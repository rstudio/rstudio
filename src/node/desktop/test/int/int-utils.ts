/*
 * int-utils.ts
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
import { ElectronApplication, _electron } from 'playwright';
import path from 'path';
import fs from 'fs';
import util from 'util';

interface LaunchArgs {
  args?: string[],
  cwd?: string,
  executablePath?: string,
}

// Find path to RStudio entrypoint
function getLaunchArgs(extraArgs?: string[]): LaunchArgs {
  // use the package build if it exists
  let isPackaged = true;
  const result: LaunchArgs = {};
  let entryPoint = '';
  let executable = '';
  let cwd = '';

  if (process.platform === 'darwin') {
    entryPoint = path.join(__dirname, '../../package/RStudio-darwin-x64/RStudio.app/Contents/Resources/app/dist/src/main/main.js');
    cwd = path.join(__dirname, '../../package/RStudio-darwin-x64/');
  } else {
    // TODO -- other platforms!
  }

  if (!fs.existsSync(entryPoint)) {
    // otherwise try the dev build
    isPackaged = false;
    entryPoint = path.join(__dirname, '../../dist/src/main/main.js');
    cwd = path.join(__dirname, '../..');
  }

  result.args = [entryPoint];
  if (extraArgs) {
    result.args = result.args.concat(extraArgs);
  }
  result.cwd = cwd;

  if (isPackaged) {
    if (process.platform === 'darwin') {
      executable = path.join(__dirname, '../../package/RStudio-darwin-x64/RStudio.app/Contents/MacOS/RStudio');
    } else {
      // TODO -- other platforms!
    }
  }

  if (executable) {
    result.executablePath = executable;
  }

  return result;
}

export async function launch(extraArgs?: string[]): Promise<ElectronApplication> {
  return _electron.launch(getLaunchArgs(extraArgs));
}

export const setTimeoutPromise = util.promisify(setTimeout);

/**
 * @returns Array of window titles
 */
export async function getWindowTitles(electronApp: ElectronApplication): Promise<string[]> {
  return electronApp.evaluate(async ({ BrowserWindow } ): Promise<string[]> => {
    const titles = Array<string>();
    const windows = BrowserWindow.getAllWindows();
    for (const window of windows) {
      titles.push(window.getTitle());
    }
    return titles;
  });
}
