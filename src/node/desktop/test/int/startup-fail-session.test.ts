/*
 * startup-fail-session.test.ts
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

import { launch } from './int-utils';

describe('Startup With Failing RSession', () => {
  let electronApp: ElectronApplication;
  let window: Page;

  beforeEach(async () => {
    // tell the rsession to immediately terminate with exit code of 1
    electronApp = await launch(['--session-exit']);
    window = await electronApp.firstWindow();
    window.setDefaultTimeout(5000);
  });

  afterEach(async () => {
    await electronApp.close();
  });

  it('Shows launch failure page if session fails to launch', async function () {
    // check that page is loaded with H1 containing "Error Starting R"
    await window.waitForSelector('#launch_failed');
    const h1 = await window.innerText('h1');
    assert.equal(h1, 'Error Starting R');
  });
});
