/*
 * application-launch.test.ts
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
import os from 'os';
import path from 'path';
import childProcess, { ChildProcess } from 'child_process';
import { app } from 'electron';

import { getenv, setenv } from '../../../src/core/environment';
import { kRStudioInitialProject, kRStudioInitialWorkingDir } from '../../../src/core/r-user-data';
import { ApplicationLaunch, resolveProjectFile } from '../../../src/main/application-launch';
import { MainWindow } from '../../../src/main/main-window';
import { createSinonStubInstance, restore, saveAndClear } from '../unit-utils';

describe('ApplicationLaunch', () => {
  const tempDirs: string[] = [];
  const launchEnvVars: Record<string, string> = {
    [kRStudioInitialProject]: '',
    [kRStudioInitialWorkingDir]: '',
  };

  beforeEach(() => {
    saveAndClear(launchEnvVars);
  });

  afterEach(() => {
    restore(launchEnvVars);
    sinon.restore();
    while (tempDirs.length) {
      fs.rmSync(tempDirs.pop() as string, { recursive: true, force: true });
    }
  });

  function projectDir(): string {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'rstudio-application-launch-test-'));
    tempDirs.push(dir);
    fs.writeFileSync(path.join(dir, 'test.Rproj'), '');
    return dir;
  }

  // the relaunched instance picks up its initial project and working directory
  // from the environment, which is only set for the duration of the spawn call
  function captureLaunchEnv(): { project: string; workingDir: string } {
    const launchEnv = { project: '', workingDir: '' };
    sinon.stub(childProcess, 'spawn').callsFake(() => {
      launchEnv.project = getenv(kRStudioInitialProject);
      launchEnv.workingDir = getenv(kRStudioInitialWorkingDir);
      return { unref: sinon.stub() } as unknown as ChildProcess;
    });
    return launchEnv;
  }

  it('static init returns new instance', () => {
    const appLaunch = ApplicationLaunch.init();
    assert.isObject(appLaunch);
  });

  it('new window matches created window', () => {
    const appLaunch = ApplicationLaunch.init();
    const testWindow = createSinonStubInstance(MainWindow);
    appLaunch.setActivationWindow(testWindow);

    const createdWindow = appLaunch.mainWindow as MainWindow;

    assert.strictEqual(testWindow, createdWindow, 'Test window does not match created window');
  });

  it('launchRStudio forwards --automation-agent to the relaunched instance', () => {
    const spawnStub = sinon.stub(childProcess, 'spawn').returns({ unref: sinon.stub() } as unknown as ChildProcess);
    const appLaunch = ApplicationLaunch.init();

    appLaunch.launchRStudio({});
    assert.notInclude(spawnStub.firstCall.args[1] as string[], '--automation-agent');

    app.commandLine.appendSwitch('automation-agent');
    try {
      appLaunch.launchRStudio({});
    } finally {
      app.commandLine.removeSwitch('automation-agent');
    }
    assert.include(spawnStub.secondCall.args[1] as string[], '--automation-agent');
  });

  it('launchRStudio opens the project found in the working directory', () => {
    const launchEnv = captureLaunchEnv();
    const dir = projectDir();

    ApplicationLaunch.init().launchRStudio({ workingDirectory: dir });

    assert.equal(launchEnv.project, path.join(dir, 'test.Rproj'));
    assert.equal(launchEnv.workingDir, dir);
  });

  it('launchRStudio starts without a project when noProject is requested', () => {
    const launchEnv = captureLaunchEnv();
    const dir = projectDir();

    ApplicationLaunch.init().launchRStudio({ workingDirectory: dir, noProject: true });

    assert.isEmpty(launchEnv.project);
    assert.equal(launchEnv.workingDir, dir);
  });

  it('launchRStudio ignores an inherited initial project when noProject is requested', () => {
    const launchEnv = captureLaunchEnv();
    const dir = projectDir();

    // set when this instance was itself started by opening a project
    setenv(kRStudioInitialProject, path.join(dir, 'test.Rproj'));

    ApplicationLaunch.init().launchRStudio({ workingDirectory: dir, noProject: true });

    assert.isEmpty(launchEnv.project);
    assert.equal(launchEnv.workingDir, dir);
  });

  it('Resolve Empty Project File Path', () => {
    const projectFilePath = resolveProjectFile('./../');
    assert.isEmpty(projectFilePath);
  });

  it('Resolve Project File Path', () => {
    const filename = 'test.rproj';
    fs.writeFileSync('./' + filename, '');

    const rprojExtension = 'rproj';
    const projectFilePath = resolveProjectFile('./');
    const extensionRegexp = new RegExp(/(?:\.([^.]+))?$/);

    const hasExtension = extensionRegexp.test(projectFilePath);
    assert.isTrue(hasExtension, 'File does not have extension');
    assert.equal(projectFilePath, filename, 'Filename does not match with test file');

    try {
      const isRprojExtensionValid =
        (extensionRegexp.exec(projectFilePath) as string[])[1].toLowerCase() === rprojExtension;

      assert.isTrue(isRprojExtensionValid, 'File extension is not .rproj');

      fs.unlinkSync('./' + filename);
    } catch (_err: unknown) {
      assert.isTrue(false, 'Error happened when trying to assert rproj extension');
    }
  });
});
