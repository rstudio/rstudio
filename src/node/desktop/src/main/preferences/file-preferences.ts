/**
 *
 * file-preferences.ts
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

import PropertiesReader from 'properties-reader';
import { FilePath } from '../../core/file-path';
import { Xdg } from '../../core/xdg';
import DesktopOptions from './desktop-options';
import { preferenceKeys } from './preferences';

const INI_FILE = 'desktop.ini';

class FilePreferences extends DesktopOptions {
  private properties?: PropertiesReader.Reader;

  constructor() {
    super();
    const userConfigDir = Xdg.userConfigDir();

    // Linux has Qt legacy code that writes desktop.ini to ~/.config/RStudio but Xdg returns ~/.config/rstudio
    // https://github.com/rstudio/rstudio/issues/6979
    const desktopIni = userConfigDir.completePath(INI_FILE).getAbsolutePath().replace('rstudio', 'RStudio');

    if (FilePath.existsSync(desktopIni)) {
      this.properties = PropertiesReader(desktopIni);
    }
  }

  fixedWidthFont(): string | undefined {
    return this.properties?.get(preferenceKeys.fontFixedWidth)?.toString();
  }

  zoomLevel(): number | undefined {
    const zoomValue = this.properties?.get(preferenceKeys.zoomLevel)?.toString();
    return zoomValue ? parseFloat(zoomValue) : undefined;
  }

  rBinDir(): string | undefined {
    if (process.platform !== 'win32') {
      return '';
    }

    return this.properties?.get(preferenceKeys.rBinDir)?.toString();
  }
}

export default FilePreferences;
