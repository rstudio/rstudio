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
import { mkdtempSync, rmSync, symlinkSync, writeFileSync } from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';
import { restore, saveAndClear } from '../unit-utils';
import i18next from 'i18next';
import { initI18n } from '../../../src/main/i18n-manager';

import { FilePath } from '../../../src/core/file-path';
import { getenv, setenv } from '../../../src/core/environment';
import { kRStudioInitialProject } from '../../../src/core/r-user-data';

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

  // some messages asserted on come from the translations
  before(async () => {
    initI18n();
    await new Promise<void>((resolve) => {
      if (i18next.isInitialized) {
        resolve();
      } else {
        i18next.on('initialized', () => resolve());
      }
    });
  });

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
        collectPendingRVersion: () => null,
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

    it('keeps the project this instance was launched to open only when asked to', async () => {
      const saved = { [kRStudioInitialProject]: '' };
      saveAndClear(saved);
      try {
        const launcher = getNewLauncher();
        launcher.mainWindow = fakeMainWindow();
        stubLaunchSession(launcher);

        setenv(kRStudioInitialProject, '/home/user/analysis/analysis.Rproj');
        await launcher.launchNextSession(true, true);
        assert.equal(getenv(kRStudioInitialProject), '/home/user/analysis/analysis.Rproj');

        await launcher.launchNextSession(true);
        assert.equal(getenv(kRStudioInitialProject), '');
      } finally {
        restore(saved);
      }
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
          collectPendingRVersion: () => null,
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
    function launcherWithPendingR(
      pending: string,
      useDefault = false,
    ): {
      launcher: SessionLauncher;
      gwtCallback: EventEmitter;
      store: sinon.SinonStub;
      storeChosen: sinon.SinonStub;
    } {
      const launcher = getNewLauncher();
      launcher.rPath = '/opt/R/4.3.3/bin/R';
      launcher.mainWindow = {
        collectPendingRVersion: () => (pending ? { path: pending, useDefault } : null),
        clearErrorDisplayed: sinon.stub(),
        setErrorDisplayed: sinon.stub(),
      } as unknown as MainWindow;

      const gwtCallback = new EventEmitter();
      appState().gwtCallback = gwtCallback as unknown as GwtCallback;

      // going back keeps an R by its path; a switch keeps it as it was chosen
      const store = sinon.stub(DetectR, 'storeRChoice').resolves();
      const storeChosen = sinon.stub(DetectR, 'storeChosenR').resolves();
      sinon.stub(DetectR, 'detectREnvironmentAsync').resolves([{ envVars: {} } as never, null]);
      sinon.stub(DetectR, 'rExecutableProblem').returns(null);
      return { launcher, gwtCallback, store, storeChosen };
    }

    afterEach(() => {
      sinon.restore();
    });

    it('does nothing when no R version is pending', () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment');
      const { launcher, gwtCallback, store, storeChosen } = launcherWithPendingR('');

      launcher.applyPendingRVersion();
      gwtCallback.emit(GwtCallback.WORKBENCH_INITIALIZED);

      assert.isFalse(prepare.called);
      assert.isFalse(store.called);
      assert.isFalse(storeChosen.called);
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
      const { launcher, gwtCallback, storeChosen } = launcherWithPendingR('/opt/R/4.4.1/bin/R');

      launcher.applyPendingRVersion();
      assert.isFalse(storeChosen.called);

      gwtCallback.emit(GwtCallback.WORKBENCH_INITIALIZED);

      assert.isTrue(storeChosen.calledOnceWithExactly('/opt/R/4.4.1/bin/R', false));
    });

    it('stores a switch to the default R, chosen as such, to be followed', () => {
      sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, gwtCallback, storeChosen } = launcherWithPendingR('/opt/R/4.4.1/bin/R', true);

      launcher.applyPendingRVersion();
      gwtCallback.emit(GwtCallback.WORKBENCH_INITIALIZED);

      assert.isTrue(storeChosen.calledOnceWithExactly('/opt/R/4.4.1/bin/R', true));
    });

    it('keeps the current R when the pending one cannot be prepared', () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment').returns(new Error('no such R'));
      const { launcher, gwtCallback, storeChosen } = launcherWithPendingR('/opt/R/missing/bin/R');

      launcher.applyPendingRVersion();
      gwtCallback.emit(GwtCallback.WORKBENCH_INITIALIZED);

      assert.isTrue(prepare.calledOnce);
      assert.isFalse(storeChosen.called);
      assert.equal(launcher.rPath, '/opt/R/4.3.3/bin/R');
    });

    it('goes back to the R in use before a switch whose session failed, without storing either', async () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, gwtCallback, store, storeChosen } = launcherWithPendingR('/opt/R/4.4.1/bin/R');
      const relaunch = sinon.stub(launcher, 'launchNextSession').resolves(null);
      sinon.stub(RChoice, 'savedRChoice').returns(null);
      sinon.stub(DetectR, 'defaultR').resolves('/usr/local/bin/R');

      launcher.applyPendingRVersion();
      assert.equal(await launcher.restartWithPreviousR(), '');
      await new Promise((resolve) => setImmediate(resolve));

      assert.isTrue(prepare.secondCall.calledWithExactly('/opt/R/4.3.3/bin/R'));
      assert.isTrue(relaunch.calledOnceWithExactly(true, false));
      assert.equal(launcher.rPath, '/opt/R/4.3.3/bin/R');

      // RStudio follows the default R, which wasn't the one that failed
      assert.isFalse(store.called);

      // the session that now starts is not the one switched to
      gwtCallback.emit(GwtCallback.WORKBENCH_INITIALIZED);
      assert.isFalse(store.called);
      assert.isFalse(storeChosen.called);
    });

    it('keeps the R in use before when the default R chosen in Global Options failed', async () => {
      sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, store } = launcherWithPendingR('/opt/R/4.4.1/bin/R');
      sinon.stub(launcher, 'launchNextSession').resolves(null);
      sinon.stub(RChoice, 'savedRChoice').returns(null);
      sinon.stub(DetectR, 'defaultR').resolves('/opt/R/4.4.1/bin/R');

      launcher.applyPendingRVersion();
      assert.equal(await launcher.restartWithPreviousR(), '');
      await new Promise((resolve) => setImmediate(resolve));

      assert.isTrue(store.calledOnceWithExactly('/opt/R/4.3.3/bin/R'));
    });

    it('puts back the R in use before, when the R that failed was already kept', async () => {
      // Global Options keeps the R chosen there before offering the switch
      sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, store } = launcherWithPendingR('/opt/R/4.4.1/bin/R');
      sinon.stub(launcher, 'launchNextSession').resolves(null);
      const saved = sinon.stub(RChoice, 'savedRChoice').returns({ path: '/opt/R/4.4.1/bin/R', rstudioWhichR: '' });

      launcher.applyPendingRVersion();
      await launcher.restartWithPreviousR();

      assert.isTrue(store.calledOnceWithExactly('/opt/R/4.3.3/bin/R'));

      // a choice made since, of yet another R, is left alone
      store.resetHistory();
      saved.returns({ path: '/opt/R/4.5.0/bin/R', rstudioWhichR: '' });
      launcher.applyPendingRVersion();
      await launcher.restartWithPreviousR();
      assert.isFalse(store.called);
    });

    it('offers no way back once a session has started with the R switched to', async () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, gwtCallback } = launcherWithPendingR('/opt/R/4.4.1/bin/R');
      const relaunch = sinon.stub(launcher, 'launchNextSession').resolves(null);

      launcher.applyPendingRVersion();
      gwtCallback.emit(GwtCallback.WORKBENCH_INITIALIZED);

      assert.equal(await launcher.restartWithPreviousR(), '');
      assert.isTrue(prepare.calledOnce);
      assert.isFalse(relaunch.called);
    });

    it('reports when the R in use before cannot be prepared, and lets the user try again', async () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment');
      prepare.onFirstCall().returns(null);
      prepare.onSecondCall().returns(new Error('R is gone'));
      prepare.onThirdCall().returns(null);
      const { launcher, store } = launcherWithPendingR('/opt/R/4.4.1/bin/R');
      const relaunch = sinon.stub(launcher, 'launchNextSession').resolves(null);
      sinon.stub(RChoice, 'savedRChoice').returns(null);
      sinon.stub(DetectR, 'defaultR').resolves('/usr/local/bin/R');

      launcher.applyPendingRVersion();
      assert.include(await launcher.restartWithPreviousR(), 'R is gone');
      assert.isFalse(relaunch.called);
      assert.equal(launcher.rPath, '/opt/R/4.4.1/bin/R');

      assert.equal(await launcher.restartWithPreviousR(), '');
      assert.isTrue(relaunch.calledOnce);
      assert.equal(launcher.rPath, '/opt/R/4.3.3/bin/R');
      assert.isFalse(store.called);
    });

    it('reports when the session cannot be launched, and lets the user try again', async () => {
      sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, store } = launcherWithPendingR('/opt/R/4.4.1/bin/R');
      const relaunch = sinon.stub(launcher, 'launchNextSession');
      relaunch.onFirstCall().resolves(new Error('spawn failed'));
      relaunch.onSecondCall().resolves(null);
      sinon.stub(RChoice, 'savedRChoice').returns({ path: '/opt/R/4.4.1/bin/R', rstudioWhichR: '' });

      launcher.applyPendingRVersion();
      assert.include(await launcher.restartWithPreviousR(), 'spawn failed');
      assert.equal(launcher.rPath, '/opt/R/4.4.1/bin/R');
      assert.isTrue((launcher.mainWindow?.setErrorDisplayed as sinon.SinonStub).calledOnce);
      assert.isFalse(store.called);

      assert.equal(await launcher.restartWithPreviousR(), '');
      assert.equal(launcher.rPath, '/opt/R/4.3.3/bin/R');
      assert.isTrue(store.calledOnceWithExactly('/opt/R/4.3.3/bin/R'));
    });
  });

  describe('a first session started with the R chosen in RStudio', () => {
    // paths that name no R on the host, which could resolve to one another
    const kChosenR = '/nonexistent/chosen-r/bin/R';
    const kDefaultR = '/nonexistent/default-r/bin/R';

    function launcherWithStartupR(rPath: string, pending = '') {
      const launcher = getNewLauncher();
      launcher.mainWindow = {
        collectPendingRVersion: () => (pending ? { path: pending, useDefault: false } : null),
        clearErrorDisplayed: sinon.stub(),
        setErrorDisplayed: sinon.stub(),
      } as unknown as MainWindow;
      appState().gwtCallback = new EventEmitter() as unknown as GwtCallback;

      sinon.stub(RChoice, 'savedRChoice').returns({ path: kChosenR, rstudioWhichR: RChoice.launchRStudioWhichR() });
      launcher.setStartupR(rPath);

      return {
        launcher,
        prepare: sinon.stub(DetectR, 'prepareEnvironment').returns(null),
        relaunch: sinon.stub(launcher, 'launchNextSession').resolves(null),
        store: sinon.stub(DetectR, 'storeRChoice').resolves(),
        query: sinon.stub(DetectR, 'detectREnvironmentAsync').resolves([{ envVars: {} } as never, null]),
        check: sinon.stub(DetectR, 'rExecutableProblem').returns(null),
      };
    }

    afterEach(() => {
      sinon.restore();
    });

    it('goes back to the default R, which RStudio then follows', async () => {
      sinon.stub(DetectR, 'defaultR').resolves(kDefaultR);
      const { launcher, prepare, relaunch, store } = launcherWithStartupR(kChosenR);

      assert.equal(await launcher.restartWithPreviousR(), '');

      assert.isTrue(prepare.calledOnceWithExactly(kDefaultR));
      assert.isTrue(relaunch.calledOnceWithExactly(true, true));
      assert.equal(launcher.rPath, kDefaultR);
      assert.isTrue(store.calledOnceWithExactly(kDefaultR));

      // and only once
      assert.equal(await launcher.restartWithPreviousR(), '');
      assert.isTrue(relaunch.calledOnce);
    });

    it('offers nothing when the session runs an R other than the chosen one', async () => {
      const { launcher, prepare, relaunch } = launcherWithStartupR(kDefaultR);

      assert.equal(await launcher.restartWithPreviousR(), '');
      assert.isFalse(prepare.called);
      assert.isFalse(relaunch.called);
    });

    it('reports a default R that is gone or would not run as itself, without querying it', async () => {
      sinon.stub(DetectR, 'defaultR').resolves(kDefaultR);
      const { launcher, prepare, query, check } = launcherWithStartupR(kChosenR);
      check.returns(`${kDefaultR} does not exist`);

      assert.include(await launcher.restartWithPreviousR(), 'does not exist');
      assert.isFalse(query.called);
      assert.isFalse(prepare.called);
    });

    it('reports a default R that does not answer, without preparing it', async () => {
      sinon.stub(DetectR, 'defaultR').resolves(kDefaultR);
      const { launcher, prepare, relaunch, query } = launcherWithStartupR(kChosenR);
      query.resolves([{ envVars: {} } as never, new Error(`${kDefaultR} did not answer`)]);

      assert.include(await launcher.restartWithPreviousR(), 'did not answer');
      assert.isTrue(query.calledOnceWith(kDefaultR));
      assert.isAbove(query.firstCall.args[1] as number, 0, 'the query is bounded');
      assert.isFalse(prepare.called);
      assert.isFalse(relaunch.called);

      // the way back is still offered
      query.resolves([{ envVars: {} } as never, null]);
      assert.equal(await launcher.restartWithPreviousR(), '');
      assert.isTrue(prepare.calledOnceWithExactly(kDefaultR));
    });

    it('reports a default R without the shared library a session needs, without preparing it', async function () {
      if (process.platform === 'win32') {
        this.skip();
      }

      sinon.stub(DetectR, 'defaultR').resolves(kDefaultR);
      const { launcher, prepare, relaunch, query } = launcherWithStartupR(kChosenR);
      const home = mkdtempSync(join(tmpdir(), 'r-home-without-library-'));
      try {
        query.resolves([{ envVars: { R_HOME: home } } as never, null]);

        assert.include(await launcher.restartWithPreviousR(), process.platform === 'darwin' ? 'libR.dylib' : 'libR.so');
        assert.isFalse(prepare.called);
        assert.isFalse(relaunch.called);
      } finally {
        rmSync(home, { recursive: true, force: true });
      }
    });

    it('says so when the default R is a link to the R that failed', async function () {
      if (process.platform === 'win32') {
        this.skip();
      }

      const dir = mkdtempSync(join(tmpdir(), 'r-default-link-'));
      try {
        const failedR = join(dir, 'R-4.4.1');
        const defaultLink = join(dir, 'R');
        writeFileSync(failedR, '');
        symlinkSync(failedR, defaultLink);
        sinon.stub(DetectR, 'defaultR').resolves(defaultLink);
        sinon.stub(RChoice, 'savedRChoice').returns({ path: failedR, rstudioWhichR: RChoice.launchRStudioWhichR() });

        const launcher = getNewLauncher();
        launcher.mainWindow = { clearErrorDisplayed: sinon.stub() } as unknown as MainWindow;
        launcher.setStartupR(failedR);
        const prepare = sinon.stub(DetectR, 'prepareEnvironment').returns(null);
        sinon.stub(DetectR, 'rExecutableProblem').returns(null);
        const query = sinon.stub(DetectR, 'detectREnvironmentAsync').resolves([{ envVars: {} } as never, null]);

        assert.isNotEmpty(await launcher.restartWithPreviousR());
        assert.isFalse(query.called);
        assert.isFalse(prepare.called);
      } finally {
        rmSync(dir, { recursive: true, force: true });
      }
    });

    it('says so when the default R is the R that failed', async () => {
      sinon.stub(DetectR, 'defaultR').resolves(kChosenR);
      const { launcher, prepare, relaunch } = launcherWithStartupR(kChosenR);

      assert.isNotEmpty(await launcher.restartWithPreviousR());
      assert.isFalse(prepare.called);
      assert.isFalse(relaunch.called);
      assert.equal(launcher.rPath, kChosenR);
    });

    it('leaves the way back to a switch made since', async () => {
      const defaultLookup = sinon.stub(DetectR, 'defaultR').resolves(kDefaultR);
      const { launcher, prepare } = launcherWithStartupR(kChosenR, '/opt/R/4.5.0/bin/R');

      launcher.applyPendingRVersion();
      assert.equal(await launcher.restartWithPreviousR(), '');

      assert.isTrue(prepare.secondCall.calledWithExactly(kChosenR));
      assert.isFalse(defaultLookup.called);
    });
  });

  describe('showUnusableRChoice', () => {
    const kUnusable = { path: '/opt/R/external/bin/R', reason: 'it does not exist' };

    function answer(response: number) {
      sinon.stub(appState().modalTracker, 'trackElectronModalAsync').resolves({ response, checkboxChecked: false });
    }

    afterEach(() => {
      sinon.restore();
    });

    it('forgets the choice when the user goes with the default R', async () => {
      sinon.stub(RChoice, 'savedRChoice').returns({ path: kUnusable.path, rstudioWhichR: '' });
      const clear = sinon.stub(RChoice, 'clearRChoice');
      answer(0);

      await getNewLauncher().showUnusableRChoice(kUnusable);

      assert.isTrue(clear.calledOnce);
    });

    it('keeps the choice, to try again at the next launch, when the user asks to', async () => {
      sinon.stub(RChoice, 'savedRChoice').returns({ path: kUnusable.path, rstudioWhichR: '' });
      const clear = sinon.stub(RChoice, 'clearRChoice');
      answer(1);

      await getNewLauncher().showUnusableRChoice(kUnusable);

      assert.isFalse(clear.called);
    });

    it('leaves a choice made while the dialog was up', async () => {
      const saved = sinon.stub(RChoice, 'savedRChoice').returns({ path: kUnusable.path, rstudioWhichR: '' });
      const clear = sinon.stub(RChoice, 'clearRChoice');
      sinon.stub(appState().modalTracker, 'trackElectronModalAsync').callsFake(async () => {
        // the user chooses another R in Global Options, then answers
        saved.returns({ path: '/opt/R/4.5.0/bin/R', rstudioWhichR: '' });
        return { response: 0, checkboxChecked: false };
      });

      await getNewLauncher().showUnusableRChoice(kUnusable);

      assert.isFalse(clear.called);
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
