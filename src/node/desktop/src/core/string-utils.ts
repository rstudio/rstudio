/*
 * string-utils.ts
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

export function removeDups(strings: string[]): string[] {
  return [...new Set(strings)];
}

const nonAttrHtml = /[<>&'"/]/g;
const attrHtml = /[<>&'"/\r\n]/g;

export function htmlEscape(str: string, isAttributeValue: boolean): string {
  return str.replace(isAttributeValue ? attrHtml : nonAttrHtml, (match) => {
    switch (match) {
      case '<':
        return '&lt;';
      case '>':
        return '&gt;';
      case '&':
        return '&amp;';
      case '\'':
        return '&#x27;';
      case '"':
        return '&quot;';
      case '/':
        return '&#x2F;';
      case '\r':
        return '&#13;';
      case '\n':
        return '&#10;';
      default:
        return match;
    }
  });
}

const jsLiteral = /[\\'"\r\n<]/g;

export function jsLiteralEscape(str: string): string {
  return str.replace(jsLiteral, (match) => {
    switch (match) {
      case '\\':
        return '\\\\';
      case '\'':
        return '\\\'';
      case '"':
        return '\\"';
      case '\r':
        return '\\r';
      case '\n':
        return '\\n';
      case '<':
        return '\\074';
      default:
        return match;
    }
  });
}
