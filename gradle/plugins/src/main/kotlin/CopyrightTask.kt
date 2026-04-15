/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Scans tracked files under [scanDirectory] for a `Copyright YYYY[-YYYY] the original author or
 * authors` header and computes the correct end year from git history. Subclasses decide what to do
 * when a year is out of date: [UpdateCopyrightTask] rewrites the file, [VerifyCopyrightTask] fails.
 *
 * Files whose path lies inside any entry of [excludedDirectories] are ignored, which lets the root
 * project's task skip everything owned by subprojects (each of which registers its own task).
 *
 * Per-file `git log --follow` calls are dispatched through Gradle's [WorkerExecutor] with
 * `noIsolation()`, so multiple subprocesses run concurrently within the task.
 */
@DisableCachingByDefault(because = "Reads git history on every invocation.")
abstract class CopyrightTask : DefaultTask() {

    /** The git repository root. Used as the working directory for all `git` invocations. */
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    /** The directory this task is responsible for; only files under it are considered. */
    @get:Internal
    abstract val scanDirectory: DirectoryProperty

    /** Subdirectories of [scanDirectory] that belong to other (sub)projects and must be skipped. */
    @get:Internal
    abstract val excludedDirectories: ListProperty<File>

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    protected abstract fun onOutOfDate(file: File, updatedText: String)

    protected abstract fun onFinish(
        outOfDate: List<String>,
        upToDate: List<String>,
        skipped: List<String>,
    )

    @TaskAction
    fun run() {
        val repoRoot = repositoryRoot.get().asFile
        val scanPrefix = repoRoot.relativeRepoPath(scanDirectory.get().asFile)
        val excludedPrefixes = excludedDirectories.get().map { repoRoot.relativeRepoPath(it) }

        val tracked = CopyrightGit.run(repoRoot, listOf("ls-files"))
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()

        // Phase 1: scan headers on the main thread — this is all in-memory file IO and is cheap.
        val candidates = mutableListOf<Candidate>()
        for (relative in tracked) {
            if (!relative.isUnder(scanPrefix)) continue
            if (excludedPrefixes.any { relative.isUnder(it) }) continue
            val file = repoRoot.resolve(relative)
            if (!file.isFile) continue
            val original = try {
                file.readText(Charsets.UTF_8)
            } catch (_: Exception) {
                continue
            }
            val match = COPYRIGHT_PATTERN.find(original.take(HEAD_CHARS)) ?: continue
            val start = match.groupValues[1].toInt()
            val existingEnd = match.groupValues[2].takeIf { it.isNotEmpty() }?.toInt()
            candidates += Candidate(
                relative = relative,
                file = file,
                original = original,
                matchRange = match.range,
                matchText = match.value,
                start = start,
                currentEnd = existingEnd ?: start,
            )
        }

        // Phase 2: each candidate's `git log --follow` runs in a worker thread; results are
        // written to per-file stub files in the task's temporary directory.
        val yearsDir = File(temporaryDir, "years").apply {
            deleteRecursively()
            mkdirs()
        }
        val yearFiles = LinkedHashMap<String, File>()
        val queue = workerExecutor.noIsolation()
        for ((index, candidate) in candidates.withIndex()) {
            val yearFile = File(yearsDir, "%06d.txt".format(index))
            yearFiles[candidate.relative] = yearFile
            val relativePath = candidate.relative
            queue.submit(
                SubstantiveYearRangeWorkAction::class.java,
                object : Action<SubstantiveYearRangeWorkParameters> {
                    override fun execute(params: SubstantiveYearRangeWorkParameters) {
                        params.repositoryRoot.set(repoRoot)
                        params.relativePath.set(relativePath)
                        params.outputFile.set(yearFile)
                    }
                },
            )
        }
        queue.await()

        // Phase 3: assemble results on the main thread.
        val outOfDate = mutableListOf<String>()
        val upToDate = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        for (candidate in candidates) {
            val content = yearFiles.getValue(candidate.relative).readText().trim()
            if (content.isEmpty()) {
                skipped += "${candidate.relative} (no non-trivial commits)"
                continue
            }
            val (firstYear, lastYear) = content.split(',').let { it[0].toInt() to it[1].toInt() }
            if (firstYear == candidate.start && lastYear == candidate.currentEnd) {
                upToDate += candidate.relative
                continue
            }
            val replacement = formatReplacement(firstYear, lastYear)
            val updatedText = candidate.original.replaceRange(candidate.matchRange, replacement)
            outOfDate += "${candidate.relative}: ${candidate.matchText} -> $replacement"
            onOutOfDate(candidate.file, updatedText)
        }

        onFinish(outOfDate, upToDate, skipped)
    }

    /** Returns [other] expressed as a forward-slash path relative to this repo root, or an empty
     *  string when it is the repo root itself. */
    private fun File.relativeRepoPath(other: File): String =
        this.toPath().toAbsolutePath().normalize()
            .relativize(other.toPath().toAbsolutePath().normalize())
            .toString()
            .replace(File.separatorChar, '/')

    /** True when this path is equal to [prefix] or lies below it, using `/` as the separator. */
    private fun String.isUnder(prefix: String): Boolean {
        if (prefix.isEmpty()) return true
        return this == prefix || this.startsWith("$prefix/")
    }

    private data class Candidate(
        val relative: String,
        val file: File,
        val original: String,
        val matchRange: IntRange,
        val matchText: String,
        val start: Int,
        val currentEnd: Int,
    )

    private fun formatReplacement(firstYear: Int, lastYear: Int): String =
        if (firstYear == lastYear) {
            "Copyright $firstYear the original author or authors"
        } else {
            "Copyright $firstYear-$lastYear the original author or authors"
        }

    companion object {
        private const val HEAD_CHARS = 2048

        /** Matches `Copyright YYYY` or `Copyright YYYY-YYYY` (hyphen or en-dash) followed by the
         *  standard project header text. Third-party headers (e.g. "Copyright 2007-2012 Arthur Blake")
         *  are intentionally skipped. */
        private val COPYRIGHT_PATTERN =
            Regex("""Copyright (\d{4})(?:[\u2013\-](\d{4}))? the original author or authors""")
    }
}

/** Rewrites stale copyright headers in place. */
@DisableCachingByDefault(because = "Mutates source files.")
abstract class UpdateCopyrightTask : CopyrightTask() {

    @get:Internal
    var preview: Boolean = false

    @Option(option = "preview", description = "Report intended changes without modifying files.")
    fun setPreviewOption(value: Boolean) {
        preview = value
    }

    override fun onOutOfDate(file: File, updatedText: String) {
        if (!preview) {
            file.writeText(updatedText, Charsets.UTF_8)
        }
    }

    override fun onFinish(outOfDate: List<String>, upToDate: List<String>, skipped: List<String>) {
        logger.lifecycle("Updated: ${outOfDate.size}${if (preview) " (preview)" else ""}")
        outOfDate.forEach { logger.lifecycle("  $it") }
        logger.lifecycle("Unchanged: ${upToDate.size}")
        logger.lifecycle("Skipped: ${skipped.size}")
        skipped.forEach { logger.lifecycle("  $it") }
    }
}

/** Fails the build if any file's copyright year is behind the last substantive change. */
@DisableCachingByDefault(because = "Reads git history on every invocation.")
abstract class VerifyCopyrightTask : CopyrightTask() {

    override fun onOutOfDate(file: File, updatedText: String) {
        // No-op: verification only reports; it never writes.
    }

    override fun onFinish(outOfDate: List<String>, upToDate: List<String>, skipped: List<String>) {
        logger.lifecycle("Checked ${upToDate.size + outOfDate.size} file(s) for copyright year drift.")
        if (skipped.isNotEmpty()) {
            logger.lifecycle("Skipped: ${skipped.size}")
            skipped.forEach { logger.lifecycle("  $it") }
        }
        if (outOfDate.isNotEmpty()) {
            logger.error("${outOfDate.size} file(s) have out-of-date copyright years:")
            outOfDate.forEach { logger.error("  $it") }
            throw GradleException(
                "Copyright headers are out of date. Run ':updateCopyright' to fix."
            )
        }
    }
}

/** Parameters for a per-file `git log --follow` subprocess dispatched via the Worker API. */
interface SubstantiveYearRangeWorkParameters : WorkParameters {
    val repositoryRoot: DirectoryProperty
    val relativePath: Property<String>
    val outputFile: RegularFileProperty
}

/** Worker action that computes `firstYear,lastYear` for a single file and writes it (or an empty
 *  string, if no qualifying commits exist) to [SubstantiveYearRangeWorkParameters.outputFile]. */
abstract class SubstantiveYearRangeWorkAction : WorkAction<SubstantiveYearRangeWorkParameters> {
    override fun execute() {
        val repoRoot = parameters.repositoryRoot.get().asFile
        val relative = parameters.relativePath.get()
        val range = CopyrightGit.substantiveYearRange(repoRoot, relative)
        parameters.outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(range?.let { (first, last) -> "$first,$last" } ?: "")
        }
    }
}

/** Helpers that shell out to git. Kept as a top-level object so both [CopyrightTask] and the
 *  [LastSubstantiveYearWorkAction] workers can use the same implementation. */
internal object CopyrightGit {

    private const val COMMIT_MARKER = "|COMMIT|"

    /** Matches on whole words so that class names like `CommandFormatter` or tool names
     *  like `checkstyle` do not cause a substantive commit to be mistaken for a cosmetic one. */
    private val TRIVIAL_SUBJECT =
        Regex(
            """\b(?:format|lint|style|whitespace|prettier|typo|reformat|cleanup|cosmetic)\b""",
            RegexOption.IGNORE_CASE,
        )

    /** Returns `(firstYear, lastYear)` covering the substantive-change history of [relative], or
     *  `null` if the file has no non-trivial, non-rename commits. `git log` lists newest first, so
     *  the first accepted entry gives `lastYear` and the final one gives `firstYear`. */
    fun substantiveYearRange(repoRoot: File, relative: String): Pair<Int, Int>? {
        val output = run(
            repoRoot,
            listOf(
                "log", "--follow",
                "--format=$COMMIT_MARKER%cd|%s",
                "--date=format:%Y",
                "--name-status", "--find-renames",
                "--", relative,
            ),
        )

        var currentYear: Int? = null
        var currentTrivial = false
        var lastYear: Int? = null
        var firstYear: Int? = null

        for (line in output.lineSequence()) {
            if (line.isBlank()) continue
            if (line.startsWith(COMMIT_MARKER)) {
                val rest = line.removePrefix(COMMIT_MARKER)
                val sep = rest.indexOf('|')
                if (sep < 0) {
                    currentYear = null
                    continue
                }
                currentYear = rest.substring(0, sep).toIntOrNull()
                currentTrivial = TRIVIAL_SUBJECT.containsMatchIn(rest.substring(sep + 1))
                continue
            }
            val year = currentYear ?: continue
            if (currentTrivial) continue
            // Skip pure renames/copies (R100, C100) where the file's content did not change.
            if (line.startsWith("R100\t") || line.startsWith("C100\t")) continue
            if (lastYear == null) lastYear = year
            firstYear = year
        }
        return if (lastYear != null && firstYear != null) firstYear to lastYear else null
    }

    fun run(dir: File, args: List<String>): String {
        val process = ProcessBuilder(listOf("git") + args)
            .directory(dir)
            .redirectErrorStream(false)
            .start()
        val stdout = ByteArrayOutputStream()
        process.inputStream.copyTo(stdout)
        val exit = process.waitFor()
        if (exit != 0) {
            val err = process.errorStream.bufferedReader().readText()
            throw RuntimeException("git ${args.joinToString(" ")} failed ($exit): $err")
        }
        return stdout.toString(Charsets.UTF_8)
    }
}
