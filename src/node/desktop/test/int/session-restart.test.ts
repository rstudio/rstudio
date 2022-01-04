/*
 * session-restart.test.ts
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
import { ElectronApplication, Page } from 'playwright';

import { launch, setTimeoutPromise } from './int-utils';
import { clearConsole, typeConsoleCommand } from './console';

describe('Session restart scenarios', () => {
  let electronApp: ElectronApplication;
  let window: Page;

  beforeEach(async () => {
    electronApp = await launch();
    window = await electronApp.firstWindow();
    window.setDefaultTimeout(9000);
  });

  afterEach(async () => {
    await electronApp.close();
  });

  it('Restart current session', async function () {
    await clearConsole(window);
    await typeConsoleCommand(window, ".rs.api.executeCommand('restartR')");
    await setTimeoutPromise(900); // TODO: yuck, find a better way to do this

    // TODO: figure out how to assert that 'Restarting R session...' is present in
    // the console output
  });
});
