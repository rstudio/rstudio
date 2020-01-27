/*
 * figure-view.ts
 *
 * Copyright (C) 2019-20 by RStudio, Inc.
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

import { ImageEditorFn } from '../../api/ui';

import { imageDialog } from './image-dialog';

export class FigureNodeView implements NodeView {
  public readonly dom: HTMLElement;
  private readonly img: HTMLImageElement;

  public readonly contentDOM: HTMLElement | null;
  private readonly figcaption: HTMLElement | null;

  private node: ProsemirrorNode;
  private readonly view: EditorView;
  private readonly getPos: () => number;

  constructor(
    node: ProsemirrorNode,
    view: EditorView,
    getPos: () => number,
    onEditImage: ImageEditorFn,
    imageAttributes: boolean,
  ) {
    this.node = node;
    this.view = view;
    this.getPos = getPos;

    const selectOnClick = () => {
      const tr = view.state.tr;
      tr.setSelection(NodeSelection.create(view.state.doc, getPos()));
      view.dispatch(tr);
    };

    const editOnDblClick = () => {
      selectOnClick();
      imageDialog(
        this.node,
        this.node.type,
        this.view.state,
        this.view.dispatch,
        this.view,
        onEditImage,
        imageAttributes,
      );
    };

    const noPropagateClick = (ev: MouseEvent) => {
      ev.stopPropagation();
    };

    this.dom = document.createElement('figure');
    this.dom.onclick = selectOnClick;
    this.dom.ondblclick = editOnDblClick;
    const container = document.createElement('div');
    this.img = document.createElement('img');
    this.img.onclick = selectOnClick;
    this.img.ondblclick = editOnDblClick;
    this.updateImg(node);
    container.append(this.img);
    container.contentEditable = 'false';
    this.dom.append(container);
    this.figcaption = document.createElement('figcaption');
    this.figcaption.classList.add('pm-figcaption');
    this.figcaption.classList.add('pm-node-caption');
    this.figcaption.onclick = noPropagateClick;
    this.figcaption.ondblclick = noPropagateClick;
    this.contentDOM = this.figcaption;
    this.dom.append(this.figcaption);
  }

  public update(node: ProsemirrorNode) {
    if (node.type !== this.node.type) {
      return false;
    }

    this.node = node;
    this.updateImg(node);

    return true;
  }

  private updateImg(node: ProsemirrorNode) {
    // update img attributes
    this.img.alt = node.textContent;
    this.img.src = node.attrs.src;
    this.img.title = node.attrs.title;

    // update figure container with width/alignment related keyvalue props
    this.updateFigure(node);
  }

  private updateFigure(node: ProsemirrorNode) {
    // clear existing styles
    this.dom.setAttribute('style', '');
    this.img.setAttribute('style', '');

    // reset
    if (node.attrs.keyvalue) {
      let hasWidth = false;
      (node.attrs.keyvalue as Array<[string, string]>).forEach(attr => {
        const [key, value] = attr;
        if (key === 'style') {
          const style = this.dom.getAttribute('style');
          const baseStyle = style ? ';' + style : '';
          this.dom.setAttribute('style', value + baseStyle);
          hasWidth = hasWidth || value.includes('width:');
        } else if (key === 'width') {
          this.dom.style.width = value;
          hasWidth = true;
        } else if (key === 'height') {
          this.dom.style.height = value;
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

      // if we have a width or height then set accordingly on image
      if (hasWidth) {
        this.img.style.width = '100%';
      }
    }
  }
}
