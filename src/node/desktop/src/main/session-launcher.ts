/*
 * session-launcher.ts
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

import { ChildProcess, execSync, spawn } from 'child_process';
import { app, BrowserWindow } from 'electron';
import fs, { existsSync, rmSync } from 'fs';

import { getenv, setenv, unsetenv } from '../core/environment';
import { Err, safeError, success } from '../core/err';
import { FilePath } from '../core/file-path';
import { readStringArrayFromFile } from '../core/file-serializer';
import { logger } from '../core/logger';
import { kRStudioInitialProject } from '../core/r-user-data';
import { generateShortenedUuid, localPeer } from '../core/system';

import i18next from 'i18next';
import { setTimeoutPromise, sleepPromise } from '../core/wait-utils';
import { DesktopActivation } from './activation-overlay';
import { appState } from './app-state';
import { ApplicationLaunch } from './application-launch';
import { buildInfo } from './build-info';
import { PendingQuit } from './gwt-callback';
import LogOptions from './log-options';
import { closeAllSatellites, MainWindow } from './main-window';
import { ElectronDesktopOptions } from './preferences/electron-desktop-options';
import { EXIT_FAILURE } from './program-status';
import { waitForUrlWithTimeout } from './url-utils';
import {
  createStandaloneErrorDialog,
  findRepoRoot,
  getCurrentlyUniqueFolderName,
  userLogPath,
} from './utils';
import path from 'path';
import { createSplashScreen } from './splash-screen';

// This allows TypeScript to pick up the magic constants auto-generated by Forge's Webpack
// plugin that tells the Electron app where to look for the Webpack-bundled app code (depending on
// whether you're running in development or production).
declare const ERROR_WINDOW_WEBPACK_ENTRY: string;

export interface LaunchContext {
  host: string;
  port: number;
  url: string;
  argList: string[];
}

let fallbackInstance: string | null = null;

/**
 * @returns A "probably unique" temporary folder name (folder isn't created by this call)
 */
function fallbackLibraryPath(): string {
  if (!fallbackInstance) {
    fallbackInstance = getCurrentlyUniqueFolderName('rstudio-fallback-library-path-').getAbsolutePath();
  }
  return fallbackInstance;
}

function launchProcess(absPath: FilePath, argList: string[]): ChildProcess {
  // create local copy of environment
  const env = Object.assign({}, process.env);

  if (process.platform === 'darwin') {
    // Create fallback library path (use TMPDIR so it's user-specific):
    // reticulate needs to do some DYLD_FALLBACK_LIBRARY_PATH shenanigans to work with Anaconda Python;
    // the solution is to have RStudio launch with a special DYLD_FALLBACK_LIBRARY_PATH, and let
    // reticulate set the associated path to a symlink of its choosing on load.
    const libraryPath = fallbackLibraryPath();
    env['RSTUDIO_FALLBACK_LIBRARY_PATH'] = libraryPath;

    // and ensure it's placed on the fallback library path
    const dyldFallbackLibraryPath = `${getenv('DYLD_FALLBACK_LIBRARY_PATH')}:${libraryPath}`;

    // on macOS with the hardened runtime, we can no longer rely on dyld
    // to lazy-load symbols from libR.dylib; to resolve this, we use
    // DYLD_INSERT_LIBRARIES to inject the library we wish to use
    const rHome = new FilePath(getenv('R_HOME'));
    const rLib = rHome.completePath('lib/libR.dylib');
    const dyldArgs = [
      '-e',
      `DYLD_INSERT_LIBRARIES=${rLib.getAbsolutePath()}`,
      '-e',
      `DYLD_FALLBACK_LIBRARY_PATH=${dyldFallbackLibraryPath}`,
    ];

    // launch via /usr/bin/arch, so we can control whether the OS requests
    // x86 or arm64 versions of the libraries in the launched rsession
    const path = absPath.getAbsolutePath();
    if (process.arch === 'arm64') {
      const fileInfo = execSync(`/usr/bin/file "${rLib}"`, { encoding: 'utf-8' });
      if (fileInfo.indexOf('arm64') === -1 && fileInfo.indexOf('x86_64') !== -1) {
        argList = ['-x86_64', ...dyldArgs, path, ...argList];
        absPath = new FilePath('/usr/bin/arch');
      } else {
        argList = ['-arm64', ...dyldArgs, path, ...argList];
        absPath = new FilePath('/usr/bin/arch');
      }
    } else {
      argList = ['-x86_64', ...dyldArgs, path, ...argList];
      absPath = new FilePath('/usr/bin/arch');
    }
  }

  const rsessionOptions = new LogOptions('rsession');
  env['RS_LOG_LEVEL'] = env['RS_LOG_LEVEL'] ?? rsessionOptions.getLogLevel().toUpperCase();

  logger().logDebug(`Launching rsession: ${absPath.getAbsolutePath()} ${argList.join(' ')}`);
  logger().logDebug(`R_HOME: ${getenv('R_HOME')}`);
  logger().logDebug(`RS_INITIAL_PROJECT: ${getenv('RS_INITIAL_PROJECT')}`);
  logger().logDebug(`RS_LOG_LEVEL: ${getenv('RS_LOG_LEVEL')}`);

  if (!appState().runDiagnostics) {
    return spawn(absPath.getAbsolutePath(), argList, { env: env });
  } else {
    // for diagnostics, redirect child process stdio to this process
    return spawn(absPath.getAbsolutePath(), argList, { stdio: 'inherit', env: env });
  }
}

function abendLogPath(): FilePath {
  return userLogPath().completePath('rsession_abort_msg.log');
}

export class SessionLauncher {
  host = '127.0.0.1';
  sessionProcess?: ChildProcess;
  mainWindow?: MainWindow;
  static launcherToken = generateShortenedUuid();
  sessionStdout: string[] = [];
  sessionStderr: string[] = [];
  private nextSessionUrl?: string;
  private splash: BrowserWindow | undefined;
  private showSplash = process.env.NODE_ENV !== 'TEST';
  private splashDelay: number;
  private splashLinger: number;

  constructor(
    private sessionPath: FilePath,
    private confPath: FilePath,
    private filename: FilePath,
    private appLaunch: ApplicationLaunch,
    private windowAllClosedHandler: (() => void) | null,
  ) {
    this.splashDelay = process.env.RS_SPLASH_DELAY ? parseInt(process.env.RS_SPLASH_DELAY) : 100;
    this.splashLinger = process.env.RS_SPLASH_LINGER ? parseInt(process.env.RS_SPLASH_LINGER) : 1300;
    if (process.env.RS_NO_SPLASH) {
      this.showSplash = false;
    }

    // don't show splash screen if started using --run-diagnostics
    if (appState().runDiagnostics) {
      this.showSplash = false;
    }
  }

  launchFirstSession(installPath: FilePath, devMode: boolean): void {
    appState().activation().on(DesktopActivation.LAUNCH_FIRST_SESSION, this.onLaunchFirstSession.bind(this));
    appState().activation().on(DesktopActivation.LAUNCH_ERROR, this.onLaunchError.bind(this));

    // This will ultimately trigger one of the above events to continue with startup (or failure).
    appState().activation().getInitialLicense(installPath, devMode);
  }

  private launchFirst(): Err {
    // build a new new launch context
    const launchContext = this.buildLaunchContext();

    // show help home on first run
    launchContext.argList.push('--show-help-home', '1');

    logger().logDiagnostic('\nAttempting to launch R session...');
    logger().logDiagnosticEnvVar('RSTUDIO_WHICH_R');
    logger().logDiagnosticEnvVar('R_HOME');
    logger().logDiagnosticEnvVar('R_DOC_DIR');
    logger().logDiagnosticEnvVar('R_INCLUDE_DIR');
    logger().logDiagnosticEnvVar('R_SHARE_DIR');
    logger().logDiagnosticEnvVar('R_LIBS');
    logger().logDiagnosticEnvVar('R_LIBS_USER');
    logger().logDiagnosticEnvVar('DYLD_LIBRARY_PATH');
    logger().logDiagnosticEnvVar('DYLD_FALLBACK_LIBRARY_PATH');
    logger().logDiagnosticEnvVar('LD_LIBRARY_PATH');
    logger().logDiagnosticEnvVar('PATH');
    logger().logDiagnosticEnvVar('HOME');
    logger().logDiagnosticEnvVar('R_USER');
    logger().logDiagnosticEnvVar('RSTUDIO_CPP_BUILD_OUTPUT');

    // launch the process
    try {
      this.sessionProcess = this.launchSession(launchContext.argList, true);
    } catch (err: unknown) {
      return safeError(err);
    }

    logger().logDiagnostic(`\nR session launched, attempting to connect on port ${launchContext.port}...`);

    this.mainWindow = new MainWindow(launchContext.url);
    this.mainWindow.sessionLauncher = this;
    this.mainWindow.setSessionProcess(this.sessionProcess);
    this.mainWindow.appLauncher = this.appLaunch;
    this.appLaunch.setActivationWindow(this.mainWindow);

    ElectronDesktopOptions().restoreMainWindowBounds(this.mainWindow.window);

    logger().logDiagnostic('\nConnected to R session, attempting to initialize...\n');

    // On Windows, we have to close the log file when running diagnostics or diagnostics.exe
    // fails to inject the log contents into the diagnostics report due to access-denied due
    // to file being in use by another process
    if (process.platform === 'win32' && appState().runDiagnostics) {
      logger().closeLogFile();

      // winston logging package emits warnings if we don't have any registered transport
      logger().ensureTransport();
    }

    if (this.windowAllClosedHandler) {
      app.removeListener('window-all-closed', this.windowAllClosedHandler);
      this.windowAllClosedHandler = null;
    }

    // show the window (but don't if we are doing a --run-diagnostics)
    if (!appState().runDiagnostics) {
      this.mainWindow.window.once('ready-to-show', async () => {
        if (appState().startupDelayMs > 0) {
          await setTimeoutPromise(appState().startupDelayMs);
        }
        this.showSplash = false;
        this.mainWindow?.window.show();

        // if the splash screen displayed, keep it visible for another brief period to
        // reduce cases where it flashes and hides without being readable
        if (this.splash) {
          setTimeoutPromise(this.splashLinger)
            .then(() => {
              this.splash?.close();
            })
            .catch((err) => logger().logError(err));
        }
      });
      appState().activation().setMainWindow(this.mainWindow.window);
      this.appLaunch.activateWindow();
      this.mainWindow.loadUrl(launchContext.url).catch((reason) => {
        logger().logErrorMessage(`Failed to load ${launchContext.url}: ${reason}`);
      });
    }

    return success();
  }

  closeAllSatellites(): void {
    if (this.mainWindow) {
      closeAllSatellites(this.mainWindow.window);
    }
  }

  /**
   * @returns [logFileName, logContents]
   */
  async getRecentSessionLogs(): Promise<[string, string]> {
    // Collect R session logs
    const logs = new Array<FilePath>();

    const error = userLogPath().getChildren(logs);
    if (error) {
      throw error;
    }

    // Sort by recency in case there are several session logs --
    // inverse sort so most recent logs are first
    logs.sort((a, b) => {
      return b.getLastWriteTimeSync() - a.getLastWriteTimeSync();
    });

    let logFile = '';

    // Loop over all the log files and stop when we find a session log
    // (desktop logs are also in this folder)
    for (const log of logs) {
      if (log.getFilename().includes('rsession')) {
        // Record the path where we found the log file
        logFile = log.getAbsolutePath();

        // Read all the lines from a file into a string vector
        const [lines, error] = await readStringArrayFromFile(log);
        if (error) {
          throw error;
        }

        // Combine the three most recent lines
        let logContents = '';
        for (let i = Math.max(lines.length - 3, 0); i < lines.length; i++) {
          logContents += lines[i] + '\n';
        }
        return [logFile, logContents];
      }
    }

    // No logs found
    return ['Log File', '[No logs available]'];
  }

  async showLaunchErrorPage(): Promise<void> {
    // String mapping of template codes to diagnostic information
    const vars = new Map<string, string>();

    const info = buildInfo();
    const gitCommit = info.RSTUDIO_GIT_COMMIT.substr(0, 8);

    // Create version string
    // eslint-disable-next-line max-len
    const ss = `RStudio ${info.RSTUDIO_VERSION} "${info.RSTUDIO_RELEASE_NAME} " (${gitCommit}, ${info.RSTUDIO_BUILD_DATE}) for ${info.RSTUDIO_PACKAGE_OS}`;
    vars.set('version', ss);

    // Collect message from the abnormal end log path
    if (abendLogPath().existsSync()) {
      vars.set('launch_failed', this.launchFailedErrorMessage());
    } else {
      vars.set('launch_failed', '[No error available]');
    }

    // Collect the rsession process exit code
    let exitCode = EXIT_FAILURE;
    if (this.sessionProcess && this.sessionProcess.exitCode) {
      exitCode = this.sessionProcess.exitCode;
    }
    vars.set('session_path', this.sessionPath.getAbsolutePath());
    vars.set('exit_code', exitCode.toString());

    // Read standard output and standard error streams
    let procStdout = this.sessionStdout.join();
    if (!procStdout) {
      procStdout = '[No output emitted]';
    }
    vars.set('process_output', procStdout);

    let procStderr = this.sessionStderr.join();
    if (!procStderr) {
      procStderr = '[No errors emitted]';
    }
    vars.set('process_error', procStderr);

    // Read recent entries from the rsession log file
    let [logFile, logContent] = ['', ''];
    try {
      [logFile, logContent] = await this.getRecentSessionLogs();
    } catch (error: unknown) {
      logger().logError(error);
    }
    vars.set('log_file', logFile);
    vars.set('log_content', logContent);

    appState().gwtCallback?.setErrorPageInfo(vars);
    this.mainWindow
      ?.loadUrl(ERROR_WINDOW_WEBPACK_ENTRY)
      .then(() => {
        if (this.mainWindow) {
          this.mainWindow.setErrorDisplayed();
        }
      })
      .catch((reason) => {
        logger().logErrorMessage(`Failed to load ${ERROR_WINDOW_WEBPACK_ENTRY}: ${reason}`);
      });
  }

  onRSessionExited(): void {
    // if this is a verify-installation session then just quit
    if (appState().runDiagnostics) {
      this.mainWindow?.quit();
      return;
    }

    // if this was an automation run then just quit
    if (app.commandLine.hasSwitch('run-automation')) {
      this.mainWindow?.quit();

      const reportFile = app.commandLine.getSwitchValue('automation-report-file');
      if (existsSync(reportFile)) {
        console.log(`-- Automation results available at ${reportFile}.`);
      } else {
        console.log('-- An unexpected error occurred: no automation results are available.');
      }

      return;
    }

    const pendingQuit = this.mainWindow?.collectPendingQuitRequest();

    // if there was no pending quit set then this is a crash
    if (pendingQuit === PendingQuit.PendingQuitNone) {
      this.closeAllSatellites();

      this.mainWindow?.window.webContents.executeJavaScript('window.desktopHooks.notifyRCrashed()').catch(() => {
        // The above can throw if the window has no desktop hooks; this is normal
        // if we haven't loaded the initial session.
      });

      if (!this.mainWindow?.workbenchInitialized) {
        // If the R session exited without initializing the workbench, treat it as
        // a boot failure.
        void this.showLaunchErrorPage();
      }

      // quit and exit means close the main window
    } else if (pendingQuit === PendingQuit.PendingQuitAndExit) {
      this.mainWindow?.quit();
    }

    // otherwise this is a restart so we need to launch the next session
    else {
      // close all satellite windows if we are reloading
      const reload = pendingQuit === PendingQuit.PendingQuitRestartAndReload;
      if (reload) {
        this.closeAllSatellites();
      }

      // launch next session
      const error = this.launchNextSession(reload);
      if (error) {
        logger().logError(error);

        // TODO
        //  showMessageBox(QMessageBox::Critical,
        //                 pMainWindow_,
        //                 desktop::activation().editionName(),
        //                 launchFailedErrorMessage(), QString());

        this.mainWindow?.quit();
      }
    }
  }

  launchNextSession(reload: boolean): Err {
    // unset the initial project environment variable it this doesn't
    // pollute future sessions
    unsetenv(kRStudioInitialProject);

    // disconnect the firstWorkbenchInitialized event so it doesn't occur
    // again when we launch the next session
    // porting note: this is handled by using 'once' for the connection

    // delete the old process object
    this.mainWindow?.setSessionProcess(undefined);

    // TODO REVIEW: the C++ code is potentially relying on destructor here; do we need
    // to do more work directly here to disconnect events, etc?
    this.sessionProcess = undefined;

    // build a new launch context -- re-use the same port if we aren't reloading
    const launchContext = this.buildLaunchContext(!reload);

    // launch the process
    try {
      this.sessionProcess = this.launchSession(launchContext.argList, false);
    } catch (err: unknown) {
      return safeError(err);
    }

    // update the main window's reference to the process object
    this.mainWindow?.setSessionProcess(this.sessionProcess);
    if (reload) {
      waitForUrlWithTimeout(launchContext.url, 50, 25, 10)
        .then((error: Err) => {
          if (error) {
            logger().logError(error);
          }
        })
        .catch((error) => {
          logger().logError(error);
        })
        .finally(() => {
          this.nextSessionUrl = launchContext.url;
          setImmediate(this.onReloadFrameForNextSession.bind(this));
        });
    }

    return success();
  }

  onReloadFrameForNextSession(): void {
    if (this.nextSessionUrl) {
      this.mainWindow?.loadUrl(this.nextSessionUrl).catch((reason) => {
        logger().logErrorMessage(`Failed to load ${this.nextSessionUrl}: ${reason}`);
      });
      this.nextSessionUrl = undefined;
    }
  }

  private onLaunchFirstSession(): void {
    // must check showSplash before and after the timeout
    // before to determine if the timeout is required
    // after to determine if the main window is ready to show
    if (this.splashDelay > 0 && this.showSplash) {
      setTimeoutPromise(this.splashDelay)
        .then(() => {
          if (this.showSplash) {
            this.splash = createSplashScreen();
            this.splash.show();
          }
        })
        .catch((err) => logger().logError(err));
    }

    const error = this.launchFirst();
    if (error) {
      logger().logError(error);
      appState().activation().emitLaunchError(this.launchFailedErrorMessage());
    }
  }

  private launchSession(argList: string[], isFirstSession: boolean): ChildProcess {
    // always remove the abend log path before launching
    const error = abendLogPath().removeIfExistsSync();
    if (error) {
      logger().logError(error);
    }

    this.sessionStdout = [];
    this.sessionStderr = [];

    if (appState().sessionStartDelaySeconds > 0) {
      setenv('RSTUDIO_SESSION_SLEEP_ON_STARTUP', appState().sessionStartDelaySeconds.toString());
    }
    if (appState().sessionEarlyExitCode !== 0) {
      setenv('RSTUDIO_SESSION_EXIT_ON_STARTUP', appState().sessionEarlyExitCode.toString());
    }

    if (isFirstSession && process.platform === 'win32') {
      // on Windows, if we're using a UCRT build of R, we'll need to use
      // our rsession-utf8.exe executable (if available)
      const runtime = getenv('R_RUNTIME');
      if (runtime === 'ucrt') {
        const utf8SessionPath = this.sessionPath.getParent().completeChildPath('rsession-utf8.exe');
        if (utf8SessionPath.existsSync()) {
          this.sessionPath = utf8SessionPath;
          logger().logDebug(`R is UCRT; using ${this.sessionPath}`);
        }
      }

      // similarly, if we're using a 32-bit version of R,
      // we'll need to use the 32-bit copy of our session executable
      const arch = getenv('R_ARCH');
      if (arch === 'i386') {
        const x86SessionPath = this.sessionPath.getParent().completeChildPath('x86/rsession.exe');
        if (x86SessionPath.existsSync()) {
          this.sessionPath = x86SessionPath;
          logger().logDebug(`R is 32-bit; using ${this.sessionPath}`);
        } else {
          logger().logWarning('R is 32-bit, but no 32-bit rsession.exe was found');
        }
      }
    }

    // on macOS, we need to look at R and figure out if we should be trying to run
    // with the arm64 session binary (rsession-arm64) or with the x64 session binary (rsession)
    if (app.isPackaged && process.platform === 'darwin' && process.arch === 'arm64') {
      const rHome = getenv('R_HOME');
      const rLibPath = `${rHome}/lib/libR.dylib`;
      logger().logDebug(`$ /usr/bin/file "${rLibPath}"`);
      const fileInfo = execSync(`/usr/bin/file "${rLibPath}"`, { encoding: 'utf-8' });
      logger().logDebug(fileInfo);
      if (fileInfo.indexOf('arm64') !== -1) {
        this.sessionPath = this.sessionPath.getParent().completeChildPath('rsession-arm64');
        logger().logDebug(`R is arm64; using ${this.sessionPath}`);
      } else {
        logger().logDebug(`R is x86_64; using ${this.sessionPath}`);
      }
    }

    // if we're running automation tests, set that up now
    if (app.commandLine.hasSwitch('run-automation')) {
      argList.push('--run-automation');

      // forward 'automation-report-file' to session
      let reportFile = app.commandLine.getSwitchValue('automation-report-file');
      if (reportFile.length === 0) {
        reportFile = path.join(process.cwd(), 'rstudio-automation-results.xml');
        app.commandLine.appendSwitch('automation-report-file', reportFile);
      }
      argList.push(`--automation-report-file=${reportFile}`);

      // forward filters, markers if specified
      if (app.commandLine.hasSwitch('automation-filter')) {
        const filter = app.commandLine.getSwitchValue('automation-filter');
        setenv('RSTUDIO_AUTOMATION_FILTER', filter);
      }

      if (app.commandLine.hasSwitch('automation-markers')) {
        const markers = app.commandLine.getSwitchValue('automation-markers');
        setenv('RSTUDIO_AUTOMATION_MARKERS', markers);
      }

      // set up environment variables to help find automation tests
      if (!app.isPackaged) {
        const projectRoot = findRepoRoot();
        setenv('RSTUDIO_AUTOMATION_ROOT', projectRoot);
        setenv('RSTUDIO_AUTOMATION_ARGS', process.cwd());
      }
    }

    // if we're an automation agent, forward that to the R session
    if (app.commandLine.hasSwitch('automation-agent')) {
      argList.push('--automation-agent');
    }

    // In Windows development builds, move the session executable
    // to a separate location, so we can more easily build and restart
    // with an "active" rsession executable.
    if (!app.isPackaged && process.platform == 'win32') {
      // Get the session path.
      const sessionPath = this.sessionPath.getAbsolutePath();
      const sessionDir = path.dirname(sessionPath);

      // Create a new session name for the development session.
      let sessionName = path.basename(sessionPath);
      let devSessionName: string;
      if (isFirstSession) {
        devSessionName = `development.${generateShortenedUuid()}.${sessionName}`;
      } else {
        devSessionName = sessionName;
        sessionName = devSessionName.substring(devSessionName.indexOf('rsession'));
      }

      logger().logDebug(`Using development session: ${sessionName} => ${devSessionName}`);
      const devSessionPath = path.join(sessionDir, devSessionName);
      fs.copyFileSync(sessionPath, devSessionPath);
      app.on('quit', async (_event) => {
        for (let i = 0; i < 3; i++) {
          try {
            rmSync(devSessionPath);
            return;
          } catch (e: unknown) {
            await sleepPromise(1);
          }
        }
      });

      this.sessionPath = new FilePath(devSessionPath);
    }

    const sessionProc = launchProcess(this.sessionPath, argList);
    sessionProc.on('error', (err) => {
      // Unable to start rsession (at all)
      logger().logError(err);
      this.onRSessionExited();
    });

    sessionProc.on('exit', (code, signal) => {
      if (code !== null) {
        logger().logDebug(`rsession exited: code=${code}`);
        if (code !== 0) {
          logger().logDebug(`${this.sessionPath} ${argList}`);
        }
      } else {
        logger().logDebug(`rsession terminated: signal=${signal}`);
      }
      this.onRSessionExited();
    });

    // capture stdout and stderr for diagnostics
    sessionProc.stdout?.on('data', (data) => {
      this.sessionStdout.push(data);
    });

    sessionProc.stderr?.on('data', (data) => {
      this.sessionStderr.push(data);
    });

    return sessionProc;
  }

  onLaunchError(message: string): void {
    const exitFn = () => {
      if (this.mainWindow) {
        this.mainWindow.window.close();
      } else {
        app.exit(EXIT_FAILURE);
      }
    };
    if (message) {
      createStandaloneErrorDialog(appState().activation().editionName(), message)
        .then(() => exitFn())
        .catch((error) =>
          console.error('[session-launcher.ts] [onLaunchError] Error when creating Standalone Error Dialog: ', error),
        );
    } else {
      exitFn();
    }
  }

  collectAbendLogMessage(): string {
    let contents = '';
    const abendLog = abendLogPath();
    if (abendLog.existsSync()) {
      try {
        contents = fs.readFileSync(abendLog.getAbsolutePath(), 'utf8');
      } catch (error: unknown) {
        logger().logError(error);
      } finally {
        abendLog.removeIfExistsSync();
      }
    }
    return contents;
  }

  launchFailedErrorMessage(): string {
    let errMsg = i18next.t('sessionLauncherTs.rSessionFatalError');

    // check for abend log
    const abendLogMessage = this.collectAbendLogMessage();

    // check for R version mismatch
    if (abendLogMessage.includes('arguments passed to .Internal')) {
      // eslint-disable-next-line max-len
      errMsg =
        errMsg +
        '\n\n' +
        i18next.t('sessionLauncherTs.errorWasCausedByRAttemptingToLoadPackagesFromADifferentIncompatibleRVersion');
    }

    if (abendLogMessage) {
      errMsg += '\n\n' + abendLogMessage;
    }

    // check for stderr
    const errmsgs = this.sessionStderr.join();
    if (errmsgs) {
      errMsg += '\n\n' + errmsgs;
    }

    return errMsg;
  }

  buildLaunchContext(reusePort = true): LaunchContext {
    const argList: string[] = [];

    if (!reusePort) {
      appState().generateNewPort();
    }

    if (!this.confPath.isEmpty()) {
      argList.push('--config-file', this.confPath.getAbsolutePath());
    } else {
      // explicitly pass "none" so that rsession doesn't read an
      // /etc/rstudio/rsession.conf file which may be sitting around
      // from a previous configuration or install
      argList.push('--config-file', 'none');
    }

    const portStr = appState().port.toString();

    argList.push('--program-mode', 'desktop');
    argList.push('--www-port', portStr);
    argList.push('--launcher-token', SessionLauncher.launcherToken);

    // make sure the session knows where to find the desktop exectuable
    setenv('RSTUDIO_DESKTOP_EXE', app.getPath('exe'));

    // recalculate the local peer and set RS_LOCAL_PEER so that
    // rsession and it's children can use it
    if (process.platform === 'win32') {
      setenv('RS_LOCAL_PEER', localPeer(appState().port));
    }

    if (appState().runDiagnostics) {
      argList.push('--verify-installation', '1');
    }

    return {
      host: this.host,
      port: appState().port,
      url: `http://${this.host}:${portStr}`,
      argList,
    };
  }
}
