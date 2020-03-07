/*
 * image-resize.ts
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

import { EditorView } from 'prosemirror-view';
import { NodeWithPos } from 'prosemirror-utils';
import { NodeSelection } from 'prosemirror-state';
import { createPopup, createHorizontalPanel, addHorizontalPanelCell } from '../../api/widgets';

export function attachResizeUI(
  container: HTMLElement,
  img: HTMLImageElement,
  view: EditorView,
  getNodeWithPos: () => NodeWithPos,
) {
  // so that we are the offsetParent for the handles
  container.style.position = 'relative';

  // so that the container matches the size of the contained image
  container.style.display = 'inline-block';

  // so that the handles can be visible outside the boundaries of the image
  container.style.overflow = 'visible';

  // don't show standard selected node outline (since we are showing the resizing handles)
  container.style.outline = 'none';

  // create resize shelf
  const popup = createPopup(view, []);
  popup.style.left = '0';
  popup.style.bottom = '-45px';
  popup.style.right = '0';
  container.append(popup);
  const panel = createHorizontalPanel();
  popup.append(panel);
  const label = document.createElement('span');
  label.innerText = 'This is the resizer UI';
  addHorizontalPanelCell(panel, label);

  // create bottom right handle
  const handle = document.createElement('span');
  handle.classList.add('pm-image-resize-handle');
  handle.style.position = 'absolute';
  handle.style.bottom = '-5px';
  handle.style.right = '-5px';
  handle.style.width = '10px';
  handle.style.height = '10px';
  handle.style.cursor = 'nwse-resize';

  handle.onmousedown = (ev: MouseEvent) => {
    ev.preventDefault();

    const startWidth = img.offsetWidth;
    const startHeight = img.offsetHeight;

    const startX = ev.pageX;
    const startY = ev.pageY;

    const onMouseMove = (e: MouseEvent) => {
      // width
      const movedX = e.pageX - startX;
      img.style.width = startWidth + movedX + 'px';

      // height
      const movedY = e.pageY - startY;
      img.style.height = startHeight + movedY + 'px';
    };

    const onMouseUp = (e: MouseEvent) => {
      e.preventDefault();

      // stop listening to events
      document.removeEventListener('mousemove', onMouseMove);
      document.removeEventListener('mouseup', onMouseUp);

      // get node and position
      const { pos, node } = getNodeWithPos();

      // edit width & height in keyvalue
      let keyvalue = node.attrs.keyvalue as Array<[string, string]>;
      keyvalue = keyvalue.filter(value => !['width', 'height'].includes(value[0]));
      keyvalue.push(['width', img.width.toString()]);
      keyvalue.push(['height', img.height.toString()]);

      // create transaction
      const tr = view.state.tr;

      // set new attributes
      tr.setNodeMarkup(pos, node.type, { ...node.attrs, keyvalue });

      // restore node selection if our tr.setNodeMarkup blew away the selection
      const prevState = view.state;
      if (prevState.selection instanceof NodeSelection && prevState.selection.from === pos) {
        tr.setSelection(NodeSelection.create(tr.doc, pos));
      }

      // dispatch transaction
      view.dispatch(tr);
    };

    document.addEventListener('mousemove', onMouseMove);
    document.addEventListener('mouseup', onMouseUp);
  };

  // append the handle
  container.append(handle);

  // return a function that can be used to destroy the resize UI
  return () => {
    handle.remove();
    popup.remove();
  };
}
