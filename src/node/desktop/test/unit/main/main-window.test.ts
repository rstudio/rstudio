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

import { MainWindow } from '../../../src/main/main-window';
import desktop from '../../../src/native/desktop.node';

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
});
