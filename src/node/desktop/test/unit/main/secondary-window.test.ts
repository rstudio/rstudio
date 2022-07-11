/*
 * secondary-window.test.ts
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

import { isWindowsDocker } from '../unit-utils';
import { SecondaryWindow } from '../../../src/main/secondary-window';

if (!isWindowsDocker()) {
  describe('SecondaryWindow', () => {
    it('construction creates a hidden BrowserWindow', () => {
      const win = new SecondaryWindow(false, 'some name');
      assert.isObject(win);
      assert.isObject(win.window);
      assert.isFalse(win.window.isVisible());
    });
  });
}