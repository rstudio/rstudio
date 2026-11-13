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
import fs, { existsSync, mkdirSync, mkdtempSync, readdirSync, rmSync, writeFileSync } from 'fs';
import os from 'os';
import path from 'path';
import sinon from 'sinon';
import {
  isProcessRunning,
  kInstanceSessionDataDir,
  prepareInstanceSessionData,
  removeExitedInstanceSessionData,
} from '../../../src/main/instance-session-data';

describe('instance session data', () => {
  let userData: string;
  let root: string;

  beforeEach(() => {
    userData = mkdtempSync(path.join(os.tmpdir(), 'rstudio-instance-session-data-'));
    root = path.join(userData, kInstanceSessionDataDir);
  });

  afterEach(() => {
    sinon.restore();
    rmSync(userData, { recursive: true, force: true });
  });

  function leaveFolder(name: string): string {
    const dir = path.join(root, name);
    mkdirSync(dir, { recursive: true });
    writeFileSync(path.join(dir, 'Cookies'), 'x');
    return dir;
  }

  const running = (pids: number[]) => (pid: number) => pids.includes(pid);

  describe('prepareInstanceSessionData', () => {
    it('creates an empty folder named for the pid', () => {
      const dir = prepareInstanceSessionData(userData, 200);
      assert.strictEqual(dir, path.join(root, '200'));
      assert.deepEqual(readdirSync(dir!), []);
    });

    it('starts afresh where an exited instance with the same pid left its storage', () => {
      leaveFolder('200');
      const dir = prepareInstanceSessionData(userData, 200);
      assert.deepEqual(readdirSync(dir!), []);
    });

    it('gives up, keeping the shared storage, when the folder cannot be created', () => {
      // a file where the folders should go
      writeFileSync(root, 'x');
      assert.isUndefined(prepareInstanceSessionData(userData, 200));
    });
  });

  describe('removeExitedInstanceSessionData', () => {
    it('removes folders of instances that are no longer running', async () => {
      leaveFolder('300');
      leaveFolder('400');
      await removeExitedInstanceSessionData(userData, running([400]));
      assert.sameMembers(readdirSync(root), ['400']);
    });

    it('leaves entries it did not create alone', async () => {
      leaveFolder('notes');
      writeFileSync(path.join(root, '500'), 'a file, not a folder');
      await removeExitedInstanceSessionData(userData, running([]));
      assert.sameMembers(readdirSync(root), ['notes', '500']);
    });

    it('copes with no instance folders yet', async () => {
      await removeExitedInstanceSessionData(userData, running([]));
      assert.isFalse(existsSync(root));
    });

    it('copes with a folder list it cannot read', async () => {
      writeFileSync(root, 'x');
      await removeExitedInstanceSessionData(userData, running([]));
      assert.isTrue(existsSync(root));
    });

    it('carries on past a folder it cannot remove', async () => {
      const stuck = leaveFolder('300');
      leaveFolder('400');
      const rm = fs.promises.rm.bind(fs.promises);
      sinon.stub(fs.promises, 'rm').callsFake(async (target, options) => {
        if (target === stuck) {
          throw Object.assign(new Error('busy'), { code: 'EBUSY' });
        }
        return rm(target, options);
      });
      await removeExitedInstanceSessionData(userData, running([]));
      assert.sameMembers(readdirSync(root), ['300']);
    });
  });

  it('detects whether a process is running', () => {
    assert.isTrue(isProcessRunning(process.pid));

    // pids are positive and well below this on every platform
    assert.isFalse(isProcessRunning(2 ** 30));
  });
});
