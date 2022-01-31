/*
 * winston-logger.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import winston from 'winston';

import { getenv } from './environment';
import { safeError } from './err';
import { FilePath } from './file-path';
import { Logger, logLevel, showDiagnosticsOutput } from './logger';

/**
 * A Logger using winston package: https://www.npmjs.com/package/winston
 */
export class WinstonLogger implements Logger {
  logger: winston.Logger;

  constructor(logFile: FilePath, level: winston.level) {
    this.logger = winston.createLogger({ level: level });

    if (!logFile.isEmpty()) {
      this.logger.add(new winston.transports.File({ filename: logFile.getAbsolutePath() }));
    }

    // also log to console if stdout attached to a tty
    if (process.stdout.isTTY) {
      this.logger.add(new winston.transports.Console({ format: winston.format.simple() }));
    }
  }

  logLevel(): winston.level {
    return this.logger.level;
  }

  setLogLevel(level: winston.level): void {
    this.logger.level = level;
  }

  logError(err: unknown): void {
    this.logErrorAtLevel('error', err);
  }

  logErrorAtLevel(level: winston.level, err: unknown): void {
    const safeErr = safeError(err);
    this.logger.log(level, err);
  }

  logErrorMessage(message: string): void {
    this.logger.log('error', message);
  }

  logWarning(warning: string): void {
    this.logger.log('warn', warning);
  }

  logInfo(message: string): void {
    this.logger.log('info', message);
  }

  logDebug(message: string): void {
    this.logger.log('debug', message);
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
