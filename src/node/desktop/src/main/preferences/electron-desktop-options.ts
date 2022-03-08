/**
 *
 * desktop-options.ts
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

import { BrowserWindow, Rectangle, screen } from 'electron';
import Store from 'electron-store';
import { logger } from '../../core/logger';
import { legacyPreferenceManager } from './../preferences/preferences';
import DesktopOptions from './desktop-options';

const kProportionalFont = 'Font.ProportionalFont';
const kFixedWidthFont = 'Font.FixedWidthFont';
const kUseFontConfigDb = 'Font.UseFontConfigDb';

const kZoomLevel = 'View.ZoomLevel';
const kWindowBounds = 'View.WindowBounds';
const kAccessibility = 'View.Accessibility';

const kLastRemoteSessionUrl = 'Session.LastRemoteSessionUrl';
const kAuthCookies = 'Session.AuthCookies';
const kTempAuthCookies = 'Session.TempAuthCookies';

const kIgnoredUpdateVersions = 'General.IgnoredUpdateVersions';
const kClipboardMonitoring = 'General.ClipboardMonitoring';

const kRBinDir = 'Platform.Windows.RBinDir';
const kPreferR64 = 'Platform.Windows.PreferR64';

export let defaultFonts = ['monospace'];

// exported for unit testing
export const kDesktopOptionDefaults = {
  Font: {
    ProportionalFont: '',
    FixedWidthFont: '',
    UseFontConfigDb: true,
  },
  View: {
    ZoomLevel: 1.0,
    WindowBounds: { width: 1200, height: 900 },
    Accessibility: false,
  },
  Session: {
    LastRemoteSessionUrl: '',
    AuthCookies: [],
    TempAuthCookies: [],
  },
  General: {
    IgnoredUpdateVersions: [],
    ClipboardMonitoring: true,
  },
  Platform: {
    Windows: {
      RBinDir: '',
      PreferR64: true,
    },
  },
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
export function ElectronDesktopOptions(directory = '', legacyOptions?: DesktopOptions): DesktopOptionsImpl {
  if (!options) {
    options = new DesktopOptionsImpl(directory, legacyOptions);
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
 * Check if one rectangle is inside (shared border inclusive) the other.
 *
 * @param inner The rectangle assumed to be the smaller, inner rectangle
 * @param outer The rectangle assumed to be the larger, outer rectangle
 *
 * @returns True if the inner rectangle is inside (shared border inclusive)
 * the outer rectangle, false otherwise
 */
export function firstIsInsideSecond(inner: Rectangle, outer: Rectangle): boolean {
  return (
    inner.x >= outer.x &&
    inner.y >= outer.y &&
    inner.x + inner.width <= outer.x + outer.width &&
    inner.y + inner.height <= outer.y + outer.height
  );
}

/**
 * Desktop Options class for storing/restoring user desktop options.
 * It will read from the new option location first. If the option is
 * not set then it will read from the legacy location.
 *
 * Exported for unit testing only, use the DesktopOptions() function
 * for creating/getting a DesktopOptionsImpl instance
 */
export class DesktopOptionsImpl {
  private config = new Store({ defaults: kDesktopOptionDefaults });
  private legacyOptions = legacyPreferenceManager;

  // unit testing constructor to expose directory and DesktopOptions mock
  constructor(directory = '', legacyOptions?: DesktopOptions) {
    if (directory.length != 0) {
      this.config = new Store({ defaults: kDesktopOptionDefaults, cwd: directory });
    }
    if (legacyOptions) {
      this.legacyOptions = legacyOptions;
    }
  }

  public setProportionalFont(font?: string): void {
    this.config.set(kProportionalFont, font ?? '');
  }

  public proportionalFont(): string {
    return this.config.get(kProportionalFont);
  }

  public setFixedWidthFont(fixedWidthFont: string): void {
    this.config.set(kFixedWidthFont, fixedWidthFont);
  }

  public fixedWidthFont(): string | undefined {
    let fontName = this.config.get<'Font.FixedWidthFont', string>(kFixedWidthFont);

    if (!fontName) {
      fontName = this.legacyOptions.fixedWidthFont() ?? '';
    }

    return fontName;
  }

  public setUseFontConfigDb(useFontConfigDb: boolean): void {
    this.config.set(kUseFontConfigDb, useFontConfigDb);
  }

  public useFontConfigDb(): boolean {
    return this.config.get(kUseFontConfigDb);
  }

  public setZoomLevel(zoom: number): void {
    this.config.set(kZoomLevel, zoom);
  }

  public zoomLevel(): number {
    return this.config.get(kZoomLevel);
  }

  public saveWindowBounds(bounds: Rectangle): void {
    this.config.set(kWindowBounds, bounds);
  }

  public windowBounds(): Rectangle {
    return this.config.get(kWindowBounds);
  }

  // Note: screen can only be used after the 'ready' event has been emitted
  public restoreMainWindowBounds(mainWindow: BrowserWindow): void {
    try {
      this.restoreMainWindowBoundsImpl(mainWindow);
    } catch (e: unknown) {
      logger().logErrorAtLevel('debug', e);
    }
  }

  private restoreMainWindowBoundsImpl(mainWindow: BrowserWindow): void {
    const savedBounds = this.windowBounds();

    // Check if saved bounds is still in one of the available displays
    const goodDisplays = screen.getAllDisplays().find((display) => {
      return firstIsInsideSecond(savedBounds, display.workArea);
    });

    // Restore it to previous location if possible, or center of primary display otherwise
    if (goodDisplays) {
      mainWindow.setBounds(savedBounds);
    } else {
      const primaryBounds = screen.getPrimaryDisplay().bounds;
      const newSize = {
        width: Math.min(kDesktopOptionDefaults.View.WindowBounds.width, primaryBounds.width),
        height: Math.min(kDesktopOptionDefaults.View.WindowBounds.height, primaryBounds.height),
      };

      mainWindow.setSize(newSize.width, newSize.height);

      // window.center() doesn't consistently pick the primary display,
      // so manually calculating the center of the primary display
      mainWindow.setPosition(
        primaryBounds.x + (primaryBounds.width - newSize.width) / 2,
        primaryBounds.y + (primaryBounds.height - newSize.height) / 2,
      );
    }

    // ensure a minimum size for the window on restore
    const currSize = mainWindow.getSize();
    mainWindow.setSize(Math.max(300, currSize[0]), Math.max(200, currSize[1]));
  }

  public setAccessibility(accessibility: boolean): void {
    this.config.set(kAccessibility, accessibility);
  }

  public accessibility(): boolean {
    return this.config.get(kAccessibility);
  }

  public setLastRemoteSessionUrl(lastRemoteSessionUrl: string): void {
    this.config.set(kLastRemoteSessionUrl, lastRemoteSessionUrl);
  }

  public lastRemoteSessionUrl(): string {
    return this.config.get(kLastRemoteSessionUrl);
  }

  public setAuthCookies(authCookies: string[]): void {
    this.config.set(kAuthCookies, authCookies);
  }

  public authCookies(): string[] {
    return this.config.get(kAuthCookies);
  }

  public setTempAuthCookies(tempAuthCookies: string[]): void {
    this.config.set(kTempAuthCookies, tempAuthCookies);
  }

  public tempAuthCookies(): string[] {
    return this.config.get(kTempAuthCookies);
  }

  public setIgnoredUpdateVersions(ignoredUpdateVersions: string[]): void {
    this.config.set(kIgnoredUpdateVersions, ignoredUpdateVersions);
  }

  public ignoredUpdateVersions(): string[] {
    return this.config.get(kIgnoredUpdateVersions);
  }

  public setClipboardMonitoring(clipboardMonitoring: boolean): void {
    this.config.set(kClipboardMonitoring, clipboardMonitoring);
  }

  public clipboardMonitoring(): boolean {
    return this.config.get(kClipboardMonitoring);
  }

  // Windows-only option
  public setRBinDir(rBinDir: string): void {
    if (process.platform !== 'win32') {
      return;
    }
    this.config.set(kRBinDir, rBinDir);
  }

  // Windows-only option
  public rBinDir(): string {
    if (process.platform !== 'win32') {
      return '';
    }
    return this.config.get(kRBinDir);
  }

  // Windows-only option
  public setPeferR64(peferR64: boolean): void {
    if (process.platform !== 'win32') {
      return;
    }
    this.config.set(kPreferR64, peferR64);
  }

  // Windows-only option
  public peferR64(): boolean {
    // Check if Windows, or if arch is x64, arm64, or ppc64
    if (process.platform !== 'win32' || !process.arch.includes('64')) {
      return false;
    }
    return this.config.get(kPreferR64);
  }
}

if (process.platform === 'darwin') {
  defaultFonts = ['Menlo', 'Monaco'];
} else if (process.platform === 'win32') {
  defaultFonts = ['Lucida Console', 'Consolas'];
} else {
  defaultFonts = ['Ubuntu Mono', 'Droid Sans Mono', 'DejaVu Sans Mono', 'Monospace'];
}
