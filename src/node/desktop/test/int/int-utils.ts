/*
 * int-utils.ts
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
import { ElectronApplication, _electron } from 'playwright';
import path from 'path';
import util from 'util';

interface LaunchArgs {
  args?: string[];
  cwd?: string;
  executablePath?: string;
}

// Find path to RStudio entrypoint
function getLaunchArgs(extraArgs?: string[]): LaunchArgs {
  const result: LaunchArgs = {};

  if (extraArgs) {
    result.args = extraArgs;
  }

  if (process.platform === 'darwin') {
    result.executablePath = path.join(
      __dirname,
      '../../../../../package/osx/install/RStudio.app/Contents/MacOS/RStudio',
    );
  } else if (process.platform === 'win32') {
    result.executablePath = path.join(__dirname, '../../out/RStudio-win32-x64/RStudio.exe');
  } else {
    // TODO -- other platforms!
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
  return electronApp.evaluate(async ({ BrowserWindow }): Promise<string[]> => {
    const titles = Array<string>();
    const windows = BrowserWindow.getAllWindows();
    for (const window of windows) {
      titles.push(window.getTitle());
    }
    return titles;
  });
}
