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
import sinon from 'sinon';

import { ConsoleLogger } from '../../../src/core/console-logger';

describe('Console-logger', () => {
  let fakeConsoleLog = sinon.fake();
  beforeEach(() => {
    fakeConsoleLog = sinon.fake();
    sinon.replace(console, 'log', fakeConsoleLog);
  });

  afterEach(() => {
    sinon.restore();
  });

  it('logError writes error', () => {
    const logger = new ConsoleLogger();
    const err = new Error('test Errormessage');
    logger.logError(err);
    assert.isTrue(fakeConsoleLog.calledWithMatch(err));
  });
  it('logErrorMessage', () => {
    const logger = new ConsoleLogger();
    const msg = 'test error message';
    logger.logErrorMessage(msg);
    assert.isTrue(fakeConsoleLog.calledWith(msg));
  });
  it('logInfo', () => {
    const logger = new ConsoleLogger();
    const msg = 'test info message';
    logger.logInfo(msg);
    assert.isTrue(fakeConsoleLog.calledWith(msg));
  });
  it('logWarning', () => {
    const logger = new ConsoleLogger();
    const msg = 'test warning message';
    logger.logWarning(msg);
    assert.isTrue(fakeConsoleLog.calledWith(msg));
  });
  it('logDebug', () => {
    const logger = new ConsoleLogger();
    const msg = 'test debug message';
    logger.logDebug(msg);
    assert.isTrue(fakeConsoleLog.calledWith(msg));
  });
  it('logDiagnostic', () => {
    const logger = new ConsoleLogger();
    const msg = 'test diagnostic message';
    logger.logDiagnostic(msg);
    assert.isTrue(fakeConsoleLog.calledWith(msg));
  });
  it('logDiagnosticEnvVar', () => {
    const logger = new ConsoleLogger();
    logger.logDiagnosticEnvVar('PATH');
    assert.isTrue(fakeConsoleLog.calledOnce);
  });
});
