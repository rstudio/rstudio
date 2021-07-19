/*
 * template-filter.ts
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

import fs from 'fs';

import { FilePath } from './file-path';
import { htmlEscape, jsLiteralEscape } from './string-utils';

// Add variables to templates using #foo# syntax. All values will be
// HTML-escaped automatically unless prepended with !, e.g. #!foo# will
// use the raw value of foo. Alternatively, you can prepend with ' and JS
// literal escaping will be used instead of HTML escaping (i.e. the value
// will be prepared for inserting into a JavaScript string literal).

export function renderTemplateFile(templateFile: FilePath, vars: Map<string, string>): string {
  const contents = fs.readFileSync(templateFile.getAbsolutePath(), 'utf-8');
  return renderTemplateString(contents, vars);
}

export function renderTemplateString(template: string, vars: Map<string, string>): string {
  return template.replace(/#([!\\']?)([A-Za-z0-9_-]+)#/g, (match, prefix, varName) => {
    const substitute = vars.get(varName);
    if (substitute) {
      if (prefix === '!') {
        return substitute;
      } else if (prefix === '\'') {
        return jsLiteralEscape(substitute);
      } else {
        return htmlEscape(substitute, true);
      }
    } else {
      return 'MISSING VALUE';
    }
  });
}