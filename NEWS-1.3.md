## v1.3 Patch 2 (Giant Goldenrod) - Release Notes

### Misc

- Add support for configurable `rsandbox` path in Server Pro (Pro #1777)
- Add support for running Slurm plugin in unprivileged Docker containers (Pro #1744)

### Bugfixes

- Fix issue where files could not be uploaded when using RStudio Server load balancing (Pro #1751)
- Fix issue where the Crash Handler notification prompt would never go away (#7243)
- Fix issue with slow shutdown on Windows (#7117)
- Fix issue where Launcher debug logs could contain user's plain text password (Pro #1687)
- Fix issue where some log entries could not be displayed on the admin logs page (Pro #1783)
- Fix "TypeError" when sign-in using IE11 (#7359)
- Fix issue where users belonging to more than 101 groups could not launch Kubernetes sessions (Pro #1796)
- Fix problem with moving Console between left and right columns (#7246)
- Fix issue with `rstudioapi::setCursorPosition()` not scrolling cursor into view (#7317)
- Fix issue with `rmarkdown` and `packrat` packages being eagerly loaded on IDE launch (#7265)
- Fix issue with folded chunk outputs getting stuck at top of IDE (#7293)
- Fix issue where Slurm Launcher jobs could not be started on systems with root squash configured for the user home directory (#1775)
- Fix issue where locking an account would cause an infinite redirect loop on the sign-in page (Pro #1785, Pro #1764)
- Fix issue where Jupyter sessions could not be started when the home directory was a root squash mount (Pro #1795)
