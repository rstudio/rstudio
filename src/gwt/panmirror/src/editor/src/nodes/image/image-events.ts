/*
 * image-events.ts
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

import { NodeType } from 'prosemirror-model';
import { EditorView } from 'prosemirror-view';
import { findParentNodeClosestToPos } from 'prosemirror-utils';

export function imageDrop() {
  return (view: EditorView, event: Event) => {
    // alias to drag event so typescript knows about event.dataTransfer
    const dragEvent = event as DragEvent;

    // ensure we have data transfer
    if (!dragEvent.dataTransfer) {
      return false;
    }

    // ensure the drop coordinates map to an editor position
    const coordinates = view.posAtCoords({
      left: dragEvent.clientX,
      top: dragEvent.clientY,
    });
    if (!coordinates) {
      return false;
    }

    // see if this is a drag of image uris
    const uriList = dragEvent.dataTransfer.getData('text/uri-list');
    if (!uriList) {
      return false;
    }

    // see whether this is a figure or image drop (image if it's immediate parent supports inline content)
    const schema = view.state.schema;
    let nodeType = schema.nodes.image;
    const dropNode = findParentNodeClosestToPos(view.state.doc.resolve(coordinates.pos), () => true);
    if (!dropNode || !dropNode.node.inlineContent) {
      nodeType = schema.nodes.figure;
    }

    // insert the images (track whether we handled at least one)
    let insertedImage = false;
    uriList.split('\r?\n').forEach(src => {

      // get extension and check it it's an image
      // https://developer.mozilla.org/en-US/docs/Web/Media/Formats/Image_types#Common_image_file_types
      const kImageExtensions = [
        'apng', 'bmp', 'gif', 'ico', 'cur',  'jpg', 'jpeg', 'jfif',
        'pjpeg', 'pjp', 'png', 'svg', 'tiff', 'webp' 
      ];
      const extension = src.split(/\./).pop()!.toLowerCase();
      if (kImageExtensions.includes(extension)) {
        insertedImage = true;
        const node = nodeType.create({ src });
        const tr = view.state.tr.insert(coordinates.pos, node);
        tr.scrollIntoView();
        view.dispatch(tr);
      }

    });

    // if we inserted an image then indicate that we handled the drop
    if (insertedImage) {
      event.preventDefault();
      return true;
    } else {
      return false;
    }
  };
}
