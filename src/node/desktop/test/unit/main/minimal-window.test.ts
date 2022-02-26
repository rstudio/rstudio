/*
 * minimal-window.test.ts
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

import { openMinimalWindow } from '../../../src/main/minimal-window';
import { clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { Application } from '../../../src/main/application';
import { GwtWindow } from '../../../src/main/gwt-window';

class TestGwtWindow extends GwtWindow {
  onActivated(): void {
    throw new Error('Method not implemented.');
  }
}

describe('minimal-window', () => {
  beforeEach(() => {
    setApplication(new Application());
  });
  afterEach(() => {
    clearApplicationSingleton();
  });

  it('can be constructed', () => {
    const gwtWindow = new TestGwtWindow({ name: '' });
    const minWin = openMinimalWindow(gwtWindow, 'test-win', 'about:blank', 640, 480);
    assert.isObject(minWin);
  });
});
