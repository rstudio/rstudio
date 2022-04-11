/*
 * application.ts
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

import { app, BrowserWindow, WebContents, screen } from 'electron';

import { getenv, setenv } from '../core/environment';
import { FilePath } from '../core/file-path';
import { generateRandomPort } from '../core/system';
import { logger } from '../core/logger';

import {
  getAppPath,
  createStandaloneErrorDialog,
  findComponents,
  initializeLang,
  initializeSharedSecret,
  raiseAndActivateWindow,
} from './utils';
import { augmentCommandLineArguments, removeStaleOptionsLockfile } from './utils';
import { exitFailure, run, ProgramStatus } from './program-status';
import { ApplicationLaunch } from './application-launch';
import { AppState } from './app-state';
import { SessionLauncher } from './session-launcher';
import { DesktopActivation } from './activation-overlay';
import { WindowTracker } from './window-tracker';
import { GwtCallback } from './gwt-callback';
import { prepareEnvironment, promptUserForR } from './detect-r';
import { PendingWindow } from './pending-window';
import { configureSatelliteWindow, configureSecondaryWindow } from './window-utils';
import i18next from 'i18next';
import { ArgsManager } from './args-manager';
import { SatelliteWindow } from './satellite-window';
import { SecondaryWindow } from './secondary-window';

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
  pendingWindows = new Array<PendingWindow>();

  appLaunch?: ApplicationLaunch;
  sessionLauncher?: SessionLauncher;
  private activationInst?: DesktopActivation;
  private scratchPath?: FilePath;

  argsManager = new ArgsManager();

  /**
   * Startup code run before app 'ready' event.
   */
  async beforeAppReady(): Promise<ProgramStatus> {
    const status = this.argsManager.initCommandLine(this);

    if (status.exit) {
      return status;
    }

    // attempt to remove stale lockfiles, as they can impede application startup
    try {
      removeStaleOptionsLockfile();
    } catch (error: unknown) {
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
    // prepare application for launch
    this.appLaunch = ApplicationLaunch.init();

    // determine paths to config file, rsession, and desktop scripts
    const [confPath, sessionPath, scriptsPath] = findComponents();
    this.sessionPath = sessionPath;
    this.scriptsPath = scriptsPath;

    if (!app.isPackaged) {
      // sanity checking for dev config
      if (!confPath.existsSync()) {
        await createStandaloneErrorDialog(
          i18next.t('applicationTs.devModeConfig'),
          `${i18next.t('applicationTs.confColon')} ${confPath.getAbsolutePath()} ${i18next.t(
            'applicationTs.notFoundDotLowercase',
          )}'`,
        );
        return exitFailure();
      }
      if (!this.sessionPath.existsSync()) {
        await createStandaloneErrorDialog(
          i18next.t('applicationTs.devModeConfig'),
          `rsession: ${this.sessionPath.getAbsolutePath()} ${i18next.t('applicationTs.notFoundDotLowercase')}'`,
        );
        return exitFailure();
      }
    }

    initializeLang();

    this.argsManager.handleAppReadyCommands(this);

    // on Windows, ask the user what version of R they'd like to use
    let rPath = '';
    if (process.platform === 'win32') {
      const [path, preflightError] = await promptUserForR();
      if (preflightError) {
        await createStandaloneErrorDialog(
          i18next.t('applicationTs.errorFindingR'),
          i18next.t('applicationTs.rstudioFailedToFindRInstalationsOnTheSystem'),
        );
        logger().logError(preflightError);
        return exitFailure();
      }

      // if no path was selected, bail (implies dialog was canceled)
      if (path == null) {
        return exitFailure();
      }
      rPath = path;
    }

    // prepare the R environment
    logger().logDebug(`Preparing environment using R: ${rPath}`);
    const prepareError = prepareEnvironment(rPath);
    if (prepareError) {
      await createStandaloneErrorDialog(
        i18next.t('applicationTs.errorFindingR'),
        i18next.t('applicationTs.rstudioFailedToFindRInstalationsOnTheSystem'),
      );
      logger().logError(prepareError);
      return exitFailure();
    }

    // TODO: desktop pro session handling
    // TODO: 'file/project' file open handling (e.g. launch by double-clicking a .R or .Rproj file)
    // TODO: select 32bit session for 32bit R on Windows

    // launch a local session
    this.sessionLauncher = new SessionLauncher(this.sessionPath, confPath, new FilePath(), this.appLaunch);
    this.sessionLauncher.launchFirstSession();

    return run();
  }

  supportingFilePath(): FilePath {
    if (!this.supportPath) {
      // default to install path
      this.supportPath = new FilePath(getAppPath());

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
      return new FilePath(getAppPath());
    } else {
      return new FilePath(getAppPath()).completePath('../../..');
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

  prepareForWindow(pendingWindow: PendingWindow): void {
    this.pendingWindows.push(pendingWindow);
  }

  windowOpening():
    | { action: 'deny' }
    | { action: 'allow'; overrideBrowserWindowOptions?: Electron.BrowserWindowConstructorOptions | undefined } {
    // no additional config if pending window is a satellite
    for (const pendingWindow of this.pendingWindows) {
      if (pendingWindow.type === 'satellite') {
        return SatelliteWindow.windowOpening();
      }
      break;
    }

    // determine size for secondary window
    const primaryDisplay = screen.getPrimaryDisplay();
    const width = Math.max(500, Math.min(850, primaryDisplay.workAreaSize.width));
    const height = Math.max(500, Math.min(1100, primaryDisplay.workAreaSize.height));
    return SecondaryWindow.windowOpening(width, height);
  }

  /**
   * Configures new Secondary or Satellite window
   */
  windowCreated(newWindow: BrowserWindow, owner: WebContents, baseUrl?: string): void {
    // check if we have a pending window waiting to come up
    const pendingWindow = this.pendingWindows.shift();
    if (pendingWindow) {
      // check for an existing window of this name
      const existingWindow = this.windowTracker.getWindow(pendingWindow.name)?.window;
      if (existingWindow) {
        // activate the existing window then deny creation of new window
        raiseAndActivateWindow(existingWindow);
        return;
      }

      if (pendingWindow.type === 'satellite') {
        configureSatelliteWindow(pendingWindow, newWindow, owner);
      } else {
        configureSecondaryWindow(pendingWindow, newWindow, owner, baseUrl);
      }
    } else {
      // No pending window, make it a generic secondary window
      configureSecondaryWindow(
        { type: 'secondary', name: '', allowExternalNavigate: false, showToolbar: true },
        newWindow,
        owner,
        baseUrl,
      );
    }
  }
}
