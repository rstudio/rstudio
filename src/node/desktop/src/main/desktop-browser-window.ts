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
import { EventEmitter } from 'stream';
import { logger } from '../core/logger';
import { executeJavaScript } from './utils';
import { WebView } from './web-view';

/**
 * Base class for browser-based windows. Subclasses include GwtWindow, SecondaryWindow,
 * SatelliteWindow, and MainWindow.
 * 
 * Porting note: This corresponds to a combination of the QMainWindow/BrowserWindow and 
 * QWebEngineView/WebView in the Qt desktop app.
 */
export class DesktopBrowserWindow extends EventEmitter {
  static WINDOW_DESTROYED = 'desktop-browser-window_destroyed';

  window: BrowserWindow;
  webView: WebView;

  /**
   * @param adjustTitle Automatically set window title to match web page title
   * @param name  Internal window name (or an empty string)
   * @param baseUrl 
   * @param parent 
   * @param opener 
   * @param allowExternalNavigate 
   * @param addApiKeys
   */
  constructor(
    private showToolbar: boolean,
    private adjustTitle: boolean,
    protected name: string,
    private baseUrl?: string,
    private parent?: DesktopBrowserWindow,
    private opener?: WebContents,
    private allowExternalNavigate = false,
    addApiKeys: string[] = []
  ) {
    super();
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

    this.webView = new WebView(this.window.webContents, baseUrl, allowExternalNavigate);
    this.webView.webContents.on('page-title-updated', (event, title, explicitSet) => {
      this.adjustWindowTitle(title, explicitSet);
    });
    this.webView.webContents.on('did-finish-load', () => {
      this.finishLoading(true);
    });
    this.webView.webContents.on('did-fail-load', () => {
      this.finishLoading(false);
    });
    this.window.on('close', this.closeEvent.bind(this));
    this.window.on('closed', () => {
      this.emit(DesktopBrowserWindow.WINDOW_DESTROYED);
    });

    // set zoom factor
    // TODO: double zoomLevel = options().zoomLevel();
    const zoomLevel = 1.0;
    this.webView.webContents.setZoomFactor(zoomLevel);

    if (this.showToolbar) {
      logger().logDebug('toolbar NYI');
      // TODO: add another BrowserView to hold an HTML-based toolbar?
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  closeEvent(event: Electron.Event): void {
    if (!this.opener) {
      // if we don't know where we were opened from, check window.opener
      // (note that this could also be empty)
      const cmd =
        `if (window.opener && window.opener.unregisterDesktopChildWindow)
           window.opener.unregisterDesktopChildWindow('${this.name}');`;
      this.executeJavaScript(cmd).catch((error) => {
        logger().logError(error);
      });
    } else {
      // if we do know where we were opened from and it has the appropriate
      // handlers, let it know we're closing
      const cmd =
        `if (window.unregisterDesktopChildWindow)
           window.unregisterDesktopChildWindow('${this.name}');`;
      this.executeJavaScript(cmd).catch((error) => {
        logger().logError(error);
      });
    }

    // forward the close event to the page
    // TODO
    // webPage()->event(event);
  }

  adjustWindowTitle(title: string, explicitSet: boolean): void {
    if (this.adjustTitle && explicitSet) {
      this.window.setTitle(title);
    }
  }

  syncWindowTitle(): void {
    if (this.adjustTitle) {
      this.window.setTitle(this.webView.webContents.getTitle());
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
      this.executeJavaScript(cmd).catch((error) => {
        logger().logError(error);
      });
    }
  }

  avoidMoveCursorIfNecessary(): void {
    if (process.platform === 'darwin') {
      this.executeJavaScript('document.body.className = document.body.className + \' avoid-move-cursor\'')
        .catch((error) => {
          logger().logError(error);
        });
    }
  }

  /**
   * Execute javascript in this window's page
   * 
   * @param cmd javascript to execute in this window
   * @returns promise with result of execution
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  executeJavaScript(cmd: string): Promise<any> {
    return executeJavaScript(this.webView.webContents, cmd);
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  setViewerUrl(url: string): void {
    // TODO: in the Qt version this is implemented in webPage()
  }
}