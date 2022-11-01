/**
 *
 * mac-preferences.ts
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

import { systemPreferences } from 'electron';
import { preferenceKeys } from './preferences';
import DesktopOptions from './desktop-options';

/**
 * MacOS specific implementation
 *
 * The preferences are stored in defaults, which is MacOS specific.
 */
class MacPreferences extends DesktopOptions {
  constructor() {
    super();
  }

  fixedWidthFont(): string | undefined {
    return systemPreferences.getUserDefault(preferenceKeys.fontFixedWidth, 'string');
  }

  zoomLevel(): number | undefined {
    return systemPreferences.getUserDefault(preferenceKeys.zoomLevel, 'double');
  }

  rBinDir(): string | undefined {
    return '';
  }
}

export default MacPreferences;
