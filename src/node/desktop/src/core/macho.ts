/*
 * macho.ts
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

import { closeSync, openSync, readSync } from 'fs';

// Mach-O header constants (mach-o/loader.h, mach-o/fat.h, mach/machine.h)
const kMachoMagic32 = 0xfeedface;
const kMachoMagic64 = 0xfeedfacf;
const kFatMagic = 0xcafebabe;
const kFatMagic64 = 0xcafebabf;
const kFatArchSize = 20;
const kFatArch64Size = 32;

const kCpuTypeNames = new Map<number, string>([
  [0x00000007, 'i386'],
  [0x01000007, 'x86_64'],
  [0x0000000c, 'arm'],
  [0x0100000c, 'arm64'],
]);

function cpuTypeName(cpuType: number): string {
  return kCpuTypeNames.get(cpuType) ?? `cputype-${cpuType.toString(16)}`;
}

/**
 * Architectures contained in a Mach-O image, given its leading bytes. A thin
 * image yields one entry; a universal ("fat") image yields one per slice.
 * Returns an empty array for anything that is not Mach-O.
 */
export function machoArchitectures(header: Buffer): string[] {
  if (header.length < 8) {
    return [];
  }

  // thin images store the header in the host byte order, which on every
  // platform Apple ships is little-endian
  const magicLE = header.readUInt32LE(0);
  if (magicLE === kMachoMagic32 || magicLE === kMachoMagic64) {
    return [cpuTypeName(header.readUInt32LE(4))];
  }

  // fat headers are always big-endian
  const magicBE = header.readUInt32BE(0);
  if (magicBE === kFatMagic || magicBE === kFatMagic64) {
    const entrySize = magicBE === kFatMagic ? kFatArchSize : kFatArch64Size;
    const count = header.readUInt32BE(4);
    const architectures: string[] = [];
    for (let i = 0; i < count; i++) {
      const offset = 8 + i * entrySize;
      if (offset + 4 > header.length) {
        break;
      }
      architectures.push(cpuTypeName(header.readUInt32BE(offset)));
    }
    return architectures;
  }

  return [];
}

/**
 * Architectures of the Mach-O image at `path`, read from its header. This is
 * what `/usr/bin/file` reports, without spawning a process for it.
 */
export function readMachoArchitectures(path: string): string[] {
  // large enough for a fat header with any realistic number of slices
  const header = Buffer.alloc(4096);
  const fd = openSync(path, 'r');
  try {
    const bytes = readSync(fd, header, 0, header.length, 0);
    return machoArchitectures(header.subarray(0, bytes));
  } finally {
    closeSync(fd);
  }
}
