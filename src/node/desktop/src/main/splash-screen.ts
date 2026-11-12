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
import { startupCheckpoint } from './startup-timing';
import { isAutomated } from './utils';

declare const SPLASH_WEBPACK_ENTRY: string;

export function createSplashScreen(): BrowserWindow {
  const splash = new BrowserWindow({
    width: 770,
    height: 440,
    frame: false,
    transparent: true,
    center: true,
    resizable: false,
    roundedCorners: false,
    show: false,
    alwaysOnTop: true,
    hasShadow: false,
  });

  splash.loadURL(SPLASH_WEBPACK_ENTRY).catch((err: unknown) => logger().logError(err));
  return splash;
}

function showSplashScreen(splash: BrowserWindow): void {
  if (isAutomated()) {
    splash.showInactive();
  } else {
    splash.show();
  }
}

export function showPersistentSplashScreen(): void {
  showSplashScreen(createSplashScreen());
}

/**
 * The splash screen shown while RStudio starts. It is there for a launch that
 * takes a while: it waits `delayMs` before appearing, is skipped when the main
 * window is on screen by then, and goes away as soon as the workbench is ready
 * or a dialog needs the user's attention.
 */
export class StartupSplash {
  private window: BrowserWindow | undefined;
  private showTimer: NodeJS.Timeout | undefined;
  private lingerTimer: NodeJS.Timeout | undefined;

  constructor(
    private readonly delayMs: number,
    private readonly lingerMs: number,
    private readonly createWindow: () => BrowserWindow = createSplashScreen,
  ) {}

  /**
   * Shows the splash once `delayMs` have passed, unless the main window is
   * shown, or the splash is closed, before then.
   */
  schedule(): void {
    this.showTimer = setTimeout(() => {
      this.showTimer = undefined;

      try {
        this.window = this.createWindow();
        showSplashScreen(this.window);
        startupCheckpoint('splash-shown');
      } catch (err: unknown) {
        logger().logError(err);
      }
    }, this.delayMs);
  }

  /**
   * The main window is on screen. A splash that has yet to appear is skipped;
   * one that is up stays for `lingerMs` more, so it doesn't flash by unread.
   */
  onMainWindowShown(): void {
    clearTimeout(this.showTimer);
    this.showTimer = undefined;

    if (this.window && this.lingerTimer === undefined) {
      this.lingerTimer = setTimeout(() => this.close(), this.lingerMs);
    }
  }

  /** Closes the splash now, if it is up, and keeps it from appearing later. */
  close(): void {
    clearTimeout(this.showTimer);
    clearTimeout(this.lingerTimer);
    this.showTimer = undefined;
    this.lingerTimer = undefined;

    const window = this.window;
    this.window = undefined;
    if (window && !window.isDestroyed()) {
      // not close(), which finishes on a later turn of the event loop -- one
      // that a modal dialog shown right after this call would hold up
      window.destroy();
      startupCheckpoint('splash-closed');
    }
  }
}
