/*
 * desktop.test.ts
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

import desktop from '../../../src/native/desktop.node';
import { clipboard } from 'electron';
import os from 'os';

describe('Desktop Native Code', () => {
  it('cleanClipboard with plain text', () => {
    clipboard.writeText('write to clipboard');
    desktop.cleanClipboard(false);
    assert.equal(clipboard.readText('clipboard'), 'write to clipboard');
  });

  // HTML stripping only available on Mac to handle pasteboard types
  it('cleanClipboard with strip HTML', () => {
    const htmlText = '<div class="body">\
    <div class="pm-content">\
    <h1 data-pm-pandoc-attr="1" class=" pm-heading">Summary</h1>\
    <p>Nullam augue</p>\
    </div></div>';
    const plainText = `Summary${JSON.stringify(os.EOL)}Nullam augue`;
    const expected = process.platform === 'darwin' ? plainText : htmlText;

    clipboard.write({
      text: plainText,
      html: htmlText
    });
    desktop.cleanClipboard(true);
    assert.equal(clipboard.readHTML('clipboard'), expected);
  });

  it('cleanClipboard with HTML', () => {
    const htmlText = '<div class="body">\
    <div class="pm-content">\
    <h1 data-pm-pandoc-attr="1" class=" pm-heading">Summary</h1>\
    <p>Nullam augue</p>\
    </div></div>';
    const plainText = 'Summary\nNullam augue';
    const expected = process.platform === 'darwin' ?  `<meta charset='utf-8'>${htmlText}` : htmlText;

    clipboard.write({
      text: plainText,
      html: htmlText
    });
    desktop.cleanClipboard(false);
    assert.equal(clipboard.readHTML('clipboard'), expected);
  });
});
