/*
 * utils.test.ts
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
import { saveAndClear, restore } from '../unit-utils';
import { app, BrowserWindow, dialog } from 'electron';
import sinon from 'sinon';

import { FilePath } from '../../../src/core/file-path';
import { getenv, setenv, unsetenv } from '../../../src/core/environment';
import { clearCoreSingleton } from '../../../src/core/core-state';
import { NullLogger, setLogger } from '../../../src/core/logger';
import { appState, clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { Application } from '../../../src/main/application';

import * as Utils from '../../../src/main/utils';
import { userHomePath } from '../../../src/core/user';
import { FileFilter } from 'electron/main';
import desktop from '../../../src/native/desktop.node';

describe('Utils', () => {
  const envVars: Record<string, string> = {
    RSTUDIO_CPP_BUILD_OUTPUT: '',
  };

  beforeEach(() => {
    saveAndClear(envVars);
    clearCoreSingleton();
    const f = new NullLogger();
    setLogger(f);
  });

  afterEach(() => {
    restore(envVars);
  });

  it('userLogPath returns a non-empty string', () => {
    assert.isNotEmpty(Utils.userLogPath().getAbsolutePath());
  });
  it('usereWebCachePath returns a non-empty string', () => {
    assert.isNotEmpty(Utils.userWebCachePath().getAbsolutePath());
  });
  it('devicePixelRatio returns 1.0', () => {
    assert.strictEqual(Utils.devicePixelRatio(), 1.0);
  });
  it('randomString generates a random string', () => {
    const str1 = Utils.randomString();
    const str2 = Utils.randomString();
    const str3 = Utils.randomString();
    assert.notEqual(str1, str2);
    assert.notEqual(str1, str3);
    assert.notEqual(str2, str3);
  });
  it('getComponentVersions returns expected JSON', () => {
    const result = Utils.getComponentVersions();
    const json: Utils.VersionInfo = JSON.parse(result);
    assert.isNotEmpty(json.electron);
    assert.isNotEmpty(json.rstudio);
    assert.isNotEmpty(json.node);
    assert.isNotEmpty(json.v8);
  });
  it('augmentCommandLineArguments adds contents of env var', () => {
    assert.isFalse(app.commandLine.hasSwitch('disable-gpu'));
    assert.isEmpty(getenv('RSTUDIO_CHROMIUM_ARGUMENTS'));
    setenv('RSTUDIO_CHROMIUM_ARGUMENTS', '--disable-gpu --use-gl=desktop');
    Utils.augmentCommandLineArguments();
    assert.isTrue(app.commandLine.hasSwitch('disable-gpu'));
    assert.isTrue(app.commandLine.getSwitchValue('use-gl') == 'desktop');
    app.commandLine.removeSwitch('disable-gpu');
    app.commandLine.removeSwitch('use-gl');
    unsetenv('RSTUDIO_CHROMIUM_ARGUMENTS');
  });
  it('augmentCommandLineArguments leaves the Ozone platform alone', () => {
    // Electron on Linux sets this switch itself, so compare against what it started with
    const hadSwitch = app.commandLine.hasSwitch('ozone-platform');
    const value = app.commandLine.getSwitchValue('ozone-platform');
    setenv(
      'RSTUDIO_CHROMIUM_ARGUMENTS',
      '--ozone-platform=headless -ozone-platform=headless --OZONE-PLATFORM=headless --disable-gpu',
    );
    Utils.augmentCommandLineArguments();
    assert.strictEqual(app.commandLine.hasSwitch('ozone-platform'), hadSwitch);
    assert.strictEqual(app.commandLine.getSwitchValue('ozone-platform'), value);
    assert.isTrue(app.commandLine.hasSwitch('disable-gpu'));
    app.commandLine.removeSwitch('disable-gpu');
    unsetenv('RSTUDIO_CHROMIUM_ARGUMENTS');
  });
  it('initializeSharedSecret generates a random string in RS_SHARED_SECRET envvar_', () => {
    const envvar = 'RS_SHARED_SECRET';
    assert.equal(getenv(envvar).length, 0);
    Utils.initializeSharedSecret();
    assert.isAtLeast(getenv(envvar).length, 0);
  });
  it('rsessionExeName returns non-empty string', () => {
    assert.isNotEmpty(Utils.rsessionExeName());
  });
  it('removeStaleOptionsLockfile is callable', () => {
    Utils.removeStaleOptionsLockfile();
    assert(true);
  });
  it('findComponents returns session and scripts paths', () => {
    process.env.RSTUDIO_CPP_BUILD_OUTPUT = '/somewhere/interesting/';
    const [, , session, scripts] = Utils.findComponents();
    assert.isNotEmpty(session.getAbsolutePath());
    assert.isNotEmpty(scripts.getAbsolutePath());
  });
  it('getCurrentlyUniqueFolderName returns a unique foldername with prefix', () => {
    const folder1 = Utils.getCurrentlyUniqueFolderName('my-prefix-');
    assert.isFalse(folder1.existsSync()); // shouldn't create folder
    assert.isTrue(folder1.getAbsolutePath().indexOf('my-prefix-') >= 0); // contains prefix
  });
  it('getCurrentlyUniqueFolderName returns a unique foldername for each call', () => {
    const folder1 = Utils.getCurrentlyUniqueFolderName('my-prefix-');
    const folder2 = Utils.getCurrentlyUniqueFolderName('my-prefix-');
    assert.notEqual(folder1, folder2);
  });
  it('resolveAliasedPathSync replaces tilde with home', () => {
    const start = '~/foo/bar';
    const result = FilePath.resolveAliasedPathSync(start, userHomePath());
    const resultStr = result.getAbsolutePath();
    assert.isAtLeast(resultStr.length, start.length);
    assert.notEqual(resultStr.charAt(0), '~');
    assert.isAbove(resultStr.lastIndexOf('/foo/bar'), -1);
  });
  it('parseFilter converts file filter with one extension', () => {
    const input = 'R Projects (*.Rproj)';
    const expected: FileFilter[] = [{ name: 'R Projects', extensions: ['Rproj'] }];
    const result = Utils.parseFilter(input);
    assert.deepEqual(expected, result);
  });
  it('parseFilter converts file filter with multiple extensions', () => {
    const input = 'Theme Files (*.tmTheme *.rstheme)';
    const expected: FileFilter[] = [{ name: 'Theme Files', extensions: ['tmTheme', 'rstheme'] }];
    const result = Utils.parseFilter(input);
    assert.deepEqual(expected, result);
  });
  it('parseFilter converts file filter with file types', () => {
    const input = 'Images (*.png *.xpm *.jpg);;Text files (*.txt);;XML files (*.xml)';
    const expected: FileFilter[] = [
      { name: 'Images', extensions: ['png', 'xpm', 'jpg'] },
      { name: 'Text files', extensions: ['txt'] },
      { name: 'XML files', extensions: ['xml'] },
    ];
    const result = Utils.parseFilter(input);
    assert.deepEqual(expected, result);
  });
  it('parseFilter accepts a variety of delimiters between extensions', () => {
    const input = 'Images (*.png; *.xpm, *.jpg *.tiff)';
    const expected: FileFilter[] = [{ name: 'Images', extensions: ['png', 'xpm', 'jpg', 'tiff'] }];
    const result = Utils.parseFilter(input);
    assert.deepEqual(expected, result);
  });
  it('getNumericEnvVar returns number for numeric values', () => {
    const numValues = [0, 1, -1, 1.1];
    const envVarName = 'NUMERIC_ENV_VAR_VALUE';
    numValues.forEach((val, index) => {
      setenv(envVarName, `${val}`);
      const envVar = Utils.getNumericEnvVar(envVarName);
      assert.equal(envVar, numValues[index]);
    });
  });
  it('getNumericEnvVar returns undefined for non-numeric values', () => {
    const nonNumValues = [NaN, undefined, null, 'hi', '', 'H3LL0'];
    const envVarName = 'NON_NUMERIC_ENV_VAR_VALUE';
    nonNumValues.forEach((val) => {
      setenv(envVarName, `${val}`);
      const envVar = Utils.getNumericEnvVar(envVarName);
      assert.equal(envVar, undefined);
    });
  });
  it('removeTrailingSlashes removes trailing slashes from path strings', () => {
    const testCases = [
      { input: '', expected: '' },
      { input: '/', expected: '/' },
      { input: '\\', expected: '\\' },
      { input: '//', expected: '//' },
      { input: '\\\\', expected: '\\\\' },
      { input: '\\/\\', expected: '\\/\\' },
      { input: 'a', expected: 'a' },
      { input: '/a/b/c', expected: '/a/b/c' },
      { input: 'a/b/c/', expected: 'a/b/c' },
      { input: '/a/b/c//', expected: '/a/b/c' },
      { input: '/a/b/c\\\\', expected: '/a/b/c' },
      { input: '/a/b/c\\/', expected: '/a/b/c' },
      { input: 'C:\\', expected: 'C:' },
      { input: '~/', expected: '~' },
    ];
    testCases.forEach((testCase) => {
      const result = Utils.removeTrailingSlashes(testCase.input);
      assert.equal(result, testCase.expected);
    });
  });
  // Startup failures can happen before the Application singleton exists
  // (e.g. a corrupt config.json, #19496); the dialog must still appear.
  it('createStandaloneErrorDialog shows a dialog before the application is set', async () => {
    clearApplicationSingleton();
    const showMessageBox = sinon.stub(dialog, 'showMessageBox').resolves({ response: 0, checkboxChecked: false });
    const consoleError = sinon.stub(console, 'error');
    const window = new BrowserWindow({ show: false });
    try {
      await Utils.createStandaloneErrorDialog('title', 'message', { window, shouldCloseWindow: false });
      assert.isTrue(showMessageBox.calledOnce);
      assert.isTrue(consoleError.notCalled);
    } finally {
      showMessageBox.restore();
      consoleError.restore();
      window.destroy();
    }
  });

  it('createStandaloneErrorDialog tracks the dialog once the application is set', async () => {
    clearApplicationSingleton();
    setApplication(new Application());
    const track = sinon.spy(appState().modalTracker, 'trackElectronModalAsync');
    const showMessageBox = sinon.stub(dialog, 'showMessageBox').resolves({ response: 0, checkboxChecked: false });
    const consoleError = sinon.stub(console, 'error');
    const window = new BrowserWindow({ show: false });
    try {
      await Utils.createStandaloneErrorDialog('title', 'message', { window, shouldCloseWindow: false });
      assert.isTrue(track.calledOnce);
      assert.isTrue(showMessageBox.calledOnce);
      assert.isTrue(consoleError.notCalled);
    } finally {
      track.restore();
      showMessageBox.restore();
      consoleError.restore();
      window.destroy();
      clearApplicationSingleton();
    }
  });

  it('createStandaloneErrorDialog closes the window it was given when asked to', async () => {
    const showMessageBox = sinon.stub(dialog, 'showMessageBox').resolves({ response: 0, checkboxChecked: false });
    const consoleError = sinon.stub(console, 'error');
    const window = new BrowserWindow({ show: false });
    const close = sinon.stub(window, 'close');
    try {
      await Utils.createStandaloneErrorDialog('title', 'message', { window, shouldCloseWindow: true });
      assert.isTrue(close.calledOnce);
      assert.isTrue(consoleError.notCalled);
    } finally {
      showMessageBox.restore();
      consoleError.restore();
      window.destroy();
    }
  });

  describe('keepAutomationWindowsRendering', () => {
    // creates a real window after the call, and reports whether its page would
    // still be throttled once the window is covered
    function throttlesNewWindows(): boolean {
      const before = app.listeners('web-contents-created');
      Utils.keepAutomationWindowsRendering();

      const window = new BrowserWindow({ show: false });
      try {
        return window.webContents.getBackgroundThrottling();
      } finally {
        window.destroy();
        for (const listener of app.listeners('web-contents-created')) {
          if (!before.includes(listener)) {
            app.removeListener('web-contents-created', listener as (...args: unknown[]) => void);
          }
        }
      }
    }

    it('leaves background throttling on outside automation', () => {
      assert.isTrue(throttlesNewWindows());
    });

    it('turns background throttling off for windows created in automation mode', () => {
      app.commandLine.appendSwitch('automation-agent');
      try {
        assert.isFalse(throttlesNewWindows());
      } finally {
        app.commandLine.removeSwitch('automation-agent');
      }
    });
  });

  describe('showWindowForAutomation', () => {
    // as on macOS, showing a minimized window takes it out of the dock at once
    function fakeWindow(state: { visible: boolean; minimized: boolean }) {
      const isMinimized = sinon.stub().returns(state.minimized);
      return {
        isVisible: sinon.stub().returns(state.visible),
        isMinimized: isMinimized,
        showInactive: sinon.stub().callsFake(() => isMinimized.returns(false)),
        getNativeWindowHandle: sinon.stub().returns(Buffer.alloc(8)),
      };
    }

    afterEach(() => {
      sinon.restore();
    });

    // the user may have clicked the window to watch the run
    it('leaves a window that is already on screen alone', () => {
      const orderWindowBack = sinon.stub(desktop, 'macOSOrderWindowBack').returns(true);
      const window = fakeWindow({ visible: true, minimized: false });
      Utils.showWindowForAutomation(window as unknown as BrowserWindow);

      assert.isFalse(window.showInactive.called);
      assert.isFalse(orderWindowBack.called);
    });

    for (const state of [
      { name: 'hidden', visible: false, minimized: false },
      { name: 'minimized', visible: false, minimized: true },
    ]) {
      it(`shows a ${state.name} window, behind other applications on macOS`, () => {
        const orderWindowBack = sinon.stub(desktop, 'macOSOrderWindowBack').returns(true);
        const window = fakeWindow(state);
        Utils.showWindowForAutomation(window as unknown as BrowserWindow);

        assert.isTrue(window.showInactive.calledOnce);
        assert.strictEqual(orderWindowBack.callCount, process.platform === 'darwin' ? 1 : 0);
        if (process.platform === 'darwin') {
          assert.isTrue(orderWindowBack.calledAfter(window.showInactive));
        }
      });
    }

    // otherwise nothing explains why the windows are back in front
    it('logs a warning when the window could not be ordered back', function () {
      if (process.platform !== 'darwin') {
        this.skip();
      }

      const log = new NullLogger();
      const logWarning = sinon.stub(log, 'logWarning');
      setLogger(log);
      sinon.stub(desktop, 'macOSOrderWindowBack').returns(false);

      Utils.showWindowForAutomation(fakeWindow({ visible: false, minimized: false }) as unknown as BrowserWindow);
      assert.isTrue(logWarning.calledOnce);
    });
  });

  describe('raiseAndActivateWindow', () => {
    function fakeWindow(state: { visible: boolean; minimized: boolean }) {
      return {
        isVisible: sinon.stub().returns(state.visible),
        isMinimized: sinon.stub().returns(state.minimized),
        restore: sinon.stub(),
        show: sinon.stub(),
        showInactive: sinon.stub(),
        moveTop: sinon.stub(),
        focus: sinon.stub(),
        getNativeWindowHandle: sinon.stub().returns(Buffer.alloc(8)),
      };
    }

    function raiseInAutomation(window: ReturnType<typeof fakeWindow>): void {
      app.commandLine.appendSwitch('automation-agent');
      try {
        Utils.raiseAndActivateWindow(window as unknown as BrowserWindow);
      } finally {
        app.commandLine.removeSwitch('automation-agent');
      }
    }

    afterEach(() => {
      sinon.restore();
    });

    it('restores, raises and focuses the window', () => {
      const window = fakeWindow({ visible: false, minimized: true });
      Utils.raiseAndActivateWindow(window as unknown as BrowserWindow);

      assert.isTrue(window.restore.calledOnce);
      assert.isTrue(window.moveTop.calledOnce);
      assert.isTrue(window.focus.calledOnce);
      assert.isFalse(window.showInactive.called);
    });

    it('leaves a window that is already on screen alone in automation mode', () => {
      const orderWindowBack = sinon.stub(desktop, 'macOSOrderWindowBack').returns(true);
      const window = fakeWindow({ visible: true, minimized: false });
      raiseInAutomation(window);

      assert.isFalse(window.restore.called);
      assert.isFalse(window.show.called);
      assert.isFalse(window.showInactive.called);
      assert.isFalse(window.moveTop.called);
      assert.isFalse(window.focus.called);
      assert.isFalse(orderWindowBack.called);
    });

    // on macOS restore() would bring the window in front of other applications
    it('un-minimizes a window without raising it in automation mode', () => {
      const orderWindowBack = sinon.stub(desktop, 'macOSOrderWindowBack').returns(true);
      const window = fakeWindow({ visible: false, minimized: true });
      raiseInAutomation(window);

      assert.strictEqual(window.restore.callCount, process.platform === 'darwin' ? 0 : 1);
      assert.isTrue(window.showInactive.calledOnce);
      assert.strictEqual(orderWindowBack.callCount, process.platform === 'darwin' ? 1 : 0);
      assert.isFalse(window.moveTop.called);
      assert.isFalse(window.focus.called);
    });
  });
});
