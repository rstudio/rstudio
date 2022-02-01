/*
 * detect-r.ts
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

import path from 'path';

import { execSync, spawnSync } from 'child_process';
import { existsSync } from 'fs';
import { EOL } from 'os';

import { Environment, getenv, setenv, setVars } from '../core/environment';
import { Expected, ok, err } from '../core/expected';
import { logger } from '../core/logger';
import { Err, success, safeError } from '../core/err';
import { ChooseRModalWindow } from '..//ui/widgets/choose-r';
import { createStandaloneErrorDialog } from './utils';
import i18next from 'i18next';

let kLdLibraryPathVariable: string;
if (process.platform === 'darwin') {
  kLdLibraryPathVariable = 'DYLD_FALLBACK_LIBRARY_PATH';
} else {
  kLdLibraryPathVariable = 'LD_LIBRARY_PATH';
}

interface REnvironment {
  rScriptPath: string;
  version: string;
  envVars: Environment;
  ldLibraryPath: string;
}

function showRNotFoundError(error?: Error): void {
  const message = error?.message ?? i18next.t('detectRTs.couldNotLocateAnRInstallationOnTheSystem') ?? '';
  void createStandaloneErrorDialog(i18next.t('detectRTs.rNotFound'), message);
}

function executeCommand(command: string): Expected<string> {
  try {
    const output = execSync(command, { encoding: 'utf-8' });
    return ok(output.trim());
  } catch (error: unknown) {
    return err(safeError(error));
  }
}

export async function promptUserForR(): Promise<Expected<string | null>> {
  // nothing to do if RSTUDIO_WHICH_R is set
  const rstudioWhichR = getenv('RSTUDIO_WHICH_R');
  if (rstudioWhichR) {
    return ok(rstudioWhichR);
  }

  // discover available R installations
  const rInstalls = findRInstallationsWin32();
  if (rInstalls.length === 0) {
    return err();
  }

  // ask the user what version of R they'd like to use
  const dialog = new ChooseRModalWindow(rInstalls);
  const [path, error] = await dialog.showModal();
  if (error) {
    return err(error);
  }

  // if path is null, the operation was cancelled
  if (path == null) {
    return ok(null);
  }

  // set RSTUDIO_WHICH_R to signal which version of R to be used
  setenv('RSTUDIO_WHICH_R', path);
  return ok(path);
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
  } catch (error: unknown) {
    logger().logError(error);
    return safeError(error);
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
    const binDir = path.dirname(scriptPath);
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

  const result = spawnSync(R, ['--vanilla', '-s'], {
    encoding: 'utf-8',
    input: rQueryScript,
  });

  if (result.error) {
    return err(result.error);
  }

  // unwrap query results
  const [rVersion, rHome, rDocDir, rIncludeDir, rShareDir, rLdLibraryPath] = result.stdout.split(EOL);

  // put it all together
  return ok({
    rScriptPath: R,
    version: rVersion,
    envVars: {
      R_HOME: rHome,
      R_DOC_DIR: rDocDir,
      R_INCLUDE_DIR: rIncludeDir,
      R_SHARE_DIR: rShareDir,
    },
    ldLibraryPath: rLdLibraryPath,
  });
}

function scanForR(): Expected<string> {
  // if the RSTUDIO_WHICH_R environment variable is set, use that
  const rstudioWhichR = getenv('RSTUDIO_WHICH_R');
  if (rstudioWhichR) {
    logger().logDebug(`Using ${rstudioWhichR} (found by RSTUDIO_WHICH_R environment variable)`);
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
    logger().logDebug(`Using ${rLocation} (found by /usr/bin/which/R)`);
    return ok(rLocation);
  }

  // otherwise, look in some hard-coded locations
  const defaultLocations = ['/opt/local/bin/R', '/usr/local/bin/R', '/usr/bin/R'];

  // also check framework directory for macOS
  if (process.platform === 'darwin') {
    defaultLocations.push('/Library/Frameworks/R.framework/Resources/bin/R');
  }

  for (const location of defaultLocations) {
    if (existsSync(location)) {
      logger().logDebug(`Using ${rLocation} (found by searching known locations)`);
      return ok(location);
    }
  }

  // nothing found
  return err();
}

function findRInstallationsWin32() {
  const rInstallations: string[] = [];

  // list all installed versions from registry
  const keyName = 'HKEY_LOCAL_MACHINE\\SOFTWARE\\R-Core';
  const regQueryCommand = `reg query ${keyName} /s /v InstallPath`;
  const [output, error] = executeCommand(regQueryCommand);
  if (error) {
    logger().logError(error);
    return rInstallations;
  }

  // parse the actual path from the output
  const lines = output.split(EOL);
  for (const line of lines) {
    const match = /^\s*InstallPath\s*REG_SZ\s*(.*)$/.exec(line);
    if (match != null) {
      const rInstallation = match[1];
      if (isValidInstallationWin32(rInstallation)) {
        rInstallations.push(rInstallation);
      }
    }
  }

  return rInstallations;
}

function isValidInstallationWin32(installPath: string): boolean {
  const rBinPath = path.normalize(`${installPath}/bin/R.exe`);
  return existsSync(rBinPath);
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
  const lines = output.split(EOL);
  for (const line of lines) {
    const match = /^\s*InstallPath\s*REG_SZ\s*(.*)$/.exec(line);
    if (match != null) {
      const rLocation = match[1];
      return rLocation;
    }
  }

  return '';
}

function scanForRWin32(): Expected<string> {
  // if the RSTUDIO_WHICH_R environment variable is set, use that
  const rstudioWhichR = getenv('RSTUDIO_WHICH_R');
  if (rstudioWhichR) {
    logger().logDebug(`Using R ${rstudioWhichR} (found by RSTUDIO_WHICH_R environment variable)`);
    return ok(rstudioWhichR);
  }

  // look for a 64-bit version of R
  if (process.arch !== 'x32') {
    const x64InstallPath = findDefaultInstallPathWin32('R64');
    if (x64InstallPath && existsSync(x64InstallPath)) {
      const rPath = `${x64InstallPath}/bin/x64/R.exe`;
      logger().logDebug(`Using R ${rPath} (found via registry)`);
      return ok(rPath);
    }
  }

  // look for a 32-bit version of R
  const i386InstallPath = findDefaultInstallPathWin32('R');
  if (i386InstallPath && existsSync(i386InstallPath)) {
    const rPath = `${i386InstallPath}/bin/i386/R.exe`;
    logger().logDebug(`Using R ${rPath} (found via registry)`);
    return ok(rPath);
  }

  // nothing found; return empty filepath
  logger().logDebug('Failed to discover R');
  return err();
}

export function findDefault32Bit(): string {
  return findDefaultInstallPathWin32('R');
}

export function findDefault64Bit(): string {
  return findDefaultInstallPathWin32('R64');
}
