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
import { GwtWindow } from './gwt-window';

export function openMinimalWindow(
  sender: GwtWindow,
  name: string,
  url: string,
  width: number,
  height: number
): DesktopBrowserWindow {

  const named = !!name && name !== '_blank';

  let browser: DesktopBrowserWindow|undefined = undefined;
  if (named) {
    browser = appState().windowTracker.getWindow(name);
  }

  if (!browser) {
    const isViewerZoomWindow = (name === '_rstudio_viewer_zoom');

    // create the new browser window; pass along our own base URL so that the new window's
    // WebProfile knows how to apply the appropriate headers
    browser = new DesktopBrowserWindow(
      false,
      !isViewerZoomWindow,
      name,
      '', // TODO pMainWindow_->webView()->baseUrl()
      sender,
      undefined /* opener */);

    // ensure minimal windows can be closed with Ctrl+W (Cmd+W on macOS)
    browser.window.webContents.on('before-input-event', (event, input) => {
      const ctrlOrMeta = (process.platform === 'darwin') ? input.meta : input.control;
      if (ctrlOrMeta && input.key.toLowerCase() === 'w') {
        event.preventDefault();
        browser?.window.close();
      }
    });

    // ensure minimal window closes when creating window closes
    sender.window.on('closed', () => {
      browser?.window.close();
    });

    if (named) {
      appState().windowTracker.addWindow(name, browser);
    }

    // set title for viewer zoom
    if (isViewerZoomWindow) {
      browser.window.setTitle('Viewer Zoom');
    }
  }

  browser.window.loadURL(url);
  browser.window.setSize(width, height);
  return browser;
}