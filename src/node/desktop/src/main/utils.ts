/*
 * utils.ts
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

import fs, { existsSync } from 'fs';
import os from 'os';
import path from 'path';
import { sep } from 'path';
import { app, BrowserWindow, Cookie, dialog, FileFilter, MessageBoxOptions, shell, WebContents } from 'electron';

import { Xdg } from '../core/xdg';
import { getenv, setenv } from '../core/environment';
import { FilePath } from '../core/file-path';
import { logger } from '../core/logger';
import { userHomePath } from '../core/user';

import i18next from 'i18next';
import { spawnSync } from 'child_process';
import { changeLanguage } from './i18n-manager';
import { randomUUID } from 'crypto';
import { appState } from './app-state';

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
function removeStaleOptionsLockfileImpl(): void {
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

export function removeStaleOptionsLockfile(): void {
  try {
    removeStaleOptionsLockfileImpl();
  } catch (err: unknown) {
    logger().logError(err);
  }
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
    `${rootDir}/build`,
    `${rootDir}/src`,
    `${rootDir}/src/cpp`,
    `${rootDir}/src/cpp/build`,
    `${rootDir}/package/linux`,
    `${rootDir}/package/osx`,
    `${rootDir}/package/win32`,
  ];

  // Look for build directories, and files within those build directories
  // that get eagerly modified after a build.
  for (const buildDirParent of buildDirParents) {
    if (fs.existsSync(buildDirParent)) {
      // First, check if this is itself a build directory.
      for (const buildFile of ['.ninja_log', 'CMakeFiles']) {
        const buildPath = `${buildDirParent}/${buildFile}`;
        if (fs.existsSync(buildPath)) {
          const stat = fs.statSync(buildPath);
          buildDirs.push({ path: buildDirParent, stat: stat });
        }
      }

      // Otherwise, check if there's a sub-directory that's a build directory.
      const buildDirFiles = fs.readdirSync(buildDirParent);
      for (const buildDirFile of buildDirFiles) {
        if (/^(?:build|cmake-build-|Desktop)/.test(buildDirFile)) {
          const buildDirPath = `${buildDirParent}/${buildDirFile}`;
          for (const buildFile of ['.ninja_log', 'CMakeFiles']) {
            const buildPath = `${buildDirPath}/${buildFile}`;
            if (fs.existsSync(buildPath)) {
              const stat = fs.statSync(buildPath);
              buildDirs.push({ path: buildDirPath, stat: stat });
            }
          }
        }
      }
    }
  }

  // if we didn't find anything, bail here
  if (buildDirs.length === 0) {
    return '';
  }

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
 * @returns Paths for install path, config file, rsession, and desktop scripts.
 */
export function findComponents(): [FilePath, FilePath, FilePath, FilePath] {
  // determine paths to config file, rsession, and desktop scripts
  const binRoot = new FilePath(getAppPath());
  if (app.isPackaged) {
    // config-file path is intentionally empty for a package build
    const sessionPath = binRoot.completePath(`bin/${rsessionExeName()}`);
    return [binRoot, new FilePath(), sessionPath, new FilePath(getAppPath())];
  }

  let confPath!: FilePath;

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
    for (const buildType of ['', 'Debug/', 'RelWithDebInfo/']) {
      const sessionPath = buildRootPath.completePath(`${subdir}/session/${buildType}${rsessionExeName()}`);
      if (sessionPath.existsSync()) {
        confPath = buildRootPath.completePath(`${subdir}/conf/rdesktop-dev.conf`);
        return [new FilePath(buildRoot), confPath, sessionPath, new FilePath(getAppPath())];
      }
    }
  }

  // we found a build root, but not rsession -- throw an error
  throw rsessionNotFoundError();
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

export function parseFilter(filters: string): FileFilter[] {
  // This function receives a variety of filter types, including the old-fashioned
  // Qt filter (as used with older releases of RStudio) which separates disparate
  // filters with the ';;' delimiter. We intentionally try to be permissive
  // when parsing the filter entries.
  const result: FileFilter[] = [];

  for (const filter of filters.split(';;')) {
    // Find the opening parenthesis
    const openIndex = filter.indexOf('(', 0);
    if (openIndex === -1) {
      continue;
    }

    // Consume the name
    const name = filter.substring(0, openIndex).trim();

    // Find the closing parenthesis
    const closeIndex = filter.indexOf(')', openIndex + 1);
    if (closeIndex === -1) {
      continue;
    }

    // Grab the extensions string within
    const exts = filter.substring(openIndex + 1, closeIndex).trim();

    // Split when multiple extensions are provided.
    // Intentionally allow multiple different kinds of delimiters here;
    // for example, the following should all be accepted.
    //
    //    Data Files (*.csv *.xls)
    //    Data Files (*.csv; *.xls)
    //    Data Files (*.csv | *.xls)
    //
    const extensions = exts
      .trim()
      .split(/[\s;,|]+/g)
      .map((value) => {
        if (value.startsWith('*.')) {
          return value.substring(2);
        } else if (value.startsWith('.')) {
          return value.substring(1);
        } else {
          return value;
        }
      });

    // Add our result
    result.push({
      name: name,
      extensions: extensions,
    });
  }

  return result;
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

    await appState().modalTracker.trackElectronModalAsync(async () =>
      dialog.showMessageBox(options.window, dialogContent),
    );

    if (options.shouldCloseWindow) window.close();
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
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

  await window.webContents.session.cookies
    .get({})
    .then(async (cookies: Cookie[]) => {
      if (cookies.length === 0) {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        await updateLocaleFromCookie({ name: 'LOCALE', value: 'en' } as any, window);
      } else {
        cookies.forEach(async (cookie) => {
          await updateLocaleFromCookie(cookie, window);
        });
      }
    })
    .catch((err) => logger().logError(err));
};

export function registerWebContentsDebugHandlers(webContents: WebContents) {
  // add debug handlers for all of the various navigation and load events
  const events = [
    'before-input-event',
    'blur',
    'certificate-error',
    'console-message',
    'context-menu',
    'crashed',
    'cursor-changed',
    'destroyed',
    'devtools-closed',
    'devtools-focused',
    'devtools-opened',
    'devtools-reload-page',
    'did-attach-webview',
    'did-change-theme-color',
    'did-create-window',
    'did-fail-load',
    'did-fail-provisional-load',
    'did-finish-load',
    'did-frame-finish-load',
    'did-frame-navigate',
    'did-navigate',
    'did-navigate-in-page',
    'did-redirect-navigation',
    'did-start-loading',
    'did-start-navigation',
    'did-stop-loading',
    'dom-ready',
    'enter-html-full-screen',
    'focus',
    'found-in-page',
    'frame-created',

    // TODO: These are pretty noisy.
    // 'ipc-message',
    // 'ipc-message-sync',

    'leave-html-full-screen',
    'login',
    'media-paused',
    'media-started-playing',
    'new-window',
    'page-favicon-updated',
    'page-title-updated',
    'paint',
    'plugin-crashed',
    'preferred-size-changed',
    'preload-error',
    'render-process-gone',
    'responsive',
    'select-bluetooth-device',
    'select-client-certificate',
    'unresponsive',
    'update-target-url',
    'will-attach-webview',
    'will-navigate',
    'will-prevent-unload',
    'will-redirect',
    'zoom-changed',
  ] as const;

  for (const event of events) {
    try {
      registerWebContentsDebugHandlerImpl(webContents, event);
    } catch (error: unknown) {
      logger().logDebug(`Error adding debug handler for event '${event}': ${error}`);
    }
  }
}

function registerWebContentsDebugHandlerImpl(webContents: WebContents, event: string) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  webContents.on(event as any, (...args) => {
    const json = args.map((arg) => {
      // check for an Electron Event and handle it specially here
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const electronEvent = arg as any;
      if (electronEvent != null && typeof electronEvent.preventDefault !== 'undefined') {
        return '<event>';
      }

      // try to stringify other arguments
      try {
        if (typeof arg === 'object') {
          return '<object>';
        } else {
          return JSON.stringify(arg);
        }
      } catch (_e: unknown) {
        return '<unknown>';
      }
    });

    logger().logDebug(`'${event}': [${json.join(', ')}]`);
  });
}

export function getNumericEnvVar(envVarName: string): number | undefined {
  const envVar = getenv(envVarName).trim();
  if (envVar) {
    const maybeNum = Number(envVar);
    return !isNaN(maybeNum) ? maybeNum : undefined;
  }
  return undefined;
}

const TRAILING_SLASHES_REGEX = /[\\/]+$/;
const ONLY_SLASHES_REGEX = /^[\\/]+$/;

/**
 * Removes trailing slashes from a path string. Paths that contain only
 * slashes will be returned as-is.
 * @param str The string to remove trailing slashes from
 * @returns The string with trailing slashes removed
 */
export function removeTrailingSlashes(str: string): string {
  if (str.length === 0 || ONLY_SLASHES_REGEX.test(str)) {
    return str;
  }
  const trailingSlashesIndex = str.search(TRAILING_SLASHES_REGEX);
  if (trailingSlashesIndex > 0) {
    return str.substring(0, trailingSlashesIndex);
  }
  return str;
}

/**
 * Load the R website in the user's default browser.
 */
export function loadRWebsite() {
  const rProjectUrl = 'https://www.rstudio.org/links/r-project';
  void shell.openExternal(rProjectUrl);
}
