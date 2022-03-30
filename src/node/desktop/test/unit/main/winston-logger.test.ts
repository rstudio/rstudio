/*
 * winston-logger.test.ts
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

import { describe } from 'mocha';
import { assert } from 'chai';
import { WinstonLogger } from '../../../src/core/winston-logger';
import { FilePath } from '../../../src/core/file-path';

describe('WinstonLogger', () => {
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
});
