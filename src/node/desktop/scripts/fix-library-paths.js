const eachLine = async function (filename, iteratee) {
  return new Promise(function (resolve, reject) {
    const lineReader = require('line-reader');
    lineReader.eachLine(filename, iteratee, function (err) {
      if (err) {
        reject(err);
      } else {
        resolve();
      }
    });
  });
};

const loadCMakeVars = async (filePath) => {
  const results = {};

  await eachLine(filePath, (line) => {
    line = line.trim();
    if (line.length > 0) {
      if (!line.startsWith('//') && !line.startsWith('#')) {
        const match = /^(.+):.+=(.+)/.exec(line);
        if (match) {
          results[match[1]] = match[2];
        }
      }
    }
  });

  // const {
  //   RSTUDIO_CODESIGN_USE_CREDENTIALS,
  //   CODESIGN_FLAGS,
  //   CODESIGN_TARGETS,
  //   CODESIGN_PLUGINS,
  //   GLOB_RECURSE,
  //   CMAKE_INSTALL_PREFIX,
  //   CODESIGN_FRAMEWORKS,
  //   CODESIGN_MACOS,
  //   CMAKE_CURRENT_SOURCE_DIR,
  // } = results;

  // console.log('------------------------- FIX LIB PATHS JS ------------------');

  // console.log({
  //   RSTUDIO_CODESIGN_USE_CREDENTIALS,
  //   CODESIGN_FLAGS,
  //   CODESIGN_TARGETS,
  //   CODESIGN_PLUGINS,
  //   GLOB_RECURSE,
  //   CMAKE_INSTALL_PREFIX,
  //   CODESIGN_FRAMEWORKS,
  //   CODESIGN_MACOS,
  //   CMAKE_CURRENT_SOURCE_DIR,
  // });

  // console.log({ env: process.env });

  // console.log(results);

  // console.log('------------------------- FIX LIB PATHS JS ------------------');

  return results;
};

exports.loadCMakeVars = loadCMakeVars;

const getValidCliString = (str) => {
  const trimmedString = str.trim();
  if (!trimmedString.includes(' ')) {
    return trimmedString;
  }
  throw new Error(`Invalid string: ${str}`);
};

const { exec, execFile } = require('child_process');

/**
 * This reduces the amount of code needed to run a command
 * by automatically taking care of error handling and logging.
 *
 * @param {*} expression
 * @return {*}
 */
const customExec = async (expression, debug = false) => {
  console.log('-----------------------------');
  console.log('customExec');
  console.log('-------');

  console.log({ expression });
  console.log('-------');

  return new Promise((resolve, reject) => {
    exec(expression, (error, stdout, stderr) => {
      if (debug) {
        console.log({ stdout });
        console.log({ stderr });
      }
      console.log('-----------------------------');

      if (error !== null) {
        reject(`exec error when running ${expression} : ${error}`);
      }
      resolve(stdout);
    });
  });
};

const customExecWithArgs = async (expression, args = []) => {
  console.log('-----------------------------');
  console.log('customExec');
  console.log('-------');

  console.log({ expression });
  console.log('-------');
  console.log({ args });
  console.log('-------');

  return new Promise((resolve, reject) => {
    execFile(expression, args, (error, stdout, stderr) => {
      console.log({ stdout });
      console.log({ stderr });

      console.log('-----------------------------');

      if (error !== null) {
        reject(`exec error when running ${expression} : ${error}`);
      }
      resolve(stdout);
    });
  });
};

const fixLibraryPaths = async function (filePath) {
  const { exec } = require('child_process');
  const fs = require('fs');

  const cache = await loadCMakeVars(__dirname + '/../../../../package/osx/build/CMakeCache.txt');
  // console.log('cache: ');
  // console.log(cache);

  const opts = {};
  opts.CMAKE_CURRENT_SOURCE_DIR = `${__dirname}/../../../../package/osx`;

  const { getPlatformName, getArchName } = require('./create-full-package-file-name.js');
  opts.CMAKE_INSTALL_PREFIX = `${__dirname}/../out/RStudio-${getPlatformName(false)}-${getArchName()}`;
  opts.appBundlePath = `${getValidCliString(opts.CMAKE_INSTALL_PREFIX + '/RStudio.app')}`;
  opts.fixLibraryPathsScriptPath = getValidCliString(opts.CMAKE_CURRENT_SOURCE_DIR + '/fix-library-paths.sh');

  console.log('CMAKE_INSTALL_PREFIX');
  console.log(opts.CMAKE_INSTALL_PREFIX);

  const homedir = require('os').homedir();

  const dylibFiles = await customExec(`find ${opts.CMAKE_INSTALL_PREFIX} -type f -iname "*.dylib"`).then(
    (filePathLines) =>
      `${filePathLines
        .split('\n')
        .filter((line) => line.length > 0)
        .join('\n')
        .replace(/ /g, '\\ ')
        .replace(/\n/g, ' ')}`,
    // .replace(/ /g, '\\ ').replace(/\n/g, ' ')}`,

    // .split('\n')
    // .map((value) => `'${value}'`)
    // .join(' ')}`,
  );

  if (!fs.existsSync(cache.RSESSION_ARM64_PATH)) {
    console.log(`No arm64 rsession binary available at ${cache.RSESSION_ARM64_PATH}`);
  } else {
    console.log(`Found arm64 rsession binary: '${cache.RSESSION_ARM64_PATH}'`);

    // todo
    if (fs.existsSync(`${homedir}/homebrew/arm64`)) {
      opts.HOMEBREW_ARM64_PREFIX = `${homedir}/homebrew/arm64`;
    } else {
      opts.HOMEBREW_ARM64_PREFIX = '/opt/homebrew';
    }

    console.log(`Homebrew prefix: '${opts.HOMEBREW_ARM64_PREFIX}'`);

    await customExec(
      `cp ${getValidCliString(cache.RSESSION_ARM64_PATH)} ${opts.appBundlePath}/Contents/MacOS/rsession-arm64`,
    );

    opts.HOMEBREW_LIBS = 'gettext krb5 libpq openssl sqlite3'.split(' ');

    await customExec(`mkdir ${opts.appBundlePath}/Contents/Frameworks/arm64`);

    opts.HOMEBREW_LIBS.forEach((lib) => {
      const libPath = `${opts.HOMEBREW_ARM64_PREFIX}/opt/${lib}/lib`;

      const libFilesResult = customExec(`ls "${libPath}/*.dylib"`);

      if (Array.isArray(libFilesResult)) {
        libFilesResult.forEach(async (libFile) => {
          await customExec(`cp ${getValidCliString(libFile)} ${opts.appBundlePath}/Contents/Frameworks/arm64/`);
        });
      }
    });

    console.log(' opts.CMAKE_CURRENT_SOURCE_DIR');
    console.log(opts.CMAKE_CURRENT_SOURCE_DIR);

    // todo where is CMAKE_CURRENT_SOURCE_DIR? maybe rstudio app folder? need to check if this will run before packaging dmg then! or if this can edit the app file inside dmg
    // todo where is @executable_path? maybe inside fix-library-paths.sh?
    await customExec(
      `sh ${opts.fixLibraryPathsScriptPath} ${opts.appBundlePath}/Contents/Frameworks/arm64 @executable_path/../Frameworks/arm64 ${dylibFiles}`,
    );
    await customExec(
      `sh ${opts.fixLibraryPathsScriptPath} ${opts.appBundlePath}/Contents/MacOS @executable_path/../Frameworks/arm64 "rsession-arm64"`,
    );
  }

  await customExec(
    `sh ./fix-specific-scripts/fix-dylib.sh ${opts.fixLibraryPathsScriptPath} ${opts.appBundlePath}/Contents/Frameworks @executable_path/../Frameworks`,
    true,
  );

  await customExec(
    `sh ./fix-specific-scripts/fix-any.sh ${opts.fixLibraryPathsScriptPath} ${opts.appBundlePath}/Contents/MacOS @executable_path/../Frameworks "RStudio diagnostics rpostback rsession"`,
    true,
  );

  // await customExecWithArgs(`${opts.fixLibraryPathsScriptPath}`, [
  //   `${opts.appBundlePath}/Contents/Frameworks`,
  //   `@executable_path/../Frameworks`,
  //   `${dylibFiles}`,
  // ]);

  // await customExecWithArgs(`${opts.fixLibraryPathsScriptPath}`, [
  //   `${opts.appBundlePath}/Contents/MacOS`,
  //   `@executable_path/../Frameworks`,
  //   "RStudio diagnostics rpostback rsession"
  // ]);
};

async function test() {
  const { getPlatformName, getArchName } = require('./create-full-package-file-name.js');

  const opts = {};
  opts.CMAKE_INSTALL_PREFIX = `${__dirname}/../out/RStudio-${getPlatformName(false)}-${getArchName()}`;

  const dylibFiles = await customExec(`find ${opts.CMAKE_INSTALL_PREFIX} -type f -iname "*.dylib"`).then(
    (filePathLines) =>
      `${filePathLines
        .split('\n')
        .filter((line) => line.length > 0)
        .join('\n')
        .replace(/ /g, '\\ ')
        .replace(/\n/g, ' ')}`,
    // (filePathLines) =>
    //   `"${filePathLines
    //     .replace(/ /g, '\\ ')
    //     .split('\n')
    //     .map((value) => `'${value}'`)
    //     .join(' ')}"`,
  );
  // const dylibFiles = await customExec(`find ${opts.CMAKE_INSTALL_PREFIX} -type f -iname "*.dylib"`).then(
  //   (filePathLines) => `"${filePathLines.replace(/ /g, '\\\ ').replace(/\n/g, ' ')}"`,
  // );

  console.log(dylibFiles);
}

// test();
fixLibraryPaths();

// exec(`ls dasdasdas`, (error, stdout, stderr) => {
//   console.log({ stdout });
//   console.log({ stderr });
//   if (error !== null) {
//     console.log(`exec error: ${error}`);
//   }
//   // console.log(stdout.split('\n').filter((line) => line.length > 0));
// });

// exec('mkdir "~/Rstudio/rstudio/src/node/desktop/scripts/test\ dir"', (error, stdout, stderr) => {
//   console.log({ stdout });
//   console.log({ stderr });
//   if (error !== null) {
//     console.log(`exec error: ${error}`);
//   }
// });
