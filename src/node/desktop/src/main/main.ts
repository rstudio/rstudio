/*
 * main.ts
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

import { app, BrowserWindow } from 'electron';
import { getenv, setenv } from '../core/environment';
import { getRStudioVersion } from './product-info';
import { initializeSharedSecret } from './utils';
import { augmentCommandLineArguments, getComponentVersions, removeStaleOptionsLockfile } from './utils';
import { exitFailure, exitSuccess, run, ProgramStatus } from './program-status';

export class Main {
  mainWindow: BrowserWindow | null = null;

  /**
   * Startup code run before app 'ready' event.
   */
  beforeAppReady(): ProgramStatus {

    // look for a version check request; if we have one, just do that and exit
    if (app.commandLine.hasSwitch('version')) {
      console.log(getRStudioVersion());
      return exitSuccess();
    }

    // report extended version info and exit
    if (app.commandLine.hasSwitch('version-json')) {
      console.log(getComponentVersions());
      return exitSuccess();
    }

    // attempt to remove stale lockfiles, as they can impede application startup
    try {
      removeStaleOptionsLockfile();
    } catch (error) {
      console.error(error);
      return exitFailure();
    }

    // set Python encoding -- this is necessary for UTF-8 input
    // not representable in the current locale to be handled
    // correctly on Windows
    if (getenv('PYTHONIOENCODING') === '') {
      setenv('PYTHONIOENCODING', 'utf-8');
    }

    initializeSharedSecret();

    // allow users to supply extra command-line arguments
    augmentCommandLineArguments();

    return run();
  }

  /**
   * Invoked when app 'ready' is received
   */
  run(): void {
    // TEMPORARY, show a window so starting the app does something visible
    this.mainWindow = new BrowserWindow({
      width: 1024,
      height: 768,
      backgroundColor: '#fff', // https://github.com/electron/electron/blob/master/docs/faq.md#the-font-looks-blurry-what-is-this-and-what-can-i-do
      webPreferences: {
        enableRemoteModule: false,
        nodeIntegration: false,
        contextIsolation: true
      }

    });
    this.mainWindow.loadURL('https://rstudio.com');
  }
}
