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

import { Logger, logger, setLogger } from '../../../src/core/logger';

/* eslint-disable @typescript-eslint/no-unused-vars */
class FakeLogger implements Logger {
  uniqueId = 123;
  logError(err: Error): void {
    throw new Error('Method not implemented.');
  }
  logErrorMessage(message: string): void {
    throw new Error('Method not implemented.');
  }
  logInfo(message: string): void {
    throw new Error('Method not implemented.');
  }
  logWarning(warning: string): void {
    throw new Error('Method not implemented.');
  }
  logDebug(message: string): void {
    throw new Error('Method not implemented.');
  }
  logDiagnostic(message: string): void {
    throw new Error('Method not implemented.');
  }
  logDiagnosticEnvVar(name: string): void {
    throw new Error('Method not implemented.');
  }
}

describe('Logger', () => {
  it('throws if unset', () => {
    assert.throws(() => { logger(); });
  });
  it('can be set and fetched', () => {
    const f = new FakeLogger();
    setLogger(f);
    const fetched = logger();
    assert.exists(logger());
    assert.deepEqual(f, fetched);
  });
});
