/*
 * utility-window.test.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import { launch, setTimeoutPromise } from './int-utils';


describe('Display secondary utility windows', () => {
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
    await window.click('#rstudio_workbench_tab_console');
    await window.type('#rstudio_console_input', '.rs.api.executeCommand(\'showGpuDiagnostics\')');
    await window.press('#rstudio_console_input', 'Enter');
    await setTimeoutPromise(500);
    const windows = electronApp.windows();
    assert.equal(windows.length, 2);
  });
});
