## v1.3 Patch 3 (Apricot Nasturtium) - Release Notes

### Bugfixes

- Fix an issue where a PAM session would be attempted for Launcher sessions when no password is available, potentially locking users out of their accounts (Pro #1831)
- Fix an issue where syslog and monitor log entries could contain newlines in them, preventing the admin logs page from properly displaying log entries (Pro #1782)
