/*
 * minimal-window.test.ts
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
import sinon from 'sinon';
import { createSinonStubInstance } from '../unit-utils';

import { MainWindow } from '../../../src/main/main-window';
import { openMinimalWindow } from '../../../src/main/minimal-window';
import { clearApplicationSingleton, setApplication } from '../../../src/main/app-state';
import { Application } from '../../../src/main/application';

describe('minimal-window', () => {
  beforeEach(() => {
    clearApplicationSingleton();
  });
  afterEach(() => {
    sinon.restore();
  });


  it('can be constructed', () => {
    setApplication(new Application());
    const mainWindowStub = createSinonStubInstance(MainWindow);
    const minWin = openMinimalWindow('test-win', 'about:blank', 640, 480, mainWindowStub);
    assert.isObject(minWin);
  });
});
