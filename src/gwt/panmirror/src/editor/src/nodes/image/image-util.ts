import { ImageProps, attrPartitionKeyvalue } from "../../api/ui";
import { imageSizePropWithUnit, isValidImageSizeUnit } from "../../api/image";
import { EditorView } from "prosemirror-view";
import { findParentNodeClosestToPos } from "prosemirror-utils";

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

const kDpi = 96;

export function isNaturalAspectRatio(width: number, height: number, img: HTMLImageElement, defaultValue: boolean) {
  if (img.naturalWidth && img.naturalHeight) {
    const diff = Math.abs(width / height - img.naturalWidth / img.naturalHeight);
    return diff <= 0.02;
  } else {
    // no naturalWidth or naturalHeight, return default
    return defaultValue;
  }
}

export function unitToPixels(value: number, unit: string, containerWidth: number) {
  let pixels;
  switch (unit) {
    case 'in':
      pixels = value * kDpi;
      break;
    case 'mm':
      pixels = value * (kDpi / 25.4);
      break;
    case 'cm':
      pixels = value * (kDpi / 2.54);
      break;
    case '%':
      pixels = (value / 100) * ensureContainerWidth(containerWidth);
      break;
    case 'px':
    default:
      pixels = value;
      break;
  }
  return Math.round(pixels);
}

export function pixelsToUnit(pixels: number, unit: string, containerWidth: number) {
  switch (unit) {
    case 'in':
      return pixels / kDpi;
    case 'mm':
      return (pixels / kDpi) * 25.4;
    case 'cm':
      return (pixels / kDpi) * 2.54;
    case '%':
      return (pixels / ensureContainerWidth(containerWidth)) * 100;
    case 'px':
    default:
      return pixels;
  }
}

export function roundUnit(value: number, unit: string) {
  switch (unit) {
    case 'in':
      return value.toFixed(2);
    case 'cm':
      return value.toFixed(1);
    default:
      return Math.round(value).toString();
  }
}


export function imagePropsWithSizes(image: ImageProps) {

  // pull width, height, and units out of keyvalue if necessary
  // (enables front-ends to provide dedicated UI for width/height)
  // note that if the value doesn't use a unit supported by the 
  // UI it's kept within the original keyvalue prop
  if (image.keyvalue) {
    let width : number | undefined;
    let height : number | undefined;
    let units : string | undefined;
    const partitionedKeyvalue = attrPartitionKeyvalue(["width", "height"], image.keyvalue);
    for (const kv of partitionedKeyvalue.partitioned) {
      const [key, value] = kv;
      let partitioned = false;
      const sizeWithUnit = imageSizePropWithUnit(value);
      if (sizeWithUnit) {
        sizeWithUnit.unit = sizeWithUnit.unit || 'px';
        if (isValidImageSizeUnit(sizeWithUnit.unit)) {
          if (key === "width") {
            width = sizeWithUnit.size;
            units = sizeWithUnit.unit;
          } else if (key === "height") {
            height = sizeWithUnit.size;
            units = units || sizeWithUnit.unit;
          }
          partitioned = true;
        }
      }
      if (!partitioned) {
        partitionedKeyvalue.base.push(kv);
      }
    }
    return {
      ...image,
      width,
      height,
      units,
      lockRatio: true,
      keyvalue: partitionedKeyvalue.base
    };
  } else {
    return image;
  }
}

export function imageDimensionsFromImg(img: HTMLImageElement, containerWidth: number) {
  return {
    naturalWidth: img.naturalWidth || null,
    naturalHeight: img.naturalHeight || null,
    containerWidth: ensureContainerWidth(containerWidth)
  };
}

export function hasPercentWidth(size: string | null) {
  return !!size && size.endsWith('%');
}

export function imageContainerWidth(pos: number, view: EditorView) {
  
  let containerWidth = (view.dom as HTMLElement).offsetWidth;
  if (containerWidth > 0) {
    if (pos) {
      const imagePos = view.state.doc.resolve(pos);
      const resizeContainer = findParentNodeClosestToPos(imagePos, nd => nd.isBlock);
      if (resizeContainer) {
        const resizeEl = view.domAtPos(resizeContainer.pos);
        containerWidth = (resizeEl.node as HTMLElement).offsetWidth;
      }
    }
  }

  return containerWidth;
}


// sometime when we are called before the DOM renders the containerWidth
// is 0, in this case provide a default of 1000
function ensureContainerWidth(containerWidth: number) {
  return containerWidth || 1000;
}
