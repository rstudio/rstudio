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
import { EOL } from 'os';

import { Environment, getenv, setenv, setVars } from '../core/environment';
import { Expected, ok, err, expect } from '../core/expected';
import { logger } from '../core/logger';
import { Err, success, safeError } from '../core/err';
import { ChooseRModalWindow } from '..//ui/widgets/choose-r';
import { createStandaloneErrorDialog, handleLocaleCookies } from './utils';
import { t } from 'i18next';

import { ElectronDesktopOptions } from './preferences/electron-desktop-options';
import { FilePath } from '../core/file-path';
import { dialog } from 'electron';

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
  const message = error?.message ?? t('detectRTs.couldNotLocateAnRInstallationOnTheSystem') ?? '';
  void createStandaloneErrorDialog(t('detectRTs.rNotFound'), message);
}

function executeCommand(command: string): Expected<string> {
  return expect(() => {
    return execSync(command, { encoding: 'utf-8' }).trim();
  });
}

export async function promptUserForR(platform = process.platform): Promise<Expected<string | null>> {
  
  if (platform === 'win32') {

    const desktop = await import('../native/desktop.node');

    const showUi =
      getenv('RSTUDIO_DESKTOP_PROMPT_FOR_R').length !== 0 ||
      desktop.isCtrlKeyDown();

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
        const rPath = `${rBinDir}/R.exe`;
        if (isValidBinary(rPath)) {
          logger().logDebug(`Using R from preferences: ${rPath}`);
          return ok(rPath);
        } else {
          logger().logDebug(`rBinDir (${rPath}) does not exist; ignoring`);
        }
      }

    }

    // discover available R installations
    const rInstalls = findRInstallationsWin32();
    if (rInstalls.length === 0) {
      return err(new Error('No R installations found via registry or common R install locations.'));
    }

    // ask the user what version of R they'd like to use
    const chooseRDialog = new ChooseRModalWindow(rInstalls);
    void handleLocaleCookies(chooseRDialog);

    const [data, error] = await chooseRDialog.showModal();
    if (error) {
      return err(error);
    }

    // if path is null, the operation was cancelled by the user
    if (data == null) {
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
      dialog.showMessageBoxSync({
        title: t('chooseRDialog.renderingEngineChangedTitle'),
        message: t('chooseRDialog.renderingEngineChangedMessage'),
        type: 'info',
      });

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
export function prepareEnvironment(rPath?: string): Err {
  try {
    return prepareEnvironmentImpl(rPath);
  } catch (error: unknown) {
    logger().logError(error);
    return safeError(error);
  }
}

function prepareEnvironmentImpl(rPath?: string): Err {

  // attempt to detect R environment
  const [rEnvironment, error] = detectREnvironment(rPath);
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

export function detectREnvironment(rPath?: string): Expected<REnvironment> {

  // scan for R
  const [R, scanError] = rPath ? ok(rPath) : scanForR();
  if (scanError) {
    showRNotFoundError();
    return err(scanError);
  }

  // resolve path to binary if we were given a directory
  let rExecutable = new FilePath(R);
  
  if (rExecutable.isDirectory()) {
    rExecutable = rExecutable.completeChildPath(process.platform === 'win32' ? 'R.exe' : 'R');
  }

  // generate small script for querying information about R
  const rQueryScript = String.raw`writeLines(c(
  format(getRversion()),
  R.home(),
  R.home("doc"),
  R.home("include"),
  R.home("share"),
  paste(R.version$crt, collapse = ""),
  .Platform$r_arch,
  Sys.getenv("${kLdLibraryPathVariable}")
))`;

  const [result, error] = expect(() => {
    return spawnSync(rExecutable.getAbsolutePath(), ['--vanilla', '-s'], {
      encoding: 'utf-8',
      input: rQueryScript,
      env: {}
    });
  });

  if (error) {
    return err(error);
  }

  // NOTE: It's possible for spawnSync to fail to launch a process,
  // and so exit with a non-zero status code, but without an error.
  // For that reason, we need to check for a non-zero exit code
  // rather than just a non-null error.
  if (result.status !== 0) {
    return err(result.error ?? new Error(t('common.unknownErrorOccurred')));
  }

  // unwrap query results
  const [
    rVersion,
    rHome,
    rDocDir,
    rIncludeDir,
    rShareDir,
    rRuntime,
    rArch,
    rLdLibraryPath
  ] = result.stdout.split(EOL);

  // put it all together
  return ok({
    rScriptPath: R,
    version: rVersion,
    envVars: {
      R_HOME: rHome,
      R_DOC_DIR: rDocDir,
      R_INCLUDE_DIR: rIncludeDir,
      R_SHARE_DIR: rShareDir,
      R_RUNTIME: rRuntime,
      R_ARCH: rArch,
    },
    ldLibraryPath: rLdLibraryPath,
  });

}

function scanForR(): Expected<string> {
  // if the RSTUDIO_WHICH_R environment variable is set, use that
  // note that this does not pick up variables set in a user's bash profile, for example
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

function queryRegistry(cmd: string, rInstallations: Set<string>): Set<string> {
  logger().logDebug(`Querying registry for ${cmd}`);
  const [output, error] = executeCommand(cmd);
  if (error) {
    logger().logErrorMessage(`Error querying the Windows registry: ${error}`);
    return rInstallations;
  }      
    
  // parse the actual path from the output
  const lines = output.split(EOL);
  for (const line of lines) {
    const match = /^\s*InstallPath\s*REG_SZ\s*(.*)$/.exec(line);
    if (match != null) {
      const rInstallation = match[1];
      if (existsSync(rInstallation)) {
        rInstallations.add(rInstallation);
      }
    }
  }
  return rInstallations;
}

export function findRInstallationsWin32(): string[] {

  const rInstallations = new Set<string>();

  for (const view of ['/reg:32', '/reg:64']) {

    // list all installed versions from registry
    const keyNames = [
      'HKEY_LOCAL_MACHINE',
      'HKEY_CURRENT_USER',
    ];

    // look specifically for R or R64, ignore Rtools directory
    const rBinaryNames = ['R', 'R64'];

    const regQueryCommands = keyNames.flatMap(key => rBinaryNames.map(
      rBin => `%SystemRoot%\\System32\\reg.exe query ${key}\\SOFTWARE\\R-Core\\${rBin} /s /v InstallPath ${view}`));  
    regQueryCommands.map(cmd => queryRegistry(cmd, rInstallations));

    logger().logInfo(`Found ${rInstallations.size} in the registry.`);
  }

  // look for R installations in some common locations
  const commonLocations = [
    'C:/R',
    `${getenv('ProgramFiles')}/R`,
    `${getenv('ProgramFiles(x86)')}/R`,
    `${getenv('LOCALAPPDATA')}/Programs/R`
  ];

  for (const location of commonLocations) {
    logger().logDebug(`Checking common R installation path: ${location}`)
    // nothing to do if it doesn't exist
    if (!existsSync(location)) {
      continue;
    }

    // read directories and check if they're valid R installations
    const rDirs = readdirSync(location, { encoding: 'utf-8' });
    for (const rDir of rDirs) {
      const path = join(location, rDir);
      if (existsSync(path)) {
        rInstallations.add(path);
      }
    }

  }

  return Array.from(rInstallations.values());

}

export function isValidInstallation(rInstallPath: string): boolean {
  const rExeName = process.platform === 'win32' ? 'R.exe' : 'R';
  const rExePath = path.normalize(`${rInstallPath}/bin/${rExeName}`);
  return isValidBinary(rExePath);
}

export function isValidBinary(rExePath: string): boolean {
  const [_, error] = detectREnvironment(rExePath);
  return error == null;
}

function findDefaultInstallPathWin32(version: string): string {

  for (const view of ['/reg:32', '/reg:64']) {
    // list all installed versions from registry
    const keyNames = [
      'HKEY_LOCAL_MACHINE',
      'HKEY_CURRENT_USER',
    ];

    const regQueryCommands = keyNames.flatMap(key => 
      `%SystemRoot%\\System32\\reg.exe query ${key}\\SOFTWARE\\R-Core\\${version} /v InstallPath ${view}`
    );

    // query registry for R install path
    for (const regQueryCommand of regQueryCommands) {
      const [output, error] = executeCommand(regQueryCommand);
      if (error) {
        logger().logError(error);
        continue;
      }

      // parse the actual path from the output
      const lines = output.split(EOL);
      for (const line of lines) {
        const match = /^\s*InstallPath\s*REG_SZ\s*(.*)$/.exec(line);
        if (match != null) {
          const rLocation = match[1];
          logger().logInfo(`Found R installation: ${rLocation}`);
          return rLocation;
        }
      }
    }

  }

  logger().logWarning('No default R installations found. R may have been installed without writing registry entries.');

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
    if (x64InstallPath) {
      const x64BinaryPath = `${x64InstallPath}/bin/x64/R.exe`;
      if (isValidBinary(x64BinaryPath)) {
        logger().logDebug(`Using R ${x64BinaryPath} (found via registry)`);
        return ok(x64BinaryPath);
      }
    }
  }

  // look for a 32-bit version of R
  const i386InstallPath = findDefaultInstallPathWin32('R');
  if (i386InstallPath) {
    const i386BinaryPath = `${i386InstallPath}/bin/i386/R.exe`;
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
