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

import { execSync, spawn, spawnSync } from 'child_process';
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

/**
 * True when the Windows R chooser was explicitly requested, via the
 * environment or by holding Ctrl at launch.
 */
export function rChooserRequested(): boolean {
  return getenv('RSTUDIO_DESKTOP_PROMPT_FOR_R').length !== 0 || desktop.isCtrlKeyDown();
}

/**
 * True when Windows startup is going to show the R chooser, as far as can be
 * predicted without consulting R: explicitly requested, or a first run with
 * no remembered selection to reuse.
 */
export function rChooserLikely(): boolean {
  if (rChooserRequested()) {
    return true;
  }
  if (getenv('RSTUDIO_WHICH_R').length !== 0) {
    return false;
  }
  return storedRCandidatesWin32().length === 0;
}

export async function promptUserForR(platform = process.platform): Promise<Expected<string | null>> {

  const options = ElectronDesktopOptions();

  if (platform === 'win32') {
    const showUi = rChooserRequested();

    if (!showUi) {
      // nothing to do if RSTUDIO_WHICH_R is set
      const rstudioWhichR = getenv('RSTUDIO_WHICH_R');
      if (rstudioWhichR) {
        logger().logDebug(`Using R from RSTUDIO_WHICH_R: ${rstudioWhichR}`);
        return ok(rstudioWhichR);
      }

      for (const candidate of storedRCandidatesWin32()) {
        if (!candidate.validate) {
          logger().logDebug(`Using default R installation at path: ${candidate.path}`);
          return ok(candidate.path);
        }

        logger().logDebug(`Trying version of R stored in RStudio Desktop options: ${candidate.path}`);
        if (isValidBinary(candidate.path)) {
          logger().logDebug(`Validation succeeded; using R: ${candidate.path}`);
          return ok(candidate.path);
        } else {
          logger().logDebug(`Validation failed; skipping R: ${candidate.path}`);
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

    // reset some options
    const path = data.binaryPath as string;
    options.setUseDefault32BitR(data.useDefault32BitR || false);
    options.setUseDefault64BitR(data.useDefault64BitR || false);
    options.setRExecutablePath(path);

    // if the user has changed the default rendering engine,
    // then we'll need to ask them to restart RStudio now
    const enginePref = options.renderingEngine() || 'auto';
    const engineValue = data.renderingEngine || 'auto';
    if (enginePref !== engineValue) {
      options.setRenderingEngine(engineValue);
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
interface RCandidateWin32 {
  path: string;

  // the defaults recorded in the registry are trusted as they stand; a path
  // the user stored in the options is checked before use
  validate: boolean;
}

/**
 * The R executables the Windows startup prefers when the user is not asked,
 * in order: the default 32-bit or 64-bit installation when the options ask
 * for one (and it exists), then the executable stored in the options.
 */
function storedRCandidatesWin32(): RCandidateWin32[] {
  const options = ElectronDesktopOptions();
  const candidates: RCandidateWin32[] = [];

  if (options.useDefault32BitR()) {
    logger().logDebug('User has requested the default 32-bit R installation.');
    const installPath = findDefault32Bit();
    if (installPath) {
      const rPath = `${installPath}/bin/i386/${kWindowsRExe}`;
      if (existsSync(rPath)) {
        candidates.push({ path: rPath, validate: false });
      }
    }
  }

  if (options.useDefault64BitR()) {
    logger().logDebug('User has requested the default 64-bit R installation.');
    const installPath = findDefault64Bit();
    if (installPath) {
      const rPath = `${installPath}/bin/x64/${kWindowsRExe}`;
      if (existsSync(rPath)) {
        candidates.push({ path: rPath, validate: false });
      }
    }
  }

  const rBinDir = options.rBinDir();
  if (rBinDir) {
    candidates.push({ path: fixWindowsRExecutablePath(`${rBinDir}/${kWindowsRExe}`), validate: true });
  }

  return candidates;
}

/**
 * The default installations recorded in the registry, 64-bit first, as
 * scanForRWin32 would try them.
 */
function registryRCandidatesWin32(): string[] {
  const candidates: string[] = [];

  if (process.arch === 'x64') {
    const x64InstallPath = findDefaultInstallPathWin32('R64');
    if (x64InstallPath) {
      candidates.push(`${x64InstallPath}/bin/x64/${kWindowsRExe}`);
    }
  }

  const i386InstallPath = findDefaultInstallPathWin32('R');
  if (i386InstallPath) {
    candidates.push(`${i386InstallPath}/bin/i386/${kWindowsRExe}`);
  }

  return candidates;
}

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

// Querying R costs a process launch (~150ms or more). The same executable is
// queried while scanning for installations and again when preparing the
// session environment, so successful results are remembered per path. The
// query also runs in the background from the moment the app starts (see
// startRDetection), which normally fills the cache before anything asks.
const rEnvironmentCache = new Map<string, REnvironment>();

export function detectREnvironment(rPath: string): Expected<REnvironment> {
  const cached = rEnvironmentCache.get(rPath);
  if (cached) {
    return ok(cached);
  }

  const rExecutable = rExecutableFor(rPath);
  logger().logDebug(`Querying information about R executable at path: ${rExecutable}`);

  const [spawned, spawnError] = expect(() => {
    return spawnSync(rExecutable.getAbsolutePath(), ['--vanilla', '-s'], {
      encoding: 'utf-8',
      input: rQueryScript(),
      env: rQueryEnvironment(),
    });
  });
  if (spawnError) {
    logger().logDebug(`Error querying information about R: ${spawnError}`);
    return err(spawnError);
  }

  return rememberREnvironment(
    rPath,
    parseRQueryResult(rPath, {
      stdout: spawned.stdout,
      stderr: spawned.stderr,
      status: spawned.status,
      error: spawned.error,
    }),
  );
}

/**
 * The asynchronous twin of detectREnvironment(); same query, same cache.
 */
export async function detectREnvironmentAsync(rPath: string): Promise<Expected<REnvironment>> {
  const cached = rEnvironmentCache.get(rPath);
  if (cached) {
    return ok(cached);
  }

  const rExecutable = rExecutableFor(rPath);
  logger().logDebug(`Querying information about R executable at path: ${rExecutable} (in background)`);

  const result = await new Promise<RQueryResult>((resolve) => {
    let stdout = '';
    let stderr = '';
    const child = spawn(rExecutable.getAbsolutePath(), ['--vanilla', '-s'], {
      env: rQueryEnvironment(),
      stdio: ['pipe', 'pipe', 'pipe'],
    });
    child.stdout.on('data', (data) => (stdout += data));
    child.stderr.on('data', (data) => (stderr += data));
    child.on('error', (error) => resolve({ stdout, stderr, status: null, error }));
    child.on('close', (code) => resolve({ stdout, stderr, status: code }));

    // R may exit before reading its input; that is reported through 'close'
    child.stdin.on('error', () => undefined);
    child.stdin.end(rQueryScript());
  });

  return rememberREnvironment(rPath, parseRQueryResult(rPath, result));
}

function rememberREnvironment(rPath: string, result: Expected<REnvironment>): Expected<REnvironment> {
  const [environment, error] = result;
  if (!error) {
    rEnvironmentCache.set(rPath, environment);
  }
  return result;
}

function rExecutableFor(rPath: string): FilePath {
  // resolve path to binary if we were given a directory
  const rExecutable = new FilePath(rPath);
  if (rExecutable.isDirectory()) {
    return rExecutable.completeChildPath(process.platform === 'win32' ? kWindowsRExe : 'R');
  }
  return rExecutable;
}

// small script for querying information about R; the marker character
// '\x1E' lets us find our output even when the R installation prints
// something on startup
function rQueryScript(): string {
  return String.raw`
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
}

function rQueryEnvironment(): NodeJS.ProcessEnv {
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
  return envCopy;
}

/** What running the query script produced. */
export interface RQueryResult {
  stdout: string | null;
  stderr: string | null;
  status: number | null;
  error?: Error;
}

/**
 * Turns the output of the R query script into the environment needed to
 * launch a session with that R.
 */
export function parseRQueryResult(rPath: string, result: RQueryResult): Expected<REnvironment> {
  let stdout = result.stdout || '';
  logger().logDebug(`stdout: ${stdout.replaceAll('\x1E', EOL).replaceAll('\x1F', ';') || '[no stdout produced]'}`);
  logger().logDebug(`stderr: ${result.stderr || '[no stderr produced]'}`);
  logger().logDebug(`status: ${result.status} [${result.status === 0 ? 'success' : 'failure'}]`);
  if (result.error) {
    logger().logDebug(`error:  ${result.error}`);
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

function defaultRLocationsPosix(): string[] {
  const defaultLocations = ['/usr/bin/R', '/usr/local/bin/R', '/opt/local/bin/R'];

  if (process.platform == 'darwin') {
    // For Mac, we want to first look in a list of hard-coded locations
    // also check framework directory and then homebrew ARM locations for macOS
    defaultLocations.push('/Library/Frameworks/R.framework/Resources/bin/R');
    defaultLocations.push('/opt/homebrew/bin/R');
  }

  return defaultLocations;
}

// for linux, look for R on the PATH
// should we launch the default shell to pick up the user modifications to the path?
function rOnPathLinux(): string | null {
  const [rLocation, error] = executeCommand('/usr/bin/which R');
  if (!error && rLocation) {
    return rLocation;
  }
  return null;
}

function scanForRPosix(): Expected<string> {
  if (process.platform !== 'darwin') {
    const rLocation = rOnPathLinux();
    if (rLocation) {
      logger().logDebug(`Using ${rLocation} (found by /usr/bin/which/R)`);
      return ok(rLocation);
    }
  }

  for (const location of defaultRLocationsPosix()) {
    if (isValidBinary(location)) {
      logger().logDebug(`Using ${location} (found by searching known locations)`);
      return ok(location);
    }
  }
  // nothing found
  return err();
}

// --- early detection --------------------------------------------------------

let rDetection: Promise<void> | undefined;

/**
 * The R executables startup would consider, in order, without asking the
 * user: RSTUDIO_WHICH_R, then the platform's stored or well-known locations.
 */
function rCandidates(): string[] {
  const rstudioWhichR = getenv('RSTUDIO_WHICH_R');
  if (rstudioWhichR) {
    return [rstudioWhichR];
  }

  if (process.platform === 'win32') {
    return [...storedRCandidatesWin32().map((candidate) => candidate.path), ...registryRCandidatesWin32()];
  }

  const candidates = defaultRLocationsPosix();
  if (process.platform !== 'darwin') {
    const rLocation = rOnPathLinux();
    if (rLocation) {
      candidates.unshift(rLocation);
    }
  }
  return candidates;
}

async function detectFirstValidR(): Promise<void> {
  for (const candidate of rCandidates()) {
    if (!existsSync(candidate)) {
      continue;
    }
    const [, error] = await detectREnvironmentAsync(candidate);
    if (!error) {
      return;
    }
  }
}

/**
 * Starts querying the R that startup is going to pick, so the answer is in
 * the cache by the time the launch sequence asks for it. Call as early as
 * possible; Electron's own startup is long enough to hide the query.
 */
export function startRDetection(): void {
  if (rDetection !== undefined) {
    return;
  }
  rDetection = detectFirstValidR().catch((error: unknown) => logger().logError(error));
}

/**
 * Resolves once the early query (if one was started) has finished, so that
 * the synchronous detection path finds its result in the cache.
 */
export async function rDetectionReady(): Promise<void> {
  if (rDetection !== undefined) {
    await rDetection;
  }
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

  // look for the default 64-bit, then 32-bit, version of R
  for (const binaryPath of registryRCandidatesWin32()) {
    if (isValidBinary(binaryPath)) {
      logger().logDebug(`Using R ${binaryPath} (found via registry)`);
      return ok(binaryPath);
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
