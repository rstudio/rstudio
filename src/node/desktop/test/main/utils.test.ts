/*
 * utils.test.ts
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
      assert.isNotEmpty(Utils.userLogPath());
    });
    it('usereWebCachePath returns a non-empty string', () => {
      assert.isNotEmpty(Utils.userWebCachePath());
    });
    it('devicePixelRatio returns 1.0', () => {
      assert.strictEqual(Utils.devicePixelRatio(), 1.0);
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
        assert.isTrue(result);
      } else {
        assert.isFalse(result);
      }
    });
    it('randomString genereates a random string', () => {
      const str1 = Utils.randomString();
      const str2 = Utils.randomString();
      const str3 = Utils.randomString();
      assert.notEqual(str1, str2);
      assert.notEqual(str1, str3);
      assert.notEqual(str2, str3);
    });
    it('getComponentVersions returns expected JSON', () => {
      const result = Utils.getComponentVersions();
      const json: Utils.VersionInfo = JSON.parse(result);
      assert.isNotEmpty(json.electron);
      assert.isNotEmpty(json.rstudio);
      assert.isNotEmpty(json.node);
      assert.isNotEmpty(json.v8);
    });
    it('removeStaleOptionsLockfile does... something', () => {
      Utils.removeStaleOptionsLockfile();
      if (process.platform === 'win32') {
        // TODO
      }
    });
    it('augmentCommandLineArguments adds contents of env var', () => {
      assert.isFalse(app.commandLine.hasSwitch('disable-gpu'));
      assert.isEmpty(env.getenv('RSTUDIO_CHROMIUM_ARGUMENTS'));
      env.setenv('RSTUDIO_CHROMIUM_ARGUMENTS', '--disable-gpu');
      Utils.augmentCommandLineArguments();
      assert.isTrue(app.commandLine.hasSwitch('disable-gpu'));
      env.unsetenv('RSTUDIO_CHROMIUM_ARGUMENTS');
    });  });
});
