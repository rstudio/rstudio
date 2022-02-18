/*
 * log.ts
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

import { coreState } from './core-state';
import { FilePath } from './file-path';

export interface Logger {
  logLevel(): winston.level;
  setLogLevel(level: winston.level): void;
  logError(err: unknown): void;
  logErrorAtLevel(level: winston.level, err: unknown): void;
  logErrorMessage(message: string): void;
  logWarning(warning: string): void;
  logInfo(message: string): void;
  logDebug(message: string): void;
  logDiagnostic(message: string): void;
  logDiagnosticEnvVar(name: string): void;
}

export interface LogOptions {
  logger: Logger;
  showDiagnostics: boolean; // special class of console logging enabled via '--run-diagnostics'
}

export class NullLogger implements Logger {
  level: winston.level = 'error';

  logLevel(): winston.level {
    return this.level;
  }
  setLogLevel(level: winston.level): void {
    this.level = level;
  }
  logError(err: unknown): void {
    this.logErrorAtLevel('error', err);
  }
  logErrorAtLevel(level: winston.level, err: unknown): void {}
  logErrorMessage(message: string): void {}
  logInfo(message: string): void {}
  logWarning(warning: string): void {}
  logDebug(message: string): void {}
  logDiagnostic(message: string): void {}
  logDiagnosticEnvVar(name: string): void {}
}

/**
 * @returns Global logger instance
 */
export function logger(): Logger {
  return coreState().logOptions.logger;
}

/**
 * @returns Current logging level
 */
export function logLevel(): winston.level {
  return coreState().logOptions.logger.logLevel();
}

export function setLogger(logger: Logger): void {
  coreState().logOptions.logger = logger;
}

export function enableDiagnosticsOutput(): void {
  coreState().logOptions.showDiagnostics = true;
}

export function showDiagnosticsOutput(): boolean {
  return coreState().logOptions.showDiagnostics;
}

/**
 * @param level minimum logging level
 */
export function setLoggerLevel(level: winston.level): void {
  coreState().logOptions.logger.setLogLevel(level);
}

/**
 * Convert a command-line log level string (e.g. 'WARN') to enum type string.
 *
 * @param level Command-line string representation of log level
 * @param defaultLevel Default logging level if unable to parse input
 * @returns string enum value for logging level
 */
export function parseCommandLineLogLevel(level: string, defaultLevel: winston.level): winston.level {
  level = level.toUpperCase();
  switch (level) {
    case 'ERR':
      return 'error';
    case 'WARN':
      return 'warn';
    case 'INFO':
      return 'info';
    case 'DEBUG':
      return 'debug';
    default:
      return defaultLevel;
  }
}
