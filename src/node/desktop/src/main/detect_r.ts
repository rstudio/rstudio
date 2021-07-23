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
import { exec, execSync } from 'child_process';
import { promisify } from 'util';
import { existsSync } from 'fs';
import { assert } from 'console';

import { logger } from '../core/logger';
import { Environment, getenv, setVars } from '../core/environment';
import { FilePath } from '../core/file-path';

import { appState } from './app-state';

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
    return await prepareEnvironmentWin32();
  } else {
    return await prepareEnvironmentPosix();
  }
}

async function prepareEnvironmentPosix(): Promise<boolean> {

  assert((process.platform !== 'win32'));

  // check for which R override
  let rWhichRPath = new FilePath();
  const whichROverride = getenv('RSTUDIO_WHICH_R');
  if (whichROverride) {
    rWhichRPath = new FilePath(whichROverride);
  }

  // determine rLdPaths script location
  let rLdScriptPath = new FilePath();
  if (process.platform === 'darwin') {
    if (app.isPackaged) {
      rLdScriptPath = appState().scriptsPath?.completePath('session/r-ldpath') ?? new FilePath();
      if (!rLdScriptPath.existsSync()) {
        rLdScriptPath = new FilePath(app.getAppPath()).completePath('r-ldpath');
      }
    } else {
      rLdScriptPath = appState().sessionPath?.completePath('../r-ldpath') ?? new FilePath();
    }
  } else {
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

  logger().logDiagnostic(`Using R script: ${detectResult.rScriptPath}`);

  setREnvironmentVars(detectResult.envVars ?? {});

  if (process.platform === 'darwin') {
    // TODO: deal with selection of x86_64 or arm64 build of R
  }

  return true;
}

async function prepareEnvironmentWin32(): Promise<boolean> {
  assert((process.platform === 'win32'));

  // TODO: this is just a hacked-together placeholder for dev purposes

  // check for which R override
  let rWhichRPath = new FilePath();
  const whichROverride = getenv('RSTUDIO_WHICH_R');
  if (!whichROverride) {
    dialog.showErrorBox(
      'RSTUDIO_WHICH_R Not Set',
      'Temporary: RSTUDIO_WHICH_R environment variable must point to R installation folder. Cannot continue.');
    return false;
  }

  rWhichRPath = new FilePath(whichROverride);
  if (!rWhichRPath.existsSync()) {
    dialog.showErrorBox(
      'RSTUDIO_WHICH_R Incorrect',
      `RSTUDIO_WHICH_R environment variable set incorrectly; ${rWhichRPath.getAbsolutePathNative()} not found. Cannot continue.`);
    return false;
  }


  process.env.R_HOME = rWhichRPath.getAbsolutePathNative();
  const rPath = rWhichRPath.completeChildPath('bin\\x64');
  process.env.PATH = `${rPath};${process.env.PATH}`;

  return true;
}

async function scanForR(rsessionWhichR: FilePath): Promise<FilePath> {
  if (process.platform === 'win32') {
    return await scanForRWin32(rsessionWhichR);
  } else {
    return await scanForRPosix(rsessionWhichR);
  }
}

async function scanForRPosix(rsessionWhichR: FilePath): Promise<FilePath> {
  assert((process.platform !== 'win32'));

  // if an override has been supplied (typically from rsession-which-r)
  // then prefer that
  if (!rsessionWhichR.isEmpty()) {
    return rsessionWhichR;
  }

  // if the RSTUDIO_WHICH_R environment variable is set, use that
  const rstudioWhichR = getenv('RSTUDIO_WHICH_R');
  if (rstudioWhichR) {
    return new FilePath(rstudioWhichR);
  }

  // first look for R on the PATH
  try {
    const { stdout } = await asyncExec('/usr/bin/which R', { encoding: 'utf-8' });
    const R = stdout.trim();
    if (R) {
      return new FilePath(R);
    }
  } catch (error) {
    logger().logError(error);
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

export async function scanForRWin32(rsessionWhichR: FilePath): Promise<FilePath> {

  assert((process.platform === 'win32'));

  // if an override has been supplied (typically from rsession-which-r)
  // then prefer that
  if (!rsessionWhichR.isEmpty()) {
    return rsessionWhichR;
  }

  // if the RSTUDIO_WHICH_R environment variable is set, use that
  const rstudioWhichR = getenv('RSTUDIO_WHICH_R');
  if (rstudioWhichR) {
    return new FilePath(rstudioWhichR);
  }

  // read default version information from registry
  const keyName = 'HKEY_LOCAL_MACHINE\\SOFTWARE\\R-Core\\R64';
  const regOutput = execSync(`reg query ${keyName} /v InstallPath`, { encoding: 'utf-8'});

  const lines = regOutput.split('\n');
  for (const line of lines) {
    const match = /^\s*InstallPath\s*REG_SZ\s*(.*)$/.exec(line);
    if (match != null) {
      const installPath = match[1];
      return new FilePath(installPath);
    }
  }

  // For now must set env var to run on Windows
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

  // TODO: detect and return R version 

  return { success: true, rScriptPath: scanResult.getAbsolutePath(), envVars: rEnv };
}

function setREnvironmentVars(vars: Environment) {
  setVars(vars);
}
