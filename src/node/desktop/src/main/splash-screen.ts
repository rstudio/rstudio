/*
 * splash-screen.ts
 *
 * Copyright (C) 2024 by Posit Software, PBC
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

import { BrowserWindow } from 'electron';
import { logger } from '../core/logger';

declare const SPLASH_WEBPACK_ENTRY: string;

export function createSplashScreen(): BrowserWindow {
  const splash = new BrowserWindow({
    width: 770,
    height: 440,
    frame: false,
    transparent: true,
    center: true,
    resizable: false,
    show: false,
    alwaysOnTop: true,
  });

  splash.loadURL(SPLASH_WEBPACK_ENTRY).catch((err: unknown) => logger().logError(err));
  return splash;
}

export function showPersistentSplashScreen(): void {
  const splash = createSplashScreen();
  splash.show();
}
