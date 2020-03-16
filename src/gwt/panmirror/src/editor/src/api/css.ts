/*
 * css.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

export function removeStyleAttrib(style: string, attrib: string, handler?: (attrib: string, value: string) => void) {
  return style.replace(new RegExp('(' + attrib + ')\\:\\s*(\\w+)', 'g'), (_match, p1, p2) => {
    if (handler) {
      handler(p1, p2);
    }
    return '';
  });
}

export function extractSizeStyles(keyvalues: Array<[string, string]> | undefined) {
  if (!keyvalues) {
    return keyvalues;
  }

  let newKeyvalues = keyvalues;

  const getValue = (key: string) => {
    const pair = newKeyvalues.find(keyvalue => keyvalue[0] === key);
    return pair ? pair[1] : null;
  };

  const setValue = (key: string, value: string | null) => {
    newKeyvalues = newKeyvalues.filter(keyvalue => keyvalue[0] !== key);
    if (value) {
      newKeyvalues.push([key, value]);
    }
  };

  let width = getValue('width');
  let height = getValue('height');
  let style = getValue('style');

  if (style) {
    style = removeStyleAttrib(style, 'width', (_attrib, value) => {
      if (!width) {
        width = value;
      }
    });
    style = removeStyleAttrib(style, 'height', (_attrib, value) => {
      if (!height) {
        height = value;
      }
    });

    // remove leading ; from style
    style = style.replace(/^\s*;+\s*/, '').trimLeft();
  }

  setValue('width', width);
  setValue('height', height);
  setValue('style', style);

  return newKeyvalues;
}
