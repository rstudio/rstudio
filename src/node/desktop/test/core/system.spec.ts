/*
 * system.spec.ts
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

import { FilePath } from '../../src/core/file-path';
import { initHook, initializeLog, userHomePath, username } from '../../src/core/system';
import * as log from '../../src/core/log';

describe('System', () => {
  describe('User info', () => {
    it('getUserHomePath returns a valid path', () => {
      expect(fs.existsSync(userHomePath().getAbsolutePath())).is.true;
    });
    it('username returns a non-empty string', () => {
      expect(username().length).is.greaterThan(0);
    });
  });
  describe('Assorted helper functions', () => {
    it('initHook does something', () => {
      initHook();
      // TODO: once this does something, test it!
    });
    it('initializeLog succeeds', () => {
      const err = initializeLog('rdesktop', log.LogLevel.WARN, new FilePath(os.tmpdir()));
      expect(!!err).is.false;
    });
  });
});
 