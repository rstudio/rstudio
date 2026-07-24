/*
 * detect-rosetta.test.ts
 *
 * Copyright (C) 2023 by Posit Software, PBC
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

import { ensureRosettaForIntelR } from '../../../src/main/detect-rosetta';

describe('ensureRosettaForIntelR', () => {
  it('permits launch on platforms that never need Rosetta 2', () => {
    const isAppleSilicon = process.platform === 'darwin' && process.arch === 'arm64';
    // Off Apple Silicon there is no Rosetta consideration, so the launch always
    // proceeds. On Apple Silicon the result depends on the local Rosetta state
    // and may show a blocking dialog, so that path is not exercised here.
    if (!isAppleSilicon) {
      assert.isTrue(ensureRosettaForIntelR());
    }
  });
});
