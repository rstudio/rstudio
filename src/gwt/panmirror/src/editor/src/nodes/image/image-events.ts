/*
 * image-events.ts
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

import { EditorView } from 'prosemirror-view';

import { Plugin, PluginKey } from 'prosemirror-state';
import { EditorUI } from '../../api/ui';

const pluginKey = new PluginKey('image-events');

export function imageEventsPlugin(ui: EditorUI) {
  return new Plugin({
    key: pluginKey,
    props: {
      handleDOMEvents: {
        drop: imageDrop(ui),
      },
    },
  });
}

function imageDrop(ui: EditorUI) {
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

    // array of uris
    let uris: string[] | null = null;

    // see if this is a drag of uris
    const uriList = dragEvent.dataTransfer.getData('text/uri-list');
    if (uriList) {
      uris = uriList.split('\r?\n');
    } else {
      // see if the ui context has some dropped uris
      uris = ui.context.droppedUris();
    }

    // exit if there are no uris
    if (!uris) {
      return false;
    }

    // filter out images
    const imageUris = uris.filter(uri => {
      // get extension and check it it's an image
      // https://developer.mozilla.org/en-US/docs/Web/Media/Formats/Image_types#Common_image_file_types
      const kImageExtensions = [
        'apng',
        'bmp',
        'gif',
        'ico',
        'cur',
        'jpg',
        'jpeg',
        'jfif',
        'pjpeg',
        'pjp',
        'png',
        'svg',
        'tif',
        'tiff',
        'webp',
      ];
      const extension = uri
        .split(/\./)
        .pop()!
        .toLowerCase();
      return kImageExtensions.includes(extension);
    });

    // exit if we have no image uris
    if (imageUris.length === 0) {
      return false;
    }

    // resolve image uris then insert them. note that this is done
    // async so we return true indicating we've handled the drop and
    // then we actually do the insertion once it returns
    ui.context.resolveImageUris(imageUris).then(images => {
      const tr = view.state.tr;
      images.forEach(image => {
        const node = view.state.schema.nodes.image.create({ src: image });
        tr.insert(coordinates.pos, node);
      });
      view.dispatch(tr);
    });

    // indicate that we will handle the event
    event.preventDefault();
    return true;
  };
}
