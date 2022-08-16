/*
 * pending-window.test.ts
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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

import { PendingWindow } from '../../../src/main/pending-window';
import { MainWindow } from '../../../src/main/main-window';

describe('PendingWindow', () => {
  afterEach(() => {
    sinon.restore();
  });

  it('PendingWindow can be either Satellite or Secondary', () => {
    const mainWindowStub = createSinonStubInstance(MainWindow);
    const pendingWindows: Array<PendingWindow> = [];
    pendingWindows.push({
      type: 'satellite',
      name: 'sputnik',
      mainWindow: mainWindowStub,
      screenX: 1,
      screenY: 2,
      width: 3,
      height: 4,
      allowExternalNavigate: false,
    });

    pendingWindows.push({
      type: 'secondary',
      name: 'moon',
      allowExternalNavigate: false,
      showToolbar: true,
    });

    let foundSatellite = false;
    let foundSecondary = false;
    for (const pending of pendingWindows) {
      switch (pending.type) {
        case 'satellite':
          assert.deepEqual(pending.name, 'sputnik');
          assert.equal(pending.width, 3);
          foundSatellite = true;
          break;
        case 'secondary':
          assert.deepEqual(pending.name, 'moon');
          assert.isTrue(pending.showToolbar);
          foundSecondary = true;
          break;
      }
    }

    assert.isTrue(foundSatellite);
    assert.isTrue(foundSecondary);
  });
});
