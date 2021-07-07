/*
 * utils.ts
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

import fs from 'fs';
import path from 'path';
import { app, WebContents } from 'electron';

import { Xdg } from '../core/xdg';
import { getenv, setenv } from '../core/environment';
import { FilePath } from '../core/file-path';
import { logger } from '../core/logger';

import { productInfo } from './product-info';
import { MainWindow } from './main-window';

export function initializeSharedSecret(): void {
  const sharedSecret = randomString() + randomString() + randomString();
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

export interface VersionInfo  {
  electron: string;
  rstudio?: string;
  node: string;
  v8: string;
}

export function getComponentVersions(): string {
  const componentVers: VersionInfo = process.versions;
  componentVers['rstudio'] = productInfo().RSTUDIO_VERSION;
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
    if (piece.startsWith('-')) {
      app.commandLine.appendSwitch(piece);
    } else {
      app.commandLine.appendArgument(piece);
    }
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
  return process.platform === 'win32' ? 'rsession.exe' : 'rsession';
}

/**
 * @returns Paths to config file, rsession, and desktop scripts.
 */
export function findComponents(): [FilePath, FilePath, FilePath] {

  // determine paths to config file, rsession, and desktop scripts
  let confPath: FilePath = new FilePath();
  let sessionPath: FilePath = new FilePath();

  const binRoot = new FilePath(app.getAppPath());
  if (app.isPackaged) {
    // confPath is intentionally left empty for a package build
    sessionPath = binRoot.completePath(`bin/${rsessionExeName()}`);
  } else {
    const buildRootEnv = getenv('RSTUDIO_CPP_BUILD_OUTPUT');
    if (!buildRootEnv) {
      throw Error('RSTUDIO_CPP_BUILD_OUTPUT env var must contain ' +
        'path where src/cpp was built (dev config).');
    }
    const buildRoot = new FilePath(buildRootEnv);
    confPath = buildRoot.completePath('conf/rdesktop-dev.conf');
    sessionPath = buildRoot.completePath(`session/${rsessionExeName()}`);
  }
  return [confPath, sessionPath, new FilePath(app.getAppPath())];
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function finalPlatformInitialize(mainWindow: MainWindow): void {
  // TODO - reimplement for each platform
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function executeJavaScript(web: WebContents, cmd: string): Promise<any> {
  logger().logDebug(`executeJavaScript(${cmd})`);
  return web.executeJavaScript(cmd);
}
