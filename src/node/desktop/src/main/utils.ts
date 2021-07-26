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
import os from 'os';
import path from 'path';
import { sep } from 'path';
import { app, WebContents } from 'electron';

import { Xdg } from '../core/xdg';
import { getenv, setenv } from '../core/environment';
import { FilePath } from '../core/file-path';
import { logger } from '../core/logger';
import { userHomePath } from '../core/user';

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

// used to help find built C++ sources in developer configurations
function findBuildRoot(): string {

  const rootPaths = [
    path.normalize(`${process.cwd()}/../..`),
    path.normalize(`${process.cwd()}/../../../..`),
  ];

  for (const rootPath of rootPaths) {
    const buildRoot = findBuildRootImpl(rootPath);
    if (buildRoot) {
      return buildRoot;
    }
  }

  throw rsessionNotFoundError();

}

function findBuildRootImpl(rootDir: string): string {

  // list all files + directories in the folder
  const files = fs.readdirSync(rootDir);

  // collect those that appear to be build directories
  const buildDirs = [];
  for (const file of files) {
    if (file.startsWith('build')) {
      const stat = fs.statSync(`${rootDir}/${file}`);
      if (stat.isDirectory()) {
        buildDirs.push({ file: file, stat: stat });
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
  const buildRoot = `${rootDir}/${buildDirs[0].file}`;
  console.log(`Using build root: ${buildRoot}`);
  return buildRoot;

}

function rsessionNotFoundError(): Error {
  
  const message =
    'Could not find rsession executable. ' +
    'Try setting the "RSTUDIO_CPP_BUILD_OUTPUT" environment variable ' +
    'to the location where src/cpp was built.';

  return Error(message);

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
    return [confPath, sessionPath, new FilePath(app.getAppPath())];
  }

  // developer builds -- first, check for environment variable
  // providing path to built C++ sources; if not set, then do
  // some primitive scanning for common developer workflows
  let buildRoot = getenv('RSTUDIO_CPP_BUILD_OUTPUT');
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
    if (sessionPath.exists()) {
      confPath = buildRootPath.completePath(`${subdir}/conf/rdesktop-dev.conf`);
      return [confPath, sessionPath, new FilePath(app.getAppPath())];
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
export function executeJavaScript(web: WebContents, cmd: string): Promise<any> {
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
