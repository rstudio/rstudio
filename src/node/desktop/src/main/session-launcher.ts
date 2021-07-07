/*
 * session-launcher.ts
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
import { spawn, ChildProcess } from 'child_process';

import { logger } from '../core/logger';
import { FilePath } from '../core/file-path';
import { generateShortenedUuid, localPeer } from '../core/system';
import { Err, Success } from '../core/err';
import { getenv, setenv } from '../core/environment';

import { ApplicationLaunch } from './application-launch';
import { appState } from './app-state';
import { DesktopActivation } from './activation-overlay';
import { EXIT_FAILURE } from './program-status';
import { MainWindow } from './main-window';
import { PendingQuit } from './gwt-callback';
import { finalPlatformInitialize, userLogPath } from './utils';
import { productInfo } from './product-info';

export interface LaunchContext {
  host: string;
  port: number;
  url: string;
  argList: string[]
}

function abendLogPath(): FilePath {
  return userLogPath().completePath('rsession_abort_msg.log');
}

export class SessionLauncher {
  host = '127.0.0.1';
  sessionProcess?: ChildProcess;
  mainWindow?: MainWindow;
  static launcherToken = generateShortenedUuid();

  constructor(
    private sessionPath: FilePath,
    private confPath: FilePath,
    private filename: FilePath,
    private appLaunch: ApplicationLaunch
  ) { }

  launchFirstSession(): void {
    appState().activation().on(DesktopActivation.LAUNCH_FIRST_SESSION, this.onLaunchFirstSession.bind(this));
    appState().activation().on(DesktopActivation.LAUNCH_ERROR, this.onLaunchError.bind(this));

    // This will ultimately trigger one of the above events to continue with startup (or failure).
    appState().activation().getInitialLicense();
  }

  // Porting note: In the C++ code this was an overload of launchFirstSession(), but
  // but that isn't a thing in TypeScript (at least not without some ugly workarounds)
  // so giving a different name.
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
      this.sessionProcess = this.launchSession(launchContext.argList);
    } catch (err) {
      return err;
    }

    logger().logDiagnostic( `\nR session launched, attempting to connect on port ${launchContext.port}...`);

    this.mainWindow = new MainWindow(launchContext.url, false);
    this.mainWindow.sessionLauncher = this;
    this.mainWindow.sessionProcess = this.sessionProcess;
    this.mainWindow.appLauncher = this.appLaunch;
    this.appLaunch.setActivationWindow(this.mainWindow);

    // TODO - reimplement
    // desktop::options().restoreMainWindowBounds(this.mainWindow);
    this.mainWindow.window.setSize(1200, 900); // TEMP

    logger().logDiagnostic('\nConnected to R session, attempting to initialize...\n');

    // TODO - reimplement
    // one-time workbench initialized hook for startup file association
    // if (!filename_.isNull() && !filename_.isEmpty()) {
    //   StringSlotBinder* filenameBinder = new StringSlotBinder(filename_);
    //   pMainWindow_->connect(pMainWindow_,
    //                         SIGNAL(firstWorkbenchInitialized()),
    //                         filenameBinder,
    //                         SLOT(trigger()));
    //   pMainWindow_->connect(filenameBinder,
    //                         SIGNAL(triggered(QString)),
    //                         pMainWindow_,
    //                         SLOT(openFileInRStudio(QString)));
    // }

    // TODO - reimplement
    // pMainWindow_->connect(pAppLaunch_,
    //                       SIGNAL(openFileRequest(QString)),
    //                       pMainWindow_,
    //                       SLOT(openFileInRStudio(QString)));
    // pMainWindow_->connect(pRSessionProcess_,
    //                       SIGNAL(finished(int,QProcess::ExitStatus)),
    //                       this, SLOT(onRSessionExited(int,QProcess::ExitStatus)));
    // pMainWindow_->connect(&activation(),
    //                       SIGNAL(licenseLost(QString)),
    //                       pMainWindow_,
    //                       SLOT(onLicenseLost(QString)));
    // pMainWindow_->connect(&activation(), &DesktopActivation::updateLicenseWarningBar,
    //                       pMainWindow_, &MainWindow::onUpdateLicenseWarningBar);


    // show the window (but don't if we are doing a --run-diagnostics)
    if (!appState().runDiagnostics) {
      finalPlatformInitialize(this.mainWindow);
      this.mainWindow.window.once('ready-to-show', () => {
        this.mainWindow?.window.show();
      });
      appState().activation().setMainWindow(this.mainWindow.window);
      this.appLaunch.activateWindow();
      this.mainWindow.loadUrl(launchContext.url);
    }

    // TODO
    // qApp->setQuitOnLastWindowClosed(true);
    return Success();
  }

  private launchSession(argList: string[]): ChildProcess {
    if (process.platform === 'darwin') {
      // on macOS with the hardened runtime, we can no longer rely on dyld
      // to lazy-load symbols from libR.dylib; to resolve this, we use
      // DYLD_INSERT_LIBRARIES to inject the library we wish to use on
      // launch 
      const rHome = new FilePath(getenv('R_HOME'));
      const rLib = rHome.completePath('lib/libR.dylib');
      if (rLib.existsSync()) {
        setenv('DYLD_INSERT_LIBRARIES', rLib.getAbsolutePath());
      }
    }

    const sessionProc = spawn(this.sessionPath.getAbsolutePath(), argList);
    sessionProc.on('error', (err) => {
      // Unable to start rsession (at all); treat as though it started and exited
      logger().logError(err);
      this.onRSessionExited();
    });
    sessionProc.stdout.on('data', (data) => {
      logger().logDebug(`rsession stdout: ${data}`);
    });
    sessionProc.stderr.on('data', (data) => {
      logger().logDebug(`rsession stderr: ${data}`);
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

    return sessionProc;
  }

  private onLaunchFirstSession(): void {
    const error = this.launchFirst();
    if (error) {
      logger().logError(error);
      appState().activation().emitLaunchError(this.launchFailedErrorMessage());
    }
  }

  onLaunchError(message: string): void {
    if (message) {
      dialog.showErrorBox(appState().activation().editionName(), message);
    }
    if (this.mainWindow) {
      this.mainWindow.window.close();
    } else {
      app.exit(EXIT_FAILURE);
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  launchNextSession(reload: boolean): Err {

    // build a new launch context -- re-use the same port if we aren't reloading
    /* const launchContext = */ this.buildLaunchContext(!reload);

    // TODO: nyi
    return Error('launchNextSession NYI');
  }

  onRSessionExited(): void {
    // if this is a verify-installation session then just quit
    if (appState().runDiagnostics) {
      this.mainWindow?.quit();
      return;
    }

    const pendingQuit = this.mainWindow?.collectPendingQuitRequest();

    // if there was no pending quit set then this is a crash
    if (pendingQuit === PendingQuit.PendingQuitNone) {

      this.closeAllSatellites();

      this.mainWindow?.window.webContents.executeJavaScript('window.desktopHooks.notifyRCrashed()')
        .catch(() => {
          // The above can throw if the window has no desktop hooks; this is normal
          // if we haven't loaded the initial session.
        });

      if (!this.mainWindow?.workbenchInitialized) {
        // If the R session exited without initializing the workbench, treat it as
        // a boot failure.
        this.showLaunchErrorPage();
      }

      // quit and exit means close the main window
    } else if (pendingQuit === PendingQuit.PendingQuitAndExit) {
      this.mainWindow?.quit();
    }

    // otherwise this is a restart so we need to launch the next session
    else {
      const reload = (pendingQuit === PendingQuit.PendingQuitRestartAndReload);
      if (reload) {
        this.closeAllSatellites();
      }

      // launch next session
      this.launchNextSession(reload);
    }
  }

  buildLaunchContext(reusePort = true): LaunchContext {
    const argList: string[] = [];

    if (!reusePort) {
      appState().generateNewPort();
    }

    if (!this.confPath.isEmpty()) {
      argList.push('--config-file');
      argList.push(this.confPath.getAbsolutePath());
    } else {
      // explicitly pass "none" so that rsession doesn't read an
      // /etc/rstudio/rsession.conf file which may be sitting around
      // from a previous configuration or install
      argList.push('--config-file');
      argList.push('none');
    }

    // recalculate the local peer and set RS_LOCAL_PEER so that
    // rsession and it's children can use it
    if (process.platform === 'win32') {
      setenv('RS_LOCAL_PEER', localPeer(appState().port));
    }

    const portStr = appState().port.toString();
    return {
      host: this.host,
      port: appState().port,
      url: `http://${this.host}:${portStr}`,
      argList: [
        '--config-file', this.confPath.getAbsolutePath(),
        '--program-mode', 'desktop',
        '--www-port', portStr,
        '--launcher-token', SessionLauncher.launcherToken
      ],
    };
  }

  showLaunchErrorPage(): void {
    // RS_CALL_ONCE(); TODO, do we need to guard against multiple calls in Electron version?

    // String mapping of template codes to diagnostic information
    const vars = new Map<string, string>();

    const info = productInfo();
    const gitCommit = info.RSTUDIO_GIT_COMMIT.substr(0, 8);

    // Create version string
    const ss =
      `RStudio ${info.RSTUDIO_VERSION} "${info.RSTUDIO_RELEASE_NAME} " (${gitCommit}, ${info.RSTUDIO_BUILD_DATE}) for ${info.RSTUDIO_PACKAGE_OS}`;
    vars.set('version', ss);

    // Collect message from the abnormal end log path
    if (abendLogPath().exists()) {
      vars.set('launch_failed', this.launchFailedErrorMessage());
    } else {
      vars.set('launch_failed', '[No error available]');
    }

    // Collect the rsession process exit code
    vars.set('exit_code', '1'); // TODO safe_convert::numberToString(pRSessionProcess_->exitCode()));

    // Read standard output and standard error streams
    let procStdout = ''; // TODO pRSessionProcess_->readAllStandardOutput().toStdString();
    if (!procStdout) {
      procStdout = '[No output emitted]';
    }
    vars.set('process_output', procStdout);

    let procStderr = ''; // TODO pRSessionProcess_->readAllStandardError().toStdString();
    if (!procStderr) {
      procStderr = '[No errors emitted]';
    }
    vars.set('process_error', procStderr);

    // Read recent entries from the rsession log file
    const logFile = '[TODO]';
    const logContent = '[TODO]';
    // TODO const error = getRecentSessionLogs(&logFile, &logContent);
    // if (error) {
    //   logger().logError(error);
    // }
    vars.set('log_file', logFile);
    vars.set('log_content', logContent);

    // TODO Read text template, substitute variables, and load HTML into the main window
    // std::ostringstream oss;
    // error = text::renderTemplate(options().resourcesPath().completePath("html/error.html"), vars, oss);
    // if (error) {
    //   LOG_ERROR(error);
    // } else {
    this.mainWindow?.setErrorDisplayed();
    this.mainWindow?.loadUrl('data:text/html;charset=utf-8,<head> <meta http-equiv="Content-Type" content="text/html; charset=utf-8" /> <meta name="viewport" content="width=device-width, initial-scale=1.0" /> <title>Session Load Failed</title> </head><body>Failed to load session.</body>');
    // TODO pMainWindow_->loadHtml(QString::fromStdString(oss.str()));
    // }
  }

  closeAllSatellites(): void {
    console.log('CloseAllSatellites not implemented');
  }

  collectAbendLogMessage(): string {
    const contents = '';

    // TODO - reimplement
    // FilePath abendLog = abendLogPath();
    // if (abendLog.exists()) {
    //   Error error = core:: readStringFromFile(abendLog, & contents);
    //   if (error)
    //     LOG_ERROR(error);

    //   error = abendLog.removeIfExists();
    //   if (error)
    //     LOG_ERROR(error);
    // }
    return contents;
  }

  launchFailedErrorMessage(): string {
    const errMsg = 'The R session had a fatal error.';

    // check for abend log
    /* const abendLogMessage = */ this.collectAbendLogMessage();

    /// TODO - reimplement
    // // check for R version mismatch
    // if (abendLogMessage.contains(QString:: fromUtf8("arguments passed to .Internal"))) {
    //   errMsg.append(QString:: fromUtf8("\n\nThis error was very likely caused "
    //                 "by R attempting to load packages from a different "
    //                 "incompatible version of R on your system. Please remove "
    //                 "other versions of R and/or remove environment variables "
    //                 "that reference libraries from other versions of R "
    //                 "before proceeding."));
    // }

    // if (!abendLogMessage.isEmpty())
    //   errMsg.append(QString:: fromUtf8("\n\n").append(abendLogMessage));

    // // check for stderr
    // if (pRSessionProcess_) {
    //   QString errmsgs = QString:: fromLocal8Bit(
    //     pRSessionProcess_ -> readAllStandardError());
    //   if (errmsgs.size()) {
    //     errMsg = errMsg.append(QString:: fromUtf8("\n\n")).append(errmsgs);
    //   }
    // }

    return errMsg;
  }
}