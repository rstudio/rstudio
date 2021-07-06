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

import { WebContents } from 'electron';
import { EventEmitter } from 'stream';

export class WebView extends EventEmitter {
  static CLOSE_WINDOW_SHORTCUT = 'webview-close_window_shortcut';

  constructor(
    public webContents: WebContents,
    public baseUrl?: string,
    public allowExternalNavigate: boolean = false
  ) {
    super();
    this.webContents.on('before-input-event', (event, input) => {
      this.keyPressEvent(event, input);
    });
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
}