/*
 * package-helper.ts
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

// Creates a runnable package of RStudio by pulling in binaries from the
// currently installed build of RStudio and combining with existing output
// of electron-packager.

import fs from 'fs';
import path from 'path';
import copy from 'recursive-copy';

import { getPackageOutputDir, getProjectRootDir, getProgramFilesWindows, section, info } from './script-tools';

blend().catch(err => console.error(err));

async function blend(): Promise<void> {
  if (!fs.existsSync(getProjectRootDir())) {
    console.error(`Project root not found at expected location: ${getProjectRootDir()}`);
    process.exit(1);
  }

  switch (process.platform) {
    case 'win32':
      process.exit(await packageWin32());
    case 'linux':
      process.exit(await packageLinux());
    case 'darwin':
      process.exit(await packageDarwin());
    default:
      console.error('Error: not supported on this platform.');
      process.exit(1);
  }
}

async function packageWin32(): Promise<number> {
  console.log(section('Creating package for Microsoft Windows'));

  const packageDir = path.join(getPackageOutputDir(), 'RStudio-win32-x64');
  if (!fs.existsSync(packageDir)) {
    console.error(`'yarn package' results not found at: ${packageDir}`);
    return 1;
  }
  console.log(info(`Using electron-packager output from ${packageDir}`));

  const rstudioInstallDir = path.join(getProgramFilesWindows(), 'RStudio');
  if (!fs.existsSync(rstudioInstallDir)) {
    console.error(`RStudio not found at ${rstudioInstallDir}. Install a recent daily build and try again.`);
    return 1;
  }
  console.log(info(`Using RStudio binaries found at: ${rstudioInstallDir}`));

  const appDest = path.join(packageDir, 'resources/app');

  console.log(info('Copying binary files'));
  await copy(
    path.join(rstudioInstallDir, 'bin'), path.join(appDest, 'bin'), {
    filter: [
      '**/*',
      '!Qt5*',
      '!QtWebEngineProcess.exe',
      '!resources/*',
      '!translations/*',
      '!rstudio.exe',
      '!d3dcompiler_47.dll',
      '!libEGL.dll',
      '!libGLESV2.dll'
    ]
  });

  console.log(info('Copying R resources'));
  await copy(path.join(rstudioInstallDir, 'R'), path.join(appDest, 'R'));
  console.log(info('Copying www files'));
  await copy(path.join(rstudioInstallDir, 'www'), path.join(appDest, 'www'));
  console.log(info('Copying www-symbolmaps files'));
  await copy(path.join(rstudioInstallDir, 'www-symbolmaps'), path.join(appDest, 'www-symbolmaps'));
  console.log(info('Copying misc resources'));
  await copy(path.join(rstudioInstallDir, 'resources'), path.join(appDest, 'resources'), { filter: ['**/*', '!html/*'] });

  return 0;
}

async function packageLinux(): Promise<number> {
  console.log(section('Creating package for Linux'));
  console.error('Error: not supported on this platform.');
  return 1;
}

async function packageDarwin(): Promise<number> {
  console.log(section('Creating package for MacOS'));
  console.error('Error: not supported on this platform.');
  return 1;
}

