/*
 * application.ts
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

import { app, BrowserWindow, dialog, Menu, nativeTheme, screen, shell, WebContents } from 'electron';
import i18next from 'i18next';
import path from 'path';
import { getenv, setenv } from '../core/environment';
import { FilePath } from '../core/file-path';
import { logger } from '../core/logger';
import { kRStudioInitialProject, kRStudioInitialWorkingDir } from '../core/r-user-data';
import { generateRandomPort } from '../core/system';
import { getDesktopBridge } from '../renderer/desktop-bridge';
import { DesktopActivation } from './activation-overlay';
import { appState, AppState } from './app-state';
import { ApplicationLaunch } from './application-launch';
import { ArgsManager } from './args-manager';
import { prepareEnvironment, promptUserForR } from './detect-r';
import { GwtCallback } from './gwt-callback';
import { PendingWindow } from './pending-window';
import { exitFailure, exitSuccess, ProgramStatus, run } from './program-status';
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
import { Client, Server } from 'net-ipc';
import { LoggerCallback } from './logger-callback';
import { Xdg } from '../core/xdg';

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
  loggerCallback?: LoggerCallback;
  sessionStartDelaySeconds = 0;
  sessionEarlyExitCode = 0;
  startupDelayMs = 0;
  pendingWindows = new Array<PendingWindow>();
  server?: Server;
  client?: Client;

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

    logger().logDebug('Ready to run');
    return run();
  }

  private shouldInstanceOpen() {
    // check if this is the primary instance
    // projects always open in a new instance
    // files prefer to reuse the primary instance
    const hasInstanceLock = app.requestSingleInstanceLock();
    const hasProjectToOpen = this.argsManager.getProjectFileArg() ?? getenv('RS_INITIAL_PROJECT');
    const hasFileToOpen = this.argsManager.getFileArgs().length > 0;

    logger().logDebug(`instance lock: ${hasInstanceLock}, project: ${hasProjectToOpen}, file: ${hasFileToOpen}`);
    if (!hasInstanceLock && !(hasProjectToOpen.length > 0) && hasFileToOpen) {
      logger().logDebug('No instance lock - exiting');
      return false;
    }

    return true;
  }

  private initializeInstance() {
    const hasInstanceLock = app.hasSingleInstanceLock();

    if (hasInstanceLock) {
      this.createMessageServer();
    } else {
      this.createMessageClient();
    }

    app.on('second-instance', (_event, argv) => {
      logger().logDebug(`second-instance event: ARGS ${argv}`);

      // for files, open in the existing instance
      this.argsManager.setUnswitchedArgs(argv);
      if (this.argsManager.getProjectFileArg() === undefined)
        this.argsManager.handleAfterSessionLaunchCommands();
    });
  }

  // listens for messages from the server
  // currently, clients only listen for a close event to determine the next primary instance
  private createMessageClient() {
    const options = { path: 'rstudio', retries: 10 };
    this.client = new Client(options);

    // connect to primary RStudio instance
    this.client.connect({ path: 'rstudio' })
      .then(() => logger().logDebug(`net-ipc: ${process.pid} connected to primary instance`))
      .catch((error: unknown) => logger().logError(error));

    this.client.on('close', (reason: unknown) => {
      logger().logDebug(`net-ipc: ${process.pid} server close event ${reason}`);

      // close out connection to primary instance that just quit
      // another connection will be created to either be the primary or listen to the new primary instance
      this.client?.close()
        .then(() => logger().logDebug(`net-ipc: ${process.pid} close client connection`))
        .catch((error: unknown) => logger().logError(error))
        .finally(() => this.client = undefined);

      const instanceLock = app.requestSingleInstanceLock();
      if (instanceLock) {
        this.createMessageServer();
      } else {
        this.createMessageClient();
      }
    });
  }

  // create a new local socket to co-ordinate and become the primary instance
  private createMessageServer() {
    const path = Xdg.userDataDir().completeChildPath('rstudio.socket');
    path.getParent().ensureDirectorySync();
    const options = { path: path.getAbsolutePath() };
    this.server = new Server(options);
    logger().logDebug(`net-ipc: creating new message server; socket=${path.getAbsolutePath()}`);
    this.server.start()
      .then(() => {
        this.client = undefined;
        logger().logDebug(`net-ipc: ${process.pid} taking over as primary instance`);
      })
      .catch((error: unknown) => logger().logError(`net-ipc: ${process.pid} ${error}`));
  }

  private registerAppEvents() {
    app.on('before-quit', () => {
      app.releaseSingleInstanceLock();

      // closing will send an event to all other instances
      // one of the other instances will take over as primary
      this.server?.close()
        .then(() => logger().logDebug(`net-ipc: ${process.pid} is shutting down and releasing the instance lock`))
        .catch((error: unknown) => logger().logError(error));

      this.client?.close()
        .then(() => logger().logDebug(`net-ipc: ${process.pid} is disconnecting from the primary instance`))
        .catch((error: unknown) => logger().logError(error));
    });

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
      else {
        // for non-Rproj files, we want to set the initial working directory, before opening the file
        // if a session does not already exist (otherwise, just open the file)
        if (!app.isReady()) {
          setenv(kRStudioInitialWorkingDir, path.dirname(filepath));
        }
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

    // // Workaround for selecting all text in the input field: https://github.com/rstudio/rstudio/issues/11581
    // if (process.platform === 'darwin') {
    //   app.whenReady()
    //     .then(() => {
    //       globalShortcut.register('Cmd+A', () => {
    //         focusedWebContents()?.selectAll();
    //       });
    //     })
    //     .catch((error: unknown) => logger().logError(error));
    // }
  }

  /**
   * Invoked when Electron app is 'ready'
   */
  async run(): Promise<ProgramStatus> {
    if (!this.shouldInstanceOpen()) {
      return exitSuccess();
    }
    this.initializeInstance();

    // prepare application for launch
    this.appLaunch = ApplicationLaunch.init();

    // determine paths to config file, rsession, and desktop scripts
    const [confPath, sessionPath, scriptsPath] = findComponents();
    this.sessionPath = sessionPath;
    this.scriptsPath = scriptsPath;

    // force light theme so menu bar matches title bar
    // Electron 20+ will have support for matching
    if (process.platform === 'win32') {
      nativeTheme.themeSource = 'light';
    }

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

    // provide logging capabiity to renderer and preload
    this.loggerCallback = new LoggerCallback();

    // on Windows, ask the user what version of R they'd like to use
    let rPath = '';
    if (process.platform === 'win32') {
      const [path, preflightError] = await promptUserForR();
      if (preflightError) {

        await dialog.showMessageBox({
          type: 'warning',
          title: i18next.t('applicationTs.errorFindingR'),
          message: i18next.t('applicationTs.rstudioFailedToFindRInstalationsOnTheSystem'),
          buttons: [ i18next.t('common.buttonYes'), i18next.t('common.buttonNo') ],
        }).then(result => {

          logger().logDebug(`You clicked ${result.response == 0 ? 'Yes' : 'No'}`);
          if (result.response == 0) {
            const rProjectUrl = 'https://www.rstudio.org/links/r-project';
            void shell.openExternal(rProjectUrl);
          }
        })
          .catch((error: unknown) => logger().logError(error));

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
        {
          type: 'secondary',
          name: '',
          allowExternalNavigate: false,
          showToolbar: true,
          mainWindow: this.gwtCallback?.mainWindow,
        },
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
