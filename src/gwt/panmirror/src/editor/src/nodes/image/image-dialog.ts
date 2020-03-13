/*
 * image-dialog.ts
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

import { Node, NodeType, Fragment } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { insertAndSelectNode } from '../../api/node';
import { ImageProps, ImageType, EditorUI } from '../../api/ui';
import { extractSizeStyles } from '../../api/css';

export async function imageDialog(
  node: Node | null,
  nodeType: NodeType,
  state: EditorState,
  dispatch: (tr: Transaction<any>) => void,
  view: EditorView | undefined,
  editorUI: EditorUI,
  imageAttributes: boolean,
) {
  // if we are being called with an existing node then read it's attributes
  let content = Fragment.empty;
  let image: ImageProps = { src: null };
  if (node && node.type === nodeType) {
    image = {
      ...(node.attrs as ImageProps),
      alt: node.textContent || node.attrs.alt,
    };
    content = node.content;
  } else {
    image = nodeType.create(image).attrs as ImageProps;
  }

  // determine the type
  const type = nodeType === state.schema.nodes.image ? ImageType.Image : ImageType.Figure;

  // edit the image
  const result = await editorUI.dialogs.editImage(image, editorUI.context.getResourceDir(), imageAttributes);
  if (result) {
    // figures treat 'alt' as their content (the caption), but since captions support
    // inline formatting (and the dialog doesn't) we only want to update the
    // content if the alt/caption actually changed (as it will blow away formatting)
    if (type === ImageType.Figure) {
      if (image.alt !== result.alt) {
        if (result.alt) {
          content = Fragment.from(state.schema.text(result.alt));
        } else {
          content = Fragment.empty;
        }
      }
    }

    // move width and height out of style if necessary
    const imageProps = {
      ...result,
      keyvalue: extractSizeStyles(result.keyvalue)
    };

    // create the image
    const newImage = nodeType.createAndFill(imageProps, content);

    // insert and select
    if (newImage) {
      insertAndSelectNode(newImage, state, dispatch);
    }
  }

  if (view) {
    view.focus();
  }
}
