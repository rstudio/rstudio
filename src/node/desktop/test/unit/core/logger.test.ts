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
/* eslint-disable @typescript-eslint/no-empty-function */
class FakeLogger implements Logger {
  uniqueId = 123;
  logError(err: Error): void {
  }
  logErrorMessage(message: string): void {
  }
  logInfo(message: string): void {
  }
  logWarning(warning: string): void {
  }
  logDebug(message: string): void {
  }
  logDiagnostic(message: string): void {
  }
  logDiagnosticEnvVar(name: string): void {
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
