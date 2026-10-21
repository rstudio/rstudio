/*
 * unit-utils.ts
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

import path from 'path';
import os from 'os';
import { mkdirSync, writeFileSync } from 'fs';

import { createStubInstance, StubbableType, SinonStubbedInstance, SinonStubbedMember, SinonSandbox } from 'sinon';
import { getenv } from '../../src/core/environment';
import { FilePath } from '../../src/core/file-path';
import { randomString } from '../../src/main/utils';

/**
 * save and clear specific env vars
 */
export function saveAndClear(vars: Record<string, string>): void {
  for (const name in vars) {
    vars[name] = process.env[name] ?? '';
    delete process.env[name];
  }
}

/**
 * put back original env vars
 */
export function restore(vars: Record<string, string>): void {
  for (const name in vars) {
    if (vars[name]) {
      process.env[name] = vars[name];
      vars[name] = '';
    } else {
      delete process.env[name];
    }
  }
}

// From: https://github.com/sinonjs/sinon/issues/1963
// Sinon can't stub members marked with TypeScript "private" or "protected"
export type StubbedClass<T> = SinonStubbedInstance<T> & T;

export function createSinonStubInstance<T>(
  constructor: StubbableType<T>,
  overrides?: { [K in keyof T]?: SinonStubbedMember<T[K]> },
): StubbedClass<T> {
  const stub = createStubInstance<T>(constructor, overrides);
  return stub as unknown as StubbedClass<T>;
}

export function createSinonStubInstanceForSandbox<T>(
  sandbox: SinonSandbox,
  constructor: StubbableType<T>,
  overrides?: { [K in keyof T]?: SinonStubbedMember<T[K]> },
): StubbedClass<T> {
  const stub = sandbox.createStubInstance<T>(constructor, overrides);
  return stub as unknown as StubbedClass<T>;
}

/**
 * Creates a random directory name located inside the temp directory
 *
 * @param label A label, if any, to include inside the random name. Useful to
 * identify the origin of any leftover directories from a unit test that weren't
 * cleaned up properly
 *
 * @returns The FilePath to the randomly generated directory
 */
export function tempDirectory(label = ''): FilePath {
  const tempName = label
    ? path.join(os.tmpdir(), label + '-' + randomString())
    : path.join(os.tmpdir(), randomString());
  return new FilePath(tempName);
}

/**
 * Smallest PE image header that reads as the given machine type
 */
export function fakePeImage(machine: number, peOffset = 0x80): Buffer {
  const buffer = Buffer.alloc(peOffset + 24);
  buffer.writeUInt16LE(0x5a4d, 0);
  buffer.writeUInt32LE(peOffset, 0x3c);
  buffer.writeUInt32LE(0x00004550, peOffset);
  buffer.writeUInt16LE(machine, peOffset + 4);
  return buffer;
}

/**
 * Lays out a Windows R installation under `rHome`: Rterm.exe and an R.dll of
 * the given machine type in bin and/or bin\x64
 */
export function fakeRInstallWin32(rHome: string, layout: { bin?: number; x64?: number }): void {
  for (const [subdir, machine] of [
    ['bin', layout.bin],
    [path.join('bin', 'x64'), layout.x64],
  ] as const) {
    if (machine !== undefined) {
      const dir = path.join(rHome, subdir);
      mkdirSync(dir, { recursive: true });
      writeFileSync(path.join(dir, 'Rterm.exe'), '');
      writeFileSync(path.join(dir, 'R.dll'), fakePeImage(machine));
    }
  }
}

/**
 * @returns true if tests are running in Docker on Windows
 */
export function isWindowsDocker(): boolean {
  return !!getenv('RSTUDIO_DOCKER_WINDOWS');
}
