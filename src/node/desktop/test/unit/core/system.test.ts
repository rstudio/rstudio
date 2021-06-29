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

import { generateRandomPort, generateShortenedUuid, generateUuid } from '../../../src/core/system';

describe('System', () => {
  it('generateUuid returns uuid string with dashes', () => {
    const uuid = generateUuid();
    assert.isTrue(uuid.indexOf('-') >= 0);
  });
  it('generateUuid returns uuid string without dashes', () => {
    const uuid = generateUuid(false);
    assert.isTrue(uuid.length > 0);
    assert.isTrue(uuid.indexOf('-') < 0);
  });
  it('generateShortenedUuid returns character string', () => {
    const crc32 = generateShortenedUuid();
    assert.isNotEmpty(crc32);
    assert.isTrue(crc32.indexOf('-') < 0);
  });
  it('generateRandomPort returns random positive number', () => {
    const port1 = generateRandomPort();
    const port2 = generateRandomPort();
    assert.notEqual(port1, port2);
    assert.isAbove(port1, 0);
    assert.isAbove(port2, 0);
  });
});
