/*
 * init.test.ts
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

import * as Init from '../../src/main/init';
import { app } from 'electron';
import * as env from '../../src/core/environment';

describe('Init', () => {
  describe('Static helpers', () => {
    it('getComponentVersions returns expected JSON', () => {
      const result = Init.getComponentVersions();
      const json: Init.VersionInfo = JSON.parse(result);
      expect(json.electron).length.is.greaterThan(0);
      expect(json.rstudio).length.is.greaterThan(0);
      expect(json.node).length.is.greaterThan(0);
      expect(json.v8).length.is.greaterThan(0);
    });
    it('removeStaleOptionsLockfile does... something', () => {
      Init.removeStaleOptionsLockfile();
      if (process.platform === 'win32') {
        // TODO
      }
    });
    it('augmentCommandLineArguments adds contents of env var', () => {
      expect(app.commandLine.hasSwitch('disable-gpu')).is.false;
      expect(env.getenv('RSTUDIO_CHROMIUM_ARGUMENTS')).is.empty;
      env.setenv('RSTUDIO_CHROMIUM_ARGUMENTS', '--disable-gpu');
      Init.augmentCommandLineArguments();
      expect(app.commandLine.hasSwitch('disable-gpu')).is.true;
      env.unsetenv('RSTUDIO_CHROMIUM_ARGUMENTS');
    });
  });
});
