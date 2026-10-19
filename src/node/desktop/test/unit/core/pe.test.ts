/*
 * pe.test.ts
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
import { join } from 'path';
import { peMachine, readPeMachine } from '../../../src/core/pe';

function pe(machine: number, peOffset = 0x80): Buffer {
  const buffer = Buffer.alloc(peOffset + 24);
  buffer.writeUInt16LE(0x5a4d, 0);
  buffer.writeUInt32LE(peOffset, 0x3c);
  buffer.writeUInt32LE(0x00004550, peOffset);
  buffer.writeUInt16LE(machine, peOffset + 4);
  return buffer;
}

describe('pe', () => {
  it('reads the machine type of an image', () => {
    assert.strictEqual(peMachine(pe(0x8664)), 'x64');
    assert.strictEqual(peMachine(pe(0xaa64)), 'arm64');
    assert.strictEqual(peMachine(pe(0x014c)), 'i386');
    assert.strictEqual(peMachine(pe(0xa641)), 'arm64ec');
  });

  it('names an unknown machine type by its value', () => {
    assert.strictEqual(peMachine(pe(0x01c4)), 'machine-1c4');
  });

  it('ignores non-PE content', () => {
    assert.isUndefined(peMachine(Buffer.from('#!/bin/sh\necho hi\n')));
    assert.isUndefined(peMachine(Buffer.alloc(0)));
    const macho = Buffer.alloc(64);
    macho.writeUInt32LE(0xfeedfacf, 0);
    assert.isUndefined(peMachine(macho));
  });

  it('ignores a truncated image', () => {
    assert.isUndefined(peMachine(pe(0x8664).subarray(0, 0x20)));
    assert.isUndefined(peMachine(pe(0x8664).subarray(0, 0x80 + 2)));
  });

  it('ignores a header that points at the wrong place', () => {
    const pastEnd = pe(0x8664);
    pastEnd.writeUInt32LE(0x10000, 0x3c);
    assert.isUndefined(peMachine(pastEnd));
    const noSignature = pe(0x8664);
    noSignature.writeUInt32LE(0x40, 0x3c);
    assert.isUndefined(peMachine(noSignature));
  });

  it('throws for a file that cannot be read', () => {
    assert.throws(() => readPeMachine(join(__dirname, 'no-such-file.dll')));
  });

  it('reads a real image on Windows', function () {
    if (process.platform !== 'win32') {
      this.skip();
    }
    const kernel32 = join(process.env.SystemRoot ?? 'C:\\Windows', 'System32', 'kernel32.dll');
    assert.include(['x64', 'arm64'], readPeMachine(kernel32));
  });
});
