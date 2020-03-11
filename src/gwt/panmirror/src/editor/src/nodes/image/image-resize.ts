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

 // Positioning of shelf (right side overflow, position at right edge, etc.)
 // Units!

import { EditorView } from 'prosemirror-view';
import { NodeWithPos } from 'prosemirror-utils';
import { NodeSelection } from 'prosemirror-state';

import { createPopup, createHorizontalPanel, addHorizontalPanelCell, createInputLabel, createNumericInput, createImageButton, createSelectInput, createCheckboxInput } from '../../api/widgets';
import { EditorUI } from '../../api/ui';
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

export interface ResizeUI {
  update: () => void;
  detach: () => void;
}

export function attachResizeUI(
  imageNode: () => NodeWithPos,
  container: HTMLElement,
  img: HTMLImageElement,
  view: EditorView,
  ui: EditorUI 
) : ResizeUI {

  // indicate that resize ui is active
  container.classList.add('pm-image-resize-active');

   // handle dims change
   const onDimsChanged = (width: number, height: number) => {
    updateImageSize(view, imageNode(), width.toString(), height.toString());
  };


  const onUnitsChanged = () => {
    // console.log('units changed');
  };

  // handle editImage request from shelf
  const onEditImage = () => {
    const nodeWithPos = imageNode();
    imageDialog(nodeWithPos.node, nodeWithPos.node.type, view.state, view.dispatch, view, ui, true);
  };


  // create resize shelf
  const shelf = resizeShelf(
    view, 
    onDimsChanged, 
    onUnitsChanged, 
    onEditImage, 
    ui.context.translateText
  );
  container.append(shelf.el);

  // initialize props
  shelf.setDims(img.offsetWidth, img.offsetHeight);
  
  // create resize handle and add it to the container
  const handle = resizeHandle(
    img, 
    shelf.props.lockRatio,
    shelf.setDims,
    onDimsChanged
  );

  // add the handle
  container.append(handle);
  
  // return functions that can be used to update and detach the ui
  return {
    update: () => {
      shelf.setDims(img.offsetWidth, img.offsetHeight);
    },
    detach: () => {
      container.classList.remove('pm-image-resize-active');
      handle.remove();
      shelf.el.remove();
    }
  };
}

function resizeShelf(
  view: EditorView, 
  onDimsChanged: (width: number, height: number) => void, 
  onUnitsChanged: () => void,
  onEditImage: () => void, 
  translateText: (text: string) => string
)  {

  // create resize shelf
  const shelf = createPopup(view, ['pm-text-color']);
 
  // TODO: min width that inspects image width (i.e might have to set right to a negative value)
  shelf.style.left = '0';
  // shelf.style.right = '0'; 
  shelf.style.bottom = "-48px";


  const panel = createHorizontalPanel();
  shelf.append(panel);
  const addToPanel = (widget: HTMLElement, paddingRight: number) => {
    addHorizontalPanelCell(panel, widget);
    const paddingSpan = window.document.createElement('span');
    paddingSpan.style.width = paddingRight + 'px';
    addHorizontalPanelCell(panel, paddingSpan);
  };
  
  const inputClasses = ['pm-text-color', 'pm-background-color'];


  const wLabel = createInputLabel('w:');
  addToPanel(wLabel, 4);

  const wInput = createNumericInput(4, 1, 10000, inputClasses);
  addToPanel(wInput, 8);

  const hLabel = createInputLabel('h:');
  addToPanel(hLabel, 4);

  const hInput = createNumericInput(4, 1, 10000, inputClasses);
  addToPanel(hInput, 10);

  const unitsSelect = createSelectInput(["px", "in", "%"], inputClasses);
  unitsSelect.onchange = onUnitsChanged;
  addToPanel(unitsSelect, 12);

  wInput.onchange = hInput.onchange = () => {
    const width = getDim(wInput);
    const height = getDim(hInput);
    if (width && height) {
      onDimsChanged(width, height);
    }
  };


  const checkboxWrapper = window.document.createElement('div');
  const lockCheckbox = createCheckboxInput();
  lockCheckbox.checked = true;
  
  checkboxWrapper.append(lockCheckbox);
  addToPanel(checkboxWrapper, 4);
  const lockLabel = createInputLabel(translateText('Lock ratio'));
  
  addToPanel(lockLabel, 20);

  const editImage = createImageButton(
    ['pm-image-button-edit-properties'], 
    translateText('Edit Image')
  );
  editImage.onclick = onEditImage;
  addHorizontalPanelCell(panel, editImage);

  const getDim = (input: HTMLInputElement) => {
    const value = input.valueAsNumber;
    if (isNaN(value)) {
      return null;
    }
    if (value > 0) {
      return value;
    } else {
      return null;
    }
  };

  return {
    el: shelf,

    setDims: (width: number, height: number) => {
      wInput.value = width.toString();
      hInput.value = height.toString();
    },

    props: {
      width: () => getDim(wInput),
      height: () => getDim(hInput),
      units: () => unitsSelect.value,
      lockRatio: () => lockCheckbox.checked
    }
  };
}


function resizeHandle(
  img: HTMLImageElement, 
  lockRatio: () => boolean,
  onSizing: (width: number, height: number) => void,
  onSizingComplete: (width: number, height: number) => void
) {

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

      let width;
      let height;
      if (lockRatio()) {
        if (movedX >= movedY) {
          width = startWidth + movedX; 
          height = startHeight + (movedX * (startHeight/startWidth));
        } else {
          height = startHeight + movedY;
          width = startWidth + (movedY * (startWidth/startHeight));
        }
      } else {
        width = startWidth + movedX;
        height = startHeight + movedY;
      }    
      width = Math.round(width);
      height = Math.round(height);
      img.style.width = width + "px";
      img.style.height = height + "px";  
      onSizing(width, height);
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
      
      // update image size
      onSizingComplete(img.offsetWidth, img.offsetHeight);
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

function updateImageSize(view: EditorView, image: NodeWithPos, width: string, height: string) {

  // edit width & height in keyvalue
  let keyvalue = image.node.attrs.keyvalue as Array<[string, string]>;
  keyvalue = keyvalue.filter(value => !['width', 'height'].includes(value[0]));
  keyvalue.push(['width', width]);
  keyvalue.push(['height', height]);

  // create transaction
  const tr = view.state.tr;

  // set new attributes
  tr.setNodeMarkup(image.pos, image.node.type, { ...image.node.attrs, keyvalue });

  // restore node selection if our tr.setNodeMarkup blew away the selection
  const prevState = view.state;
  if (prevState.selection instanceof NodeSelection && prevState.selection.from === image.pos) {
    tr.setSelection(NodeSelection.create(tr.doc, image.pos));
  }

  // dispatch transaction
  view.dispatch(tr);
}
