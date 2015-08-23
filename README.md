MiniProfiler-JVM
================

[![Build Status](https://travis-ci.org/tomdcc/miniprofiler-jvm.png)][1]

This library provides (some of) the functionality of the StackExchange [MiniProfiler][2] for JVM-based applications.

(Screenshot)

Installation
------------
The JVM MiniProfiler is deployed to Maven Central and can be added to your project as a dependency using the following coordinates:

    groupId: io.jdev.miniprofiler
    artifactId: miniprofiler-core
    version: 0.5.1

Or just download the jar from http://search.maven.org/ if your build system is a bit less connected.

Basic Usage
-----------
Once installed, start using the profiler thusly:

    Profiler profiler = MiniProfiler.start("/url/my web request");
    try {
        // do some stuff
    } finally {
        profiler.stop();
    }

To profile individual parts of your program, create and stop individual timings:

    Timing t = MiniProfiler.getCurrentProfiler().step("My complicated step");
    try {
        // stuff
    } finally {
        timing.stop();
    }

If you are using Java 7, you can use Java 7 auto-closable resource blocks for even cleaner code:

    try (Timing t = MiniProfiler.getCurrentProfiler().step("My complicated step")) {
        // stuff
    }

Steps are nestable, and will appear nested in profiler output:

    try (Timing t = MiniProfiler.getCurrentProfiler().step("My complicated step")) {
        // stuff
	    try (Timing t1 = MiniProfiler.getCurrentProfiler().step("A sub-part of the complicated step")) {
	        // sub-stuff 1
	    }
	    try (Timing t2 = MiniProfiler.getCurrentProfiler().step("A second, sibling sub-part of the complicated step")) {
	        // sub-stuff 2
	    }
    }

Usage with Dependency Injection
-------------------------------
The default code above uses a static reference to a global ProfilerProvider object. If you are using a dependency injection framework such as Spring or Guice, or if you just like more testable code, then create a ProfilerProvider and inject it into your code. The included DefaultProfilerProvider should be enough for most purposes. Then use it in your code like this:

    // at start of request
    Profiler profiler = profilerProvider.start("/url/my web request");
    try {
        // do some stuff
    } finally {
        profiler.stop();
    }


    // further in where stuff is happening
    Timing t = profilerProvider.getCurrentProfiler().step("My complicated step");
    try {
        // stuff
    } finally {
        timing.stop();
    }


Profiling JDBC Queries
----------------------
To see your SQL queries in your profiling output, just wrap your JDBC DataSource in the ProfilingDataSource, and call
getConnection() on that data source as normal.

Seeing the output
-----------------
To see the output of the profiled request on your web page, add a script tag to the bottom of your HTML page, just inside the body tag. The library comes with a `ScriptTagWriter` class to help with this:

    <%= new ScriptTagWriter().printScriptTag(MiniProfiler.getCurrentProfiler(), request.getContextPath() + "/miniprofiler")%>

This will output a javascript script tag which will load the necessary javascript, css and data from under `/miniprofiler` in your web app.

Inclusion in Servlet Web App
----------------------------
The core library includes a `ProfilingFilter` which does several jobs:

 - Start and stop profiling for all web requests, excluding anything that looks like a file to be served up directly
 - Serve up front-end javascript, CSS and templates used for displaying the profiling info in a web page
 - Serve up profiling data as JSON

The filter can be included in your web.xml like this:

	<filter>
		<filter-name>miniprofiler</filter-name>
		<filter-class>io.jdev.miniprofiler.servlet.ProfilingFilter</filter-class>
	</filter>

	<filter-mapping>
		<filter-name>miniprofiler</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>

By default, the filter will expect requests for the static resources and data under `/miniprofiler` in your web app.
You can change this by passing it an `init-param` of `path`. Be sure to pass the same value to the `ScriptTagWriter` if you are using that.

The filter can also be configured to accept cross-origin requests by passing an `allowed-origin` `init-param` with the value that you would like the `Access-Control-Allow-Origin` header to be.

Inclusion in Java EE applications with CDI
------------------------------------------
There are a couple of extra pieces of support for modern Java EE applications. Simply include the `miniprofiler-javaee` module:

    groupId: io.jdev.miniprofiler
    artifactId: miniprofiler-javaee
    version: 0.4

That module contains a `DefaultCDIProfilerProvider` which is a `ProfilerProvider` instance ready to be injected into your CDI-managed beans.

It also contains an interceptor for profiling EJB calls. Add the following to your `beans.xml`:

    <interceptors>
        <class>io.jdev.miniprofiler.javaee.ProfilingEJBInterceptor</class>
    </interceptors>

Then add the `@Profiled` annotation to any EJB that you want profiled, and EJB method calls will appear in your profiler output.

Feedback / Contributions
------------------------
Please raise issues in the [GitHub issue tracker][3]

[1]:https://travis-ci.org/tomdcc/miniprofiler-jvm
[2]:http://miniprofiler.com/
[3]:https://github.com/tomdcc/miniprofiler-jvm/issues
