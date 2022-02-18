/*
 * session-launcher.test.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
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
import { Application } from '../../../src/main/application';
import { appState, clearApplicationSingleton, setApplication } from '../../../src/main/app-state';

function getNewLauncher(): SessionLauncher {
  return new SessionLauncher(new FilePath(), new FilePath(), new FilePath(), new ApplicationLaunch());
}

describe('session-launcher', () => {
  const saveVars: Record<string, string> = {
    RS_LOCAL_PEER: '',
  };

  beforeEach(() => {
    setApplication(new Application());
    saveAndClear(saveVars);
  });

  afterEach(() => {
    clearApplicationSingleton();
    restore(saveVars);
  });

  it('generates and stores launcher token', () => {
    const token = SessionLauncher.launcherToken;
    assert.isNotEmpty(token);
    assert.strictEqual(SessionLauncher.launcherToken, token);
  });
  it('buildLaunchContext sets RS_LOCAL_PEER on Win32', () => {
    const launcher = getNewLauncher();
    launcher.buildLaunchContext();
    const localPeer = getenv('RS_LOCAL_PEER');
    if (process.platform === 'win32') {
      assert.isNotEmpty(localPeer);
      assert.isAbove(localPeer.indexOf(appState().port.toString()), -1);
    } else {
      assert.isEmpty(localPeer);
    }
  });
  it('buildLaunchContext reuses same port', () => {
    const launcher = getNewLauncher();
    const origPort = appState().port;
    launcher.buildLaunchContext(true);
    const newPort = appState().port;
    assert.equal(origPort, newPort);
  });
  it('buildLaunchContext triggers new port number', () => {
    const launcher = getNewLauncher();
    const origPort = appState().port;
    launcher.buildLaunchContext(false);
    const newPort = appState().port;
    assert.notEqual(origPort, newPort);
  });
});
