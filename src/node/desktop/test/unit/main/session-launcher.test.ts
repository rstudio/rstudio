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
import { EventEmitter } from 'events';
import { mkdtempSync, writeFileSync } from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';
import { restore, saveAndClear } from '../unit-utils';

import { FilePath } from '../../../src/core/file-path';
import { getenv } from '../../../src/core/environment';

import { launchFailedDetail, SessionLauncher } from '../../../src/main/session-launcher';
import { ApplicationLaunch } from '../../../src/main/application-launch';
import { Application } from '../../../src/main/application';
import { appState, clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { MainWindow } from '../../../src/main/main-window';
import { GwtCallback } from '../../../src/main/gwt-callback';
import * as DetectR from '../../../src/main/detect-r';
import * as RChoice from '../../../src/main/r-choice';
import * as UrlUtils from '../../../src/main/url-utils';

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
    // setSessionProcess, the pending R version, and (on reload)
    // workbenchInitialized.
    function fakeMainWindow(): MainWindow {
      return {
        workbenchInitialized: true,
        setSessionProcess: () => {},
        collectPendingRVersion: () => '',
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

    // A reload waits for the new session's URL before loading it; by then
    // the session may have exited (leaving the launch error page up), or a
    // later launch (going back to the previous R) may have replaced it.
    describe('loading the page of a reloaded session', () => {
      interface FakeProcess {
        exitCode: number | null;
        signalCode: string | null;
      }

      interface Setup {
        launcher: SessionLauncher;
        loaded: string[];
        processes: FakeProcess[];
        answerWait: Array<() => void>;
      }

      function setup(): Setup {
        const launcher = getNewLauncher();
        const loaded: string[] = [];
        launcher.mainWindow = {
          workbenchInitialized: true,
          setSessionProcess: sinon.stub(),
          collectPendingRVersion: () => '',
          loadUrl: async (url: string) => {
            loaded.push(url);
          },
        } as unknown as MainWindow;

        const contexts = sinon.stub(launcher, 'buildLaunchContext');
        contexts.onFirstCall().resolves({ host: '', port: 1, url: 'http://failed/', argList: [] });
        contexts.onSecondCall().resolves({ host: '', port: 2, url: 'http://recovered/', argList: [] });

        const processes: FakeProcess[] = [
          { exitCode: null, signalCode: null },
          { exitCode: null, signalCode: null },
        ];
        const launch = sinon.stub(launcher as unknown as { launchSession: () => unknown }, 'launchSession');
        launch.onFirstCall().returns(processes[0]);
        launch.onSecondCall().returns(processes[1]);

        const answerWait: Array<() => void> = [];
        sinon
          .stub(UrlUtils, 'waitForUrlWithTimeout')
          .callsFake(async () => new Promise((resolve) => answerWait.push(() => resolve(null))));

        return { launcher, loaded, processes, answerWait };
      }

      async function settle(): Promise<void> {
        for (let i = 0; i < 5; i++) {
          await new Promise((resolve) => setImmediate(resolve));
        }
      }

      afterEach(() => {
        sinon.restore();
      });

      it('loads the page of the session that is running', async () => {
        const { launcher, loaded, answerWait } = setup();

        await launcher.launchNextSession(true);
        answerWait[0]();
        await settle();

        assert.deepEqual(loaded, ['http://failed/']);
      });

      it('keeps the launch error page when the session has exited', async () => {
        const { launcher, loaded, processes, answerWait } = setup();

        await launcher.launchNextSession(true);
        processes[0].exitCode = 1;
        answerWait[0]();
        await settle();

        assert.deepEqual(loaded, []);
      });

      it('keeps the launch error page when the session exits just before its page loads', async () => {
        const { launcher, loaded, processes, answerWait } = setup();

        // the load is deferred, and the session exits once it is scheduled
        const deferred: Array<() => void> = [];
        sinon.stub(global, 'setImmediate').callsFake(((callback: () => void) => {
          deferred.push(callback);
          return {} as NodeJS.Immediate;
        }) as unknown as typeof setImmediate);

        await launcher.launchNextSession(true);
        answerWait[0]();
        await new Promise((resolve) => setTimeout(resolve, 0));
        assert.lengthOf(deferred, 1);

        processes[0].exitCode = 1;
        deferred[0]();
        await new Promise((resolve) => setTimeout(resolve, 0));

        assert.deepEqual(loaded, []);
      });

      it('loads only the page of a later launch that replaced it', async () => {
        const { launcher, loaded, answerWait } = setup();

        await launcher.launchNextSession(true);
        await launcher.launchNextSession(true);
        answerWait[1]();
        await settle();
        answerWait[0]();
        await settle();

        assert.deepEqual(loaded, ['http://recovered/']);
      });
    });
  });

  describe('applyPendingRVersion', () => {
    // The R a restart asked for is prepared for the next session; the current
    // R stays in place when nothing is pending or the requested R fails.
    function launcherWithPendingR(pending: string): {
      launcher: SessionLauncher;
      gwtCallback: EventEmitter;
      store: sinon.SinonStub;
    } {
      const launcher = getNewLauncher();
      launcher.rPath = '/opt/R/4.3.3/bin/R';
      launcher.mainWindow = {
        collectPendingRVersion: () => pending,
        clearErrorDisplayed: sinon.stub(),
      } as unknown as MainWindow;

      const gwtCallback = new EventEmitter();
      appState().gwtCallback = gwtCallback as unknown as GwtCallback;

      const store = sinon.stub(DetectR, 'storeRChoice').resolves();
      return { launcher, gwtCallback, store };
    }

    afterEach(() => {
      sinon.restore();
    });

    it('does nothing when no R version is pending', () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment');
      const { launcher, gwtCallback, store } = launcherWithPendingR('');

      launcher.applyPendingRVersion();
      gwtCallback.emit(GwtCallback.WORKBENCH_INITIALIZED);

      assert.isFalse(prepare.called);
      assert.isFalse(store.called);
      assert.equal(launcher.rPath, '/opt/R/4.3.3/bin/R');
    });

    it('prepares the environment for the pending R', () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher } = launcherWithPendingR('/opt/R/4.4.1/bin/R');

      launcher.applyPendingRVersion();

      assert.isTrue(prepare.calledOnceWithExactly('/opt/R/4.4.1/bin/R'));
      assert.equal(launcher.rPath, '/opt/R/4.4.1/bin/R');
    });

    it('stores the switch as the chosen R once a session starts with it', () => {
      sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, gwtCallback, store } = launcherWithPendingR('/opt/R/4.4.1/bin/R');

      launcher.applyPendingRVersion();
      assert.isFalse(store.called);

      gwtCallback.emit(GwtCallback.WORKBENCH_INITIALIZED);

      assert.isTrue(store.calledOnceWithExactly('/opt/R/4.4.1/bin/R'));
    });

    it('keeps the current R when the pending one cannot be prepared', () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment').returns(new Error('no such R'));
      const { launcher, gwtCallback, store } = launcherWithPendingR('/opt/R/missing/bin/R');

      launcher.applyPendingRVersion();
      gwtCallback.emit(GwtCallback.WORKBENCH_INITIALIZED);

      assert.isTrue(prepare.calledOnce);
      assert.isFalse(store.called);
      assert.equal(launcher.rPath, '/opt/R/4.3.3/bin/R');
    });

    it('goes back to the R in use before a switch whose session failed, without storing either', async () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, gwtCallback, store } = launcherWithPendingR('/opt/R/4.4.1/bin/R');
      const relaunch = sinon.stub(launcher, 'launchNextSession').resolves(null);
      sinon.stub(RChoice, 'savedRChoice').returns(null);
      sinon.stub(DetectR, 'defaultR').resolves('/usr/local/bin/R');

      launcher.applyPendingRVersion();
      launcher.restartWithPreviousR();
      await new Promise((resolve) => setImmediate(resolve));

      assert.isTrue(prepare.secondCall.calledWithExactly('/opt/R/4.3.3/bin/R'));
      assert.isTrue(relaunch.calledOnceWithExactly(true));
      assert.equal(launcher.rPath, '/opt/R/4.3.3/bin/R');

      // RStudio follows the default R, which wasn't the one that failed
      assert.isFalse(store.called);

      // the session that now starts is not the one switched to
      gwtCallback.emit(GwtCallback.WORKBENCH_INITIALIZED);
      assert.isFalse(store.called);
    });

    it('keeps the R in use before when the default R chosen in Global Options failed', async () => {
      sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, store } = launcherWithPendingR('/opt/R/4.4.1/bin/R');
      sinon.stub(launcher, 'launchNextSession').resolves(null);
      sinon.stub(RChoice, 'savedRChoice').returns(null);
      sinon.stub(DetectR, 'defaultR').resolves('/opt/R/4.4.1/bin/R');

      launcher.applyPendingRVersion();
      launcher.restartWithPreviousR();
      await new Promise((resolve) => setImmediate(resolve));

      assert.isTrue(store.calledOnceWithExactly('/opt/R/4.3.3/bin/R'));
    });

    it('puts back the R in use before, when the R that failed was already kept', () => {
      // Global Options keeps the R chosen there before offering the switch
      sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, store } = launcherWithPendingR('/opt/R/4.4.1/bin/R');
      sinon.stub(launcher, 'launchNextSession').resolves(null);
      const saved = sinon.stub(RChoice, 'savedRChoice').returns({ path: '/opt/R/4.4.1/bin/R', rstudioWhichR: '' });

      launcher.applyPendingRVersion();
      launcher.restartWithPreviousR();

      assert.isTrue(store.calledOnceWithExactly('/opt/R/4.3.3/bin/R'));

      // a choice made since, of yet another R, is left alone
      store.resetHistory();
      saved.returns({ path: '/opt/R/4.5.0/bin/R', rstudioWhichR: '' });
      launcher.applyPendingRVersion();
      launcher.restartWithPreviousR();
      assert.isFalse(store.called);
    });

    it('offers no way back once a session has started with the R switched to', () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, gwtCallback } = launcherWithPendingR('/opt/R/4.4.1/bin/R');
      const relaunch = sinon.stub(launcher, 'launchNextSession').resolves(null);

      launcher.applyPendingRVersion();
      gwtCallback.emit(GwtCallback.WORKBENCH_INITIALIZED);
      launcher.restartWithPreviousR();

      assert.isTrue(prepare.calledOnce);
      assert.isFalse(relaunch.called);
    });
  });

  describe('sessionBinaryForR', () => {
    const vars: Record<string, string> = {
      R_RUNTIME: '',
      R_ARCH: '',
    };

    beforeEach(() => {
      saveAndClear(vars);
    });

    afterEach(() => {
      restore(vars);
    });

    it('picks the build for the R in use on every launch', function () {
      if (process.platform !== 'win32') {
        this.skip();
      }

      const dir = mkdtempSync(join(tmpdir(), 'rsession-'));
      writeFileSync(join(dir, 'rsession.exe'), '');
      writeFileSync(join(dir, 'rsession-utf8.exe'), '');

      const launcher = new SessionLauncher(
        new FilePath(join(dir, 'rsession.exe')),
        new FilePath(),
        new FilePath(),
        new ApplicationLaunch(),
        null,
      );

      // a UCRT R needs the UTF-8 build, and switching back to an older R
      // returns to the regular one
      process.env.R_RUNTIME = 'ucrt';
      assert.equal(launcher.sessionBinaryForR().getFilename(), 'rsession-utf8.exe');

      process.env.R_RUNTIME = '';
      assert.equal(launcher.sessionBinaryForR().getFilename(), 'rsession.exe');
    });
  });
});
