/*
 * utils.test.ts
 *
 * Copyright (C) 2024 by Posit Software, PBC
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
import { describe } from 'mocha';

import { NullLogger, setLogger } from '../../../src/core/logger';
import { createAliasedPath, normalizeSeparators } from '../../../src/ui/utils';

describe('utils', () => {
  before(() => {
    setLogger(new NullLogger());
  });

  afterEach(() => {
    // make sure we leave cwd in a valid place
    process.chdir(__dirname);
  });

  describe('Aliased paths', () => {
    it('Paths are aliased correctly', () => {
      const path = '/Users/user/path/to/project';
      const home = '/Users/user';
      const aliasedPath = createAliasedPath(path, home);
      assert(aliasedPath === '~/path/to/project');
    });

    it('Home folder path is aliased correctly', () => {
      const path = '/Users/user';
      const home = '/Users/user';
      const aliasedPath = createAliasedPath(path, home);
      assert(aliasedPath === '~');
    });

    it('DOS paths are aliased correctly', () => {
      const path = 'C:\\Users\\user\\Documents\\path\\to\\project';
      const home = 'C:\\Users\\user\\Documents';
      const aliasedPath = createAliasedPath(path, home);
      assert(aliasedPath === '~/path/to/project');
    });

    it('DOS home folder path is aliased correctly', () => {
      const path = 'C:\\Users\\user\\Documents';
      const home = 'C:\\Users\\user\\Documents';
      const aliasedPath = createAliasedPath(path, home);
      assert(aliasedPath === '~');
    });


    it('Paths are aliased correctly, even with trailing slash on home folder', () => {
      const path = '/Users/user/path/to/project';
      const home = '/Users/user/';
      const aliasedPath = createAliasedPath(path, home);
      assert(aliasedPath === '~/path/to/project');
    });

    it('Paths are aliased correctly, even with trailing slash on folder', () => {
      const path = '/Users/user/path/to/project/';
      const home = '/Users/user';
      const aliasedPath = createAliasedPath(path, home);
      assert(aliasedPath === '~/path/to/project');
    });

    it('Paths are aliased correctly, even with trailing slashes on both folders', () => {
      const path = '/Users/user/path/to/project/';
      const home = '/Users/user/';
      const aliasedPath = createAliasedPath(path, home);
      assert(aliasedPath === '~/path/to/project');
    });

    it('Paths with differing slashes are aliased correctly', () => {
      const path = '//server/home/user/path/to/project';
      const home = '\\\\server\\home\\user';
      const aliasedPath = createAliasedPath(path, home);
      assert(aliasedPath === '~/path/to/project');
    });

    it('DOS paths are handled', () => {
      const path = 'C:/Users/user/path/to/project';
      const home = 'C:\\Users\\user';
      const aliasedPath = createAliasedPath(path, home);
      assert(aliasedPath === '~/path/to/project');
    });

    it('Original path returned is aliasing fails', () => {
      const path = '/home/other/path/to/project';
      const home = '/home/user';
      const aliasedPath = createAliasedPath(path, home);
      assert(aliasedPath === path);
    });

    it('Path that matches homepath prefix does not get an alias', () => {
      const path = '/home/user2/path/to/project';
      const home = '/home/user';
      const aliasedPath = createAliasedPath(path, home);
      assert(aliasedPath === path);
    });
  });

  describe('Normalize separators', () => {
    it('Already normalized posix path not changed', () => {
      const path = '/Users/user/path/to/project';
      const normalized = normalizeSeparators(path);
      assert(path === normalized);
    });
    it('Already normalized Windows path not changed', () => {
      const path = 'C:\\Users\\user\\path\\to\\project';
      const normalized = normalizeSeparators(path, '\\');
      assert(path === normalized);
    });
    it('Already normalized Windows UNC path not changed', () => {
      const path = '\\\\machine\\Users\\user\\path\\to\\project';
      const normalized = normalizeSeparators(path, '\\');
      assert(path === normalized);
    });
    it('Extra posix separators removed', () => {
      const path = '/Users///user//path//to/project';
      const expected = '/Users/user/path/to/project';
      const normalized = normalizeSeparators(path);
      assert(normalized === expected);
    });
    it('Extra Windows separators removed', () => {
      const path = 'C:\\\\Users\\\\user\\\\path\\to\\project';
      const expected = 'C:\\Users\\user\\path\\to\\project';
      const normalized = normalizeSeparators(path, '\\');
      assert(normalized === expected);
    });
    it('Extra Windows separators removed from UNC path', () => {
      const path = '\\\\machine\\\\Users\\\\user\\path\\to\\\\project';
      const expected = '\\\\machine\\Users\\user\\path\\to\\project';
      const normalized = normalizeSeparators(path, '\\');
      assert(normalized === expected);
    });
    it('Extra Windows separators removed and replaced with posix separators', () => {
      const path = 'C:\\\\Users\\\\user\\\\path\\to\\project';
      const expected = 'C:/Users/user/path/to/project';
      const normalized = normalizeSeparators(path, '/');
      assert(normalized === expected);
    });
    it('Extra Windows separators removed from UNC path and replaced with posix separators', () => {
      const path = '\\\\machine\\\\Users\\\\user\\path\\to\\\\project';
      const expected = '//machine/Users/user/path/to/project';
      const normalized = normalizeSeparators(path, '/');
      assert(normalized === expected);
    });
    it('Renormalizing already normalized path is a no-op', () => {
      const path = '/Users///user//path//to/project';
      const expected = '/Users/user/path/to/project';
      const normalized = normalizeSeparators(path);
      const renormalized = normalizeSeparators(normalized);
      assert(renormalized === expected);
    });
    it('Renormalizing already normalized Windows path is a no-op', () => {
      const path = 'C:\\\\Users\\\\user\\\\path\\to\\project';
      const expected = 'C:/Users/user/path/to/project';
      const normalized = normalizeSeparators(path);
      const renormalized = normalizeSeparators(normalized);
      assert(renormalized === expected);
    });
    it('Renormalizing already normalized UNC Windows path is a no-op', () => {
      const path = '\\\\machine\\\\Users\\\\user\\path\\to\\\\project';
      const expected = '//machine/Users/user/path/to/project';
      const normalized = normalizeSeparators(path);
      const renormalized = normalizeSeparators(normalized);
      assert(renormalized === expected);
    });
  });

});
