/*
 * console-logger.test.ts
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

import { describe } from 'mocha';
import { assert } from 'chai';

import { ConsoleLogger } from '../../../src/core/console-logger';

describe('Console-logger', () => {
  it('can be created', () => {
    const logger = new ConsoleLogger();
    assert.isNotNull(logger);
  });
  it('logError', () => {
    const logger = new ConsoleLogger();
    logger.logError(new Error('test Errormessage'));
    assert.isTrue(true);
  });
  it('logErrorMessage', () => {
    const logger = new ConsoleLogger();
    logger.logErrorMessage('test error message');
    assert.isTrue(true);
  });
  it('logInfo', () => {
    const logger = new ConsoleLogger();
    logger.logInfo('test info message');
    assert.isTrue(true);
  });
  it('logWarning', () => {
    const logger = new ConsoleLogger();
    logger.logWarning('test warning message');
    assert.isTrue(true);
  });
  it('logDebug', () => {
    const logger = new ConsoleLogger();
    logger.logDebug('test debug message');
    assert.isTrue(true);
  });
  it('logDiagnostic', () => {
    const logger = new ConsoleLogger();
    logger.logDiagnostic('test diagnostic message');
    assert.isTrue(true);
  });
  it('logDiagnosticEnvVar', () => {
    const logger = new ConsoleLogger();
    logger.logDiagnosticEnvVar('PATH');
    assert.isTrue(true);
  });
});
