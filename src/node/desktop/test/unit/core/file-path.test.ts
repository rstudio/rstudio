/*
 * file-path.test.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
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
import { randomString } from '../unit-utils';

import fs from 'fs';
import fsPromises from 'fs/promises';
import path from 'path';
import os from 'os';

import { FilePath, normalizeSeparatorsNative } from '../../../src/core/file-path';
import { userHomePath } from '../../../src/core/user';
import { setLogger, NullLogger } from '../../../src/core/logger';
import { clearCoreSingleton } from '../../../src/core/core-state';
import { isFailure, isSuccessful } from '../../../src/core/err';

function realpathSync(path: string): string {
  return fs.realpathSync(path);
}

async function realpath(path: string): Promise<string> {
  return fsPromises.realpath(path);
}

function getTestDir(): FilePath {
  return new FilePath(path.join(os.tmpdir(), 'rstudio-temp-tests-' + randomString()));
}

// A path that should never exist
const bogusPath = '/super/bogus/path/42';

// A path that a non-elevated user cannot create
const cannotCreatePath = process.platform === 'win32' ? '\\\\rstudio-foo-bar-23456\\a_test_folder' : '/foo/bar/crazy';

// An absolute path in generic format (see boost filesystem for description of "generic" format)
const absolutePath = process.platform === 'win32' ? 'C:/Users/human/documents' : '/users/human/documents';

describe('FilePath', () => {
  before(() => {
    setLogger(new NullLogger());
  });
  after(() => {
    clearCoreSingleton();
  });

  afterEach(() => {
    // make sure we leave cwd in a valid place
    process.chdir(__dirname);
  });

  describe('Constructor checks', () => {
    it('Should store and return the supplied path', () => {
      const path1 = 'hello/world';
      const path2 = '~/foo';
      const path3 = '/once/upon/a/time';
      const path4 = '~';
      assert.strictEqual(new FilePath(path1).getAbsolutePath(), path1);
      assert.strictEqual(new FilePath(path2).getAbsolutePath(), path2);
      assert.strictEqual(new FilePath(path3).getAbsolutePath(), path3);
      assert.strictEqual(new FilePath(path4).getAbsolutePath(), path4);
      assert.strictEqual(new FilePath(path3).toString(), path3);
    });
    it('Should create empty path when given no arguments', () => {
      const path = new FilePath();
      assert.isEmpty(path.getAbsolutePath());
      assert.isTrue(path.isEmpty());
    });
    it('getAbsolutePathNative should return raw path on Windows', () => {
      if (process.platform === 'win32') {
        const originalPath = 'C:\\Windows\\Was\\Here';
        const fp1 = new FilePath(originalPath);
        const native = fp1.getAbsolutePathNative();
        assert(native === originalPath);
      }
    });
  });

  describe('Comparisons', () => {
    it('equals should return true if storing exact same path string', () => {
      const path1 = new FilePath('/hello/world');
      const path2 = new FilePath('/hello/world');
      assert.isTrue(path1.equals(path2));
    });
    it('equals should return false if they are storing different path strings', () => {
      const path1 = new FilePath('/hello/world');
      const path2 = new FilePath('/hello/../hello/world');
      assert.isFalse(path1.equals(path2));
    });
    it('equals should return true if both are empty', () => {
      const path1 = new FilePath();
      const path2 = new FilePath();
      assert.isTrue(path1.equals(path2));
    });
    it('isWithin should handle detect simple path containment checks', () => {
      const pPath = new FilePath('/path/to');
      const aPath = new FilePath('/path/to/a');
      const bPath = new FilePath('/path/to/b');
      assert.isTrue(aPath.isWithin(pPath));
      assert.isTrue(bPath.isWithin(pPath));
      assert.isFalse(aPath.isWithin(bPath));
    });
    it('isWithin should not be fooled by directory traversal', () => {
      // the first path is not inside the second even though it appears to be lexically
      const aPath = new FilePath('/path/to/a/../b');
      const bPath = new FilePath('path/to/a');
      assert.isFalse(aPath.isWithin(bPath));
    });
    it('isWithin should not be fooled by substrings', () => {
      const cPath = new FilePath('/path/to/foo');
      const dPath = new FilePath('path/to/foobar');
      assert.isFalse(dPath.isWithin(cPath));
    });
  });

  describe('Retrieval methods', () => {
    it('getCanonicalPathSync should return empty results for empty path', () => {
      const f = new FilePath();
      assert.isFalse(f.existsSync());
      assert.isFalse(!!f.getAbsolutePath());
      assert.isTrue(f.isEmpty());
      assert.isTrue(f.isWithin(f));
      assert.isFalse(f.isWithin(new FilePath('/some/path')));
      assert.isEmpty(f.getCanonicalPathSync());
      assert.isEmpty(f.getExtension());
      assert.isEmpty(f.getExtensionLowerCase());
      assert.isEmpty(f.getFilename());
    });
    it('getCanonicalPathSync should return a non-empty path for a path that exists', () => {
      const f = new FilePath(os.tmpdir());
      assert.isTrue(f.existsSync());
      assert.isNotEmpty(f.getCanonicalPathSync());
    });
    it("getCanonicalPathSync should return an empty path for a path that doesn't exist", () => {
      const f = new FilePath('/some/really/bogus/path');
      assert.isFalse(f.existsSync());
      assert.isEmpty(f.getCanonicalPathSync());
    });
    it('getCanonicalPath should return empty results for empty path', async () => {
      const f = new FilePath();
      assert.isFalse(await f.existsAsync());
      assert.isFalse(!!f.getAbsolutePath());
      assert.isTrue(f.isEmpty());
      assert.isTrue(f.isWithin(f));
      assert.isFalse(f.isWithin(new FilePath('/some/path')));
      assert.isEmpty(await f.getCanonicalPath());
    });
    it('getCanonicalPath should return a non-empty path for a path that exists', async () => {
      const f = new FilePath(os.tmpdir());
      assert.isTrue(await f.existsAsync());
      assert.isNotEmpty(await f.getCanonicalPath());
    });
    it("getCanonicalPath should return an empty path for a path that doesn't exist", async () => {
      const f = new FilePath('/some/really/bogus/path');
      assert.isFalse(await f.existsAsync());
      const result = await f.getCanonicalPath();
      assert.isEmpty(result);
    });
    it('getExtension should return the extension including leading period', () => {
      const f = new FilePath('/some/stuff/hello.tXt');
      assert.strictEqual(f.getExtension(), '.tXt');
    });
    it('getExtension should return no extension for extension-only filename', () => {
      const f = new FilePath('/some/stuff/.foo');
      assert.isEmpty(f.getExtension());
    });
    it('getExtension should return blank extension for file without extension', () => {
      const f = new FilePath('/some/stuff/hello');
      assert.isEmpty(f.getExtension());
    });
    it('getExtensionLowerCase should return the extension in lowercase', () => {
      const f = new FilePath('/some/stuff/hello.tXt');
      assert.strictEqual(f.getExtensionLowerCase(), '.txt');
    });
    it('getExtensionLowerCase should return blank extension for file without extension', () => {
      const f = new FilePath('/some/stuff/hello');
      assert.isEmpty(f.getExtension());
    });
    it('getFilename should return filename including extension', () => {
      const f = new FilePath('/etc/foo/hello.txt.world');
      assert.strictEqual(f.getFilename(), 'hello.txt.world');
    });
    it('getFilename should return filename when there is no extension', () => {
      const f = new FilePath('/etc/foo/hello');
      assert.strictEqual(f.getFilename(), 'hello');
    });
    it('getFilename should return filename for extension-only filename', () => {
      const f = new FilePath('/etc/foo/.hello');
      assert.strictEqual(f.getFilename(), '.hello');
    });
    it('getLastWriteTimeSync should return zero for non-existent file', () => {
      const f = new FilePath('/some/file/that/will/not/exist');
      assert.strictEqual(f.getLastWriteTimeSync(), 0);
    });
    it('getStem returns the file stem', () => {
      assert.equal(new FilePath('/path/to/file.txt').getStem(), 'file');
      assert.equal(new FilePath('file.txt').getStem(), 'file');
      assert.equal(new FilePath('noExtension').getStem(), 'noExtension');
      assert.equal(new FilePath('.hiddenFile.txt').getStem(), '.hiddenFile');
      assert.equal(new FilePath('.hiddenFileNoExtension').getStem(), '.hiddenFileNoExtension');

      // You could debate that this should only return 'file'. Including test here to show current behavior
      assert.equal(new FilePath('/path/to/file.extra.txt').getStem(), 'file.extra');

      // A full path to a file with no extension is ambiguous. Including test here to show current behavior
      assert.equal(new FilePath('/path/to/noExtensionOrDirectory').getStem(), 'noExtensionOrDirectory');
      assert.equal(new FilePath('/path/to/directory/').getStem(), '');
    });
  });

  describe('Get a safe current path', () => {
    it('safeCurrentPathSync should return current working directory if it exists', () => {
      const cwd = new FilePath(process.cwd());
      const rootPath = new FilePath('/');
      const currentPath = FilePath.safeCurrentPathSync(rootPath);
      assert.strictEqual(currentPath.getAbsolutePath(), cwd.getAbsolutePath());
      assert.strictEqual(realpathSync(cwd.getAbsolutePath()), realpathSync(process.cwd()));
    });
    it("safeCurrentPathSync should change to supplied safe path if it exists if cwd doesn't exist", () => {
      const origDir = new FilePath(process.cwd());
      // create a temp folder, chdir to it, then delete it
      let testDir = getTestDir().getAbsolutePath();
      fs.mkdirSync(testDir);
      process.chdir(testDir);
      testDir = realpathSync(testDir);
      try {
        fs.rmdirSync(testDir);
      } catch (error: unknown) {
        // On Windows, trying to remove current-working-directory may fail so can't
        // execute rest of this test case; cleanup and exit
        assert.isTrue(process.platform === 'win32');
        assert.doesNotThrow(() => process.chdir(origDir.getAbsolutePath()));
        assert.doesNotThrow(() => fs.rmdirSync(testDir));
        return;
      }

      const currentPath = FilePath.safeCurrentPathSync(origDir);
      assert.strictEqual(realpathSync(origDir.getAbsolutePath()), realpathSync(process.cwd()));
      assert.strictEqual(realpathSync(currentPath.getAbsolutePath()), realpathSync(process.cwd()));
    });
    it("safeCurrentPathSync should change to home folder when both cwd and revert paths don't exist", () => {
      const origDir = new FilePath(process.cwd());
      // create a temp folder, chdir to it, then delete it
      let testDir = getTestDir().getAbsolutePath();
      fs.mkdirSync(testDir);
      process.chdir(testDir);
      testDir = realpathSync(testDir);
      try {
        fs.rmdirSync(testDir);
      } catch (error: unknown) {
        // On Windows, trying to remove current-working-directory may fail so can't
        // execute rest of this test case; cleanup and exit
        assert.isTrue(process.platform === 'win32');
        assert.doesNotThrow(() => process.chdir(origDir.getAbsolutePath()));
        assert.doesNotThrow(() => fs.rmdirSync(testDir));
        return;
      }

      const currentPath = FilePath.safeCurrentPathSync(new FilePath(bogusPath));
      assert.strictEqual(realpathSync(currentPath.getAbsolutePath()), realpathSync(os.homedir()));
    });
    it('safeCurrentPath should return current working directory if it exists', async () => {
      const cwd = new FilePath(process.cwd());
      const rootPath = new FilePath('/');
      const currentPath = await FilePath.safeCurrentPath(rootPath);
      assert.strictEqual(currentPath.getAbsolutePath(), cwd.getAbsolutePath());
      assert.strictEqual(await realpath(cwd.getAbsolutePath()), await realpath(process.cwd()));
    });
    it("safeCurrentPath should change to supplied safe path if it exists if cwd doesn't exist", async () => {
      const origDir = new FilePath(process.cwd());
      // create a temp folder, chdir to it, then delete it
      let testDir = getTestDir().getAbsolutePath();
      await fsPromises.mkdir(testDir);
      process.chdir(testDir);
      testDir = await realpath(testDir);
      try {
        await fsPromises.rmdir(testDir);
      } catch (error: unknown) {
        // On Windows, trying to remove current-working-directory may fail so can't
        // execute rest of this test case; cleanup and exit
        assert.isTrue(process.platform === 'win32');
        assert.doesNotThrow(() => process.chdir(origDir.getAbsolutePath()));
        assert.doesNotThrow(() => fs.rmdirSync(testDir));
        return;
      }

      const currentPath = await FilePath.safeCurrentPath(origDir);
      assert.strictEqual(await realpath(origDir.getAbsolutePath()), await realpath(process.cwd()));
      assert.strictEqual(await realpath(currentPath.getAbsolutePath()), await realpath(process.cwd()));
    });
    it("safeCurrentPath should change to home folder when both cwd and revert paths don't exist", async () => {
      const origDir = new FilePath(process.cwd());
      // create a temp folder, chdir to it, then delete it
      let testDir = getTestDir().getAbsolutePath();
      await fsPromises.mkdir(testDir);
      process.chdir(testDir);
      testDir = await realpath(testDir);
      try {
        await fsPromises.rmdir(testDir);
      } catch (error: unknown) {
        // On Windows, trying to remove current-working-directory may fail so can't
        // execute rest of this test case; cleanup and exit
        assert.isTrue(process.platform === 'win32');
        assert.doesNotThrow(() => process.chdir(origDir.getAbsolutePath()));
        assert.doesNotThrow(() => fs.rmdirSync(testDir));
        return;
      }

      const currentPath = await FilePath.safeCurrentPath(new FilePath(bogusPath));
      assert.strictEqual(await realpath(currentPath.getAbsolutePath()), await realpath(os.homedir()));
    });
  });

  describe('Path existence checks', () => {
    it("isEmpty should detect if this object's path is empty", () => {
      assert.isTrue(new FilePath().isEmpty());
    });
    it("existsSync should detect when object's path exists", () => {
      assert.isTrue(new FilePath(os.tmpdir()).existsSync());
    });
    it('existsSync should return false for empty path', () => {
      assert.isFalse(new FilePath('').existsSync());
    });
    it("existsSync should detect when object's path doesn't exist", () => {
      assert.isFalse(new FilePath(bogusPath).existsSync());
    });
    it('existsSync should detect when a supplied path exists', () => {
      assert.isTrue(FilePath.existsSync(os.tmpdir()));
    });
    it("existsSync should detect when a supplied path doesn't exist", () => {
      assert.isFalse(FilePath.existsSync(bogusPath));
    });
    it('existsSync should return false for existence of a null path', () => {
      assert.isFalse(new FilePath().existsSync());
    });
    it('exists should return false for empty path', async () => {
      assert.isFalse(await new FilePath().existsAsync());
    });
    it("exists should detect when object's path exists", async () => {
      assert.isTrue(await new FilePath(os.tmpdir()).existsAsync());
    });
    it("exists should detect when object's path doesn't exist", async () => {
      assert.isFalse(await new FilePath(bogusPath).existsAsync());
    });
    it('exists should detect when a supplied path exists', async () => {
      assert.isTrue(await FilePath.existsAsync(os.tmpdir()));
    });
    it("exists should detect when a supplied path doesn't exist", async () => {
      assert.isFalse(await FilePath.existsAsync(bogusPath));
    });
    it('exists should return false for existence of a null path', async () => {
      assert.isFalse(await new FilePath().existsAsync());
    });
  });

  describe('Synchronous Directory creation', () => {
    it('createDirectorySync should create directory stored in FilePath', () => {
      const target = path.join(os.tmpdir(), randomString());
      const fp = new FilePath(target);
      assert(isSuccessful(fp.createDirectorySync()));
      assert.isTrue(fp.existsSync());
      fs.rmdirSync(target);
    });
    it('createDirectorySync should succeed if directory in FilePath already exists', () => {
      const target = path.join(os.tmpdir(), randomString());
      const fp = new FilePath(target);
      assert(isSuccessful(fp.createDirectorySync()));
      assert.isTrue(fp.existsSync());
      assert(isSuccessful(fp.createDirectorySync()));
      assert.isTrue(fp.existsSync());
      fs.rmdirSync(target);
    });
    it('createDirectorySync should create directory relative to path in FilePath', () => {
      const target = randomString();
      const fp = new FilePath(os.tmpdir());
      assert(isSuccessful(fp.createDirectorySync(target)));
      const newPath = path.join(os.tmpdir(), target);
      assert.isTrue(fs.existsSync(newPath));
      fs.rmdirSync(newPath);
    });
    it('createDirectorySync should recursively create directories', () => {
      const target = path.join(os.tmpdir(), randomString(), randomString());
      const fp = new FilePath(target);
      assert(isSuccessful(fp.createDirectorySync()));
      assert.isTrue(fp.existsSync());
      fs.rmdirSync(target);
    });
    it('createDirectorySync should recursively create directories relative to path in FilePath', () => {
      const firstLevel = randomString();
      const extraFolder = randomString();
      const target = path.join(os.tmpdir(), firstLevel, randomString());
      const fp = new FilePath(target);
      const result = fp.createDirectorySync(extraFolder);
      assert(isSuccessful(result));
      const newPath = path.join(target, extraFolder);
      assert.isTrue(fs.existsSync(newPath));
      fs.rmSync(path.join(os.tmpdir(), firstLevel), { recursive: true });
    });
    it('createDirectorySync should fail when it cannot create the directory', () => {
      const fp = new FilePath(cannotCreatePath);
      assert.isFalse(fp.existsSync());
      let result = fp.createDirectorySync('');
      assert(isFailure(result));
      result = fp.createDirectorySync('stuff');
      assert(isFailure(result));
    });
    it('createDirectorySync should ignore base when given an absolute path', () => {
      const fp = new FilePath(cannotCreatePath);
      assert.isFalse(fp.existsSync());
      const target = path.join(os.tmpdir(), randomString());
      const result = fp.createDirectorySync(target);
      assert(isSuccessful(result));
      assert.isTrue(fs.existsSync(target));
      fs.rmdirSync(target);
    });
    it('ensureDirectorySync should return success when asked to ensure existing directory exists', () => {
      const existingFolder = new FilePath(os.homedir());
      assert.isTrue(existingFolder.existsSync());
      const result = existingFolder.ensureDirectorySync();
      assert(isSuccessful(result));
    });
    it('ensureDirectorySync should create directory when asked to ensure it exists', () => {
      const newFolder = path.join(os.tmpdir(), randomString());
      const newFilePath = new FilePath(newFolder);
      assert.isFalse(fs.existsSync(newFolder));
      const result = newFilePath.ensureDirectorySync();
      assert(isSuccessful(result));
      assert.isTrue(fs.existsSync(newFolder));
      fs.rmdirSync(newFolder);
    });
    it('ensureDirectory should return success when asked to ensure existing directory exists', async () => {
      const existingFolder = new FilePath(os.homedir());
      assert.isTrue(await existingFolder.existsAsync());
      const result = await existingFolder.ensureDirectory();
      assert(isSuccessful(result));
    });
    it('ensureDirectory should create directory when asked to ensure it exists', async () => {
      const newFolder = path.join(os.tmpdir(), randomString());
      const newFilePath = new FilePath(newFolder);
      assert.isFalse(await FilePath.existsAsync(newFolder));
      const result = await newFilePath.ensureDirectory();
      assert(isSuccessful(result));
      assert.isTrue(await FilePath.existsAsync(newFolder));
      await fsPromises.rmdir(newFolder);
    });
  });

  describe('Async Directory creation', () => {
    it('createDirectory should create directory stored in FilePath', async () => {
      const target = path.join(os.tmpdir(), randomString());
      const fp = new FilePath(target);
      const result = await fp.createDirectory();
      assert(isSuccessful(result));
      assert.isTrue(await fp.existsAsync());
      await fsPromises.rmdir(target);
    });
    it('createDirectory should succeed if directory in FilePath already exists', async () => {
      const target = path.join(os.tmpdir(), randomString());
      const fp = new FilePath(target);
      let result = await fp.createDirectory();
      assert.isTrue(await fp.existsAsync());
      result = await fp.createDirectory();
      assert(isSuccessful(result));
      assert.isTrue(await fp.existsAsync());
      await fsPromises.rmdir(target);
    });
    it('createDirectory should create directory relative to path in FilePath', async () => {
      const target = randomString();
      const fp = new FilePath(os.tmpdir());
      assert(isSuccessful(await fp.createDirectory(target)));
      const newPath = path.join(os.tmpdir(), target);
      assert.isTrue(await FilePath.existsAsync(newPath));
      await fsPromises.rmdir(newPath);
    });
    it('createDirectory should recursively create directories', async () => {
      const target = path.join(os.tmpdir(), randomString(), randomString());
      const fp = new FilePath(target);
      const result = await fp.createDirectory();
      assert(isSuccessful(result));
      assert.isTrue(await fp.existsAsync());
      await fsPromises.rmdir(target);
    });
    it('createDirectory should recursively create directories relative to path in FilePath', async () => {
      const firstLevel = randomString();
      const extraFolder = randomString();
      const target = path.join(os.tmpdir(), firstLevel, randomString());
      const fp = new FilePath(target);
      const result = await fp.createDirectory(extraFolder);
      assert(isSuccessful(result));
      const newPath = path.join(target, extraFolder);
      assert.isTrue(await FilePath.existsAsync(newPath));
      await fsPromises.rm(path.join(os.tmpdir(), firstLevel), { recursive: true });
    });
    it('createDirectory should fail when it cannot create the directory', async () => {
      const fp = new FilePath(cannotCreatePath);
      let result = await fp.createDirectory('');
      assert(isFailure(result));
      result = await fp.createDirectory('stuff');
      assert(isFailure(result));
    });
    it('createDirectory should ignore base when given an absolute path', async () => {
      const fp = new FilePath(cannotCreatePath);
      const target = path.join(os.tmpdir(), randomString());
      const result = await fp.createDirectory(target);
      assert(isSuccessful(result));
      assert.isTrue(await FilePath.existsAsync(target));
      await fsPromises.rmdir(target);
    });
  });

  describe('Manipulate current working directory', () => {
    it('makeCurrentPath should change cwd to existing folder', () => {
      const cwd = new FilePath(process.cwd());
      const newFolder = path.join(os.tmpdir(), randomString());
      fs.mkdirSync(newFolder);
      const newFilePath = new FilePath(newFolder);
      const result = newFilePath.makeCurrentPath();
      assert(isSuccessful(result));
      assert.strictEqual(realpathSync(process.cwd()), realpathSync(newFolder));
      process.chdir(cwd.getAbsolutePath());
      fs.rmdirSync(newFolder);
    });
    it('makeCurrentPath with false autocreate flag should fail to change cwd to non-existent folder', () => {
      const cwd = process.cwd();
      const newFolder = path.join(os.tmpdir(), randomString());
      const f1 = new FilePath(newFolder);
      assert.isFalse(fs.existsSync(newFolder));
      const result = f1.makeCurrentPath();
      assert(isFailure(result));
      assert.strictEqual(process.cwd(), cwd);
    });
    it('makeCurrentPath with autocreate should create and change cwd to non-existent folder', () => {
      const origCwd = process.cwd();
      const newFolder = path.join(os.tmpdir(), randomString());
      const f1 = new FilePath(newFolder);
      assert.isFalse(fs.existsSync(newFolder));
      const result = f1.makeCurrentPath(true);
      assert(isSuccessful(result));
      assert.strictEqual(realpathSync(process.cwd()), realpathSync(newFolder));
      process.chdir(origCwd);
      fs.rmdirSync(newFolder);
    });
  });

  describe('Path resolutions', () => {
    it('resolveAliasedPathSync should return home if empty path provided', () => {
      const home = userHomePath();
      const result = FilePath.resolveAliasedPathSync('', home);
      assert.strictEqual(result.getAbsolutePath(), home.getAbsolutePath());
    });
    it("resolveAliasedPathSync should resolve '~' as home", () => {
      const home = userHomePath();
      const result = FilePath.resolveAliasedPathSync('~', home);
      assert.strictEqual(result.getAbsolutePath(), home.getAbsolutePath());
    });
    it("resolveAliasedPathSync should replace '~' in path", () => {
      const start = '~/foo/bar';
      const result = FilePath.resolveAliasedPathSync(start, userHomePath());
      const resultStr = result.getAbsolutePath();
      assert.isAtLeast(resultStr.length, start.length);
      assert.notEqual(resultStr.charAt(0), '~');
      assert.isAbove(resultStr.lastIndexOf('/foo/bar'), -1);
    });
    it('completePath should return absolute path as-is ignoring base', () => {
      const f1 = userHomePath();
      const result = f1.completePath(absolutePath);
      assert.strictEqual(result.getAbsolutePath(), absolutePath);
    });
    it('completePath should resolve relative path to cwd when no base', () => {
      const f1 = new FilePath();
      const result = f1.completePath('some/path');
      const expected = new FilePath(path.join(process.cwd(), 'some/path'));
      assert.strictEqual(result.getAbsolutePath(), expected.getAbsolutePath());
    });
    it('completeChildPath should return same path when no child provided', () => {
      const aPath = new FilePath('/path/to/a');
      const result = aPath.completeChildPath('');
      assert.isTrue(result.equals(aPath));
    });
    it('completeChildPath should correctly handle a simple request', () => {
      const aPathStr = process.platform === 'win32' ? 'C:\\path\\to\\a' : '/path/to/a';
      const bPathStr = process.platform === 'win32' ? 'C:\\path\\to\\a\\b' : '/path/to/a/b';
      const aPath = new FilePath(aPathStr);
      const bPath = new FilePath(bPathStr);
      const result = aPath.completeChildPath('b');
      assert.isTrue(result.equals(bPath));
    });
    it('completeChildPath should not complete a path outside and instead return original path', () => {
      const cPath = new FilePath('/path/to/foo');
      assert.strictEqual(cPath.completeChildPath('../bar'), cPath);
      assert.strictEqual(cPath.completeChildPath('/path/to/quux'), cPath);
    });
    it('Paths only contain forward slashes with no duplicates', () => {
      const paths = ['c:\\www\\app\\my/folder/file.r',
        'C:\\R\\4.1.2\\bin\\\\R.exe',
        'c:\\\\www\\\\app\\my/folder/file.r'];
      paths.forEach((path) => {
        assert.isTrue(path.includes('\\'), `Path ${path} should contain at least a single backward slash for this test to be valid`);
        const normalizedPath = normalizeSeparatorsNative(path);
        assert.isFalse(
          normalizedPath.includes('\\'),
          `Path ${normalizedPath} should NOT contain backward slashes for this test to be valid`,
        );
        assert.isFalse(
          normalizedPath.includes('/'),
          `Path ${normalizedPath} should NOT contain double forward slashes for this test to be valid`,
        );
      });
    });
  });

  describe('Aliased paths', () => {
    it('Paths are aliased correctly', () => {
      const path = new FilePath('/Users/user/path/to/project');
      const home = new FilePath('/Users/user');
      const aliasedPath = FilePath.createAliasedPath(path, home);
      assert(aliasedPath === '~/path/to/project');
    });

    it('Paths are aliased correctly, even with trailing slashes', () => {
      const path = new FilePath('/Users/user/path/to/project');
      const home = new FilePath('/Users/user/');
      const aliasedPath = FilePath.createAliasedPath(path, home);
      assert(aliasedPath === '~/path/to/project');
    });

    it('Paths with differing slashes are aliased correctly', () => {
      const path = new FilePath('//server/home/user/path/to/project');
      const home = new FilePath('\\\\server\\home\\user');
      const aliasedPath = FilePath.createAliasedPath(path, home);
      assert(aliasedPath === '~/path/to/project');
    });

    it('DOS paths are handled', () => {
      const path = new FilePath('C:/Users/user/path/to/project');
      const home = new FilePath('C:\\Users\\user');
      const aliasedPath = FilePath.createAliasedPath(path, home);
      assert(aliasedPath === '~/path/to/project');
    });

    it('Original path returned is aliasing fails', () => {
      const path = new FilePath('/home/other/path/to/project');
      const home = new FilePath('/home/user');
      const aliasedPath = FilePath.createAliasedPath(path, home);
      assert(new FilePath(aliasedPath).getAbsolutePath() === path.getAbsolutePath());
    });
  });

  describe('Remove file and folders', () => {
    it('remove deletes an existing folder', async () => {
      const testDir = getTestDir();
      await testDir.createDirectory();
      assert.isTrue(await testDir.existsAsync());
      assert(isSuccessful(await testDir.remove()));
      assert.isFalse(await testDir.existsAsync());
    });
    it('remove fails if asked to delete a non-existing folder', async () => {
      const testDir = getTestDir();
      assert.isFalse(await testDir.existsAsync());
      assert(isFailure(await testDir.remove()));
    });

    it('removeSync deletes an existing folder', () => {
      const testDir = getTestDir();
      testDir.createDirectorySync();
      assert.isTrue(testDir.existsSync());
      assert(isSuccessful(testDir.removeSync()));
      assert.isFalse(testDir.existsSync());
    });
    it('removeSync fails if asked to delete a non-existing folder', () => {
      const testDir = getTestDir();
      assert.isFalse(testDir.existsSync());
      assert(isFailure(testDir.removeSync()));
    });

    it('removeIfExists deletes an existing folder', async () => {
      const testDir = getTestDir();
      await testDir.createDirectory();
      assert.isTrue(await testDir.existsAsync());
      assert(isSuccessful(await testDir.removeIfExists()));
      assert.isFalse(await testDir.existsAsync());
    });
    it('removeIfExists returns success if asked to delete a non-existing folder', async () => {
      const testDir = getTestDir();
      assert.isFalse(await testDir.existsAsync());
      assert(isSuccessful(await testDir.removeIfExists()));
    });

    it('removeIfExistsSync deletes an existing folder', () => {
      const testDir = getTestDir();
      testDir.createDirectorySync();
      assert.isTrue(testDir.existsSync());
      assert(isSuccessful(testDir.removeIfExistsSync()));
      assert.isFalse(testDir.existsSync());
    });
    it('removeIfExistsSync returns success if asked to delete a non-existing folder', () => {
      const testDir = getTestDir();
      assert.isFalse(testDir.existsSync());
      assert(isSuccessful(testDir.removeIfExistsSync()));
    });
  });

  describe('enumerate children', () => {
    it("getChildren returns error if it doesn't exist", () => {
      const testDir = getTestDir();
      const children: FilePath[] = [];
      assert(isFailure(testDir.getChildren(children)));
    });
    it("getChildren returns error if it isn' a directory", () => {
      const testDir = getTestDir();
      testDir.createDirectorySync();
      const fileName = testDir.completeChildPath(randomString());
      fs.writeFileSync(fileName.getAbsolutePath(), 'This is a test file.');
      const children: FilePath[] = [];
      assert(isFailure(fileName.getChildren(children)));
      fileName.removeIfExistsSync();
      testDir.removeIfExistsSync();
    });
    it('getChildren handles case with no children', () => {
      const testDir = getTestDir();
      testDir.createDirectorySync();
      const children: FilePath[] = [];
      assert(isSuccessful(testDir.getChildren(children)));
      assert.equal(0, children.length);
      testDir.removeIfExistsSync();
    });
    it('getChildren handles case with single child file', () => {
      const testDir = getTestDir();
      testDir.createDirectorySync();
      const fileName = testDir.completeChildPath(randomString());
      fs.writeFileSync(fileName.getAbsolutePath(), 'This is a test file.');
      const children: FilePath[] = [];
      assert(isSuccessful(testDir.getChildren(children)));
      assert.equal(1, children.length);
      assert.equal(fileName.getAbsolutePath(), children[0].getAbsolutePath());
      fileName.removeIfExistsSync();
      testDir.removeIfExistsSync();
    });
    it('getChildren handles case with multiple child files', () => {
      const testDir = getTestDir();
      testDir.createDirectorySync();
      const fileName1 = testDir.completeChildPath(randomString());
      fs.writeFileSync(fileName1.getAbsolutePath(), 'This is test file one.');
      const fileName2 = testDir.completeChildPath(randomString());
      fs.writeFileSync(fileName2.getAbsolutePath(), 'This is test file two.');
      const children: FilePath[] = [];
      assert(isSuccessful(testDir.getChildren(children)));
      assert.equal(2, children.length);
      fileName1.removeIfExistsSync();
      fileName2.removeIfExistsSync();
      testDir.removeIfExistsSync();
    });
    it('getChildren handles case with child directory', () => {
      const testDir = getTestDir();
      testDir.createDirectorySync();
      const subDir = testDir.completeChildPath(randomString());
      fs.mkdirSync(subDir.getAbsolutePath());
      const children: FilePath[] = [];
      assert(isSuccessful(testDir.getChildren(children)));
      assert.equal(1, children.length);
      assert.equal(subDir.getAbsolutePath(), children[0].getAbsolutePath());
      subDir.removeIfExistsSync();
      testDir.removeIfExistsSync();
    });
  });

  describe('NYI placeholders', () => {
    it('sync methods should throw exception', () => {
      const fp1 = new FilePath();
      const fp2 = new FilePath();
      assert.throws(() => FilePath.isEqualCaseInsensitive(fp1, fp2));
      assert.throws(() => FilePath.isRootPath('/'));
      assert.throws(() => FilePath.makeCurrent('/'));
      assert.throws(() => FilePath.tempFilePath());
      assert.throws(() => FilePath.uniqueFilePath('/'));
      if (process.platform === 'win32') {
        assert.doesNotThrow(() => fp1.changeFileMode(''));
      } else {
        assert.throws(() => fp1.changeFileMode(''));
      }
      assert.throws(() => fp1.copy(fp2));
      assert.throws(() => fp1.copyDirectoryRecursive(fp2));
      assert.throws(() => fp1.ensureFile());
      assert.throws(() => fp1.getChildrenRecursive());
      assert.throws(() => fp1.getFileMode());
      assert.throws(() => fp1.getMimeContentType());
      assert.throws(() => fp1.getRelativePath(fp2));
      assert.throws(() => fp1.getSize());
      assert.throws(() => fp1.getSizeRecursive());
      assert.throws(() => fp1.hasExtension('.txt'));
      assert.throws(() => fp1.hasExtensionLowerCase('.txt'));
      assert.throws(() => fp1.hasTextMimeType());
      assert.throws(() => fp1.isEquivalentTo(fp2));
      assert.throws(() => fp1.isHidden());
      assert.throws(() => fp1.isJunction());
      assert.throws(() => fp1.isReadable());
      assert.throws(() => fp1.isRegularFile());
      assert.throws(() => fp1.isSymlink());
      assert.throws(() => fp1.move());
      assert.throws(() => fp1.moveIndirect(fp2));
      assert.throws(() => fp1.openForRead());
      assert.throws(() => fp1.openForWrite());
      assert.throws(() => fp1.resetDirectory());
      assert.throws(() => fp1.resolveSymlink());
      assert.throws(() => fp1.setLastWriteTime());
      assert.throws(() => fp1.testWritePermissions());
    });
  });
});
