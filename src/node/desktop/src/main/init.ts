/*
 * init.ts
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
import { app } from 'electron';

import { getenv } from '../core/environment';
import { getRStudioVersion } from './product-info';

/**
 * Startup code run before app 'ready' event. Return 0 (zero) to continue 
 * launching, non-zero to exit process with that value.
 */
export function beforeAppReadyEvent(): number {
  // look for a version check request; if we have one, just do that and exit
  if (app.commandLine.hasSwitch('version')) {
    console.log(getRStudioVersion());
    return 1;
  }

  // report extended version info and exit
  if (app.commandLine.hasSwitch('version-json')) {
    console.log(getComponentVersions());
    return 1;
  }

  // attempt to remove stale lockfiles, as they can impede application startup
  try {
    removeStaleOptionsLockfile();
  } catch (error) {
    console.error(error);
    return 1;
  }

  // allow users to supply extra command-line arguments
  augmentCommandLineArguments();

  return 0;
}

export interface VersionInfo  {
  electron: string;
  rstudio?: string;
  node: string;
  v8: string;
}

export function getComponentVersions(): string {
  const componentVers: VersionInfo = process.versions;
  componentVers['rstudio'] = getRStudioVersion();
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
