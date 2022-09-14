/*
 * app-state.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { BrowserWindow, WebContents } from 'electron';
import { Client, Server } from 'net-ipc';
import { FilePath } from '../core/file-path';

import { DesktopActivation } from './activation-overlay';
import { Application } from './application';
import { GwtCallback } from './gwt-callback';
import { PendingWindow } from './pending-window';
import { WindowTracker } from './window-tracker';

/**
 * Global application state
 */
export interface AppState {
  runDiagnostics: boolean;
  sessionPath?: FilePath;
  scriptsPath?: FilePath;
  activation(): DesktopActivation;
  port: number;
  generateNewPort(): void;
  windowTracker: WindowTracker;
  gwtCallback?: GwtCallback;
  setScratchTempDir(path: FilePath): void;
  scratchTempDir(defaultPath: FilePath): FilePath;
  sessionStartDelaySeconds: number;
  sessionEarlyExitCode: number;
  prepareForWindow(pendingWindow: PendingWindow): void;
  windowOpening():
    | { action: 'deny' }
    | { action: 'allow'; overrideBrowserWindowOptions?: Electron.BrowserWindowConstructorOptions | undefined };
  windowCreated(newWindow: BrowserWindow, owner: WebContents, baseUrl?: string): void;
  server?: Server;
  client?: Client;
}

let rstudio: AppState | null = null;

/**
 * @returns Global application state
 */
export function appState(): AppState {
  if (!rstudio) {
    throw Error('application not set');
  }
  return rstudio;
}

/**
 * @returns Set application singleton
 */
export function setApplication(app: Application): void {
  if (rstudio) {
    throw Error('tried to create multiple Applications');
  }
  rstudio = app;
}

/**
 * Clear application singleton; intended for unit tests only
 */
export function clearApplicationSingleton(): void {
  rstudio = null;
}
