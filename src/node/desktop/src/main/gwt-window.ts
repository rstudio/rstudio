/*
 * gwt-window.ts
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

import { WebContents } from 'electron';

import { nextHighest, nextLowest } from '../core/array-utils';

import { DesktopBrowserWindow } from './desktop-browser-window';


export class GwtWindow extends DesktopBrowserWindow {

  fakeZoomLevelPersistence = 1.0; // TODO: temporary fake zoom persistence

  // initialize zoom levels (synchronize with AppearancePreferencesPane.java)
  zoomLevels = [
    0.25, 0.50, 0.75, 0.80, 0.90,
    1.00, 1.10, 1.25, 1.50, 1.75,
    2.00, 2.50, 3.00, 4.00, 5.00];

  constructor(
    showToolbar: boolean,
    adjustTitle: boolean,
    name: string,
    baseUrl?: string,
    parent?: DesktopBrowserWindow,
    opener?: WebContents,
    isRemoteDesktop = false,
    addedCallbacks: string[] = []
  ) {
    super(showToolbar, adjustTitle, name, baseUrl, parent, opener, isRemoteDesktop, addedCallbacks);

    this.window.on('focus', this.onActivated.bind(this));
  }

  zoomActualSize(): void {
    this.fakeOptionsSetZoomLevel(1);
    this.setWindowZoomLevel(1);
  }

  setZoomLevel(zoomLevel: number): void {
    this.fakeOptionsSetZoomLevel(zoomLevel);
    this.setWindowZoomLevel(zoomLevel);
  }

  zoomIn(): void {
    const zoomLevel = this.fakeOptionsZoomLevel();

    // get next greatest value
    const newZoomLevel = nextHighest(zoomLevel, this.zoomLevels);
    if (newZoomLevel != zoomLevel) {
      this.fakeOptionsSetZoomLevel(newZoomLevel);
      this.setWindowZoomLevel(newZoomLevel);
    }
  }

  zoomOut(): void {
    // get next smallest value
    const zoomLevel = this.fakeOptionsZoomLevel();
    const newZoomLevel = nextLowest(zoomLevel, this.zoomLevels);
    if (newZoomLevel != zoomLevel) {
      this.fakeOptionsSetZoomLevel(newZoomLevel);
      this.setWindowZoomLevel(newZoomLevel);
    }
  }

  onActivated(): void {
    // override in subclasses
  }

  onCloseWindowShortcut(): void {
    // check to see if the window has desktop hooks (not all GWT windows do); if it does, check to
    // see whether it has a closeSourceDoc() command we should be executing instead
    this.executeJavaScript(
      `if (window.desktopHooks)
           window.desktopHooks.isCommandEnabled('closeSourceDoc');
         else false`)
      .then((closeSourceDocEnabled) => {
        if (!closeSourceDocEnabled.toBool()) {
          this.window.close();
        }
      });
  }

  private setWindowZoomLevel(zoomLevel: number): void {
    this.webView.webContents.setZoomFactor(zoomLevel);
  }

  // TODO: this is a placeholder for options().setZoomLevel() in the C++ code
  fakeOptionsSetZoomLevel(zoomLevel: number): void {
    this.fakeZoomLevelPersistence = zoomLevel;
  }

  // TODO: this is a placeholder for options().zoomLevel() in the C++ code
  fakeOptionsZoomLevel(): number {
    return this.fakeZoomLevelPersistence;
  }
}
