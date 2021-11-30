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

/**
 * Enum representing logging detail level
 */
export enum LogLevel {
  OFF = 0, // No messages will be logged
  ERR = 1, // Error messages will be logged
  WARN = 2, // Warning and error messages will be logged
  INFO = 3, // Info, warning, and error messages will be logged
  DEBUG = 4, // All messages will be logged
}

export interface Logger {
  logError(err: unknown): void;
  logErrorMessage(message: string): void;
  logWarning(warning: string): void;
  logInfo(message: string): void;
  logDebug(message: string): void;
  logDiagnostic(message: string): void;
  logDiagnosticEnvVar(name: string): void;
}

export interface LogOptions {
  logger?: Logger;
  logLevel: LogLevel;
  showDiagnostics: boolean;
}

export class NullLogger implements Logger {
  logError(err: unknown): void {}
  logErrorMessage(message: string): void {}
  logInfo(message: string): void {}
  logWarning(warning: string): void {}
  logDebug(message: string): void {}
  logDiagnostic(message: string): void {}
  logDiagnosticEnvVar(name: string): void {}
}

export function logger(): Logger {
  const logger = coreState().logOptions.logger;
  if (!logger) {
    throw Error('Logger not set');
  }
  return logger;
}

/**
 * @returns Current logging level
 */
export function logLevel(): LogLevel {
  return coreState().logOptions.logLevel;
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
export function setLoggerLevel(level: LogLevel): void {
  coreState().logOptions.logLevel = level;
}

/**
 * Convert a string log level (e.g. 'WARN') to LogLevel enum.
 *
 * @param level String representation of log level
 * @param defaultLevel Default logging level if unable to parse input
 * @returns LogLevel enum value
 */
export function parseCommandLineLogLevel(level: string, defaultLevel: LogLevel): LogLevel {
  level = level.toUpperCase();
  switch (level) {
    case 'OFF':
      return LogLevel.OFF;
    case 'ERR':
      return LogLevel.ERR;
    case 'WARN':
      return LogLevel.WARN;
    case 'INFO':
      return LogLevel.INFO;
    case 'DEBUG':
      return LogLevel.DEBUG;
    default:
      return defaultLevel;
  }
}
