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

import { app, BrowserWindow, dialog, Event, Menu, screen, session, WebContents } from 'electron';
import i18next from 'i18next';
import path from 'path';
import { getenv, setenv } from '../core/environment';
import { FilePath } from '../core/file-path';
import { logger } from '../core/logger';
import { kRStudioInitialProject, kRStudioInitialWorkingDir } from '../core/r-user-data';
import { findFreePort, generateRandomPort } from '../core/system';
import { DesktopActivation } from './activation-overlay';
import { appState, AppState, getEventBus } from './app-state';
import { ApplicationLaunch } from './application-launch';
import { ArgsManager } from './args-manager';
import {
  prepareEnvironment,
  promptUserForR,
  rChooserLikely,
  rDetectionReady,
  scanForR,
  showRNotFoundError,
} from './detect-r';
import { GwtCallback } from './gwt-callback';
import { prepareInstanceSessionData, removeExitedInstanceSessionData } from './instance-session-data';
import { PendingWindow } from './pending-window';
import { ElectronDesktopOptions } from './preferences/electron-desktop-options';
import { exitFailure, exitSuccess, ProgramStatus, run } from './program-status';
import { registerProcessDiagnostics } from './process-diagnostics';
import { SatelliteWindow } from './satellite-window';
import { SecondaryWindow } from './secondary-window';
import { SessionLauncher } from './session-launcher';
import { shareBootstrapCookies } from './shared-cookies';
import { startupCheckpoint } from './startup-timing';
import {
  augmentCommandLineArguments,
  createStandaloneErrorDialog,
  findComponents,
  initializeLang,
  initializeSharedSecret,
  isAutomated,
  keepAutomationWindowsRendering,
  showWindowForAutomation,
  loadRWebsite,
  raiseAndActivateWindow,
  removeStaleOptionsLockfile,
  resolveAliasedPath,
} from './utils';
import { WindowTracker } from './window-tracker';
import { configureSatelliteWindow, configureSecondaryWindow } from './window-utils';
import { Client, Server } from 'net-ipc';
import { LoggerCallback } from './logger-callback';
import { registerLanguageChannel, trackLocaleCookie } from './page-language';
import { checkDirectoryWritable, UserDirIssue, Xdg } from '../core/xdg';
import { ModalDialogTracker } from './modal-dialog-tracker';
import LogOptions from './log-options';

/**
 * Collects problems with the state folders RStudio needs to write at runtime:
 * the user config and data directories, plus the log directory actually in
 * use (which may have been relocated via logging.conf). Missing folders are
 * created as a side effect.
 */
export function collectStateDirIssues(): UserDirIssue[] {
  const issues = Xdg.verifyUserDirs();

  try {
    const logDir = new LogOptions().getLogFile().getParent();
    const message = checkDirectoryWritable(logDir);
    if (message) {
      issues.push({ directory: logDir.getAbsolutePath(), message: message });
    }
  } catch (error: unknown) {
    logger().logError(error);
  }

  return issues;
}

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
  modalTracker = new ModalDialogTracker();
  gwtCallback?: GwtCallback;
  loggerCallback?: LoggerCallback;
  sessionStartDelaySeconds = 0;
  sessionEarlyExitCode = 0;
  pendingWindows = new Array<PendingWindow>();
  server?: Server;
  client?: Client;
  stateDirIssues: UserDirIssue[] = [];

  appLaunch?: ApplicationLaunch;
  sessionLauncher?: SessionLauncher;
  private activationInst?: DesktopActivation;
  private scratchPath?: FilePath;
  private sharedSessionData = true;

  argsManager = new ArgsManager();

  /**
   * Startup code run before app 'ready' event.
   */
  async beforeAppReady(): Promise<ProgramStatus> {
    const status = this.argsManager.initCommandLine(this);

    if (status.exit) {
      return status;
    }

    // verify that the folders RStudio writes at runtime are accessible, and
    // surface any problems in an error dialog once the app is ready
    // https://github.com/rstudio/rstudio/issues/18179
    this.checkStateDirectories();
    this.reportDesktopOptionsRecovery();

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

    // On macOS, an instance that fails to take the lock before NSApplication starts launching
    // doesn't get 'ready' until it's activated (by a click on its Dock icon, say). Elsewhere,
    // 'will-finish-launching' comes with 'ready', too late to move the session data.
    if (process.platform === 'darwin') {
      app.once('will-finish-launching', () => this.initializeInstanceLock());
    } else {
      this.initializeInstanceLock();
    }

    this.registerAppEvents();

    // allow users to supply extra command-line arguments for Chromium
    augmentCommandLineArguments();

    logger().logDebug('Ready to run');
    return run();
  }

  /**
   * Checks that the state folders RStudio needs to write at runtime exist
   * (creating them when missing) and are writable, reporting any problems
   * with an error dialog. Startup continues regardless, so that the R session
   * error page (which also includes these details) can be shown.
   */
  private checkStateDirectories(): void {
    const issues = collectStateDirIssues();
    if (issues.length === 0) {
      return;
    }

    for (const issue of issues) {
      logger().logErrorMessage(`Unable to write to folder ${issue.directory}: ${issue.message}`);
    }

    this.stateDirIssues = issues;

    // the dialog can only be shown once the app is ready; don't block startup
    // on it, as Chromium command-line switches must be appended before then
    const details = issues.map((issue) => `${issue.directory}\n(${issue.message})`).join('\n\n');
    app
      .whenReady()
      .then(async () =>
        createStandaloneErrorDialog(
          i18next.t('applicationTs.stateFolderErrorTitle'),
          `${i18next.t('applicationTs.stateFolderErrorPrefix')}\n\n${details}\n\n${i18next.t(
            'applicationTs.stateFolderErrorSuffix',
          )}`,
        ),
      )
      .catch((error: unknown) => logger().logError(error));
  }

  /**
   * Desktop options are read before the file logger or this Application
   * exist, so a config.json moved aside at that point (#19496) could not be
   * logged or shown then. Do both now: the user's zoom, window bounds and
   * fonts have just been reset, and the backup is where they can get them back.
   */
  private reportDesktopOptionsRecovery(): void {
    const recovery = ElectronDesktopOptions().configRecovery();
    if (!recovery) {
      return;
    }

    logger().logWarning(
      `Could not read ${recovery.configPath} (${recovery.reason}); moved it to ${recovery.backupPath} and reset desktop options to their defaults.`,
    );

    // as above, the dialog has to wait for the app to be ready
    app
      .whenReady()
      .then(async () =>
        createStandaloneErrorDialog(
          i18next.t('applicationTs.desktopOptionsResetTitle'),
          // the native message box shows text, not HTML: keep the paths readable
          i18next.t('applicationTs.desktopOptionsResetMessage', {
            configPath: recovery.configPath,
            backupPath: recovery.backupPath,
            reason: recovery.reason,
            interpolation: { escapeValue: false },
          }),
        ),
      )
      .catch((error: unknown) => logger().logError(error));
  }

  /**
   * Whether this launch only hands its files to the primary instance and exits: projects always
   * open in a new instance, and files prefer to reuse the primary instance.
   */
  private forwardsToPrimary(hasInstanceLock: boolean): boolean {
    const hasProjectToOpen = this.argsManager.getProjectFileArg() ?? getenv('RS_INITIAL_PROJECT');
    const hasFileToOpen = this.argsManager.getFileArgs().length > 0;

    logger().logDebug(`instance lock: ${hasInstanceLock}, project: ${hasProjectToOpen}, file: ${hasFileToOpen}`);
    return !hasInstanceLock && !(hasProjectToOpen.length > 0) && hasFileToOpen;
  }

  private shouldInstanceOpen() {
    if (this.forwardsToPrimary(app.hasSingleInstanceLock())) {
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

    keepAutomationWindowsRendering();
  }

  /**
   * Takes the instance lock, which decides where Chromium keeps this instance's storage; that
   * can only be changed before the app is ready. The listener for files forwarded by later
   * launches goes in with the lock, as they can arrive from then on.
   */
  private initializeInstanceLock() {
    const userData = app.getPath('userData');
    const hasInstanceLock = app.requestSingleInstanceLock();

    // a launch that only hands its files to the primary instance never opens a window, so it
    // has no use for storage of its own
    if (!hasInstanceLock && !this.forwardsToPrimary(hasInstanceLock)) {
      const sessionData = prepareInstanceSessionData(userData);
      if (sessionData) {
        logger().logDebug(`Another instance is running; using session data folder ${sessionData}`);
        app.setPath('sessionData', sessionData);
        this.sharedSessionData = false;
      }
    }

    getEventBus().once('main-window-loaded', () => {
      removeExitedInstanceSessionData(userData).catch((error: unknown) => logger().logError(error));
    });

    app.on('second-instance', (_event, argv) => {
      logger().logDebug(`second-instance event: ARGS ${argv}`);

      // for files, open in the existing instance
      //
      // The slice(1) removes the RStudio executable argument; the attempt to detect
      // and remove it inside setUnswitchedArgs() won't work if second instance of RStudio is in a
      // different location than the first. https://github.com/rstudio/rstudio/issues/15554
      this.argsManager.setUnswitchedArgs(argv.slice(1));
      if (this.argsManager.getProjectFileArg() === undefined) this.argsManager.handleAfterSessionLaunchCommands();
    });
  }

  // listens for messages from the server
  // currently, clients only listen for a close event to determine the next primary instance
  private createMessageClient() {
    const options = { path: 'rstudio', retries: 10 };
    this.client = new Client(options);

    // connect to primary RStudio instance
    this.client
      .connect({ path: 'rstudio' })
      .then(() => logger().logDebug(`net-ipc: ${process.pid} connected to primary instance`))
      .catch((error: unknown) => logger().logError(error));

    this.client.on('close', (reason: unknown) => {
      logger().logDebug(`net-ipc: ${process.pid} server close event ${reason}`);

      // close out connection to primary instance that just quit
      // another connection will be created to either be the primary or listen to the new primary instance
      this.client
        ?.close()
        .then(() => logger().logDebug(`net-ipc: ${process.pid} close client connection`))
        .catch((error: unknown) => logger().logError(error))
        .finally(() => (this.client = undefined));

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
    this.server
      .start()
      .then(() => {
        this.client = undefined;
        logger().logDebug(`net-ipc: ${process.pid} taking over as primary instance`);
      })
      .catch((error: unknown) => logger().logError(`net-ipc: ${process.pid} ${error}`));
  }

  private registerAppEvents() {
    registerProcessDiagnostics();

    app.on('before-quit', () => {
      app.releaseSingleInstanceLock();

      // closing will send an event to all other instances
      // one of the other instances will take over as primary
      this.server
        ?.close()
        .then(() => logger().logDebug(`net-ipc: ${process.pid} is shutting down and releasing the instance lock`))
        .catch((error: unknown) => logger().logError(error));

      this.client
        ?.close()
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
          this.appLaunch?.launchRStudio({ projectFilePath: filepath });
        } else {
          setenv(kRStudioInitialProject, filepath);
        }
        return;
      } else {
        // for non-Rproj files, we want to set the initial working directory, before opening the file
        // if a session does not already exist (otherwise, just open the file)
        if (!app.isReady()) {
          setenv(kRStudioInitialWorkingDir, path.dirname(filepath));
        }
      }

      app
        .whenReady()
        .then(() => {
          if (this.gwtCallback == null || !this.gwtCallback.initialized) {
            // wait for main window to load
            getEventBus().once('main-window-loaded', () => {
              void this.gwtCallback?.openFileInMainWindow(resolvedPath);
            });
          } else {
            void this.gwtCallback.openFileInMainWindow(resolvedPath);
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

    // the desktop's own pages (choose R, loading) need the UI language; read it while the
    // rest of startup runs, and wait for it after R detection. It's one of the cookies an
    // instance with its own session data starts from, so those come first
    registerLanguageChannel();
    const sharedCookiesDir = path.join(app.getPath('userData'), 'shared-cookies');
    const languageReady = shareBootstrapCookies(session.defaultSession, sharedCookiesDir, this.sharedSessionData).then(
      async () => trackLocaleCookie(session.defaultSession),
    );

    // prepare application for launch
    this.appLaunch = ApplicationLaunch.init();

    // determine paths to config file, rsession, and desktop scripts
    const [installPath, confPath, sessionPath, scriptsPath] = findComponents();
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

    // provide logging capabiity to renderer and preload
    this.loggerCallback = new LoggerCallback();

    // Prevent app from terminating when last window closes during startup; this can cause
    // app to exit after Choose R dialog closes but before the license dialog shows. Also
    // happens after the Manage License dialog (w/o Choose R being involved).
    // https://github.com/rstudio/rstudio-pro/issues/6062

    const windowAllClosedHandler = () => {
      // intentionally empty
    };
    app.on('window-all-closed', windowAllClosedHandler);

    // the query started when the app launched has normally finished by now,
    // so the detection below finds its answer in the cache. When the Windows
    // R chooser is going to show anyway (explicitly requested, or a first run
    // with nothing to reuse), don't wait on the probe: the chooser must not
    // be delayed by a slow or hanging R
    if (process.platform !== 'win32' || !rChooserLikely()) {
      await rDetectionReady();
    }
    await languageReady;
    startupCheckpoint('r-query-ready');

    // on Windows, ask the user what version of R they'd like to use
    let rPath;
    if (process.platform === 'win32') {
      const [path, preflightError] = await promptUserForR();
      if (preflightError) {
        logger().logError(preflightError);
        await appState()
          .modalTracker.trackElectronModalAsync(async () =>
            dialog.showMessageBox({
              type: 'warning',
              title: i18next.t('applicationTs.errorFindingR'),
              message: i18next.t('applicationTs.rstudioFailedToFindRInstalationsOnTheSystem'),
              buttons: [i18next.t('common.buttonYes'), i18next.t('common.buttonNo')],
            }),
          )
          .then((result) => {
            logger().logDebug(`You clicked ${result.response == 0 ? 'Yes' : 'No'}`);
            if (result.response == 0) {
              loadRWebsite();
            }
          })
          .catch((error: unknown) => logger().logError(error));

        return exitFailure();
      }

      // if no path was selected, bail (implies dialog was canceled)
      if (path == null) {
        logger().logDebug('Did not get a path, app will exit');
        return exitFailure();
      }

      // a path was selected
      logger().logDebug(`Path selected was: "${path}"`);
      rPath = path;
    }

    // if we don't have an R path at this point, try scanning for R
    startupCheckpoint('r-detect-begin');
    if (!rPath) {
      logger().logDebug('No rPath found, scanning for R');
      const [scannedPath, error] = scanForR();
      if (error) {
        logger().logDebug(`Error scanning for R: ${error}`);

        // an R that was found but would not start is a different problem
        // from no R at all; the error says which one it was
        await showRNotFoundError(error);
        return exitFailure();
      }

      rPath = scannedPath;
    }

    logger().logDebug('Done choosing R');
    startupCheckpoint('r-detected');

    // prepare the R environment
    logger().logDebug(`Preparing environment using R: ${rPath}`);
    const prepareError = prepareEnvironment(rPath);
    if (prepareError) {
      // the failure was logged where it happened, with R's full output
      logger().logDebug(`Error preparing R environment: ${prepareError}`);
      await showRNotFoundError(prepareError);
      return exitFailure();
    }
    startupCheckpoint('r-environment-prepared');

    // launch a local session
    this.sessionLauncher = new SessionLauncher(
      this.sessionPath,
      confPath,
      new FilePath(),
      this.appLaunch,
      windowAllClosedHandler,
    );
    this.sessionLauncher.launchFirstSession(installPath, !app.isPackaged);

    this.setDockMenu();

    return run();
  }

  activation(): DesktopActivation {
    if (!this.activationInst) {
      this.activationInst = new DesktopActivation();
    }
    return this.activationInst;
  }

  async generateNewPort(): Promise<void> {
    this.port = await findFreePort();
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

  /**
   * Returns the pending entry queued for a window.open() of this name, if any,
   * leaving it at the head of the queue.
   *
   * Entries queued ahead of it are stale: when a window of the requested name
   * is already open in the opener's browsing context group, Chromium navigates
   * that window instead of creating one, so neither windowOpening() nor
   * windowCreated() ever sees the request. Drop them here so they can't attach
   * to a later, unrelated request.
   */
  private pendingWindowFor(frameName: string): PendingWindow | undefined {
    const index = this.pendingWindows.findIndex((pending) => pending.name === frameName);
    if (index === -1) {
      return undefined;
    }

    this.pendingWindows.splice(0, index);
    return this.pendingWindows[0];
  }

  windowOpening(
    frameName: string,
  ):
    | { action: 'deny' }
    | { action: 'allow'; overrideBrowserWindowOptions?: Electron.BrowserWindowConstructorOptions | undefined } {
    const pendingWindow = this.pendingWindowFor(frameName);
    if (pendingWindow) {
      // a window of this name is already open: activate it instead. This has
      // to be denied here, before Electron creates the window; a denied
      // request never reaches windowCreated(), so consume the pending entry.
      const existingWindow = this.windowTracker.getWindow(pendingWindow.name)?.window;
      if (existingWindow) {
        this.pendingWindows.shift();
        raiseAndActivateWindow(existingWindow);
        return { action: 'deny' };
      }

      // no additional config if pending window is a satellite
      if (pendingWindow.type === 'satellite') {
        return SatelliteWindow.windowOpening();
      }
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
  windowCreated(newWindow: BrowserWindow, owner: WebContents, frameName: string, baseUrl?: string): void {
    // check if we have a pending window waiting to come up
    const pendingWindow = this.pendingWindowFor(frameName);
    if (pendingWindow) {
      this.pendingWindows.shift();
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

    // windowOpening() created the window hidden; surface it without pulling
    // OS focus away from whatever the user is doing while the tests run.
    if (isAutomated()) {
      showWindowForAutomation(newWindow);
    }
  }

  setDockMenu() {
    if (process.platform === 'darwin') {
      // Type assertion to reassure TypeScript that dock exists on macOS
      const dock = app.dock as Electron.Dock;
      const menuDock = Menu.buildFromTemplate([
        {
          label: i18next.t('applicationTs.newRstudioWindow'),
          click: () => {
            // this command starts a new, separate session: no project, and the user's
            // default working directory rather than anything from this window (#15669)
            this.appLaunch?.launchRStudio({ noProject: true });
          },
        },
      ]);

      dock.setMenu(menuDock);
    }
  }
}
