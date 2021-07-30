/*
 * application.ts
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

import { app, dialog, globalShortcut } from 'electron';
import util from 'util';

import { getenv, setenv } from '../core/environment';
import { FilePath } from '../core/file-path';
import { generateRandomPort } from '../core/system';
import { logger, enableDiagnosticsOutput } from '../core/logger';

import { productInfo } from './product-info';
import { findComponents, initializeSharedSecret } from './utils';
import { augmentCommandLineArguments, getComponentVersions, removeStaleOptionsLockfile } from './utils';
import { exitFailure, exitSuccess, run, ProgramStatus } from './program-status';
import { ApplicationLaunch } from './application-launch';
import { AppState } from './app-state';
import { prepareEnvironment } from './detect-r';
import { SessionLauncher } from './session-launcher';
import { DesktopActivation } from './activation-overlay';
import { WindowTracker } from './window-tracker';
import { GwtCallback } from './gwt-callback';

// RStudio command-line switches
export const kRunDiagnosticsOption = '--run-diagnostics';
export const kSessionServerOption = '--session-server';
export const kSessionServerUrlOption = '--session-url';
export const kTempCookiesOption = '--use-temp-cookies';
export const kVersion = '--version';
export const kVersionJson = '--version-json';
export const kLogLevel = 'log-level';
export const kDelaySessionSeconds = 'session-delay-seconds';
export const kSessionExitCode = 'session-exit-code';

// This should be made a core utility function (also exists in int-utils.ts)
const setTimeoutPromise = util.promisify(setTimeout);

/**
 * The RStudio application
 */
export class Application implements AppState {
  runDiagnostics = false;
  scriptsPath?: FilePath;
  sessionPath?: FilePath;
  supportPath?: FilePath;
  port = generateRandomPort();
  windowTracker = new WindowTracker();
  gwtCallback?: GwtCallback;
  sessionStartDelaySeconds = 0;
  sessionEarlyExitCode = 0;

  appLaunch?: ApplicationLaunch;
  sessionLauncher?: SessionLauncher;
  private activationInst?: DesktopActivation;
  private scratchPath?: FilePath;

  /**
   * Startup code run before app 'ready' event.
   */
  async beforeAppReady(): Promise<ProgramStatus> {
    const status = this.initCommandLine(process.argv);
    if (status.exit) {
      return status;
    }

    // attempt to remove stale lockfiles, as they can impede application startup
    try {
      removeStaleOptionsLockfile();
    } catch (error) {
      logger().logError(error);
      return exitFailure();
    }

    // set Python encoding -- this is necessary for UTF-8 input
    // not representable in the current locale to be handled
    // correctly on Windows
    if (getenv('PYTHONIOENCODING') === '') {
      setenv('PYTHONIOENCODING', 'utf-8');
    }

    initializeSharedSecret();

    // allow users to supply extra command-line arguments for Chromium
    augmentCommandLineArguments();

    return run();
  }

  /**
   * Invoked when Electron app is 'ready'
   */
  async run(): Promise<ProgramStatus> {

    // test to see if we can detect a shortcut during initial startup
    const testShortcut = 'Control+Alt+R';
    let shortcutPressed = false;
    globalShortcut.register(testShortcut, () => {
      shortcutPressed = true;
      globalShortcut.unregister(testShortcut);
    });

    // prepare application for launch
    this.appLaunch = ApplicationLaunch.init();

    // determine paths to config file, rsession, and desktop scripts
    const [confPath, sessionPath, scriptsPath] = findComponents();
    this.sessionPath = sessionPath;
    this.scriptsPath = scriptsPath;

    if (!app.isPackaged) {
      // sanity checking for dev config
      if (!confPath.existsSync()) {
        dialog.showErrorBox('Dev Mode Config', `conf: ${confPath.getAbsolutePath()} not found.'`);
        return exitFailure();
      }
      if (!this.sessionPath.existsSync()) {
        dialog.showErrorBox('Dev Mode Config', `rsession: ${this.sessionPath.getAbsolutePath()} not found.'`);
        return exitFailure();
      }
    }

    // switch for setting a session start delay in seconds (used for testing, troubleshooting)
    if (app.commandLine.hasSwitch(kDelaySessionSeconds)) {
      const delay = parseInt(app.commandLine.getSwitchValue(kDelaySessionSeconds), 10);
      if (!isNaN(delay) && delay > 0) {
        this.sessionStartDelaySeconds = delay;
      }
    }

    // switch for forcing rsession to exit immediately with given exit code (testing, troubleshooting)
    // (will happen after session start delay above, if also specified)
    if (app.commandLine.hasSwitch(kSessionExitCode)) {
      const exitCode = parseInt(app.commandLine.getSwitchValue(kSessionExitCode), 0);
      if (!isNaN(exitCode) && exitCode !== 0) {
        this.sessionEarlyExitCode = exitCode;
      }
    }

    const error = prepareEnvironment();
    if (error) {
      return exitFailure();
    }

    // TODO: desktop pro session handling
    // TODO: 'file/project' file open handling (e.g. launch by double-clicking a .R or .Rproj file)

    // for this to "really" work we'd need to make more of the subsequent startup code truly
    // async to give more opportunity for shortcut to be detected before it is too
    // late (without simply pausing like we're doing here). Maybe via setImmediate that tries
    // several times (until the "too late" code, whatever that is, is reached).
    await setTimeoutPromise(1000).then(() => {
      if (shortcutPressed) {
        dialog.showMessageBoxSync({ message: `You were holding ${testShortcut}` });
      }
      globalShortcut.unregister(testShortcut);
    });

    // launch a local session
    this.sessionLauncher = new SessionLauncher(this.sessionPath, confPath, new FilePath(), this.appLaunch);
    this.sessionLauncher.launchFirstSession();
    return run();
  }

  initCommandLine(argv: string[]): ProgramStatus {
    // look for a version check request; if we have one, just do that and exit
    if (argv.indexOf(kVersion) > -1) {
      console.log(productInfo().RSTUDIO_VERSION);
      return exitSuccess();
    }

    // report extended version info and exit
    if (argv.indexOf(kVersionJson) > -1) {
      console.log(getComponentVersions());
      return exitSuccess();
    }

    if (argv.indexOf(kRunDiagnosticsOption) > -1) {
      this.runDiagnostics = true;
      enableDiagnosticsOutput();
    }

    return run();
  }

  supportingFilePath(): FilePath {
    if (!this.supportPath) {
      // default to install path
      this.supportPath = new FilePath(app.getAppPath());

      // adapt for OSX resource bundles
      if (process.platform === 'darwin') {
        if (this.supportPath.completePath('Info.plist').existsSync()) {
          this.supportPath = this.supportPath.completePath('Resources');
        }
      }
    }
    return this.supportPath;
  }

  resourcesPath(): FilePath {
    if (app.isPackaged) {
      return new FilePath(app.getAppPath());
    } else {
      return new FilePath(app.getAppPath()).completePath('../../..');
    }
  }

  activation(): DesktopActivation {
    if (!this.activationInst) {
      this.activationInst = new DesktopActivation();
    }
    return this.activationInst;
  }

  generateNewPort(): void {
    this.port = generateRandomPort();
  }

  setScratchTempDir(path: FilePath): void {
    this.scratchPath = path;
  }

  scratchTempDir(defaultPath: FilePath): FilePath {
    let dir = this.scratchPath;
    if (!dir?.isEmpty() && dir?.existsSync()) {
      dir = dir.completeChildPath('tmp');
      const error = dir.ensureDirectorySync();
      if (!error) {
        return dir;
      }
    }
    return defaultPath;
  }
}
