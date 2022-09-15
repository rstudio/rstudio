/*
 * gwt-callback.test.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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

import { GwtCallback } from '../../../src/main/gwt-callback';
import { MainWindow } from '../../../src/main/main-window';

describe('DesktopCallback', () => {
  afterEach(() => {
    sinon.restore();
  });

  it('can be constructed', () => {
    const isRemoteDesktop = false;
    const mainWindow = createSinonStubInstance(MainWindow);
    const callback = new GwtCallback(mainWindow, isRemoteDesktop);
    assert.equal(callback.isRemoteDesktop, isRemoteDesktop);
  });
});
