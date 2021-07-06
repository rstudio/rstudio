/*
 * logger.test.ts
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

import { NullLogger, logger, LogLevel, parseCommandLineLogLevel, setLogger } from '../../../src/core/logger';
import { clearCoreSingleton } from '../../../src/core/core-state';


describe('Logger', () => {
  beforeEach(() => {
    clearCoreSingleton();
  });

  it('throws if unset', () => {
    assert.throws(() => { logger(); });
  });
  it('can be set and fetched', () => {
    const f = new NullLogger();
    setLogger(f);
    const fetched = logger();
    assert.exists(logger());
    assert.deepEqual(f, fetched);
  });
  it('log level can be parsed from string representation', () => {
    assert.equal(parseCommandLineLogLevel('OFF', LogLevel.DEBUG), LogLevel.OFF);
    assert.equal(parseCommandLineLogLevel('ERR', LogLevel.DEBUG), LogLevel.ERR);
    assert.equal(parseCommandLineLogLevel('WARN', LogLevel.DEBUG), LogLevel.WARN);
    assert.equal(parseCommandLineLogLevel('INFO', LogLevel.DEBUG), LogLevel.INFO);
    assert.equal(parseCommandLineLogLevel('DEBUG', LogLevel.OFF), LogLevel.DEBUG);
  });
});
