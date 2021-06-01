/*
 * file-path.spec.ts
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
import { expect } from 'chai';

import fs from 'fs';
import fsPromises from 'fs/promises';
import path from 'path';
import os from 'os';

import { FilePath } from '../../src/core/file-path';
import { User } from '../../src/core/user';

function randomString() {
  return Math.trunc(Math.random() * 2147483647).toString();
}

function realpathSync(path: string): string {
  return fs.realpathSync(path);
}

async function realpath(path: string): Promise<string> {
  return fsPromises.realpath(path);
}

// A path that should never exist
const bogusPath = '/super/bogus/path/42';

// A path that a non-elevated user cannot create
const cannotCreatePath = process.platform === 'win32' ? 'C:\\Program Files\\a_test_folder' : '/foo/bar/crazy';

// An absolute path in generic format (see boost filesystem for description of "generic" format)
const absolutePath = process.platform === 'win32' ? 'C:/Users/human/documents' : '/users/human/documents';

describe('FilePath', () => {
  afterEach(() => {
    // make sure we leave cwd in a valid place
    process.chdir(__dirname);
  });

  describe('Constructor checks', () => {
    it('Should store and return the supplied path', () => {
      const path1= 'hello/world';
      const path2 = '~/foo';
      const path3 = '/once/upon/a/time';
      const path4 = '~';
      expect(new FilePath(path1).getAbsolutePath()).to.equal(path1);
      expect(new FilePath(path2).getAbsolutePath()).to.equal(path2);
      expect(new FilePath(path3).getAbsolutePath()).to.equal(path3);
      expect(new FilePath(path4).getAbsolutePath()).to.equal(path4);
      expect(new FilePath(path3).toString()).to.equal(path3);
    });
    it('Should create empty path when given no arguments', () => {
      const path = new FilePath();
      expect(path.getAbsolutePath().length).to.equal(0);
      expect(path.isEmpty());
    });
    it('getAbsolutePathNative should return raw path on Windows', () => {
      const originalPath = 'C:\\Windows\\Was\\Here';
      const expectedPosix = 'C:/Windows/Was/Here';
      const fp1 = new FilePath(originalPath);
      const native = fp1.getAbsolutePathNative();
      expect(native).equals(process.platform === 'win32' ? originalPath : expectedPosix);
    });
  });

  describe('Comparisons', () => {
    it('equals should return true if storing exact same path string', () => {
      const path1 = new FilePath('/hello/world');
      const path2 = new FilePath('/hello/world');
      expect(path1.equals(path2)).is.true;
    });
    it('equals should return false if they are storing different path strings', () => {
      const path1 = new FilePath('/hello/world');
      const path2 = new FilePath('/hello/../hello/world');
      expect(path1.equals(path2)).is.false;
    });
    it('equals should return true if both are empty', () => {
      const path1 = new FilePath();
      const path2 = new FilePath();
      expect(path1.equals(path2)).is.true;
    });
    it('isWithin should handle detect simple path containment checks', () => {
      const pPath = new FilePath('/path/to');
      const aPath = new FilePath('/path/to/a');
      const bPath = new FilePath('/path/to/b');
      expect(aPath.isWithin(pPath)).is.true;
      expect(bPath.isWithin(pPath)).is.true;
      expect(aPath.isWithin(bPath)).is.false;
    });
    it('isWithin should not be fooled by directory traversal', () => {
      // the first path is not inside the second even though it appears to be lexically
      const aPath = new FilePath('/path/to/a/../b');
      const bPath = new FilePath('path/to/a');
      expect(!!aPath.isWithin(bPath)).is.false;
    });
    it('isWithin should not be fooled by substrings', () => {
      const cPath = new FilePath('/path/to/foo');
      const dPath = new FilePath('path/to/foobar');
      expect(dPath.isWithin(cPath)).is;
    });
  });

  describe('Retrieval methods', () => {
    it('getCanonicalPathSync should return empty results for empty path', () => {
      const f = new FilePath();
      expect(f.existsSync()).is.false;
      expect(!!f.getAbsolutePath()).is.false;
      expect(!!f.isEmpty()).is.true;
      expect(!!f.isWithin(f)).is.true;
      expect(!!f.isWithin(new FilePath('/some/path'))).is.false;
      expect(f.getCanonicalPathSync()).is.empty;
      expect(f.getExtension()).is.empty;
      expect(f.getExtensionLowerCase()).is.empty;
      expect(f.getFilename()).is.empty;
    });
    it('getCanonicalPathSync should return a non-empty path for a path that exists', () => {
      const f = new FilePath(os.tmpdir());
      expect(f.existsSync()).is.true;
      const result = f.getCanonicalPathSync();
      expect(result).length.is.greaterThan(0);
    });
    it('getCanonicalPathSync should return an empty path for a path that doesn\'t exist', () => {
      const f = new FilePath('/some/really/bogus/path');
      expect(f.existsSync()).is.false;
      const result = f.getCanonicalPathSync();
      expect(result).length.lessThanOrEqual(0);
    });
    it('getCanonicalPath should return empty results for empty path', async () => {
      const f = new FilePath();
      expect(await f.exists()).is.false;
      expect(!!f.getAbsolutePath()).is.false;
      expect(!!f.isEmpty()).is.true;
      expect(!!f.isWithin(f)).is.true;
      expect(!!f.isWithin(new FilePath('/some/path'))).is.false;
      expect(await f.getCanonicalPath()).is.empty;
    });
    it('getCanonicalPath should return a non-empty path for a path that exists', async () => {
      const f = new FilePath(os.tmpdir());
      expect(await f.exists()).is.true;
      const result = await f.getCanonicalPath();
      expect(result).length.is.greaterThan(0);
    });
    it('getCanonicalPath should return an empty path for a path that doesn\'t exist', async () => {
      const f = new FilePath('/some/really/bogus/path');
      expect(await f.exists()).is.false;
      const result = await f.getCanonicalPath();
      expect(result).length.lessThanOrEqual(0);
    });
    it('getExtension should return the extension including leading period', () => {
      const f = new FilePath('/some/stuff/hello.tXt');
      expect(f.getExtension()).equals('.tXt');
    });
    it('getExtension should return no extension for extension-only filename', () => {
      const f = new FilePath('/some/stuff/.foo');
      expect(f.getExtension()).is.empty;
    });
    it('getExtension should return blank extension for file without extension', () => {
      const f = new FilePath('/some/stuff/hello');
      expect(f.getExtension()).is.empty;
    });
    it('getExtensionLowerCase should return the extension in lowercase', () => {
      const f = new FilePath('/some/stuff/hello.tXt');
      expect(f.getExtensionLowerCase()).equals('.txt');
    });
    it('getExtensionLowerCase should return blank extension for file without extension', () => {
      const f = new FilePath('/some/stuff/hello');
      expect(f.getExtension()).is.empty;
    });
    it('getFilename should return filename including extension', () => {
      const f = new FilePath('/etc/foo/hello.txt.world');
      expect(f.getFilename()).equals('hello.txt.world');
    });
    it('getFilename should return filename when there is no extension', () => {
      const f = new FilePath('/etc/foo/hello');
      expect(f.getFilename()).equals('hello');
    });
    it('getFilename should return filename for extension-only filename', () => {
      const f = new FilePath('/etc/foo/.hello');
      expect(f.getFilename()).equals('.hello');
    });
    it('getLastWriteTimeSync should return zero for non-existent file', () => {
      const f = new FilePath('/some/file/that/will/not/exist');
      expect(f.getLastWriteTimeSync()).equals(0);
    });
  });

  describe('Get a safe current path', () => {
    it('safeCurrentPathSync should return current working directory if it exists', () => {
      const cwd = new FilePath(process.cwd());
      const rootPath = new FilePath('/');
      const currentPath = FilePath.safeCurrentPathSync(rootPath);
      expect(currentPath.getAbsolutePath()).to.equal(cwd.getAbsolutePath());
      expect(realpathSync(cwd.getAbsolutePath())).to.equal(realpathSync(process.cwd()));
    });
    it('safeCurrentPathSync should change to supplied safe path if it exists if cwd doesn\'t exist', () => {
      const origDir = new FilePath(process.cwd());
      // create a temp folder, chdir to it, then delete it
      let testDir = path.join(
        os.tmpdir(),
        'temp-folder-for-FilePath-tests-' + randomString()
      );
      fs.mkdirSync(testDir);
      process.chdir(testDir);
      testDir = realpathSync(testDir);
      try {
        fs.rmdirSync(testDir);
      }
      catch (error) {
        // On Windows, trying to remove current-working-directory may fail so can't 
        // execute rest of this test case; cleanup and exit
        expect(process.platform === 'win32').is.true;
        expect(() => process.chdir(origDir.getAbsolutePath())).to.not.throw();
        expect(() => fs.rmdirSync(testDir)).to.not.throw();
        return;
      }

      const currentPath = FilePath.safeCurrentPathSync(origDir);
      expect(realpathSync(origDir.getAbsolutePath())).to.equal(realpathSync(process.cwd()));
      expect(realpathSync(currentPath.getAbsolutePath())).to.equal(realpathSync(process.cwd()));
    });
    it('safeCurrentPathSync should change to home folder when both cwd and revert paths don\'t exist', () => {
      const origDir = new FilePath(process.cwd());
      // create a temp folder, chdir to it, then delete it
      let testDir = path.join(
        os.tmpdir(),
        'temp-folder-for-FilePath-tests-' + randomString()
      );
      fs.mkdirSync(testDir);
      process.chdir(testDir);
      testDir = realpathSync(testDir);
      try {
        fs.rmdirSync(testDir);
      }
      catch (error) {
        // On Windows, trying to remove current-working-directory may fail so can't 
        // execute rest of this test case; cleanup and exit
        expect(process.platform === 'win32').is.true;
        expect(() => process.chdir(origDir.getAbsolutePath())).to.not.throw();
        expect(() => fs.rmdirSync(testDir)).to.not.throw();
        return;
      }

      const currentPath = FilePath.safeCurrentPathSync(new FilePath(bogusPath));
      expect(realpathSync(currentPath.getAbsolutePath())).to.equal(realpathSync(os.homedir()));
    });
    it('safeCurrentPath should return current working directory if it exists', async () => {
      const cwd = new FilePath(process.cwd());
      const rootPath = new FilePath('/');
      const currentPath = await FilePath.safeCurrentPath(rootPath);
      expect(currentPath.getAbsolutePath()).to.equal(cwd.getAbsolutePath());
      expect(await realpath(cwd.getAbsolutePath())).to.equal(await realpath(process.cwd()));
    });
    it('safeCurrentPath should change to supplied safe path if it exists if cwd doesn\'t exist', async () => {
      const origDir = new FilePath(process.cwd());
      // create a temp folder, chdir to it, then delete it
      let testDir = path.join(
        os.tmpdir(),
        'temp-folder-for-FilePath-tests-' + randomString()
      );
      await fsPromises.mkdir(testDir);
      process.chdir(testDir);
      testDir = await realpath(testDir);
      try {
        await fsPromises.rmdir(testDir);
      }
      catch (error) {
        // On Windows, trying to remove current-working-directory may fail so can't 
        // execute rest of this test case; cleanup and exit
        expect(process.platform === 'win32').is.true;
        expect(() => process.chdir(origDir.getAbsolutePath())).to.not.throw();
        expect(() => fs.rmdirSync(testDir)).to.not.throw();
        return;
      }

      const currentPath = await FilePath.safeCurrentPath(origDir);
      expect(await realpath(origDir.getAbsolutePath())).to.equal(await realpath(process.cwd()));
      expect(await realpath(currentPath.getAbsolutePath())).to.equal(await realpath(process.cwd()));
    });
    it('safeCurrentPath should change to home folder when both cwd and revert paths don\'t exist', async () => {
      const origDir = new FilePath(process.cwd());
      // create a temp folder, chdir to it, then delete it
      let testDir = path.join(
        os.tmpdir(),
        'temp-folder-for-FilePath-tests-' + randomString()
      );
      await fsPromises.mkdir(testDir);
      process.chdir(testDir);
      testDir = await realpath(testDir);
      try {
        await fsPromises.rmdir(testDir);
      }
      catch (error) {
        // On Windows, trying to remove current-working-directory may fail so can't 
        // execute rest of this test case; cleanup and exit
        expect(process.platform === 'win32').is.true;
        expect(() => process.chdir(origDir.getAbsolutePath())).to.not.throw();
        expect(() => fs.rmdirSync(testDir)).to.not.throw();
        return;
      }

      const currentPath = await FilePath.safeCurrentPath(new FilePath(bogusPath));
      expect(await realpath(currentPath.getAbsolutePath())).to.equal(await realpath(os.homedir()));
    });
  });

  describe('Path existence checks', () => {
    it('isEmpty should detect if this object\'s path is empty', () => {
      expect(new FilePath().isEmpty()).is.true;
    });
    it('existsSync should detect when object\'s path exists', () => {
      expect(new FilePath(os.tmpdir()).existsSync()).is.true;
    });
    it('existsSync should return false for empty path', () => {
      expect(new FilePath('').existsSync()).is.false;
    });
    it('existsSync should detect when object\'s path doesn\'t exist', () => {
      expect(new FilePath(bogusPath).existsSync()).is.false;
    });
    it('existsSync should detect when a supplied path exists', () => {
      expect(FilePath.existsSync(os.tmpdir())).is.true;
    });
    it('existsSync should detect when a supplied path doesn\'t exist', () => {
      expect(FilePath.existsSync(bogusPath)).is.false;
    });
    it('existsSync should return false for existence of a null path', () => {
      expect(new FilePath().existsSync()).is.false;
    });
    it('exists should return false for empty path', async () => {
      expect(await new FilePath().exists()).is.false;
    });
    it('exists should detect when object\'s path exists', async () => {
      expect(await new FilePath(os.tmpdir()).exists()).is.true;
    });
    it('exists should detect when object\'s path doesn\'t exist', async () => {
      expect(await new FilePath(bogusPath).exists()).is.false;
    });
    it('exists should detect when a supplied path exists', async () => {
      expect(await FilePath.exists(os.tmpdir())).is.true;
    });
    it('exists should detect when a supplied path doesn\'t exist', async () => {
      expect(await FilePath.exists(bogusPath)).is.false;
    });
    it('exists should return false for existence of a null path', async () => {
      expect(await new FilePath().exists()).is.false;
    });
  });

  describe('Synchronous Directory creation', () => {
    it('createDirectorySync should create directory stored in FilePath', () => {
      const target = path.join(os.tmpdir(), randomString());
      const fp = new FilePath(target);
      const result = fp.createDirectorySync();
      expect(!!result).is.false;
      expect(fp.existsSync()).is.true;
      fs.rmdirSync(target);
    });
    it('createDirectorySync should succeed if directory in FilePath already exists', () => {
      const target = path.join(os.tmpdir(), randomString());
      const fp = new FilePath(target);
      let result = fp.createDirectorySync();
      expect(fp.existsSync()).is.true;
      result = fp.createDirectorySync();
      expect(!!result).is.false;
      expect(fp.existsSync()).is.true;
      fs.rmdirSync(target);
    });
    it('createDirectorySync should create directory relative to path in FilePath', () => {
      const target = randomString();
      const fp = new FilePath(os.tmpdir());
      const result = fp.createDirectorySync(target);
      expect(!!result).is.false;
      const newPath = path.join(os.tmpdir(), target);
      expect(fs.existsSync(newPath)).is.true;
      fs.rmdirSync(newPath);
    });
    it('createDirectorySync should recursively create directories', () => {
      const target = path.join(os.tmpdir(), randomString(), randomString());
      const fp = new FilePath(target);
      const result = fp.createDirectorySync();
      expect(!!result).is.false;
      expect(fp.existsSync()).is.true;
      fs.rmdirSync(target);
    });
    it('createDirectorySync should recursively create directories relative to path in FilePath', () => {
      const firstLevel = randomString();
      const extraFolder = randomString();
      const target = path.join(os.tmpdir(), firstLevel, randomString());
      const fp = new FilePath(target);
      const result = fp.createDirectorySync(extraFolder);
      expect(!!result).is.false;
      const newPath = path.join(target, extraFolder);
      expect(fs.existsSync(newPath)).is.true;
      fs.rmdirSync(path.join(os.tmpdir(), firstLevel), { recursive: true });
    });
    it('createDirectorySync should fail when it cannot create the directory', () => {
      const fp = new FilePath(cannotCreatePath);
      expect(fp.existsSync()).is.false;
      let result = fp.createDirectorySync('');
      expect(!!result).is.true;
      result = fp.createDirectorySync('stuff');
      expect(!!result).is.true;
    });
    it('createDirectorySync should ignore base when given an absolute path', () => {
      const fp = new FilePath(cannotCreatePath);
      expect(fp.existsSync()).is.false;
      const target = path.join(os.tmpdir(), randomString());
      const result = fp.createDirectorySync(target);
      expect(!!result).is.false;
      expect(fs.existsSync(target));
      fs.rmdirSync(target);
    });
    it('ensureDirectorySync should return success when asked to ensure existing directory exists', () => {
      const existingFolder = new FilePath(os.homedir());
      expect(existingFolder.existsSync()).is.true;
      const result = existingFolder.ensureDirectorySync();
      expect(!!result).is.false;
    });
    it('ensureDirectorySync should create directory when asked to ensure it exists', () => {
      const newFolder = path.join(os.tmpdir(), randomString());
      const newFilePath = new FilePath(newFolder);
      expect(fs.existsSync(newFolder)).is.false;
      const result = newFilePath.ensureDirectorySync();
      expect(!!result).is.false;
      expect(fs.existsSync(newFolder)).is.true;
      fs.rmdirSync(newFolder);
    });
    it('ensureDirectory should return success when asked to ensure existing directory exists', async () => {
      const existingFolder = new FilePath(os.homedir());
      expect(await existingFolder.exists()).is.true;
      const result = await existingFolder.ensureDirectory();
      expect(!!result).is.false;
    });
    it('ensureDirectory should create directory when asked to ensure it exists', async () => {
      const newFolder = path.join(os.tmpdir(), randomString());
      const newFilePath = new FilePath(newFolder);
      expect(await FilePath.exists(newFolder)).is.false;
      const result = await newFilePath.ensureDirectory();
      expect(!!result).is.false;
      expect(await FilePath.exists(newFolder)).is.true;
      await fsPromises.rmdir(newFolder);
    });
  });

  describe('Async Directory creation', () => {
    it('createDirectory should create directory stored in FilePath', async () => {
      const target = path.join(os.tmpdir(), randomString());
      const fp = new FilePath(target);
      const result = await fp.createDirectory();
      expect(!!result).is.false;
      expect(await fp.exists()).is.true;
      await fsPromises.rmdir(target);
    });
    it('createDirectory should succeed if directory in FilePath already exists', async () => {
      const target = path.join(os.tmpdir(), randomString());
      const fp = new FilePath(target);
      let result = await fp.createDirectory();
      expect(await fp.exists()).is.true;
      result = await fp.createDirectory();
      expect(!!result).is.false;
      expect(await fp.exists()).is.true;
      await fsPromises.rmdir(target);
    });
    it('createDirectory should create directory relative to path in FilePath', async () => {
      const target = randomString();
      const fp = new FilePath(os.tmpdir());
      const result = await fp.createDirectory(target);
      expect(!!result).is.false;
      const newPath = path.join(os.tmpdir(), target);
      expect(await FilePath.exists(newPath)).is.true;
      await fsPromises.rmdir(newPath);
    });
    it('createDirectory should recursively create directories', async () => {
      const target = path.join(os.tmpdir(), randomString(), randomString());
      const fp = new FilePath(target);
      const result = await fp.createDirectory();
      expect(!!result).is.false;
      expect(await fp.exists()).is.true;
      await fsPromises.rmdir(target);
    });
    it('createDirectory should recursively create directories relative to path in FilePath', async () => {
      const firstLevel = randomString();
      const extraFolder = randomString();
      const target = path.join(os.tmpdir(), firstLevel, randomString());
      const fp = new FilePath(target);
      const result = await fp.createDirectory(extraFolder);
      expect(!!result).is.false;
      const newPath = path.join(target, extraFolder);
      expect(await FilePath.exists(newPath)).is.true;
      await fsPromises.rmdir(path.join(os.tmpdir(), firstLevel), { recursive: true });
    });
    it('createDirectory should fail when it cannot create the directory', async () => {
      const fp = new FilePath(cannotCreatePath);
      let result = await fp.createDirectory('');
      expect(!!result).is.true;
      result = await fp.createDirectory('stuff');
      expect(!!result).is.true;
    });
    it('createDirectory should ignore base when given an absolute path', async () => {
      const fp = new FilePath(cannotCreatePath);
      const target = path.join(os.tmpdir(), randomString());
      const result = await fp.createDirectory(target);
      expect(!!result).is.false;
      expect(await FilePath.exists(target));
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
      expect(!!result).is.false;
      expect(realpathSync(process.cwd())).equals(realpathSync(newFolder));
      process.chdir(cwd.getAbsolutePath());
      fs.rmdirSync(newFolder);
    });
    it('makeCurrentPath with false autocreate flag should fail to change cwd to non-existent folder', () => {
      const cwd = process.cwd();
      const newFolder = path.join(os.tmpdir(), randomString());
      const f1 = new FilePath(newFolder);
      expect(fs.existsSync(newFolder)).is.false;
      const result = f1.makeCurrentPath();
      expect(!!result).is.true;
      expect(process.cwd()).equals(cwd);
    });
    it('makeCurrentPath with autocreate should create and change cwd to non-existent folder', () => {
      const origCwd = process.cwd();
      const newFolder = path.join(os.tmpdir(), randomString());
      const f1 = new FilePath(newFolder);
      expect(fs.existsSync(newFolder)).is.false;
      const result = f1.makeCurrentPath(true);
      expect(!!result).is.false;
      expect(realpathSync(process.cwd())).equals(realpathSync(newFolder));
      process.chdir(origCwd);
      fs.rmdirSync(newFolder);
    });
  });

  describe('Path resolutions', () => {
    it('resolveAliasedPathSync should return home if empty path provided', () => {
      const home = User.getUserHomePath();
      const result = FilePath.resolveAliasedPathSync('', home);
      expect(result.getAbsolutePath()).equals(home.getAbsolutePath());
    });
    it('resolveAliasedPathSync should resolve \'~\' as home', () => {
      const home = User.getUserHomePath();
      const result = FilePath.resolveAliasedPathSync('~', home);
      expect(result.getAbsolutePath()).equals(home.getAbsolutePath());
    });
    it('resolveAliasedPathSync should replace \'~\' in path', () => {
      const start = '~/foo/bar';
      const result = FilePath.resolveAliasedPathSync(start, User.getUserHomePath());
      const resultStr = result.getAbsolutePath();
      expect(resultStr.length).is.greaterThanOrEqual(start.length);
      expect(resultStr.charAt(0)).is.not.equals('~');
      expect(resultStr.lastIndexOf('/foo/bar')).is.greaterThan(-1);
    });
    it('completePath should return absolute path as-is ignoring base', () => {
      const f1 = User.getUserHomePath();
      const result = f1.completePath(absolutePath);
      expect(result.getAbsolutePath()).equals(absolutePath);
    });
    it('completePath should resolve relative path to cwd when no base', () => {
      const f1 = new FilePath();
      const result = f1.completePath('some/path');
      const expected = new FilePath(path.join(process.cwd(), 'some/path'));
      expect(result.getAbsolutePath()).equals(expected.getAbsolutePath());
    });
    it('completeChildPath should return same path when no child provided', () => {
      const aPath = new FilePath('/path/to/a');
      const result = aPath.completeChildPath('');
      expect(result.equals(aPath)).is.true;
    });
    it('completeChildPath should correctly handle a simple request', () => {
      const aPathStr = process.platform === 'win32' ? 'C:\\path\\to\\a' : '/path/to/a';
      const bPathStr = process.platform === 'win32' ? 'C:\\path\\to\\a\\b' : '/path/to/a/b';
      const aPath = new FilePath(aPathStr);
      const bPath = new FilePath(bPathStr);
      const result = aPath.completeChildPath('b');
      console.log(`result of completeChildPath is ${result.getAbsolutePath()}`);
      expect(result.equals(bPath)).is.true;
    });
    it('completeChildPath should not complete a path outside and instead return original path', () => {
      const cPath = new FilePath('/path/to/foo');
      expect(cPath.completeChildPath('../bar').equals(cPath)).is.true;
      expect(cPath.completeChildPath('/path/to/quux').equals(cPath));
    });
  });
  describe('NYI placeholders', () => {
    it('sync methods should throw exception', () => {
      const fp1 = new FilePath();
      const fp2 = new FilePath();
      expect(() => FilePath.createAliasedPath(fp1, fp2)).to.throw();
      expect(() => FilePath.isEqualCaseInsensitive(fp1, fp2)).to.throw();
      expect(() => FilePath.isRootPath('/')).to.throw();
      expect(() =>FilePath.makeCurrent('/')).to.throw();
      expect(() => FilePath.tempFilePath()).to.throw();
      expect(() => FilePath.uniqueFilePath('/')).to.throw();
      if (process.platform === 'win32') {
        expect(() => fp1.changeFileMode('')).to.not.throw();
      } else {
        expect(() => fp1.changeFileMode('')).to.throw();
      }
      expect(() => fp1.copy(fp2)).to.throw();
      expect(() => fp1.copyDirectoryRecursive(fp2)).to.throw();
      expect(() => fp1.ensureFile()).to.throw();
      expect(() => fp1.getChildren(new Array<FilePath>())).to.throw();
      expect(() => fp1.getChildrenRecursive()).to.throw();
      expect(() => fp1.getFileMode()).to.throw();
      expect(() => fp1.getMimeContentType()).to.throw();
      expect(() => fp1.getParent()).to.throw();
      expect(() => fp1.getRelativePath(fp2)).to.throw();
      expect(() => fp1.getSize()).to.throw();
      expect(() => fp1.getSizeRecursive()).to.throw();
      expect(() => fp1.getStem()).to.throw();
      expect(() => fp1.hasExtension('.txt')).to.throw();
      expect(() => fp1.hasExtensionLowerCase('.txt')).to.throw();
      expect(() => fp1.hasTextMimeType()).to.throw();
      expect(() => fp1.isDirectory()).to.throw();
      expect(() => fp1.isEquivalentTo(fp2)).to.throw();
      expect(() => fp1.isHidden()).to.throw();
      expect(() => fp1.isJunction()).to.throw();
      expect(() => fp1.isReadable()).to.throw();
      expect(() => fp1.isRegularFile()).to.throw();
      expect(() => fp1.isSymlink()).to.throw();
      expect(() => fp1.move()).to.throw();
      expect(() => fp1.moveIndirect(fp2)).to.throw();
      expect(() => fp1.openForRead()).to.throw();
      expect(() => fp1.openForWrite()).to.throw();
      expect(() => fp1.remove()).to.throw();
      expect(() => fp1.removeIfExists()).to.throw();
      expect(() => fp1.resetDirectory()).to.throw();
      expect(() => fp1.resolveSymlink()).to.throw();
      expect(() => fp1.setLastWriteTime()).to.throw();
      expect(() => fp1.testWritePermissions()).to.throw();
    });
  });
});
