/*
 * window-tracker.test.ts
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { describe } from 'mocha';
import { assert } from 'chai';

import { NullLogger, setLogger } from '../../../src/core/logger';

import { isWindowsDocker } from '../unit-utils';
import { WindowTracker } from '../../../src/main/window-tracker';
import { DesktopBrowserWindow } from '../../../src/main/desktop-browser-window';
import { GwtWindow } from '../../../src/main/gwt-window';
import { clearCoreSingleton } from '../../../src/core/core-state';
import { setTimeoutPromise } from '../../../src/core/wait-utils';

class TestGwtWindow extends GwtWindow {
  onActivated(): void {
    throw new Error('Method not implemented.');
  }
}

describe('WindowTracker', () => {
  beforeEach(() => {
    clearCoreSingleton();
    const f = new NullLogger();
    setLogger(f);
  });

  it('empty after creation', () => {
    assert.equal(new WindowTracker().length(), 0);
  });
  if (!isWindowsDocker()) {
    it('tracks and returns a window by name', () => {
      const tracker = new WindowTracker();
      const oneWin = new DesktopBrowserWindow({ name: 'some name', skipLocaleDetection: true });
      tracker.addWindow('one', oneWin);
      assert.equal(tracker.length(), 1);
      const result = tracker.getWindow('one');
      assert.isObject(result);
      assert.deepEqual(result, oneWin);
    });
    it('tracks and returns two windows by name', () => {
      const tracker = new WindowTracker();
      const oneWin = new DesktopBrowserWindow({ name: 'some name', skipLocaleDetection: true });
      tracker.addWindow('one', oneWin);
      const twoWin = new DesktopBrowserWindow({ name: 'another name', skipLocaleDetection: true });
      tracker.addWindow('two', twoWin);
      const twoResult = tracker.getWindow('two');
      const oneResult = tracker.getWindow('one');
      assert.isObject(oneResult);
      assert.deepEqual(oneResult, oneWin);
      assert.isObject(twoResult);
      assert.deepEqual(twoResult, twoWin);
    });
    it('duplicate name replaces the original', () => {
      const tracker = new WindowTracker();
      const oneWin = new DesktopBrowserWindow({ name: 'some name', skipLocaleDetection: true });
      tracker.addWindow('one', oneWin);
      const twoWin = new TestGwtWindow({ name: 'the gwt window', skipLocaleDetection: true });
      tracker.addWindow('one', twoWin);
      assert.equal(tracker.length(), 1);
      const result = tracker.getWindow('one');
      assert.deepEqual(result, twoWin);
    });
    it('delete window removes it from map', async function () {
      const tracker = new WindowTracker();
      const oneWin = new DesktopBrowserWindow({ name: 'some name', skipLocaleDetection: true });
      tracker.addWindow('one', oneWin);
      oneWin.window.close();
      await setTimeoutPromise(200); // TODO: yuck, find a better way to do this
      assert.equal(tracker.length(), 0);
    });

    it('delete window removes it from map', async function () {
      const tracker = new WindowTracker();
      const oneWin = new DesktopBrowserWindow({ name: 'some name', skipLocaleDetection: true });
      tracker.addWindow('one', oneWin);
      assert.equal(tracker.length(), 1);
      const result = tracker.getWindow('one');

      assert.isObject(result);
      assert.deepEqual(result, oneWin);

      tracker.onWindowDestroyed('one');

      assert.equal(tracker.length(), 0);
      assert.isUndefined(tracker.getWindow('one'));
    });
  }
});
