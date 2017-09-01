## v1.2 - Release Notes

### Miscellaneous

* Added option to temporarily disable environment pane refreshing

### RStudio Server Pro

* Overhauled R versions, allowing you to specify version labels, load environment modules, and execute a prelaunch script when loading specific versions.
* New rsession-diagnostics-enabled option for rserver.conf to enable session launch diagnostics mode to help diagnose session launch failures.
* Added support for auth-pam-sessions-use-password option in a load balanced setup.
* Added ability to suspend sessions from user home page.
* Added hmac signature verification for proxy auth mode with new auth-proxy-require-hmac option in rserver.conf.
