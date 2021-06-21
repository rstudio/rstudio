/*
 * detect_r.ts
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

import { app, dialog } from 'electron';

import { exec } from 'child_process';
import { promisify } from 'util';
import { existsSync } from 'fs';

import { appState } from './app-state';
import { Environment, getenv, setVars } from '../core/environment';
import { FilePath } from '../core/file-path';

const asyncExec = promisify(exec);

function showRNotFoundError(msg: string): void {
  dialog.showErrorBox('R Not Found', msg);
}

/**
 * Detect R and prepare environment for launching rsession.
 * 
 * @returns true if startup should continue, false on fatal error
 */
export async function prepareEnvironment(): Promise<boolean> {
  if (process.platform === 'win32') {
    // TODO: current implementation in DesktopWin32DetectRHome.cpp
    dialog.showErrorBox('NYI', 'R detection and environment setup NYI on Windows, cannot continue');
    return false;
  }

  // check for which R override
  let rWhichRPath = new FilePath();
  const whichROverride = getenv('RSTUDIO_WHICH_R');
  if (whichROverride) {
    rWhichRPath = new FilePath(whichROverride);
  }

  let rLdScriptPath = new FilePath();
  if (process.platform === 'darwin') {
    if (app.isPackaged) {
      rLdScriptPath = appState().scriptsPath?.completePath('session/r-ldpath') ?? new FilePath();
      if (!rLdScriptPath.existsSync()) {
        rLdScriptPath = new FilePath(app.getAppPath()).completePath('r-ldpath');
      }
    } else {
      rLdScriptPath = appState().scriptsPath?.completePath('../session/r-ldpath') ?? new FilePath();
    }
  } else {
    // determine rLdPaths script location
    rLdScriptPath = appState().supportingFilePath().completePath('bin/r-ldpath');
    if (!rLdScriptPath.existsSync()) {
      rLdScriptPath = appState().supportingFilePath().completePath('session/r-ldpath');
    }
  }

  // attempt to detect R environment
  const detectResult = await detectREnvironment(rWhichRPath, rLdScriptPath, '');
  if (!detectResult.success) {
    showRNotFoundError(detectResult.errMsg ?? 'Unknown error');
    return false;
  }

  if (appState().runDiagnostics) {
    console.log(`Using R script: ${detectResult.rScriptPath}`);
  }

  // set environment and return true
  setREnvironmentVars(detectResult.envVars ?? {});
  return true;
}

export async function scanForR(rstudioWhichR: FilePath): Promise<FilePath> {
  // prefer RSTUDIO_WHICH_R
  if (!rstudioWhichR.isEmpty()) {
    return rstudioWhichR;
  }

  if (process.platform === 'win32') {
    // For now must set env var to run on Windows
    return new FilePath();
  }

  // first look for R on the PATH
  const { stdout } = await asyncExec('/usr/bin/which R', { encoding: 'utf-8' });
  const R = stdout.trim();
  if (R) {
    return new FilePath(R);
  }

  // otherwise, look in some hard-coded locations
  const defaultLocations = [
    '/opt/local/bin/R',
    '/usr/local/bin/R',
    '/usr/bin/R',
  ];

  // also check framework directory for macOS
  if (process.platform === 'darwin') {
    defaultLocations.push('/Library/Frameworks/R.framework/Resources/bin/R');
  }

  for (const location of defaultLocations) {
    if (existsSync(location)) {
      return new FilePath(location);
    }
  }

  // nothing found
  return new FilePath();
}

interface REnvironment {
  success: boolean,
  rScriptPath?: string,
  version?: string,
  envVars?: Environment,
  errMsg?: string,
}

async function detectREnvironment(
  whichRScript: FilePath,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  ldPathsScript: FilePath,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  ldLibraryPath: string
): Promise<REnvironment> {

  const scanResult = await scanForR(whichRScript);
  if (scanResult.isEmpty()) {
    return { success: false, errMsg: 'Did not find R' };
  }
  const R = scanResult.getAbsolutePath();

  // read some important environment variables
  const { stdout } = await asyncExec(`${R} --no-save --no-restore RHOME`);
  const rHome = stdout.trim();
  const rEnv = {
    R_HOME: `${rHome}`,
    R_SHARE_DIR: `${rHome}/share`,
    R_DOC_DIR: `${rHome}/doc`
  };

  return { success: true, rScriptPath: scanResult.getAbsolutePath(), envVars: rEnv };
}

function setREnvironmentVars(vars: Environment) {
  setVars(vars);
}