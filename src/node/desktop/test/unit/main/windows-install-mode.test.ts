/*
 * windows-install-mode.test.ts
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
import { parseRegQueryValue, windowsInstallModeFor } from '../../../src/main/windows-install-mode';

const regOutput = `
HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\RStudio
    InstallLocation    REG_SZ    C:\\Users\\me\\AppData\\Local\\Programs\\RStudio

`;

describe('windows install mode', () => {
  describe('parseRegQueryValue', () => {
    it('extracts a REG_SZ value with spaces preserved', () => {
      assert.equal(
        parseRegQueryValue(regOutput, 'InstallLocation'),
        'C:\\Users\\me\\AppData\\Local\\Programs\\RStudio',
      );
      assert.equal(
        parseRegQueryValue(
          '    InstallLocation    REG_EXPAND_SZ    C:\\Program Files\\RStudio  \r\n',
          'InstallLocation',
        ),
        'C:\\Program Files\\RStudio',
      );
    });

    it('returns an empty string when the value is absent', () => {
      assert.equal(parseRegQueryValue(regOutput, 'DisplayVersion'), '');
      assert.equal(parseRegQueryValue('', 'InstallLocation'), '');
    });
  });

  describe('windowsInstallModeFor', () => {
    const currentUser = 'C:\\Users\\me\\AppData\\Local\\Programs\\RStudio';
    const allUsers = 'C:\\Program Files\\RStudio';

    it('matches the registered location, ignoring case and trailing separators', () => {
      assert.equal(
        windowsInstallModeFor('c:\\users\\me\\appdata\\local\\programs\\rstudio\\', { currentUser, allUsers }),
        'currentuser',
      );
      assert.equal(windowsInstallModeFor('C:/Program Files/RStudio', { currentUser, allUsers }), 'allusers');
    });

    it('is null for an unregistered (zip) install or empty registry values', () => {
      assert.isNull(windowsInstallModeFor('D:\\tools\\RStudio', { currentUser, allUsers }));
      assert.isNull(windowsInstallModeFor('', { currentUser, allUsers }));
      assert.isNull(windowsInstallModeFor('C:\\Program Files\\RStudio', { currentUser: '', allUsers: '' }));
    });
  });
});
