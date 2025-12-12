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
  levelToCppLevel,
  normalizeToWinstonLevel,
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
  it('log level can be parsed from string representation', () => {
    // Test abbreviated forms (backwards compatibility)
    assert.equal(parseCommandLineLogLevel('ERR', 'silly'), 'error');
    assert.equal(parseCommandLineLogLevel('WARN', 'silly'), 'warn');
    assert.equal(parseCommandLineLogLevel('INFO', 'silly'), 'info');
    assert.equal(parseCommandLineLogLevel('DEBUG', 'silly'), 'debug');

    // Test full names
    assert.equal(parseCommandLineLogLevel('ERROR', 'silly'), 'error');
    assert.equal(parseCommandLineLogLevel('WARNING', 'silly'), 'warn');

    // Test TRACE support (returns 'trace' which will be normalized later)
    assert.equal(parseCommandLineLogLevel('TRACE', 'silly'), 'trace');

    // Test case insensitivity
    assert.equal(parseCommandLineLogLevel('error', 'silly'), 'error');
    assert.equal(parseCommandLineLogLevel('warning', 'silly'), 'warn');
    assert.equal(parseCommandLineLogLevel('trace', 'silly'), 'trace');

    // Test invalid input falls back to default
    assert.equal(parseCommandLineLogLevel('INVALID', 'warn'), 'warn');
  });

  it('log levels can be normalized to Winston-compatible levels', () => {
    // Test lowercase levels (already Winston-compatible, pass through)
    assert.equal(normalizeToWinstonLevel('error'), 'error');
    assert.equal(normalizeToWinstonLevel('warn'), 'warn');
    assert.equal(normalizeToWinstonLevel('info'), 'info');
    assert.equal(normalizeToWinstonLevel('debug'), 'debug');

    // Test C++ level names that need conversion
    assert.equal(normalizeToWinstonLevel('WARNING'), 'warn');
    assert.equal(normalizeToWinstonLevel('warning'), 'warn');
    assert.equal(normalizeToWinstonLevel('ERR'), 'error');
    assert.equal(normalizeToWinstonLevel('err'), 'error');

    // Test C++ uppercase levels that match Winston names (pass through)
    assert.equal(normalizeToWinstonLevel('ERROR'), 'error');
    assert.equal(normalizeToWinstonLevel('INFO'), 'info');
    assert.equal(normalizeToWinstonLevel('DEBUG'), 'debug');

    // Test TRACE (maps to debug since Winston doesn't have trace)
    assert.equal(normalizeToWinstonLevel('trace'), 'debug');
    assert.equal(normalizeToWinstonLevel('TRACE'), 'debug');

    // Test that other Winston levels pass through
    assert.equal(normalizeToWinstonLevel('http'), 'http');
    assert.equal(normalizeToWinstonLevel('verbose'), 'verbose');
    assert.equal(normalizeToWinstonLevel('silly'), 'silly');
  });

  it('log levels can be converted to C++ Logger levels', () => {
    // Test standard levels
    assert.equal(levelToCppLevel('error'), 'ERROR');
    assert.equal(levelToCppLevel('warn'), 'WARNING');
    assert.equal(levelToCppLevel('info'), 'INFO');
    assert.equal(levelToCppLevel('debug'), 'DEBUG');
    assert.equal(levelToCppLevel('trace'), 'TRACE');

    // Test case insensitivity
    assert.equal(levelToCppLevel('ERROR'), 'ERROR');
    assert.equal(levelToCppLevel('WARN'), 'WARNING');
    assert.equal(levelToCppLevel('INFO'), 'INFO');
    assert.equal(levelToCppLevel('DEBUG'), 'DEBUG');
    assert.equal(levelToCppLevel('TRACE'), 'TRACE');

    // Test other Winston levels map to DEBUG
    assert.equal(levelToCppLevel('verbose'), 'DEBUG');
    assert.equal(levelToCppLevel('silly'), 'DEBUG');

    // Test invalid input falls back to WARNING
    assert.equal(levelToCppLevel('invalid'), 'WARNING');
  });
});
