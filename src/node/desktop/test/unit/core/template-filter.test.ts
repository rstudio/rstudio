/*
 * template-filter.test.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import { resolveTemplateVar } from '../../../src/core/template-filter';

describe('template-filter', () => {
  it('resolveTemplateVar replaces single simple variable', () => {
    const vars = new Map<string, string>([['world', 'People of Earth']]);
    const expected = 'People of Earth';

    const result = resolveTemplateVar('world', vars);
    assert.equal(expected, result);
  });
  it('resolveTemplateVar HTML-encodes variable', () => {
    const vars = new Map<string, string>([['world', '<my test>']]);
    const expected = '&lt;my test&gt;';

    const result = resolveTemplateVar('world', vars);
    assert.equal(expected, result);
  });
  it('resolveTemplateVar passes through raw variable', () => {
    const vars = new Map<string, string>([['world', '<my test>']]);
    const expected = '<my test>';

    const result = resolveTemplateVar('!world', vars);
    assert.equal(expected, result);
  });
  it('resolveTemplateVar js-encodes variable', () => {
    const vars = new Map<string, string>([['world', '"hello"']]);
    const expected = '\\"hello\\"';

    const result = resolveTemplateVar("'world", vars);
    assert.equal(expected, result);
  });
});
