/*
 * application.test.ts
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

import fs from 'fs';
import path from 'path';
import os from 'os';

import { Menu } from 'electron';

import { createSinonStubInstance, restore, saveAndClear } from '../unit-utils';
import { Application, collectStateDirIssues } from '../../../src/main/application';
import { ApplicationLaunch } from '../../../src/main/application-launch';
import { clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { NullLogger, setLogger } from '../../../src/core/logger';
import { clearCoreSingleton } from '../../../src/core/core-state';
import { randomString } from '../../../src/main/utils';
import { FilePath } from '../../../src/core/file-path';
import { kRunDiagnosticsOption } from '../../../src/main/args-manager';
import { DesktopBrowserWindow } from '../../../src/main/desktop-browser-window';
import { MainWindow } from '../../../src/main/main-window';
import { PendingWindow } from '../../../src/main/pending-window';

describe('Application', () => {
  before(() => {
    setLogger(new NullLogger());
  });
  after(() => {
    clearCoreSingleton();
  });

  function testDir(): FilePath {
    return new FilePath(path.join(os.tmpdir(), 'temp-folder-for-Application-tests-' + randomString()));
  }

  describe('collectStateDirIssues', () => {
    // NOTE: non-writable scenarios are skipped on win32; creating a directory
    // that denies writes requires ACL manipulation that isn't practical here,
    // so the probe-file behavior is only exercised on POSIX platforms
    const stateEnv: Record<string, string> = {
      RSTUDIO_CONFIG_HOME: '',
      RSTUDIO_DATA_HOME: '',
      RS_LOG_DIR: '',
      RS_LOG_CONF_FILE: '',
    };
    let testRoot: string;

    beforeEach(() => {
      saveAndClear(stateEnv);
      testRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'rstudio-application-test-'));
      process.env.RSTUDIO_CONFIG_HOME = path.join(testRoot, 'config');
      process.env.RSTUDIO_DATA_HOME = path.join(testRoot, 'data');
      process.env.RS_LOG_DIR = path.join(testRoot, 'logs');
    });

    afterEach(() => {
      restore(stateEnv);
      for (const entry of fs.readdirSync(testRoot)) {
        try {
          fs.chmodSync(path.join(testRoot, entry), 0o700);
        } catch {
          // ignore; removal below is best-effort
        }
      }
      fs.rmSync(testRoot, { recursive: true, force: true });
    });

    it('returns no issues when all state dirs are writable', () => {
      assert.isEmpty(collectStateDirIssues());
    });

    it('reports a relocated, unwritable log directory', function () {
      if (process.platform === 'win32') {
        this.skip();
      }

      const logDir = path.join(testRoot, 'logs');
      fs.mkdirSync(logDir);
      fs.chmodSync(logDir, 0o500);

      const issues = collectStateDirIssues();
      assert.lengthOf(issues, 1);
      assert.strictEqual(issues[0].directory, logDir);
      assert.isNotEmpty(issues[0].message);
    });
  });

  describe('Dock menu', () => {
    beforeEach(() => {
      clearApplicationSingleton();
    });
    afterEach(() => {
      sinon.restore();
      clearApplicationSingleton();
    });

    it('new window opens with no project', function () {
      if (process.platform !== 'darwin') {
        this.skip();
      }

      const application = new Application();
      setApplication(application);

      const appLaunch = createSinonStubInstance(ApplicationLaunch);
      application.appLaunch = appLaunch;

      const buildFromTemplate = sinon.spy(Menu, 'buildFromTemplate');
      application.setDockMenu();

      // the menu item's click handler ignores the arguments Electron passes it
      const newWindowItem = buildFromTemplate.firstCall.args[0][0];
      (newWindowItem.click as unknown as () => void)();

      assert.isTrue(appLaunch.launchRStudio.calledOnceWithExactly({ noProject: true }));
    });
  });

  describe('Command line switches', () => {
    it('run-diagnostics sets diag mode and continues', () => {
      const app = new Application();
      assert.isFalse(app.runDiagnostics);
      const argv = [kRunDiagnosticsOption];
      const result = app.argsManager.initCommandLine(app, argv);
      assert.isFalse(result.exit);
      assert.isTrue(app.runDiagnostics);
    });
  });
  describe('Window creation', () => {
    // stands in for a tracked window; only the methods the tracker and
    // raiseAndActivateWindow() touch are provided
    function trackedWindow(): { tracked: DesktopBrowserWindow; focus: sinon.SinonStub } {
      const focus = sinon.stub();
      const window = {
        isMinimized: sinon.stub().returns(false),
        restore: sinon.stub(),
        moveTop: sinon.stub(),
        showInactive: sinon.stub(),
        focus: focus,
        addListener: sinon.stub(),
      };
      return { tracked: { window } as unknown as DesktopBrowserWindow, focus };
    }

    function pendingSatellite(name: string): PendingWindow {
      return {
        type: 'satellite',
        name: name,
        mainWindow: {} as MainWindow,
        screenX: -1,
        screenY: -1,
        width: 100,
        height: 100,
        allowExternalNavigate: false,
      };
    }

    function pendingSecondary(name: string): PendingWindow {
      return {
        type: 'secondary',
        name: name,
        allowExternalNavigate: false,
        showToolbar: true,
      };
    }

    it('denies a satellite whose name is already open and activates that window', () => {
      const app = new Application();
      const { tracked, focus } = trackedWindow();
      app.windowTracker.addWindow('rstudio_satellite_source', tracked);
      app.prepareForWindow(pendingSatellite('rstudio_satellite_source'));

      const result = app.windowOpening('rstudio_satellite_source');

      assert.equal(result.action, 'deny');
      assert.isTrue(focus.calledOnce);
      // a denied request never reaches windowCreated(), so the pending entry
      // must not be left behind for the next unrelated window.open()
      assert.equal(app.pendingWindows.length, 0);
    });

    it('denies a secondary window whose name is already open and activates that window', () => {
      const app = new Application();
      const { tracked, focus } = trackedWindow();
      app.windowTracker.addWindow('_rstudio_help', tracked);
      app.prepareForWindow(pendingSecondary('_rstudio_help'));

      const result = app.windowOpening('_rstudio_help');

      assert.equal(result.action, 'deny');
      assert.isTrue(focus.calledOnce);
      assert.equal(app.pendingWindows.length, 0);
    });

    it('allows a satellite with an unused name and leaves it queued for windowCreated()', () => {
      const app = new Application();
      app.windowTracker.addWindow('rstudio_satellite_other', trackedWindow().tracked);
      app.prepareForWindow(pendingSatellite('rstudio_satellite_source'));

      const result = app.windowOpening('rstudio_satellite_source');

      assert.equal(result.action, 'allow');
      assert.equal(app.pendingWindows.length, 1);
    });

    it('drops stale entries queued ahead of the requested window', () => {
      // Chromium reuses an already-open window of the same name without
      // consulting us, so the entry queued for it never gets consumed; it must
      // not deny (or configure) the next, unrelated window.open()
      const app = new Application();
      const { tracked, focus } = trackedWindow();
      app.windowTracker.addWindow('rstudio_satellite_source', tracked);
      app.prepareForWindow(pendingSatellite('rstudio_satellite_source'));
      app.prepareForWindow(pendingSecondary('_rstudio_help'));

      const result = app.windowOpening('_rstudio_help');

      assert.equal(result.action, 'allow');
      assert.isTrue(focus.notCalled);
      assert.deepEqual(
        app.pendingWindows.map((pending) => pending.name),
        ['_rstudio_help'],
      );
    });

    it('leaves the queue alone for a request with no pending entry', () => {
      const app = new Application();
      app.prepareForWindow(pendingSatellite('rstudio_satellite_source'));

      const result = app.windowOpening('_blank');

      assert.equal(result.action, 'allow');
      assert.equal(app.pendingWindows.length, 1);
    });
  });

  describe('Assorted helpers', () => {
    it('generates and stores port', () => {
      const app = new Application();
      const port = app.port;
      assert.isAbove(port, 0);
      assert.strictEqual(app.port, port);
    });
    it('generates new port', async () => {
      const app = new Application();
      await app.generateNewPort();
      assert.isAbove(app.port, 0);
    });
    it('returns default if scratch path not set or does not exist', () => {
      const app = new Application();
      const tmpDir = testDir();
      const result = app.scratchTempDir(tmpDir);
      assert.equal(result, tmpDir);
    });
    it("returns set scratch path plus 'tmp' if it exists", async () => {
      const app = new Application();
      const tmpDir = testDir();

      app.setScratchTempDir(tmpDir);
      assert.isFalse(await tmpDir.existsAsync());
      assert.isFalse(!!(await tmpDir.ensureDirectory()));

      const expectedDir = tmpDir.completeChildPath('tmp');
      assert.isFalse(await expectedDir.existsAsync());

      // note, every testDir call returns different random path
      const scratch = app.scratchTempDir(testDir());
      assert.equal(scratch.getAbsolutePath(), expectedDir.getAbsolutePath());

      await expectedDir.removeIfExists();
      await tmpDir.removeIfExists();
    });
  });
});
