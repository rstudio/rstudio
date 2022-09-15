/*
 * window-tracker.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { DesktopBrowserWindow } from './desktop-browser-window';

/**
 * Tracks DesktopBrowserWindow objects by name.
 */
export class WindowTracker {
  private nameMap = new Map<string, DesktopBrowserWindow>();

  getWindow(key: string): DesktopBrowserWindow | undefined {
    return this.nameMap.get(key);
  }

  addWindow(key: string, browserWindow: DesktopBrowserWindow): void {
    this.nameMap.set(key, browserWindow);
    browserWindow.window.addListener('closed', () => {
      this.onWindowDestroyed(key);
    });
  }

  onWindowDestroyed(key: string): void {
    this.nameMap.delete(key);
  }

  length(): number {
    return this.nameMap.size;
  }
}
