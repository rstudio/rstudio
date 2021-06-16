/*
 * main.ts
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

import { BrowserWindow } from 'electron';

export class Main {
  mainWindow: BrowserWindow | null = null;

  /**
   * Invoked when app 'ready' is received
   */
  run(): void {
    // TEMPORARY, show a window so starting the app does something visible
    this.mainWindow = new BrowserWindow({
      width: 1024,
      height: 768,
      backgroundColor: '#fff', // https://github.com/electron/electron/blob/master/docs/faq.md#the-font-looks-blurry-what-is-this-and-what-can-i-do
      webPreferences: {
        enableRemoteModule: false,
        nodeIntegration: false,
        contextIsolation: true
      }

    });
    this.mainWindow.loadURL('https://rstudio.com');
  }
}
