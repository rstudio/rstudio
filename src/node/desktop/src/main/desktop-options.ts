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
import { MainWindow } from './main-window';
 
// exported for unit testing
export const kDesktopOptionDefaults = {
  zoomLevel: 1.0,
  windowBounds: {width: 1200, height: 900}
};
 
let options: DesktopOptionsImpl | null = null;
 
/**
 * Creates or returns the DesktopOptions singleton
 * 
 * @param directory Intended for unit testing only. The directory to 
 * place the config.json
 * 
 * @returns The DesktopOptions singleton
 */
export function DesktopOptions(directory = ''): DesktopOptionsImpl {
  if (!options) {
    options = new DesktopOptionsImpl(directory);
  }
  return options;
}
 
/**
  * Clear the options singleton. For unit testing only
  */
export function clearOptionsSingleton(): void {
  options = null;
}
 
/**
 * Desktop Options class for storing/restoring user desktop options.
 * 
 * Exported for unit testing only, use the DesktopOptions() function
 * for creating/getting a DesktopOptionsImpl instance
 */
export class DesktopOptionsImpl {
   private config = new Store({defaults: kDesktopOptionDefaults});
 
   // Directory exposed for unit testing
   constructor(directory = '') {
     if (directory.length != 0) {
       this.config = new Store({defaults: kDesktopOptionDefaults, cwd: directory});
     }
   }
 
   public setZoomLevel(zoom: number): void {
     this.config.set('zoomLevel', zoom);
   }
 
   public zoomLevel(): number {
     return this.config.get('zoomLevel');
   }
 
   public saveWindowBounds(size: {width: number, height: number}): void {
     this.config.set('windowBounds', size);
   }
 
   public windowBounds(): {width: number, height: number} {
     return this.config.get('windowBounds');
   }

   public restoreMainWindowBounds(mainWindow: MainWindow): void {
     const size = this.windowBounds(); 
     mainWindow.window.setSize(Math.max(300, size.width) , Math.max(200, size.height));
   }
}