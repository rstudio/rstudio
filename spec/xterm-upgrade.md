# Overview

RStudio's terminal pane uses xterm.js version 4.9.0, which is very old (September 2020).

We need to integrate the latest version 5.5.0. There have been many API changes and also changes
to the addons so this won't be a simple drop-in.

## References

- xterm.js release notes (note there are multiple pages): https://github.com/xtermjs/xterm.js/releases
- API (Typescript) for newest xterm.js (5.5.0): https://github.com/xtermjs/xterm.js/blob/5.5.0/typings/xterm.d.ts
- API for version we're currently using (4.9.0): https://github.com/xtermjs/xterm.js/blob/4.9.0/typings/xterm.d.ts
- script for integrating a new version of xterm.js: @src/gwt/tools/build-xterm
- Most of the UI code related to the Terminal pane and xterm.js integration lives in files
  under @src/gwt/src/org/rstudio/studio/client/workbench/views/terminal/
- The server-side code is primarily in or near the following files:
  - @src/cpp/session/SessionConsoleProcess.cpp
  - @src/cpp/session/include/session/SessionConsoleProcess.hpp
  - @src/cpp/session/modules/SessionTerminal.cpp
  - @src/cpp/session/modules/SessionTerminal.hpp
  - @src/cpp/session/modules/SessionTerminalShell.cpp
  - @src/cpp/session/include/session/SessionTerminalShell.hpp

## Concerns

The Terminal Pane was added to RStudio a long time ago (2016/2017) and there are workarounds to deal
with features that didn't exist in xterm.js at the time, such as reading and writing the terminal
buffer for scenarios such as reloading the terminal contents when reconnecting to an existing
RStudio session via a new instance of the UI (web). Pay careful attention to places where we are
potentially using undocumented internals that could be updated to use official APIs.

## Other Instructions

- do not use experimental APIs
- if an xterm.js addon is available that will simplify a scenario, propose adding it
- if you see problems in the existing Terminal code, whether or not they are related to updating
  xterm.js, point them out
