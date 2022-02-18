# RStudio Electron UI

This README documents the structure of custom widgets used in RStudio.

For example, consider a widget called 'clock'. The following files should be created:

```
widgets/clock.ts            <required>
widgets/clock/ui.html       <required>
widgets/clock/preload.ts    <required>
widgets/clock/load.ts       <optional>
widgets/clock/styles.css    <optional>
```

## widgets/clock.ts

This module should export a class defining the widget itself. It should inherit
either from Electron's `BrowserWindow` class, or from one of our utility classes
derived from `BrowserWindow`. This is the main module that should be imported by
others that want to use the 'clock' widget.

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

## widgets/clock/styles.css

An optional CSS stylesheet. To be applied to the HTML content after the page
has been loaded. Note that the CSS stylesheet contents are transferred via
Electron IPC, so if your widget has a custom stylesheet you should ensure
`preload.ts` receives that CSS and applies the style in an appropriate channel.

## Modal Widgets

Modal widgets which need to return a value should inherit from the `ModalWindow`
class, and provide a `showModal()` method. For example:

```
export class ModalClock extends ModalWindow<Time> {

    async onShowModal(): Promise<Time> {
        // perform other page initialization
        // return a promise that is resolved on some appropriate user gesture;
        // e.g. signaled via a message received via IPC from the page
    }

}
```

Users of these widgets can then do something of the form:

```
const widget = new ModalClock();
[result, error] = await widget.showModal();
if (error) {
    // handle error
}
```
