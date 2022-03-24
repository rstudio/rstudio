/**
 *
 * preferences.ts
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

import FilePreferences from './file-preferences';
import MacPreferences from './mac-preferences';
import DesktopOptions from './desktop-options';
import { Schema } from 'electron-store';

const isMacOS = process.platform === 'darwin';

/**
 * MacOS stores periods as · in defaults. When adding a new key, ensure that the key is valid
 * on all platforms.
 */
export const preferenceKeys = {
  fontFixedWidth: isMacOS ? 'font·fixedWidth' : 'General.font.fixedWidth',
  zoomLevel: isMacOS ? 'view·zoomLevel' : 'General.view.zoomLevel',
  rBinDir: 'RBinDir',
};

/**
 * Create the Schema from a JSON schema file. The `Schema` is templated with the
 * definition created with `scripts/generate.ts`, which is generated from the
 * JSON schema file. `electron-store` requires a schema passed in when creating
 * the store. Otherwise, type validation will not work.
 * @param schemaJson the JSON schema object created from the schema file
 * @returns a Schema
 */
export function generateSchema<T>(schemaJson: object): Schema<T> {
  // workaround for defining the schema since electron-store cannot be given the file
  return JSON.parse(JSON.stringify(schemaJson));
}

/**
 * This is the legacy preference manager. It will read the preferences from platform-specific
 * location. Settings should only be used from here if the new location does not contain a
 * value for the preference.
 */
export let legacyPreferenceManager: DesktopOptions;

switch (process.platform) {
  case 'darwin':
    legacyPreferenceManager = new MacPreferences();
    break;
  case 'win32':
  case 'linux':
    legacyPreferenceManager = new FilePreferences();
    break;
  default:
    throw new Error('unsupported platform');
}
