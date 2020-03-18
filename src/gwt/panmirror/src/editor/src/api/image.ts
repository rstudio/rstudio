
/*
 * image.ts
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

 // https://github.com/jgm/pandoc/blob/master/src/Text/Pandoc/ImageSize.hs
const kValidUnits = ['px', 'in', 'cm', 'mm', '%']; //

export interface ImageDimensions {
  naturalWidth: number | null;
  naturalHeight: number | null;
  containerWidth: number;
}

export function validImageSizeUnits() {
  return kValidUnits;
}

export function isValidImageSizeUnit(unit: string) {
  return kValidUnits.includes(unit);
}

export function imageSizePropWithUnit(prop: string | null) {
  if (prop) {
    const match = prop.match(/(^\d*\.?\d*)(.*)$/);
    if (match) {
      return {
        size: parseFloat(match[1]),
        unit: match[2],
      };
    } else {
      return null;
    }
  } else {
    return null;
  }
}