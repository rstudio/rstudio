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
import { setLoggerLevel, LogLevel, enableDiagnosticsOutput } from '../../../src/core/logger';

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
    setLoggerLevel(LogLevel.ERR);
    const logger = new ConsoleLogger();
    const err = new Error('test Errormessage');
    logger.logError(err);
    assert.isTrue(fakeConsoleLog.calledWithMatch(err));
  });
  it('logError output suppressed when LogLevel = OFF', () => {
    setLoggerLevel(LogLevel.OFF);
    const logger = new ConsoleLogger();
    const err = new Error('test Errormessage');
    logger.logError(err);
    assert.isFalse(fakeConsoleLog.called);
  });
  it('logError writes error when LogLevel = DEBUG', () => {
    setLoggerLevel(LogLevel.DEBUG);
    const logger = new ConsoleLogger();
    const err = new Error('test Errormessage');
    logger.logError(err);
    assert.isTrue(fakeConsoleLog.calledWithMatch(err));
  });
  it('logErrorMessage', () => {
    setLoggerLevel(LogLevel.ERR);
    const logger = new ConsoleLogger();
    const msg = 'test error message';
    logger.logErrorMessage(msg);
    assert.isTrue(fakeConsoleLog.calledWith(msg));
  });
  it('logErrorMessage suppressed when LogLevel = OFF', () => {
    setLoggerLevel(LogLevel.OFF);
    const logger = new ConsoleLogger();
    const msg = 'test error message';
    logger.logErrorMessage(msg);
    assert.isFalse(fakeConsoleLog.called);
  });
  it('logInfo', () => {
    setLoggerLevel(LogLevel.INFO);
    const logger = new ConsoleLogger();
    const msg = 'test info message';
    logger.logInfo(msg);
    assert.isTrue(fakeConsoleLog.calledWith(msg));
  });
  it('logInfo suppressed when LogLevel = WARN', () => {
    setLoggerLevel(LogLevel.WARN);
    const logger = new ConsoleLogger();
    const msg = 'test info message';
    logger.logInfo(msg);
    assert.isFalse(fakeConsoleLog.called);
  });
  it('logWarning', () => {
    setLoggerLevel(LogLevel.WARN);
    const logger = new ConsoleLogger();
    const msg = 'test warning message';
    logger.logWarning(msg);
    assert.isTrue(fakeConsoleLog.calledWith(msg));
  });
  it('logWarning suppressed when LogLevel = ERR', () => {
    setLoggerLevel(LogLevel.ERR);
    const logger = new ConsoleLogger();
    const msg = 'test warning message';
    logger.logWarning(msg);
    assert.isFalse(fakeConsoleLog.called);
  });
  it('logDebug', () => {
    setLoggerLevel(LogLevel.DEBUG);
    const logger = new ConsoleLogger();
    const msg = 'test debug message';
    logger.logDebug(msg);
    assert.isTrue(fakeConsoleLog.calledWith(msg));
  });
  it('logDebug suppressed whebn LogLevel = INFO', () => {
    setLoggerLevel(LogLevel.INFO);
    const logger = new ConsoleLogger();
    const msg = 'test debug message';
    logger.logDebug(msg);
    assert.isFalse(fakeConsoleLog.called);
  });
  it('logDiagnostic silent by default', () => {
    const logger = new ConsoleLogger();
    const msg = 'test diagnostic message';
    logger.logDiagnostic(msg);
    assert.isFalse(fakeConsoleLog.called);
  });
  it('logDiagnosticEnvVar silent by default', () => {
    const logger = new ConsoleLogger();
    logger.logDiagnosticEnvVar('PATH');
    assert.isFalse(fakeConsoleLog.called);
  });
  it('logDiagnostic obeys setting', () => {
    const logger = new ConsoleLogger();
    enableDiagnosticsOutput();
    const msg = 'test diagnostic message';
    logger.logDiagnostic(msg);
    assert.isTrue(fakeConsoleLog.calledWith(msg));
  });
  it('logDiagnosticEnvVar obeys setting', () => {
    const logger = new ConsoleLogger();
    enableDiagnosticsOutput();
    logger.logDiagnosticEnvVar('PATH');
    assert.isTrue(fakeConsoleLog.calledOnce);
  });
});
