/*
 * image-util.ts
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



 export function sizePropToStyle(prop: string) {
  // if it's a number, append px
  if (/^\d*\.?\d*$/.test(prop)) {
    return prop + "px";
  } else {
    return prop;
  }
 }

 export function sizePropWithUnits(prop: string | null) {
  if (prop) {
    const match = prop.match(/(^\d*\.?\d*)(.*)$/);
    if (match) {
      return {
        size: parseFloat(match[1]),
        units: match[2]
      };
    } else {
      return null;
    }
  } else {
    return null;
  }
 
 }