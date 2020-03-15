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

import { findParentNodeClosestToPos } from 'prosemirror-utils';

import { EditorUI, ImageType } from '../../api/ui';
import { removeStyleAttrib, extractSizeStyles } from '../../api/css';

import { imageDialog } from './image-dialog';
import { attachResizeUI, initResizeContainer, ResizeUI, isResizeUICompatible } from './image-resize';
import { sizePropWithUnit, isValidUnit, unitToPixels, hasPercentWidth } from './image-util';

import './image-styles.css';

export class ImageNodeView implements NodeView {
  private readonly type: ImageType;

  public readonly dom: HTMLElement;
  private readonly img: HTMLImageElement;

  private readonly getPos: () => number;

  public readonly contentDOM: HTMLElement | null;
  private readonly figcaption: HTMLElement | null;

  private node: ProsemirrorNode;
  private readonly view: EditorView;

  private readonly imageAttributes: boolean;
  private resizeUI: ResizeUI | null;

  private readonly editorUI: EditorUI;

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
    this.imageAttributes = imageAttributes;
    this.editorUI = editorUI;
    this.resizeUI = null;

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

      // standard inline image
    } else {
      this.dom = document.createElement('span');

      this.updateImg(node);
      this.dom.append(this.img);

      this.contentDOM = null;
      this.figcaption = null;
    }

    // prevent drag/drop if the event doesn't target the
    this.dom.ondragstart = (event: DragEvent) => {
      if (event.target !== this.img) {
        event.preventDefault();
        event.stopPropagation();
      }
    };

    // init resize if we support imageAttributes
    if (imageAttributes) {
      initResizeContainer(this.dom);
    }
  }

  public selectNode() {
    // mirror default implementation
    this.dom.classList.add('ProseMirror-selectednode');
    if (this.contentDOM || !this.node.type.spec.draggable) {
      this.dom.draggable = true;
    }

    // attach resize UI
    this.attachResizeUI();
  }

  public deselectNode() {
    // mirror default implementation
    this.dom.classList.remove('ProseMirror-selectednode');
    if (this.contentDOM || !this.node.type.spec.draggable) {
      this.dom.draggable = false;
    }

    // remove resize UI
    if (this.resizeUI) {
      this.resizeUI.detach();
      this.resizeUI = null;
    }
  }

  // update image with latest node/attributes
  public update(node: ProsemirrorNode) {
    if (node.type !== this.node.type) {
      return false;
    }
    this.node = node;
    this.updateImg(node);
    if (this.resizeUI) {
      if (isResizeUICompatible(this.img!)) {
        this.resizeUI.update();
      } else {
        this.resizeUI.detach();
        this.resizeUI = null;
      }
    } else if (this.isNodeSelected()) {
      this.attachResizeUI();
    }
    return true;
  }

  // ignore mutations outside of the content time so sizing actions don't cause PM re-render
  public ignoreMutation(mutation: MutationRecord | { type: 'selection'; target: Element }) {
    if (!this.contentDOM) {
      return true;
    }

    if (mutation.target === this.img) {
      return true;
    }

    return !this.contentDOM.contains(mutation.target);
  }

  // prevent bubbling of events into editor
  public stopEvent(event: Event) {
    // allow drag events if they target the image
    if (event instanceof DragEvent && event.target instanceof HTMLImageElement) {
      return false;
    }

    // filter other events that target our element or it's children
    if (event.target instanceof HTMLElement) {
      const stop = this.dom === event.target || this.dom.contains(event.target as HTMLElement);
      return stop;
    } else {
      return false;
    }
  }

  private isNodeSelected() {
    return this.dom.classList.contains('ProseMirror-selectednode');
  }

  // attach resize UI if appropriate
  private attachResizeUI() {
    if (this.imageAttributes && isResizeUICompatible(this.img!)) {
      const imageNode = () => ({ pos: this.getPos(), node: this.node });
      const imgContainerWidth = () => this.containerWidth();
      this.resizeUI = attachResizeUI(imageNode, this.dom, this.img!, imgContainerWidth, this.view, this.editorUI);
    }
  }

  // map node to img tag
  private updateImg(node: ProsemirrorNode) {
    // map to path reachable within current editing frame
    this.img.src = this.editorUI.context.mapResourcePath(node.attrs.src);

    // title/tooltip
    this.img.title = node.attrs.title;

    // ensure alt attribute so that we get default browser broken image treatment
    this.img.alt = node.textContent || node.attrs.src;

    // reset attributes
    this.img.removeAttribute('style');
    this.img.removeAttribute('data-width');
    this.img.removeAttribute('data-height');

    // reset figure styles (only reset styles that we explicitly set below, b/c some
    // styles may have been set by e.g. the attachResizeUI function)
    if (this.isFigure()) {
      this.dom.style.cssFloat = '';
      this.dom.style.verticalAlign = '';
      this.dom.style.margin = '';
      this.dom.style.marginTop = '';
      this.dom.style.marginBottom = '';
      this.dom.style.marginRight = '';
      this.dom.style.marginLeft = '';
    }

    // apply keyvalue attribute to image
    if (node.attrs.keyvalue) {
      // determine containerWidth
      const containerWidth = this.containerWidth();

      const keyvalue = extractSizeStyles(node.attrs.keyvalue);

      (keyvalue as Array<[string, string]>).forEach(attr => {
        // alias key and value
        const key = attr[0];
        let value = attr[1];

        // forward styles to image (but set align oriented styles on figure parent)
        if (key === 'style') {
          // pull out certain styles that shoudl really belong to the block container
          if (this.isFigure()) {
            const liftStyle = (attrib: string, val: string) => this.dom.style.setProperty(attrib, val);
            value = removeStyleAttrib(value, 'float', liftStyle);
            value = removeStyleAttrib(value, 'vertical-align', liftStyle);
            value = removeStyleAttrib(value, 'margin(?:[\\w\\-])*', liftStyle);
          }

          // we don't set the style, because that will override styles set in width/height.
          // if there are specific styles we want to reflect we should whitelist them in
          // via calls to removeStyleAttrib
        } else if (key === 'width') {
          // see if this is a unit we can edit
          const widthProp = sizePropWithUnit(value);
          if (widthProp) {
            widthProp.unit = widthProp.unit || 'px';
            if (isValidUnit(widthProp.unit)) {
              this.img.setAttribute('data-width', widthProp.size + widthProp.unit);
              this.img.style.width = unitToPixels(widthProp.size, widthProp.unit, containerWidth) + 'px';
            }
          }

          // if not, just pass it straight through (editing UI will be disabled)
          if (!this.img.hasAttribute('data-width')) {
            this.img.style.width = value;
          }
        } else if (key === 'height') {
          // see if this is a unit we can edit
          const heightProp = sizePropWithUnit(value);
          if (heightProp) {
            heightProp.unit = heightProp.unit || 'px';
            if (isValidUnit(heightProp.unit)) {
              this.img.setAttribute('data-height', heightProp.size + heightProp.unit);
              this.img.style.height = unitToPixels(heightProp.size, heightProp.unit, containerWidth) + 'px';
            }
          }

          // if not, just pass it straight through (editing UI will be disabled)
          if (!this.img.hasAttribute('data-height')) {
            this.img.style.height = value;
          }

          // use of legacy 'align' attribute is common for some pandoc users
          // so we convert it to the requisite CSS
        } else if (this.isFigure() && key === 'align') {
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

      // if width is a percentage, then displayed height needs to be 'auto'
      if (hasPercentWidth(this.img.getAttribute('data-width'))) {
        this.img.style.height = 'auto';
      }
    }
  }

  private isFigure() {
    return this.type === ImageType.Figure;
  }

  private containerWidth() {
    let containerWidth = (this.view.dom as HTMLElement).offsetWidth;
    if (containerWidth > 0) {
      const pos = this.getPos();
      if (pos) {
        const imagePos = this.view.state.doc.resolve(pos);
        const resizeContainer = findParentNodeClosestToPos(imagePos, nd => nd.isBlock);
        if (resizeContainer) {
          const resizeEl = this.view.domAtPos(resizeContainer.pos);
          containerWidth = (resizeEl.node as HTMLElement).offsetWidth;
        }
      }
    }
    return containerWidth;
  }
}
