/*
 * err.test.ts
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

import { Err, Success } from '../../src/core/err';

function beSuccessful(): Err {
  return Success();
}

function beUnsuccessful(): Err {
  return new Error('Some error');
}

describe('WIP Err', () => {
  describe('Success helper', () => {
    it('Success return should be falsy', () => {
      assert.isNotOk(beSuccessful());
      assert.isNull(beSuccessful());
    });
    it('Error return should be truthy', () => {
      assert.isOk(beUnsuccessful());
      assert.instanceOf(beUnsuccessful(), Error);
    });
  });
});
