/*
 * xdg.test.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
import { saveAndClear, restore } from '../unit-utils';

import fs from 'fs';
import os from 'os';
import path from 'path';

import { FilePath } from '../../../src/core/file-path';
import { checkDirectoryWritable, Xdg, SHGetKnownFolderPath, WinFolderID } from '../../../src/core/xdg';

function folder() {
  return process.platform === 'win32' ? 'RStudio' : 'rstudio';
}

function hasExpectedEnding(path: FilePath) {
  return path.getAbsolutePath().endsWith(folder());
}

function randomString() {
  return Math.trunc(Math.random() * 2147483647).toString();
}

function addDrive(path: string): string {
  if (process.platform === 'win32') {
    return 'C:' + path;
  } else {
    return path;
  }
}

describe('Xdg', () => {
  // store env values at start of each test so we can restore them
  const xdgVars: Record<string, string> = {
    RSTUDIO_CONFIG_HOME: '',
    RSTUDIO_CONFIG_DIR: '',
    RSTUDIO_DATA_HOME: '',
    RSTUDIO_DATA_DIR: '',
    XDG_CONFIG_HOME: '',
    XDG_CONFIG_DIRS: '',
    XDG_DATA_HOME: '',
    XDG_DATA_DIRS: '',
  };

  // save and clear any XDG-related env vars
  beforeEach(() => {
    saveAndClear(xdgVars);
  });

  // put back the original XDG env vars
  afterEach(() => {
    restore(xdgVars);
  });

  describe('Fetch Xdg paths', () => {
    it('userConfigDir returns default path', () => {
      const result = Xdg.userConfigDir();
      assert.isTrue(hasExpectedEnding(result));
      assert.isAbove(result.getAbsolutePath().length, folder().length);
    });
    it('userConfigDir returns path set with RSTUDIO_CONFIG_HOME', () => {
      const p = addDrive('/once/upon/a/time/rstudio');
      process.env.RSTUDIO_CONFIG_HOME = p;
      const result = Xdg.userConfigDir();
      assert.strictEqual(result.getAbsolutePath(), p);
    });
    it('userConfigDir returns path set with XDG_CONFIG_HOME', () => {
      const p = addDrive('/once/upon/a/time/rstudio');
      process.env.XDG_CONFIG_HOME = p;
      const result = Xdg.userConfigDir();
      assert.isTrue(result.getAbsolutePath().startsWith(p));
      assert.isTrue(hasExpectedEnding(result));
    });
    it('userConfigDir expands placeholders', () => {
      const p = '${HOME}/a/$USER/stuff';
      const user = 'fred';
      const home = addDrive('/somewhere/nifty');
      const expected = addDrive('/somewhere/nifty/a/fred/stuff');
      process.env.XDG_CONFIG_HOME = p;
      const result = Xdg.userConfigDir(user, new FilePath(home));
      assert.isTrue(result.getAbsolutePath().startsWith(expected));
      assert.isTrue(hasExpectedEnding(result));
    });

    it('userDataDir returns default path', () => {
      const result = Xdg.userDataDir();
      assert.isTrue(hasExpectedEnding(result));
      assert.isAbove(result.getAbsolutePath().length, folder().length);
    });
    it('userDataDir returns path set with RSTUDIO_DATA_HOME', () => {
      const p = addDrive('/once/upon/a/time/rstudio');
      process.env.RSTUDIO_DATA_HOME = p;
      const result = Xdg.userDataDir();
      assert.strictEqual(result.getAbsolutePath(), p);
    });
    it('userDataDir returns path set with XDG_DATA_HOME', () => {
      const p = addDrive('/once/upon/a/time');
      process.env.XDG_DATA_HOME = p;
      const result = Xdg.userDataDir();
      assert.isTrue(result.getAbsolutePath().startsWith(p));
      assert.isTrue(hasExpectedEnding(result));
    });
    it('userDataDir expands placeholders', () => {
      const p = '${HOME}/a/$USER/stuff';
      const user = 'fred';
      const home = addDrive('/somewhere/nifty');
      const expected = addDrive('/somewhere/nifty/a/fred/stuff');
      process.env.XDG_DATA_HOME = p;
      const result = Xdg.userDataDir(user, new FilePath(home));
      assert.isTrue(result.getAbsolutePath().startsWith(expected));
      assert.isTrue(hasExpectedEnding(result));
    });

    it('systemConfigDir returns default path', () => {
      const result = Xdg.systemConfigDir();
      assert.isTrue(hasExpectedEnding(result));
      assert.isAbove(result.getAbsolutePath().length, folder().length);
    });
    it('systemConfigDir returns path set with RSTUDIO_CONFIG_DIR', () => {
      const p = addDrive('/config/goes/here');
      process.env.RSTUDIO_CONFIG_DIR = p;
      const result = Xdg.systemConfigDir();
      assert.strictEqual(result.getAbsolutePath(), p);
    });
    it('systemConfigDir returns path set with single-path XDG_CONFIG_DIRS', () => {
      const p = addDrive('/once/upon/a/time');
      process.env.XDG_CONFIG_DIRS = p;
      const result = Xdg.systemConfigDir();
      assert.isTrue(result.getAbsolutePath().startsWith(p));
      assert.isTrue(hasExpectedEnding(result));
    });
    it('systemConfigDir returns path set with multi-path XDG_CONFIG_DIRS', () => {
      if (process.platform === 'win32') return; // not supported on Windows

      // path must end with 'rstudio' and exist to be returned
      const baseDir = path.join(os.tmpdir(), randomString());
      const fullDir = path.join(baseDir, 'rstudio');
      fs.mkdirSync(fullDir, { recursive: true });
      assert.isTrue(fs.existsSync(fullDir));
      const p = `/once/upon/a/time:${baseDir}:/something/else/rstudio`;
      process.env.XDG_CONFIG_DIRS = p;
      const result = Xdg.systemConfigDir();
      assert.isTrue(result.getAbsolutePath().startsWith(fullDir));
      assert.isTrue(hasExpectedEnding(result));
      fs.rmSync(fullDir, { recursive: true });
      assert.isFalse(fs.existsSync(fullDir));
    });
    it('systemConfigFile returns default location if requested file not found', () => {
      const result = Xdg.systemConfigFile('fake-config-file.foo');
      assert.isFalse(result.isEmpty());
      assert.isTrue(result.getAbsolutePath().endsWith('fake-config-file.foo'));
    });
    it('systemConfigFile ignores bogus XDG_CONFIG_DIRS and eturns default location', () => {
      process.env.XDG_CONFIG_DIRS = '/once/upon/a/time:/somewhere/else';
      const result = Xdg.systemConfigFile('fake-config-file.foo');
      assert.isFalse(result.isEmpty());
      assert.isTrue(result.getAbsolutePath().endsWith('fake-config-file.foo'));
    });
  });
  describe('Misc helpers', () => {
    it('SHGetKnownFolderPath returns a string', () => {
      if (process.platform === 'win32') {
        // Can't use node's os.userInfo().username because on Windows the username and the
        // name of the user's home directory are not necessarily the same.
        const userProfile = process.env.USERPROFILE;
        const profileFolderName = userProfile ? path.basename(userProfile) : '';
        const expectedLocalAppData = `C:\\Users\\${profileFolderName}\\AppData\\Local`;
        const expectedProgramData = 'C:\\ProgramData';
        const expectedRoamingAppData = `C:\\Users\\${profileFolderName}\\AppData\\Roaming`;

        let result = SHGetKnownFolderPath(WinFolderID.FOLDERID_LocalAppData);
        assert.strictEqual(result, expectedLocalAppData);
        result = SHGetKnownFolderPath(WinFolderID.FOLDERID_ProgramData);
        assert.strictEqual(result, expectedProgramData);
        result = SHGetKnownFolderPath(WinFolderID.FOLDERID_RoamingAppData);
        assert.strictEqual(result, expectedRoamingAppData);
      } else {
        const result = SHGetKnownFolderPath(WinFolderID.FOLDERID_LocalAppData);
        assert.strictEqual(result, '');
      }
    });
  });
  describe('NYI placeholders', () => {
    it('sync methods should throw exception', () => {
      assert.throws(() => Xdg.forwardXdgEnvVars({ FOO: 'bar' }));
    });
  });
  describe('verifyUserDirs', () => {
    let testRoot: string;

    beforeEach(() => {
      testRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'rstudio-xdg-test-'));
    });

    afterEach(() => {
      // restore permissions so cleanup can succeed
      for (const entry of fs.readdirSync(testRoot)) {
        try {
          fs.chmodSync(path.join(testRoot, entry), 0o700);
        } catch {
          // ignore; removal below is best-effort
        }
      }
      fs.rmSync(testRoot, { recursive: true, force: true });
    });

    it('checkDirectoryWritable creates missing directories', () => {
      const dir = new FilePath(path.join(testRoot, 'a', 'b'));

      assert.isNull(checkDirectoryWritable(dir));
      assert.isTrue(dir.existsSync());
    });

    it('checkDirectoryWritable reports non-writable directories', function () {
      if (process.platform === 'win32') {
        this.skip();
      }

      const dir = path.join(testRoot, 'readonly');
      fs.mkdirSync(dir);
      fs.chmodSync(dir, 0o500);

      const message = checkDirectoryWritable(new FilePath(dir));
      assert.isString(message);
      assert.isNotEmpty(message);
    });

    it('checkDirectoryWritable reports directories that cannot be created', function () {
      if (process.platform === 'win32') {
        this.skip();
      }

      const parent = path.join(testRoot, 'readonly');
      fs.mkdirSync(parent);
      fs.chmodSync(parent, 0o500);

      const message = checkDirectoryWritable(new FilePath(path.join(parent, 'child')));
      assert.isString(message);
      assert.isNotEmpty(message);
    });

    it('verifyUserDirs returns no issues when user dirs are writable', () => {
      process.env.RSTUDIO_CONFIG_HOME = path.join(testRoot, 'config');
      process.env.RSTUDIO_DATA_HOME = path.join(testRoot, 'data');

      const issues = Xdg.verifyUserDirs();
      assert.isEmpty(issues);
    });

    it('verifyUserDirs reports non-writable user dirs', function () {
      if (process.platform === 'win32') {
        this.skip();
      }

      const configDir = path.join(testRoot, 'config');
      const dataDir = path.join(testRoot, 'data');
      fs.mkdirSync(dataDir);
      fs.chmodSync(dataDir, 0o500);
      process.env.RSTUDIO_CONFIG_HOME = configDir;
      process.env.RSTUDIO_DATA_HOME = dataDir;

      const issues = Xdg.verifyUserDirs();
      assert.lengthOf(issues, 1);
      assert.strictEqual(issues[0].directory, dataDir);
      assert.isNotEmpty(issues[0].message);
    });
  });
});
