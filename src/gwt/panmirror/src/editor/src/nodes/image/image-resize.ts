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

import { createPopup, createHorizontalPanel, addHorizontalPanelCell, createInputLabel, createNumericInput, createImageButton, createSelectInput, createCheckboxInput } from '../../api/widgets';
import { EditorDisplay, EditorUI } from '../../api/ui';
import { imageDialog } from './image-dialog';

export function initResizeContainer(container: HTMLElement) {

  // add standard parent class
  container.classList.add('pm-image-resize-container', 'pm-selected-node-outline-color');

  // so that we are the offsetParent for the resize handles and shelf
  container.style.position = 'relative';

  // so that the container matches the size of the contained image
  container.style.display = 'inline-block';

  // so that the handles and shelf can be visible outside the boundaries of the image
  container.style.overflow = 'visible';

  // return for convenience
  return container;
}

export function attachResizeUI(
  nodeWithPos: NodeWithPos,
  container: HTMLElement,
  img: HTMLImageElement,
  view: EditorView,
  ui: EditorUI 
) {

  // indicate that resize ui is active
  container.classList.add('pm-image-resize-active');

  // create resize shelf
  const shelf = resizeShelf(nodeWithPos, img, view, ui);
  container.append(shelf);
  
  // create resize handle and add it to the container
  const handle = resizeHandle(nodeWithPos, img, view);
  container.append(handle);
  
  // return a function that can be used to destroy the resize UI
  return () => {
    container.classList.remove('pm-image-resize-active');
    handle.remove();
    shelf.remove();
  };
}


function resizeShelf(
  nodeWithPos: NodeWithPos, 
  img: HTMLImageElement, 
  view: EditorView, 
  ui: EditorUI
) {

  // create resize shelf
  const shelf = createPopup(view, ['pm-light-text-color']);
 
  // TODO: min width that inspects image width (i.e might have to set right to a negative value)
  shelf.style.left = '0';
  // shelf.style.right = '0';


  const panel = createHorizontalPanel();
  shelf.append(panel);
  const addToPanel = (widget: HTMLElement, paddingRight: number) => {
    addHorizontalPanelCell(panel, widget);
    const paddingSpan = window.document.createElement('span');
    paddingSpan.style.width = paddingRight + 'px';
    addHorizontalPanelCell(panel, paddingSpan);
  };
  
  const inputClasses = ['pm-light-text-color', 'pm-background-color'];

  const wInput = createNumericInput(4, 1, 10000, inputClasses);
  addToPanel(wInput, 4);

  const xLabel = createInputLabel('x');
  addToPanel(xLabel, 4);

  const hInput = createNumericInput(4, 1, 10000, inputClasses);
  addToPanel(hInput, 10);

  const unitsSelect = createSelectInput(["px", "pct"], inputClasses);
  addToPanel(unitsSelect, 12);


  const checkboxWrapper = window.document.createElement('div');
  const lockCheckbox = createCheckboxInput();
  lockCheckbox.checked = true;
  checkboxWrapper.append(lockCheckbox);
  addToPanel(checkboxWrapper, 4);
  const lockLabel = createInputLabel(ui.context.translateText('Lock ratio'));
  addToPanel(lockLabel, 20);

  const editImage = createImageButton(
    ['pm-image-button-edit-properties'], 
    ui.context.translateText('Edit Image')
  );
  editImage.onclick = () => {
    imageDialog(nodeWithPos.node, nodeWithPos.node.type, view.state, view.dispatch, view, ui, true);
  };
  addHorizontalPanelCell(panel, editImage);



  shelf.style.bottom = "-45px";


  return shelf;
}

function resizeHandle(nodeWithPos: NodeWithPos, img: HTMLImageElement, view: EditorView) {

  const handle = document.createElement('span');
  handle.classList.add(
    'pm-image-resize-handle', 
    'pm-background-color', 
    'pm-selected-node-border-color'
  );
  handle.style.position = 'absolute';
  handle.style.bottom = '-6px';
  handle.style.right = '-6px';
  handle.style.cursor = 'nwse-resize';

  const havePointerEvents = !!document.body.setPointerCapture;  

  const onPointerDown = (ev: MouseEvent) => {
    ev.preventDefault();

    const startWidth = img.offsetWidth;
    const startHeight = img.offsetHeight;

    const startX = ev.pageX;
    const startY = ev.pageY;

    const onPointerMove = (e: MouseEvent) => {

      // detect pointer movement
      const movedX = e.pageX - startX;
      const movedY = e.pageY - startY;

      const lockRatio = true;
      if (lockRatio) {
        if (movedX >= movedY) {
          img.style.width = startWidth + movedX + 'px'; 
          img.style.height = startHeight + (movedX * (startHeight/startWidth)) + 'px';
        } else {
          img.style.height = startHeight + movedY + 'px';
          img.style.width = startWidth + (movedY * (startWidth/startHeight)) + 'px';
        }
      } else {
        img.style.width = startWidth + movedX + 'px';
        img.style.height = startHeight + movedY + 'px';
      }      
    };

    const onPointerUp = (e: MouseEvent) => {
      e.preventDefault();

      // stop listening to events
      if (havePointerEvents) {
        handle.releasePointerCapture((e as PointerEvent).pointerId);
        handle.removeEventListener('pointermove', onPointerMove);
        handle.removeEventListener('pointerup', onPointerUp);
      } else {
        document.removeEventListener('mousemove', onPointerMove);
        document.removeEventListener('mouseup', onPointerUp);
      }
      
      // get node and position
      const { pos, node } = nodeWithPos;

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

    if (havePointerEvents) {
      handle.setPointerCapture((ev as PointerEvent).pointerId);
      handle.addEventListener('pointermove', onPointerMove);
      handle.addEventListener('pointerup', onPointerUp);
    } else {
      document.addEventListener('mousemove', onPointerMove);
      document.addEventListener('mouseup', onPointerUp);
    }
    
  };

  if (havePointerEvents) {
    handle.addEventListener('pointerdown', onPointerDown);
  } else {
    handle.addEventListener('mousedown', onPointerDown);
  }

  return handle;

}
