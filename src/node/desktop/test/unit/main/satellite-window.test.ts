/*
 * satellite-window.test.ts
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
import sinon from 'sinon';
import { createSinonStubInstance, isWindowsDocker } from '../unit-utils';

import { BrowserWindow } from 'electron';

import { SatelliteWindow } from '../../../src/main/satellite-window';
import { MainWindow } from '../../../src/main/main-window';
import { clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { Application } from '../../../src/main/application';

if (!isWindowsDocker()) {
  describe('SatelliteWindow', () => {
    beforeEach(() => {
      setApplication(new Application());
    });

    afterEach(() => {
      clearApplicationSingleton();
      sinon.restore();
    });

    it('construction creates a hidden BrowserWindow', () => {
      const mainWindowStub = createSinonStubInstance(MainWindow);
      const browserWin = new BrowserWindow({ show: false });
      const win = new SatelliteWindow(mainWindowStub, 'satellite window', browserWin.webContents);
      assert.isObject(win, 'failed isObject test');
      assert.isObject(win.window, 'failed has window test');
      assert.isFalse(win.window.isVisible(), 'failed window not visible test');
    });
  });
}
