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

import { detectRosetta } from '../../../src/main/detect-rosetta';

describe('Detect Rosetta', () => {
  it('Only checks for Rosetta installation on Apple silicon', async () => {
    const isRosettaInstalled = detectRosetta();
    const isAppleSilicon = process.platform === 'darwin' && process.arch === 'arm64';
    if (isAppleSilicon) {
      assert.isBoolean(isRosettaInstalled);
    } else {
      assert.isUndefined(isRosettaInstalled);
    }
  });
});
