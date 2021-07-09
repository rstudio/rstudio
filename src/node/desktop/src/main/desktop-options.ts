/**
 * 
 * desktop-options.ts
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

import Store from 'electron-store';

// exported for unit testing
export const kDesktopOptionDefaults = {
  zoomLevel: 1.0,
  windowBounds: {width: 1200, height: 900}
};

export class DesktopOptions {
  private _config = new Store({defaults: kDesktopOptionDefaults});

  // Filename exposed for unit testing
  constructor(filename = '') {
    if (filename.length != 0) {
      this._config = new Store({defaults: kDesktopOptionDefaults, cwd: filename});
    }
  }

  public setZoomLevel(zoom: number): void {
    this._config.set('zoomLevel', zoom);
  }

  public zoomLevel(): number {
    return this._config.get('zoomLevel');
  }

  public setWindowBounds(size: {width: number, height: number}): void {
    this._config.set('windowBounds', size);
  }

  public windowBounds(): {width: number, height: number} {
    return this._config.get('windowBounds');
  }
}