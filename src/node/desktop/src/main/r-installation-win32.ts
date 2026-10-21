/*
 * r-installation-win32.ts
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

import { existsSync } from 'fs';
import { join } from 'path';

import { logger } from '../core/logger';
import { readPeMachine } from '../core/pe';
import { kWindowsRExe } from '../ui/utils';

/**
 * Machine type of the R whose programs are in `binDir`, read from the R.dll
 * there ('arm64', 'x64', 'i386', ...). Undefined when it can't be read.
 */
export function rArchitectureWin32(binDir: string): string | undefined {
  const rDll = join(binDir, 'R.dll');
  try {
    const machine = readPeMachine(rDll);
    logger().logDebug(`R architecture from ${rDll}: ${machine ?? 'not a PE image'}`);
    return machine;
  } catch (error: unknown) {
    logger().logDebug(`Could not read R architecture from ${rDll}: ${String(error)}`);
    return undefined;
  }
}

/**
 * True when the 64-bit R at `rHome` should run from bin\Rterm.exe rather than
 * bin\x64\Rterm.exe: an ARM64 R has no architecture subfolder. When both exist,
 * bin\Rterm.exe wins only if its R.dll is ARM64.
 */
export function rTermInBinFolderWin32(rHome: string): boolean {
  if (!existsSync(join(rHome, 'bin', kWindowsRExe))) {
    return false;
  }
  if (!existsSync(join(rHome, 'bin', 'x64', kWindowsRExe))) {
    return true;
  }
  return rArchitectureWin32(join(rHome, 'bin')) === 'arm64';
}

/**
 * Rterm.exe of the 64-bit R at `rHome`: bin\x64\Rterm.exe, or bin\Rterm.exe
 * for an R with no architecture subfolder (see rTermInBinFolderWin32).
 */
export function rExecutablePathWin32(rHome: string): string {
  return rTermInBinFolderWin32(rHome) ? `${rHome}/bin/${kWindowsRExe}` : `${rHome}/bin/x64/${kWindowsRExe}`;
}
