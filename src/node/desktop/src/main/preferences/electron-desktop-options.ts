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

import { app, BrowserWindow } from 'electron';
import ElectronStore from 'electron-store';
import { renameSync, statSync } from 'fs';
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
import { WindowBounds, isWayland, kMinWindowHeight, kMinWindowWidth, positionAndEnsureVisible } from '../window-utils';

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

const kMacOSDefaultFileHandlerPrompted = 'platform.macos.defaultFileHandlerPrompted';
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

const kConfigFileName = 'config.json';

/**
 * Describes a config.json that could not be read and was moved aside so
 * that startup could continue with default desktop options.
 */
export interface ConfigRecovery {
  configPath: string;
  backupPath: string;
  reason: string;
}

// conf rethrows a JSON SyntaxError or its own schema violation for a file it
// cannot use; anything else (EACCES, ENOSPC, ...) is not a damaged file and
// must not be "recovered" from by discarding the user's settings
function isCorruptConfigError(error: unknown): error is Error {
  if (!(error instanceof Error)) {
    return false;
  }
  return error.name === 'SyntaxError' || error.message.startsWith('Config schema violation:');
}

// electron-store (conf) reads and validates config.json inside its constructor
// and rethrows a JSON SyntaxError or schema violation. The first read happens
// during startup, before the main window or even the Application singleton
// exist, so a file truncated by a crash or power loss used to abort the launch
// with no dialog (#19496). Move the damaged file aside, where it can still be
// inspected, and start from defaults instead.
function createConfigStore(cwd: string): {
  store: ElectronStoreInterface<RStudioUserState>;
  recovery?: ConfigRecovery;
} {
  const create = () =>
    new ElectronStore<RStudioUserState>({
      cwd,
      schema: userStateSchema,
    }) as unknown as ElectronStoreInterface<RStudioUserState>;

  try {
    return { store: create() };
  } catch (error: unknown) {
    if (!isCorruptConfigError(error)) {
      throw error;
    }

    const configPath = join(cwd, kConfigFileName);
    const reason = error.message;
    const backupPath = `${configPath}.corrupt-${new Date().toISOString().replace(/[:.]/g, '-')}`;
    try {
      renameSync(configPath, backupPath);
    } catch (renameError: unknown) {
      // a fresh store could not be written here either, so let the startup
      // error dialog say what needs fixing rather than failing twice
      const renameReason = renameError instanceof Error ? renameError.message : String(renameError);
      // tsconfig's es2021 lib has no Error `cause` option; the message carries it instead
      // eslint-disable-next-line preserve-caught-error
      throw new Error(`Could not read ${configPath} (${reason}), and could not move it aside (${renameReason}).`);
    }

    // the file logger and the Application do not exist yet; the caller reports
    // the recovery through those once they do, so only the console hears now
    console.warn(`Could not read ${configPath} (${reason}); moved it to ${backupPath}.`);

    // with the damaged file gone this is a fresh store
    return { store: create(), recovery: { configPath, backupPath, reason } };
  }
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
  private config: ElectronStoreInterface<RStudioUserState>;
  private recovery?: ConfigRecovery;
  private legacyOptions = legacyPreferenceManager;

  // unit testing constructor to expose directory and DesktopOptions mock
  constructor(directory = '', legacyOptions?: DesktopOptions) {
    const { store, recovery } = createConfigStore(directory.length != 0 ? directory : app.getPath('userData'));
    this.config = store;
    this.recovery = recovery;
    if (legacyOptions) {
      this.legacyOptions = legacyOptions;
    }
  }

  /**
   * How a damaged config.json was handled when these options were loaded,
   * or undefined if the file was read normally.
   */
  public configRecovery(): ConfigRecovery | undefined {
    return this.recovery;
  }

  // electron-store persists via write-temp + renameSync. That rename can race
  // against external cleanup of the user-data-dir (e.g. Playwright sandbox
  // teardown), surfacing as an ENOENT thrown synchronously out of config.set().
  // Because every call here happens on the main process, an unhandled throw
  // pops Electron's "JavaScript error in main process" modal. Settings are
  // best-effort persistence -- the read path falls back to defaults if the
  // value is missing -- so swallow the error and log instead.
  private safeSet(key: string, value: any): void {
    try {
      this.config.set(key, value);
    } catch (e: unknown) {
      logger().logErrorAtLevel(
        'debug',
        `Failed to persist desktop option '${key}': ${e instanceof Error ? e.message : String(e)}`,
      );
    }
  }

  public setProportionalFont(font?: string): void {
    this.safeSet(kProportionalFont, font ?? '');
  }

  public proportionalFont(): string {
    return this.config.get(kProportionalFont, '');
  }

  public setFixedWidthFont(fixedWidthFont: string): void {
    this.safeSet(kFixedWidthFont, fixedWidthFont);
  }

  public fixedWidthFont(): string | undefined {
    let fontName: string | undefined = this.config.get(kFixedWidthFont);

    if (!fontName) {
      fontName = this.legacyOptions.fixedWidthFont() ?? '';
      this.safeSet(kFixedWidthFont, fontName);
    }

    return fontName;
  }

  public setZoomLevel(zoom: number): void {
    const min = properties.view.properties.zoomLevel.minimum;
    const max = properties.view.properties.zoomLevel.maximum;
    if (zoom < min || zoom > max) {
      throw new Error(`Invalid zoom level: Must be between ${min} and ${max}`);
    }
    this.safeSet(kZoomLevel, zoom);
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
      this.safeSet(kZoomLevel, zoomLevel);
    }

    return zoomLevel;
  }

  public saveWindowBounds(bounds: WindowBounds): void {
    this.safeSet(kWindowBounds, bounds);
  }

  public windowBounds(): WindowBounds {
    return this.config.get(kWindowBounds, properties.view.default.windowBounds);
  }

  /**
   * The size to create the main window at: its saved unmaximized size, which
   * is also what it returns to when a maximized window is unmaximized.
   */
  public initialMainWindowSize(): { width: number; height: number } {
    const { width, height } = this.windowBounds();
    return {
      width: Math.max(kMinWindowWidth, width),
      height: Math.max(kMinWindowHeight, height),
    };
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

    // On Wayland, resizing the window before the compositor first configures
    // it races the maximize request, and can leave the window maximized in
    // name but drawn at its unmaximized size (#19136). The window was created
    // at its saved size (see initialMainWindowSize), and Wayland clients can't
    // place their windows anyway, so maximizing is all that's left to do.
    if (savedBounds.maximized && isWayland()) {
      mainWindow.maximize();
      return;
    }

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
    this.safeSet(kAccessibility, accessibility);
  }

  public accessibility(): boolean {
    return this.config.get(kAccessibility, properties.view.default.accessibility);
  }

  public setEnableSplashScreen(enabled: boolean): void {
    this.safeSet(kEnableSplashScreen, enabled);
  }

  public enableSplashScreen(): boolean {
    return this.config.get(kEnableSplashScreen, properties.view.default.enableSplashScreen);
  }

  public setDisableRendererAccessibility(accessibility: boolean): void {
    this.safeSet(kDisableRendererAccessibility, accessibility);
  }

  public disableRendererAccessibility(): boolean {
    return this.config.get(kDisableRendererAccessibility, properties.view.default.disableRendererAccessibility);
  }

  public setIgnoredUpdateVersions(ignoredUpdateVersions: string[]): void {
    this.safeSet(kIgnoredUpdateVersions, ignoredUpdateVersions);
  }

  public ignoredUpdateVersions(): string[] {
    return this.config.get(kIgnoredUpdateVersions, properties.general.default.ignoredUpdateVersions);
  }

  public setMacOSDefaultFileHandlerPrompted(value: boolean): void {
    this.safeSet(kMacOSDefaultFileHandlerPrompted, value);
  }

  public macOSDefaultFileHandlerPrompted(): boolean {
    return this.config.get(
      kMacOSDefaultFileHandlerPrompted,
      properties.platform.default.macos.defaultFileHandlerPrompted,
    );
  }

  public setRenderingEngine(renderingEngine: string): void {
    this.safeSet(kRendererEngine, renderingEngine);
  }

  public renderingEngine(): string {
    return this.config.get(kRendererEngine, 'auto');
  }

  public setUseGpuExclusionList(value: boolean) {
    this.safeSet(kRendererUseGpuExclusionList, value);
  }

  public useGpuExclusionList(): boolean {
    return this.config.get(kRendererUseGpuExclusionList, properties.renderer.default.useGpuExclusionList);
  }

  public setUseGpuDriverBugWorkarounds(value: boolean) {
    this.safeSet(kRendererUseGpuDriverBugWorkarounds, value);
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

    if (!rExecutablePath || rExecutablePath === '') {
      return this.legacyOptions.rBinDir() ?? properties.platform.default.windows.rBinDir;
    }

    return dirname(rExecutablePath);
  }

  // Windows-only options
  public useDefault32BitR(): boolean {
    return this.config.get(kUseDefault32BitR, false);
  }

  public setUseDefault32BitR(useDefault: boolean) {
    this.safeSet(kUseDefault32BitR, useDefault);
  }

  // Windows-only option
  public useDefault64BitR(): boolean {
    return this.config.get(kUseDefault64BitR, false);
  }

  public setUseDefault64BitR(useDefault: boolean) {
    this.safeSet(kUseDefault64BitR, useDefault);
  }

  // Windows-only option
  public setRExecutablePath(rExecutablePath: string): void {
    if (process.platform !== 'win32') {
      return;
    }

    this.safeSet(kRExecutablePath, normalizeSeparatorsNative(rExecutablePath));
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
    this.safeSet(kPreferR64, peferR64);
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
