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

import { launch } from './int-utils';

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
  it('Shows launch failure page if session fails to launch', async function () {
    const electronApp = await launch(['--session-exit-code=1']);
    
    // wait for a window
    const window = await electronApp.firstWindow();

    // check that page is loaded with H1 containing "Error Starting R"
    const h1 = await window.innerText('h1');
    assert.equal(h1, 'Error Starting R');

    // exit app
    await electronApp.close();
  });
});