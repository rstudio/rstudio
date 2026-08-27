/*
 * gwt-callback.test.ts
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
import { BrowserWindow, ipcMain } from 'electron';
import { createSinonStubInstance, StubbedClass } from '../unit-utils';

import { GwtCallback } from '../../../src/main/gwt-callback';
import { MainWindow } from '../../../src/main/main-window';

function fakeBrowserWindow(state?: { visible: boolean; minimized: boolean }) {
  return {
    isVisible: sinon.stub().returns(state?.visible ?? true),
    isMinimized: sinon.stub().returns(state?.minimized ?? false),
    show: sinon.stub(),
    showInactive: sinon.stub(),
    restore: sinon.stub(),
    moveTop: sinon.stub(),
    focus: sinon.stub(),
  };
}

describe('DesktopCallback', () => {
  // GwtCallback registers its ipcMain handlers in the constructor, and a channel
  // can only be handled once per process, so the instance is shared by the suite
  let mainWindow: StubbedClass<MainWindow>;
  let callback: GwtCallback;

  before(() => {
    mainWindow = createSinonStubInstance(MainWindow);
    callback = new GwtCallback(mainWindow);
  });

  afterEach(() => {
    sinon.restore();
  });

  it('can be constructed', () => {
    assert.isNotEmpty(callback);
  });

  describe('desktop_bring_main_frame_behind_active', () => {
    // this callback means "make the main window as visible as possible, but leave
    // focus alone"; touching focus at all strands it on the main window under
    // window managers with focus-stealing prevention (#18635)
    function emit(main: ReturnType<typeof fakeBrowserWindow>, active: unknown) {
      mainWindow.window = main as unknown as BrowserWindow;
      sinon.stub(BrowserWindow, 'getFocusedWindow').returns(active as BrowserWindow | null);
      ipcMain.emit('desktop_bring_main_frame_behind_active');
    }

    it('restacks an already-visible main window beneath the active one', () => {
      const main = fakeBrowserWindow({ visible: true, minimized: false });
      const active = fakeBrowserWindow();
      emit(main, active);

      assert.isTrue(main.moveTop.calledOnce);
      assert.isTrue(active.moveTop.calledOnce);
      assert.isTrue(main.moveTop.calledBefore(active.moveTop));
      assert.isFalse(main.show.called);
      assert.isFalse(main.showInactive.called);
      assert.isFalse(main.restore.called);
    });

    it('surfaces a hidden main window without activating it', () => {
      const main = fakeBrowserWindow({ visible: false, minimized: false });
      emit(main, fakeBrowserWindow());

      assert.isTrue(main.showInactive.calledOnce);
      assert.isTrue(main.moveTop.calledOnce);
      assert.isFalse(main.show.called);
    });

    it('preserves the prior window state when restoring a minimized main window', () => {
      // In particular, calling showInactive() here on Windows would restore a
      // previously maximized window at its normal bounds.
      const main = fakeBrowserWindow({ visible: true, minimized: true });
      const active = fakeBrowserWindow();
      emit(main, active);

      assert.isTrue(main.restore.calledOnce);
      assert.isTrue(main.moveTop.calledOnce);
      assert.isFalse(main.show.called);
      assert.isFalse(main.showInactive.called);
      assert.isFalse(main.focus.called);
      assert.isTrue(active.focus.calledOnce);
      assert.isTrue(main.restore.calledBefore(main.moveTop));
      assert.isTrue(main.moveTop.calledBefore(active.moveTop));
      assert.isTrue(active.moveTop.calledBefore(active.focus));
    });

    it('never focuses either window unless the main window was minimized', () => {
      for (const state of [
        { visible: true, minimized: false },
        { visible: false, minimized: false },
      ]) {
        const main = fakeBrowserWindow(state);
        const active = fakeBrowserWindow();
        emit(main, active);

        assert.isFalse(main.focus.called, `main focused for ${JSON.stringify(state)}`);
        assert.isFalse(active.focus.called, `active focused for ${JSON.stringify(state)}`);
        sinon.restore();
      }
    });

    it('does nothing when the main window is itself the active window', () => {
      const main = fakeBrowserWindow({ visible: false, minimized: true });
      emit(main, main);

      assert.isFalse(main.showInactive.called);
      assert.isFalse(main.restore.called);
      assert.isFalse(main.moveTop.called);
    });

    it('does nothing when no window is focused', () => {
      const main = fakeBrowserWindow({ visible: false, minimized: true });
      emit(main, null);

      assert.isFalse(main.showInactive.called);
      assert.isFalse(main.restore.called);
      assert.isFalse(main.moveTop.called);
    });
  });
});
