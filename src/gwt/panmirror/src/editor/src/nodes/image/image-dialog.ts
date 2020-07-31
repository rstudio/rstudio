/*
 * image-dialog.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import { Node as ProsemirrorNode, NodeType, Fragment, Mark } from 'prosemirror-model';
import { EditorView } from 'prosemirror-view';

import { insertAndSelectNode } from '../../api/node';
import { EditorUI } from '../../api/ui';
import { ImageProps } from '../../api/ui-dialogs';
import { extractSizeStyles, kPercentUnit, kPixelUnit } from '../../api/css';
import { ImageType, ImageDimensions, isNaturalAspectRatio } from '../../api/image';
import { kWidthAttrib, kHeightAttrib } from '../../api/pandoc_attr';

import { imagePropsWithSizes, hasPercentWidth } from './image-util';

export async function imageDialog(
  node: ProsemirrorNode | null,
  dims: ImageDimensions | null,
  nodeType: NodeType,
  view: EditorView,
  editorUI: EditorUI,
  imageAttributes: boolean,
) {
  // alias schema
  const schema = view.state.schema;

  // if we are being called with an existing node then read it's attributes
  let content = Fragment.empty;
  let image: ImageProps = { src: null };
  let linkMark: Mark | undefined;
  let marks: Mark[] = [];
  if (node && dims && node.type === nodeType) {
    // base attributess
    image = {
      ...(node.attrs as ImageProps),
      alt: nodeType === schema.nodes.figure ? node.textContent : node.attrs.alt,
    };

    // move width and height out of style and into keyvalue if necessary
    image = {
      ...image,
      keyvalue: extractSizeStyles(image.keyvalue),
    };

    // move width, height, and units out of keyvalue into explicit
    // top level image properties if necessary
    image = imagePropsWithSizes(image, dims);

    // record marks / linkTo
    if (nodeType === schema.nodes.image) {
      marks = node.marks;
      linkMark = node.marks.find(mark => mark.type === schema.marks.link);
      if (linkMark) {
        image.linkTo = linkMark.attrs.href;
      }
    }

    // content (will be caption for figures)
    content = node.content;
  } else {
    // create a new image
    image = nodeType.create(image).attrs as ImageProps;
  }

  // determine the type
  const type = nodeType === view.state.schema.nodes.image ? ImageType.Image : ImageType.Figure;

  // edit the image
  const result = await editorUI.dialogs.editImage(image, dims, imageAttributes);
  if (result) {
    // figures treat 'alt' as their content (the caption), but since captions support
    // inline formatting (and the dialog doesn't) we only want to update the
    // content if the alt/caption actually changed (as it will blow away formatting)
    if (type === ImageType.Figure && image.alt !== result.alt) {
      if (result.alt) {
        content = Fragment.from(view.state.schema.text(result.alt));
      } else {
        content = Fragment.empty;
      }
    }

    // if we have width and height move them into keyvalue
    let keyvalue = result.keyvalue;
    if (result.units) {
      // no units for px
      const units = result.units && result.units === kPixelUnit ? '' : result.units;
      // width
      if (result.width) {
        let width = result.width;
        if (hasPercentWidth(units)) {
          width = Math.min(width, 100);
        }
        keyvalue = keyvalue || [];
        keyvalue.push([kWidthAttrib, width + units]);
      }
      // only record height if it's not % units and it's not at it's natural height
      if (result.height && units !== kPercentUnit && !isNaturalHeight(result.width, result.height, dims)) {
        keyvalue = keyvalue || [];
        keyvalue.push([kHeightAttrib, result.height + units]);
      }
    }

    // merge updated keyvalue
    const imageProps = { ...result, keyvalue };

    // update or create link mark as necessary
    if (nodeType === schema.nodes.image) {
      if (linkMark) {
        marks = linkMark.removeFromSet(marks);
        if (imageProps.linkTo) {
          linkMark = linkMark.type.create({ ...linkMark.attrs, href: imageProps.linkTo });
        }
      } else if (imageProps.linkTo) {
        linkMark = schema.marks.link.create({ href: imageProps.linkTo });
      }
      if (imageProps.linkTo && linkMark) {
        marks = linkMark.addToSet(marks);
      }
    }

    // create the image
    const newImage = nodeType.createAndFill(imageProps, content, marks);

    // insert and select
    if (newImage) {
      insertAndSelectNode(view, newImage);
    }
  }

  if (view) {
    view.focus();
  }
}

// wrapper for isNaturalHeight that handles potentially undefined params
function isNaturalHeight(width: number | undefined, height: number | undefined, dims: ImageDimensions | null) {
  if (width && height && dims) {
    return isNaturalAspectRatio(width, height, dims, false);
  } else {
    return false;
  }
}
