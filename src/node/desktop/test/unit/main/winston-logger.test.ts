/*
 * winston-logger.test.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { describe } from 'mocha';
import { assert } from 'chai';
import { WinstonLogger } from '../../../src/core/winston-logger';
import { FilePath } from '../../../src/core/file-path';
import { restore, saveAndClear } from '../unit-utils';
import { ArgsManager } from '../../../src/main/args-manager';
import { coreState } from '../../../src/core/core-state';

describe('WinstonLogger', () => {
  const testEnv = { RS_LOG_LEVEL: '' };

  beforeEach(() => {
    saveAndClear(testEnv);
  });

  afterEach(() => {
    restore(testEnv);
  });

  it('Logger is created correctly', () => {
    const logLevels: any[] = ['warn', 'error', 'info', 'http', 'verbose', 'debug', 'silly'];

    logLevels.forEach((logLevel) => {
      const logger = new WinstonLogger(new FilePath('./tmp/test.log'), logLevel);
      assert.equal(logger.logLevel(), logLevel, 'Logger log level should be ' + logLevel);
    });
  });

  it('Logger level is changed correctly', () => {
    const logLevels: any[] = ['warn', 'error', 'info', 'http', 'verbose', 'debug', 'silly'];

    logLevels.forEach((logLevel) => {
      const logger = new WinstonLogger(new FilePath('./tmp/test.log'), logLevel == 'warn' ? 'info' : 'warn');

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
  })
});
