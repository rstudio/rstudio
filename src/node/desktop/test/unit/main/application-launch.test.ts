/*
 * application-launch.test.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { describe } from 'mocha';
import { assert } from 'chai';
import fs from 'fs';

import { ApplicationLaunch, resolveProjectFile } from '../../../src/main/application-launch';
import { MainWindow } from '../../../src/main/main-window';
import { createSinonStubInstance } from '../unit-utils';

describe('ApplicationLaunch', () => {
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
    } catch (err: unknown) {
      assert.isTrue(false, 'Error happened when trying to assert rproj extension');
    }
  });
});
