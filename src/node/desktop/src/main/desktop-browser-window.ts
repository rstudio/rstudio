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

import path from 'path';
import { logger } from '../core/logger';

/**
 * Base class for browser-based windows. Subclasses include GwtWindow, SecondaryWindow,
 * SatelliteWindow, and MainWindow.
 * 
 * Porting note: This corresponds to a combination of the QMainWindow/BrowserWindow and 
 * QWebEngineView/WebView in the Qt desktop app.
 */
export class DesktopBrowserWindow {
  window: BrowserWindow;

  /**
   * @param adjustTitle Automatically set window title to match web page title
   * @param name 
   * @param baseUrl 
   * @param parent 
   * @param opener 
   * @param allowExternalNavigate 
   * @param addApiKeys
   */
  constructor(
    private showToolbar: boolean,
    private adjustTitle: boolean,
    private name: string,
    private baseUrl?: string,
    private parent?: DesktopBrowserWindow,
    private opener?: WebContents,
    private allowExternalNavigate = false,
    addApiKeys: string[] = []
  ) {
    const apiKeys = [['desktopInfo', ...addApiKeys].join('|')];
    this.window = new BrowserWindow({
      // https://github.com/electron/electron/blob/master/docs/faq.md#the-font-looks-blurry-what-is-this-and-what-can-i-do
      backgroundColor: '#fff', 
      webPreferences: {
        enableRemoteModule: false,
        nodeIntegration: false,
        contextIsolation: true,
        additionalArguments: apiKeys,
        preload: path.join(__dirname, '../renderer/preload.js'),
      },
      show: false
    });

    this.window.webContents.on('page-title-updated', (event, title, explicitSet) => {
      this.adjustWindowTitle(title, explicitSet);
    });
    this.window.webContents.on('did-finish-load', () => {
      this.finishLoading(true);
    });
    this.window.webContents.on('did-fail-load', () => {
      this.finishLoading(false);
    });

    // set zoom factor
    // TODO: double zoomLevel = options().zoomLevel();
    const zoomLevel = 1.0;
    this.window.webContents.setZoomFactor(zoomLevel);

    if (this.showToolbar) {
      logger().logDebug('toolbar NYI');
      // TODO: add another BrowserView to hold an HTML-based toolbar?
    }
  }

  adjustWindowTitle(title: string, explicitSet: boolean): void {
    if (this.adjustTitle && explicitSet) {
      this.window.setTitle(title);
    }
  }

  syncWindowTitle(): void {
    if (this.adjustTitle) {
      this.window.setTitle(this.window.webContents.getTitle());
    }
  }

  finishLoading(succeeded: boolean): void {
    logger().logDebug(`window finished loading: success=${succeeded}`);
    this.syncWindowTitle();

    if (succeeded) {
      // TODO: Qt version sets up a tiny resize of the window here in response to the
      // window being shown on a different screen. Need to test if this is necessary.

      const cmd =
        `if (window.opener && window.opener.registerDesktopChildWindow)
         window.opener.registerDesktopChildWindow('${this.name}', window);`;
      this.window.webContents.executeJavaScript(cmd);
    }
  }

  avoidMoveCursorIfNecessary(): void {
    if (process.platform === 'darwin') {
      this.window.webContents.executeJavaScript('document.body.className = document.body.className + \' avoid-move-cursor\'');
    }
  }
}