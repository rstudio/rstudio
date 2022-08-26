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

import { app } from 'electron';
import winston from 'winston';
import DailyRotateFile from 'winston-daily-rotate-file';

import { getenv } from './environment';
import { safeError } from './err';
import { FilePath } from './file-path';
import { Logger, showDiagnosticsOutput } from './logger';

const { combine, printf, timestamp } = winston.format;

/**
 * A Logger using winston package: https://www.npmjs.com/package/winston
 */
export class WinstonLogger implements Logger {
  logger: winston.Logger;

  constructor(logFile: FilePath, level: string) {
    level = this.readLogLevelOverride(level);
    this.logger = winston.createLogger({
      level: level,
      format: combine(
        timestamp(),
        printf((info) => `${info.timestamp} ${info.level}: ${info.message}`)
      ),
    });

    if (!logFile.isEmpty()) {
      const logName = `${logFile.getStem()}.%DATE%${logFile.getExtension()}`;
      this.logger.add(
        new DailyRotateFile({
          filename: logName,
          dirname: logFile.getParent().getAbsolutePath(),
          datePattern: 'YYYY-MM-DD',
          frequency: '1d',
        })
      );
    }
  }

  readLogLevelOverride(defaultLevel: string): string {
    const envvars = ['RSTUDIO_DESKTOP_LOG_LEVEL', 'RS_LOG_LEVEL'];
    for (const envvar of envvars) {
      const envval = getenv(envvar);
      if (envval.length !== 0) {
        return envval as string;
      }
    }

    return defaultLevel;
  }

  logLevel(): string {
    return this.logger.level;
  }

  setLogLevel(level: string): void {
    this.logger.level = level;
  }

  log(level: string, message: string): void {
    // log to default log locations
    this.logger.log(level, message);

    // log to console in debug configurations
    // NOTE: process.stderr.isTTY seems to be unreliable?
    if (!app.isPackaged && this.logger.isLevelEnabled(level)) {
      const ts = new Date().toISOString();
      const tlevel = level.toUpperCase();
      const tmessage = message.replace(/\n/g, '|||') + '\n';
      process.stderr.write(`${ts} ${tlevel} ${tmessage}`);
    }
  }

  logError(err: unknown): void {
    this.logErrorAtLevel('error', err);
  }

  logErrorAtLevel(level: string, err: unknown): void {
    this.log(level, safeError(err).message);
  }

  logErrorMessage(message: string): void {
    this.log('error', message);
  }

  logWarning(warning: string): void {
    this.log('warn', warning);
  }

  logInfo(message: string): void {
    this.log('info', message);
  }

  logDebug(message: string): void {
    this.log('debug', message);
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
