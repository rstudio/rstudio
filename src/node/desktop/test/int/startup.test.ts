/*
 * startup.test.ts
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

import { ElectronApplication, _electron } from 'playwright';

import path from 'path';

// Find path to RStudio entrypoint
function getMain(): string {
  return path.join(__dirname, '../../dist/main/main.js');
}

async function launch(): Promise<ElectronApplication> {
  return await _electron.launch({ args: [getMain()] });
}

describe('Startup and Exit', async function () {
  this.timeout(15000);

  it('Shows a window with Console tab', async function () {
    const electronApp = await launch();
    
    // wait for a window
    const window = await electronApp.firstWindow();

    // check that Console tab has role=tab
    const consoleTabRole = await window.getAttribute('#rstudio_workbench_tab_console', 'role');
    assert.equal(consoleTabRole, 'tab');

    // exit app
    await electronApp.close();
  });
});