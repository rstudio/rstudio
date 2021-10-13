/*
 * import-resources.ts
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

// Copies resources such as rsession from the build output folder into the packaged 
// RStudio Electron app.

import path from 'path';
import * as tools from './script-tools';

// Run the script!
main().catch(err => console.error(err));
async function main(): Promise<void> {
  switch (process.platform) {
    case 'win32':
      process.exit(await packageWin32());
    case 'linux':
      process.exit(await packageLinux());
    case 'darwin':
      process.exit(await packageDarwin());
    default:
      console.error(`Error: Electron build not supported on this platform: ${process.platform}.`);
      process.exit(1);
  }
}

/**
 * Windows implementation
 */
async function packageWin32(): Promise<number> {
  console.error('Error: not implemented on this platform.');
  // const packageDir = tools.getPlatformPackageOutputDir();
  // if (!fs.existsSync(packageDir)) {
  //   console.error(`'yarn package' results not found at: ${packageDir}`);
  //   return 1;
  // }

  // const rstudioInstallDir = path.join(tools.getProgramFilesWindows(), 'RStudio');
  // if (!fs.existsSync(rstudioInstallDir)) {
  //   console.error(`RStudio not found at ${rstudioInstallDir}. Install a recent daily build and try again.`);
  //   return 1;
  // }

  // const appDest = path.join(packageDir, 'resources/app');

  // await tools.copyFiles(
  //   [path.join(rstudioInstallDir, 'bin')], path.join(appDest, 'bin'), {
  //   filter: [
  //     '**/*',
  //     '!Qt5*',
  //     '!QtWebEngineProcess.exe',
  //     '!resources/*',
  //     '!translations/*',
  //     '!rstudio.exe',
  //     '!d3dcompiler_47.dll',
  //     '!libEGL.dll',
  //     '!libGLESV2.dll'
  //   ]
  // });

  // await copy(path.join(rstudioInstallDir, 'R'), path.join(appDest, 'R'));
  // await copy(path.join(rstudioInstallDir, 'www'), path.join(appDest, 'www'));
  // await copy(path.join(rstudioInstallDir, 'www-symbolmaps'), path.join(appDest, 'www-symbolmaps'));
  // await copy(path.join(rstudioInstallDir, 'resources'), path.join(appDest, 'resources'), { filter: ['**/*', '!html/*'] });

  return 1;
}

/**
 * Linux implementation
 */
async function packageLinux(): Promise<number> {
  console.error('Error: not implemented on this platform.');
  return 1;
}

/**
 * Mac implementation
 */
async function packageDarwin(): Promise<number> {

  const makePackageDir = tools.getMakePackageDir();
  const packageDir = tools.getForgePlatformOutputDir();

  const appDest = path.join(packageDir, 'RStudio.app', 'Contents', 'resources', 'app');

  try {
    await tools.copyFiles(['**/*'], path.join(makePackageDir, 'install'), appDest);

  } catch (e) {
    console.error(e.message);
    return 1;
  }
  return 0;
}
