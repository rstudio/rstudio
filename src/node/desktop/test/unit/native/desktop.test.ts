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
import { clipboard, ClipboardItem } from 'electron';
import { describe } from 'mocha';

import os from 'os';

import desktop from '../../../src/native/desktop.node';

// Raw macOS pasteboard flavor holding UTF-16 text; Electron 44 reaches raw
// platform formats only through this custom MIME type.
const utf16Format = 'electron application/osclipboard;format="public.utf16-plain-text"';

async function readClipboardType(mimeType: string): Promise<Blob> {
  const items = await clipboard.read();
  const item = items.find((candidate) => candidate.types.includes(mimeType));
  assert.isDefined(item, `clipboard has no '${mimeType}' entry`);
  return item!.getType(mimeType) as Promise<Blob>;
}

async function readClipboardHtml(): Promise<string> {
  return (await readClipboardType('text/html')).text();
}

describe('Desktop Native Code', () => {
  it('cleanClipboard with plain text', async () => {
    await clipboard.writeText('write to clipboard');
    desktop.cleanClipboard(false);
    assert.equal(await clipboard.readText(), 'write to clipboard');
  });

  // Exercises the UTF-16 -> UTF-8 conversion with multibyte BMP characters
  // (cafe, CJK) and an astral character requiring a surrogate pair (emoji),
  // which the ASCII-only cases above do not cover.
  it('cleanClipboard preserves non-ASCII plain text', async () => {
    const text = 'café 世界 😀';
    await clipboard.writeText(text);
    desktop.cleanClipboard(false);
    assert.equal(await clipboard.readText(), text);
  });

  // Malformed UTF-16 that decodes to nothing must not wipe the clipboard:
  // conversion happens before the pasteboard is cleared, and cleanClipboard
  // bails out when it produces no text. macOS-only (utf16 pasteboard flavor).
  it('cleanClipboard leaves malformed UTF-16 data untouched', async function () {
    if (process.platform !== 'darwin') {
      this.skip();
    }
    const malformed = Buffer.from([0x00, 0xd8]); // lone high surrogate (U+D800)
    clipboard.clear();
    await clipboard.write([new ClipboardItem({ [utf16Format]: new Blob([malformed]) })]);
    desktop.cleanClipboard(false);
    const actual = Buffer.from(await (await readClipboardType(utf16Format)).arrayBuffer());
    assert.deepEqual([...actual], [...malformed]);
  });

  // Valid UTF-16 that begins with a NUL must still be cleaned rather than
  // mistaken for a decode failure: the NUL is preserved and the text converted.
  it('cleanClipboard handles valid UTF-16 with a leading NUL', async function () {
    if (process.platform !== 'darwin') {
      this.skip();
    }
    clipboard.clear();
    // UTF-16LE bytes for a NUL (U+0000) followed by 'A'
    const utf16 = Buffer.from([0x00, 0x00, 0x41, 0x00]);
    await clipboard.write([new ClipboardItem({ [utf16Format]: new Blob([utf16]) })]);
    desktop.cleanClipboard(false);
    assert.equal(await clipboard.readText(), '\u0000A');
  });

  // HTML stripping only available on Mac to handle pasteboard types
  it('cleanClipboard with strip HTML', async () => {
    const htmlText =
      '<div class="body">\
    <div class="pm-content">\
    <h1 data-pm-pandoc-attr="1" class=" pm-heading">Summary</h1>\
    <p>Nullam augue</p>\
    </div></div>';
    const plainText = `Summary${JSON.stringify(os.EOL)}Nullam augue`;

    await clipboard.write([new ClipboardItem({ 'text/plain': plainText, 'text/html': htmlText })]);
    desktop.cleanClipboard(true);
    if (process.platform === 'darwin') {
      assert.isFalse(await clipboard.has('text/html'));
      assert.equal(await clipboard.readText(), plainText);
    } else {
      assert.equal(await readClipboardHtml(), htmlText);
    }
  });

  it('cleanClipboard with HTML', async () => {
    const htmlText =
      '<div class="body">\
    <div class="pm-content">\
    <h1 data-pm-pandoc-attr="1" class=" pm-heading">Summary</h1>\
    <p>Nullam augue</p>\
    </div></div>';
    const plainText = 'Summary\nNullam augue';
    const expected = process.platform === 'darwin' ? `<meta charset='utf-8'>${htmlText}` : htmlText;

    await clipboard.write([new ClipboardItem({ 'text/plain': plainText, 'text/html': htmlText })]);
    desktop.cleanClipboard(false);
    assert.equal(await readClipboardHtml(), expected);
  });

  // The dialog watcher's observable behavior (raising rsession dialogs above
  // the RStudio window) needs a live rsession showing a Win32 dialog, so these
  // tests only exercise the exported API surface across its lifecycle.
  describe('win32 session dialog watcher', () => {
    it('watch followed by stop does not throw', () => {
      desktop.win32WatchSessionDialogs(process.pid);
      desktop.win32StopWatchingSessionDialogs();
    });

    it('watching again with an active hook does not throw', () => {
      desktop.win32WatchSessionDialogs(process.pid);
      desktop.win32WatchSessionDialogs(process.pid);
      desktop.win32StopWatchingSessionDialogs();
    });

    it('stop without a prior watch is a no-op', () => {
      desktop.win32StopWatchingSessionDialogs();
      desktop.win32StopWatchingSessionDialogs();
    });
  });
});
