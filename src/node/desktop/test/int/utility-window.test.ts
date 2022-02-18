/*
 * utility-window.test.ts
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
import { ElectronApplication, Page } from 'playwright';

import { getWindowTitles, launch, setTimeoutPromise } from './int-utils';
import { typeConsoleCommand } from './console';

describe.skip('Display secondary utility windows', () => {
  let electronApp: ElectronApplication;
  let window: Page;

  beforeEach(async () => {
    electronApp = await launch();
    window = await electronApp.firstWindow();
    window.setDefaultTimeout(5000);
  });

  afterEach(async () => {
    await electronApp.close();
  });

  it('Shows GPU utility window', async function () {
    await typeConsoleCommand(window, ".rs.api.executeCommand('showGpuDiagnostics')");
    await setTimeoutPromise(500); // TODO: yuck, find a better way to do this
    const windows = electronApp.windows();
    assert.equal(windows.length, 2);
    const titles = await getWindowTitles(electronApp);
    let found = false;
    for (const title of titles) {
      if (title === 'chrome://gpu') {
        found = true;
        break;
      }
    }
    assert.isTrue(found);
  });
});
