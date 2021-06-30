/*
 * minimal-window.ts
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

import { appState } from './app-state';
import { DesktopBrowserWindow } from './desktop-browser-window';
import { MainWindow } from './main-window';

export function openMinimalWindow(
  name: string,
  url: string,
  width: number,
  height: number,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  mainWindow: MainWindow
): DesktopBrowserWindow {

  const urlStr = url.toString();
  const named = !!name && name !== '_blank';

  let browser: DesktopBrowserWindow|undefined = undefined;
  if (named) {
    browser = appState().windowTracker.getWindow(name);
  }

  if (!browser) {
    const isViewerZoomWindow = name === '_rstudio_viewer_zoom';

    // TODO
    // create the new browser window; pass along our own base URL so that the new window's
    // WebProfile knows how to apply the appropriate headers
    browser = new DesktopBrowserWindow(!isViewerZoomWindow, name,
      // TODO
      // pMainWindow_->webView()->baseUrl(), nullptr, pMainWindow_->webPage());
      undefined);
      
    // TODO  https://www.electronjs.org/docs/tutorial/keyboard-shortcuts#shortcuts-within-a-browserwindow
    //     // ensure minimal windows can be closed with Ctrl+W (Cmd+W on macOS)
    //     QAction* closeWindow = new QAction(browser);
    //     closeWindow->setShortcut(Qt::CTRL + Qt::Key_W);
    //     connect(closeWindow, &QAction::triggered,
    //             browser, &BrowserWindow::close);
    //     browser->addAction(closeWindow);
      
    //     connect(this, &GwtCallback::destroyed, browser, &BrowserWindow::close);
      
    if (named) {
      appState().windowTracker.addWindow(name, browser);
    }

    // set title for viewer zoom
    if (isViewerZoomWindow) {
      browser.window.setTitle('Viewer Zoom');
    }
  }

  browser.window.loadURL(urlStr);
  browser.window.setSize(width, height);
  return browser;
}