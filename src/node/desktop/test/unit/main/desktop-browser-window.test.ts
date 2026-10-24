/*
 * desktop-browser-window.test.ts
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

import { app, dialog, screen } from 'electron';

import { setVars } from '../../../src/core/environment';
import { clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { Application } from '../../../src/main/application';
import { DesktopBrowserWindow } from '../../../src/main/desktop-browser-window';
import { isWindowsDocker, restore, saveAndClear } from '../unit-utils';

if (!isWindowsDocker()) {
  describe('DesktopBrowserWindow', () => {
    const vars: Record<string, string> = {
      NODE_ENV: '',
    };
    const baseUrl = 'http://127.0.0.1:8080';

    before(() => {
      saveAndClear(vars);
      setVars({ NODE_ENV: 'TEST' });
    });

    after(() => {
      restore(vars);
    });

    it('construction creates a hidden BrowserWindow', () => {
      const win = new DesktopBrowserWindow({ name: '_blank', skipLocaleDetection: true });
      assert.isObject(win);
      assert.isObject(win.window);
      assert.isFalse(win.window.isVisible());
    });

    it('construction creates the BrowserWindow at the requested size', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        initialSize: { width: 1000, height: 600 },
      });
      assert.deepEqual(win.window.getSize(), [1000, 600]);
    });

    describe('unmaximize handling', () => {
      afterEach(() => {
        sinon.restore();
      });

      function emitUnmaximize(): sinon.SinonStub {
        const win = new DesktopBrowserWindow({ name: '_blank', skipLocaleDetection: true });
        const reposition = sinon.stub(win, 'positionAndEnsureVisibleDebounced');
        win.window.emit('unmaximize');
        return reposition;
      }

      it('guards against restoring offscreen', () => {
        sinon.stub(app.commandLine, 'getSwitchValue').returns('x11');
        assert.isTrue(emitUnmaximize().calledOnce);
      });

      it('leaves the restored bounds to the compositor on Wayland', () => {
        sinon.stub(process, 'platform').value('linux');
        sinon.stub(app.commandLine, 'getSwitchValue').withArgs('ozone-platform').returns('wayland');
        assert.isTrue(emitUnmaximize().notCalled);
      });
    });

    describe('collapsed maximized window recovery', () => {
      let clock: sinon.SinonFakeTimers;
      const workAreaSize = { width: 2560, height: 1408 };

      beforeEach(() => {
        clock = sinon.useFakeTimers();
      });

      afterEach(() => {
        clock.restore();
        sinon.restore();
      });

      function useWayland(): void {
        sinon.stub(process, 'platform').value('linux');
        sinon.stub(app.commandLine, 'getSwitchValue').withArgs('ozone-platform').returns('wayland');
      }

      // emit a resize with the window in the given state; returns the setSize
      // and screen.getAllDisplays stubs
      function emitResize(
        maximized: boolean,
        width: number,
        height: number,
        displays: number,
      ): { setSize: sinon.SinonStub; getAllDisplays: sinon.SinonStub } {
        const win = new DesktopBrowserWindow({ name: '_blank', skipLocaleDetection: true });
        sinon.stub(win.window, 'isMaximized').returns(maximized);
        sinon.stub(win.window, 'getBounds').returns({ x: 0, y: 0, width, height });
        const getAllDisplays = sinon
          .stub(screen, 'getAllDisplays')
          .returns(new Array(displays).fill(screen.getPrimaryDisplay()));
        sinon.stub(screen, 'getDisplayMatching').returns({ ...screen.getPrimaryDisplay(), workAreaSize });
        const setSize = sinon.stub(win.window, 'setSize');
        win.window.emit('resize');
        clock.tick(300);
        return { setSize, getAllDisplays };
      }

      it('resizes a maximized window collapsed to its frame to the work area', () => {
        useWayland();
        const { setSize } = emitResize(true, 44, 44, 1);
        assert.isTrue(setSize.calledOnceWithExactly(workAreaSize.width, workAreaSize.height));
      });

      it('waits for a display before resizing', () => {
        useWayland();
        const { setSize, getAllDisplays } = emitResize(true, 44, 44, 0);
        assert.isTrue(setSize.notCalled);

        getAllDisplays.returns([screen.getPrimaryDisplay()]);
        screen.emit('display-added');
        clock.tick(300);
        assert.isTrue(setSize.calledOnceWithExactly(workAreaSize.width, workAreaSize.height));
      });

      it('leaves a maximized window of a usable size alone', () => {
        useWayland();
        assert.isTrue(emitResize(true, 2560, 1408, 1).setSize.notCalled);
      });

      it('leaves an unmaximized small window alone', () => {
        useWayland();
        assert.isTrue(emitResize(false, 44, 44, 1).setSize.notCalled);
      });

      it('does nothing off Wayland', () => {
        sinon.stub(app.commandLine, 'getSwitchValue').returns('x11');
        assert.isTrue(emitResize(true, 44, 44, 1).setSize.notCalled);
      });
    });

    it('allows navigation to presentation url', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        baseUrl: baseUrl,
        allowExternalNavigate: false,
      });

      const presentationUrl = 'http://127.0.0.1:123';

      win.setPresentationUrl(presentationUrl);
      assert.isTrue(win.allowNavigation(presentationUrl));
    });

    it('allows navigation to tutorial url', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        baseUrl: baseUrl,
        allowExternalNavigate: false,
      });

      const tutorialUrl = 'http://127.0.0.1:123';

      win.setTutorialUrl(tutorialUrl);
      assert.isTrue(win.allowNavigation(tutorialUrl));
    });

    it('allows navigation to viewer url', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        baseUrl: baseUrl,
        allowExternalNavigate: false,
      });

      const viewerUrl = 'http://127.0.0.1:123';

      win.setViewerUrl(viewerUrl);
      assert.isTrue(win.allowNavigation(viewerUrl));
    });

    it('allows navigation to Shiny dialog url', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        baseUrl: baseUrl,
        allowExternalNavigate: false,
      });

      const shinyDialogUrl = 'http://127.0.0.1:123';

      win.setShinyDialogUrl(shinyDialogUrl);
      assert.isTrue(win.allowNavigation(shinyDialogUrl));
    });

    it('isSafeHost detects unsafe host that looks safe', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        baseUrl: baseUrl,
        allowExternalNavigate: false,
      });

      win.setViewerUrl('http://127.0.0.1:123');
      const unsafeUrl = 'http://www.example.com/127.0.0.1:123';

      assert.isFalse(win.allowNavigation(unsafeUrl));
    });

    describe('will-prevent-unload handling', () => {
      beforeEach(() => {
        setApplication(new Application());
      });

      afterEach(() => {
        clearApplicationSingleton();
        sinon.restore();
      });

      function emitWillPreventUnload(win: DesktopBrowserWindow): boolean {
        let prevented = false;
        const event = {
          preventDefault: () => {
            prevented = true;
          },
        };
        win.window.webContents.emit('will-prevent-unload', event);
        return prevented;
      }

      it('forces close when user chooses to leave', () => {
        const showMessageBox = sinon.stub(dialog, 'showMessageBoxSync').returns(0);
        const win = new DesktopBrowserWindow({ name: '_blank', skipLocaleDetection: true });

        assert.isTrue(emitWillPreventUnload(win));
        assert.isTrue(showMessageBox.calledOnce);
      });

      it('cancels close when user chooses to stay', () => {
        const showMessageBox = sinon.stub(dialog, 'showMessageBoxSync').returns(1);
        const win = new DesktopBrowserWindow({ name: '_blank', skipLocaleDetection: true });

        assert.isFalse(emitWillPreventUnload(win));
        assert.isTrue(showMessageBox.calledOnce);
      });

      it('forces close without prompting when RSTUDIO_DESKTOP_IGNORE_BEFOREUNLOAD is set', () => {
        const showMessageBox = sinon.stub(dialog, 'showMessageBoxSync');
        setVars({ RSTUDIO_DESKTOP_IGNORE_BEFOREUNLOAD: '1' });
        try {
          const win = new DesktopBrowserWindow({ name: '_blank', skipLocaleDetection: true });

          assert.isTrue(emitWillPreventUnload(win));
          assert.isTrue(showMessageBox.notCalled);
        } finally {
          setVars({ RSTUDIO_DESKTOP_IGNORE_BEFOREUNLOAD: '' });
        }
      });
    });

    it('set viewer URL checks for local URL', () => {
      const win = new DesktopBrowserWindow({
        name: '_blank',
        skipLocaleDetection: true,
        baseUrl:  baseUrl,
        allowExternalNavigate: false,
      });

      const unsafeUrl = 'http://www.example.com';
      win.setViewerUrl(unsafeUrl);
      win.setTutorialUrl(unsafeUrl);
      win.setPresentationUrl(unsafeUrl);
      win.setShinyDialogUrl(unsafeUrl);
      assert.isFalse(win.allowNavigation(unsafeUrl));
    });
  });
}
