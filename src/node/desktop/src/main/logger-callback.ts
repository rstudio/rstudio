/*
 * logger-callback.ts
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


import { ipcMain } from 'electron';
import EventEmitter from 'events';
import { logger } from '../core/logger';

/**
 * This is the main-process side of the DesktopLogger Callbacks; dispatched from renderer processes
 * via the ContextBridge to enable logging from preload and renderer code.
 */
export class LoggerCallback extends EventEmitter {
  constructor() {
    super();
    ipcMain.on('desktop_log_message', (_event, level: string, message: string) => {
      level = level.toLowerCase();
      switch (level) {
        case 'err':
        case 'error':
          logger().logErrorMessage(message);
          break;
        case 'warn':
        case 'warning':
          logger().logWarning(message);
          break;
        case 'info':
          logger().logInfo(message);
          break;
        case 'debug':
          logger().logDebug(message);
          break;
      }
    });
  }
}
