/*
 * satellite-window.test.ts
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
import { createSinonStubInstance, isWindowsDocker } from '../unit-utils';

import { BrowserWindow } from 'electron';

import { SatelliteWindow } from '../../../src/main/satellite-window';
import { MainWindow } from '../../../src/main/main-window';
import { GwtCallback } from '../../../src/main/gwt-callback';
import { appState, clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { Application } from '../../../src/main/application';

if (!isWindowsDocker()) {
  describe('SatelliteWindow', () => {
    beforeEach(() => {
      setApplication(new Application());
    });

    afterEach(async () => {
      // destroy any windows created by the test before clearing the
      // application singleton; SatelliteWindow's 'closed' handler calls
      // appState(), which throws once the singleton is gone
      BrowserWindow.getAllWindows().forEach((window) => window.destroy());
      await new Promise<void>((resolve) => setImmediate(resolve));

      clearApplicationSingleton();
      sinon.restore();
    });

    it('construction creates a hidden BrowserWindow', () => {
      const mainWindowStub = createSinonStubInstance(MainWindow);
      const browserWin = new BrowserWindow({ show: false });
      const win = new SatelliteWindow(mainWindowStub, 'satellite window', browserWin.webContents);
      assert.isObject(win, 'failed isObject test');
      assert.isObject(win.window, 'failed has window test');
      assert.isFalse(win.window.isVisible(), 'failed window not visible test');
    });

    it('unregisters from GwtCallback only once the window is destroyed', async () => {
      const mainWindowStub = createSinonStubInstance(MainWindow);
      const gwtCallbackStub = createSinonStubInstance(GwtCallback);
      appState().gwtCallback = gwtCallbackStub;

      const browserWin = new BrowserWindow({ show: false });
      const win = new SatelliteWindow(mainWindowStub, 'satellite window', browserWin.webContents);
      assert.isTrue(gwtCallbackStub.registerOwner.calledOnceWithExactly(win));

      // a close attempt by itself must not unregister; the close can still be
      // cancelled by the page's beforeunload handler (rstudio#17439)
      win.closeEvent({ preventDefault: sinon.stub() } as unknown as Electron.Event);
      assert.isTrue(gwtCallbackStub.unregisterOwner.notCalled);

      const closed = new Promise<void>((resolve) => {
        win.window.once('closed', () => setImmediate(resolve));
      });
      win.window.destroy();
      await closed;
      assert.isTrue(gwtCallbackStub.unregisterOwner.calledOnceWithExactly(win));
    });

    it('prunes the main window satellite bookkeeping once the window is destroyed', async () => {
      const mainWindowStub = createSinonStubInstance(MainWindow);
      mainWindowStub.window = new BrowserWindow({ show: false });
      mainWindowStub.executeJavaScript.resolves();

      const browserWin = new BrowserWindow({ show: false });
      const win = new SatelliteWindow(
        mainWindowStub,
        '_rstudio_satellite_shiny_foreground',
        browserWin.webContents,
      );

      // a close attempt by itself must not prune; the close can still be
      // cancelled by the page's beforeunload handler (rstudio#17439)
      win.closeEvent({ preventDefault: sinon.stub() } as unknown as Electron.Event);
      assert.isTrue(mainWindowStub.executeJavaScript.notCalled);

      const closed = new Promise<void>((resolve) => {
        win.window.once('closed', () => setImmediate(resolve));
      });
      win.window.destroy();
      await closed;

      assert.isTrue(mainWindowStub.executeJavaScript.calledOnce);
      const script = mainWindowStub.executeJavaScript.firstCall.args[0];
      assert.include(script, 'unregisterDesktopChildWindow');
      assert.include(script, '"_rstudio_satellite_shiny_foreground"');
    });

    it('does not touch the main window when it has already been destroyed', async () => {
      const mainWindowStub = createSinonStubInstance(MainWindow);
      mainWindowStub.window = new BrowserWindow({ show: false });

      const browserWin = new BrowserWindow({ show: false });
      const win = new SatelliteWindow(mainWindowStub, 'satellite window', browserWin.webContents);

      mainWindowStub.window.destroy();

      const closed = new Promise<void>((resolve) => {
        win.window.once('closed', () => setImmediate(resolve));
      });
      win.window.destroy();
      await closed;

      assert.isTrue(mainWindowStub.executeJavaScript.notCalled);
    });

    it('keeps a source window registered when the user cancels the close', async () => {
      const mainWindowStub = createSinonStubInstance(MainWindow);
      const gwtCallbackStub = createSinonStubInstance(GwtCallback);
      appState().gwtCallback = gwtCallbackStub;

      const browserWin = new BrowserWindow({ show: false });
      const win = new SatelliteWindow(
        mainWindowStub,
        '_rstudio_satellite_source_window_test',
        browserWin.webContents,
      );

      // simulate the page reporting it is not ready to close (e.g. the user
      // cancelled an unsaved-changes prompt); the window stays open, so it
      // must remain registered for desktop callbacks (rstudio#17439)
      const executeStub = sinon.stub(win, 'executeJavaScript').resolves(false);
      const preventDefault = sinon.stub();
      win.closeEvent({ preventDefault } as unknown as Electron.Event);
      await new Promise<void>((resolve) => setImmediate(resolve));

      assert.isTrue(preventDefault.calledOnce);
      assert.isTrue(executeStub.calledWith('window.rstudioCloseSourceWindow()'));
      assert.strictEqual(win.closeStage, 'CloseStageOpen');
      assert.isTrue(gwtCallbackStub.unregisterOwner.notCalled);

      const closed = new Promise<void>((resolve) => {
        win.window.once('closed', () => setImmediate(resolve));
      });
      win.window.destroy();
      await closed;
      assert.isTrue(gwtCallbackStub.unregisterOwner.calledOnceWithExactly(win));
    });
  });
}
