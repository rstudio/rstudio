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
import { EventEmitter } from 'events';
import { BrowserWindow } from 'electron';

import { NullLogger, setLogger } from '../../../src/core/logger';
import { MainWindow, trackWindowMaximizedState } from '../../../src/main/main-window';
import desktop from '../../../src/native/desktop.node';

describe('MainWindow', () => {
  describe('trackWindowMaximizedState', () => {
    function fakeWindow(maximized: boolean) {
      const window = new EventEmitter() as EventEmitter & { isMaximized: sinon.SinonStub };
      window.isMaximized = sinon.stub().returns(maximized);
      return window;
    }

    afterEach(() => {
      sinon.restore();
    });

    it('retains the maximized state while the window is minimized', () => {
      const window = fakeWindow(false);
      const wasMaximized = trackWindowMaximizedState(window as unknown as BrowserWindow);

      window.emit('maximize');
      window.isMaximized.returns(false);
      window.emit('minimize');

      assert.isTrue(wasMaximized());
    });

    it('refreshes the tracked state when the window is restored or unmaximized', () => {
      const window = fakeWindow(true);
      const wasMaximized = trackWindowMaximizedState(window as unknown as BrowserWindow);

      window.isMaximized.returns(false);
      window.emit('restore');
      assert.isFalse(wasMaximized());

      window.emit('maximize');
      assert.isTrue(wasMaximized());

      window.emit('unmaximize');
      assert.isFalse(wasMaximized());
    });
  });

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
    let nullLogger: NullLogger;
    let logSpy: sinon.SinonSpy;

    beforeEach(() => {
      nullLogger = new NullLogger();
      logSpy = sinon.spy(nullLogger, 'logErrorAtLevel');
      setLogger(nullLogger);
    });

    afterEach(() => {
      sinon.restore();
      setLogger(new NullLogger());
    });

    // simulates the close-during-crashed-renderer path from #18391: the
    // window is closing while a session is still attached, and the renderer
    // can no longer run the '!!window.desktopHooks' probe
    function closeEventWithRejectingRenderer(error: Error) {
      const fake = {
        geometrySaved: true,
        quitConfirmed: false,
        sessionProcess: { exitCode: null },
        window: {},
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
  });
});
