/*
 * desktop.test.ts
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

import { assert } from 'chai';
import { clipboard } from 'electron';
import { describe } from 'mocha';

import os from 'os';

import desktop from '../../../src/native/desktop.node';

describe('Desktop Native Code', () => {
  it('cleanClipboard with plain text', () => {
    clipboard.writeText('write to clipboard');
    desktop.cleanClipboard(false);
    assert.equal(clipboard.readText('clipboard'), 'write to clipboard');
  });

  // Exercises the UTF-16 -> UTF-8 conversion with multibyte BMP characters
  // (cafe, CJK) and an astral character requiring a surrogate pair (emoji),
  // which the ASCII-only cases above do not cover.
  it('cleanClipboard preserves non-ASCII plain text', () => {
    const text = 'café 世界 😀';
    clipboard.writeText(text);
    desktop.cleanClipboard(false);
    assert.equal(clipboard.readText('clipboard'), text);
  });

  // Malformed UTF-16 that decodes to nothing must not wipe the clipboard:
  // conversion happens before the pasteboard is cleared, and cleanClipboard
  // bails out when it produces no text. macOS-only (utf16 pasteboard flavor).
  it('cleanClipboard leaves malformed UTF-16 data untouched', function () {
    if (process.platform !== 'darwin') {
      this.skip();
    }
    const malformed = Buffer.from([0x00, 0xd8]); // lone high surrogate (U+D800)
    clipboard.clear();
    clipboard.writeBuffer('public.utf16-plain-text', malformed);
    desktop.cleanClipboard(false);
    assert.deepEqual([...clipboard.readBuffer('public.utf16-plain-text')], [...malformed]);
  });

  // Valid UTF-16 that begins with a NUL must still be cleaned rather than
  // mistaken for a decode failure: the NUL is preserved and the text converted.
  it('cleanClipboard handles valid UTF-16 with a leading NUL', function () {
    if (process.platform !== 'darwin') {
      this.skip();
    }
    clipboard.clear();
    // UTF-16LE bytes for a NUL (U+0000) followed by 'A'
    clipboard.writeBuffer('public.utf16-plain-text', Buffer.from([0x00, 0x00, 0x41, 0x00]));
    desktop.cleanClipboard(false);
    assert.equal(clipboard.readText('clipboard'), '\u0000A');
  });

  // HTML stripping only available on Mac to handle pasteboard types
  it('cleanClipboard with strip HTML', () => {
    const htmlText =
      '<div class="body">\
    <div class="pm-content">\
    <h1 data-pm-pandoc-attr="1" class=" pm-heading">Summary</h1>\
    <p>Nullam augue</p>\
    </div></div>';
    const plainText = `Summary${JSON.stringify(os.EOL)}Nullam augue`;
    const expected = process.platform === 'darwin' ? plainText : htmlText;

    clipboard.write({
      text: plainText,
      html: htmlText,
    });
    desktop.cleanClipboard(true);
    assert.equal(clipboard.readHTML('clipboard'), expected);
  });

  it('cleanClipboard with HTML', () => {
    const htmlText =
      '<div class="body">\
    <div class="pm-content">\
    <h1 data-pm-pandoc-attr="1" class=" pm-heading">Summary</h1>\
    <p>Nullam augue</p>\
    </div></div>';
    const plainText = 'Summary\nNullam augue';
    const expected = process.platform === 'darwin' ? `<meta charset='utf-8'>${htmlText}` : htmlText;

    clipboard.write({
      text: plainText,
      html: htmlText,
    });
    desktop.cleanClipboard(false);
    assert.equal(clipboard.readHTML('clipboard'), expected);
  });
});
