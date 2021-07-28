/*
 * detect-r.ts
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
import { dirname } from 'path';
import { execSync } from 'child_process';
import { existsSync } from 'fs';

import { Environment, getenv, setVars } from '../core/environment';
import { Expected, ok, err } from '../core/expected';
import { logger } from '../core/logger';
import { Err, success } from '../core/err';

let kLdLibraryPathVariable : string;
if (process.platform === 'darwin') {
  kLdLibraryPathVariable = 'DYLD_FALLBACK_LIBRARY_PATH';
} else {
  kLdLibraryPathVariable = 'LD_LIBRARY_PATH';
}

interface REnvironment {
  rScriptPath: string,
  version: string,
  envVars: Environment,
  ldLibraryPath: string
}

function showRNotFoundError(error?: Error): void {
  const message = error?.message ?? 'Could not locate an R installation on the system.';
  dialog.showErrorBox('R not found', message);
}

function executeCommand(command: string): Expected<string> {

  try {
    const output = execSync(command, { encoding: 'utf-8' });
    return ok(output.trim());
  } catch (error) {
    return err(error);
  }

}

/**
 * Detect R and prepare environment for launching the rsession process.
 * 
 * This entails setting environment variables relevant to R on startup
 * // (for example, R_HOME) and other platform-specific work required
 * for R to launch.
 */
export function prepareEnvironment(): Err {

  try {
    return prepareEnvironmentImpl();
  } catch (error) {
    logger().logError(error);
    return error;
  }

}

function prepareEnvironmentImpl(): Err {

  // attempt to detect R environment
  const [rEnvironment, error] = detectREnvironment();
  if (error) {
    showRNotFoundError(error);
    return error;
  }

  // set environment variables from R
  setVars(rEnvironment.envVars);

  // on Linux + macOS, forward LD_LIBRARY_PATH and friends
  if (process.platform !== 'win32') {
    process.env[kLdLibraryPathVariable] = rEnvironment.ldLibraryPath;
  }

  // on Windows, ensure R is on the PATH so that companion DLLs
  // in the same directory can be resolved
  const scriptPath = rEnvironment.rScriptPath;
  if (process.platform === 'win32') {
    const binDir = dirname(scriptPath);
    process.env.PATH = `${binDir};${process.env.PATH}`;
  }

  return success();

}

function detectREnvironment(): Expected<REnvironment> {

  // scan for R
  const [R, scanError] = scanForR();
  if (scanError) {
    showRNotFoundError();
    return err(scanError);
  }

  // generate small script for querying information about R
  const rQueryScript = String.raw`writeLines(c(
  format(getRversion()),
  R.home(),
  R.home("doc"),
  R.home("include"),
  R.home("share"),
  Sys.getenv("${kLdLibraryPathVariable}")
))`;

  const rQueryResult = execSync(`${R} --vanilla -s`, {
    encoding: 'utf-8',
    input: rQueryScript,
  });

  // unwrap query results
  const [
    rVersion,
    rHome,
    rDocDir,
    rIncludeDir,
    rShareDir,
    rLdLibraryPath,
  ] = rQueryResult.split('\n');

  // put it all together
  const result = {
    rScriptPath: R,
    version: rVersion,
    envVars: {
      R_HOME:        rHome,
      R_DOC_DIR:     rDocDir,
      R_INCLUDE_DIR: rIncludeDir,
      R_SHARE_DIR:   rShareDir,
    },
    ldLibraryPath: rLdLibraryPath,
  };

  return ok(result);

}

function scanForR(): Expected<string> {

  // if the RSTUDIO_WHICH_R environment variable is set, use that
  const rstudioWhichR = getenv('RSTUDIO_WHICH_R');
  if (rstudioWhichR) {
    logger().logDiagnostic(`Using ${rstudioWhichR} (found by RSTUDIO_WHICH_R environment variable)`);
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
  const [rLocation, error] = executeCommand('/usr/bin/which R');
  if (!error && rLocation) {
    logger().logDiagnostic(`Using ${rLocation} (found by /usr/bin/which/R)`);
    return ok(rLocation);
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
      logger().logDiagnostic(`Using ${rLocation} (found by searching known locations)`);
      return ok(location);
    }
  }

  // nothing found
  return err();

}

function findDefaultInstallPathWin32(version: string): string {

  // query registry for R install path
  const keyName = `HKEY_LOCAL_MACHINE\\SOFTWARE\\R-Core\\${version}`;
  const regQueryCommand = `reg query ${keyName} /v InstallPath`;
  const [output, error] = executeCommand(regQueryCommand);
  if (error) {
    logger().logError(error);
    return '';
  }

  // parse the actual path from the output
  const lines = output.split('\r\n');
  for (const line of lines) {
    const match = /^\s*InstallPath\s*REG_SZ\s*(.*)$/.exec(line);
    if (match != null) {
      const rLocation = match[1];
      logger().logDiagnostic(`Using ${rLocation} (found by searching registry`);
      return rLocation;
    }
  }

  return '';

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
    if (x64InstallPath && existsSync(x64InstallPath)) {
      return ok(`${x64InstallPath}/bin/x64/R.exe`);
    }
  }

  // look for a 32-bit version of R
  const i386InstallPath = findDefaultInstallPathWin32('R');
  if (i386InstallPath && existsSync(i386InstallPath)) {
    return ok(`${i386InstallPath}/bin/i386/R.exe`);
  }

  // nothing found; return empty filepath
  return err();

}
