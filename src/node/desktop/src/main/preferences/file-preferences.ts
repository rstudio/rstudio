/**
 *
 * file-preferences.ts
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

import { homedir } from 'os';
import PropertiesReader from 'properties-reader';
import DesktopOptions from './desktop-options';
import { preferenceKeys } from './preferences';

const INI_FILE = 'desktop.ini';

class FilePreferences extends DesktopOptions {
  private iniFile: string; // existing location for some preferences
  private properties: PropertiesReader.Reader;

  constructor() {
    super();
    if (process.platform === 'win32') {
      this.iniFile = `${homedir()}\\AppData\\Roaming\\RStudio\\${INI_FILE}`;
    } else if (process.platform === 'linux') {
      this.iniFile = `${homedir()}/.config/RStudio/${INI_FILE}`;
    } else {
      throw new Error('unsupported platform');
    }

    this.properties = PropertiesReader(this.iniFile);
  }

  fixedWidthFont(): string | undefined {
    return this.properties.get(preferenceKeys.fontFixedWidth)?.toString();
  }
}

export default FilePreferences;
