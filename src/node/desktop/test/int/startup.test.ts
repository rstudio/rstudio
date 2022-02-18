/*
 * startup.test.ts
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
import { waitForConsoleReady } from './console';

describe('Startup and Exit', () => {
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

  it('Shows a window with expected main menu', async function () {
    await waitForConsoleReady(window);

    assert.isTrue(
      await electronApp.evaluate(async ({ app }): Promise<boolean> => {
        return !!app.applicationMenu?.getMenuItemById('File');
      }),
      'Failed to find File menu',
    );
    assert.isTrue(
      await electronApp.evaluate(async ({ app }): Promise<boolean> => {
        return !!app.applicationMenu?.getMenuItemById('Edit');
      }),
      'Failed to find Edit menu',
    );
    assert.isTrue(
      await electronApp.evaluate(async ({ app }): Promise<boolean> => {
        return !!app.applicationMenu?.getMenuItemById('Code');
      }),
      'Failed to find Code menu',
    );
    assert.isTrue(
      await electronApp.evaluate(async ({ app }): Promise<boolean> => {
        return !!app.applicationMenu?.getMenuItemById('View');
      }),
      'Failed to find View menu',
    );
    assert.isTrue(
      await electronApp.evaluate(async ({ app }): Promise<boolean> => {
        return !!app.applicationMenu?.getMenuItemById('Plots');
      }),
      'Failed to find Plots menu',
    );
    assert.isTrue(
      await electronApp.evaluate(async ({ app }): Promise<boolean> => {
        return !!app.applicationMenu?.getMenuItemById('Session');
      }),
      'Failed to find Session menu',
    );
    assert.isTrue(
      await electronApp.evaluate(async ({ app }): Promise<boolean> => {
        return !!app.applicationMenu?.getMenuItemById('Build');
      }),
      'Failed to find Build menu',
    );
    assert.isTrue(
      await electronApp.evaluate(async ({ app }): Promise<boolean> => {
        return !!app.applicationMenu?.getMenuItemById('Debug');
      }),
      'Failed to find Debug menu',
    );
    assert.isTrue(
      await electronApp.evaluate(async ({ app }): Promise<boolean> => {
        return !!app.applicationMenu?.getMenuItemById('Profile');
      }),
      'Failed to find Profile menu',
    );
    assert.isTrue(
      await electronApp.evaluate(async ({ app }): Promise<boolean> => {
        return !!app.applicationMenu?.getMenuItemById('Tools');
      }),
      'Failed to find Tools menu',
    );
    assert.isTrue(
      await electronApp.evaluate(async ({ app }): Promise<boolean> => {
        return !!app.applicationMenu?.getMenuItemById('Help');
      }),
      'Failed to find Help menu',
    );
  });
});
