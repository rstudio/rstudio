/*
 * err.test.ts
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

import { Err, isFailure, isSuccessful, safeError, success } from '../../../src/core/err';

function beSuccessful(): Err {
  return success();
}

function beUnsuccessful(): Err {
  return new Error('Some error');
}

class MyError extends Error {
  constructor(message: string) {
    super(message);
  }
}

describe('Err', () => {
  it('Success return should be falsy', () => {
    assert.isTrue(beSuccessful() === success());
    assert.isNull(beSuccessful());
  });
  it('Error return should be truthy', () => {
    assert.isTrue(beUnsuccessful() !== success());
    assert.instanceOf(beUnsuccessful(), Error);
  });
  it('isSuccessful returns true for success', () => {
    const result: Err = success();
    assert.isTrue(isSuccessful(result));
  });
  it('isSuccessful returns false for failure', () => {
    const result: Err = new Error('oh no');
    assert.isFalse(isSuccessful(result));
  });
  it('isFailure returns true for failure', () => {
    const result: Err = new Error('whoop');
    assert.isTrue(isFailure(result));
  });
  it('isFailure returns false for success', () => {
    const result: Err = success();
    assert.isFalse(isFailure(result));
  });
  it('safeError returns unknown error Error for unsupported error type', () => {
    const bogusErr = 3; // pretend somebody did "throw 3;"
    const err = safeError(bogusErr);
    assert.deepEqual(err.message, 'unknown error');
  });
  it('safeError returns received Error', () => {
    const realErr = new Error('hello');
    const err = safeError(realErr);
    assert.deepEqual(err.message, 'hello');
  });
  it('safeError returns received Error subclass', () => {
    const realErr = new MyError('hello world');
    const err = safeError(realErr);
    assert.deepEqual(err.message, 'hello world');
  });
  it('safeError returns unknown error for null', () => {
    const bogusErr = null;
    const err = safeError(bogusErr);
    assert.deepEqual(err.message, 'unknown error');
  });
  it('safeError returns unknown error for undefined', () => {
    const bogusErr = undefined;
    const err = safeError(bogusErr);
    assert.deepEqual(err.message, 'unknown error');
  });
});
