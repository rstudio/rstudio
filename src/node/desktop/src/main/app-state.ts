/*
 * app-state.ts
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

import { BrowserWindow } from 'electron';

import { FilePath } from '../core/file-path';

import { Application } from './application';

/**
 * Global application state
 */
export interface AppState {
  mainWindow?: BrowserWindow;
  runDiagnostics: boolean;
  scriptsPath?: FilePath;
  supportingFilePath(): FilePath;
}

let rstudio: Application | null = null;

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
