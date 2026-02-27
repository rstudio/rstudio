/*
 * gwt-window.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { nextHighest, nextLowest } from '../core/array-utils';
import { logger } from '../core/logger';
import { DesktopBrowserWindow, WindowConstructorOptions } from './desktop-browser-window';
import { ElectronDesktopOptions } from './preferences/electron-desktop-options';

export abstract class GwtWindow extends DesktopBrowserWindow {
  // initialize zoom levels (synchronize with AppearancePreferencesPane.java)
  zoomLevels = [0.25, 0.5, 0.75, 0.8, 0.9, 1.0, 1.1, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0, 4.0, 5.0];

  constructor(options: WindowConstructorOptions) {
    super(options);
    this.window.on('focus', this.onActivated.bind(this));

    // when a GWT window is navigated (for example, because we opened a new project),
    // we need to clear the history -- otherwise, Electron may attempt to handle the
    // mouse forward + backward buttons and "navigate" to the previously-open project,
    // which will fail
    //
    // https://github.com/rstudio/rstudio/issues/11016
    this.window.webContents.on('did-navigate', (_event, _params) => {
      this.window.webContents.navigationHistory.clear();
    });
  }

  zoomActualSize(): void {
    this.setWindowZoomLevel(1);
  }

  setZoomLevel(zoomLevel: number): void {
    this.setWindowZoomLevel(zoomLevel);
  }

  zoomIn(): void {
    const zoomLevel = ElectronDesktopOptions().zoomLevel();

    // get next greatest value
    const newZoomLevel = nextHighest(zoomLevel, this.zoomLevels);
    if (newZoomLevel != zoomLevel) {
      this.setWindowZoomLevel(newZoomLevel);
    }
  }

  zoomOut(): void {
    // get next smallest value
    const zoomLevel = ElectronDesktopOptions().zoomLevel();
    const newZoomLevel = nextLowest(zoomLevel, this.zoomLevels);
    if (newZoomLevel != zoomLevel) {
      this.setWindowZoomLevel(newZoomLevel);
    }
  }

  abstract onActivated(): void;

  onCloseWindowShortcut(): void {
    // check to see if the window has desktop hooks (not all GWT windows do); if it does, check to
    // see whether it has a closeSourceDoc() command we should be executing instead
    this.executeJavaScript(
      `if (window.desktopHooks)
           window.desktopHooks.isCommandEnabled('closeSourceDoc');
         else false`,
    )
      .then((closeSourceDocEnabled) => {
        if (closeSourceDocEnabled as boolean) {
          // Explicitly invoke closeSourceDoc rather than relying on the
          // menu accelerator, since we now preventDefault() on Cmd+W in
          // before-input-event to avoid double-dispatch (rstudio#17115).
          // On macOS, route through $RStudio.last_focused_window so that
          // Cmd+W in the main window targets a satellite source window
          // when one was last focused.
          const cmd =
            process.platform === 'darwin'
              ? `var wnd;
                 try { wnd = window.$RStudio.last_focused_window; }
                 catch (e) { wnd = window; }
                 (wnd || window).desktopHooks.invokeCommand('closeSourceDoc');`
              : "window.desktopHooks.invokeCommand('closeSourceDoc')";
          this.executeJavaScript(cmd).catch((error) => logger().logError(error));
        } else {
          this.window.close();
        }
      })
      .catch((error) => logger().logError(error));
  }

  private setWindowZoomLevel(zoomLevel: number): void {
    ElectronDesktopOptions().setZoomLevel(zoomLevel);
    this.window.webContents.setZoomFactor(zoomLevel);
  }
}
