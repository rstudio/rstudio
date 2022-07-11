/*
 * generate.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import { access, constants, mkdir, writeFileSync } from 'fs';
import { compileFromFile } from 'json-schema-to-typescript';
import path from 'path';

// schemas to convert to types
const schemas: string[] = ['../../cpp/session/resources/schema/user-state-schema.json'];

schemas.forEach(generateTypes);

function generateTypes(filename: string) {
  access(filename, constants.F_OK, (error) => {
    if (error) {
      console.warn(`Cannot generate type for ${filename}: ${error.message}`);
      return;
    }

    const outputFile = path.parse(filename);
    console.log(`Generating type for ${outputFile.base}`);

    // generate types from json schema
    compileFromFile(filename, {
      style: { singleQuote: true },
    }).then((output) => {
      // json-schema-to-typescript generates [k: string]: unknown
      const searchPattern = /^\s+\[k: string\]: unknown;\n/gm;
      const filtered = output.replace(searchPattern, '');
      mkdir('src/types', { recursive: true }, (error) => {
        if (!error) {
          writeFileSync(`src/types/${outputFile.name}.d.ts`, filtered);
        } else {
          console.log(error);
        }
      });
    });
  });
}
