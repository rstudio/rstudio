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

import { FilePath } from '../core/file-path';
import { generateShortenedUuid } from '../core/system';
import { Err } from '../core/err';
import { getenv, logEnvVar, setenv } from '../core/environment';

import { ApplicationLaunch } from './application-launch';
import { appState } from './app-state';
import { ActivationEvents } from './activation-overlay';
import { EXIT_FAILURE } from './program-status';
import { MainWindow } from './main-window';
import { PendingQuit } from './desktop-callback';

export class SessionLauncher {
  host = '127.0.0.1';
  private port?: string;
  private launcherToken?: string;
  sessionProcess?: ChildProcess;
  mainWindow?: MainWindow;

  constructor(
    private sessionPath: FilePath,
    private confPath: FilePath,
    private filename: FilePath,
    private appLaunch: ApplicationLaunch
  ) { }

  launchFirstSession(): void {
    appState().activation().on(ActivationEvents.LAUNCH_FIRST_SESSION, this.onLaunchFirstSession.bind(this));
    appState().activation().on(ActivationEvents.LAUNCH_ERROR, this.onLaunchError.bind(this));
    appState().activation().getInitialLicense();
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
    sessionProc.stdout.on('data', (data) => {
      console.log(`rsession stdout: ${data}`);
    });
    sessionProc.stderr.on('data', (data) => {
      console.log(`rsession stderr: ${data}`);
    });
    sessionProc.on('exit', (code, signal) => {
      if (code !== null) {
        console.log(`rsession exited: code=${code}`);
        if (code !== 0) {
          console.log(`${this.sessionPath} ${argList}`);
        }
      } else {
        console.log(`rsession terminated: signal=${signal}`);
      }
      this.onRSessionExited();
    });

    return sessionProc;
  }

  private onLaunchFirstSession(): void {
    const launchContext = this.buildLaunchContext();

    // show help home on first run
    launchContext.argList.push('--show-help-home', '1');

    if (appState().runDiagnostics) {
      console.log('\nAttempting to launch R session...');
      logEnvVar('RSTUDIO_WHICH_R');
      logEnvVar('R_HOME');
      logEnvVar('R_DOC_DIR');
      logEnvVar('R_INCLUDE_DIR');
      logEnvVar('R_SHARE_DIR');
      logEnvVar('R_LIBS');
      logEnvVar('R_LIBS_USER');
      logEnvVar('DYLD_LIBRARY_PATH');
      logEnvVar('DYLD_FALLBACK_LIBRARY_PATH');
      logEnvVar('LD_LIBRARY_PATH');
      logEnvVar('PATH');
      logEnvVar('HOME');
      logEnvVar('R_USER');
    }

    // launch the process
    this.sessionProcess = this.launchSession(launchContext.argList);

    this.mainWindow = new MainWindow(launchContext.url, false);
    this.mainWindow.sessionLauncher = this;
    this.mainWindow.sessionProcess = this.sessionProcess;

    // show the window
    this.mainWindow.load(launchContext.url);
   
  }

  onLaunchError(message: string): void {
    if (message) {
      dialog.showErrorBox(appState().activation().editionName(), message);
    }
    if (appState().mainWindow) {
      appState().mainWindow?.close();
    } else {
      app.exit(EXIT_FAILURE);
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  launchNextSession(reload: boolean): Err {
    return Error('launchNextSession NYI');
  }

  onRSessionExited(): void {
    const pendingQuit = this.mainWindow?.collectPendingQuitRequest();

    // if there was no pending quit set then this is a crash
    if (pendingQuit === PendingQuit.PendingQuitNone) {

      // closeAllSatellites();

      this.mainWindow?.window?.webContents.executeJavaScript('window.desktopHooks.notifyRCrashed()')
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

  buildLaunchContext(): { host: string, port: string, url: string, argList: string[] } {
    const port = this.newPortNumber();
    return {
      host: this.host,
      port: port,
      url: `http://${this.host}:${port}`,
      argList: [
        '--config-file', this.confPath.getAbsolutePath(),
        '--program-mode', 'desktop',
        '--www-port', port,
        '--launcher-token', this.getLauncherToken(),
      ],
    };
  }

  showLaunchErrorPage(): void {
    console.log('Launch error page not implemented');
  }

  closeAllSatellites(): void {
    console.log('CloseAllSatellites not implemented');
  }

  getLauncherToken(): string {
    if (!this.launcherToken) {
      this.launcherToken = generateShortenedUuid();
    }
    return this.launcherToken;
  }

  getPort(): string {
    if (!this.port) {
      // Use a random-ish port number to avoid collisions between different
      // instances of rdesktop-launched rsessions; not a cryptographically
      // secure technique so don't copy/paste for such purposes.
      const base = Math.floor(Math.random() * Number.MAX_SAFE_INTEGER);
      this.port = ((base % 40000) + 8080).toString();

      // recalculate the local peer and set RS_LOCAL_PEER so that
      // rsession and it's children can use it
      if (process.platform === 'win32') {
        const localPeer = '\\\\.\\pipe\\' + this.port + '-rsession';
        setenv('RS_LOCAL_PEER', localPeer);
      }
    }
    return this.port;
  }

  newPortNumber(): string {
    this.port = '';
    return this.getPort();
  }
}
