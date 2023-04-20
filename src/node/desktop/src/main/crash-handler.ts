/*
 * crash-handler.ts
 *
 * Copyright (C) 2023 by Posit Software, PBC
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

import { app, crashReporter } from 'electron';
import { getenv } from '../core/environment';
import path from 'path';

/**
 * Enable Electron crash handling. Until we've done more testing to see if this clashes with
 * our use of a different build of crashpad in rsession, this is OFF by default.
 * 
 * Enable by setting RSTUDIO_ENABLE_CRASHPAD=1 before starting RStudio.
 * 
 * If Electron/RStudio crashes, the results will be found under:
 * 
 *     Windows: %appdata%\RStudio\Crashpad
 *     Mac:     ~/Library/Application Support/RStudio/Crashpad
 *     Linux:   ~/.config/rstudio/Crashpad
 */
export function initCrashHandler() {
  const enableCrashpad = getenv('RSTUDIO_ENABLE_CRASHPAD');
  if (enableCrashpad === '1' || enableCrashpad.toLowerCase() === 'true') {
    console.log(`Starting crash logging to ${crashDumpLocation()}`);
    crashReporter.start({uploadToServer: false});
  }
}

/**
 * Returns the location of Electron crash dumps (if any).
 */
export function crashDumpLocation() {
  return path.join(app.getPath('userData'), 'Crashpad');
}
