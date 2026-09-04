/*
 * macho.test.ts
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
import { machoArchitectures, readMachoArchitectures } from '../../../src/core/macho';

function thin(cpuType: number): Buffer {
  const buffer = Buffer.alloc(32);
  buffer.writeUInt32LE(0xfeedfacf, 0);
  buffer.writeUInt32LE(cpuType, 4);
  return buffer;
}

function fat(cpuTypes: number[]): Buffer {
  const buffer = Buffer.alloc(8 + 20 * cpuTypes.length);
  buffer.writeUInt32BE(0xcafebabe, 0);
  buffer.writeUInt32BE(cpuTypes.length, 4);
  cpuTypes.forEach((cpuType, i) => buffer.writeUInt32BE(cpuType, 8 + 20 * i));
  return buffer;
}

describe('macho', () => {
  it('reads the architecture of a thin image', () => {
    assert.deepEqual(machoArchitectures(thin(0x0100000c)), ['arm64']);
    assert.deepEqual(machoArchitectures(thin(0x01000007)), ['x86_64']);
  });

  it('reads every slice of a universal image', () => {
    assert.deepEqual(machoArchitectures(fat([0x01000007, 0x0100000c])), ['x86_64', 'arm64']);
  });

  it('ignores non-Mach-O content', () => {
    assert.deepEqual(machoArchitectures(Buffer.from('#!/bin/sh\necho hi\n')), []);
    assert.deepEqual(machoArchitectures(Buffer.alloc(0)), []);
  });

  it('reads a real image on macOS', function () {
    if (process.platform !== 'darwin') {
      this.skip();
    }
    const architectures = readMachoArchitectures('/bin/ls');
    assert.isNotEmpty(architectures);
    assert.include(['arm64', 'x86_64'], architectures[architectures.length - 1]);
  });
});
