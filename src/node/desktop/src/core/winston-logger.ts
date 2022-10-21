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
import DailyRotateFile from 'winston-daily-rotate-file';
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

  constructor(logOptions: LogOptions) {
    const level = this.readLogLevelOverride(logOptions.getLogLevel());
    const format = logOptions.getLogMessageFormat() === 'pretty' ? 
      printf((info) => `${info.timestamp} ${info.level.toUpperCase()} ${info.message}`)
      : combine(removeTimestamp(), json());

    const messageFormat = combine(timestamp({alias: 'time'}), format);
    const logFile = logOptions.getLogFile();

    this.logger = winston.createLogger({
      level: level,
      format: messageFormat,
      defaultMeta: { service: 'rdesktop'}
    });

    let logTransport;
    if (logOptions.getLoggerType() === 'stderr') {
      logTransport = new Console();
    } else if (logOptions.getLoggerType() === 'syslog') {
      if (process.platform === 'linux') {
        logTransport = new Syslog({
          protocol: 'unix',
          path: '/dev/log',
          app_name: 'rdesktop',
        });
        this.logger.levels = winston.config.syslog.levels;
      }
    }

    this.logger.add(logTransport ?? new DailyRotateFile({
      filename: `${logFile.getStem()}.%DATE%${logFile.getExtension()}`,
      dirname: logFile.getParent().getAbsolutePath(),
      datePattern: 'YYYY-MM-DD',
      frequency: '1d',
    }));
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
}

// removes `timestamp` key from json since we add `time` to match Qt log format
const removeTimestamp = format((info) => {
  delete info.timestamp;

  return info;
});
