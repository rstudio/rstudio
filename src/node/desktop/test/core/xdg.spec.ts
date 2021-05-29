/*
 * xdg.spec.ts
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
import os from 'os';
import path from 'path';

import { FilePath } from '../../src/core/file-path';
import { Xdg, SHGetKnownFolderPath, WinFolderID } from '../../src/core/xdg';

function folder() {
  return process.platform === 'win32' ? 'RStudio' : 'rstudio';
}

function hasExpectedEnding(path: FilePath) {
  return path.getAbsolutePath().endsWith(folder());
}

function randomString() {
  return Math.trunc(Math.random() * 2147483647).toString();
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
    for (const name in xdgVars) {
      xdgVars[name] = process.env[name] ?? '';
      delete process.env[name];
    }
  });

  // put back the original XDG env vars
  afterEach(() => {
    for (const name in xdgVars) {
      if (xdgVars[name]) {
        process.env[name] = xdgVars[name];
        xdgVars[name] = '';
      } else {
        delete process.env[name];
      }
    }
  });

  describe('Fetch Xdg paths', () => {
    it('userConfigDir returns default path', () => {
      const result = Xdg.userConfigDir();
      expect(hasExpectedEnding(result)).is.true;
      expect(result.getAbsolutePath().length).is.greaterThan(folder().length);
    });
    it('userConfigDir returns path set with RSTUDIO_CONFIG_HOME', () => {
      const p = '/once/upon/a/time/rstudio';
      process.env.RSTUDIO_CONFIG_HOME = p;
      const result = Xdg.userConfigDir();
      expect(result.getAbsolutePath()).equals(p);
    });
    it('userConfigDir returns path set with XDG_CONFIG_HOME', () => {
      const p = '/once/upon/a/time';
      process.env.XDG_CONFIG_HOME = p;
      const result = Xdg.userConfigDir();
      expect(result.getAbsolutePath().startsWith(p)).is.true;
      expect(hasExpectedEnding(result)).is.true;
    });
    it('userConfigDir expands placeholders', () => {
      const p = '${HOME}/a/$USER/stuff';
      const user = 'fred';
      const home = '/somewhere/nifty';
      const expected = '/somewhere/nifty/a/fred/stuff';
      process.env.XDG_CONFIG_HOME = p;
      const result = Xdg.userConfigDir(user, new FilePath(home));
      expect(result.getAbsolutePath().startsWith(expected)).is.true;
      expect(hasExpectedEnding(result)).is.true;
    });

    it('userDataDir returns default path', () => {
      const result = Xdg.userDataDir();
      expect(hasExpectedEnding(result)).is.true;
      expect(result.getAbsolutePath().length).is.greaterThan(folder().length);
    });
    it('userDataDir returns path set with RSTUDIO_DATA_HOME', () => {
      const p = '/once/upon/a/time/rstudio';
      process.env.RSTUDIO_DATA_HOME = p;
      const result = Xdg.userDataDir();
      expect(result.getAbsolutePath()).equals(p);
    });
    it('userDataDir returns path set with XDG_DATA_HOME', () => {
      const p = '/once/upon/a/time';
      process.env.XDG_DATA_HOME = p;
      const result = Xdg.userDataDir();
      expect(result.getAbsolutePath().startsWith(p)).is.true;
      expect(hasExpectedEnding(result)).is.true;
    });
    it('userDataDir expands placeholders', () => {
      const p = '${HOME}/a/$USER/stuff';
      const user = 'fred';
      const home = '/somewhere/nifty';
      const expected = '/somewhere/nifty/a/fred/stuff';
      process.env.XDG_DATA_HOME = p;
      const result = Xdg.userDataDir(user, new FilePath(home));
      expect(result.getAbsolutePath().startsWith(expected)).is.true;
      expect(hasExpectedEnding(result)).is.true;
    });

    it('systemConfigDir returns default path', () => {
      const result = Xdg.systemConfigDir();
      expect(hasExpectedEnding(result)).is.true;
      expect(result.getAbsolutePath().length).is.greaterThan(folder().length);
    });
    it('systemConfigDir returns path set with RSTUDIO_CONFIG_DIR', () => {
      const p = '/config/goes/here';
      process.env.RSTUDIO_CONFIG_DIR = p;
      const result = Xdg.systemConfigDir();
      expect(result.getAbsolutePath()).equals(p);
    });
    it('systemConfigDir returns path set with single-path XDG_CONFIG_DIRS', () => {
      const p = '/once/upon/a/time';
      process.env.XDG_CONFIG_DIRS = p;
      const result = Xdg.systemConfigDir();
      expect(result.getAbsolutePath().startsWith(p)).is.true;
      expect(hasExpectedEnding(result)).is.true;
    });
    it('systemConfigDir returns path set with multi-path XDG_CONFIG_DIRS', () => {
      if (process.platform === 'win32') return; // not supported on Windows

      // path must end with 'rstudio' and exist to be returned
      const baseDir = path.join(os.tmpdir(), randomString());
      const fullDir = path.join(baseDir, 'rstudio');
      fs.mkdirSync(fullDir, { recursive: true });
      expect(fs.existsSync(fullDir)).is.true;
      const p = `/once/upon/a/time:${baseDir}:/something/else/rstudio`;
      process.env.XDG_CONFIG_DIRS = p;
      const result = Xdg.systemConfigDir();
      expect(result.getAbsolutePath().startsWith(fullDir)).is.true;
      expect(hasExpectedEnding(result)).is.true;
      fs.rmdirSync(fullDir, { recursive: true });
      expect(fs.existsSync(fullDir)).is.false;
    });
    it('systemConfigFile returns default location if requested file not found', () => {
      const result = Xdg.systemConfigFile('fake-config-file.foo');
      expect(result.isEmpty()).is.false;
      expect(result.getAbsolutePath().endsWith('fake-config-file.foo')).is.true;
    });
    it('systemConfigFile ignores bogus XDG_CONFIG_DIRS and eturns default location', () => {
      process.env.XDG_CONFIG_DIRS = '/once/upon/a/time:/somewhere/else';
      const result = Xdg.systemConfigFile('fake-config-file.foo');
      expect(result.isEmpty()).is.false;
      expect(result.getAbsolutePath().endsWith('fake-config-file.foo')).is.true;
    });
  });
  describe('Misc helpers', () => {
    it('SHGetKnownFolderPath returns a string', () => {
      if (process.platform === 'win32') {
        // TODO, actually check results?
      } else {
        const result = SHGetKnownFolderPath(WinFolderID.FOLDERID_LocalAppData);
        expect(result).equals('');
      }
    });
  });
  describe('NYI placeholders', () => {
    it('sync methods should throw exception', () => {
      expect(() => Xdg.verifyUserDirs()).to.throw();
      expect(() => Xdg.forwardXdgEnvVars({'FOO': 'bar'})).to.throw();
    });
  });
});
