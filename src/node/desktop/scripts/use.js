/*
 * use.js
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

const path = require('path');
const fs = require('fs');

if (process.argv.length < 3 || process.argv[2] == '--help') {
    console.log("Usage: ./scripts/use.js [development|release]");
    process.exit(0);
}

const name = process.argv[2];
const src = `config/${name}/config.ts`
if (!fs.existsSync(src)) {
    console.error(`${src} does not exist`);
    process.exit(1);
}

const dst = 'src/config/config.ts'
try {
    const parent = path.dirname(dst);
    if (!fs.existsSync(parent)) {
        fs.mkdirSync(path.dirname(dst));
    }
    fs.copyFileSync(src, dst);
} catch (error) {
    console.error(error);
    process.exit(1);
}

process.exit(0);
