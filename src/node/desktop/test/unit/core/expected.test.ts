/*
 * expected.test.ts
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
import { err, Expected, ok } from '../../../src/core/expected';

function returnResult(shouldSucceed: boolean): Expected<string> {
  if (shouldSucceed) {
    return ok('victory');
  } else {
    return err(new Error('Try again'));
  }
}

describe('Expected', () => {
  it('Success return should contain result', () => {
    const [result, error] = returnResult(true);
    assert.isNull(error);
    assert.isNotNull(result);
    assert.deepEqual(result, 'victory');
  });
  it('Error return should have an error', () => {
    const [result, error] = returnResult(false);
    assert.isNull(result);
    assert.isNotNull(error);
    assert.deepEqual(error?.message, 'Try again');
  });
});
