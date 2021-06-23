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
import { Logger } from './logger';

/**
 * A Logger using console.log()
 */
export class ConsoleLogger implements Logger {

  logError(err: Error): void {
    console.log(err);
  }

  logErrorMessage(message: string): void {
    console.log(message);
  }

  logInfo(message: string): void {
    console.log(message);
  }

  logWarning(warning: string): void {
    console.log(warning);
  }

  logDebug(message: string): void {
    console.log(message);
  }

  logDiagnostic(message: string): void {
    console.log(message);
  }

  logDiagnosticEnvVar(name: string): void {
    const value = getenv(name);
    if (value) {
      this.logDiagnostic(` . ${name} = ${value}`);
    }
  }
}