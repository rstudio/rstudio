/**
 * 
 * file-serializer.test.ts
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
 
import { Success } from '../../../src/core/err';
import { FilePath } from '../../../src/core/file-path';
import { stringifyStringPair, writeCollectionToFile, writeStringMapToFile } from '../../../src/core/file-serializer';
 
describe('file-serializer', () => {
  it('collection/map written to a file', () => {
    const filepath = new FilePath(process.cwd() + 'test.conf');
    const map = new Map<string, string>();
    map.set('1', 'one');
    map.set('2', 'two');
    map.set('3', 'three');

    assert.equal(writeCollectionToFile<string, string>(filepath, map, stringifyStringPair), Success());
    assert.equal(filepath.remove(), Success());
  });
  it('WIP string map written to a file', () => {
    const filepath = new FilePath(process.cwd() + 'test.conf');
    const map = new Map<string, string>();
    map.set('1', 'one');
    map.set('2', 'two');
    map.set('3', 'three');

    assert.isFalse(filepath.existsSync());
    assert.equal(writeStringMapToFile(filepath, map), Success());
    assert.isTrue(filepath.existsSync());
    assert.equal(filepath.remove(), Success());
    assert.isFalse(filepath.existsSync());
  });
  it('key-value pair created, quoted, and escaped', () => {
    const key = 'testKey';
    const val = 'this\n is" my\r value\\';
    const expected = 'testKey="this\\n is\\" my\\r value\\\\"';
    assert.equal(stringifyStringPair([key, val]), expected);
  });
});