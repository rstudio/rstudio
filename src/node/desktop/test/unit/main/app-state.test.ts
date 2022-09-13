/*
 * app-state.test.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

import { Application } from '../../../src/main/application';
import { appState, clearApplicationSingleton, setApplication } from '../../../src/main/app-state';

describe('AppState', () => {
  beforeEach(() => {
    clearApplicationSingleton();
  });
  afterEach(() => {
    clearApplicationSingleton();
  });

  it('fetching appstate before created throws', () => {
    assert.throws(() => appState());
  });
  it('fetching appstate after setting singleton works', () => {
    setApplication(new Application());
    assert.isFalse(appState().runDiagnostics);
  });
  it('trying to set singleton more than once throws', () => {
    setApplication(new Application());
    assert.throws(() => setApplication(new Application()));
  });
});
