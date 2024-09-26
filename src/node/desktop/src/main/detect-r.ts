/*
 * detect-r.ts
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

import path, { join } from 'path';

import { execSync, spawnSync } from 'child_process';
import { existsSync, readdirSync } from 'fs';

import { Environment, getenv, setenv, setVars } from '../core/environment';
import { Expected, ok, err, expect } from '../core/expected';
import { logger } from '../core/logger';
import { Err, success, safeError } from '../core/err';
import { ChooseRModalWindow } from '..//ui/widgets/choose-r';
import { createStandaloneErrorDialog, handleLocaleCookies } from './utils';
import { t } from 'i18next';

import { ElectronDesktopOptions, fixWindowsRExecutablePath } from './preferences/electron-desktop-options';
import { FilePath } from '../core/file-path';

import desktop from '../native/desktop.node';
import { EOL } from 'os';
import { kWindowsRExe } from '../ui/utils';
import { dialog } from 'electron';
import { appState } from './app-state';

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

export async function showRNotFoundError(error?: Error) {
  const message = error?.message ?? t('detectRTs.couldNotLocateAnRInstallationOnTheSystem');
  await createStandaloneErrorDialog(t('detectRTs.rNotFound'), message);
}

function executeCommand(command: string): Expected<string> {
  return expect(() => {
    return execSync(command, { encoding: 'utf-8' }).trim();
  });
}

export async function promptUserForR(platform = process.platform): Promise<Expected<string | null>> {
  if (platform === 'win32') {
    const showUi = getenv('RSTUDIO_DESKTOP_PROMPT_FOR_R').length !== 0 || desktop.isCtrlKeyDown();

    if (!showUi) {
      // nothing to do if RSTUDIO_WHICH_R is set
      const rstudioWhichR = getenv('RSTUDIO_WHICH_R');
      if (rstudioWhichR) {
        logger().logDebug(`Using R from RSTUDIO_WHICH_R: ${rstudioWhichR}`);
        return ok(rstudioWhichR);
      }

      // if the user selected a version of R previously, then use it
      const rBinDir = ElectronDesktopOptions().rBinDir();
      if (rBinDir) {
        const rPath = fixWindowsRExecutablePath(`${rBinDir}/${kWindowsRExe}`);
        logger().logDebug(`Trying version of R stored in RStudio Desktop options: ${rPath}`);
        if (isValidBinary(rPath)) {
          logger().logDebug(`Validation succeeded; using R: ${rPath}`);
          return ok(rPath);
        } else {
          logger().logDebug(`Validation failed; skipping R: ${rPath}`);
        }
      }
    }

    // discover available R installations
    const rInstalls = findRInstallationsWin32();
    if (rInstalls.length === 0) {
      logger().logDebug('No R installations found via registry or common R install locations.');
    }

    // ask the user what version of R they'd like to use
    const chooseRDialog = new ChooseRModalWindow(rInstalls);
    void handleLocaleCookies(chooseRDialog);

    const [data, error] = await chooseRDialog.showModal();
    if (error) {
      return err(error);
    }

    // if path is null, the operation was cancelled by the user
    if (data == null || data.binaryPath == null) {
      return ok(null);
    }

    // save the stored version of R
    const path = data.binaryPath as string;

    ElectronDesktopOptions().setRExecutablePath(path);

    // if the user has changed the default rendering engine,
    // then we'll need to ask them to restart RStudio now
    const enginePref = ElectronDesktopOptions().renderingEngine() || 'auto';
    const engineValue = data.renderingEngine || 'auto';
    if (enginePref !== engineValue) {
      ElectronDesktopOptions().setRenderingEngine(engineValue);
      appState().modalTracker.trackElectronModalSync(() =>
        dialog.showMessageBoxSync({
          title: t('chooseRDialog.renderingEngineChangedTitle'),
          message: t('chooseRDialog.renderingEngineChangedMessage'),
          type: 'info',
        }),
      );

      // TODO: It'd be nice if we could use app.relaunch(), but that doesn't
      // seem to do what we want it to?
      return ok(null);
    }

    // set RSTUDIO_WHICH_R to signal which version of R to be used
    setenv('RSTUDIO_WHICH_R', path);
    return ok(path);
  }

  return err(new Error('This window can only be opened on Windows'));
}

/**
 * Detect R and prepare environment for launching the rsession process.
 *
 * This entails setting environment variables relevant to R on startup
 * // (for example, R_HOME) and other platform-specific work required
 * for R to launch.
 */
export function prepareEnvironment(rPath: string): Err {
  try {
    return prepareEnvironmentImpl(rPath);
  } catch (error: unknown) {
    logger().logError(error);
    return safeError(error);
  }
}

function prepareEnvironmentImpl(rPath: string): Err {
  // attempt to detect R environment
  const [rEnvironment, error] = detectREnvironment(rPath);
  if (error) {
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

export function detectREnvironment(rPath: string): Expected<REnvironment> {
  // resolve path to binary if we were given a directory
  let rExecutable = new FilePath(rPath);
  if (rExecutable.isDirectory()) {
    rExecutable = rExecutable.completeChildPath(process.platform === 'win32' ? kWindowsRExe : 'R');
  }

  // generate small script for querying information about R
  // we write the marker character '\x1E' just in case the R installation
  // prints some output on startup, so we can find and trim that out
  const rQueryScript = String.raw`
cat("\x1E", sep = "")
writeLines(sep = "\x1F", c(
  format(getRversion()),
  R.home(),
  R.home("doc"),
  R.home("include"),
  R.home("share"),
  paste(R.version$crt, collapse = ""),
  .Platform$r_arch,
  Sys.getenv("${kLdLibraryPathVariable}"),
  Sys.getenv("R_PLATFORM")
))`;

  logger().logDebug(`Querying information about R executable at path: ${rExecutable}`);

  // remove R-related environment variables before invoking R
  // note that we intentionally preserve an already-set LD_LIBRARY_PATH
  // see https://github.com/rstudio/rstudio/issues/15044 for motivation
  const envCopy = Object.assign({}, process.env);
  delete envCopy['R_HOME'];
  delete envCopy['R_ARCH'];
  delete envCopy['R_DOC_DIR'];
  delete envCopy['R_INCLUDE_DIR'];
  delete envCopy['R_RUNTIME'];
  delete envCopy['R_SHARE_DIR'];
  delete envCopy['R_PLATFORM'];

  const [result, error] = expect(() => {
    return spawnSync(rExecutable.getAbsolutePath(), ['--vanilla', '-s'], {
      encoding: 'utf-8',
      input: rQueryScript,
      env: envCopy,
    });
  });

  let stdout = result.stdout || '';
  logger().logDebug(`stdout: ${stdout.replaceAll('\x1E', EOL).replaceAll('\x1F', ';') || '[no stdout produced]'}`);
  logger().logDebug(`stderr: ${result.stderr || '[no stderr produced]'}`);
  logger().logDebug(`status: ${result.status} [${result.status === 0 ? 'success' : 'failure'}]`);
  if (result.error) {
    logger().logDebug(`error:  ${result.error}`);
  }

  if (error) {
    logger().logDebug(`Error querying information about R: ${error}`);
    return err(error);
  }

  // NOTE: It's possible for spawnSync to fail to launch a process,
  // and so exit with a non-zero status code, but without an error.
  // For that reason, we need to check for a non-zero exit code
  // rather than just a non-null error.
  //
  // As an added safe-guard, if an error occurs, but we still have
  // something on 'stdout', try and use that to activate this version
  // of R. Maybe the process exited abnormally for an unknown reason,
  // even though it started and gave us the necessary information?
  //
  // Also, contrary to the declared type signatures, the values in 'result' can
  // be null, so check those in a 'null'-y way.
  if (!stdout) {
    logger().logDebug('Error querying information about R: no output available');
    return err(new Error(t('common.unknownErrorOccurred')));
  }

  // find marker character for our output
  const index = stdout.indexOf('\x1E');
  if (index === -1) {
    logger().logError('internal error: missing output marker in R output');
    return err(new Error(t('common.unknownErrorOccurred')));
  }

  // trim off marker
  stdout = stdout.substring(index + 1);

  // unwrap query results
  const [rVersion, rHome, rDocDir, rIncludeDir, rShareDir, rRuntime, rArch, rLdLibraryPath, rPlatform] =
    stdout.split('\x1F');

  let adjustedRLdLibraryPath = rLdLibraryPath;

  // if this appears to be a conda installation of R, manually set LD_LIBRARY_PATH appropriately
  // https://github.com/rstudio/rstudio/issues/13184
  if (adjustedRLdLibraryPath.length === 0 && rPlatform.indexOf('-conda-') !== -1) {
    const rLibPaths = [`${rHome}/lib`, `${rHome}/../../lib`]
      .filter((value) => existsSync(value))
      .map((value) => path.normalize(value));

    adjustedRLdLibraryPath = rLibPaths.join(':');
  }

  if (process.platform !== 'win32' && getenv(kLdLibraryPathVariable) != '') {
    logger().logDebug(`Pre-pending user-defined ${kLdLibraryPathVariable} to path set by R: ${adjustedRLdLibraryPath}`);
    adjustedRLdLibraryPath = getenv(kLdLibraryPathVariable) + ':' + adjustedRLdLibraryPath;
  }

  // put it all together
  return ok({
    rScriptPath: rPath,
    version: rVersion,
    envVars: {
      R_HOME: rHome,
      R_DOC_DIR: rDocDir,
      R_INCLUDE_DIR: rIncludeDir,
      R_SHARE_DIR: rShareDir,
      R_RUNTIME: rRuntime,
      R_ARCH: rArch,
      R_PLATFORM: rPlatform,
    },
    ldLibraryPath: adjustedRLdLibraryPath,
  });
}

export function scanForR(): Expected<string> {
  // if the RSTUDIO_WHICH_R environment variable is set, use that
  // note that this does not pick up variables set in a user's bash profile, for example
  const rstudioWhichR = getenv('RSTUDIO_WHICH_R');

  if (rstudioWhichR) {
    logger().logDebug(`Using ${rstudioWhichR} (found by RSTUDIO_WHICH_R environment variable)`);
    return ok(rstudioWhichR);
  }

  // otherwise, use platform-specific lookup strategies
  logger().logDebug('Scanning system for R installations');
  if (process.platform === 'win32') {
    return scanForRWin32();
  } else {
    return scanForRPosix();
  }
}

function scanForRPosix(): Expected<string> {
  const defaultLocations = ['/usr/bin/R', '/usr/local/bin/R', '/opt/local/bin/R'];

  if (process.platform == 'darwin') {
    // For Mac, we want to first look in a list of hard-coded locations
    // also check framework directory and then homebrew ARM locations for macOS
    defaultLocations.push('/Library/Frameworks/R.framework/Resources/bin/R');
    defaultLocations.push('/opt/homebrew/bin/R');
  } else {
    // for linux, look for R on the PATH
    // should we launch the default shell to pick up the user modifications to the path?
    const [rLocation, error] = executeCommand('/usr/bin/which R');
    if (!error && rLocation) {
      logger().logDebug(`Using ${rLocation} (found by /usr/bin/which/R)`);
      return ok(rLocation);
    }
  }

  for (const location of defaultLocations) {
    if (isValidBinary(location)) {
      logger().logDebug(`Using ${location} (found by searching known locations)`);
      return ok(location);
    }
  }
  // nothing found
  return err();
}

export function findRInstallationsWin32(): string[] {
  const rInstallations = desktop.searchRegistryForInstallationsOfR();
  logger().logDebug(`Found the following R installations in the registry: ${JSON.stringify(rInstallations, null, 2)}`);

  // look for R installations in some common locations
  const commonLocations = [
    'C:/R',
    `${getenv('ProgramFiles')}/R`,
    `${getenv('ProgramFiles(x86)')}/R`,
    `${getenv('LOCALAPPDATA')}/Programs/R`,
  ];

  for (const location of commonLocations) {
    // nothing to do if it doesn't exist
    if (!existsSync(location)) {
      continue;
    }

    // read directories and check if they're valid R installations
    const rDirs = readdirSync(location, { encoding: 'utf-8' });
    for (const rDir of rDirs) {
      const path = join(location, rDir);
      if (existsSync(path)) {
        logger().logDebug(`Found R installation at path: ${path}`);
        rInstallations.push(path);
      }
    }
  }

  // Remove duplicates
  const uniqueInstallations = Array.from(new Set(rInstallations));
  logger().logDebug(`Found the following R installations: ${JSON.stringify(uniqueInstallations, null, 2)}`);
  return uniqueInstallations;
}

export function isValidInstallation(rInstallPath: string, archDir: string): boolean {
  if (process.platform !== 'win32') {
    logger().logErrorMessage('Windows-only API invoked on non-Windows codepath (isValidInstallation)');
    return true;
  }

  const rExePath = path.normalize(`${rInstallPath}/bin/${archDir}/${kWindowsRExe}`);
  return isValidBinary(rExePath);
}

export function isValidBinary(rExePath: string): boolean {
  if (!existsSync(rExePath)) {
    return false;
  }

  logger().logDebug(`Validating R installation at path: ${rExePath}`);
  const [_, error] = detectREnvironment(rExePath);
  return error == null;
}

function findDefaultInstallPathWin32(registryVersionKey: string): string {
  const rLocation = desktop.searchRegistryForDefaultInstallationOfR(registryVersionKey);
  if (rLocation.length === 0) {
    logger().logWarning('No default R installation was found in the registry.');
    return '';
  }

  if (!existsSync(rLocation)) {
    logger().logWarning(`Default version of R recorded in registry ${rLocation} does not exist.`);
    return '';
  }

  return rLocation;
}

function scanForRWin32(): Expected<string> {
  // if the RSTUDIO_WHICH_R environment variable is set, use that
  const rstudioWhichR = getenv('RSTUDIO_WHICH_R');
  if (rstudioWhichR) {
    logger().logDebug(`Using R ${rstudioWhichR} (found by RSTUDIO_WHICH_R environment variable)`);
    return ok(rstudioWhichR);
  }

  // look for a 64-bit version of R
  if (process.arch === 'x64') {
    const x64InstallPath = findDefaultInstallPathWin32('R64');
    if (x64InstallPath) {
      const x64BinaryPath = `${x64InstallPath}/bin/x64/${kWindowsRExe}`;
      if (isValidBinary(x64BinaryPath)) {
        logger().logDebug(`Using R ${x64BinaryPath} (found via registry)`);
        return ok(x64BinaryPath);
      }
    }
  }

  // look for a 32-bit version of R
  const i386InstallPath = findDefaultInstallPathWin32('R');
  if (i386InstallPath) {
    const i386BinaryPath = `${i386InstallPath}/bin/i386/${kWindowsRExe}`;
    if (isValidBinary(i386BinaryPath)) {
      logger().logDebug(`Using R ${i386BinaryPath} (found via registry)`);
      return ok(i386BinaryPath);
    }
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
