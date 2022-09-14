/*
 * detect-r.test.ts
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

import { saveAndClear, restore } from '../unit-utils';
import { promptUserForR } from '../../../src/main/detect-r';

describe('DetectR', () => {
  const vars: Record<string, string> = {
    RSTUDIO_WHICH_R: '',
  };

  beforeEach(() => {
    saveAndClear(vars);
  });

  afterEach(() => {
    restore(vars);
  });

  it('Prompt User for R on Non-Windows OS', async () => {
    const platform = 'linux';

    const [path, preflightError] = await promptUserForR(platform);
    assert.equal(path, null);
    assert.equal(preflightError?.message, 'This window can only be opened on Windows');
  });
});
