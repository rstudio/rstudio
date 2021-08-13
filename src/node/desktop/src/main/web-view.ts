/*
 * web-view.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import { shell, WebContents } from 'electron';
import { EventEmitter } from 'stream';

import { logger } from '../core/logger';

import { appState } from './app-state';
import { PendingWindow } from './pending-window';

// Porting note: this class mirrors DesktopWebPage and DesktopWebView in the C++ code.
export class WebView extends EventEmitter {
  static CLOSE_WINDOW_SHORTCUT = 'webview-close_window_shortcut';
  pendingWindows = new Array<PendingWindow>();

  constructor(
    public webContents: WebContents,
    public baseUrl?: string,
    public allowExternalNavigate: boolean = false
  ) {
    super();
    this.webContents.on('before-input-event', (event, input) => {
      this.keyPressEvent(event, input);
    });

    this.webContents.setWindowOpenHandler(this.createWindow.bind(this));
  }

  keyPressEvent(event: Electron.Event, input: Electron.Input): void {
    if (process.platform === 'darwin') {
      if (input.meta && input.key.toLowerCase() === 'w') {
        // on macOS, intercept Cmd+W and emit the window close signal
        this.emit(WebView.CLOSE_WINDOW_SHORTCUT);
        event.preventDefault();
      }
    }
  }

  prepareForWindow(pendingWindow: PendingWindow): void {
    this.pendingWindows.push(pendingWindow);
  }

  createWindow(details: Electron.HandlerDetails): { action: 'deny' } | { action: 'allow', overrideBrowserWindowOptions?: Electron.BrowserWindowConstructorOptions | undefined } {

    // check if this is target="_blank" from an IDE window
    if (this.baseUrl && (details.disposition === 'foreground-tab' || details.disposition === 'background-tab')) {
      void shell.openExternal(details.url);
      return { action: 'deny' };
    }

    // TODO: handle satellite windows and secondary-windows
    return { action: 'deny' };
  }
}