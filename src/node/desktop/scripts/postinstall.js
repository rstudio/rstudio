/*
 * postinstall.js
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

// this script will run after `npm i` completes, and before `electron-rebuild`
// builds our native dependencies against the Electron headers

const fs = require('fs');
const path = require('path');

const ADDON_GYPI = path.join(__dirname, '..', 'node_modules', '@electron', 'node-gyp', 'addon.gypi');
const DEFINE = 'V8_DEPRECATION_WARNINGS=1';
const DEFINE_COMMENT = '# Warn when using deprecated V8 APIs.';

/**
 * Electron 43 ships V8 15, where v8::String::Value carries a class-head
 * deprecation:
 *
 *     class V8_DEPRECATED("...") V8_EXPORT Value {
 *
 * With V8_DEPRECATION_WARNINGS defined that expands to a C++11 attribute
 * followed by a GNU attribute, which GCC <= 12 cannot parse in either order.
 * Our older Linux build images ship GCC 11, so every native module that
 * includes v8.h -- msgpackr-extract, and unix-dgram by way of nan.h -- fails
 * to build there. addon.gypi adds the define to every addon electron-rebuild
 * builds, so dropping it there covers all of them.
 *
 * Remove this once the build images move to GCC 13 or later, or once the
 * Electron headers stop mixing the two attribute syntaxes.
 * See https://github.com/electron/electron/issues/53284.
 */
function removeV8DeprecationWarningsDefine() {
  if (!fs.existsSync(ADDON_GYPI)) {
    throw new Error(
      `postinstall: no addon.gypi at ${ADDON_GYPI}, so the ${DEFINE} define could not be removed. ` +
        'Run `npm install` in src/node/desktop first.',
    );
  }

  const contents = fs.readFileSync(ADDON_GYPI, 'utf8');
  if (!contents.includes(DEFINE)) {
    // already removed, either by a previous run or by @electron/node-gyp itself
    return;
  }

  const patched = contents
    .split('\n')
    .filter((line) => !line.includes(DEFINE) && line.trim() !== DEFINE_COMMENT)
    .join('\n');

  if (patched.includes(DEFINE)) {
    throw new Error(`postinstall: failed to remove the ${DEFINE} define from ${ADDON_GYPI}.`);
  }

  fs.writeFileSync(ADDON_GYPI, patched);
  console.log(`postinstall: removed the ${DEFINE} define from ${ADDON_GYPI}`);
}

removeV8DeprecationWarningsDefine();
