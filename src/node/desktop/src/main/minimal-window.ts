/*
 * minimal-window.ts
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

import { WebContents } from 'electron';

import { appState } from './app-state';
import { DesktopBrowserWindow } from './desktop-browser-window';
import { GwtWindow } from './gwt-window';

class MinimalWindow extends DesktopBrowserWindow {
  constructor(
    adjustTitle: boolean,
    name: string,
    baseUrl?: string,
    parent?: DesktopBrowserWindow,
    opener?: WebContents,
    allowExternalNavigate = false,
  ) {
    super(false, adjustTitle, true/*autohideMenu*/, name, baseUrl, parent, opener, allowExternalNavigate);

    // ensure minimal windows can be closed with Ctrl+W (Cmd+W on macOS)
    this.window.webContents.on('before-input-event', (event, input) => {
      const ctrlOrMeta = process.platform === 'darwin' ? input.meta : input.control;
      if (ctrlOrMeta && input.key.toLowerCase() === 'w') {
        event.preventDefault();
        this.window.close();
      }
    });

    // ensure this window closes when the creating window closes
    this.parentWindowDestroyed = this.parentWindowDestroyed.bind(this);
    parent?.on(DesktopBrowserWindow.WINDOW_DESTROYED, this.parentWindowDestroyed);

    this.window.on('close', () => {
      // release external event listeners
      parent?.removeListener(DesktopBrowserWindow.WINDOW_DESTROYED, this.parentWindowDestroyed);
    });
  }

  parentWindowDestroyed(): void {
    this.window.close();
  }
}

export function openMinimalWindow(
  sender: GwtWindow,
  name: string,
  url: string,
  width: number,
  height: number,
): DesktopBrowserWindow {
  const named = !!name && name !== '_blank';

  let browser: DesktopBrowserWindow | undefined = undefined;
  if (named) {
    browser = appState().windowTracker.getWindow(name);
  }

  if (!browser) {
    const isViewerZoomWindow = name === '_rstudio_viewer_zoom';

    // pass along our own base URL so that the new window's WebProfile knows how to
    // apply the appropriate headers
    browser = new MinimalWindow(
      !isViewerZoomWindow,
      name,
      '', // TODO pMainWindow_->webView()->baseUrl()
      sender,
      undefined /* opener */,
    );

    if (named) {
      appState().windowTracker.addWindow(name, browser);
    }

    // set title for viewer zoom
    if (isViewerZoomWindow) {
      browser.window.setTitle('Viewer Zoom');
    }
  }

  void browser.window.loadURL(url);
  browser.window.setSize(width, height);
  return browser;
}
