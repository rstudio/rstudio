/**
 *
 * electron-desktop-options.ts
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { BrowserWindow, Rectangle, screen } from 'electron';
import Store from 'electron-store';
import { dirname } from 'path';
import { properties } from '../../../../../cpp/session/resources/schema/user-state-schema.json';
import { normalizeSeparatorsNative } from '../../core/file-path';
import { logger } from '../../core/logger';
import { RStudioUserState } from '../../types/user-state-schema';

import { generateSchema, legacyPreferenceManager } from './../preferences/preferences';
import DesktopOptions from './desktop-options';

const kProportionalFont = 'font.proportionalFont';
const kFixedWidthFont = 'font.fixedWidthFont';

const kZoomLevel = 'view.zoomLevel';
const kWindowBounds = 'view.windowBounds';
const kAccessibility = 'view.accessibility';

const kLastRemoteSessionUrl = 'session.lastRemoteSessionUrl';
const kAuthCookies = 'session.authCookies';
const kTempAuthCookies = 'session.tempAuthCookies';

const kIgnoredUpdateVersions = 'general.ignoredUpdateVersions';

const kRendererEngine = 'renderer.engine';
const kRendererUseGpuExclusionList = 'renderer.useGpuExclusionList';
const kRendererUseGpuDriverBugWorkarounds = 'renderer.useGpuDriverBugWorkarounds';

const kRExecutablePath = 'platform.windows.rExecutablePath';
const kPreferR64 = 'platform.windows.preferR64';

const userStateSchema = generateSchema<RStudioUserState>(properties);

export let defaultFonts = ['monospace'];

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
export class DesktopOptionsImpl implements DesktopOptions {
  private config = new Store<RStudioUserState>({ schema: userStateSchema });
  private legacyOptions = legacyPreferenceManager;

  // unit testing constructor to expose directory and DesktopOptions mock
  constructor(directory = '', legacyOptions?: DesktopOptions) {
    if (directory.length != 0) {
      this.config = new Store<RStudioUserState>({ cwd: directory, schema: userStateSchema });
    }
    if (legacyOptions) {
      this.legacyOptions = legacyOptions;
    }
  }

  public setProportionalFont(font?: string): void {
    this.config.set(kProportionalFont, font ?? '');
  }

  public proportionalFont(): string {
    return this.config.get(kProportionalFont, '');
  }

  public setFixedWidthFont(fixedWidthFont: string): void {
    this.config.set(kFixedWidthFont, fixedWidthFont);
  }

  public fixedWidthFont(): string | undefined {
    let fontName: string | undefined = this.config.get(kFixedWidthFont);

    if (!fontName) {
      fontName = this.legacyOptions.fixedWidthFont() ?? '';
      this.config.set(kFixedWidthFont, fontName);
    }

    return fontName;
  }

  public setZoomLevel(zoom: number): void {
    const min = properties.view.properties.zoomLevel.minimum;
    const max = properties.view.properties.zoomLevel.maximum;
    if (zoom < min || zoom > max) {
      throw new Error(`Invalid zoom level: Must be between ${min} and ${max}`);
    }
    this.config.set(kZoomLevel, zoom);
  }

  public zoomLevel(): number {
    let zoomLevel: number | undefined = this.config.get(kZoomLevel);

    if (!zoomLevel) {
      zoomLevel = this.legacyOptions.zoomLevel() ?? properties.view.default.zoomLevel;
      this.config.set(kZoomLevel, zoomLevel);
    }

    return zoomLevel;
  }

  public saveWindowBounds(bounds: Rectangle): void {
    this.config.set(kWindowBounds, bounds);
  }

  public windowBounds(): Rectangle {
    return this.config.get(kWindowBounds, properties.view.default.windowBounds);
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
        width: Math.min(properties.view.default.windowBounds.width, primaryBounds.width),
        height: Math.min(properties.view.default.windowBounds.height, primaryBounds.height),
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
    return this.config.get(kAccessibility, properties.view.default.accessibility);
  }

  public setLastRemoteSessionUrl(lastRemoteSessionUrl: string): void {
    this.config.set(kLastRemoteSessionUrl, lastRemoteSessionUrl);
  }

  public lastRemoteSessionUrl(): string {
    return this.config.get(kLastRemoteSessionUrl, properties.remote_session.default.lastRemoteSessionUrl);
  }

  public setAuthCookies(authCookies: string[]): void {
    this.config.set(kAuthCookies, authCookies);
  }

  public authCookies(): string[] {
    return this.config.get(kAuthCookies, properties.remote_session.default.authCookies);
  }

  public setTempAuthCookies(tempAuthCookies: string[]): void {
    this.config.set(kTempAuthCookies, tempAuthCookies);
  }

  public tempAuthCookies(): string[] {
    return this.config.get(kTempAuthCookies, properties.remote_session.default.tempAuthCookies);
  }

  public setIgnoredUpdateVersions(ignoredUpdateVersions: string[]): void {
    this.config.set(kIgnoredUpdateVersions, ignoredUpdateVersions);
  }

  public ignoredUpdateVersions(): string[] {
    return this.config.get(kIgnoredUpdateVersions, properties.general.default.ignoredUpdateVersions);
  }

  public setRenderingEngine(renderingEngine: string): void {
    this.config.set(kRendererEngine, renderingEngine);
  }

  public renderingEngine(): string {
    return this.config.get(kRendererEngine, 'desktop');
  }

  public setUseGpuExclusionList(value: boolean) {
    this.config.set(kRendererUseGpuExclusionList, value);
  }

  public useGpuExclusionList(): boolean {
    return this.config.get(kRendererUseGpuExclusionList, properties.renderer.default.useGpuExclusionList);
  }

  public setUseGpuDriverBugWorkarounds(value: boolean) {
    this.config.set(kRendererUseGpuDriverBugWorkarounds, value);
  }

  public useGpuDriverBugWorkarounds(): boolean {
    return this.config.get(kRendererUseGpuDriverBugWorkarounds, properties.renderer.default.useGpuDriverBugWorkarounds);
  }

  // Windows-only option
  public rBinDir(): string {
    if (process.platform !== 'win32') {
      return '';
    }

    const rExecutablePath = this.rExecutablePath();

    let rBinDir = '';

    if (!rExecutablePath || rExecutablePath === '') {
      rBinDir = this.legacyOptions.rBinDir() ?? properties.platform.default.windows.rBinDir;
    } else {
      rBinDir = dirname(rExecutablePath);
    }

    return rBinDir;
  }

  // Windows-only option
  public setRExecutablePath(rExecutablePath: string): void {
    if (process.platform !== 'win32') {
      return;
    }

    this.config.set(kRExecutablePath, normalizeSeparatorsNative(rExecutablePath));
  }

  // Windows-only option
  public rExecutablePath(): string {
    if (process.platform !== 'win32') {
      return '';
    }

    const rExecutablePath: string = this.config.get(kRExecutablePath,
      properties.platform.default.windows.rExecutablePath);

    if (!rExecutablePath) {
      return '';
    }

    return rExecutablePath;
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
    return this.config.get(kPreferR64, properties.platform.default.windows.preferR64);
  }
}

if (process.platform === 'darwin') {
  defaultFonts = ['Menlo', 'Monaco'];
} else if (process.platform === 'win32') {
  defaultFonts = ['Lucida Console', 'Consolas'];
} else {
  defaultFonts = ['Ubuntu Mono', 'Droid Sans Mono', 'DejaVu Sans Mono', 'Monospace'];
}
