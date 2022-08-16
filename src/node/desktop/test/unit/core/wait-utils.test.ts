/*
 * wait-utils.test.ts
 *
 * Copyright (C) 2022 by Posit, PBC
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
import { WaitResult, WaitTimeoutFn, waitWithTimeout } from '../../../src/core/wait-utils';
import { isFailure, isSuccessful } from '../../../src/core/err';

describe('wait-utils', () => {
  it('waitWithTimeout returns error after max wait reached', async () => {
    let numCalls = 0;
    const neverSucceeds: WaitTimeoutFn = async () => {
      numCalls++;
      return Promise.resolve(new WaitResult('WaitContinue'));
    };

    const error = await waitWithTimeout(neverSucceeds, 1, 2, 1);
    assert.isTrue(isFailure(error));
    assert.isAbove(numCalls, 0);
  });
  it('waitWithTimeout returns success on first try', async () => {
    let numCalls = 0;
    const immediatelySucceeds: WaitTimeoutFn = async () => {
      numCalls++;
      return Promise.resolve(new WaitResult('WaitSuccess'));
    };

    const error = await waitWithTimeout(immediatelySucceeds, 1, 2, 1);
    assert.isTrue(isSuccessful(error));
    assert.equal(numCalls, 1);
  });
  it('waitWithTimeout returns success on retry', async () => {
    let numCalls = 0;
    const eventuallySucceeds: WaitTimeoutFn = async () => {
      numCalls++;
      return Promise.resolve(new WaitResult(numCalls == 2 ? 'WaitSuccess' : 'WaitContinue'));
    };

    const error = await waitWithTimeout(eventuallySucceeds, 1, 2, 1);
    assert.isTrue(isSuccessful(error));
    assert.equal(numCalls, 2);
  });
});
