/*
 * winston-logger.ts
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

import { app } from 'electron';
import winston, { format } from 'winston';
import { Syslog } from 'winston-syslog';
import { Console } from 'winston/lib/winston/transports';
import LogOptions from '../main/log-options';
import { getenv } from './environment';
import { safeError } from './err';
import { Logger, showDiagnosticsOutput } from './logger';

const { combine, printf, timestamp, json } = winston.format;

/**
 * A Logger using winston package: https://www.npmjs.com/package/winston
 */
export class WinstonLogger implements Logger {
  logger: winston.Logger;

  // track file transport so we can close it
  fileTransport: winston.transports.FileTransportInstance | null = null;

  constructor(logOptions: LogOptions) {
    const level = this.readLogLevelOverride(logOptions.getLogLevel());
    const format = logOptions.getLogMessageFormat() === 'pretty' ? 
      printf((info) => `${info.timestamp} ${info.level.toUpperCase()} ${info.message}`)
      : combine(removeTimestamp(), json());

    const messageFormat = combine(timestamp({alias: 'time'}), format);
    const logFile = logOptions.getLogFile();
    let optionError;

    this.logger = winston.createLogger({
      level: level,
      format: messageFormat,
      defaultMeta: { service: 'rdesktop'}
    });

    let consoleLogging = false;
    let logTransport;
    if (logOptions.getLoggerType() === 'stderr') {
      logTransport = new Console();
      consoleLogging = true;
    }

    if (logOptions.getLoggerType() === 'syslog') {
      if (process.platform === 'linux') {
        logTransport = new Syslog({
          protocol: 'unix',
          path: '/dev/log',
          app_name: 'rdesktop',
        });
        this.logger.levels = winston.config.syslog.levels;
      } else {
        optionError = 'syslog not supported';
      }
    }

    if (!logTransport) {
      logTransport = new winston.transports.File({
        filename: logFile.getAbsolutePath(),
        tailable: true,
        maxsize: 2000000, // TODO: use max-size from logging.conf (convert from mb to bytes)
        maxFiles: 100 }); // TODO: use max-rotation from logging.conf

      this.fileTransport = logTransport;
    }

    this.logger.add(logTransport);

    // on dev builds, always log to console
    if (!consoleLogging && !app.isPackaged) {
      this.logger.add(new Console());
      consoleLogging = true;
    }

    if (optionError) {
      this.logErrorMessage(optionError);
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

  closeLogFile(): void {
    if (this.fileTransport) {
      this.logger.remove(this.fileTransport);
      this.fileTransport = null;
    }
  }

  ensureTransport(): void {
    // Winston emits warnings if there is no transport
    if (this.logger.transports.length === 0) {
      this.logger.add(new Console());
    }
  }

}

// removes `timestamp` key from json since we add `time` to match Qt log format
const removeTimestamp = format((info) => {
  delete info.timestamp;

  return info;
});
