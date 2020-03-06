/*
 * image-view.ts
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

import { Node as ProsemirrorNode } from 'prosemirror-model';
import { NodeView, EditorView } from 'prosemirror-view';
import { NodeSelection } from 'prosemirror-state';

import { EditorUI, ImageType } from '../../api/ui';

import { imageDialog } from './image-dialog';

export class ImageNodeView implements NodeView {
  private readonly type: ImageType;

  public readonly dom: HTMLElement;
  private readonly img: HTMLImageElement;

  private readonly getPos: () => number;

  public readonly contentDOM: HTMLElement | null;
  private readonly figcaption: HTMLElement | null;

  private node: ProsemirrorNode;
  private readonly view: EditorView;

  private readonly mapResourcePath: (path: string) => string;

  constructor(
    node: ProsemirrorNode,
    view: EditorView,
    getPos: () => number,
    editorUI: EditorUI,
    imageAttributes: boolean,
  ) {
    // determine type
    const schema = node.type.schema;
    this.type = node.type === schema.nodes.image ? ImageType.Image : ImageType.Figure;

    this.node = node;
    this.view = view;
    this.getPos = getPos;
    this.mapResourcePath = editorUI.context.mapResourcePath;

    const selectOnClick = () => {
      const tr = view.state.tr;
      tr.setSelection(NodeSelection.create(view.state.doc, getPos()));
      view.dispatch(tr);
    };

    const editOnDblClick = () => {
      selectOnClick();
      imageDialog(this.node, this.node.type, this.view.state, this.view.dispatch, this.view, editorUI, imageAttributes);
    };

    const noPropagateClick = (ev: MouseEvent) => {
      ev.stopPropagation();
    };

    // create the image (used by both image and figure node types)
    this.img = document.createElement('img');
    this.img.onclick = selectOnClick;
    this.img.ondblclick = editOnDblClick;

    // wrap in figure if appropriate
    if (this.type === ImageType.Figure) {
      
      // create figure wrapper
      this.dom = document.createElement('figure');

      // create container and add resize handles to it
      const container = document.createElement('div');
      this.addResizeHandles(container);
      
      // initialize the image, add it to the container, then add 
      // the container to the DOM
      this.updateImg(node);
      container.append(this.img);
      container.contentEditable = 'false';
      this.dom.append(container);

      // create the caption and make it our contentDOM
      this.figcaption = document.createElement('figcaption');
      this.figcaption.classList.add('pm-figcaption');
      this.figcaption.classList.add('pm-node-caption');
      this.figcaption.onclick = noPropagateClick;
      this.figcaption.ondblclick = noPropagateClick;
      this.contentDOM = this.figcaption;
      this.dom.append(this.figcaption);
    } else {
      this.dom = document.createElement('span');
      this.dom.append(this.img);
      this.addResizeHandles(this.dom);
      this.contentDOM = null;
      this.figcaption = null;
      this.updateImg(node);
    }
  }

  public update(node: ProsemirrorNode) {

    if (node.type !== this.node.type) {
      return false;
    }

    this.node = node;
    this.updateImg(node);

    return true;
  }

  // ignore resizing mutations
  public ignoreMutation(mutation: MutationRecord | { type: "selection"; target: Element; }) {
    if (!this.contentDOM) {
      return true;
    }
    return !this.contentDOM.contains(mutation.target);
  }

  private updateImg(node: ProsemirrorNode) {

    // update img attributes (ensure we display an alt attribute so that we get
    // default browser broken image treatment)
    this.img.alt = node.textContent || node.attrs.src;
    this.img.src = this.mapResourcePath(node.attrs.src);
    this.img.title = node.attrs.title;

    // update figure container with width/alignment related keyvalue props
    // if (this.type === ImageType.Figure) {
      this.updateFigure(node);
    // }
  }

  private updateFigure(node: ProsemirrorNode) {
    // clear existing styles
    // TODO: how should this clearing work since we need the resize properties on there
    // this.dom.setAttribute('style', '');
    
    this.img.setAttribute('style', '');

    // TODO: float properties need to go onto the figure

    // reset
    if (node.attrs.keyvalue) {
      (node.attrs.keyvalue as Array<[string, string]>).forEach(attr => {
        const [key, value] = attr;
        if (key === 'style') {
          const style = this.dom.getAttribute('style');
          const baseStyle = style ? ';' + style : '';
          this.img.setAttribute('style', value + baseStyle);
        } else if (key === 'width') {
          this.img.style.width = value + "px";
        } else if (key === 'height') {
          this.img.style.height = value + "px";
        } else if (key === 'align') {
          switch (value) {
            case 'left':
            case 'right':
              this.dom.style.cssFloat = value;
              break;
            case 'top':
            case 'bottom':
            case 'middle':
              this.dom.style.verticalAlign = value;
              break;
          }
        }
      });
    }
  }

  private addResizeHandles(container: HTMLElement) {


    // so that we are the offsetParent for the handles
    container.style.position = "relative";

    // so that the container matches the size of the contained image
    container.style.display = "inline-block";

    // so that the handles can be visible outside the boundaries of the image
    container.style.overflow = "visible";

    // create bottom right handle
    const handle = document.createElement('span');
    handle.style.position = "absolute";
    handle.style.border = "3px solid black";
    handle.style.borderTop = "none";
    handle.style.borderLeft = "none";
    handle.style.bottom = "-5px";
    handle.style.right = "-8px";
    handle.style.width = "10px";
    handle.style.height = "10px";
    handle.style.cursor = "nwse-resize";

    handle.onmousedown = (ev: MouseEvent) => {

      ev.preventDefault();

      const startWidth = this.img!.offsetWidth;

      const startX = ev.pageX;
      const startY = ev.pageY;
      

      const onMouseMove = (e: MouseEvent) => {
        const currentX = e.pageX;
        const currentY = e.pageY;
        
        const diffInPx = currentX - startX;
                
        this.img!.style.width = (startWidth + diffInPx) + "px";
      };
      
      const onMouseUp = (e: MouseEvent) => {    

        e.preventDefault();
        
        document.removeEventListener("mousemove", onMouseMove);
        document.removeEventListener("mouseup", onMouseUp);

        
        const newAttrs = {
          ...this.node.attrs,
          keyvalue: [["width", this.img.width.toString()]]
        };
        
        // create transaction
        const tr = this.view.state.tr;

        // set new attributes
        tr.setNodeMarkup(this.getPos(), this.node.type, newAttrs);

        // restore node selection if our tr.setNodeMarkup blew away the selection
        const prevState = this.view.state;
        if (prevState.selection instanceof NodeSelection && prevState.selection.from === this.getPos() ) {
          tr.setSelection(NodeSelection.create(tr.doc, this.getPos()));
        }

        // dispatch transaction
        this.view.dispatch(tr);
      };
      
      
      document.addEventListener("mousemove", onMouseMove);
      document.addEventListener("mouseup", onMouseUp);

    };


    container.append(handle);

  }
}
