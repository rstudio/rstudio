/*
 * logger.test.ts
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

import { NullLogger, logger, parseCommandLineLogLevel, setLogger } from '../../../src/core/logger';
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
    assert.equal(parseCommandLineLogLevel('ERR', 'silly'), 'error');
    assert.equal(parseCommandLineLogLevel('WARN', 'silly'), 'warn');
    assert.equal(parseCommandLineLogLevel('INFO', 'silly'), 'info');
    assert.equal(parseCommandLineLogLevel('DEBUG', 'silly'), 'debug');
  });
});
