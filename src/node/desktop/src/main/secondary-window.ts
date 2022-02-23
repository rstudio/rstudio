/*
 * secondary-window.ts
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
import { DesktopBrowserWindow } from './desktop-browser-window';

export class SecondaryWindow extends DesktopBrowserWindow {
  constructor(
    showToolbar: boolean,
    name: string,
    baseUrl?: string,
    parent?: DesktopBrowserWindow,
    opener?: WebContents,
    allowExternalNavigate = false,
    existingWindow?: BrowserWindow,
  ) {
    super(showToolbar, true, name, baseUrl, parent, opener, allowExternalNavigate, undefined, existingWindow);

    this.on(DesktopBrowserWindow.CLOSE_WINDOW_SHORTCUT, this.onCloseWindowShortcut.bind(this));
  }

  onCloseWindowShortcut(): void {
    this.window.close();
  }

  /**
   * 
   * @returns Window creation request response
   */
  static windowOpening(width: number, height: number):
    | { action: 'deny' }
    | { action: 'allow'; overrideBrowserWindowOptions?: Electron.BrowserWindowConstructorOptions | undefined } {
      
    return {
      action: 'allow',
      overrideBrowserWindowOptions: {
        width: width,
        height: height,
        webPreferences: {
          additionalArguments: ['--apiKeys=desktopInfo'],
          preload: DesktopBrowserWindow.getPreload()
        }
      }
    }
  }
}
