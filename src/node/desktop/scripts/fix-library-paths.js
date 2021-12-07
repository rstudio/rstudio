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
  const results = new Map();

  await eachLine(filePath, (line) => {
    line = line.trim();
    if (line.length > 0) {
      if (!line.startsWith('//') && !line.startsWith('#')) {
        const match = /^(.+):.+=(.+)/.exec(line);
        if (match) {
          results.set(match[1], match[2]);
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
