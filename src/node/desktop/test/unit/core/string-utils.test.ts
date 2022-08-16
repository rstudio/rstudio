/*
 * string-utils.ts
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

import { htmlEscape, jsLiteralEscape, removeDups } from '../../../src/core/string-utils';

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
  it('htmlEscape non-attr escapes expected characters', () => {
    const source = "6 is > 5 and < than 12. Time for some R&R. \"Some\" 'quotes'. Don't /slash/ and burn!";
    const expected =
      '6 is &gt; 5 and &lt; than 12. Time for some R&amp;R. &quot;Some&quot; &#x27;quotes&#x27;. Don&#x27;t &#x2F;slash&#x2F; and burn!';
    const result = htmlEscape(source, false);
    assert.equal(result, expected);
  });
  it('htmlEscape attr escapes expected characters', () => {
    const source = "6 is > 5 and < than 12. Time for some R&R. \"Some\" 'quotes'. Don't /slash/ and burn!";
    const expected =
      '6 is &gt; 5 and &lt; than 12. Time for some R&amp;R. &quot;Some&quot; &#x27;quotes&#x27;. Don&#x27;t &#x2F;slash&#x2F; and burn!';
    const result = htmlEscape(source, true);
    assert.equal(result, expected);
  });
  it('htmlEscape attr escapes line endings', () => {
    const source = '6 is > 5 and < than 12.\nTime for some R&R.\r"Some" \'quotes\'.';
    const expected =
      '6 is &gt; 5 and &lt; than 12.&#10;Time for some R&amp;R.&#13;&quot;Some&quot; &#x27;quotes&#x27;.';
    const result = htmlEscape(source, true);
    assert.equal(result, expected);
  });
  it('jsLiteralEscape escapes expected characters', () => {
    const source = '"hello" \'goodbye\' </script>';
    const expected = '\\"hello\\" \\\'goodbye\\\' \\074/script>';
    const result = jsLiteralEscape(source);
    assert.equal(result, expected);
  });
});
