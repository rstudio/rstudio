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

/**
 * Class representing the options for a file logger.
 */
export class FileLogOptions {

  /**
   * @param directory     The directory in which to create log files.
   * @param fileMode      The permissions to set on log files.
   * @param maxSizeMb     The maximum size of log files, in MB, before they are rotated and/or overwritten.b
   * @param doRotation    Whether to rotate log files or not.on
   * @param includePid    Whether to include the PID of the process in the logs.id
   */
  constructor(
    public readonly directory: string,
    public readonly fileMode = '666',
    public readonly maxSizeMb = 2,
    public readonly doRotation = true,
    public readonly includePid = false
  ) {}
}

/**
 * Enum representing the level of detail for logging messages.
 */
export enum LogLevel {
  OFF, // No messages will be logged.
  ERR, // Error messages will be logged.
  WARN, // Warning and error messages will be logged.
  INFO, // Info, warning, and error messages will be logged.
  DEBUG, // All messages will be logged.
}

export enum LoggerType {
  kStdErr,
  kSysLog,
  kFile,
}
