/*
 * log-options.ts
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
import PropertiesReader from 'properties-reader';
import { FilePath } from '../core/file-path';
import { parseCommandLineLogLevel } from '../core/logger';
import { Xdg } from '../core/xdg';
import { Option, options } from './args-manager';
import { userLogPath } from './utils';

const logConfFile = 'logging.conf';

interface LogConfig {
  config?: string, // can be contents of logging.conf
  file?: string, // path to conf file to use instead of logging.conf
}

class LogOptions {
  private logConfContent: PropertiesReader.Reader;
  private executableName: string;

  /**
   * @param config `logging.conf` file contents
   */
  constructor(executableName?: string, logConfig?: LogConfig) {
    const execPath = new FilePath(process.execPath);
    this.executableName = executableName ?? execPath.getFilename();

    // env var RS_LOG_CONF_FILE
    let location = logConfig?.file ? new FilePath(logConfig.file) : new FilePath(process.env.RS_LOG_CONF_FILE);

    if (!logConfig?.config) {
      if (!location.existsSync()) {
        // userConfigDir
        location = Xdg.userConfigDir().completeChildPath(logConfFile);
  
        if (!location.existsSync()) {
          // systemConfigDir
          location = Xdg.systemConfigFile(logConfFile);
        }
      }
  
      if (location.existsSync()) {
        this.logConfContent = PropertiesReader(location.getAbsolutePathNative());
      } else {
        this.logConfContent = PropertiesReader('');
      }
    } else {
      this.logConfContent = PropertiesReader('').read(logConfig.config);
    }
  }

  getLogLevel(): string {
    const argValue = app.commandLine.getSwitchValue(options['logLevel'].arg);
    if (argValue) {
      return parseCommandLineLogLevel(argValue, 'warn');
    }
    return this.getProperty(options['logLevel']) ?? 'warn';
  }

  getLoggerType(): string {
    const loggerType = this.getProperty(options['loggerType']) ?? 'file';

    return loggerType;
  }

  getLogMessageFormat(): string {
    const loggerType = this.getProperty(options['logMessageFormat']) ?? 'pretty';

    return loggerType;
  }

  getLogFile(): FilePath {
    const logDirName = this.getProperty(options['logDir']);
    const logDir = logDirName ? new FilePath(logDirName) : userLogPath();

    return logDir.completeChildPath('rdesktop.log');
  }

  private getProperty(option: Option): string | undefined {
    // check environment variable then command-line argument
    const value = process.env[option.env] ?? app.commandLine.getSwitchValue(option.arg);

    // if no value specified, check logging.conf
    if (value) {
      return value;
    } else {
      return this.logConfContent.get(`@${this.executableName}.${option.arg}`)?.toString() ??
      this.logConfContent.get(`*.${option.arg}`)?.toString();
    }
  }
}

export default LogOptions;
