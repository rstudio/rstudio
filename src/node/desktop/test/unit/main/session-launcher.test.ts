/*
 * session-launcher.test.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { describe } from 'mocha';
import { assert } from 'chai';
import { restore, saveAndClear } from '../unit-utils';

import { FilePath } from '../../../src/core/file-path';
import { getenv } from '../../../src/core/environment';

import { SessionLauncher } from '../../../src/main/session-launcher';
import { ApplicationLaunch } from '../../../src/main/application-launch';

function getNewLauncher(): SessionLauncher {
  return new SessionLauncher(new FilePath(), new FilePath(), new FilePath(), new ApplicationLaunch());
}

describe('session-launcher', () => {
  const saveVars: Record<string, string> = {
    RS_LOCAL_PEER: ''
  };

  beforeEach(() => {
    saveAndClear(saveVars);
  });

  afterEach(() => {
    restore(saveVars);
  });

  it('generates and stores launcher token', () => {
    const launcher = getNewLauncher();
    const token = launcher.getLauncherToken();
    assert.isNotEmpty(token);
    assert.strictEqual(launcher.getLauncherToken(), token);
  });
  it('generates and stores port', () => {
    const launcher = getNewLauncher();
    const port = launcher.getPort();
    assert.isNotEmpty(port);
    assert.strictEqual(launcher.getPort(), port);
  });
  it('generates new port', () => {
    const launcher = getNewLauncher();
    const origPort = launcher.getPort();
    const newPort = launcher.newPortNumber();
    assert.notStrictEqual(origPort, newPort);
  });
  it('sets RS_LOCAL_PEER on Win32', () => {
    const launcher = getNewLauncher();
    const port = launcher.getPort();
    const localPeer = getenv('RS_LOCAL_PEER');
    if (process.platform === 'win32') {
      assert.isNotEmpty(localPeer);
      assert.isAbove(-1, localPeer.indexOf(port));
    } else {
      assert.isEmpty(localPeer);
    }
  });
});
