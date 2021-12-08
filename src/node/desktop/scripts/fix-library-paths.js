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

  const {
    RSTUDIO_CODESIGN_USE_CREDENTIALS,
    CODESIGN_FLAGS,
    CODESIGN_TARGETS,
    CODESIGN_PLUGINS,
    GLOB_RECURSE,
    CMAKE_INSTALL_PREFIX,
    CODESIGN_FRAMEWORKS,
    CODESIGN_MACOS,
    CMAKE_CURRENT_SOURCE_DIR,
  } = results;

  console.log('------------------------- FIX LIB PATHS JS ------------------');

  console.log({
    RSTUDIO_CODESIGN_USE_CREDENTIALS,
    CODESIGN_FLAGS,
    CODESIGN_TARGETS,
    CODESIGN_PLUGINS,
    GLOB_RECURSE,
    CMAKE_INSTALL_PREFIX,
    CODESIGN_FRAMEWORKS,
    CODESIGN_MACOS,
    CMAKE_CURRENT_SOURCE_DIR,
  });

  console.log({ env: process.env });

  console.log(results);

  console.log('------------------------- FIX LIB PATHS JS ------------------');

  return results;
};

exports.loadCMakeVars = loadCMakeVars;

const getValidCliString = (str) => {
  if (str.includes(' ')) {
    return str;
  }
  throw new Error(`Invalid string: ${str}`);
};

const { exec } = require('child_process');

/**
 * This reduces the amount of code needed to run a command
 * by automatically taking care of error handling and logging.
 *
 * @param {*} expression
 * @return {*}
 */
const customExec = async (expression) => {
  return Promise((resolve, reject) => {
    exec(expression, (error, stdout, stderr) => {
      // console.log(stdout);
      // console.log(stderr);
      if (error !== null) {
        reject(`exec error when running ${expression} : ${error}`);
      }
      resolve(stdout);
    });
  });
};

const fixLibraryPaths = async function (filePath) {
  const { exec } = require('child_process');

  // exec('sh hi.sh', (error, stdout, stderr) => {
  //   console.log(stdout);
  //   console.log(stderr);
  //   if (error !== null) {
  //     console.log(`exec error: ${error}`);
  //   }
  // });
  const cache = {};

  const opts = {};

  if (!file.exists(cache.RSESSION_ARM64_PATH)) {
    console.log(`No arm64 rsession binary available at ${cache.RSESSION_ARM64_PATH}`);
  } else {
    console.log(`Found arm64 rsession binary: '${cache.RSESSION_ARM64_PATH}'`);

    // todo
    if (filePath.exists('$ENV{HOME}/homebrew/arm64')) {
      opts.HOMEBREW_ARM64_PREFIX = '$ENV{HOME}/homebrew/arm64';
    } else {
      opts.HOMEBREW_ARM64_PREFIX = '/opt/homebrew';
    }

    console.log(`Homebrew prefix: '${opts.HOMEBREW_ARM64_PREFIX}'`);

    await customExec(
      `cp ${getValidCliString(cache.RSESSION_ARM64_PATH)} ${getValidCliString(
        cache.CMAKE_INSTALL_PREFIX + '/RStudio.app/Contents/MacOS/rsession-arm64',
      )}`,
    );

    opts.HOMEBREW_LIBS = 'gettext krb5 libpq openssl sqlite3'.split(' ');

    await customExec(
      `mkdir ${getValidCliString(cache.CMAKE_INSTALL_PREFIX + '/RStudio.app/Contents/Frameworks/arm64')}`,
    );

    opts.HOMEBREW_LIBS.forEach((lib) => {
      const libPath = `${opts.HOMEBREW_ARM64_PREFIX}/opt/${lib}/lib`;

      const libFilesResult = customExec(`ls "${libPath}/*.dylib"`);

      if (Array.isArray(libFilesResult)) {
        libFilesResult.forEach((libFile) => {
          await customExec(
            `cp ${getValidCliString(libFile)} ${getValidCliString(
              cache.CMAKE_INSTALL_PREFIX + '/RStudio.app/Contents/Frameworks/arm64/',
            )}`,
          );
        });
      }
    });

    // todo where is CMAKE_CURRENT_SOURCE_DIR? maybe rstudio app folder? need to check if this will run before packaging dmg then! or if this can edit the app file inside dmg
    // todo where is @executable_path? maybe inside fix-library-paths.sh?
    await customExec(
      `${getValidCliString(opts.CMAKE_CURRENT_SOURCE_DIR + '/fix-library-paths.sh')} ${getValidCliString(
        cache.CMAKE_INSTALL_PREFIX + '/RStudio.app/Contents/Frameworks/arm64',
      )} @executable_path/../Frameworks/arm64 *.dylib`,
    );
    await customExec(
      `${getValidCliString(opts.CMAKE_CURRENT_SOURCE_DIR + '/fix-library-paths.sh')} ${getValidCliString(
        cache.CMAKE_INSTALL_PREFIX + '/RStudio.app/Contents/MacOS',
      )} @executable_path/../Frameworks/arm64 rsession-arm64`,
    );
  }

  await customExec(
    `${getValidCliString(opts.CMAKE_CURRENT_SOURCE_DIR + '/fix-library-paths.sh')} ${getValidCliString(
      cache.CMAKE_INSTALL_PREFIX + '/RStudio.app/Contents/Frameworks',
    )} @executable_path/../Frameworks *.dylib`,
  );
  await customExec(
    `${getValidCliString(opts.CMAKE_CURRENT_SOURCE_DIR + '/fix-library-paths.sh')} ${getValidCliString(
      cache.CMAKE_INSTALL_PREFIX + '/RStudio.app/Contents/MacOS',
    )} @executable_path/../Frameworks RStudio diagnostics rpostback rsession`,
  );
};

exec(`ls dasdasdas`, (error, stdout, stderr) => {
  console.log({ stdout });
  console.log({ stderr });
  if (error !== null) {
    console.log(`exec error: ${error}`);
  }
  // console.log(stdout.split('\n').filter((line) => line.length > 0));
});

// exec('mkdir "~/Rstudio/rstudio/src/node/desktop/scripts/test\ dir"', (error, stdout, stderr) => {
//   console.log({ stdout });
//   console.log({ stderr });
//   if (error !== null) {
//     console.log(`exec error: ${error}`);
//   }
// });
