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
import { attachResizeUI, initResizeContainer } from './image-resize';

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
  private removeResizeUI?: () => void;

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
    this.imageAttributes = imageAttributes;
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
    if (this.imageAttributes) {
      this.removeResizeUI = attachResizeUI(this.dom, this.img!, this.view, () => ({
        pos: this.getPos(),
        node: this.node,
      }));
    }
  }

  public deselectNode() {
    // mirror default implementation
    this.dom.classList.remove('ProseMirror-selectednode');
    if (this.contentDOM || !this.node.type.spec.draggable) {
      this.dom.draggable = false;
    }

    // remove resize UI
    if (this.removeResizeUI) {
      this.removeResizeUI();
      this.removeResizeUI = undefined;
    }
  }

  // update image with latest node/attributes
  public update(node: ProsemirrorNode) {
    if (node.type !== this.node.type) {
      return false;
    }
    this.node = node;
    this.updateImg(node);
    return true;
  }

  // ignore mutations outside of the content time so sizing actions don't cause PM re-render
  public ignoreMutation(mutation: MutationRecord | { type: 'selection'; target: Element }) {
    if (!this.contentDOM) {
      return true;
    }
    return !this.contentDOM.contains(mutation.target);
  }

  // map node to img tag
  private updateImg(node: ProsemirrorNode) {
    // map to path reachable within current editing frame
    this.img.src = this.mapResourcePath(node.attrs.src);

    // title/tooltip
    this.img.title = node.attrs.title;

    // ensure alt attribute so that we get default browser broken image treatment
    this.img.alt = node.textContent || node.attrs.src;

    // reset img style
    this.img.setAttribute('style', '');

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
      (node.attrs.keyvalue as Array<[string, string]>).forEach(attr => {
        // alias key and value
        const key = attr[0];
        let value = attr[1];

        // forward styles to image (but set align oriented styles on figure parent)
        if (key === 'style') {
          // pull out certain styles that shoudl really belong to the block container
          if (this.isFigure()) {
            const liftImageStyle = (style: string) => {
              // mutate the value to remove the lifted style
              value = value.replace(new RegExp('(' + style + ')\\:\\s*(\\w+)', 'g'), (_match, p1, p2) => {
                this.dom.style.setProperty(p1, p2);
                return '';
              });
            };
            liftImageStyle('float');
            liftImageStyle('vertical-align');
            liftImageStyle('margin(?:[\\w\\-])*');
          }

          // set image style (modulo the properties lifted above)
          this.img.setAttribute('style', value);
        } else if (key === 'width') {
          this.img.style.width = value + 'px';
        } else if (key === 'height') {
          this.img.style.height = value + 'px';

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
    }
  }

  private isFigure() {
    return this.type === ImageType.Figure;
  }
}
