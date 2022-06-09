/*
 * utils.ts
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

import fs, { existsSync } from 'fs';
import os from 'os';
import path from 'path';
import { sep } from 'path';
import { app, BrowserWindow, dialog, FileFilter, MessageBoxOptions, WebContents } from 'electron';
import http from 'http';

import { Xdg } from '../core/xdg';
import { getenv, setenv } from '../core/environment';
import { FilePath } from '../core/file-path';
import { logger } from '../core/logger';
import { userHomePath } from '../core/user';
import { WaitResult, WaitTimeoutFn, waitWithTimeout } from '../core/wait-utils';
import { Err } from '../core/err';

import { MainWindow } from './main-window';
import i18next from 'i18next';
import { spawnSync } from 'child_process';
import { changeLanguage } from './i18n-manager';
import { randomUUID } from 'crypto';

// work around Electron resolving the application path to 'app.asar'
const appPath = path.join(path.dirname(app.getAppPath()), 'app');

export function getAppPath(): string {
  return appPath;
}

export function initializeSharedSecret(): void {
  const sharedSecret = randomUUID();
  setenv('RS_SHARED_SECRET', sharedSecret);
}

export function userLogPath(): FilePath {
  return Xdg.userDataDir().completeChildPath('log');
}

export function userWebCachePath(): FilePath {
  return Xdg.userDataDir().completeChildPath('web-cache');
}

export function devicePixelRatio(/*QMainWindow * pMainWindow*/): number {
  // TODO
  return 1.0;
}

export function randomString(): string {
  return Math.trunc(Math.random() * 2147483647).toString();
}

export interface VersionInfo {
  electron: string;
  rstudio?: string;
  node: string;
  v8: string;
}

export function getComponentVersions(): string {
  const componentVers: VersionInfo = process.versions;
  componentVers['rstudio'] = app.getVersion();
  return JSON.stringify(componentVers, null, 2);
}

/**
 * Pass additional Chromium arguments set by user via RSTUDIO_CHROMIUM_ARGUMENTS
 * environment variable.
 */
export function augmentCommandLineArguments(): void {
  const user = getenv('RSTUDIO_CHROMIUM_ARGUMENTS');
  if (!user) {
    return;
  }

  const pieces = user.split(' ');
  pieces.forEach((piece) => {

    // if this piece doesn't start with '-', treat it as a plain argument
    if (!piece.startsWith('-')) {
      app.commandLine.appendArgument(piece);
      return;
    }

    // otherwise, parse it as a switch and add it
    // switches will have the form '--key=value', so split on the first '='
    const idx = piece.indexOf('=');
    if (idx == -1) {
      if (!app.commandLine.hasSwitch(piece)) {
        app.commandLine.appendSwitch(piece);
      }
      return;
    }

    const lhs = piece.substring(0, idx);
    const rhs = piece.substring(idx + 1);

    // replace the old switch if needed
    if (app.commandLine.hasSwitch(lhs)) {
      app.commandLine.removeSwitch(lhs);
    }

    app.commandLine.appendSwitch(lhs, rhs);
  });

}

/**
 * Attempt to remove stale lockfiles that might inhibit
 * RStudio startup (currently Windows only). Throws
 * an error only when a stale lockfile exists, but
 * we could not successfully remove it
 */
export function removeStaleOptionsLockfile(): void {
  if (process.platform !== 'win32') {
    return;
  }

  const appData = getenv('APPDATA');
  if (!appData) {
    return;
  }

  const lockFilePath = path.join(appData, 'RStudio/desktop.ini.lock');
  if (!fs.existsSync(lockFilePath)) {
    return;
  }

  const diff = (Date.now() - fs.statSync(lockFilePath).mtimeMs) / 1000;
  if (diff < 10) {
    return;
  }

  fs.unlinkSync(lockFilePath);
}

export function rsessionExeName(): string {
  switch (process.platform) {
    case 'win32':
      return 'rsession.exe';
    default:
      return 'rsession';
  }
}

/**
 * 
 * @returns Root of the RStudio repo for a dev build, nothing for packaged build
 */
export function findRepoRoot(): string {
  if (app.isPackaged) {
    return '';
  }
  for (let dir = process.cwd(); dir !== path.dirname(dir); dir = path.dirname(dir)) {
    // check for release file
    const releaseFile = path.join(dir, 'version', 'RELEASE');
    if (existsSync(releaseFile)) {
      return dir;
    }
  }
  return '';
}

// used to help find built C++ sources in developer configurations
function findBuildRoot(): string {
  // look for the project root directory. note that the current
  // working directory may differ depending on how we are launched
  // (e.g. unit tests will have their parent folder as the working directory)
  const dir = findRepoRoot();
  if (dir.length > 0) {
    return findBuildRootImpl(dir);
  }

  throw rsessionNotFoundError();
}

function findBuildRootImpl(rootDir: string): string {
  // array of discovered build directories
  const buildDirs = [];

  // root directories to search
  const buildDirParents = [
    `${rootDir}`,
    `${rootDir}/src`,
    `${rootDir}/src/cpp`,
    `${rootDir}/package/linux`,
    `${rootDir}/package/osx`,
    `${rootDir}/package/win32`
  ];

  // list all files + directories in root folder
  for (const buildDirParent of buildDirParents) {
    const buildDirFiles = fs.readdirSync(buildDirParent);
    for (const file of buildDirFiles) {
      if (file.startsWith('build') || file.startsWith('cmake-build-')) {
        const path = `${buildDirParent}/${file}`;
        const stat = fs.statSync(path);
        if (stat.isDirectory()) {
          buildDirs.push({ path: path, stat: stat });
        }
      }
    }
  }

  // if we didn't find anything, bail here
  if (buildDirs.length === 0) {
    return '';
  }

  // sort build directories by last modified time
  buildDirs.sort((lhs, rhs) => {
    return rhs.stat.mtime.getTime() - lhs.stat.mtime.getTime();
  });

  // return the newest one
  const buildRoot = buildDirs[0].path;
  logger().logDebug(`Using build root: ${buildRoot}`);
  return buildRoot;
}

function rsessionNotFoundError(): Error {
  const workingDirectory = '' + process.cwd();
  const message = i18next.t('utilsTs.rsessionNotFoundError', { workingDirectory });

  return Error(message);
}

/**
 * @returns Paths to config file, rsession, and desktop scripts.
 */
export function findComponents(): [FilePath, FilePath, FilePath] {
  // determine paths to config file, rsession, and desktop scripts
  let confPath: FilePath = new FilePath();
  let sessionPath: FilePath = new FilePath();

  const binRoot = new FilePath(getAppPath());
  if (app.isPackaged) {
    // confPath is intentionally left empty for a package build
    sessionPath = binRoot.completePath(`bin/${rsessionExeName()}`);
    return [confPath, sessionPath, new FilePath(getAppPath())];
  }

  // developer builds -- first, check for environment variable
  // providing path to built C++ sources; if not set, then do
  // some primitive scanning for common developer workflows
  let buildRoot = getenv('RSTUDIO_CPP_BUILD_OUTPUT');
  if (buildRoot && !existsSync(buildRoot)) {
    logger().logDebug(`RSTUDIO_CPP_BUILD_OUTPUT is set (${buildRoot}) but does not exist`);
    buildRoot = '';
  }

  // if we couldn't resolve the build root from RSTUDIO_CPP_BUILD_OUTPUT,
  // then fall back to lookup heuristics
  if (!buildRoot) {
    buildRoot = findBuildRoot();
  }

  // if we still don't have a root, bail
  if (!buildRoot) {
    throw rsessionNotFoundError();
  }

  // try to find rsession in build root
  const buildRootPath = new FilePath(buildRoot);
  for (const subdir of ['.', 'src/cpp']) {
    const sessionPath = buildRootPath.completePath(`${subdir}/session/${rsessionExeName()}`);
    if (sessionPath.existsSync()) {
      confPath = buildRootPath.completePath(`${subdir}/conf/rdesktop-dev.conf`);
      return [confPath, sessionPath, new FilePath(getAppPath())];
    }
  }

  // we found a build root, but not rsession -- throw an error
  throw rsessionNotFoundError();
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function finalPlatformInitialize(mainWindow: MainWindow): void {
  // TODO - reimplement for each platform
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function executeJavaScript(web: WebContents, cmd: string): Promise<any> {
  logger().logDebug(`executeJavaScript(${cmd})`);
  return web.executeJavaScript(cmd);
}

/**
 * Return a "probably unique" folder name in the system tempdir, with a user-provided
 * prefix string followed by a randomly generated component.
 *
 * The folder is not created by this call so possible someone else could create it, thus the
 * "probably unique" nature of this call.
 *
 * @param folderPrefix Custom prefix string to include at the beginning of the folder name.
 * @returns A fully qualified path in the temporary folder (not actually created).
 */
export function getCurrentlyUniqueFolderName(folderPrefix: string): FilePath {
  const prefix = `${os.tmpdir()}${sep}${folderPrefix}`;

  // should be highly unlikely to ever get stuck in the loop, but just in case...
  for (let tries = 0; tries < 10; tries++) {
    const fallbackPath = new FilePath(`${prefix}${randomString()}`);
    if (!fallbackPath.existsSync()) {
      return fallbackPath;
    }
  }
  return new FilePath();
}

export function resolveAliasedPath(path: string): string {
  const resolved = FilePath.resolveAliasedPathSync(path, userHomePath());
  return resolved.getAbsolutePath();
}

export function filterFromQFileDialogFilter(qtFilters: string): FileFilter[] {
  // Qt filters are specified in this format:
  //   "Images (*.png *.xpm *.jpg);;Text files (*.txt);;XML files (*.xml)"

  const result: FileFilter[] = [];

  const filters = qtFilters.split(';;');
  for (const filter of filters) {
    // get the name portion
    const extopen = filter.indexOf(' (*.');
    if (extopen === -1) {
      logger().logDebug(`Skipping malformed filter: '${filter}'`);
      continue;
    }
    const name = filter.substring(0, extopen);

    // remove the name and opening ' (*.'
    let extensions = filter.substring(extopen + 4);

    // remove the trailing ')'
    const extclose = extensions.lastIndexOf(')');
    if (extclose === -1) {
      logger().logDebug(`Skipping malformed filter: '${filter}`);
      continue;
    }
    extensions = extensions.substring(0, extclose);

    // capture the extensions minus each ' *.'
    const exts: string[] = extensions.split(' *.');
    result.push({ name: name, extensions: exts });
  }
  return result;
}

/**
 * Wait for a URL to respond, with retries and timeout
 */
export async function waitForUrlWithTimeout(
  url: string,
  initialWaitMs: number,
  incrementWaitMs: number,
  maxWaitSec: number,
): Promise<Err> {
  const checkReady: WaitTimeoutFn = async () => {
    return new Promise((resolve) => {
      http
        .get(url, (res) => {
          res.resume(); // consume response data to free up memory
          resolve(new WaitResult('WaitSuccess'));
        })
        .on('error', (e) => {
          logger().logDebug(`Connection to ${url} failed: ${e.message}`);
          resolve(new WaitResult('WaitContinue'));
        });
    });
  };

  return waitWithTimeout(checkReady, initialWaitMs, incrementWaitMs, maxWaitSec);
}

export function raiseAndActivateWindow(window: BrowserWindow): void {
  if (window.isMinimized()) {
    window.restore();
  }
  window.moveTop();
  window.focus();
}

export function getDpiZoomScaling(): number {
  return 1.0;
}

/**
 * Determine if given host is considered safe to load in an IDE window.
 */
export function isSafeHost(host: string): boolean {
  const safeHosts = ['.youtube.com', '.vimeo.com', '.c9.ms', '.google.com'];

  for (const safeHost of safeHosts) {
    if (host.endsWith(safeHost)) {
      return true;
    }
  }
  return false;
}

// this code follows in the footsteps of R.app
function initializeLangDarwin(): string {

  {
    // if 'force.LANG' is set, that takes precedencec
    const args = ['read', 'org.R-project.R', 'force.LANG'];
    const result = spawnSync('/usr/bin/defaults', args, { encoding: 'utf-8' });
    if (result.status === 0) {
      return result.stdout.trim();
    }
  }

  {
    // if 'ignore.system.locale' is set, we'll use a UTF-8 locale
    const args = ['read', 'org.R-project.R', 'ignore.system.locale'];
    const result = spawnSync('/usr/bin/defaults', args, { encoding: 'utf-8' });
    if (result.status === 0 && result.stdout.trim() === 'YES') {
      return 'en_US.UTF-8';
    }
  }

  // next, check the LANG environment variable
  const lang = getenv('LANG');
  if (lang.length !== 0) {
    return lang;
  }

  // otherwise, just use UTF-8 locale
  return 'en_US.UTF-8';

}

export function initializeLang(): void {
  if (process.platform === 'darwin') {
    const lang = initializeLangDarwin();
    if (lang.length !== 0) {
      setenv('LANG', lang);
      setenv('LC_CTYPE', lang);
    }
  }
}

/**
 * Create an Error Dialog.
 * You can pass in a custom window.
 * If so, you must pass shouldCloseWindow = true as well.
 *
 * @export
 * @param {string} title
 * @param {string} message
 * @param {{
 *     window: BrowserWindow;
 *     shouldCloseWindow: boolean;
 *   }} [options={
 *     window = new BrowserWindow({ width: 0, height: 0 }),
 *     shouldCloseWindow: false,
 *   }]
 */
export async function createStandaloneErrorDialog(
  title: string,
  message: string,
  options: {
    window: BrowserWindow;
    shouldCloseWindow: boolean;
  } = {
    window: new BrowserWindow({ width: 0, height: 0 }),
    shouldCloseWindow: false,
  },
) {
  try {
    const dialogContent: MessageBoxOptions = {
      message: '',
      type: 'error',
      buttons: ['OK'],
    };

    dialogContent[process.platform === 'win32' ? 'title' : 'message'] = title;
    dialogContent[process.platform === 'win32' ? 'message' : 'detail'] = message;

    await dialog.showMessageBox(options.window, dialogContent);

    if (options.shouldCloseWindow) window.close();
    // eslint-disable-next-line @typescript-eslint/no-implicit-any-catch
  } catch (error: any) {
    console.error('[utils.ts] [createStandaloneErrorDialog] Error when creating Standalone Error Dialog: ', error);
  }
}

export const handleLocaleCookies = async (window: BrowserWindow, isMainWindow = false) => {
  const updateLocaleFromCookie = async (cookie: Electron.Cookie, window: BrowserWindow) => {
    const localeCookieName = 'LOCALE';

    if (cookie.name == localeCookieName) {
      const localeLastTimeSetStorageItemKey = 'LAST_TIME';

      const newLanguage = cookie.value;

      const jsSetLocaleScript = `
          window.localStorage.setItem('${localeCookieName}', '${newLanguage}');
          window.localStorage.setItem('${localeLastTimeSetStorageItemKey}', '${new Date().getTime()}');
      `;

      await window.webContents.executeJavaScript(jsSetLocaleScript);

      await changeLanguage(newLanguage);
    }
  };

  if (isMainWindow) {
    window.webContents.session.cookies.on('changed', async (_, cookie) => {
      await updateLocaleFromCookie(cookie, window);
    });
  }

  await window.webContents.session.cookies.get({}).then(async (cookies) => {
    if (cookies.length === 0) {
      await updateLocaleFromCookie({ name: 'LOCALE', value: 'en' } as any, window);
    } else {
      cookies.forEach(async (cookie) => {
        await updateLocaleFromCookie(cookie, window);
      });
    }
  });
};
