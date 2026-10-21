/*
 * r-installation-win32.test.ts
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
import { mkdtempSync, rmSync } from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';

import { readPeMachine } from '../../../src/core/pe';
import {
  isArm64MachineWin32,
  rArchitectureWin32,
  rExecutablePathWin32,
  rTermInBinFolderWin32,
} from '../../../src/main/r-installation-win32';
import { fakeRInstallWin32 } from '../unit-utils';

const kArm64 = 0xaa64;
const kX64 = 0x8664;

describe('r-installation-win32', () => {
  let rHome: string;

  beforeEach(() => {
    rHome = mkdtempSync(join(tmpdir(), 'r-installation-win32-test-'));
  });

  afterEach(() => {
    rmSync(rHome, { recursive: true, force: true });
  });

  it('uses bin\\x64 for an x64 R', () => {
    fakeRInstallWin32(rHome, { x64: kX64 });
    assert.isFalse(rTermInBinFolderWin32(rHome));
    assert.strictEqual(rExecutablePathWin32(rHome), `${rHome}/bin/x64/Rterm.exe`);
  });

  it('uses bin for an R with no architecture subfolder', () => {
    fakeRInstallWin32(rHome, { bin: kArm64 });
    assert.isTrue(rTermInBinFolderWin32(rHome));
    assert.strictEqual(rExecutablePathWin32(rHome), `${rHome}/bin/Rterm.exe`);
  });

  it('prefers bin when both exist and bin holds an ARM64 R', () => {
    fakeRInstallWin32(rHome, { bin: kArm64, x64: kX64 });
    assert.strictEqual(rExecutablePathWin32(rHome), `${rHome}/bin/Rterm.exe`);
  });

  it('prefers bin\\x64 when both exist and bin does not hold an ARM64 R', () => {
    fakeRInstallWin32(rHome, { bin: kX64, x64: kX64 });
    assert.strictEqual(rExecutablePathWin32(rHome), `${rHome}/bin/x64/Rterm.exe`);
  });

  it('falls back to bin\\x64 when neither exists', () => {
    assert.strictEqual(rExecutablePathWin32(rHome), `${rHome}/bin/x64/Rterm.exe`);
  });

  it('reads the architecture of the R.dll in the given folder only', () => {
    fakeRInstallWin32(rHome, { bin: kArm64, x64: kX64 });
    assert.strictEqual(rArchitectureWin32(join(rHome, 'bin')), 'arm64');
    assert.strictEqual(rArchitectureWin32(join(rHome, 'bin', 'x64')), 'x64');
  });

  it('reports no architecture when R.dll is missing', () => {
    assert.isUndefined(rArchitectureWin32(join(rHome, 'bin')));
  });

  it('tells an ARM64 machine from kernel32.dll on Windows, and is false elsewhere', () => {
    if (process.platform === 'win32') {
      const kernel32 = join(process.env.SystemRoot ?? 'C:\\Windows', 'System32', 'kernel32.dll');
      assert.strictEqual(isArm64MachineWin32(), readPeMachine(kernel32) === 'arm64');
    } else {
      assert.isFalse(isArm64MachineWin32());
    }
  });
});
