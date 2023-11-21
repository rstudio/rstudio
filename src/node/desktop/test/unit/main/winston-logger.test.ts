/*
 * winston-logger.test.ts
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

import { assert } from 'chai';
import { openSync, writeFileSync } from 'fs';
import { describe } from 'mocha';
import { coreState } from '../../../src/core/core-state';
import { FilePath } from '../../../src/core/file-path';
import { WinstonLogger } from '../../../src/core/winston-logger';
import { ArgsManager } from '../../../src/main/args-manager';
import LogOptions from '../../../src/main/log-options';
import { restore, saveAndClear } from '../unit-utils';

describe('WinstonLogger', () => {
  const testEnv = { RS_LOG_LEVEL: '', RS_LOG_CONF_FILE: '' };

  beforeEach(() => {
    saveAndClear(testEnv);
  });

  afterEach(() => {
    const tmpPath = new FilePath('./tmp');
    tmpPath.removeSync();
    restore(testEnv);
  });

  it('Logger is created correctly', () => {
    const logLevels: string[] = ['warn', 'error', 'info', 'http', 'verbose', 'debug', 'silly'];

    logLevels.forEach((logLevel) => {
      const logOptions = new LogOptions(undefined, { config: `[*]\nlog-level=${logLevel}\nlog-dir=./tmp` });
      const logger = new WinstonLogger(logOptions);
      assert.equal(logger.logLevel(), logLevel, 'Logger log level should be ' + logLevel);
    });
  });

  it('Logger level is changed correctly', () => {
    const logLevels: string[] = ['warn', 'error', 'info', 'http', 'verbose', 'debug', 'silly'];

    logLevels.forEach((logLevel) => {
      const logOptions = new LogOptions(undefined, { config: `[*]\nlog-level=${logLevel}\nlog-dir=./tmp` });
      const logger = new WinstonLogger(logOptions);
      logger.setLogLevel(logLevel);

      assert.equal(logger.logLevel(), logLevel, 'Logger log level should be ' + logLevel);
    });
  });

  it('Logger level is set correctly from environment variable', () => {
    const argsMananager = new ArgsManager();

    process.env.RS_LOG_LEVEL = 'debug';
    argsMananager.handleLogLevel();
    assert.equal(coreState().logOptions.logger.logLevel(), 'debug');

    process.env.RS_LOG_LEVEL = 'info';
    argsMananager.handleLogLevel();
    assert.equal(coreState().logOptions.logger.logLevel(), 'info');
  });

  it('Log dir can be set for log file', () => {
    const logDir = './tmp/rstudio/logs';
    const logConf =
    `[*]\n\
    log-dir=${logDir}\
    `;
    const logOptions = new LogOptions(undefined, { config: logConf });

    const expected = (new FilePath(logDir)).completeChildPath('rdesktop.log');
    assert.equal(logOptions.getLogFile().getAbsolutePath(), expected.getAbsolutePath());
  });

  it('Can set logging.conf', async () => {
    const logConf =
    '[*]\n\
    log-level=debug\
    ';
    const confPath = './tmp/conf/custom_logging.conf';
    const confFile = new FilePath(confPath);

    confFile.getParent().ensureDirectorySync();

    const fd = openSync(confPath, 'a');
    writeFileSync(fd, logConf);
    const logOptions = new LogOptions(undefined, { file: confPath });

    assert.equal(logOptions.getLogLevel(), 'debug');
  });

  it('Can set RS_LOG_CONF_FILE', async () => {
    const logConf =
    '[*]\n\
    log-level=error\
    ';
    const confPath = './tmp/conf/env_set.conf';
    const confFile = new FilePath(confPath);
    process.env.RS_LOG_CONF_FILE = confPath;

    confFile.getParent().ensureDirectorySync();

    const fd = openSync(confPath, 'a');
    writeFileSync(fd, logConf);
    const logOptions = new LogOptions();

    assert.equal(logOptions.getLogLevel(), 'error');
  });

  it('Can set log message format to json', () => {
    const logConf =
    '[*]\n\
    log-message-format=json\
    ';
    const logOptions = new LogOptions(undefined, { config: logConf });

    assert.equal(logOptions.getLogMessageFormat(), 'json');
  });

  it('Can set logger type', () => {
    const loggerTypes: string[] = ['file', 'syslog', 'stderr'];
    loggerTypes.forEach((loggerType) => {
      const logConf =
      `[*]\n\
      logger-type=${loggerType}\
      `;
      const logOptions = new LogOptions(undefined, { config: logConf });

      assert.equal(logOptions.getLoggerType(), loggerType);
    });
  });

  it('Can get log level for rsession', () => {
    const logConf =
    `[*]\n\
    log-level=debug
    [@rsession]
    log-level=info
    `;
    const logOptions = new LogOptions('rsession', { config: logConf });

    assert.equal(logOptions.getLogLevel(), 'info');
  });

  it('Can get global log level', () => {
    const logConf =
    `[*]\n\
    log-level=debug
    [@rsession]
    log-level=info
    `;
    const logOptions = new LogOptions(undefined, { config: logConf });

    assert.equal(logOptions.getLogLevel(), 'debug');
  });
});
