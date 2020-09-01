## v1.3 Patch 3 (Apricot Nasturtium) - Release Notes

### Bugfixes

- Fix an issue where a PAM session would be attempted for Launcher sessions when no password is available, potentially locking users out of their accounts (Pro #1831)
- Fix an issue causing logs to fail to load in the admin panel when using the local time zone (Pro #1766)
- Fix an issue where syslog and monitor log entries could contain newlines in them, preventing the admin logs page from properly displaying log entries (Pro #1782)
- Fix an issue where failing to open the Slurm job output file could result in hanging `tail -f` child processes (Pro #1856)
- Fix an issue where a delay in the creation of the Slurm job output file could result in a hanging "Determining session network" page when opening a session (Pro #1792)
- Fix an issue where `auth-none` would not load sessions (#7575)
- Fix session crashing on Windows desktop (#7637, #7652, #7665)
- Fix an issue where launching many sessions simultaneously could cause a segfault (#7670)
- Fix an issue where invalid logging statements were not properly skipped in the Admin log page, causing a crash of the rserver-admin process (#1766)
