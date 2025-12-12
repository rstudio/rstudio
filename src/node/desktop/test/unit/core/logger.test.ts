/*
 * logger.test.ts
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

import { describe } from 'mocha';
import { assert } from 'chai';

import {
  NullLogger,
  logger,
  parseCommandLineLogLevel,
  setLogger,
  winstonLevelToCppLevel,
} from '../../../src/core/logger';
import { clearCoreSingleton } from '../../../src/core/core-state';

describe('Logger', () => {
  beforeEach(() => {
    clearCoreSingleton();
  });

  it('can be set and fetched', () => {
    const f = new NullLogger();
    setLogger(f);
    const fetched = logger();
    assert.exists(logger());
    assert.deepEqual(f, fetched);
  });
  it('log level can be parsed from string representation to Winston levels', () => {
    // Test abbreviated forms (backwards compatibility)
    assert.equal(parseCommandLineLogLevel('ERR', 'silly'), 'error');
    assert.equal(parseCommandLineLogLevel('WARN', 'silly'), 'warn');
    assert.equal(parseCommandLineLogLevel('INFO', 'silly'), 'info');
    assert.equal(parseCommandLineLogLevel('DEBUG', 'silly'), 'debug');

    // Test full names
    assert.equal(parseCommandLineLogLevel('ERROR', 'silly'), 'error');
    assert.equal(parseCommandLineLogLevel('WARNING', 'silly'), 'warn');

    // Test TRACE support (maps to debug since Winston doesn't have trace)
    assert.equal(parseCommandLineLogLevel('TRACE', 'silly'), 'debug');

    // Test case insensitivity
    assert.equal(parseCommandLineLogLevel('error', 'silly'), 'error');
    assert.equal(parseCommandLineLogLevel('warning', 'silly'), 'warn');
    assert.equal(parseCommandLineLogLevel('trace', 'silly'), 'debug');

    // Test invalid input falls back to default
    assert.equal(parseCommandLineLogLevel('INVALID', 'warn'), 'warn');
  });

  it('Winston levels can be converted to C++ Logger levels', () => {
    // Test standard levels
    assert.equal(winstonLevelToCppLevel('error'), 'ERROR');
    assert.equal(winstonLevelToCppLevel('warn'), 'WARNING');
    assert.equal(winstonLevelToCppLevel('info'), 'INFO');
    assert.equal(winstonLevelToCppLevel('debug'), 'DEBUG');

    // Test case insensitivity
    assert.equal(winstonLevelToCppLevel('ERROR'), 'ERROR');
    assert.equal(winstonLevelToCppLevel('WARN'), 'WARNING');
    assert.equal(winstonLevelToCppLevel('INFO'), 'INFO');
    assert.equal(winstonLevelToCppLevel('DEBUG'), 'DEBUG');

    // Test other Winston levels map to DEBUG
    assert.equal(winstonLevelToCppLevel('verbose'), 'DEBUG');
    assert.equal(winstonLevelToCppLevel('silly'), 'DEBUG');

    // Test invalid input falls back to WARNING
    assert.equal(winstonLevelToCppLevel('invalid'), 'WARNING');
  });
});
