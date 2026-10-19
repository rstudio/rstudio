/*
 * pe.ts
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

// PE header constants (Microsoft PE format: MS-DOS header, PE signature, COFF header)
const kDosMagic = 0x5a4d; // "MZ"
const kDosHeaderSize = 0x40;
const kPeOffsetField = 0x3c;
const kPeSignature = 0x00004550; // "PE\0\0"

const kMachineNames = new Map<number, string>([
  [0x014c, 'i386'],
  [0x8664, 'x64'],
  [0xaa64, 'arm64'],
  [0xa641, 'arm64ec'],
]);

function machineName(machine: number): string {
  return kMachineNames.get(machine) ?? `machine-${machine.toString(16)}`;
}

/**
 * Machine type of a PE image (a Windows executable or DLL), given its leading
 * bytes. Returns undefined for anything that is not a PE image.
 */
export function peMachine(header: Buffer): string | undefined {
  if (header.length < kDosHeaderSize || header.readUInt16LE(0) !== kDosMagic) {
    return undefined;
  }

  // the MS-DOS header points at the PE signature; the machine field follows it
  const peOffset = header.readUInt32LE(kPeOffsetField);
  if (peOffset + 6 > header.length || header.readUInt32LE(peOffset) !== kPeSignature) {
    return undefined;
  }

  return machineName(header.readUInt16LE(peOffset + 4));
}

/**
 * Machine type of the PE image at `path`, read from its header. A file that
 * is not a PE image yields undefined; a file that cannot be read throws.
 */
export function readPeMachine(path: string): string | undefined {
  // the PE signature follows the MS-DOS stub, well within the first block
  const header = Buffer.alloc(4096);
  const fd = openSync(path, 'r');
  try {
    const bytes = readSync(fd, header, 0, header.length, 0);
    return peMachine(header.subarray(0, bytes));
  } finally {
    closeSync(fd);
  }
}
