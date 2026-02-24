/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 *
 * electron-desktop-options.ts
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

import { BrowserWindow } from 'electron';
import ElectronStore from 'electron-store';
import { statSync } from 'fs';
import { basename, dirname, join } from 'path';
import { properties } from '../../../../../cpp/session/resources/schema/user-state-schema.json';
import { normalizeSeparatorsNative } from '../../ui/utils';
import { logger } from '../../core/logger';
import { RStudioUserState } from '../../types/user-state-schema';

// Workaround for TypeScript not recognizing ElectronStore methods in CommonJS project
// See: https://github.com/sindresorhus/electron-store/issues/276
interface ElectronStoreInterface<T extends Record<string, any>> {
  get(key: string, defaultValue?: any): any;
  set(key: string, value: any): void;
  has(key: string): boolean;
  delete(key: string): void;
  clear(): void;
  store: T;
}

import { generateSchema, legacyPreferenceManager } from './../preferences/preferences';
import DesktopOptions from './desktop-options';
import { kWindowsRExe } from '../../ui/utils';
import { WindowBounds, positionAndEnsureVisible } from '../window-utils';

const kProportionalFont = 'font.proportionalFont';
const kFixedWidthFont = 'font.fixedWidthFont';

const kZoomLevel = 'view.zoomLevel';
const kWindowBounds = 'view.windowBounds';
const kAccessibility = 'view.accessibility';
const kEnableSplashScreen = 'view.enableSplashScreen';
const kDisableRendererAccessibility = 'view.disableRendererAccessibility';

const kIgnoredUpdateVersions = 'general.ignoredUpdateVersions';

const kRendererEngine = 'renderer.engine';
const kRendererUseGpuExclusionList = 'renderer.useGpuExclusionList';
const kRendererUseGpuDriverBugWorkarounds = 'renderer.useGpuDriverBugWorkarounds';

const kUseDefault32BitR = 'platform.windows.useDefault32BitR';
const kUseDefault64BitR = 'platform.windows.useDefault64BitR';
const kRExecutablePath = 'platform.windows.rExecutablePath';
const kPreferR64 = 'platform.windows.preferR64';

const kCheckForRosetta = 'platform.macos.checkForRosetta';

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
 * Desktop Options class for storing/restoring user desktop options.
 * It will read from the new option location first. If the option is
 * not set then it will read from the legacy location.
 *
 * Exported for unit testing only, use the DesktopOptions() function
 * for creating/getting a DesktopOptionsImpl instance
 */
export class DesktopOptionsImpl implements DesktopOptions {
  private config = new ElectronStore<RStudioUserState>({
    schema: userStateSchema,
  }) as unknown as ElectronStoreInterface<RStudioUserState>;
  private legacyOptions = legacyPreferenceManager;

  // unit testing constructor to expose directory and DesktopOptions mock
  constructor(directory = '', legacyOptions?: DesktopOptions) {
    if (directory.length != 0) {
      this.config = new ElectronStore<RStudioUserState>({
        cwd: directory,
        schema: userStateSchema,
      }) as unknown as ElectronStoreInterface<RStudioUserState>;
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
      const min = properties.view.properties.zoomLevel.minimum;
      const max = properties.view.properties.zoomLevel.maximum;
      if (zoomLevel < min || zoomLevel > max) {
        zoomLevel = properties.view.default.zoomLevel;
      }
      this.config.set(kZoomLevel, zoomLevel);
    }

    return zoomLevel;
  }

  public saveWindowBounds(bounds: WindowBounds): void {
    this.config.set(kWindowBounds, bounds);
  }

  public windowBounds(): WindowBounds {
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

    positionAndEnsureVisible(
      mainWindow,
      savedBounds,
      properties.view.default.windowBounds.width,
      properties.view.default.windowBounds.height,
    );

    if (savedBounds.maximized) {
      mainWindow.maximize();
    }
  }

  public setAccessibility(accessibility: boolean): void {
    this.config.set(kAccessibility, accessibility);
  }

  public accessibility(): boolean {
    return this.config.get(kAccessibility, properties.view.default.accessibility);
  }

  public setEnableSplashScreen(enabled: boolean): void {
    this.config.set(kEnableSplashScreen, enabled);
  }

  public enableSplashScreen(): boolean {
    return this.config.get(kEnableSplashScreen, properties.view.default.enableSplashScreen);
  }

  public setDisableRendererAccessibility(accessibility: boolean): void {
    this.config.set(kDisableRendererAccessibility, accessibility);
  }

  public disableRendererAccessibility(): boolean {
    return this.config.get(kDisableRendererAccessibility, properties.view.default.disableRendererAccessibility);
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

  // MacOS Apple Silicon-only option
  public setCheckForRosetta(value: boolean): void {
    const isAppleSilicon = process.platform === 'darwin' && process.arch === 'arm64';
    if (!isAppleSilicon) {
      return;
    }
    this.config.set(kCheckForRosetta, value);
  }

  // MacOS Apple Silicon-only option
  public checkForRosetta(): boolean {
    const isAppleSilicon = process.platform === 'darwin' && process.arch === 'arm64';
    if (!isAppleSilicon) {
      return false;
    }
    const checkForRosettaConfig = this.config.get(kCheckForRosetta, properties.platform.default.macos.checkForRosetta);
    logger().logDebug(`Desktop option 'checkForRosetta' is: ${checkForRosettaConfig}`);
    return checkForRosettaConfig;
  }

  // Windows-only option
  public rBinDir(): string {
    if (process.platform !== 'win32') {
      return '';
    }

    const rExecutablePath = this.rExecutablePath();

    if (!rExecutablePath || rExecutablePath === '') {
      return this.legacyOptions.rBinDir() ?? properties.platform.default.windows.rBinDir;
    } else {
      return dirname(rExecutablePath);
    }
  }

  // Windows-only options
  public useDefault32BitR(): boolean {
    return this.config.get(kUseDefault32BitR, false);
  }

  public setUseDefault32BitR(useDefault: boolean) {
    this.config.set(kUseDefault32BitR, useDefault);
  }

  // Windows-only option
  public useDefault64BitR(): boolean {
    return this.config.get(kUseDefault64BitR, false);
  }

  public setUseDefault64BitR(useDefault: boolean) {
    this.config.set(kUseDefault64BitR, useDefault);
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

    const rExecutablePath: string = this.config.get(
      kRExecutablePath,
      properties.platform.default.windows.rExecutablePath,
    );

    if (!rExecutablePath) {
      return '';
    }

    // 2022.12 and 2023.03 allowed the user to select bin\R.exe, which will cause sessions
    // to fail to load. We prevent that now, but fix it up if we encounter it.
    const fixedPath = fixWindowsRExecutablePath(rExecutablePath);
    if (fixedPath !== rExecutablePath) {
      this.setRExecutablePath(fixedPath);
    }

    return fixedPath;
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

/**
 * If user manually chooses bin\R.exe, sessions won't load, so insert the
 * architecture folder (i386 if they are on a 32-bit machine, otherwise x64).
 *
 * If they want to use 32-bit R on a 64-bit machine they will need to
 * choose it directly from the i386 folder.
 *
 * Use Rterm.exe instead of R.exe (or anything else the user might have
 * chosen; we can't prevent them from choosing arbitrary files).
 *
 * @param rExePath Full path to Rterm.exe
 * @returns Full path to Rterm.exe including arch folder
 */
export function fixWindowsRExecutablePath(rExePath: string): string {
  // skip on other platforms
  if (process.platform !== 'win32') {
    return rExePath;
  }

  // if we were given the path to a directory, or a non-existent file,
  // then just return the path as-is (unexpected)
  try {
    const info = statSync(rExePath);
    if (info.isDirectory()) {
      return rExePath;
    }
  } catch {
    return rExePath;
  }

  const selectedDir = basename(dirname(rExePath)).toLowerCase();
  const origPath = rExePath;
  if (selectedDir === 'bin') {
    // User picked bin\*.exe; insert the subfolder matching the machine's architecture.
    const archDir = process.arch === 'x64' ? 'x64' : 'i386';
    rExePath = join(dirname(rExePath), archDir, kWindowsRExe);
    logger().logDebug(`User selected ${origPath}, replacing with ${rExePath}`);
  } else {
    // Even if they chose the right folder, make sure they picked Rterm.exe
    const exe = basename(rExePath).toLowerCase();
    if (exe !== kWindowsRExe.toLowerCase()) {
      rExePath = join(dirname(rExePath), kWindowsRExe);
      logger().logDebug(`User selected ${origPath}, replacing with ${rExePath}`);
    }
  }
  return rExePath;
}
