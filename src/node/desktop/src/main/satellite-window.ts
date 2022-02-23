/*
 * satellite-window.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { BrowserWindow, WebContents } from 'electron';
import { logger } from '../core/logger';

import { GwtWindow } from './gwt-window';
import { MainWindow } from './main-window';
import { appState } from './app-state';
import { DesktopBrowserWindow } from './desktop-browser-window';

const SOURCE_WINDOW_PREFIX = '_rstudio_satellite_source_window_';

type CloseStage = 'CloseStageOpen' | 'CloseStagePending' | 'CloseStageAccepted';

export class SatelliteWindow extends GwtWindow {
  closeStage: CloseStage = 'CloseStageOpen';

  constructor(mainWindow: MainWindow, name: string, opener: WebContents, existingWindow?: BrowserWindow) {
    super(false, true, name, undefined, undefined, opener, mainWindow.isRemoteDesktop, ['desktop'], existingWindow);
    appState().gwtCallback?.registerOwner(this);

    this.on(DesktopBrowserWindow.CLOSE_WINDOW_SHORTCUT, this.onCloseWindowShortcut.bind(this));

    // TODO
    // satellites don't have a menu, so connect zoom keyboard shortcuts directly
  }

  onActivated(): void {
    this.executeJavaScript(
      'if (window.notifyRStudioSatelliteReactivated) ' + '  window.notifyRStudioSatelliteReactivated(null);',
    ).catch(logger().logError);
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  closeSatellite(event: Electron.Event): void {
    this.executeJavaScript(
      'if (window.notifyRStudioSatelliteClosing) ' + '  window.notifyRStudioSatelliteClosing();',
    ).catch(logger().logError);
  }

  closeEvent(event: Electron.Event): void {
    // the source window has special close semantics; if we're not currently closing, then invoke
    // custom close handlers.
    //
    // TODO: not sure how to determine "spontaneous" with Electron
    // we only do this for spontaneous (user-initiated) closures; when the window gets shut down by
    // its parent or by the OS, we don't prompt since in those cases unsaved document accumulation
    // and prompting is handled by the parent.
    if (
      this.name.startsWith(SOURCE_WINDOW_PREFIX) &&
      this.closeStage === 'CloseStageOpen' /*&& event->spontaneous()*/
    ) {
      // ignore this event; we need to make sure the window can be closed ourselves
      event.preventDefault();
      this.closeStage = 'CloseStagePending';

      this.executeJavaScript('window.rstudioReadyToClose')
        .then((readyToClose: boolean) => {
          if (readyToClose) {
            this.closeStage = 'CloseStageAccepted';
            this.window.close();
          } else {
            // not ready to close, revert close stage and take care of business
            this.closeStage = 'CloseStageOpen';
            this.executeJavaScript('window.rstudioCloseSourceWindow()').catch(logger().logError);
          }
        })
        .catch(logger().logError);
    } else {
      // not a  source window, just close it
      this.closeSatellite(event);
    }
  }

  /**
   *
   * @returns Window creation request response
   */
  static windowOpening():
    | { action: 'deny' }
    | { action: 'allow'; overrideBrowserWindowOptions?: Electron.BrowserWindowConstructorOptions | undefined } {
    return {
      action: 'allow',
      overrideBrowserWindowOptions: {
        autoHideMenuBar: true,
        webPreferences: {
          additionalArguments: ['--apiKeys=desktopInfo|desktop'],
          preload: DesktopBrowserWindow.getPreload(),
        },
      },
    };
  }
}
