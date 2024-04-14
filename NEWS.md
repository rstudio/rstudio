## RStudio 2024.07.0 "Cranberry Hibiscus" Release Notes

### New
#### RStudio
-

#### Posit Workbench
-
- Show custom project names on Workbench homepage (rstudio-pro#5589)
- Added support for JupyterLab 4 through the Python extension [PWB JupyterLab](https://pypi.org/project/pwb-jupyterlab/) (rstudio-pro#5777)
- Added search results copy button and search results breadcrumbs to Workbench Administration Guide, Workbench User Guide, RStudio Desktop Pro Administration Guide (#5088, #5603)
- Migrated Troubleshooting Launcher and Kubernetes guide from docs.posit.co site to Workbench Administration Guide: Job Launcher > Launcher Troubleshooting section (#5720)
- Add search results copy button and search results breadcrumbs to Workbench Administration Guide, Workbench User Guide, RStudio Desktop Pro Administration Guide (#5088, #5603)
- When launching sessions from the home page, memory is now shown in GB rather than MB (rstudio-pro#2722)
- When running sessions on Kubernetes or Slurm with Singularity, the home page now remembers the container image used most recently and sets it as the default. This includes custom images, when permitted (rstudio-pro#3601, rstudio-pro#4079)
- When launching a session from the home page on Kubernetes or Slurm, the dropdown for available container images is now better organized and manages long image names more gracefully (rstudio-pro#5630)
- Opening the job summary for a session on the home page now shows much more detailed information, including the session's host (if available), AWS role and/or Databricks Workspace (if any), queue (on Slurm), platform-native ID (on Slurm and Kubernetes) and resource profile (if applicable) (rstudio-pro#3690, rstudio-pro#5537)
- The Databricks pane is now enabled by default if there are Databricks workspaces configured in the `databricks.conf` file. There is no need to enable it separately. To disable the pane, set `databricks-enabled=0` in the `rserver.conf` file (rstudio-pro#5556)
- A new icon on the RStudio Pro toolbar shows information on active AWS or Databricks credentials, if present (rstudio-pro#5860)
- The PostgreSQL password can now be set via environment variable (rstudio-pro#5332)
- Updated nginx to 1.25.4 (rstudio/rstudio-pro#5970)
- Updated nodejs to 18.19.1 (rstudio/rstudio-pro#5935)
- Performance improvement where nginx handles static files, configurable with nginx.static.conf file (rstudio/rstudio-pro#5671)
- VSCode: upgraded coder/code-server to 4.22.1 (centos 4.16.1). Added preview of Code OSS - with Posit, pwb-code-server (rstudio/rstudio-pro#3777)
- Robustness and scalability improvements for streaming connections. Close all streaming connections when tabs are invisible, and impose limits on "session output view". Added options for tuning large-scale system performance: launcher-sessions-proxy-timeout-seconds, max-streams-per-user, streaming-connection-timeout-seconds rserver.conf option.  (rstudio/rstudio-pro#5888, rstudio/rstudio-pro#5616, rstudio/rstudio-pro#5771, rstudio/rstudio-pro#3417)
- Fixed sporadic CPU usage and crash in rworkspaces (rstudio/rstudio-pro#5690)
- Fixed problem with `rstudio-server reload` changing permissions on nginx proxy/body dirs leading to user errors accessing the system (rstudio/rstudio-pro#5636)
- Added support for debug, and strace session diagnostics for individual user sessions. Create a file with 'strace' or 'debug' in ~/.config/rstudio/rsession-diagnostics or ~/.config/pwb/jupyter-diagnostics, vscode-diagnostics. Logs in ~/.local/share/{rstudio,pwb}/log. (rstudio/rstudio-pro#5529)
- Posit Workbench no longer has a dependency on the `libuser` package (rstudio/rstudio-pro#4261)
- Container users that have special characters in their names, such as `@`, can now be created at session start (rstudio/rstudio-pro#2714)
- Prometheus metrics are now available as a Preview feature (rstudio/rstudio-pro#5793)
- Prometheus metrics can now be enabled or disabled with `metrics-enabled=[1|0]` in `rserver.conf` (rstudio/rstudio-pro#5792)
- Prometheus metrics can now be configured. For more details see [Promethes Metrics in the Workbench Admin Guide](https://docs.posit.co/ide/server-pro/2024.04.0/auditing_and_monitoring/prometheus_metrics.html) (rstudio/rstudio-pro#5791)
- Add support for automatically provisioning users when using Okta or Microsoft Entra ID as an IdP. When using this feature, users can be assigned to Workbench from the IdP and do not need to be created as POSIX users. This feature also supports migrating from local, or ldap/sssd, users to IdP provisioned users. For more details the [User Provisioning section of the Admin Guide](https://docs.posit.co/ide/server-pro/2024.04.0/authenticating_users/user_provisioning.html) (rstudio/rstudio-pro#2784)

### Fixed
#### RStudio
-

#### Posit Workbench
-

