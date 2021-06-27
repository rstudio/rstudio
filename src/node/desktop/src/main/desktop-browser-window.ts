/*
 * desktop-browser-window.ts
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

import { BrowserWindow, WebContents } from 'electron';

import { URL } from 'url';
import path from 'path';

/**
 * Base class for browser-based windows. Subclasses include GwtWindow, SecondaryWindow,
 * SatelliteWindow, and MainWindow.
 */
export class DesktopBrowserWindow {
  window?: BrowserWindow;

  /**
   * @param adjustTitle 
   * @param name 
   * @param baseUrl 
   * @param parent 
   * @param opener 
   * @param allowExternalNavigate 
   */
  constructor(
    private adjustTitle: boolean,
    private name: string,
    private baseUrl?: URL,
    private parent?: DesktopBrowserWindow,
    private opener?: WebContents,
    private allowExternalNavigate = false
  ) {
    this.window = new BrowserWindow({
      // https://github.com/electron/electron/blob/master/docs/faq.md#the-font-looks-blurry-what-is-this-and-what-can-i-do
      backgroundColor: '#fff', 
      webPreferences: {
        enableRemoteModule: false,
        nodeIntegration: false,
        contextIsolation: true,
        additionalArguments: ['desktopInfo'],
        preload: path.join(__dirname, '../renderer/preload.js'),
      },
      show: false
    });
  }
}