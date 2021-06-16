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

import * as Utils from '../../../src/main/utils';
import { app } from 'electron';
import { getenv, setenv, unsetenv } from '../../../src/core/environment';

describe('DesktopUtils', () => {
  describe('Helper functions', () => {
    it('userLogPath returns a non-empty string', () => {
      assert.isNotEmpty(Utils.userLogPath());
    });
    it('usereWebCachePath returns a non-empty string', () => {
      assert.isNotEmpty(Utils.userWebCachePath());
    });
    it('devicePixelRatio returns 1.0', () => {
      assert.strictEqual(Utils.devicePixelRatio(), 1.0);
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
    it('augmentCommandLineArguments adds contents of env var', () => {
      assert.isFalse(app.commandLine.hasSwitch('disable-gpu'));
      assert.isEmpty(getenv('RSTUDIO_CHROMIUM_ARGUMENTS'));
      setenv('RSTUDIO_CHROMIUM_ARGUMENTS', '--disable-gpu');
      Utils.augmentCommandLineArguments();
      assert.isTrue(app.commandLine.hasSwitch('disable-gpu'));
      unsetenv('RSTUDIO_CHROMIUM_ARGUMENTS');
    });
  });

  describe('Static helpers', () => {
    it('initializeSharedSecret generates a random string in RS_SHARED_SECRET envvar_', () => {
      const envvar = 'RS_SHARED_SECRET';
      assert.equal(getenv(envvar).length, 0);
      Utils.initializeSharedSecret();
      assert.isAtLeast(getenv(envvar).length, 0);
    });
  });
});
