/*
 * logger.ts
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

import { getenv } from './environment';
import { safeError } from './err';
import { Logger, LogLevel, logLevel, showDiagnosticsOutput } from './logger';

/**
 * A Logger using console.log()
 */
export class ConsoleLogger implements Logger {

  logError(err: unknown): void {
    if (logLevel() >= LogLevel.ERR) {
      const safeErr = safeError(err);
      console.log(safeErr);
    }
  }

  logErrorMessage(message: string): void {
    if (logLevel() >= LogLevel.ERR) {
      console.log(message);
    }
  }

  logWarning(warning: string): void {
    if (logLevel() >= LogLevel.WARN) {
      console.log(warning);
    }
  }

  logInfo(message: string): void {
    if (logLevel() >= LogLevel.INFO) {
      console.log(message);
    }
  }

  logDebug(message: string): void {
    if (logLevel() >= LogLevel.DEBUG) {
      console.log(message);
    }
  }

  logDiagnostic(message: string): void {
    if (showDiagnosticsOutput()) {
      console.log(message);
    }
  }

  logDiagnosticEnvVar(name: string): void {
    if (showDiagnosticsOutput()) {
      const value = getenv(name);
      if (value) {
        this.logDiagnostic(` . ${name} = ${value}`);
      }
    }
  }
}