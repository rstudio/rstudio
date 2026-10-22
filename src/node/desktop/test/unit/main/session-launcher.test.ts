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
import { mkdirSync, mkdtempSync, writeFileSync } from 'fs';
import i18next from 'i18next';
import { tmpdir } from 'os';
import { join } from 'path';
import { fakeRInstallWin32, restore, saveAndClear } from '../unit-utils';

import { FilePath } from '../../../src/core/file-path';
import { getenv } from '../../../src/core/environment';

import { launchFailedDetail, SessionLauncher, withMissingArm64SessionNote } from '../../../src/main/session-launcher';
import { ApplicationLaunch } from '../../../src/main/application-launch';
import { Application } from '../../../src/main/application';
import { appState, clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { MainWindow } from '../../../src/main/main-window';
import { GwtCallback } from '../../../src/main/gwt-callback';
import * as DetectR from '../../../src/main/detect-r';
import * as DesktopOptions from '../../../src/main/preferences/electron-desktop-options';
import { DesktopOptionsImpl } from '../../../src/main/preferences/electron-desktop-options';

function getNewLauncher(): SessionLauncher {
  return new SessionLauncher(new FilePath(), new FilePath(), new FilePath(), new ApplicationLaunch(), null);
}

describe('SessionLauncher', () => {
  const saveVars: Record<string, string> = {
    RS_LOCAL_PEER: '',
  };

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
  describe('withMissingArm64SessionNote', () => {
    it('replaces the placeholder with the note', () => {
      const note = i18next.t('sessionLauncherTs.noArm64Session');
      assert.strictEqual(withMissingArm64SessionNote(launchFailedDetail(null, []), 0xc000007b), note);
    });
    it('puts the note above real error details', () => {
      const note = i18next.t('sessionLauncherTs.noArm64Session');
      assert.strictEqual(withMissingArm64SessionNote('session aborted', 0xc000007b), `${note}\n\nsession aborted`);
    });
    it('leaves the detail alone when the session failed for another reason', () => {
      assert.strictEqual(withMissingArm64SessionNote('session aborted', 3), 'session aborted');
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
        collectPendingRVersion: () => '',
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

    it('preserves workbenchInitialized when not reloading', async () => {
      const launcher = getNewLauncher();
      const mainWindow = fakeMainWindow();
      launcher.mainWindow = mainWindow;
      stubLaunchSession(launcher);

      const error = await launcher.launchNextSession(false);

      assert.isNotNull(error);
      assert.isTrue(mainWindow.workbenchInitialized);
    });
  });

  describe('applyPendingRVersion', () => {
    // The R a restart asked for is prepared for the next session; the current
    // R stays in place when nothing is pending or the requested R fails.
    function launcherWithPendingR(pending: string): {
      launcher: SessionLauncher;
      options: Record<string, sinon.SinonStub>;
    } {
      const launcher = getNewLauncher();
      launcher.mainWindow = {
        collectPendingRVersion: () => pending,
      } as unknown as MainWindow;

      const options = {
        setRExecutablePath: sinon.stub(),
        setUseDefault32BitR: sinon.stub(),
        setUseDefault64BitR: sinon.stub(),
      };
      sinon.stub(DesktopOptions, 'ElectronDesktopOptions').returns(options as unknown as DesktopOptionsImpl);

      return { launcher, options };
    }

    afterEach(() => {
      sinon.restore();
    });

    it('does nothing when no R version is pending', () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment');
      const { launcher, options } = launcherWithPendingR('');

      launcher.applyPendingRVersion();

      assert.isFalse(prepare.called);
      assert.isFalse(options.setRExecutablePath.called);
    });

    it('prepares the environment for the pending R', () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher } = launcherWithPendingR('/opt/R/4.4.1/bin/R');

      launcher.applyPendingRVersion();

      assert.isTrue(prepare.calledOnceWithExactly('/opt/R/4.4.1/bin/R'));
    });

    it('keeps the switch to this run of RStudio on macOS and Linux', function () {
      if (process.platform === 'win32') {
        this.skip();
      }

      sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, options } = launcherWithPendingR('/opt/R/4.4.1/bin/R');

      launcher.applyPendingRVersion();

      assert.isFalse(options.setRExecutablePath.called);
    });

    it('stores the switch as the chosen R on Windows once a session starts with it', function () {
      if (process.platform !== 'win32') {
        this.skip();
      }

      const gwtCallback = new EventEmitter();
      appState().gwtCallback = gwtCallback as unknown as GwtCallback;

      sinon.stub(DetectR, 'prepareEnvironment').returns(null);
      const { launcher, options } = launcherWithPendingR('C:/R/R-4.4.1/bin/x64/R.exe');

      launcher.applyPendingRVersion();
      assert.isFalse(options.setRExecutablePath.called);

      gwtCallback.emit(GwtCallback.WORKBENCH_INITIALIZED);

      // the default installations would otherwise win over the stored path
      assert.isTrue(options.setUseDefault32BitR.calledOnceWithExactly(false));
      assert.isTrue(options.setUseDefault64BitR.calledOnceWithExactly(false));
      assert.isTrue(options.setRExecutablePath.calledOnceWithExactly('C:/R/R-4.4.1/bin/x64/R.exe'));
    });

    it('keeps the current R when the pending one cannot be prepared', () => {
      const prepare = sinon.stub(DetectR, 'prepareEnvironment').returns(new Error('no such R'));
      const { launcher, options } = launcherWithPendingR('/opt/R/missing/bin/R');

      launcher.applyPendingRVersion();

      assert.isTrue(prepare.calledOnce);
      assert.isFalse(options.setRExecutablePath.called);
    });
  });

  describe('sessionBinaryForR', () => {
    const vars: Record<string, string> = {
      R_RUNTIME: '',
      R_ARCH: '',
      R_HOME: '',
    };

    // a session folder with the given subfolders, each holding both session builds
    function launcherWithSessions(...subdirs: string[]): SessionLauncher {
      const dir = mkdtempSync(join(tmpdir(), 'rsession-'));
      for (const subdir of ['', ...subdirs]) {
        mkdirSync(join(dir, subdir), { recursive: true });
        writeFileSync(join(dir, subdir, 'rsession.exe'), '');
        writeFileSync(join(dir, subdir, 'rsession-utf8.exe'), '');
      }
      return new SessionLauncher(
        new FilePath(join(dir, 'rsession.exe')),
        new FilePath(),
        new FilePath(),
        new ApplicationLaunch(),
        null,
      );
    }

    // a fake R of the given machine type: ARM64 in bin, anything else in bin\x64
    function useR(machine: number): void {
      const rHome = mkdtempSync(join(tmpdir(), 'r-home-'));
      fakeRInstallWin32(rHome, machine === 0xaa64 ? { bin: machine } : { x64: machine });
      process.env.R_HOME = rHome;
      process.env.R_ARCH = machine === 0xaa64 ? '' : 'x64';
      process.env.R_RUNTIME = 'ucrt';
    }

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

    it('picks the ARM64 build for an ARM64 R', function () {
      if (process.platform !== 'win32') {
        this.skip();
      }

      const launcher = launcherWithSessions('arm64');
      useR(0xaa64);
      const sessionPath = launcher.sessionBinaryForR();
      assert.equal(sessionPath.getFilename(), 'rsession-utf8.exe');
      assert.equal(sessionPath.getParent().getFilename(), 'arm64');
      assert.isFalse(launcher.missingArm64Session);
    });

    it('keeps the regular build for an ARM64 R when no ARM64 build is installed', function () {
      if (process.platform !== 'win32') {
        this.skip();
      }

      const launcher = launcherWithSessions();
      useR(0xaa64);
      const sessionPath = launcher.sessionBinaryForR();
      assert.equal(sessionPath.getFilename(), 'rsession-utf8.exe');
      assert.notEqual(sessionPath.getParent().getFilename(), 'arm64');
      assert.isTrue(launcher.missingArm64Session);
    });

    it('keeps the regular build for an x64 R even when an ARM64 build is installed', function () {
      if (process.platform !== 'win32') {
        this.skip();
      }

      const launcher = launcherWithSessions('arm64');
      useR(0x8664);
      const sessionPath = launcher.sessionBinaryForR();
      assert.equal(sessionPath.getFilename(), 'rsession-utf8.exe');
      assert.notEqual(sessionPath.getParent().getFilename(), 'arm64');
      assert.isFalse(launcher.missingArm64Session);
    });

    it('still picks the 32-bit build for a 32-bit R', function () {
      if (process.platform !== 'win32') {
        this.skip();
      }

      const launcher = launcherWithSessions('x86', 'arm64');
      process.env.R_ARCH = 'i386';
      const sessionPath = launcher.sessionBinaryForR();
      assert.equal(sessionPath.getFilename(), 'rsession.exe');
      assert.equal(sessionPath.getParent().getFilename(), 'x86');
    });
  });
});
