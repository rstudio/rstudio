/*
 * widgets.ts
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

import tlite from 'tlite';

import './widgets.css';
import { editingRootNodeClosestToPos } from './node';

export function createHorizontalPanel() {
  const div = window.document.createElement('div');
  div.classList.add('pm-horizontal-panel');
  return div;
}

export function addHorizontalPanelCell(panel: HTMLDivElement, el: HTMLElement) {
  el.classList.add('pm-horizontal-panel-cell');
  panel.append(el);
}

export function createPopup(
  view: EditorView,
  classes: string[],
  onDestroyed?: () => void,
  style?: { [key: string]: string },
) {
  // create popup
  const popup = window.document.createElement('span');
  popup.classList.add('pm-popup', 'pm-pane-border-color', 'pm-background-color', 'pm-text-color', ...classes);
  popup.style.position = 'absolute';
  popup.style.zIndex = '10';
  applyStyle(popup, style);

  // create mutation observer that watches for destruction
  if (onDestroyed) {
    const observer = new MutationObserver(mutationsList => {
      mutationsList.forEach(mutation => {
        mutation.removedNodes.forEach(node => {
          if (node === popup) {
            observer.disconnect();
            onDestroyed();
          }
        });
      });
    });
    observer.observe(view.dom, { attributes: false, childList: true, subtree: true });
  }

  return popup;
}


// popup that appears immediately below a range of text (e.g. actions popup below links)
export function createTextRangePopup(
  view: EditorView, 
  range: { from: number, to: number },  
  classes: string[],
  maxWidth: number,
  onDestroyed?: () => void
) {
  // get the (window) DOM coordinates for the start of the range. we use range.from + 1 so
  // that ranges that are at the beginning of a line don't have their position set
  // to the previous line
  const linkCoords = view.coordsAtPos(range.from + 1);

  // get the (window) DOM coordinates for the current editing root node (body or notes)
  const rangePos = view.state.doc.resolve(range.from);
  const editingNode = editingRootNodeClosestToPos(rangePos);
  const editingEl = view.domAtPos(editingNode!.pos + 1).node as HTMLElement;
  const editingBox = editingEl.getBoundingClientRect();

  // we are going to stick the decoration at the beginning of the containing
  // top level body block, then position it by calculating the relative location of 
  // the range within text block. we do this so that the decoration isn't located
  // *within* the range (which confounds double-click selection and spell checking)
  const containingBlockPos = rangePos.start(2);
  const containingBlockEl = view.domAtPos(containingBlockPos).node as HTMLElement;
  const containingBlockStyle = window.getComputedStyle(containingBlockEl);
  const containingBlockBox = containingBlockEl.getBoundingClientRect();

  // base popup style
  const popupStyle = {
    'margin-top': (linkCoords.bottom - containingBlockBox.top + 3) + "px"
  };

  // we need to compute whether the popup will be visible (horizontally), do
  // this by testing whether we have room for the max link width + controls/padding
  const positionRight = linkCoords.left + maxWidth > editingBox.right;
  let popup: HTMLElement;
  if (positionRight) {
    const rightCoords = view.coordsAtPos(range.to);
    const rightPos = editingBox.right - rightCoords.right;
    popup = createPopup(view, classes, onDestroyed, { 
      ...popupStyle,
      right: rightPos + 'px' 
    });
  } else {
    const marginLeft = "calc(" + 
      (linkCoords.left - containingBlockBox.left) + "px " + 
      " - " + containingBlockStyle.borderLeftWidth + 
      " - " + containingBlockStyle.paddingLeft + 
      " - " + containingBlockStyle.marginLeft +
      " - 1ch" + 
    ")";
    popup = createPopup(view, classes, onDestroyed, {
      ...popupStyle,
      'margin-left': marginLeft
    });
  }

  return {
    pos: rangePos.start(2),
    popup
  };
}


export function createLinkButton(text: string, title?: string, maxWidth?: number, style?: { [key: string]: string }) {
  const link = window.document.createElement('a');
  link.classList.add('pm-link', 'pm-link-text-color');
  link.href = '#';
  link.innerText = text;
  link.title = title || text;
  if (maxWidth) {
    link.style.maxWidth = maxWidth + 'px';
  }
  applyStyle(link, style);
  return link;
}

export function createImageButton(classes: string[], title: string, style?: { [key: string]: string }) {
  const button = window.document.createElement('button');
  button.classList.add('pm-image-button', ...classes);
  button.title = title;
  applyStyle(button, style);
  return button;
}

export function showTooltip(
  el: Element,
  text: string,
  grav: 's' | 'n' | 'e' | 'w' | 'sw' | 'se' | 'nw' | 'ne' = 'n',
  timeout = 2000,
) {
  el.setAttribute('title', '');
  el.setAttribute('data-tlite', text);
  tlite.show(el, { grav });
  setTimeout(() => tlite.hide(el), timeout);
}

function applyStyle(el: HTMLElement, style?: { [key: string]: string }) {
  if (style) {
    Object.keys(style).forEach(name => {
      el.style.setProperty(name, style[name]);
    });
  }
}
