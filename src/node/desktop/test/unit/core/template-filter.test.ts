/*
 * template-filter.test.ts
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

import { renderTemplateString } from '../../../src/core/template-filter';

describe('template-filter', () => {
  it('renderTemplateString returns same string when no templates', () => {
    const source =
      `Hello world, how
       are you?`;
    const result = renderTemplateString(source, new Map<string, string>());
    assert.equal(source, result);
  });
  it('renderTemplateString returns same string when string contains unmatched #', () => {
    const source =
      `Hello #world, how
       are you?`;
    const result = renderTemplateString(source, new Map<string, string>());
    assert.equal(source, result);
  });
  it('renderTemplateString replaces single simple variable', () => {
    const source =
      `Hello #world#, how
       are you?`;
    const vars = new Map<string, string>([
      ['world', 'People of Earth']
    ]);
    const expected =
      `Hello People of Earth, how
       are you?`;

    const result = renderTemplateString(source, vars);
    assert.equal(expected, result);
  });
  it('renderTemplateString HTML-encodes variable', () => {
    const source = 'Hello #world#, how are you?';
    const vars = new Map<string, string>([
      ['world', '<my test>']
    ]);
    const expected = 'Hello &lt;my test&gt;, how are you?';

    const result = renderTemplateString(source, vars);
    assert.equal(expected, result);
  });
  it('renderTemplateString passes through raw variable', () => {
    const source = 'Hello #!world#, how are you?';
    const vars = new Map<string, string>([
      ['world', '<my test>']
    ]);
    const expected = 'Hello <my test>, how are you?';

    const result = renderTemplateString(source, vars);
    assert.equal(expected, result);
  });
  it('renderTemplateString js-encodes variable', () => {
    const source = 'Hello #\'world#, how are you?';
    const vars = new Map<string, string>([
      ['world', '"hello"']
    ]);
    const expected = 'Hello \\"hello\\", how are you?';

    const result = renderTemplateString(source, vars);
    assert.equal(expected, result);
  });
});