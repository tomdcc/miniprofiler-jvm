Changelog
=========

0.9.3
---
- Resource URLs use correct version for cache-busting.

0.9.2
---
- Profiler JSON has stable field order

0.9.1
---
- Always use original Ratpack execution for cleanup
- Add async Ratpack storage interface.

0.9
---
- Drop support for JDK < 8
- Split out servlet support into subproject
- Add JSP tag
- Update to latest upstream UI, now in miniprofiler/dotnet project
- Fix Ratpack interceptor binding

0.8.1
---
- Minor jOOQ integration improvement

0.8.1
---
- Make jOOQ integration more extensible

0.8
---
- Add jOOQ integration
- Some API rearranging
- Minor fixes and improvements

0.7
---
- Add child profilers, mainly to support forked Ratpack executions

0.6.3
-----
- Minor Ratpack improvement

0.6.2
-----
- Close profiler on Ratpack response send

0.6.1
-----
- Add ability to pass config to script tag writer

0.6
---
- Improve Ratpack promise profiling efficiency
- Move back to upstream UI

0.5.7
-----
- Allow Ratpack module subclassing to customise behaviour

0.5.6
-----
- Various Ratpack integration improvements

0.5.5
-----
- Use forked UI that supports capturing AJAX page loads

0.5.4
-----
- Make Ratpack integration more extensible

0.5.3
-----
- Don't provide Ratpack version dependency

0.5.2
-----
- Minor Ratpack-related fixes

0.5.1
-----
- Minor Ratpack-related fixes

0.5
---
- Add Ratpack support

0.4.1
-----
- Move Grails support code from plugin to a module in this project
- Minor bug fixes

0.4
---
- Various API improvements
- Use UI from miniprofiler/ui project
- Support JDK7 JDBC methods
- Add JavaEE / EJB integration
- Profile Hibernate / Eclipselink generated SQL
- Fix JSON serialisation

0.3
---
- Initial public release
- Basic Java, JDBC and servlet integration
