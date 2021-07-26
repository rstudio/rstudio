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

import { dialog } from 'electron';
import { execSync } from 'child_process';
import { existsSync } from 'fs';

import { logger } from '../core/logger';
import { Environment, getenv, setVars } from '../core/environment';

import { dirname } from 'path';
import { Expected, ok, err } from '../core/expected';

interface REnvironment {
  rScriptPath: string,
  version: string,
  envVars: Environment,
}

function showRNotFoundError(error?: Error): void {
  const message = error?.message ?? 'Could not locate an R installation on the system.';
  dialog.showErrorBox('R not found', message);
}

/**
 * Detect R and prepare environment for launching rsession.
 * 
 * @returns true if startup should continue, false on fatal error
 */
export function prepareEnvironment(): Expected<REnvironment> {

  try {
    return prepareEnvironmentImpl();
  } catch (error) {
    logger().logError(error);
    return err(error);
  }

}

function prepareEnvironmentImpl(): Expected<REnvironment> {

  // attempt to detect R environment
  const [rEnvironment, error] = detectREnvironment();
  if (error) {
    showRNotFoundError(error);
    return err(error);
  }

  // set environment variables from R
  setVars(rEnvironment.envVars);

  // on Windows, ensure R is on the PATH so that companion DLLs
  // in the same directory can be resolved
  const scriptPath = rEnvironment.rScriptPath;
  if (process.platform === 'win32') {
    const binDir = dirname(scriptPath);
    process.env.PATH = `${binDir};${process.env.PATH}`;
  }

  return ok(rEnvironment);

}

function detectREnvironment(): Expected<REnvironment> {

  // scan for R
  const [R, error] = scanForR();
  if (error) {
    showRNotFoundError();
    return err(error);
  }

  // get R_HOME + other related R environment variables
  const stdout = execSync(`${R} RHOME`, { encoding: 'utf-8' });
  const home = stdout.trim();
  const envvars = {
    R_HOME:        `${home}`,
    R_SHARE_DIR:   `${home}/share`,
    R_INCLUDE_DIR: `${home}/include`,
    R_DOC_DIR:     `${home}/doc`
  };

  // get R version string
  const rVersion = execSync(
    `${R} --vanilla -s -e "cat(format(getRversion()))"`,
    { encoding: 'utf-8' }
  );

  const result = {
    rScriptPath: R,
    version:     rVersion,
    envVars:     envvars
  };

  return ok(result);

}

function scanForR(): Expected<string> {

  // if the RSTUDIO_WHICH_R environment variable is set, use that
  const rstudioWhichR = getenv('RSTUDIO_WHICH_R');
  if (rstudioWhichR) {
    logger().logDiagnostic(`Using RSTUDIO_WHICH_R: ${rstudioWhichR}`);
    return ok(rstudioWhichR);
  }

  // otherwise, use platform-specific lookup strategies
  if (process.platform === 'win32') {
    return scanForRWin32();
  } else {
    return scanForRPosix();
  }

}

function scanForRPosix(): Expected<string> {

  // first, look for R on the PATH
  const stdout = execSync('/usr/bin/which R', { encoding: 'utf-8' });
  const R = stdout.trim();
  if (R) {
    return ok(R);
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
      return ok(location);
    }
  }

  // nothing found
  return err();

}

function findDefaultInstallPathWin32(version: string): Expected<string> {

  const keyName = `HKEY_LOCAL_MACHINE\\SOFTWARE\\R-Core\\${version}`;
  const regOutput = execSync(`reg query ${keyName} /v InstallPath`, { encoding: 'utf-8'});

  const lines = regOutput.split('\r\n');
  for (const line of lines) {
    const match = /^\s*InstallPath\s*REG_SZ\s*(.*)$/.exec(line);
    if (match != null) {
      return ok(match[1]);
    }
  }

  return err();

}

function scanForRWin32(): Expected<string> {

  // if the RSTUDIO_WHICH_R environment variable is set, use that
  const rstudioWhichR = getenv('RSTUDIO_WHICH_R');
  if (rstudioWhichR) {
    return ok(rstudioWhichR);
  }

  // look for a 64-bit version of R
  if (process.arch !== 'x32') {
    const x64InstallPath = findDefaultInstallPathWin32('R64');
    if (x64InstallPath) {
      return ok(`${x64InstallPath}/bin/x64/R.exe`);
    }
  }

  // look for a 32-bit version of R
  const i386InstallPath = findDefaultInstallPathWin32('R');
  if (i386InstallPath) {
    return ok(`${i386InstallPath}/bin/i386/R.exe`);
  }

  // nothing found; return empty filepath
  return err();

}
