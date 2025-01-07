/*
 * app-state.ts
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

import { BrowserWindow, WebContents } from 'electron';
import { Client, Server } from 'net-ipc';
import { FilePath } from '../core/file-path';

import { DesktopActivation } from './activation-overlay';
import { Application } from './application';
import { GwtCallback } from './gwt-callback';
import { LoggerCallback } from './logger-callback';
import { PendingWindow } from './pending-window';
import { WindowTracker } from './window-tracker';
import { ModalDialogTracker } from './modal-dialog-tracker';
import { EventBusTypes } from './event-bus-types';
import { TypedEventEmitter } from './typed-event-emitter';
import { ArgsManager } from './args-manager';

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
  modalTracker: ModalDialogTracker;
  gwtCallback?: GwtCallback;
  loggerCallback?: LoggerCallback;
  setScratchTempDir(path: FilePath): void;
  scratchTempDir(defaultPath: FilePath): FilePath;
  sessionStartDelaySeconds: number;
  sessionEarlyExitCode: number;
  startupDelayMs: number;
  prepareForWindow(pendingWindow: PendingWindow): void;
  windowOpening():
    | { action: 'deny' }
    | { action: 'allow'; overrideBrowserWindowOptions?: Electron.BrowserWindowConstructorOptions | undefined };
  windowCreated(newWindow: BrowserWindow, owner: WebContents, baseUrl?: string): void;
  server?: Server;
  client?: Client;
  eventBus?: TypedEventEmitter<EventBusTypes>;
  argsManager: ArgsManager;
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

/**
 * Use this to broadcast events that any part of the code can subscribe to.
 * @returns Global event bus
 */
export function getEventBus(): TypedEventEmitter<EventBusTypes> {
  if (!appState().eventBus) {
    appState().eventBus = new TypedEventEmitter<EventBusTypes>();
  }
  return appState().eventBus!;
}
