/// <reference path="./node_modules/@types/extjs/index.d.ts">
/// <reference path="./node_modules/@types/microsoft-ajax/index.d.ts">
/// <reference path="./MiniProfiler.Globals.d.ts">
var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
var StackExchange;
(function (StackExchange) {
    var Profiling;
    (function (Profiling) {
        var ClientTiming = /** @class */ (function () {
            function ClientTiming(name, start, duration) {
                this.Name = name;
                this.Start = start;
                this.Duration = duration;
            }
            return ClientTiming;
        }());
        var ColorScheme;
        (function (ColorScheme) {
            ColorScheme["Light"] = "Light";
            ColorScheme["Dark"] = "Dark";
            ColorScheme["Auto"] = "Auto";
        })(ColorScheme || (ColorScheme = {}));
        var RenderMode;
        (function (RenderMode) {
            RenderMode[RenderMode["Corner"] = 0] = "Corner";
            RenderMode[RenderMode["Full"] = 1] = "Full";
        })(RenderMode || (RenderMode = {}));
        var RenderPosition;
        (function (RenderPosition) {
            RenderPosition["Left"] = "Left";
            RenderPosition["Right"] = "Right";
            RenderPosition["BottomLeft"] = "BottomLeft";
            RenderPosition["BottomRight"] = "BottomRight";
        })(RenderPosition || (RenderPosition = {}));
        var ResultRequest = /** @class */ (function () {
            function ResultRequest(id, perfTimings) {
                this.Id = id;
                if (perfTimings && window.performance && window.performance.timing) {
                    var resource_1 = window.performance.timing;
                    var start_1 = resource_1.fetchStart;
                    this.Performance = perfTimings
                        .filter(function (current) { return resource_1[current.name]; })
                        .map(function (current, i) { return ({ item: current, index: i }); })
                        .sort(function (a, b) { return resource_1[a.item.name] - resource_1[b.item.name] || a.index - b.index; })
                        .map(function (x, i, sorted) {
                        var current = x.item;
                        var next = i + 1 < sorted.length ? sorted[i + 1].item : null;
                        return __assign(__assign({}, current), {
                            startTime: resource_1[current.name] - start_1,
                            timeTaken: !next ? 0 : (resource_1[next.name] - resource_1[current.name]),
                        });
                    })
                        .map(function (item, i) { return ({
                        Name: item.name,
                        Start: item.startTime,
                        Duration: item.point ? undefined : item.timeTaken,
                    }); });
                    if (window.performance.navigation) {
                        this.RedirectCount = window.performance.navigation.redirectCount;
                    }
                    if (window.mPt) {
                        var pResults_1 = window.mPt.results();
                        this.Probes = Object.keys(pResults_1).map(function (k) { return pResults_1[k].start && pResults_1[k].end
                            ? {
                                Name: k,
                                Start: pResults_1[k].start - start_1,
                                Duration: pResults_1[k].end - pResults_1[k].start,
                            } : null; }).filter(function (v) { return v; });
                        window.mPt.flush();
                    }
                    if (window.performance.getEntriesByType && window.PerformancePaintTiming) {
                        var entries = window.performance.getEntriesByType('paint');
                        var firstPaint = void 0;
                        var firstContentPaint = void 0;
                        for (var _i = 0, entries_1 = entries; _i < entries_1.length; _i++) {
                            var entry = entries_1[_i];
                            switch (entry.name) {
                                case 'first-paint':
                                    firstPaint = new ClientTiming('firstPaintTime', Math.round(entry.startTime));
                                    this.Performance.push(firstPaint);
                                    break;
                                case 'first-contentful-paint':
                                    firstContentPaint = new ClientTiming('firstContentfulPaintTime', Math.round(entry.startTime));
                                    break;
                            }
                        }
                        if (firstPaint && firstContentPaint && firstContentPaint.Start > firstPaint.Start) {
                            this.Performance.push(firstContentPaint);
                        }
                    }
                    else if (window.chrome && window.chrome.loadTimes) {
                        // fallback to Chrome timings
                        var chromeTimes = window.chrome.loadTimes();
                        if (chromeTimes.firstPaintTime) {
                            this.Performance.push(new ClientTiming('firstPaintTime', Math.round(chromeTimes.firstPaintTime * 1000 - start_1)));
                        }
                        if (chromeTimes.firstPaintAfterLoadTime && chromeTimes.firstPaintAfterLoadTime > chromeTimes.firstPaintTime) {
                            this.Performance.push(new ClientTiming('firstPaintAfterLoadTime', Math.round(chromeTimes.firstPaintAfterLoadTime * 1000 - start_1)));
                        }
                    }
                }
            }
            return ResultRequest;
        }());
        var MiniProfiler = /** @class */ (function () {
            function MiniProfiler() {
                var _this = this;
                this.fetchStatus = {}; // so we never pull down a profiler twice
                this.clientPerfTimings = [
                    // { name: 'navigationStart', description: 'Navigation Start' },
                    // { name: 'unloadEventStart', description: 'Unload Start' },
                    // { name: 'unloadEventEnd', description: 'Unload End' },
                    // { name: 'redirectStart', description: 'Redirect Start' },
                    // { name: 'redirectEnd', description: 'Redirect End' },
                    ({ name: 'fetchStart', description: 'Fetch Start', lineDescription: 'Fetch', point: true }),
                    ({ name: 'domainLookupStart', description: 'Domain Lookup Start', lineDescription: 'DNS Lookup', type: 'dns' }),
                    ({ name: 'domainLookupEnd', description: 'Domain Lookup End', type: 'dns' }),
                    ({ name: 'connectStart', description: 'Connect Start', lineDescription: 'Connect', type: 'connect' }),
                    ({ name: 'secureConnectionStart', description: 'Secure Connection Start', lineDescription: 'SSL/TLS Connect', type: 'ssl' }),
                    ({ name: 'connectEnd', description: 'Connect End', type: 'connect' }),
                    ({ name: 'requestStart', description: 'Request Start', lineDescription: 'Request', type: 'request' }),
                    ({ name: 'responseStart', description: 'Response Start', lineDescription: 'Response', type: 'response' }),
                    ({ name: 'responseEnd', description: 'Response End', type: 'response' }),
                    ({ name: 'domLoading', description: 'DOM Loading', lineDescription: 'DOM Loading', type: 'dom' }),
                    ({ name: 'domInteractive', description: 'DOM Interactive', lineDescription: 'DOM Interactive', type: 'dom', point: true }),
                    ({ name: 'domContentLoadedEventStart', description: 'DOM Content Loaded Event Start', lineDescription: 'DOM Content Loaded', type: 'domcontent' }),
                    ({ name: 'domContentLoadedEventEnd', description: 'DOM Content Loaded Event End', type: 'domcontent' }),
                    ({ name: 'domComplete', description: 'DOM Complete', lineDescription: 'DOM Complete', type: 'dom', point: true }),
                    ({ name: 'loadEventStart', description: 'Load Event Start', lineDescription: 'Load Event', type: 'load' }),
                    ({ name: 'loadEventEnd', description: 'Load Event End', type: 'load' }),
                    ({ name: 'firstPaintTime', description: 'First Paint', lineDescription: 'First Paint', type: 'paint', point: true }),
                    ({ name: 'firstContentfulPaintTime', description: 'First Content Paint', lineDescription: 'First Content Paint', type: 'paint', point: true }),
                ];
                this.savedJson = [];
                this.highlight = function (elem) { return undefined; };
                this.init = function () {
                    var mp = _this;
                    var script = document.getElementById('mini-profiler');
                    var data = script.dataset;
                    var wait = 0;
                    var alreadyDone = false;
                    if (!script || !window.fetch) {
                        return;
                    }
                    var bool = function (arg) { return arg === 'true'; };
                    _this.options = {
                        ids: (data.ids || '').split(','),
                        path: data.path,
                        version: data.version,
                        renderPosition: data.position,
                        colorScheme: data.scheme,
                        decimalPlaces: parseInt(data.decimalPlaces || '2', 10),
                        showTrivial: bool(data.trivial),
                        trivialMilliseconds: parseFloat(data.trivialMilliseconds),
                        showChildrenTime: bool(data.children),
                        maxTracesToShow: parseInt(data.maxTraces, 10),
                        showControls: bool(data.controls),
                        currentId: data.currentId,
                        authorized: bool(data.authorized),
                        toggleShortcut: data.toggleShortcut,
                        startHidden: bool(data.startHidden),
                        ignoredDuplicateExecuteTypes: (data.ignoredDuplicateExecuteTypes || '').split(','),
                        nonce: script.nonce,
                    };
                    function doInit() {
                        var initPopupView = function () {
                            if (mp.options.authorized) {
                                // all fetched profilers will go in here
                                // MiniProfiler.RenderIncludes() sets which corner to render in - default is upper left
                                var container = document.createElement('div');
                                container.className = 'mp-results mp-' + mp.options.renderPosition.toLowerCase() + ' mp-scheme-' + mp.options.colorScheme.toLowerCase();
                                document.body.appendChild(container);
                                mp.container = container;
                                // initialize the controls
                                mp.initControls(mp.container);
                                // fetch and render results
                                mp.fetchResults(mp.options.ids);
                                var lsDisplayValue = void 0;
                                try {
                                    lsDisplayValue = window.localStorage.getItem('MiniProfiler-Display');
                                }
                                catch (e) { }
                                if (lsDisplayValue) {
                                    mp.container.style.display = lsDisplayValue;
                                }
                                else if (mp.options.startHidden) {
                                    mp.container.style.display = 'none';
                                }
                                // if any data came in before the view popped up, render now
                                if (mp.savedJson) {
                                    for (var _i = 0, _a = mp.savedJson; _i < _a.length; _i++) {
                                        var saved = _a[_i];
                                        mp.buttonShow(saved);
                                    }
                                }
                            }
                            else {
                                mp.fetchResults(mp.options.ids);
                            }
                        };
                        // when rendering a shared, full page, this div will exist
                        var fullResults = document.getElementsByClassName('mp-result-full');
                        if (fullResults.length > 0) {
                            mp.container = fullResults[0];
                            // Full page view
                            if (window.location.href.indexOf('&trivial=1') > 0) {
                                mp.options.showTrivial = true;
                            }
                            // profiler will be defined in the full page's head
                            window.profiler.Started = new Date('' + window.profiler.Started); // Ugh, JavaScript
                            var profilerHtml = mp.renderProfiler(window.profiler, false);
                            mp.container.insertAdjacentHTML('beforeend', profilerHtml);
                            // highight
                            mp.container.querySelectorAll('pre code').forEach(function (block) { return mp.highlight(block); });
                            mp.bindDocumentEvents(RenderMode.Full);
                        }
                        else {
                            initPopupView();
                            mp.bindDocumentEvents(RenderMode.Corner);
                        }
                    }
                    function deferInit() {
                        if (!alreadyDone) {
                            if ((mp.initCondition && !mp.initCondition())
                                || (window.performance && window.performance.timing && window.performance.timing.loadEventEnd === 0 && wait < 10000)) {
                                setTimeout(deferInit, 100);
                                wait += 100;
                            }
                            else {
                                alreadyDone = true;
                                if (mp.options.authorized) {
                                    document.head.insertAdjacentHTML('beforeend', "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + mp.options.path + "includes.min.css?v=" + mp.options.version + "\" " + (mp.options.nonce ? "nonce=\"" + mp.options.nonce + "\" " : '') + "/>");
                                }
                                doInit();
                            }
                        }
                    }
                    ;
                    function onLoad() {
                        mp.installAjaxHandlers();
                        deferInit();
                    }
                    if (document.readyState === 'loading') {
                        document.addEventListener('DOMContentLoaded', onLoad);
                    }
                    else {
                        onLoad();
                    }
                    return _this;
                };
                this.listInit = function (options) {
                    var mp = _this;
                    var opt = _this.options = options || {};
                    function updateGrid(id) {
                        var getTiming = function (profiler, name) {
                            return profiler.ClientTimings.Timings.filter(function (t) { return t.Name === name; })[0] || { Name: name, Duration: '', Start: '' };
                        };
                        document.documentElement.classList.add('mp-scheme-' + opt.colorScheme.toLowerCase());
                        fetch(opt.path + 'results-list?last-id=' + id, {
                            method: 'GET',
                            headers: {
                                'Accept': 'application/json',
                                'Content-Type': 'application/json'
                            }
                        })
                            .then(function (data) { return data.json(); })
                            .then(function (data) {
                            var html = '';
                            data.forEach(function (profiler) {
                                html += ("\n<tr>\n  <td><a href=\"" + options.path + "results?id=" + profiler.Id + "\">" + mp.htmlEscape(profiler.Name) + "</a></td>\n  <td>" + mp.htmlEscape(profiler.MachineName) + "</td>\n  <td class=\"mp-results-index-date\">" + profiler.Started + "</td>\n  <td>" + profiler.DurationMilliseconds + "</td>" + (profiler.ClientTimings ? "\n  <td>" + getTiming(profiler, 'requestStart').Start + "</td>\n  <td>" + getTiming(profiler, 'responseStart').Start + "</td>\n  <td>" + getTiming(profiler, 'domComplete').Start + "</td> " : "\n  <td colspan=\"3\" class=\"mp-results-none\">(no client timings)</td>") + "\n</tr>");
                            });
                            document.querySelector('.mp-results-index').insertAdjacentHTML('beforeend', html);
                            var oldId = id;
                            var oldData = data;
                            setTimeout(function () {
                                var newId = oldId;
                                if (oldData.length > 0) {
                                    newId = oldData[oldData.length - 1].Id;
                                }
                                updateGrid(newId);
                            }, 4000);
                        });
                    }
                    updateGrid();
                };
                this.fetchResults = function (ids) {
                    var _loop_1 = function (i) {
                        var id = ids[i];
                        var request = new ResultRequest(id, id === _this.options.currentId ? _this.clientPerfTimings : null);
                        var mp = _this;
                        if (!id || mp.fetchStatus.hasOwnProperty(id)) {
                            return "continue";
                        }
                        var isoDate = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2}(?:\.\d*)?)(?:Z|(\+|-)([\d|:]*))?$/;
                        var parseDates = function (key, value) {
                            return key === 'Started' && typeof value === 'string' && isoDate.exec(value) ? new Date(value) : value;
                        };
                        mp.fetchStatus[id] = 'Starting fetch';
                        fetch(_this.options.path + 'results', {
                            method: 'POST',
                            body: JSON.stringify(request),
                            headers: {
                                'Accept': 'application/json',
                                'Content-Type': 'application/json'
                            }
                        })
                            .then(function (data) { return data.text(); })
                            .then(function (text) { return JSON.parse(text, parseDates); })
                            .then(function (json) {
                            mp.fetchStatus[id] = 'Fetch succeeded';
                            if (json instanceof String) {
                                // hidden
                            }
                            else {
                                mp.buttonShow(json);
                            }
                            mp.fetchStatus[id] = 'Fetch complete';
                        })
                            .catch(function (error) {
                            mp.fetchStatus[id] = 'Fetch complete';
                        });
                    };
                    for (var i = 0; ids && i < ids.length; i++) {
                        _loop_1(i);
                    }
                };
                this.processJson = function (profiler) {
                    var result = __assign({}, profiler);
                    var mp = _this;
                    result.CustomTimingStats = {};
                    result.CustomLinks = result.CustomLinks || {};
                    result.AllCustomTimings = [];
                    function processTiming(timing, parent, depth) {
                        timing.DurationWithoutChildrenMilliseconds = timing.DurationMilliseconds;
                        timing.DurationOfChildrenMilliseconds = 0;
                        timing.Parent = parent;
                        timing.Depth = depth;
                        timing.HasDuplicateCustomTimings = {};
                        timing.HasWarnings = {};
                        for (var _i = 0, _a = timing.Children || []; _i < _a.length; _i++) {
                            var child = _a[_i];
                            processTiming(child, timing, depth + 1);
                            timing.DurationWithoutChildrenMilliseconds -= child.DurationMilliseconds;
                            timing.DurationOfChildrenMilliseconds += child.DurationMilliseconds;
                        }
                        // do this after subtracting child durations
                        if (timing.DurationWithoutChildrenMilliseconds < mp.options.trivialMilliseconds) {
                            timing.IsTrivial = true;
                            result.HasTrivialTimings = true;
                        }
                        function ignoreDuplicateCustomTiming(customTiming) {
                            return customTiming.ExecuteType && mp.options.ignoredDuplicateExecuteTypes.indexOf(customTiming.ExecuteType) > -1;
                        }
                        if (timing.CustomTimings) {
                            timing.CustomTimingStats = {};
                            timing.HasCustomTimings = true;
                            result.HasCustomTimings = true;
                            for (var _b = 0, _c = Object.keys(timing.CustomTimings); _b < _c.length; _b++) {
                                var customType = _c[_b];
                                var customTimings = timing.CustomTimings[customType] || [];
                                var customStat = {
                                    Duration: 0,
                                    Count: 0,
                                };
                                var duplicates = {};
                                for (var _d = 0, customTimings_1 = customTimings; _d < customTimings_1.length; _d++) {
                                    var customTiming = customTimings_1[_d];
                                    // Add to the overall list for the queries view
                                    result.AllCustomTimings.push(customTiming);
                                    customTiming.Parent = timing;
                                    customTiming.CallType = customType;
                                    customStat.Duration += customTiming.DurationMilliseconds;
                                    var ignored = ignoreDuplicateCustomTiming(customTiming);
                                    if (!ignored) {
                                        customStat.Count++;
                                    }
                                    if (customTiming.Errored) {
                                        timing.HasWarnings[customType] = true;
                                        result.HasWarning = true;
                                    }
                                    if (customTiming.CommandString && duplicates[customTiming.CommandString]) {
                                        customTiming.IsDuplicate = true;
                                        timing.HasDuplicateCustomTimings[customType] = true;
                                        result.HasDuplicateCustomTimings = true;
                                    }
                                    else if (!ignored) {
                                        duplicates[customTiming.CommandString] = true;
                                    }
                                }
                                timing.CustomTimingStats[customType] = customStat;
                                if (!result.CustomTimingStats[customType]) {
                                    result.CustomTimingStats[customType] = {
                                        Duration: 0,
                                        Count: 0,
                                    };
                                }
                                result.CustomTimingStats[customType].Duration += customStat.Duration;
                                result.CustomTimingStats[customType].Count += customStat.Count;
                            }
                        }
                        else {
                            timing.CustomTimings = {};
                        }
                    }
                    processTiming(result.Root, null, 0);
                    _this.processCustomTimings(result);
                    return result;
                };
                this.processCustomTimings = function (profiler) {
                    var result = profiler.AllCustomTimings;
                    result.sort(function (a, b) { return a.StartMilliseconds - b.StartMilliseconds; });
                    function removeDuration(list, duration) {
                        var newList = [];
                        for (var _i = 0, list_1 = list; _i < list_1.length; _i++) {
                            var item = list_1[_i];
                            if (duration.start > item.start) {
                                if (duration.start > item.finish) {
                                    newList.push(item);
                                    continue;
                                }
                                newList.push(({ start: item.start, finish: duration.start }));
                            }
                            if (duration.finish < item.finish) {
                                if (duration.finish < item.start) {
                                    newList.push(item);
                                    continue;
                                }
                                newList.push(({ start: duration.finish, finish: item.finish }));
                            }
                        }
                        return newList;
                    }
                    function processTimes(elem) {
                        var duration = ({ start: elem.StartMilliseconds, finish: (elem.StartMilliseconds + elem.DurationMilliseconds) });
                        elem.richTiming = [duration];
                        if (elem.Parent != null) {
                            elem.Parent.richTiming = removeDuration(elem.Parent.richTiming, duration);
                        }
                        for (var _i = 0, _a = elem.Children || []; _i < _a.length; _i++) {
                            var child = _a[_i];
                            processTimes(child);
                        }
                    }
                    processTimes(profiler.Root);
                    // sort results by time
                    result.sort(function (a, b) { return a.StartMilliseconds - b.StartMilliseconds; });
                    function determineOverlap(gap, node) {
                        var overlap = 0;
                        for (var _i = 0, _a = node.richTiming; _i < _a.length; _i++) {
                            var current = _a[_i];
                            if (current.start > gap.finish) {
                                break;
                            }
                            if (current.finish < gap.start) {
                                continue;
                            }
                            overlap += Math.min(gap.finish, current.finish) - Math.max(gap.start, current.start);
                        }
                        return overlap;
                    }
                    function determineGap(gap, node, match) {
                        var overlap = determineOverlap(gap, node);
                        if (match == null || overlap > match.duration) {
                            match = { name: node.Name, duration: overlap };
                        }
                        else if (match.name === node.Name) {
                            match.duration += overlap;
                        }
                        for (var _i = 0, _a = node.Children || []; _i < _a.length; _i++) {
                            var child = _a[_i];
                            match = determineGap(gap, child, match);
                        }
                        return match;
                    }
                    var time = 0;
                    result.forEach(function (elem) {
                        elem.PrevGap = {
                            duration: (elem.StartMilliseconds - time).toFixed(2),
                            start: time,
                            finish: elem.StartMilliseconds,
                        };
                        elem.PrevGap.Reason = determineGap(elem.PrevGap, profiler.Root, null);
                        time = elem.StartMilliseconds + elem.DurationMilliseconds;
                    });
                    if (result.length > 0) {
                        var me = result[result.length - 1];
                        me.NextGap = {
                            duration: (profiler.Root.DurationMilliseconds - time).toFixed(2),
                            start: time,
                            finish: profiler.Root.DurationMilliseconds,
                        };
                        me.NextGap.Reason = determineGap(me.NextGap, profiler.Root, null);
                    }
                    return result;
                };
                this.htmlEscape = function (orig) { return (orig || '')
                    .replace(/&/g, '&amp;')
                    .replace(/</g, '&lt;')
                    .replace(/>/g, '&gt;')
                    .replace(/"/g, '&quot;')
                    .replace(/'/g, '&#039;'); };
                this.renderProfiler = function (json, isNew) {
                    var p = _this.processJson(json);
                    var mp = _this;
                    var encode = _this.htmlEscape;
                    var duration = function (milliseconds, decimalPlaces) {
                        if (milliseconds === undefined) {
                            return '';
                        }
                        return (milliseconds || 0).toFixed(decimalPlaces === undefined ? _this.options.decimalPlaces : decimalPlaces);
                    };
                    var renderDebugInfo = function (timing) {
                        if (timing.DebugInfo) {
                            var customTimings_2 = (p.CustomTimingStats ? Object.keys(p.CustomTimingStats) : []).map(function (tk) { return timing.CustomTimings[tk] ? "\n                <div class=\"mp-nested-timing\">\n                    <span class=\"mp-duration\">" + timing.CustomTimingStats[tk].Count + "</span> " + encode(tk) + " call" + (timing.CustomTimingStats[tk].Count == 1 ? '' : 's') + " \n                    totalling <span class=\"mp-duration\">" + duration(timing.CustomTimingStats[tk].Duration) + "</span> <span class=\"mp-unit\">ms</span>\n                    " + ((timing.HasDuplicateCustomTimings[tk] || timing.HasWarnings[tk]) ? '<span class="mp-warning">(duplicates deletected)</span>' : '') + "\n                </div>" : ''; }).join('');
                            return "\n          <div class=\"mp-debug-tooltip\">\n            <div class=\"mp-name\">Detailed info for " + encode(timing.Name) + "</div>\n            <div>Starts at: <span class=\"mp-duration\">" + duration(timing.StartMilliseconds) + "</span> <span class=\"mp-unit\">ms</span></div>\n            <div>\n                Overall duration (with children): <span class=\"mp-duration\">" + duration(timing.DurationMilliseconds) + "</span> <span class=\"mp-unit\">ms</span>\n                <div class=\"mp-nested-timing\">\n                  Self duration: <span class=\"mp-duration\">" + duration(timing.DurationWithoutChildrenMilliseconds) + "</span> <span class=\"mp-unit\">ms</span>\n                  " + customTimings_2 + "\n                </div>\n                <div class=\"mp-nested-timing\">\n                  Children (" + (timing.Children ? timing.Children.length : '0') + ") duration: <span class=\"mp-duration\">" + duration(timing.DurationOfChildrenMilliseconds) + "</span> <span class=\"mp-unit\">ms</span>\n                </div>\n            </div>\n            <div>Stack:</div>\n            <pre class=\"mp-stack-trace\">" + timing.DebugInfo.RichHtmlStack + "</pre>\n          </div>\n          <span title=\"Debug Info\">\uD83D\uDD0D</span>";
                        }
                        return '';
                    };
                    var renderTiming = function (timing) {
                        var customTimingTypes = p.CustomTimingStats ? Object.keys(p.CustomTimingStats) : [];
                        var str = "\n  <tr class=\"" + (timing.IsTrivial ? 'mp-trivial' : '') + (timing.DebugInfo ? ' mp-debug' : '') + "\" data-timing-id=\"" + timing.Id + "\">\n    <td>" + renderDebugInfo(timing) + "</td>\n    <td class=\"mp-label\" title=\"" + encode(timing.Name) + "\"" + (timing.Depth > 0 ? " style=\"padding-left:" + timing.Depth * 11 + "px;\"" : '') + ">\n      " + encode(timing.Name) + "\n    </td>\n    <td class=\"mp-duration\" title=\"duration of this step without any children's durations\">\n      " + duration(timing.DurationWithoutChildrenMilliseconds) + "\n    </td>\n    <td class=\"mp-duration mp-more-columns\" title=\"duration of this step and its children\">\n      " + duration(timing.DurationMilliseconds) + "\n    </td>\n    <td class=\"mp-duration mp-more-columns time-from-start\" title=\"time elapsed since profiling started\">\n      <span class=\"mp-unit\">+</span>" + duration(timing.StartMilliseconds) + "\n    </td>\n    " + customTimingTypes.map(function (tk) { return timing.CustomTimings[tk] ? "\n    <td class=\"mp-duration\">\n      <a class=\"mp-queries-show" + (timing.HasWarnings[tk] ? ' mp-queries-warning' : '') + "\" title=\"" + duration(timing.CustomTimingStats[tk].Duration) + " ms in " + timing.CustomTimingStats[tk].Count + " " + encode(tk) + " call(s)" + (timing.HasDuplicateCustomTimings[tk] ? '; duplicate calls detected!' : '') + "\">\n        " + duration(timing.CustomTimingStats[tk].Duration) + "\n        (" + timing.CustomTimingStats[tk].Count + ((timing.HasDuplicateCustomTimings[tk] || timing.HasWarnings[tk]) ? '<span class="mp-warning">!</span>' : '') + ")\n      </a>\n    </td>" : '<td></td>'; }).join('') + "\n  </tr>";
                        // Append children
                        if (timing.Children) {
                            timing.Children.forEach(function (ct) { return str += renderTiming(ct); });
                        }
                        return str;
                    };
                    var timingsTable = "\n        <table class=\"mp-timings\">\n          <thead>\n            <tr>\n              <th colspan=\"2\"></th>\n              <th>duration (ms)</th>\n              <th class=\"mp-more-columns\">with children (ms)</th>\n              <th class=\"time-from-start mp-more-columns\">from start (ms)</th>\n              " + Object.keys(p.CustomTimingStats).map(function (k) { return "<th title=\"call count\">" + encode(k) + " (ms)</th>"; }).join('') + "\n            </tr>\n          </thead>\n          <tbody>\n            " + renderTiming(p.Root) + "\n          </tbody>\n          <tfoot>\n            <tr>\n              <td colspan=\"3\"></td>\n              <td class=\"mp-more-columns\" colspan=\"2\"></td>\n            </tr>\n          </tfoot>\n        </table>";
                    var customTimings = function () {
                        if (!p.HasCustomTimings) {
                            return '';
                        }
                        return "\n        <table class=\"mp-custom-timing-overview\">\n            " + Object.getOwnPropertyNames(p.CustomTimingStats).map(function (key) { return "\n          <tr title=\"" + p.CustomTimingStats[key].Count + " " + encode(key.toLowerCase()) + " calls spent " + duration(p.CustomTimingStats[key].Duration) + " ms of total request time\">\n            <td class=\"mp-number\">\n              " + encode(key) + ":\n            </td>\n            <td class=\"mp-number\">\n              " + duration(p.CustomTimingStats[key].Duration / p.DurationMilliseconds * 100) + " <span class=\"mp-unit\">%</span>\n            </td>\n          </tr>"; }).join('') + "\n        </table>";
                    };
                    var clientTimings = function () {
                        if (!p.ClientTimings) {
                            return '';
                        }
                        var end = 0;
                        var list = p.ClientTimings.Timings.map(function (t) {
                            var results = mp.clientPerfTimings ? mp.clientPerfTimings.filter(function (pt) { return pt.name === t.Name; }) : [];
                            var info = results.length > 0 ? results[0] : null;
                            end = Math.max(end, t.Start + t.Duration);
                            return {
                                isTrivial: t.Start === 0 || t.Duration < 2,
                                name: info && info.lineDescription || t.Name,
                                duration: info && info.point ? undefined : t.Duration,
                                type: info && info.type || 'unknown',
                                point: info && info.point,
                                start: t.Start,
                                left: null,
                                width: null,
                            };
                        });
                        p.HasTrivialTimings = p.HasTrivialTimings || list.some(function (t) { return t.isTrivial; });
                        list.sort(function (a, b) { return a.start - b.start; });
                        list.forEach(function (l) {
                            var percent = (100 * l.start / end) + '%';
                            l.left = l.point ? "calc(" + percent + " - 2px)" : percent;
                            l.width = l.point ? '4px' : (100 * l.duration / end + '%');
                        });
                        return "\n        <table class=\"mp-timings mp-client-timings\">\n          <thead>\n            <tr>\n              <th style=\"text-align:left\">client event</th>\n              <th></th>\n              <th>duration (ms)</th>\n              <th class=\"mp-more-columns\">from start (ms)</th>\n            </tr>\n          </thead>\n          <tbody>\n            " + list.map(function (t) { return "\n            <tr class=\"" + (t.isTrivial ? 'mp-trivial' : '') + "\">\n              <td class=\"mp-label\">" + encode(t.name) + "</td>\n              <td class=\"t-" + t.type + (t.point ? ' t-point' : '') + "\"><div style=\"margin-left: " + t.left + "; width: " + t.width + ";\"></div></td>\n              <td class=\"mp-duration\">\n                " + (t.duration >= 0 ? "<span class=\"mp-unit\"></span>" + duration(t.duration, 0) : '') + "\n              </td>\n              <td class=\"mp-duration time-from-start mp-more-columns\">\n                <span class=\"mp-unit\">+</span>" + duration(t.start, 0) + "\n              </td>\n            </tr>"; }).join('') + "\n          </tbody>\n        </table>";
                    };
                    var profilerQueries = function () {
                        if (!p.HasCustomTimings) {
                            return '';
                        }
                        var renderGap = function (gap) { return gap && gap.Reason.duration > 0.02 ? "\n  <tr class=\"mp-gap-info " + (gap.Reason.duration < 4 ? 'mp-trivial-gap' : '') + "\">\n    <td class=\"mp-info\">\n      " + gap.duration + " <span class=\"mp-unit\">ms</span>\n    </td>\n    <td class=\"query\">\n      <div>" + encode(gap.Reason.name) + " &mdash; " + gap.Reason.duration.toFixed(2) + " <span class=\"mp-unit\">ms</span></div>\n    </td>\n  </tr>" : ''; };
                        return "\n    <div class=\"mp-queries\">\n      <table>\n        <thead>\n          <tr>\n            <th>\n              <div class=\"mp-call-type\">Call Type</div>\n              <div>Step</div>\n              <div>Duration <span class=\"mp-unit\">(from start)</span></div>\n            </th>\n            <th>\n              <div class=\"mp-stack-trace\">Call Stack</div>\n              <div>Command</div>\n            </th>\n          </tr>\n        </thead>\n        <tbody>\n          " + p.AllCustomTimings.map(function (ct, index) { return "\n            " + renderGap(ct.PrevGap) + "\n            <tr class=\"" + (index % 2 === 1 ? 'mp-odd' : '') + "\" data-timing-id=\"" + ct.Parent.Id + "\">\n              <td>\n                <div class=\"mp-call-type" + (ct.Errored ? ' mp-warning' : '') + "\">" + encode(ct.CallType) + encode(!ct.ExecuteType || ct.CallType === ct.ExecuteType ? '' : ' - ' + ct.ExecuteType) + ((ct.IsDuplicate || ct.Errored) ? ' <span class="mp-warning" title="Duplicate">!</span>' : '') + "</div>\n                <div>" + encode(ct.Parent.Name) + "</div>\n                <div class=\"mp-number\">\n                  " + duration(ct.DurationMilliseconds) + " <span class=\"mp-unit\">ms (T+" + duration(ct.StartMilliseconds) + " ms)</span>\n                </div>\n                " + (ct.FirstFetchDurationMilliseconds ? "<div>First Result: " + duration(ct.FirstFetchDurationMilliseconds) + " <span class=\"mp-unit\">ms</span></div>" : '') + "\n              </td>\n              <td>\n                <div class=\"query\">\n                  <div class=\"mp-stack-trace\">" + encode(ct.StackTraceSnippet) + "</div>\n                  <pre><code>" + encode(ct.CommandString) + "</code></pre>\n                </div>\n              </td>\n            </tr>\n            " + renderGap(ct.NextGap); }).join('') + "\n        </tbody>\n      </table>\n      <p class=\"mp-trivial-gap-container\">\n        <a class=\"mp-toggle-trivial-gaps\" href=\"#\">toggle trivial gaps</a>\n      </p>\n    </div>";
                    };
                    return "\n  <div class=\"mp-result" + (_this.options.showTrivial ? ' show-trivial' : '') + (_this.options.showChildrenTime ? ' show-columns' : '') + (isNew ? ' new' : '') + "\">\n    <div class=\"mp-button" + (p.HasWarning ? ' mp-button-warning' : '') + "\" title=\"" + encode(p.Name) + "\">\n      <span class=\"mp-number\">" + duration(p.DurationMilliseconds) + " <span class=\"mp-unit\">ms</span></span>\n      " + ((p.HasDuplicateCustomTimings || p.HasWarning) ? '<span class="mp-warning">!</span>' : '') + "\n    </div>\n    <div class=\"mp-popup\">\n      <div class=\"mp-info\">\n        <div>\n          <div class=\"mp-name\">" + encode(p.Name) + "</div>\n          <div class=\"mp-machine-name\">" + encode(p.MachineName) + "</div>\n        </div>\n        <div>\n          <div class=\"mp-overall-duration\">(" + duration(p.DurationMilliseconds) + " ms)</div>\n          <div class=\"mp-started\">" + (p.Started ? p.Started.toUTCString() : '') + "</div>\n        </div>\n      </div>\n      <div class=\"mp-output\">\n        " + timingsTable + "\n\t\t" + customTimings() + "\n        " + clientTimings() + "\n        <div class=\"mp-links\">\n          <a href=\"" + _this.options.path + "results?id=" + p.Id + "\" class=\"mp-share-mp-results\" target=\"_blank\">share</a>\n          " + Object.keys(p.CustomLinks).map(function (k) { return "<a href=\"" + p.CustomLinks[k] + "\" class=\"mp-custom-link\" target=\"_blank\">" + k + "</a>"; }).join('') + "\n  \t\t  <span>\n            <a class=\"mp-toggle-columns\" title=\"shows additional columns\">more columns</a>\n            <a class=\"mp-toggle-columns mp-more-columns\" title=\"hides additional columns\">fewer columns</a>\n            " + (p.HasTrivialTimings ? "\n            <a class=\"mp-toggle-trivial\" title=\"shows any rows with &lt; " + _this.options.trivialMilliseconds + " ms duration\">show trivial</a>\n            <a class=\"mp-toggle-trivial mp-trivial\" title=\"hides any rows with &lt; " + _this.options.trivialMilliseconds + " ms duration\">hide trivial</a>" : '') + "\n          </span>\n        </div>\n      </div>\n    </div>\n    " + profilerQueries() + "\n  </div>";
                };
                this.buttonShow = function (json) {
                    if (!_this.container) {
                        // container not rendered yet
                        _this.savedJson.push(json);
                        return;
                    }
                    var profilerHtml = _this.renderProfiler(json, true);
                    if (_this.controls) {
                        _this.controls.insertAdjacentHTML('beforebegin', profilerHtml);
                    }
                    else {
                        _this.container.insertAdjacentHTML('beforeend', profilerHtml);
                    }
                    // limit count to maxTracesToShow, remove those before it
                    var results = _this.container.querySelectorAll('.mp-result');
                    var toRemove = results.length - _this.options.maxTracesToShow;
                    for (var i = 0; i < toRemove; i++) {
                        results[i].parentNode.removeChild(results[i]);
                    }
                };
                this.scrollToQuery = function (link, queries) {
                    var id = link.closest('tr').dataset['timingId'];
                    var rows = queries.querySelectorAll('tr[data-timing-id="' + id + '"]');
                    rows.forEach(function (n) { return n.classList.add('highlight'); });
                    if (rows && rows[0]) {
                        rows[0].scrollIntoView();
                    }
                };
                // some elements want to be hidden on certain doc events
                this.bindDocumentEvents = function (mode) {
                    var mp = _this;
                    // Common handlers
                    document.addEventListener('click', function (event) {
                        var target = event.target;
                        if (target.matches('.mp-toggle-trivial')) {
                            target.closest('.mp-result').classList.toggle('show-trivial');
                        }
                        if (target.matches('.mp-toggle-columns')) {
                            target.closest('.mp-result').classList.toggle('show-columns');
                        }
                        if (target.matches('.mp-toggle-trivial-gaps')) {
                            target.closest('.mp-queries').classList.toggle('show-trivial');
                        }
                    }, false);
                    // Full vs. Corner handlers
                    if (mode === RenderMode.Full) {
                        // since queries are already shown, just highlight and scroll when clicking a '1 sql' link
                        document.addEventListener('click', function (event) {
                            var target = event.target;
                            var queriesButton = target.closest('.mp-popup .mp-queries-show');
                            if (queriesButton) {
                                mp.scrollToQuery(queriesButton, document.body.querySelector('.mp-queries'));
                            }
                        });
                        document.documentElement.classList.add('mp-scheme-' + mp.options.colorScheme.toLowerCase());
                    }
                    else {
                        document.addEventListener('click', function (event) {
                            var target = event.target;
                            var button = target.closest('.mp-button');
                            if (button) {
                                var popup = button.parentElement.querySelector('.mp-popup');
                                var wasActive = button.parentElement.classList.contains('active');
                                var pos = mp.options.renderPosition;
                                var parent_1 = button.parentElement;
                                parent_1.classList.remove('new');
                                var allChildren = button.parentElement.parentElement.children;
                                for (var i = 0; i < allChildren.length; i++) {
                                    // Set Active only on the curent button
                                    allChildren[i].classList.toggle('active', allChildren[i] == parent_1);
                                }
                                if (!wasActive) {
                                    // move left or right, based on config
                                    popup.style[pos === RenderPosition.Left || pos === RenderPosition.BottomLeft ? 'left' : 'right'] = (button.offsetWidth - 1) + "px";
                                    // is this rendering on the bottom (if no, then is top by default)
                                    if (pos === RenderPosition.BottomLeft || pos === RenderPosition.BottomRight) {
                                        var bottom = window.innerHeight - button.getBoundingClientRect().top - button.offsetHeight + window.scrollY; // get bottom of button
                                        popup.style.bottom = '0';
                                        popup.style.maxHeight = 'calc(100vh - ' + (bottom + 25) + 'px)';
                                    }
                                    else {
                                        popup.style.top = '0';
                                        popup.style.maxHeight = 'calc(100vh - ' + (button.getBoundingClientRect().top - window.window.scrollY + 25) + 'px)';
                                    }
                                }
                                return;
                            }
                            var queriesButton = target.closest('.mp-queries-show');
                            if (queriesButton) {
                                // opaque background
                                document.body.insertAdjacentHTML('beforeend', '<div class="mp-overlay"><div class="mp-overlay-bg"/></div>');
                                var overlay = document.querySelector('.mp-overlay');
                                var queriesOrig = queriesButton.closest('.mp-result').querySelector('.mp-queries');
                                var queries = queriesOrig.cloneNode(true);
                                queries.style.display = 'block';
                                overlay.classList.add('mp-scheme-' + mp.options.colorScheme.toLowerCase());
                                overlay.appendChild(queries);
                                mp.scrollToQuery(queriesButton, queries);
                                // syntax highlighting
                                queries.querySelectorAll('pre code').forEach(function (block) { return mp.highlight(block); });
                                return;
                            }
                        });
                        // Background and esc binding to close popups
                        var tryCloseActive = function (event) {
                            var target = event.target;
                            var active = document.querySelector('.mp-result.active');
                            if (!active)
                                return;
                            var bg = document.querySelector('.mp-overlay');
                            var isEscPress = event.type === 'keyup' && event.which === 27;
                            var isBgClick = event.type === 'click' && !target.closest('.mp-queries, .mp-results');
                            if (isEscPress || isBgClick) {
                                if (bg && bg.offsetParent !== null) {
                                    bg.remove();
                                }
                                else {
                                    active.classList.remove('active');
                                }
                            }
                        };
                        document.addEventListener('click', tryCloseActive);
                        document.addEventListener('keyup', tryCloseActive);
                        if (mp.options.toggleShortcut && !mp.options.toggleShortcut.match(/^None$/i)) {
                            /**
                             * Based on http://www.openjs.com/scripts/events/keyboard_shortcuts/
                             * Version : 2.01.B
                             * By Binny V A
                             * License : BSD
                             */
                            var keys_1 = mp.options.toggleShortcut.toLowerCase().split("+");
                            document.addEventListener('keydown', function (e) {
                                var element = e.target;
                                if (element.nodeType == 3)
                                    element = element.parentElement;
                                if (element.tagName == 'INPUT' || element.tagName == 'TEXTAREA')
                                    return;
                                //Find Which key is pressed
                                var code;
                                if (e.keyCode)
                                    code = e.keyCode;
                                else if (e.which)
                                    code = e.which;
                                var character = String.fromCharCode(code).toLowerCase();
                                if (code == 188)
                                    character = ","; //If the user presses , when the type is onkeydown
                                if (code == 190)
                                    character = "."; //If the user presses , when the type is onkeydown
                                //Key Pressed - counts the number of valid keypresses - if it is same as the number of keys, the shortcut function is invoked
                                var kp = 0;
                                //Work around for stupid Shift key bug created by using lowercase - as a result the shift+num combination was broken
                                var shift_nums = {
                                    "`": "~",
                                    "1": "!",
                                    "2": "@",
                                    "3": "#",
                                    "4": "$",
                                    "5": "%",
                                    "6": "^",
                                    "7": "&",
                                    "8": "*",
                                    "9": "(",
                                    "0": ")",
                                    "-": "_",
                                    "=": "+",
                                    ";": ":",
                                    "'": "\"",
                                    ",": "<",
                                    ".": ">",
                                    "/": "?",
                                    "\\": "|"
                                };
                                //Special Keys - and their codes
                                var special_keys = {
                                    'esc': 27,
                                    'escape': 27,
                                    'tab': 9,
                                    'space': 32,
                                    'return': 13,
                                    'enter': 13,
                                    'backspace': 8,
                                    'scrolllock': 145,
                                    'scroll_lock': 145,
                                    'scroll': 145,
                                    'capslock': 20,
                                    'caps_lock': 20,
                                    'caps': 20,
                                    'numlock': 144,
                                    'num_lock': 144,
                                    'num': 144,
                                    'pause': 19,
                                    'break': 19,
                                    'insert': 45,
                                    'home': 36,
                                    'delete': 46,
                                    'end': 35,
                                    'pageup': 33,
                                    'page_up': 33,
                                    'pu': 33,
                                    'pagedown': 34,
                                    'page_down': 34,
                                    'pd': 34,
                                    'left': 37,
                                    'up': 38,
                                    'right': 39,
                                    'down': 40,
                                    'f1': 112,
                                    'f2': 113,
                                    'f3': 114,
                                    'f4': 115,
                                    'f5': 116,
                                    'f6': 117,
                                    'f7': 118,
                                    'f8': 119,
                                    'f9': 120,
                                    'f10': 121,
                                    'f11': 122,
                                    'f12': 123
                                };
                                var modifiers = {
                                    shift: { wanted: false },
                                    ctrl: { wanted: false },
                                    alt: { wanted: false }
                                };
                                for (var i = 0; i < keys_1.length; i++) {
                                    var k = keys_1[i];
                                    if (k == 'ctrl' || k == 'control') {
                                        kp++;
                                        modifiers.ctrl.wanted = true;
                                    }
                                    else if (k == 'shift') {
                                        kp++;
                                        modifiers.shift.wanted = true;
                                    }
                                    else if (k == 'alt') {
                                        kp++;
                                        modifiers.alt.wanted = true;
                                    }
                                    else if (k.length > 1) { //If it is a special key
                                        if (special_keys[k] == code)
                                            kp++;
                                    }
                                    else { //The special keys did not match
                                        if (character == k)
                                            kp++;
                                        else if (shift_nums[character] && e.shiftKey) { //Stupid Shift key bug created by using lowercase
                                            character = shift_nums[character];
                                            if (character == k)
                                                kp++;
                                        }
                                    }
                                }
                                if (kp == keys_1.length
                                    && e.ctrlKey == modifiers.ctrl.wanted
                                    && e.shiftKey == modifiers.shift.wanted
                                    && e.altKey == modifiers.alt.wanted) {
                                    var results = document.querySelector('.mp-results');
                                    var newValue = results.style.display == 'none' ? 'block' : 'none';
                                    results.style.display = newValue;
                                    try {
                                        window.localStorage.setItem('MiniProfiler-Display', newValue);
                                    }
                                    catch (e) { }
                                }
                            }, false);
                        }
                    }
                };
                this.initControls = function (container) {
                    if (_this.options.showControls) {
                        container.insertAdjacentHTML('beforeend', '<div class="mp-controls"><span class="mp-min-max" title="Minimize">m</span><span class="mp-clear" title="Clear">c</span></div>');
                        _this.controls = container.querySelector('.mp-controls');
                        var minMax = container.querySelector('.mp-controls .mp-min-max');
                        minMax.addEventListener('click', function () {
                            container.classList.toggle('mp-min');
                        });
                        var clear = container.querySelector('.mp-controls .mp-clear');
                        clear.addEventListener('click', function () {
                            var results = container.querySelectorAll('.mp-result');
                            results.forEach(function (item) {
                                item.parentNode.removeChild(item);
                            });
                        });
                    }
                    else {
                        container.classList.add('mp-no-controls');
                    }
                };
                this.installAjaxHandlers = function () {
                    var mp = _this;
                    function handleIds(jsonIds) {
                        if (jsonIds) {
                            var ids = JSON.parse(jsonIds);
                            mp.fetchResults(ids);
                        }
                    }
                    function handleXHR(xhr) {
                        // iframed file uploads don't have headers
                        if (xhr && xhr.getResponseHeader) {
                            // should be an array of strings, e.g. ["008c4813-9bd7-443d-9376-9441ec4d6a8c","16ff377b-8b9c-4c20-a7b5-97cd9fa7eea7"]
                            handleIds(xhr.getResponseHeader('X-MiniProfiler-Ids'));
                        }
                    }
                    // we need to attach our AJAX complete handler to the window's (profiled app's) copy, not our internal, no conflict version
                    var windowjQuery = window.jQuery;
                    // fetch profile results for any AJAX calls
                    if (windowjQuery && windowjQuery(document) && windowjQuery(document).ajaxComplete) {
                        windowjQuery(document).ajaxComplete(function (_e, xhr, _settings) { return handleXHR(xhr); });
                    }
                    // fetch results after ASP Ajax calls
                    if (typeof (Sys) !== 'undefined' && typeof (Sys.WebForms) !== 'undefined' && typeof (Sys.WebForms.PageRequestManager) !== 'undefined') {
                        Sys.WebForms.PageRequestManager.getInstance().add_endRequest(function (sender, args) {
                            if (args) {
                                var response = args.get_response(); // Trust me, it's there.
                                if (response.get_responseAvailable() && response._xmlHttpRequest != null) {
                                    handleXHR(response);
                                }
                            }
                        });
                    }
                    if (typeof (Sys) !== 'undefined' && typeof (Sys.Net) !== 'undefined' && typeof (Sys.Net.WebRequestManager) !== 'undefined') {
                        Sys.Net.WebRequestManager.add_completedRequest(function (sender, args) {
                            if (sender) {
                                var webRequestExecutor = sender;
                                if (webRequestExecutor.get_responseAvailable()) {
                                    handleXHR(webRequestExecutor);
                                }
                            }
                        });
                    }
                    // more Asp.Net callbacks
                    if (typeof (window.WebForm_ExecuteCallback) === 'function') {
                        window.WebForm_ExecuteCallback = (function (callbackObject) {
                            // Store original function
                            var original = window.WebForm_ExecuteCallback;
                            return function (callbackObjectInner) {
                                original(callbackObjectInner);
                                handleXHR(callbackObjectInner.xmlRequest);
                            };
                        })(null);
                    }
                    // also fetch results after ExtJS requests, in case it is being used
                    if (typeof (Ext) !== 'undefined' && typeof (Ext.Ajax) !== 'undefined' && typeof (Ext.Ajax.on) !== 'undefined') {
                        // Ext.Ajax is a singleton, so we just have to attach to its 'requestcomplete' event
                        Ext.Ajax.on('requestcomplete', function (e, xhr, settings) { return handleXHR(xhr); });
                    }
                    if (typeof (MooTools) !== 'undefined' && typeof (Request) !== 'undefined') {
                        Request.prototype.addEvents({
                            onComplete: function () {
                                handleXHR(this.xhr);
                            },
                        });
                    }
                    // add support for AngularJS, which uses the basic XMLHttpRequest object.
                    if ((window.angular || window.axios || window.xhr) && typeof (XMLHttpRequest) !== 'undefined') {
                        var oldSend_1 = XMLHttpRequest.prototype.send;
                        XMLHttpRequest.prototype.send = function sendReplacement(data) {
                            if (this.onreadystatechange) {
                                if (typeof (this.miniprofiler) === 'undefined' || typeof (this.miniprofiler.prev_onreadystatechange) === 'undefined') {
                                    this.miniprofiler = { prev_onreadystatechange: this.onreadystatechange };
                                    this.onreadystatechange = function onReadyStateChangeReplacement() {
                                        if (this.readyState === 4) {
                                            handleXHR(this);
                                        }
                                        if (this.miniprofiler.prev_onreadystatechange != null) {
                                            return this.miniprofiler.prev_onreadystatechange.apply(this, arguments);
                                        }
                                    };
                                }
                            }
                            else if (this.onload) {
                                if (typeof (this.miniprofiler) === 'undefined' || typeof (this.miniprofiler.prev_onload) === 'undefined') {
                                    this.miniprofiler = { prev_onload: this.onload };
                                    this.onload = function onLoadReplacement() {
                                        handleXHR(this);
                                        if (this.miniprofiler.prev_onload != null) {
                                            return this.miniprofiler.prev_onload.apply(this, arguments);
                                        }
                                    };
                                }
                            }
                            return oldSend_1.apply(this, arguments);
                        };
                    }
                    // wrap fetch
                    if (window.fetch) {
                        var windowFetch_1 = window.fetch;
                        window.fetch = function (input, init) {
                            return windowFetch_1(input, init).then(function (response) {
                                handleIds(response.headers.get('X-MiniProfiler-Ids'));
                                return response;
                            });
                        };
                    }
                };
            }
            return MiniProfiler;
        }());
        Profiling.MiniProfiler = MiniProfiler;
    })(Profiling = StackExchange.Profiling || (StackExchange.Profiling = {}));
})(StackExchange || (StackExchange = {}));
window.MiniProfiler = new StackExchange.Profiling.MiniProfiler().init();
//# sourceMappingURL=MiniProfiler.js.map
(function () {
    // save previous, if any
    var oldHljs = window.hljs;

/*! highlight.js v9.12.0 | BSD3 License | git.io/hljslicense */
    !function (e) { var n = "object" == typeof window && window || "object" == typeof self && self; "undefined" != typeof exports ? e(exports) : n && (n.hljs = e({}), "function" == typeof define && define.amd && define([], function () { return n.hljs })) }(function (e) { function n(e) { return e.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;") } function t(e) { return e.nodeName.toLowerCase() } function r(e, n) { var t = e && e.exec(n); return t && 0 === t.index } function a(e) { return k.test(e) } function i(e) { var n, t, r, i, o = e.className + " "; if (o += e.parentNode ? e.parentNode.className : "", t = B.exec(o)) return w(t[1]) ? t[1] : "no-highlight"; for (o = o.split(/\s+/), n = 0, r = o.length; r > n; n++)if (i = o[n], a(i) || w(i)) return i } function o(e) { var n, t = {}, r = Array.prototype.slice.call(arguments, 1); for (n in e) t[n] = e[n]; return r.forEach(function (e) { for (n in e) t[n] = e[n] }), t } function u(e) { var n = []; return function r(e, a) { for (var i = e.firstChild; i; i = i.nextSibling)3 === i.nodeType ? a += i.nodeValue.length : 1 === i.nodeType && (n.push({ event: "start", offset: a, node: i }), a = r(i, a), t(i).match(/br|hr|img|input/) || n.push({ event: "stop", offset: a, node: i })); return a }(e, 0), n } function c(e, r, a) { function i() { return e.length && r.length ? e[0].offset !== r[0].offset ? e[0].offset < r[0].offset ? e : r : "start" === r[0].event ? e : r : e.length ? e : r } function o(e) { function r(e) { return " " + e.nodeName + '="' + n(e.value).replace('"', "&quot;") + '"' } s += "<" + t(e) + E.map.call(e.attributes, r).join("") + ">" } function u(e) { s += "</" + t(e) + ">" } function c(e) { ("start" === e.event ? o : u)(e.node) } for (var l = 0, s = "", f = []; e.length || r.length;) { var g = i(); if (s += n(a.substring(l, g[0].offset)), l = g[0].offset, g === e) { f.reverse().forEach(u); do c(g.splice(0, 1)[0]), g = i(); while (g === e && g.length && g[0].offset === l); f.reverse().forEach(o) } else "start" === g[0].event ? f.push(g[0].node) : f.pop(), c(g.splice(0, 1)[0]) } return s + n(a.substr(l)) } function l(e) { return e.v && !e.cached_variants && (e.cached_variants = e.v.map(function (n) { return o(e, { v: null }, n) })), e.cached_variants || e.eW && [o(e)] || [e] } function s(e) { function n(e) { return e && e.source || e } function t(t, r) { return new RegExp(n(t), "m" + (e.cI ? "i" : "") + (r ? "g" : "")) } function r(a, i) { if (!a.compiled) { if (a.compiled = !0, a.k = a.k || a.bK, a.k) { var o = {}, u = function (n, t) { e.cI && (t = t.toLowerCase()), t.split(" ").forEach(function (e) { var t = e.split("|"); o[t[0]] = [n, t[1] ? Number(t[1]) : 1] }) }; "string" == typeof a.k ? u("keyword", a.k) : x(a.k).forEach(function (e) { u(e, a.k[e]) }), a.k = o } a.lR = t(a.l || /\w+/, !0), i && (a.bK && (a.b = "\\b(" + a.bK.split(" ").join("|") + ")\\b"), a.b || (a.b = /\B|\b/), a.bR = t(a.b), a.e || a.eW || (a.e = /\B|\b/), a.e && (a.eR = t(a.e)), a.tE = n(a.e) || "", a.eW && i.tE && (a.tE += (a.e ? "|" : "") + i.tE)), a.i && (a.iR = t(a.i)), null == a.r && (a.r = 1), a.c || (a.c = []), a.c = Array.prototype.concat.apply([], a.c.map(function (e) { return l("self" === e ? a : e) })), a.c.forEach(function (e) { r(e, a) }), a.starts && r(a.starts, i); var c = a.c.map(function (e) { return e.bK ? "\\.?(" + e.b + ")\\.?" : e.b }).concat([a.tE, a.i]).map(n).filter(Boolean); a.t = c.length ? t(c.join("|"), !0) : { exec: function () { return null } } } } r(e) } function f(e, t, a, i) { function o(e, n) { var t, a; for (t = 0, a = n.c.length; a > t; t++)if (r(n.c[t].bR, e)) return n.c[t] } function u(e, n) { if (r(e.eR, n)) { for (; e.endsParent && e.parent;)e = e.parent; return e } return e.eW ? u(e.parent, n) : void 0 } function c(e, n) { return !a && r(n.iR, e) } function l(e, n) { var t = N.cI ? n[0].toLowerCase() : n[0]; return e.k.hasOwnProperty(t) && e.k[t] } function p(e, n, t, r) { var a = r ? "" : I.classPrefix, i = '<span class="' + a, o = t ? "" : C; return i += e + '">', i + n + o } function h() { var e, t, r, a; if (!E.k) return n(k); for (a = "", t = 0, E.lR.lastIndex = 0, r = E.lR.exec(k); r;)a += n(k.substring(t, r.index)), e = l(E, r), e ? (B += e[1], a += p(e[0], n(r[0]))) : a += n(r[0]), t = E.lR.lastIndex, r = E.lR.exec(k); return a + n(k.substr(t)) } function d() { var e = "string" == typeof E.sL; if (e && !y[E.sL]) return n(k); var t = e ? f(E.sL, k, !0, x[E.sL]) : g(k, E.sL.length ? E.sL : void 0); return E.r > 0 && (B += t.r), e && (x[E.sL] = t.top), p(t.language, t.value, !1, !0) } function b() { L += null != E.sL ? d() : h(), k = "" } function v(e) { L += e.cN ? p(e.cN, "", !0) : "", E = Object.create(e, { parent: { value: E } }) } function m(e, n) { if (k += e, null == n) return b(), 0; var t = o(n, E); if (t) return t.skip ? k += n : (t.eB && (k += n), b(), t.rB || t.eB || (k = n)), v(t, n), t.rB ? 0 : n.length; var r = u(E, n); if (r) { var a = E; a.skip ? k += n : (a.rE || a.eE || (k += n), b(), a.eE && (k = n)); do E.cN && (L += C), E.skip || (B += E.r), E = E.parent; while (E !== r.parent); return r.starts && v(r.starts, ""), a.rE ? 0 : n.length } if (c(n, E)) throw new Error('Illegal lexeme "' + n + '" for mode "' + (E.cN || "<unnamed>") + '"'); return k += n, n.length || 1 } var N = w(e); if (!N) throw new Error('Unknown language: "' + e + '"'); s(N); var R, E = i || N, x = {}, L = ""; for (R = E; R !== N; R = R.parent)R.cN && (L = p(R.cN, "", !0) + L); var k = "", B = 0; try { for (var M, j, O = 0; ;) { if (E.t.lastIndex = O, M = E.t.exec(t), !M) break; j = m(t.substring(O, M.index), M[0]), O = M.index + j } for (m(t.substr(O)), R = E; R.parent; R = R.parent)R.cN && (L += C); return { r: B, value: L, language: e, top: E } } catch (T) { if (T.message && -1 !== T.message.indexOf("Illegal")) return { r: 0, value: n(t) }; throw T } } function g(e, t) { t = t || I.languages || x(y); var r = { r: 0, value: n(e) }, a = r; return t.filter(w).forEach(function (n) { var t = f(n, e, !1); t.language = n, t.r > a.r && (a = t), t.r > r.r && (a = r, r = t) }), a.language && (r.second_best = a), r } function p(e) { return I.tabReplace || I.useBR ? e.replace(M, function (e, n) { return I.useBR && "\n" === e ? "<br>" : I.tabReplace ? n.replace(/\t/g, I.tabReplace) : "" }) : e } function h(e, n, t) { var r = n ? L[n] : t, a = [e.trim()]; return e.match(/\bhljs\b/) || a.push("hljs"), -1 === e.indexOf(r) && a.push(r), a.join(" ").trim() } function d(e) { var n, t, r, o, l, s = i(e); a(s) || (I.useBR ? (n = document.createElementNS("http://www.w3.org/1999/xhtml", "div"), n.innerHTML = e.innerHTML.replace(/\n/g, "").replace(/<br[ \/]*>/g, "\n")) : n = e, l = n.textContent, r = s ? f(s, l, !0) : g(l), t = u(n), t.length && (o = document.createElementNS("http://www.w3.org/1999/xhtml", "div"), o.innerHTML = r.value, r.value = c(t, u(o), l)), r.value = p(r.value), e.innerHTML = r.value, e.className = h(e.className, s, r.language), e.result = { language: r.language, re: r.r }, r.second_best && (e.second_best = { language: r.second_best.language, re: r.second_best.r })) } function b(e) { I = o(I, e) } function v() { if (!v.called) { v.called = !0; var e = document.querySelectorAll("pre code"); E.forEach.call(e, d) } } function m() { addEventListener("DOMContentLoaded", v, !1), addEventListener("load", v, !1) } function N(n, t) { var r = y[n] = t(e); r.aliases && r.aliases.forEach(function (e) { L[e] = n }) } function R() { return x(y) } function w(e) { return e = (e || "").toLowerCase(), y[e] || y[L[e]] } var E = [], x = Object.keys, y = {}, L = {}, k = /^(no-?highlight|plain|text)$/i, B = /\blang(?:uage)?-([\w-]+)\b/i, M = /((^(<[^>]+>|\t|)+|(?:\n)))/gm, C = "</span>", I = { classPrefix: "hljs-", tabReplace: null, useBR: !1, languages: void 0 }; return e.highlight = f, e.highlightAuto = g, e.fixMarkup = p, e.highlightBlock = d, e.configure = b, e.initHighlighting = v, e.initHighlightingOnLoad = m, e.registerLanguage = N, e.listLanguages = R, e.getLanguage = w, e.inherit = o, e.IR = "[a-zA-Z]\\w*", e.UIR = "[a-zA-Z_]\\w*", e.NR = "\\b\\d+(\\.\\d+)?", e.CNR = "(-?)(\\b0[xX][a-fA-F0-9]+|(\\b\\d+(\\.\\d*)?|\\.\\d+)([eE][-+]?\\d+)?)", e.BNR = "\\b(0b[01]+)", e.RSR = "!|!=|!==|%|%=|&|&&|&=|\\*|\\*=|\\+|\\+=|,|-|-=|/=|/|:|;|<<|<<=|<=|<|===|==|=|>>>=|>>=|>=|>>>|>>|>|\\?|\\[|\\{|\\(|\\^|\\^=|\\||\\|=|\\|\\||~", e.BE = { b: "\\\\[\\s\\S]", r: 0 }, e.ASM = { cN: "string", b: "'", e: "'", i: "\\n", c: [e.BE] }, e.QSM = { cN: "string", b: '"', e: '"', i: "\\n", c: [e.BE] }, e.PWM = { b: /\b(a|an|the|are|I'm|isn't|don't|doesn't|won't|but|just|should|pretty|simply|enough|gonna|going|wtf|so|such|will|you|your|they|like|more)\b/ }, e.C = function (n, t, r) { var a = e.inherit({ cN: "comment", b: n, e: t, c: [] }, r || {}); return a.c.push(e.PWM), a.c.push({ cN: "doctag", b: "(?:TODO|FIXME|NOTE|BUG|XXX):", r: 0 }), a }, e.CLCM = e.C("//", "$"), e.CBCM = e.C("/\\*", "\\*/"), e.HCM = e.C("#", "$"), e.NM = { cN: "number", b: e.NR, r: 0 }, e.CNM = { cN: "number", b: e.CNR, r: 0 }, e.BNM = { cN: "number", b: e.BNR, r: 0 }, e.CSSNM = { cN: "number", b: e.NR + "(%|em|ex|ch|rem|vw|vh|vmin|vmax|cm|mm|in|pt|pc|px|deg|grad|rad|turn|s|ms|Hz|kHz|dpi|dpcm|dppx)?", r: 0 }, e.RM = { cN: "regexp", b: /\//, e: /\/[gimuy]*/, i: /\n/, c: [e.BE, { b: /\[/, e: /\]/, r: 0, c: [e.BE] }] }, e.TM = { cN: "title", b: e.IR, r: 0 }, e.UTM = { cN: "title", b: e.UIR, r: 0 }, e.METHOD_GUARD = { b: "\\.\\s*" + e.UIR, r: 0 }, e }); hljs.registerLanguage("json", function (e) { var i = { literal: "true false null" }, n = [e.QSM, e.CNM], r = { e: ",", eW: !0, eE: !0, c: n, k: i }, t = { b: "{", e: "}", c: [{ cN: "attr", b: /"/, e: /"/, c: [e.BE], i: "\\n" }, e.inherit(r, { b: /:/ })], i: "\\S" }, c = { b: "\\[", e: "\\]", c: [e.inherit(r)], i: "\\S" }; return n.splice(n.length, 0, t, c), { c: n, k: i, i: "\\S" } }); hljs.registerLanguage("java", function (e) { var a = "[a-zA-Z_$][a-zA-Z_$0-9]*", t = a + "(<" + a + "(\\s*,\\s*" + a + ")*>)?", r = "false synchronized int abstract float private char boolean static null if const for true while long strictfp finally protected import native final void enum else break transient catch instanceof byte super volatile case assert short package default double public try this switch continue throws protected public private module requires exports do", s = "\\b(0[bB]([01]+[01_]+[01]+|[01]+)|0[xX]([a-fA-F0-9]+[a-fA-F0-9_]+[a-fA-F0-9]+|[a-fA-F0-9]+)|(([\\d]+[\\d_]+[\\d]+|[\\d]+)(\\.([\\d]+[\\d_]+[\\d]+|[\\d]+))?|\\.([\\d]+[\\d_]+[\\d]+|[\\d]+))([eE][-+]?\\d+)?)[lLfF]?", c = { cN: "number", b: s, r: 0 }; return { aliases: ["jsp"], k: r, i: /<\/|#/, c: [e.C("/\\*\\*", "\\*/", { r: 0, c: [{ b: /\w+@/, r: 0 }, { cN: "doctag", b: "@[A-Za-z]+" }] }), e.CLCM, e.CBCM, e.ASM, e.QSM, { cN: "class", bK: "class interface", e: /[{;=]/, eE: !0, k: "class interface", i: /[:"\[\]]/, c: [{ bK: "extends implements" }, e.UTM] }, { bK: "new throw return else", r: 0 }, { cN: "function", b: "(" + t + "\\s+)+" + e.UIR + "\\s*\\(", rB: !0, e: /[{;=]/, eE: !0, k: r, c: [{ b: e.UIR + "\\s*\\(", rB: !0, r: 0, c: [e.UTM] }, { cN: "params", b: /\(/, e: /\)/, k: r, r: 0, c: [e.ASM, e.QSM, e.CNM, e.CBCM] }, e.CLCM, e.CBCM] }, c, { cN: "meta", b: "@[A-Za-z]+" }] } }); hljs.registerLanguage("xml", function (s) { var e = "[A-Za-z0-9\\._:-]+", t = { eW: !0, i: /</, r: 0, c: [{ cN: "attr", b: e, r: 0 }, { b: /=\s*/, r: 0, c: [{ cN: "string", endsParent: !0, v: [{ b: /"/, e: /"/ }, { b: /'/, e: /'/ }, { b: /[^\s"'=<>`]+/ }] }] }] }; return { aliases: ["html", "xhtml", "rss", "atom", "xjb", "xsd", "xsl", "plist"], cI: !0, c: [{ cN: "meta", b: "<!DOCTYPE", e: ">", r: 10, c: [{ b: "\\[", e: "\\]" }] }, s.C("<!--", "-->", { r: 10 }), { b: "<\\!\\[CDATA\\[", e: "\\]\\]>", r: 10 }, { b: /<\?(php)?/, e: /\?>/, sL: "php", c: [{ b: "/\\*", e: "\\*/", skip: !0 }] }, { cN: "tag", b: "<style(?=\\s|>|$)", e: ">", k: { name: "style" }, c: [t], starts: { e: "</style>", rE: !0, sL: ["css", "xml"] } }, { cN: "tag", b: "<script(?=\\s|>|$)", e: ">", k: { name: "script" }, c: [t], starts: { e: "</script>", rE: !0, sL: ["actionscript", "javascript", "handlebars", "xml"] } }, { cN: "meta", v: [{ b: /<\?xml/, e: /\?>/, r: 10 }, { b: /<\?\w+/, e: /\?>/ }] }, { cN: "tag", b: "</?", e: "/?>", c: [{ cN: "name", b: /[^\/><\s]+/, r: 0 }, t] }] } }); hljs.registerLanguage("sql", function (e) { var t = e.C("--", "$"); return { cI: !0, i: /[<>{}*#]/, c: [{ bK: "begin end start commit rollback savepoint lock alter create drop rename call delete do handler insert load replace select truncate update set show pragma grant merge describe use explain help declare prepare execute deallocate release unlock purge reset change stop analyze cache flush optimize repair kill install uninstall checksum restore check backup revoke comment", e: /;/, eW: !0, l: /[\w\.]+/, k: { keyword: "abort abs absolute acc acce accep accept access accessed accessible account acos action activate add addtime admin administer advanced advise aes_decrypt aes_encrypt after agent aggregate ali alia alias allocate allow alter always analyze ancillary and any anydata anydataset anyschema anytype apply archive archived archivelog are as asc ascii asin assembly assertion associate asynchronous at atan atn2 attr attri attrib attribu attribut attribute attributes audit authenticated authentication authid authors auto autoallocate autodblink autoextend automatic availability avg backup badfile basicfile before begin beginning benchmark between bfile bfile_base big bigfile bin binary_double binary_float binlog bit_and bit_count bit_length bit_or bit_xor bitmap blob_base block blocksize body both bound buffer_cache buffer_pool build bulk by byte byteordermark bytes cache caching call calling cancel capacity cascade cascaded case cast catalog category ceil ceiling chain change changed char_base char_length character_length characters characterset charindex charset charsetform charsetid check checksum checksum_agg child choose chr chunk class cleanup clear client clob clob_base clone close cluster_id cluster_probability cluster_set clustering coalesce coercibility col collate collation collect colu colum column column_value columns columns_updated comment commit compact compatibility compiled complete composite_limit compound compress compute concat concat_ws concurrent confirm conn connec connect connect_by_iscycle connect_by_isleaf connect_by_root connect_time connection consider consistent constant constraint constraints constructor container content contents context contributors controlfile conv convert convert_tz corr corr_k corr_s corresponding corruption cos cost count count_big counted covar_pop covar_samp cpu_per_call cpu_per_session crc32 create creation critical cross cube cume_dist curdate current current_date current_time current_timestamp current_user cursor curtime customdatum cycle data database databases datafile datafiles datalength date_add date_cache date_format date_sub dateadd datediff datefromparts datename datepart datetime2fromparts day day_to_second dayname dayofmonth dayofweek dayofyear days db_role_change dbtimezone ddl deallocate declare decode decompose decrement decrypt deduplicate def defa defau defaul default defaults deferred defi defin define degrees delayed delegate delete delete_all delimited demand dense_rank depth dequeue des_decrypt des_encrypt des_key_file desc descr descri describ describe descriptor deterministic diagnostics difference dimension direct_load directory disable disable_all disallow disassociate discardfile disconnect diskgroup distinct distinctrow distribute distributed div do document domain dotnet double downgrade drop dumpfile duplicate duration each edition editionable editions element ellipsis else elsif elt empty enable enable_all enclosed encode encoding encrypt end end-exec endian enforced engine engines enqueue enterprise entityescaping eomonth error errors escaped evalname evaluate event eventdata events except exception exceptions exchange exclude excluding execu execut execute exempt exists exit exp expire explain export export_set extended extent external external_1 external_2 externally extract failed failed_login_attempts failover failure far fast feature_set feature_value fetch field fields file file_name_convert filesystem_like_logging final finish first first_value fixed flash_cache flashback floor flush following follows for forall force form forma format found found_rows freelist freelists freepools fresh from from_base64 from_days ftp full function general generated get get_format get_lock getdate getutcdate global global_name globally go goto grant grants greatest group group_concat group_id grouping grouping_id groups gtid_subtract guarantee guard handler hash hashkeys having hea head headi headin heading heap help hex hierarchy high high_priority hosts hour http id ident_current ident_incr ident_seed identified identity idle_time if ifnull ignore iif ilike ilm immediate import in include including increment index indexes indexing indextype indicator indices inet6_aton inet6_ntoa inet_aton inet_ntoa infile initial initialized initially initrans inmemory inner innodb input insert install instance instantiable instr interface interleaved intersect into invalidate invisible is is_free_lock is_ipv4 is_ipv4_compat is_not is_not_null is_used_lock isdate isnull isolation iterate java join json json_exists keep keep_duplicates key keys kill language large last last_day last_insert_id last_value lax lcase lead leading least leaves left len lenght length less level levels library like like2 like4 likec limit lines link list listagg little ln load load_file lob lobs local localtime localtimestamp locate locator lock locked log log10 log2 logfile logfiles logging logical logical_reads_per_call logoff logon logs long loop low low_priority lower lpad lrtrim ltrim main make_set makedate maketime managed management manual map mapping mask master master_pos_wait match matched materialized max maxextents maximize maxinstances maxlen maxlogfiles maxloghistory maxlogmembers maxsize maxtrans md5 measures median medium member memcompress memory merge microsecond mid migration min minextents minimum mining minus minute minvalue missing mod mode model modification modify module monitoring month months mount move movement multiset mutex name name_const names nan national native natural nav nchar nclob nested never new newline next nextval no no_write_to_binlog noarchivelog noaudit nobadfile nocheck nocompress nocopy nocycle nodelay nodiscardfile noentityescaping noguarantee nokeep nologfile nomapping nomaxvalue nominimize nominvalue nomonitoring none noneditionable nonschema noorder nopr nopro noprom nopromp noprompt norely noresetlogs noreverse normal norowdependencies noschemacheck noswitch not nothing notice notrim novalidate now nowait nth_value nullif nulls num numb numbe nvarchar nvarchar2 object ocicoll ocidate ocidatetime ociduration ociinterval ociloblocator ocinumber ociref ocirefcursor ocirowid ocistring ocitype oct octet_length of off offline offset oid oidindex old on online only opaque open operations operator optimal optimize option optionally or oracle oracle_date oradata ord ordaudio orddicom orddoc order ordimage ordinality ordvideo organization orlany orlvary out outer outfile outline output over overflow overriding package pad parallel parallel_enable parameters parent parse partial partition partitions pascal passing password password_grace_time password_lock_time password_reuse_max password_reuse_time password_verify_function patch path patindex pctincrease pctthreshold pctused pctversion percent percent_rank percentile_cont percentile_disc performance period period_add period_diff permanent physical pi pipe pipelined pivot pluggable plugin policy position post_transaction pow power pragma prebuilt precedes preceding precision prediction prediction_cost prediction_details prediction_probability prediction_set prepare present preserve prior priority private private_sga privileges procedural procedure procedure_analyze processlist profiles project prompt protection public publishingservername purge quarter query quick quiesce quota quotename radians raise rand range rank raw read reads readsize rebuild record records recover recovery recursive recycle redo reduced ref reference referenced references referencing refresh regexp_like register regr_avgx regr_avgy regr_count regr_intercept regr_r2 regr_slope regr_sxx regr_sxy reject rekey relational relative relaylog release release_lock relies_on relocate rely rem remainder rename repair repeat replace replicate replication required reset resetlogs resize resource respect restore restricted result result_cache resumable resume retention return returning returns reuse reverse revoke right rlike role roles rollback rolling rollup round row row_count rowdependencies rowid rownum rows rtrim rules safe salt sample save savepoint sb1 sb2 sb4 scan schema schemacheck scn scope scroll sdo_georaster sdo_topo_geometry search sec_to_time second section securefile security seed segment select self sequence sequential serializable server servererror session session_user sessions_per_user set sets settings sha sha1 sha2 share shared shared_pool short show shrink shutdown si_averagecolor si_colorhistogram si_featurelist si_positionalcolor si_stillimage si_texture siblings sid sign sin size size_t sizes skip slave sleep smalldatetimefromparts smallfile snapshot some soname sort soundex source space sparse spfile split sql sql_big_result sql_buffer_result sql_cache sql_calc_found_rows sql_small_result sql_variant_property sqlcode sqldata sqlerror sqlname sqlstate sqrt square standalone standby start starting startup statement static statistics stats_binomial_test stats_crosstab stats_ks_test stats_mode stats_mw_test stats_one_way_anova stats_t_test_ stats_t_test_indep stats_t_test_one stats_t_test_paired stats_wsr_test status std stddev stddev_pop stddev_samp stdev stop storage store stored str str_to_date straight_join strcmp strict string struct stuff style subdate subpartition subpartitions substitutable substr substring subtime subtring_index subtype success sum suspend switch switchoffset switchover sync synchronous synonym sys sys_xmlagg sysasm sysaux sysdate sysdatetimeoffset sysdba sysoper system system_user sysutcdatetime table tables tablespace tan tdo template temporary terminated tertiary_weights test than then thread through tier ties time time_format time_zone timediff timefromparts timeout timestamp timestampadd timestampdiff timezone_abbr timezone_minute timezone_region to to_base64 to_date to_days to_seconds todatetimeoffset trace tracking transaction transactional translate translation treat trigger trigger_nestlevel triggers trim truncate try_cast try_convert try_parse type ub1 ub2 ub4 ucase unarchived unbounded uncompress under undo unhex unicode uniform uninstall union unique unix_timestamp unknown unlimited unlock unpivot unrecoverable unsafe unsigned until untrusted unusable unused update updated upgrade upped upper upsert url urowid usable usage use use_stored_outlines user user_data user_resources users using utc_date utc_timestamp uuid uuid_short validate validate_password_strength validation valist value values var var_samp varcharc vari varia variab variabl variable variables variance varp varraw varrawc varray verify version versions view virtual visible void wait wallet warning warnings week weekday weekofyear wellformed when whene whenev wheneve whenever where while whitespace with within without work wrapped xdb xml xmlagg xmlattributes xmlcast xmlcolattval xmlelement xmlexists xmlforest xmlindex xmlnamespaces xmlpi xmlquery xmlroot xmlschema xmlserialize xmltable xmltype xor year year_to_month years yearweek", literal: "true false null", built_in: "array bigint binary bit blob boolean char character date dec decimal float int int8 integer interval number numeric real record serial serial8 smallint text varchar varying void" }, c: [{ cN: "string", b: "'", e: "'", c: [e.BE, { b: "''" }] }, { cN: "string", b: '"', e: '"', c: [e.BE, { b: '""' }] }, { cN: "string", b: "`", e: "`", c: [e.BE] }, e.CNM, e.CBCM, t] }, e.CBCM, t] } });

    window.MiniProfiler.highlight = window.hljs.highlightBlock;
    // restore
    window.hljs = oldHljs;
})();