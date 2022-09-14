/*
 * template-filter.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { htmlEscape, jsLiteralEscape } from './string-utils';

// Add variables to templates using #foo# syntax. All values will be
// HTML-escaped automatically unless prepended with !, e.g. #!foo# will
// use the raw value of foo. Alternatively, you can prepend with ' and JS
// literal escaping will be used instead of HTML escaping (i.e. the value
// will be prepared for inserting into a JavaScript string literal).

export function resolveTemplateVar(varName: string, vars: Map<string, string>): string {
  if (!varName) {
    return 'NO PROPERTY PROVIDED';
  }
  if (varName) {
    // If varName starts with ! then raw value is returned; if it starts with ' then
    // JS literal escaping will be used; no prefix then the returned value will be
    // HTML escaped
    let prefix = '';
    if (varName.startsWith('!')) {
      prefix = '!';
      varName = varName.slice(1);
    } else if (varName.startsWith('\'')) {
      prefix = '\'';
      varName = varName.slice(1);
    }
    const result = vars.get(varName);
    if (result) {
      if (prefix === '!') {
        return result;
      } else if (prefix === '\'') {
        return jsLiteralEscape(result);
      } else {
        return htmlEscape(result, true);
      }
    }
  }
  return 'MISSING VALUE';
}
