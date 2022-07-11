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

import { app, BrowserWindow, Menu, screen, WebContents } from 'electron';
import i18next from 'i18next';
import path from 'path';
import { getenv, setenv } from '../core/environment';
import { FilePath } from '../core/file-path';
import { logger } from '../core/logger';
import { kRStudioInitialProject } from '../core/r-user-data';
import { generateRandomPort } from '../core/system';
import { getDesktopBridge } from '../renderer/desktop-bridge';
import { DesktopActivation } from './activation-overlay';
import { AppState } from './app-state';
import { ApplicationLaunch } from './application-launch';
import { ArgsManager } from './args-manager';
import { prepareEnvironment, promptUserForR } from './detect-r';
import { GwtCallback } from './gwt-callback';
import { PendingWindow } from './pending-window';
import { exitFailure, ProgramStatus, run } from './program-status';
import { SatelliteWindow } from './satellite-window';
import { SecondaryWindow } from './secondary-window';
import { SessionLauncher } from './session-launcher';
import {
  augmentCommandLineArguments,
  createStandaloneErrorDialog,
  findComponents,
  initializeLang,
  initializeSharedSecret,
  raiseAndActivateWindow,
  removeStaleOptionsLockfile,
  resolveAliasedPath
} from './utils';
import { WindowTracker } from './window-tracker';
import { configureSatelliteWindow, configureSecondaryWindow } from './window-utils';

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
    this.registerAppEvents();

    // allow users to supply extra command-line arguments for Chromium
    augmentCommandLineArguments();

    return run();
  }

  private registerAppEvents() {
    app.on('open-file', (event: Event, filepath: string) => {
      const resolvedPath = resolveAliasedPath(filepath);
      event.preventDefault();

      const ext = path.extname(filepath).toLowerCase();

      // first startup - open the project by setting the initial project env var
      // otherwise it would open in the last state then open the project
      if (ext === '.rproj') {
        if (app.isReady()) {
          this.appLaunch?.launchRStudio({projectFilePath: filepath});
        } else {
          setenv(kRStudioInitialProject, filepath);
        }
        return;
      }

      app.whenReady()
        .then(() => {
          // app may be ready but GWT may not be ready
          if (this.gwtCallback?.initialized) {
            getDesktopBridge().openFile(resolvedPath);
          } else {
            this.gwtCallback?.once(GwtCallback.WORKBENCH_INITIALIZED, () => {
              getDesktopBridge().openFile(resolvedPath);
            });
          }
        })
        .catch((error: unknown) => logger().logError(error));
    });
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

    // launch a local session
    this.sessionLauncher = new SessionLauncher(this.sessionPath, confPath, new FilePath(), this.appLaunch);
    this.sessionLauncher.launchFirstSession();

    this.gwtCallback?.once(GwtCallback.WORKBENCH_INITIALIZED, () => {
      this.argsManager.handleAfterSessionLaunchCommands();
    });

    this.setDockMenu();

    return run();
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

  setDockMenu(){
    if (process.platform === 'darwin'){
      const  menuDock = Menu.buildFromTemplate([{
        label: i18next.t('applicationTs.newRstudioWindow'),
        click: () => {
          this.appLaunch?.launchRStudio({});
        }
      }]);

      app.dock.setMenu(menuDock);
    }
  }
}
