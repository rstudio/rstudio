/*
 * install-fuses.mjs
 *
 * Copyright (C) 2024 by Posit Software, PBC
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

import { flipFuses, FuseVersion, FuseV1Options } from '@electron/fuses';
import { execSync } from 'child_process';
import { arch, platform } from 'process';

// figure out path to Electron executable
let prefix = `out/RStudio-${platform}-${arch}`;

let exePath = "";
if (platform === 'darwin') {
  exePath = `${prefix}/RStudio.app/Contents/MacOS/RStudio`;
} else if (platform === 'win32') {
  exePath = `${prefix}/RStudio.exe`;
} else if (platform === 'linux') {
  exePath = `${prefix}/rstudio`;
} else {
  throw new Error(`Unsupported platform: ${platform}`);
}

// disable potentially insecure Electron fuses
await flipFuses(exePath, {
  version: FuseVersion.V1,
  [FuseV1Options.RunAsNode]: false,
  [FuseV1Options.EnableNodeOptionsEnvironmentVariable]: false,
  [FuseV1Options.EnableNodeCliInspectArguments]: false,
});

// verify it worked
execSync(`npx @electron/fuses read --app ${exePath}`, {
  encoding: 'utf8',
  stdio: 'inherit',
});

