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

// Copies resources such as rsession from the specified build output folder into the packaged 
// RStudio Electron app.

import fs from 'fs';
import path from 'path';
import copy from 'recursive-copy';

import { getPackageOutputDir, getProjectRootDir, getProgramFilesWindows, section, info, isDirectory } from './script-tools';

const buildDirOpt = '--builddir=';

// Source of non-Electron cmake build output (rsession and friends)
async function getBuildDir(): Promise<string> {
  let buildDir = '';
  for (const arg of process.argv) {
    if (arg.startsWith(buildDirOpt)) {
      buildDir = arg.substring(buildDirOpt.length);
    }
  }
  if (buildDir.length === 0) {
    // default to make-package build location
    let osFolder = '';
    switch (process.platform) {
      case 'darwin':
        osFolder = 'osx';
        break;
      case 'linux':
        osFolder = 'linux';
        break;
      case 'win32':
        osFolder = 'win32';
        break;
      default:
        console.error(`Unsupported platform: ${process.platform}`);
        process.exit(1);
    }
    buildDir = path.join('..', '..', '..', 'package', osFolder, 'build');
  }

  if (!await isDirectory(buildDir)) {
    console.error(`Build folder not found: '${buildDir}'`);
    process.exit(1);
  }

  return buildDir;
}

// Run the script!
main().catch(err => console.error(err));
async function main(): Promise<void> {

  if (!fs.existsSync(getProjectRootDir())) {
    console.error(`Project root not found at expected location: ${getProjectRootDir()}`);
    process.exit(1);
  }

  const buildDir = await getBuildDir();
  switch (process.platform) {
    case 'win32':
      process.exit(await packageWin32(buildDir));
    case 'linux':
      process.exit(await packageLinux(buildDir));
    case 'darwin':
      process.exit(await packageDarwin(buildDir));
    default:
      console.error(`Error: Electron build not supported on this platform: ${process.platform}.`);
      process.exit(1);
  }
}

async function packageWin32(buildDir: string): Promise<number> {
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

async function packageLinux(buildDir: string): Promise<number> {
  console.log(section('Creating package for Linux'));
  console.error('Error: not supported on this platform.');
  return 1;
}

async function packageDarwin(buildDir: string): Promise<number> {
  const gwtFolder = path.join(buildDir, 'gwt');
  if (!await isDirectory(gwtFolder)) {
    console.error(`Folder not found: '${gwtFolder}'`);
    process.exit(1);
  }
  const cppFolder = path.join(buildDir, 'src', 'cpp');
  if (!await isDirectory(cppFolder)) {
    console.error(`Folder not found: '${cppFolder}'`);
    process.exit(1);
  }

  const packageDir = path.join(getPackageOutputDir(), 'RStudio-darwin-x64');
  if (!fs.existsSync(packageDir)) {
    console.error(`'yarn package' output not found at: ${packageDir}`);
    return 1;
  }
  console.log(info(`Using electron-packager output from ${packageDir}`));

  return 0;
}
