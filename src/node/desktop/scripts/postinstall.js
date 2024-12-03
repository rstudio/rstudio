/*
 * postinstall.js
 *
 * Copyright (C) 2024 by Posit Software, PBC
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


// https://github.com/nodejs/nan/issues/978 
const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, '../node_modules/nan/nan.h');
const fileContent = fs.readFileSync(filePath, 'utf8');
const updatedContent = fileContent.replace(/#include "nan_scriptorigin.h"/, '// #include "nan_scriptorigin.h"');

fs.writeFileSync(filePath, updatedContent, 'utf8');
