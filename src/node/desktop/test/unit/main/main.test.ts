/*
 * main.test.ts
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

import { getenv } from '../../../src/core/environment';
import Main from '../../../src/main/main';

describe('Main', () => {
  describe('Static helpers', () => {
    it('initializeSharedSecret generates a random string in RS_SHARED_SECRET envvar_', () => {
      const envvar = 'RS_SHARED_SECRET';
      assert.equal(getenv(envvar).length, 0);
      Main.initializeSharedSecret();
      assert.isAtLeast(getenv(envvar).length, 0);
    });
  });
});
