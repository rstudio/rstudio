/*
 * r-environment-cache.test.ts
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

import { assert } from 'chai';
import { mkdirSync, mkdtempSync, rmSync, symlinkSync, utimesSync, writeFileSync } from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';
import { REnvironmentCache } from '../../../src/main/r-environment-cache';

describe('R environment disk cache', () => {
  let root: string;
  let home: string;
  let executable: string;
  let directory: string;
  let stdout: string;
  const environment = { PATH: '/usr/bin', LD_LIBRARY_PATH: '/opt/libs' };

  beforeEach(() => {
    root = mkdtempSync(join(tmpdir(), 'r-environment-cache-'));
    home = join(root, 'R');
    directory = join(root, 'cache');
    mkdirSync(join(home, 'bin'), { recursive: true });
    mkdirSync(join(home, 'lib'));
    executable = join(home, 'bin', 'R');
    writeFileSync(executable, 'R launcher');
    writeFileSync(join(home, 'lib', 'libR.so'), 'R shared library');
    stdout =
      '\x1E' +
      ['4.5.1', home, home + '/doc', home + '/include', home + '/share', '', '', '/opt/libs', 'test-platform'].join(
        '\x1F',
      );
  });

  afterEach(() => rmSync(root, { recursive: true, force: true }));

  it('reuses a successful query in a later process', () => {
    new REnvironmentCache(directory).write(executable, environment, stdout);
    assert.equal(new REnvironmentCache(directory).read(executable, environment), stdout);
  });

  it('accepts the trailing separator produced by the R query script', () => {
    const output = stdout + '\x1F';
    new REnvironmentCache(directory).write(executable, environment, output);
    assert.equal(new REnvironmentCache(directory).read(executable, environment), output);
  });

  it('rejects replacement launchers and shared libraries', () => {
    const cache = new REnvironmentCache(directory);
    cache.write(executable, environment, stdout);
    utimesSync(executable, 1, 1);
    assert.isNull(cache.read(executable, environment));

    cache.write(executable, environment, stdout);
    writeFileSync(join(home, 'lib', 'libR.so'), 'updated R shared library');
    assert.isNull(cache.read(executable, environment));
  });

  it('rejects a changed library search path', () => {
    const cache = new REnvironmentCache(directory);
    cache.write(executable, environment, stdout);
    assert.isNull(cache.read(executable, { ...environment, LD_LIBRARY_PATH: '/new/libs' }));
  });

  it('rejects a removed installation', () => {
    const cache = new REnvironmentCache(directory);
    cache.write(executable, environment, stdout);
    rmSync(executable);
    assert.isNull(cache.read(executable, environment));
  });

  it('rejects a symlink redirected to another R installation', function () {
    if (process.platform === 'win32') this.skip();
    const alias = join(root, 'selected-R');
    symlinkSync(executable, alias);
    const cache = new REnvironmentCache(directory);
    cache.write(alias, environment, stdout);
    rmSync(alias);
    const replacement = join(root, 'replacement-R');
    writeFileSync(replacement, 'another R launcher');
    symlinkSync(replacement, alias);
    assert.isNull(cache.read(alias, environment));
  });

  it('treats malformed and damaged entries as cache misses', () => {
    const cache = new REnvironmentCache(directory);
    cache.write(executable, environment, stdout);
    writeFileSync(join(directory, 'r-environment.json'), '{"query":{"version":1}}');
    assert.isNull(cache.read(executable, environment));
    writeFileSync(join(directory, 'r-environment.json'), 'broken json');
    assert.isNull(cache.read(executable, environment));
  });

  it('does not remember incomplete query output', () => {
    const cache = new REnvironmentCache(directory);
    cache.write(executable, environment, 'not R');
    assert.isNull(cache.read(executable, environment));
  });
});
