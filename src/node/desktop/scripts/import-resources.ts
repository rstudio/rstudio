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
  const sourceDir = path.join(tools.getProjectRootDir(), '..', '..');
  const buildDir = tools.getMakePackageBuildDir();
  const cppBuildDir = path.join(buildDir, 'src', 'cpp');
  const gwtBuildDir = path.join(buildDir, 'gwt');
  const cppSourceDir = path.join(sourceDir, 'cpp');
  const nodeFolder = path.join(buildDir, 'src', 'node');

  const packageDir = tools.getForgePlatformOutputDir();
  const appDest = path.join(packageDir, 'RStudio.app', 'Contents', 'resources', 'app');
  const appResDest = path.join(appDest, 'resources');
  const binDest = path.join(appDest, 'bin');

  try {
    const cmakeVars = await tools.loadCMakeVars(path.join(buildDir, 'CMakeCache.txt'));

    // src/cpp/session
    const sessionCppDir = path.join(cppSourceDir, 'session');
    const sessionBuildDir = path.join(cppBuildDir, 'session');
    await tools.copyFiles(
      ['r-ldpath', 'rsession'],
      sessionBuildDir,
      binDest);
    await tools.copyFiles(
      ['*.html', '*.css', '*.js', '*.lua', '*.csl', 'NOTICE'],
      path.join(sessionCppDir, 'resources'),
      appResDest);
    await tools.copyFiles(
      ['**/*'],
      path.join(sessionCppDir, 'resources', 'templates'),
      path.join(appResDest, 'templates'));
    await tools.copyFiles(
      ['**/*'],
      path.join(sessionCppDir, 'resources', 'presentation'),
      path.join(appResDest, 'presentation'));
    await tools.copyFiles(
      ['CITATION'],
      sessionBuildDir,
      appResDest);
    await tools.copyFiles(
      ['*.rstheme', '*.R'],
      path.join(sessionCppDir, 'resources', 'themes'),
      path.join(appResDest, 'themes'));
    await tools.copyFiles(
      ['*.css'],
      path.join(sessionCppDir, 'resources', 'themes', 'css'),
      path.join(appResDest, 'themes', 'css'));
    await tools.copyFiles(
      ['*.R'],
      path.join(sessionBuildDir, 'modules', 'R'),
      path.join(appDest, 'R', 'modules'));
    await tools.copyFiles(
      ['**/*'],
      path.join(sessionCppDir, 'resources', 'connections'),
      path.join(appResDest, 'connections'));
    await tools.copyFiles(
      ['**/*'],
      path.join(sessionCppDir, 'resources', 'schema'),
      path.join(appResDest, 'schema'));
    await tools.copyFiles(
      ['**/*'],
      path.join(sessionCppDir, 'resources', 'dependencies'),
      path.join(appResDest, 'dependencies'));
    await tools.copyFiles(
      ['**/*'],
      cmakeVars.get('RSTUDIO_DEPENDENCIES_DICTIONARIES_DIR'),
      path.join(appResDest, path.basename(cmakeVars.get('RSTUDIO_DEPENDENCIES_DICTIONARIES_DIR'))));
    await tools.copyFiles(
      ['**/*'],
      cmakeVars.get('RSTUDIO_DEPENDENCIES_MATHJAX_DIR'),
      path.join(appResDest, path.basename(cmakeVars.get('RSTUDIO_DEPENDENCIES_MATHJAX_DIR'))));
    await tools.copyFiles(
      ['pandoc*'],
      cmakeVars.get('RSTUDIO_DEPENDENCIES_PANDOC_DIR'),
      path.join(binDest, 'pandoc'));
    if (cmakeVars.get('RSTUDIO_EMBEDDED_PACKAGES')) {
      // TODO - Embedded R Packages
      throw new Error('Embedding R Packages NYI for Electron packaging');
    }
    await tools.copyFiles(
      ['**/*'],
      path.join(sessionCppDir, 'resources', 'pdfjs'),
      path.join(appResDest, 'pdfjs'));
    await tools.copyFiles(
      ['**/*'],
      path.join(sessionCppDir, 'resources', 'grid'),
      path.join(appResDest, 'grid'));
    await tools.copyFiles(
      ['**/*'],
      path.join(sessionCppDir, 'resources', 'help_resources'),
      path.join(appResDest, 'help_resources'));
    await tools.copyFiles(
      ['**/*'],
      path.join(sessionCppDir, 'resources', 'pagedtable'),
      path.join(appResDest, 'pagedtable'));
    await tools.copyFiles(
      ['**/*'],
      path.join(sessionCppDir, 'resources', 'profiler'),
      path.join(appResDest, 'profiles'));
    await tools.copyFiles(
      ['**/*'],
      path.join(sessionCppDir, 'resources', 'tutorial_resources'),
      path.join(appResDest, 'tutorial_resources'));
    await tools.copyFiles(
      ['**/*'],
      path.join(sessionCppDir, 'resources', 'terminal'),
      path.join(appResDest, 'terminal'));

    // TODO : win32 has several additional things installed via session

    // src/cpp/r
    await tools.copyFiles(
      ['**/*.R'],
      path.join(cppSourceDir, 'r', 'R'),
      path.join(appDest, 'R'));

    // src/cpp/session/postback
    await tools.copyFiles(['rpostback'], path.join(cppBuildDir, 'session', 'postback'), binDest);
    await tools.copyFiles(['**/*'], path.join(cppBuildDir, 'session', 'postback', 'postback'), path.join(binDest, 'postback'));

    // src/cpp/diagnostics
    await tools.copyFiles(['diagnostics'], path.join(cppBuildDir, 'diagnostics'), binDest);

    // src/node/desktop
    await tools.copyFiles(['mac-terminal'], path.join(nodeFolder, 'desktop'), binDest);

    // src/gwt
    const gwtSourceDir = path.join(sourceDir, 'gwt');
    await tools.copyFiles(
      ['**/*'],
      path.join(gwtSourceDir, 'www'),
      path.join(appDest, 'www')
    )
     await tools.copyFiles(
      ['**/*'],
      path.join(gwtBuildDir, 'www', 'rstudio'),
      path.join(appDest, 'www', 'rstudio')
    )
    await tools.copyFiles(
      ['**/*'],
      path.join(gwtBuildDir, 'extras', 'rstudio', 'symbolMaps'),
      path.join(appDest, 'www-symbolmaps')
    )

  } catch (e) {
    console.error(e.message);
    return 1;
  }

  return 0;
}
