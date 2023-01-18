/*
 * logger-bridge.ts
 *
 * Copyright (C) 2023 by Posit Software, PBC
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

import { ipcRenderer } from 'electron';

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function getDesktopLoggerBridge() {
  return {
    logString: (level: 'err'|'warn'|'info'|'debug', message: string) => {
      logString(level, message);
    },

    logError: (error: unknown) => {
      logError(error);
    },
  };
}

/**
 * Log a string in the main process (renderer process cannot directly use logger)
 *
 * @param level logging level (err|warn|info|debug)
 * @param message  string to log
 */
export function logString(level: 'err'|'warn'|'info'|'debug', message: string): void {
  ipcRenderer.send('desktop_log_message', level, message);
}

/**
 * Log an Error in the main process (renderer process cannot directly use logger)
 *
 * @param error  Error to log
 */
export function logError(error: unknown): void {

  if (error instanceof Error) {
    logString('err', error.message);
  } else {
    logString('err', 'unknown error type in logger bridge');
  }
}
