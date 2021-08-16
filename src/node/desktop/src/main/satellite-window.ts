/*
 * satellite-window.ts
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

import { GwtWindow } from './gwt-window';
import { MainWindow } from './main-window';
import { appState } from './app-state';
import { DesktopBrowserWindow } from './desktop-browser-window';

export class SatelliteWindow extends GwtWindow {
  constructor(
    mainWindow: MainWindow,
    name: string,
    opener: WebContents,
    existingWindow?: BrowserWindow
  ) {
    super(false, true, name, undefined, undefined, opener,
      mainWindow.isRemoteDesktop, ['desktop'], existingWindow);
    appState().gwtCallback?.registerOwner(this);

    this.on(DesktopBrowserWindow.CLOSE_WINDOW_SHORTCUT, this.onCloseWindowShortcut.bind(this));
  }

  onActivated(): void {
    // TODO
  }
}
