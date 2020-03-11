import { mathAppendMarkTransaction } from "../../marks/math/math-transaction";

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



export function pixelsToUnit(pixels: number, unit: string, containerWidth: number) {
  const dpi = 96;
  switch(unit) {
    case "in":
      return roundUnit(pixels / dpi, unit) + unit;
    case "mm":
      return roundUnit((pixels / dpi) * 25.4, unit) + unit;
    case "cm":
      return roundUnit((pixels / dpi) * 2.54, unit) + unit;
    case "%":
      return roundUnit((pixels / containerWidth) * 100, unit) + unit;
    case "px":
    default:
      return roundUnit(pixels, unit) + unit;
  }
}

export function roundUnit(value: number, unit: string)  {
  switch(unit) {
    case "in":
      return value.toFixed(2);
    case "cm":
      return value.toFixed(1);
    default:
      return Math.round(value).toString();
  }
}

export function imageDimsWithUnits(img: HTMLImageElement) {
  const width = sizePropWithUnit(img.style.width || (img.offsetWidth + 'px'))!;
  const height = sizePropWithUnit(img.style.height || (img.offsetHeight + 'px'))!;
  return {
    width,
    height
  };
}

export function sizePropToStyle(prop: string) {
  // if it's a number, append px
  if (/^\d*\.?\d*$/.test(prop)) {
    return prop + "px";
  } else {
    return prop;
  }
}

export function sizePropWithUnit(prop: string | null) {
  if (prop) {
    const match = prop.match(/(^\d*\.?\d*)(.*)$/);
    if (match) {
      return {
        size: parseFloat(match[1]),
        unit: match[2]
      };
    } else {
      return null;
    }
  } else {
    return null;
  }
 
}