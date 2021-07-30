
# RStudio Electron UI

This README documents the structure of custom widgets used in RStudio.

For example, consider a widget called 'clock'. The following files should be created:

```
widgets/clock.ts          <required>
widgets/clock/ui.html     <required>
widgets/clock/preload.ts  <required>
wdigets/clock/load.ts     <optional>
```

## widgets/clock.ts

This module should export a class defining the widget itself. It should inherit
either from Electron's `BrowserWindow` class, or from one of our utility classes
derived from `BrowserWindow`.

## widgets/clock/ui.html

The actual HTML used for the window.

## widgets/clock/preload.ts

The preload script. This should primarily be used to expose callbacks used so
that the window can communicate with the main process, which should typically
communicate via messages sent on the `ipcRenderer` object. See
https://www.electronjs.org/docs/api/context-bridge for more details.

## widgets/clock/load.ts

An optional load script. This should be used to initialize widget-specific
behaviours in the widget.
