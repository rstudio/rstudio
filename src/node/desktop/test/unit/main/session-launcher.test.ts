/*
 * session-launcher.test.ts
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

import { describe } from 'mocha';
import { assert } from 'chai';
import sinon from 'sinon';
import { restore, saveAndClear } from '../unit-utils';

import { FilePath } from '../../../src/core/file-path';
import { getenv } from '../../../src/core/environment';

import { launchFailedDetail, SessionLauncher } from '../../../src/main/session-launcher';
import { ApplicationLaunch } from '../../../src/main/application-launch';
import { Application } from '../../../src/main/application';
import { appState, clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { MainWindow } from '../../../src/main/main-window';

function getNewLauncher(): SessionLauncher {
  return new SessionLauncher(new FilePath(), new FilePath(), new FilePath(), new ApplicationLaunch(), null);
}

describe('SessionLauncher', () => {
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
  describe('launchFailedDetail', () => {
    const issues = [{ directory: '/home/user/.local/share/rstudio/log', message: 'permission denied' }];

    it('prefers the abend log message when present', () => {
      assert.strictEqual(launchFailedDetail('session aborted', issues), 'session aborted');
    });
    it('reports state folder issues when there is no abend message', () => {
      const detail = launchFailedDetail(null, issues);
      assert.include(detail, '/home/user/.local/share/rstudio/log');
      assert.include(detail, 'permission denied');
    });
    it('falls back to a placeholder when there is nothing to report', () => {
      assert.strictEqual(launchFailedDetail(null, []), '[No error available]');
    });
  });
  it('buildLaunchContext sets RS_LOCAL_PEER on Win32', async () => {
    const launcher = getNewLauncher();
    await launcher.buildLaunchContext();
    const localPeer = getenv('RS_LOCAL_PEER');
    if (process.platform === 'win32') {
      assert.isNotEmpty(localPeer);
      assert.isAbove(localPeer.indexOf(appState().port.toString()), -1);
    } else {
      assert.isEmpty(localPeer);
    }
  });
  it('buildLaunchContext reuses same port', async () => {
    const launcher = getNewLauncher();
    const origPort = appState().port;
    await launcher.buildLaunchContext(true);
    const newPort = appState().port;
    assert.equal(origPort, newPort);
  });
  it('buildLaunchContext triggers new port number', async () => {
    const launcher = getNewLauncher();
    await launcher.buildLaunchContext(false);
    const newPort = appState().port;
    assert.isAbove(newPort, 0);
  });

  describe('launchNextSession', () => {
    // A minimal stand-in for MainWindow: launchNextSession only touches
    // setSessionProcess and (on reload) workbenchInitialized.
    function fakeMainWindow(): MainWindow {
      return {
        workbenchInitialized: true,
        setSessionProcess: () => {},
      } as unknown as MainWindow;
    }

    // Stub the private launchSession to throw so launchNextSession returns
    // right after the flag handling under test, without spawning a process.
    function stubLaunchSession(launcher: SessionLauncher): void {
      sinon
        .stub(launcher as unknown as { launchSession: () => unknown }, 'launchSession')
        .throws(new Error('no session process in unit tests'));
    }

    it('clears workbenchInitialized when reloading', async () => {
      const launcher = getNewLauncher();
      const mainWindow = fakeMainWindow();
      launcher.mainWindow = mainWindow;
      stubLaunchSession(launcher);

      const error = await launcher.launchNextSession(true);

      assert.isNotNull(error);
      assert.isFalse(mainWindow.workbenchInitialized);
    });

    it('preserves workbenchInitialized when not reloading', async () => {
      const launcher = getNewLauncher();
      const mainWindow = fakeMainWindow();
      launcher.mainWindow = mainWindow;
      stubLaunchSession(launcher);

      const error = await launcher.launchNextSession(false);

      assert.isNotNull(error);
      assert.isTrue(mainWindow.workbenchInitialized);
    });
  });
});
