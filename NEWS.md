## RStudio 2026.08.0 "Yellow Yarrow" Release Notes

### New
- ([#1634](https://github.com/rstudio/rstudio/issues/1634)): The Windows installer now lets you choose between installing for all users (the default, which prompts for administrator rights) and installing for the current user only (no administrator rights required). A current-user install no longer prompts for elevation, and uninstalling one now removes its files, Start Menu shortcut, and registry entries cleanly. Note for automated deployments: silent installs must now specify the install mode -- a bare `/S` exits with an error, so pass `/allusers /S` (the previous behavior) or `/currentuser /S`.
- ([#18153](https://github.com/rstudio/rstudio/issues/18153)): Added a preference (General > Basic) to disable the "What's New" window that RStudio Desktop shows after updating to a new version.
- ([#18158](https://github.com/rstudio/rstudio/issues/18158)): On macOS, the Files pane now follows Finder aliases: clicking an alias to a folder navigates to that folder, and clicking an alias to a file opens the file.
- ([#9924](https://github.com/rstudio/rstudio/issues/9924)): The Files pane now shows a link indicator on symbolic links and macOS Finder aliases, with the link target shown in the icon's tooltip.
- ([#18274](https://github.com/rstudio/rstudio/issues/18274)): On Windows, the Files pane now follows .lnk shortcuts: clicking a shortcut to a folder navigates to that folder, and clicking a shortcut to a file opens the file. Shortcuts are marked with a link indicator, like symbolic links and macOS Finder aliases.
- ([#8715](https://github.com/rstudio/rstudio/issues/8715)): Inline LaTeX / math previews in the source editor and visual editor are now rendered with MathJax 4 (previously MathJax 2.7), adding support for the TeX input extensions introduced in MathJax 3 and later. Rendered R Markdown documents using `mathjax = "local"` continue to use the bundled MathJax 2.7.
- ([#7350](https://github.com/rstudio/rstudio/issues/7350)): RStudio can now load custom Vim key mappings from `~/.rstudio-vimrc` (or `~/.vimrc`) when Vim editor keybindings are in use. Enable the new preference under Tools > Global Options > Code > Editing; key mapping commands (`map`, `noremap`, `unmap`, and friends) and `set` commands supported by the editor's Vim emulation are applied, and other vimrc content is ignored.

### Fixed
- ([#18152](https://github.com/rstudio/rstudio/issues/18152)): Fixed a compilation error when building RStudio Server against SOCI 4.1.4 or newer.
- ([#18174](https://github.com/rstudio/rstudio/issues/18174)): Fixed an error when viewing an object from the Object Explorer with the French user interface language enabled.
- ([#18198](https://github.com/rstudio/rstudio/issues/18198)): Fixed tar errors and warnings printed to the console after installing a package from a URL with `install.packages(..., repos = NULL)`.
- ([#18197](https://github.com/rstudio/rstudio/issues/18197)): Fixed an issue where the Render button failed to render a Quarto document living within a sub-directory of a Quarto project.
- ([#18208](https://github.com/rstudio/rstudio/issues/18208)): Fixed an issue in RStudio Server where requests could fail with "Unable to connect to service" for 30 seconds or more while a suspended session was relaunching.
- ([#17650](https://github.com/rstudio/rstudio/issues/17650)): Fixed unreadable label text in dark modal dialogs when using third-party themes (e.g. rsthemes) that style dialog labels for light backgrounds.
- ([#18215](https://github.com/rstudio/rstudio/issues/18215)): Fixed an issue where the Data Viewer could lose its scroll position, or render an empty grid, after switching to another tab and back.
- ([#18221](https://github.com/rstudio/rstudio/issues/18221)): Fixed an issue on Windows where scrolling the Data Viewer with the mouse wheel could get stuck when the pointer was over the table body.
- ([#17191](https://github.com/rstudio/rstudio/issues/17191)): Fixed per-session memory limits not being enforced for Workbench sessions running in a container with a read-only cgroup filesystem, and made the memory gauge, memory-usage report, and abort gate reflect the container rather than the host. Added an `rsession.conf` `memory-usage-mode` option to control how session and total memory usage are computed and reported.
- ([#18225](https://github.com/rstudio/rstudio/issues/18225)): Clicking a console hyperlink to a package source file that no longer exists (for example, srcref paths recording the temporary directory used during package installation) now recovers the source from the package's srcref database when available, instead of showing a "No such file" error.
- ([#13078](https://github.com/rstudio/rstudio/issues/13078)): Fixed an issue on Windows where `utils::choose.dir()` returned `NA` without showing the folder-selection dialog when running R 4.3.0 or newer.
- ([#18257](https://github.com/rstudio/rstudio/issues/18257)): Fixed the IDE becoming unresponsive for an extended period after a large number of git-tracked files changed at once (e.g. moving or committing thousands of files); Git pane updates are now batched instead of rebuilding the changelist once per changed file.
- ([#18260](https://github.com/rstudio/rstudio/issues/18260)): Fixed an issue on macOS where a file change landing just after a bulk change of many files could go undetected by the Files and Git panes until a manual refresh or unrelated later file activity.

### Dependencies
- MathJax 4.1.3 (inline LaTeX / math previews)
- Copilot Language Server 1.520.0
- Electron 41.10.2
