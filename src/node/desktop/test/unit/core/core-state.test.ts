/*
 * core-state.test.ts
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

import { coreState, clearCoreSingleton } from '../../../src/core/core-state';

describe('core-state', () => {
  afterEach(() => {
    clearCoreSingleton();
  });

  it('creates and stores singleton', () => {
    const state1 = coreState();
    const state2 = coreState();
    assert.isNotEmpty(state1);
    assert.isNotEmpty(state2);
    assert.deepEqual(state1, state2);
  });
  it('creates new singleton after clearing', () => {
    const state1 = coreState();
    assert.isNotEmpty(state1);
    clearCoreSingleton();
    const state2 = coreState();
    assert.isNotEmpty(state2);
    assert.notDeepEqual(state1, state2);
  });
});
