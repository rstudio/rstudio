/*
 * instance-session-data.test.ts
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
import { existsSync, mkdirSync, mkdtempSync, readdirSync, rmSync, writeFileSync } from 'fs';
import os from 'os';
import path from 'path';
import {
  instanceSessionDataPath,
  isProcessRunning,
  kInstanceSessionDataDir,
} from '../../../src/main/instance-session-data';

describe('instance session data', () => {
  let userData: string;
  let root: string;

  beforeEach(() => {
    userData = mkdtempSync(path.join(os.tmpdir(), 'rstudio-instance-session-data-'));
    root = path.join(userData, kInstanceSessionDataDir);
  });

  afterEach(() => {
    rmSync(userData, { recursive: true, force: true });
  });

  function leaveFolder(name: string): string {
    const dir = path.join(root, name);
    mkdirSync(dir, { recursive: true });
    writeFileSync(path.join(dir, 'Cookies'), 'x');
    return dir;
  }

  const running = (pids: number[]) => (pid: number) => pids.includes(pid);

  it('keeps the shared profile storage for the primary instance', () => {
    assert.isUndefined(instanceSessionDataPath(userData, true, 100, running([100])));
  });

  it('gives a secondary instance a folder named for its pid', () => {
    const dir = instanceSessionDataPath(userData, false, 200, running([100, 200]));
    assert.strictEqual(dir, path.join(root, '200'));
  });

  it('removes folders of instances that are no longer running', () => {
    leaveFolder('300');
    leaveFolder('400');
    instanceSessionDataPath(userData, true, 100, running([100, 400]));
    assert.sameMembers(readdirSync(root), ['400']);
  });

  it('cleans up from a secondary instance too, keeping its own folder', () => {
    leaveFolder('200');
    leaveFolder('300');
    instanceSessionDataPath(userData, false, 200, running([100, 200]));
    assert.sameMembers(readdirSync(root), ['200']);
  });

  it('leaves entries it did not create alone', () => {
    leaveFolder('notes');
    writeFileSync(path.join(root, '500'), 'a file, not a folder');
    instanceSessionDataPath(userData, true, 100, running([100]));
    assert.sameMembers(readdirSync(root), ['notes', '500']);
  });

  it('copes with no instance folders yet', () => {
    assert.isUndefined(instanceSessionDataPath(userData, true, 100, running([100])));
    assert.isFalse(existsSync(root));
  });

  it('detects whether a process is running', () => {
    assert.isTrue(isProcessRunning(process.pid));

    // pids are positive and well below this on every platform
    assert.isFalse(isProcessRunning(2 ** 30));
  });
});
