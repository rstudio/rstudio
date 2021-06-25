/*
 * pending-window.test.ts
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

import { PendingWindow } from '../../../src/main/pending-window';

describe('PendingWindow', () => {
  it('can be constructed', () => {
    const name = 'foo';
    const x = 1;
    const y = 2;
    const width = 3;
    const height = 4;
    const pw = new PendingWindow(name, x, y, width, height);
    assert.equal(pw.name, name);
    assert.equal(pw.x, x);
    assert.equal(pw.y, y);
    assert.equal(pw.width, width);
    assert.equal(pw.height, height);
    assert.isFalse(pw.isEmpty);
  });
});
