
## RStudio 2022-12.0 "Elsbeth Geranium" Release Notes

### New

- RStudio now displays a splash screen on startup, while the R session is being initialized. (#11604)
- Updated RStudio Desktop installer on Windows to detect running RStudio by process name. (#10588)

### R

- Whether pending console input is discarded on error can now be controlled via a preference in the Console pane. (#10391)
- Improved handling of diagnostics within pipeline expressions. (#11780)
- Improved handling of diagnostics within glue() expressions.
- Completions within R Markdown documents now respect the `knitr` `root.dir` chunk option if set. (#12047)
- RStudio now provides autocompletion results for packages used but not loaded within a project.
- Improved handling of missing arguments for some functions in the diagnostics system.
- Code editor can show previews of color in strings (R named colors e.g. "tomato3" or of the forms "#rgb", "#rrggbb", "#rrggbbaa") when `Options > Code > Display > [ ] enable preview of named and hexadecimal colors` is checked.
- Fixes the bug introduced with `rlang` >= 1.03 where Rmd documents show the error message `object 'partition_yaml_front_matter' not found` upon project startup. (#11552)
- Name autocompletion following a `$` now correctly quotes names that start with underscore followed by alphanumeric characters. (#11689)
- Suspended sessions will now default to using the project directory, rather than the user home directory, if the prior working directory is no longer accessible. (#11960)
- The fuzzy finder indexes C(++) macros. (#11981)
- Improved handling for large amounts of `message()` output in the Console pane. (#12059)
- Build output is now truncating when very large amounts of output are produced (e.g. from C++ compilation warnings).
- Memory usage in the environment pane now works correctly on Linux when using cgroups v2. (#11894)
- Fixed an issue where code execution could pause in RStudio Server after closing the browser tab even with active computations. (Pro #3943)
- Chunk output calls `format()` method on vctrs-based classes stored in a dataframe. (#6878)
  
### Python

- RStudio attempts to infer the appropriate version of Python when "Automatically activate project-local Python environments" is checked and the user has not requested a specific version of Python. This Python will be stored in the environment variable "RETICULATE_PYTHON_FALLBACK", available from the R console, the Python REPL, and the RStudio Terminal (#9990)
- Shiny for Python apps now display a "Run App" button on the Source editor toolbar. (Requires `shiny` Python package v0.2.7 or later.)

### Quarto

- Support for v2 format of Quarto crossref index
- Support for RHEL7 and CentOS7 and fixes missing Pandoc for RMarkdown (rstudio-pro#3804)

### Posit Workbench

- Adds `-l` (long) option to `rserver-url`. When `/usr/lib/rstudio-server/bin/rserver-url -l <port number>` is executed within a VS Code or Jupyter session, the full URL where a user can view a server proxied at that port is displayed (rstudio-pro#3620)

#### Posit Workbench VS Code Sessions

- Install VS Code session support (code-server) with Posit Workbench instead of requiring a separate download (rstudio-pro#3643)
- Enable VS Code sessions by default on initial Workbench install (rstudio-pro#3643)
- Updated code-server to version 4.7.1 (VS Code version 1.71.2) (rstudio-pro#3643)
- Sets the `UVICORN_ROOT_PATH` environment variable to the proxied port URL for port 8000 in VS Code and Jupyter sessions, allowing FastAPI applications to run without additional configuration. (rstudio-pro#2660)
- Workbench now checks for the `code-server` binary in the `WORKBENCH_VSCODE_PATH` environment variable (if present) then the `vscode-exe` setting in `vscode.conf`, or falls back to the preinstalled version and finally the `PATH`. This allows more flexibility when launching sessions on Kubernetes or Slurm clusters. (rstudio/rstudio-pro#3942)

#### Posit Workbench VS Code Extension

- Install VS Code Extension with Posit Workbench instead of requiring a separate download (rstudio-pro#3643)
- Introduce Workbench Job management to VS Code Extension (rstudio/rstudio-pro#3784, rstudio/rstudio-pro#3565)
- Added a pop-up notification when working with certain relevant filetypes that makes it easier to find the Workbench Extension. This notification is a one-time view per user. It can be re-enabled in the user settings (vscode-ext#96).
- Rebranded the interface to match Posit Software, PBC's new branding terminology and iconography
- Fixed extension servers appearing in Proxied Servers list (vscode-ext#116)
- Added support for Flask, including a help dialog and the Posit Workbench Flask Interface code snippet, for proxying Flask applications (rstudio-pro#2660)
- Added a FastAPI help dialog and the following code snippets for setting the root_path in FastAPI applications (rstudio/rstudio-pro#3698):
    - Posit Workbench Uvicorn Root Path Snippet
    - Posit Workbench FastAPI Uvicorn Root Path Snippet
- Added a Warning notification that appears when a uvicorn process was started with a custom port, without the root-path argument. These processes will now appear in the Proxied Servers view with a warning icon (rstudio/rstudio-pro#3699)
- Fixed mislabelling of Shiny app in Proxied Servers list (#117)

#### Posit Workbench Jupyter Sessions

- Workbench now checks for the `jupyter` binary in the `WORKBENCH_JUPYTER_PATH` environment variable (if present), then the `jupyter-exe` setting in `jupyter.conf`, and finally the `PATH`. This is allows more flexibility when launching sessions on Kubernetes or Slurm clusters. (rstudio/rstudio-pro#3942)
- Fixed an issue where Workbench would not install the `jupyter` extension automatically if the configured `jupyter` path was a symlink to the actual install location. We now follow the symlink. (rstudio/rstudio-pro#3942)

#### Posit Workbench Jupyter Extension

- The Jupyter Notebook and JupyterLab extensions have been updated to match with the new Posit Software, PBC branding (rstudio-pro#3645)

### Deprecated / Removed

- Removed the Tools / Shell command (#11253)
- Removed the "rstudio-server install-vs-code" admin command for downloading and configuring code-server; code-server is now installed with Workbench, use the "rstudio-server configure-vs-code" command for configuration (rstudio-pro#3643)
- It is highly recommended that you disable session culling in Jupyter sessions by done by setting `session-cull-minutes=0` in `jupyter.conf` to prevent potential data loss (rstudio-pro#3054) 

### Experimental ARM64 and RedHat 9 support

- Experimental (preview) support for Linux `aarch64` platforms, such as the Raspberry Pi and AWS Graviton
- Experimental (preview) support for RedHat Enterprise Linux 9 and compatible platforms, such as Rocky Linux 9

### Fixed

- Fixed an issue where the plots displayed in the inline preview were incorrectly scaled on devices with high DP (#4521, #2098, #10825, #7028, #4913).
- Fixed an issue where the console history scroll position was not preserved when switching focus to a separate application (#1638)
- Fixed an issue where Find in Files could omit matches in some cases on Windows (#11736)
- Fixed an issue where the Git History window inverted the display of merge diffs (#10150)
- Fixed an issue where Find in Files could fail to find results with certain versions of git (#11822)
- Fixed visual mode outline missing nested R code chunks (#11410)
- Fixed an issue where chunks containing multibyte characters was not executed correctly (#10632)
- Fixed bringing main window under active secondary window when executing background command (#11407)
- Fix for schema version comparison that breaks db in downgrade -> upgrade scenarios (rstudio-pro#3572)
- Fixed an issue in the Electron build of the IDE on Macs where users could not clone a git repository via password-protected SSH or HTTPS (#11693)
- Fixed scroll speed sensitivity for Mac and Linux and added a preference to adjust it (#11578)
- Fixed an issue in server environments where invoking `systemd` directly could lead to orphaned processes and undefined behavior. Processes are now cleaned up more consistently as a part of server or launcher shutdown. (rstudio/rstudio-pro#2585)
- RStudio sessions now shut down and suspend properly when they receive a `SIGTERM` signal, as they have for `SIGUSR2` (rstudio/rstudio-pro#2585)
- Fixed an issue where `ssl-hsts-include-subdomains=1` would render Workbench non-functional. The setting now works as expected. (rstudio/rstudio-pro#3010)
- Fixed an issue where the `suspend-session`, `suspend-all`, and `kill-all` subcommands of `rstudio-server` did not work when using the Launcher. (rstudio/rstudio-pro#4007)
