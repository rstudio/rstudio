/*
 * file-serializer.test.ts
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
import path from 'path';
import os from 'os';
import fs from 'fs';

import { randomString } from '../unit-utils';

import { readStringArrayFromFile } from '../../../src/core/file-serializer';
import { FilePath } from '../../../src/core/file-path';

function writeTestFile(lineCount: number, includeBlanks: boolean): FilePath {
  const file = new FilePath(path.join(os.tmpdir(), 'rstudio-temp-test-file-' + randomString()));
  const blankLine = Buffer.from('  \t   \n', 'utf8');
  const fd = fs.openSync(file.getAbsolutePath(), 'w');
  for (let i = 0; i < lineCount; i++) {
    // make every third line a blank (whitespace-only)
    if (includeBlanks && i % 3 === 0) {
      fs.writeSync(fd, blankLine);
    } else {
      fs.writeSync(fd, Buffer.from(`Line ${i}\n`, 'utf8'));
    }
  }
  fs.closeSync(fd);
  return file;
}

describe('file-serializer', () => {
  it('readStringArrayFromFile reads a simple file including blank lines', async () => {
    const tmpFile = writeTestFile(100, true);
    const [result, error] = await readStringArrayFromFile(tmpFile, false);
    assert.isNull(error);
    assert.equal(100, result.length);
    tmpFile.removeSync();
  });
  it('readStringArrayFromFile reads a simple file without any blank lines', async () => {
    const tmpFile = writeTestFile(100, false);
    const [result, error] = await readStringArrayFromFile(tmpFile, false);
    assert.isNull(error);
    assert.equal(100, result.length);
    tmpFile.removeSync();
  });
  it('readStringArrayFromFile skips blank lines when requested', async () => {
    const tmpFile = writeTestFile(100, true);
    const [result, error] = await readStringArrayFromFile(tmpFile, true);
    assert.isNull(error);
    assert.equal(66, result.length);
    for (const line in result) {
      assert.isFalse(line.length === 0);
    }
    tmpFile.removeSync();
  });
  it('readStringArrayFromFile with skip set works when no blank lines', async () => {
    const tmpFile = writeTestFile(100, false);
    const [result, error] = await readStringArrayFromFile(tmpFile, true);
    assert.isNull(error);
    assert.equal(100, result.length);
    tmpFile.removeSync();
  });
});
