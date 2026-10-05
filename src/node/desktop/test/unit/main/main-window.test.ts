/*
 * main-window.test.ts
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

import { assert } from 'chai';
import { describe } from 'mocha';
import sinon from 'sinon';

import { ChildProcess } from 'child_process';

import { FilePath } from '../../../src/core/file-path';
import { NullLogger, setLogger } from '../../../src/core/logger';
import { MainWindow } from '../../../src/main/main-window';
import { clearOptionsSingleton, ElectronDesktopOptions } from '../../../src/main/preferences/electron-desktop-options';
import desktop from '../../../src/native/desktop.node';
import { tempDirectory } from '../unit-utils';

describe('MainWindow', () => {
  // MainWindow can't be instantiated in unit tests (GwtCallback needs a live
  // window), so setSessionProcess is invoked against a bare object instead.
  describe('setSessionProcess', () => {
    function setSessionProcess(sessionProcess?: { pid?: number }): void {
      MainWindow.prototype.setSessionProcess.call({} as MainWindow, sessionProcess as ChildProcess | undefined);
    }

    let watchStub: sinon.SinonStub;
    let stopStub: sinon.SinonStub;

    beforeEach(() => {
      watchStub = sinon.stub(desktop, 'win32WatchSessionDialogs');
      stopStub = sinon.stub(desktop, 'win32StopWatchingSessionDialogs');
    });

    afterEach(() => {
      sinon.restore();
    });

    it('watches the session pid (win32)', () => {
      setSessionProcess({ pid: 1234 });
      if (process.platform === 'win32') {
        assert.isTrue(watchStub.calledOnceWithExactly(1234));
      } else {
        assert.isTrue(watchStub.notCalled);
      }
      assert.isTrue(stopStub.notCalled);
    });

    it('stops watching when the session process is cleared (win32)', () => {
      setSessionProcess(undefined);
      assert.isTrue(watchStub.notCalled);
      if (process.platform === 'win32') {
        assert.isTrue(stopStub.calledOnce);
      } else {
        assert.isTrue(stopStub.notCalled);
      }
    });

    it('never watches a falsy pid (win32)', () => {
      // pid is undefined when spawn fails; a pid of 0 must never reach the
      // native watch, where it would mean "watch all processes"
      setSessionProcess({ pid: undefined });
      setSessionProcess({ pid: 0 });
      assert.isTrue(watchStub.notCalled);
      if (process.platform === 'win32') {
        assert.isTrue(stopStub.calledTwice);
      } else {
        assert.isTrue(stopStub.notCalled);
      }
    });
  });

  describe('closeEvent', () => {
    const configDirectory = tempDirectory('MainWindowTesting').toString();

    let nullLogger: NullLogger;
    let logSpy: sinon.SinonSpy;
    let saveWindowBounds: sinon.SinonStub;

    beforeEach(() => {
      nullLogger = new NullLogger();
      logSpy = sinon.spy(nullLogger, 'logErrorAtLevel');
      setLogger(nullLogger);

      // point the options singleton at a temp directory so the real user
      // config is never touched, then stub the write itself
      clearOptionsSingleton();
      saveWindowBounds = sinon.stub(ElectronDesktopOptions(configDirectory), 'saveWindowBounds');
    });

    afterEach(() => {
      sinon.restore();
      setLogger(new NullLogger());
      clearOptionsSingleton();
      new FilePath(configDirectory).removeIfExistsSync();
    });

    const windowStub = () => ({
      getNormalBounds: sinon.stub().returns({ x: 10, y: 20, width: 800, height: 600 }),
      isMaximized: sinon.stub().returns(false),
    });

    // simulates the close-during-crashed-renderer path from #18391: the
    // window is closing while a session is still attached, and the renderer
    // can no longer run the '!!window.desktopHooks' probe
    function closeEventWithRejectingRenderer(error: Error) {
      const fake = {
        quitConfirmed: false,
        sessionProcess: { exitCode: null },
        window: windowStub(),
        executeJavaScript: sinon.stub().rejects(error),
        quit: sinon.stub(),
      };
      const event = { preventDefault: sinon.stub() } as unknown as Electron.Event;
      MainWindow.prototype.closeEvent.call(fake as unknown as MainWindow, event);
      return fake;
    }

    it('logs the original error when the desktopHooks probe rejects', async () => {
      const boom = new Error('render frame was disposed');
      closeEventWithRejectingRenderer(boom);
      await new Promise(setImmediate);
      assert.isTrue(logSpy.calledWith('error', boom));
    });

    it('quits instead of leaving a headless process when the desktopHooks probe rejects', async () => {
      const fake = closeEventWithRejectingRenderer(new Error('render frame was disposed'));
      await new Promise(setImmediate);
      assert.isTrue(fake.quit.calledOnce);
    });

    // simulates the healthy path: the renderer answers the probe and quitR()
    // resolves as soon as the GWT save prompt is on screen, which is well
    // before the user has answered it (#18818). Nothing here distinguishes a
    // quit the user cancelled from one still waiting on an answer -- the main
    // process is not told either way -- so both are this same state.
    function closeEventWithLiveRenderer() {
      const fake = {
        quitConfirmed: false,
        sessionProcess: { exitCode: null },
        window: windowStub(),
        executeJavaScript: sinon.stub().resolves(true),
        quit: sinon.stub(),
      };
      const close = () => {
        const event = { preventDefault: sinon.stub() };
        MainWindow.prototype.closeEvent.call(fake as unknown as MainWindow, event as unknown as Electron.Event);
        return event;
      };
      return { fake, close };
    }

    it('does not confirm the quit just because quitR() was dispatched', async () => {
      const { fake, close } = closeEventWithLiveRenderer();
      close();
      await new Promise(setImmediate);
      assert.isTrue(fake.executeJavaScript.calledWith('window.desktopHooks.quitR()'));
      assert.isFalse(fake.quitConfirmed);
    });

    it('still intercepts the close after a cancelled quit', async () => {
      const { fake, close } = closeEventWithLiveRenderer();
      close();
      await new Promise(setImmediate);

      // the session is still running, so the next close must run the quit
      // sequence again rather than closing the window. Suppressing the
      // re-run would need a cancellation signal ApplicationQuit does not
      // send; without one, suppression would leave the close button dead.
      const second = close();
      await new Promise(setImmediate);
      assert.isTrue(second.preventDefault.calledOnce);
      assert.strictEqual(fake.executeJavaScript.withArgs('window.desktopHooks.quitR()').callCount, 2);
      assert.isTrue(fake.quit.notCalled);
    });

    it('saves the geometry of the window that is actually closing', () => {
      const fake = {
        quitConfirmed: true,
        sessionProcess: { exitCode: null },
        window: windowStub(),
        executeJavaScript: sinon.stub().resolves(true),
        quit: sinon.stub(),
      };
      const event = { preventDefault: sinon.stub() };
      MainWindow.prototype.closeEvent.call(fake as unknown as MainWindow, event as unknown as Electron.Event);

      assert.isTrue(event.preventDefault.notCalled);
      assert.isTrue(
        saveWindowBounds.calledOnceWithExactly({ x: 10, y: 20, width: 800, height: 600, maximized: false }),
      );
    });

    it('does not save geometry for a close the user may still cancel', async () => {
      // bounds recorded here would go stale the moment the user cancelled and
      // carried on moving or resizing the window (#18818)
      const { close } = closeEventWithLiveRenderer();
      close();
      await new Promise(setImmediate);
      assert.isTrue(saveWindowBounds.notCalled);
    });
  });
});
