/*
 * desktop-utils.test.ts
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

import * as Utils from '../../src/main/utils';
import { app } from 'electron';
import * as env from '../../src/core/environment';

describe('DesktopUtils', () => {
  describe('Static helpers', () => {
    it('reattachConsoleIfNecessary does... something', () => {
      Utils.reattachConsoleIfNecessary();
      if (process.platform === 'win32') {
        // TODO 
      }
    });
    it('userLogPath returns a non-empty string', () => {
      expect(Utils.userLogPath()).to.not.be.empty;
    });
    it('usereWebCachePath returns a non-empty string', () => {
      expect(Utils.userWebCachePath()).to.not.be.empty;
    });
    it('devicePixelRatio returns 1.0', () => {
      expect(Utils.devicePixelRatio()).equals(1.0);
    });
    it('initializeLang does... something', () => {
      Utils.initializeLang();
      if (process.platform === 'darwin') {
        // TODO
      }
    });
    it('isMacOS detects... macOS', () => {
      const result = Utils.isMacOS();
      if (process.platform === 'darwin') {
        expect(result).is.true;
      } else {
        expect(result).is.false;
      }
    });
    it('randomString genereates a random string', () => {
      const str1 = Utils.randomString();
      const str2 = Utils.randomString();
      const str3 = Utils.randomString();
      expect(str1).not.equal(str2);
      expect(str1).not.equal(str3);
      expect(str2).not.equal(str3);
    });
    it('getComponentVersions returns expected JSON', () => {
      const result = Utils.getComponentVersions();
      const json: Utils.VersionInfo = JSON.parse(result);
      expect(json.electron).length.is.greaterThan(0);
      expect(json.rstudio).length.is.greaterThan(0);
      expect(json.node).length.is.greaterThan(0);
      expect(json.v8).length.is.greaterThan(0);
    });
    it('removeStaleOptionsLockfile does... something', () => {
      Utils.removeStaleOptionsLockfile();
      if (process.platform === 'win32') {
        // TODO
      }
    });
    it('augmentCommandLineArguments adds contents of env var', () => {
      expect(app.commandLine.hasSwitch('disable-gpu')).is.false;
      expect(env.getenv('RSTUDIO_CHROMIUM_ARGUMENTS')).is.empty;
      env.setenv('RSTUDIO_CHROMIUM_ARGUMENTS', '--disable-gpu');
      Utils.augmentCommandLineArguments();
      expect(app.commandLine.hasSwitch('disable-gpu')).is.true;
      env.unsetenv('RSTUDIO_CHROMIUM_ARGUMENTS');
    });  });
});
