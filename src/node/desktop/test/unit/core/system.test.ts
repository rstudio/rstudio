/*
 * system.test.ts
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

import { generateShortenedUuid, generateUuid } from '../../../src/core/system';

describe('System', () => {
  describe('uuid generation', () => {
    it('generateUuid returns uuid string with dashes', () => {
      const uuid = generateUuid();
      assert(uuid.indexOf('-') >= 0);
    });
    it('generateUuid returns uuid string without dashes', () => {
      const uuid = generateUuid(false);
      assert(uuid.length > 0);
      assert(uuid.indexOf('-') < 0);
    });
  });
  describe('hash functions', () => {
    const crc32 = generateShortenedUuid();
    assert(crc32.length === 8);
    assert(crc32.indexOf('-') < 0);
  });
});
 
