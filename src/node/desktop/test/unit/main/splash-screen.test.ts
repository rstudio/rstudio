/*
 * splash-screen.test.ts
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
import sinon from 'sinon';
import { app, BrowserWindow } from 'electron';

import { StartupSplash } from '../../../src/main/splash-screen';

const kDelayMs = 500;
const kLingerMs = 1300;

function fakeSplashWindow() {
  return {
    show: sinon.stub(),
    showInactive: sinon.stub(),
    destroy: sinon.stub(),
    isDestroyed: sinon.stub().returns(false),
  };
}

describe('StartupSplash', () => {
  let clock: sinon.SinonFakeTimers;
  let window: ReturnType<typeof fakeSplashWindow>;
  let createWindow: sinon.SinonStub;
  let splash: StartupSplash;

  beforeEach(() => {
    clock = sinon.useFakeTimers();
    window = fakeSplashWindow();
    createWindow = sinon.stub().returns(window);
    splash = new StartupSplash(kDelayMs, kLingerMs, createWindow as unknown as () => BrowserWindow);
  });

  afterEach(() => {
    clock.restore();
  });

  it('appears once the delay has passed', () => {
    splash.schedule();
    clock.tick(kDelayMs - 1);
    assert.isFalse(createWindow.called);

    clock.tick(1);
    assert.isTrue(createWindow.calledOnce);
    assert.isTrue(window.show.calledOnce);
  });

  it('appears without taking focus in automation mode', () => {
    app.commandLine.appendSwitch('automation-agent');
    try {
      splash.schedule();
      clock.tick(kDelayMs);
    } finally {
      app.commandLine.removeSwitch('automation-agent');
    }

    assert.isTrue(window.showInactive.calledOnce);
    assert.isFalse(window.show.called);
  });

  it('is skipped when the main window is shown first', () => {
    splash.schedule();
    clock.tick(kDelayMs - 1);
    splash.onMainWindowShown();

    clock.tick(kDelayMs + kLingerMs);
    assert.isFalse(createWindow.called);
  });

  it('stays up for as long as there is no main window', () => {
    splash.schedule();
    clock.tick(kDelayMs + 60 * 1000);

    assert.isFalse(window.destroy.called);
  });

  it('lingers once the main window is shown, then closes', () => {
    splash.schedule();
    clock.tick(kDelayMs);
    splash.onMainWindowShown();

    clock.tick(kLingerMs - 1);
    assert.isFalse(window.destroy.called);

    clock.tick(1);
    assert.isTrue(window.destroy.calledOnce);
  });

  it('closes when asked, without waiting out the linger', () => {
    splash.schedule();
    clock.tick(kDelayMs);
    splash.onMainWindowShown();

    clock.tick(100);
    splash.close();
    assert.isTrue(window.destroy.calledOnce);

    // and the linger running out afterwards has nothing left to close
    clock.tick(kLingerMs);
    assert.isTrue(window.destroy.calledOnce);
  });

  it('never appears once closed', () => {
    splash.schedule();
    splash.close();

    clock.tick(kDelayMs);
    assert.isFalse(createWindow.called);
  });

  it('leaves a window that is already gone alone', () => {
    splash.schedule();
    clock.tick(kDelayMs);
    window.isDestroyed.returns(true);

    splash.close();
    assert.isFalse(window.destroy.called);
  });
});
