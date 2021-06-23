/*
 * log.ts
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

import { coreState } from './core-state';

export interface Logger {
  logError(err: Error): void;
  logErrorMessage(message: string): void;
  logInfo(message: string): void;
  logWarning(warning: string): void;
  logDebug(message: string): void;
  logDiagnostic(message: string): void;
  logDiagnosticEnvVar(name: string): void;
}

export function logger(): Logger {
  const logger = coreState().logger;
  if (!logger) {
    throw Error('Logger not set');
  }
  return logger;
}

export function setLogger(logger: Logger): void {
  coreState().logger = logger;
}
