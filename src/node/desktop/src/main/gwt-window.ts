/*
 * gwt-window.ts
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
import { logger } from '../core/logger';

import { nextHighest, nextLowest } from '../core/array-utils';

import { DesktopBrowserWindow } from './desktop-browser-window';
import { DesktopOptions } from './desktop-options';

export abstract class GwtWindow extends DesktopBrowserWindow {
  // initialize zoom levels (synchronize with AppearancePreferencesPane.java)
  zoomLevels = [0.25, 0.5, 0.75, 0.8, 0.9, 1.0, 1.1, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0, 4.0, 5.0];

  constructor(
    showToolbar: boolean,
    adjustTitle: boolean,
    name: string,
    baseUrl?: string,
    parent?: DesktopBrowserWindow,
    opener?: WebContents,
    isRemoteDesktop = false,
    addedCallbacks: string[] = [],
    existingWindow?: BrowserWindow,
  ) {
    super(showToolbar, adjustTitle, name, baseUrl, parent, opener, isRemoteDesktop, addedCallbacks, existingWindow);

    this.window.on('focus', this.onActivated.bind(this));
  }

  zoomActualSize(): void {
    this.setWindowZoomLevel(1);
  }

  setZoomLevel(zoomLevel: number): void {
    this.setWindowZoomLevel(zoomLevel);
  }

  zoomIn(): void {
    const zoomLevel = DesktopOptions().zoomLevel();

    // get next greatest value
    const newZoomLevel = nextHighest(zoomLevel, this.zoomLevels);
    if (newZoomLevel != zoomLevel) {
      this.setWindowZoomLevel(newZoomLevel);
    }
  }

  zoomOut(): void {
    // get next smallest value
    const zoomLevel = DesktopOptions().zoomLevel();
    const newZoomLevel = nextLowest(zoomLevel, this.zoomLevels);
    if (newZoomLevel != zoomLevel) {
      this.setWindowZoomLevel(newZoomLevel);
    }
  }

  abstract onActivated(): void;

  onCloseWindowShortcut(): void {
    // check to see if the window has desktop hooks (not all GWT windows do); if it does, check to
    // see whether it has a closeSourceDoc() command we should be executing instead
    this.executeJavaScript(
      `if (window.desktopHooks)
           window.desktopHooks.isCommandEnabled('closeSourceDoc');
         else false`,
    )
      .then((closeSourceDocEnabled) => {
        if (!(closeSourceDocEnabled as boolean)) {
          this.window.close();
        }
      })
      .catch((error) => logger().logError(error));
  }

  private setWindowZoomLevel(zoomLevel: number): void {
    DesktopOptions().setZoomLevel(zoomLevel);
    this.window.webContents.setZoomFactor(zoomLevel);
  }
}
