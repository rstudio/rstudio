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
 
const kProportionalFont = 'Font.Proportional';
const kFixWidthFont = 'Font.FixedWidth';
const kUseFontConfigDb = 'Font.UseFontConfigDatabase';

const kWindowBounds = 'View.WindowBounds';
const kZoomLevel = 'View.ZoomLevel';
const kAccessibility = 'View.EnableAccessibility';

const kLastRemoteSessionUrl = 'Session.LastRemoteSessionUrl';
const kAuthCookies = 'Session.AuthCookies';
const kTempAuthCookies = 'Session.TempAuthCookies';

const kIgnoredUpdateVersions = 'General.IgnoredUpdateVersions';
const kClipboardMonitoring = 'General.ClipboardMonitoring';

const kRBinDir = 'WindowsOnly.RBinDir';
const kPeferR64 = 'WindowsOnly.PreferR64';

// exported for unit testing
export const kDesktopOptionDefaults = {
  ZoomLevel: 1.0,
  WindowBounds: {width: 1200, height: 900}
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

  public setProportionalFont(font: string): void {
    this.config.set(kProportionalFont, font);
  }

  public proportionalFont(): string {
    return this.config.get(kProportionalFont);
  }

  public setZoomLevel(zoom: number): void {
    this.config.set(kZoomLevel, zoom);
  }

  public zoomLevel(): number {
    return this.config.get(kZoomLevel);
  }

  public saveWindowBounds(size: {width: number, height: number}): void {
    this.config.set(kWindowBounds, size);
  }

  public windowBounds(): {width: number, height: number} {
    return this.config.get(kWindowBounds);
  }

  public restoreMainWindowBounds(mainWindow: MainWindow): void {
    const size = this.windowBounds(); 
    mainWindow.window.setSize(Math.max(300, size.width) , Math.max(200, size.height));
  }
  
  public setFixWidthFont(fixWidthFont: string): void {
    this.config.set(kFixWidthFont, fixWidthFont);
  }
  
  public fixWidthFont(): string {
    return this.config.get(kFixWidthFont);
  }
  
  public setUseFontConfigDb(useFontConfigDb: boolean): void {
    this.config.set(kUseFontConfigDb, useFontConfigDb);
  }
  
  public useFontConfigDb(): boolean {
    return this.config.get(kUseFontConfigDb);
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
  
  public authCookies(): string[]
  {
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
    this.config.set(kPeferR64, peferR64);
  }

  // Windows-only option
  public peferR64(): boolean {
    // Check if Windows, or if arch is x64, arm64, or ppc64
    if (process.platform !== 'win32' || !process.arch.includes('64')) {
      return false;
    }
    return this.config.get(kPeferR64);
  }
}