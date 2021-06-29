/*
 * user.test.ts
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

import { removeDups } from '../../../src/core/string-utils';

describe('string-util', () => {
  it('removeDups is a no-op on empty array', () => {
    assert.equal(removeDups([]).length, 0);
  });
  it('removeDups returns same array when no dups', () => {
    const orig = ['one', 'two', 'three'];
    const result = removeDups(orig);
    assert.deepEqual(result, orig);
  });
  it('removeDups removes a single dup in middle', () => {
    const orig = ['one', 'two', 'one', 'three'];
    const expected = ['one', 'two', 'three'];
    const result = removeDups(orig);
    assert.deepEqual(result, expected);
  });
  it('removeDups removes multiple equivalent dups', () => {
    const orig = ['one', 'two', 'two', 'two', 'three', 'two'];
    const expected = ['one', 'two', 'three'];
    const result = removeDups(orig);
    assert.deepEqual(result, expected);
  });
  it('removeDups removes multiple dups', () => {
    const orig = ['one', 'one', 'two', 'one', 'three', 'two', 'two', 'one', 'three', 'two'];
    const expected = ['one', 'two', 'three'];
    const result = removeDups(orig);
    assert.deepEqual(result, expected);
  });
});
 