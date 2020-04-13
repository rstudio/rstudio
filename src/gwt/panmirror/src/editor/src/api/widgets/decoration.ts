/*
 * decoration.ts
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


import { ResolvedPos, Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorView } from 'prosemirror-view';

import { ContentNodeWithPos } from 'prosemirror-utils';

import React from 'react';

import { editingRootNodeClosestToPos } from '../node';

import { kPixelUnit } from '../css';

export interface DecorationPosition {
  pos: number; 
  style: React.CSSProperties;
}

export function nodeDecorationPosition(
  doc: ProsemirrorNode,
  view: EditorView,
  nodeWithPos: ContentNodeWithPos,
  offsets?: { top: number, right: number }
) : DecorationPosition | null {

  // default offsets
  offsets = offsets || { top: 0, right: 0 };

  // get the dom node
  const nodeDOM = view.nodeDOM(nodeWithPos.pos);
  if (!nodeDOM) {
    return null;
  }

  // get the editing container element
  const editingNode = editingRootNodeClosestToPos(doc.resolve(nodeWithPos.pos));
  if (!editingNode) {
    return null;
  }
  const editingDOM = view.nodeDOM(editingNode.pos);
  if (!editingDOM) {
    return null;
  }

  // cast to HTML element
  const nodeEl = nodeDOM as HTMLElement;
  const editingEl = editingDOM as HTMLElement;

  // find the top offset by looping up until we get to the editingEl
  let topOffset = offsets.top;
  let offsetTarget: Element | null = nodeEl;
  while (offsetTarget && (offsetTarget !== editingEl)) {
    topOffset += (offsetTarget as HTMLElement).offsetTop;
    offsetTarget = (offsetTarget as HTMLElement).offsetParent;
  }

  // get the rectangle of each
  const nodeRect = nodeEl.getBoundingClientRect();
  const editingRect = editingEl.getBoundingClientRect();
  
  return {
    pos: editingNode.pos + editingNode.node.nodeSize - 1,
    style: {
      position: 'absolute',
      top: topOffset + 'px',
      right: (editingRect.right - nodeRect.right) + offsets.right + 'px'
    }
  };

}


export function textRangePopupDecorationPosition(
  view: EditorView,
  range: { from: number; to: number },
  maxWidth: number,
): DecorationPosition {
  // get the (window) DOM coordinates for the start of the range. we use range.from + 1 so
  // that ranges that are at the beginning of a line don't have their position set
  // to the previous line
  const linkCoords = view.coordsAtPos(range.from + 1);

  // get the (window) DOM coordinates for the current editing root node (body or notes)
  const rangePos = view.state.doc.resolve(range.from);
  const editingBox = getEditingBox(view, rangePos);
 
  // we are going to stick the decoration at the beginning of the containing
  // top level body block, then position it by calculating the relative location of
  // the range within text block. we do this so that the decoration isn't located
  // *within* the range (which confounds double-click selection and spell checking)
  const containingBlockPos = rangePos.start(2);
  const containingBlockEl = view.domAtPos(containingBlockPos).node as HTMLElement;
  const containingBlockStyle = window.getComputedStyle(containingBlockEl);
  const containingBlockBox = containingBlockEl.getBoundingClientRect();

  // base popup style
  const topPadding = parseInt(containingBlockStyle.paddingTop!, 10) || 0;
  const popupStyle = {
    marginTop: linkCoords.bottom - containingBlockBox.top - topPadding + 3 + kPixelUnit,
  };

  // we need to compute whether the popup will be visible (horizontally), do
  // this by testing whether we have room for the max link width + controls/padding
  let style: React.CSSProperties;
  const positionRight = linkCoords.left + maxWidth > editingBox.right;
  if (positionRight) {
    const rightCoords = view.coordsAtPos(range.to);
    const rightPos = editingBox.right - rightCoords.right;
    style = {
      ...popupStyle,
      right: rightPos + kPixelUnit,
    };
  } else {
    const marginLeft =
      'calc(' +
      (linkCoords.left - containingBlockBox.left) +
      'px ' +
      ' - ' +
      containingBlockStyle.borderLeftWidth +
      ' - ' +
      containingBlockStyle.paddingLeft +
      ' - ' +
      containingBlockStyle.marginLeft +
      ' - 1ch' +
      ')';
    style = {
      ...popupStyle,
      marginLeft,
    };
  }

  return {
    pos: containingBlockPos,
    style,
  };
}

function getEditingBox(view: EditorView, pos$: ResolvedPos) {
  const editingNode = editingRootNodeClosestToPos(pos$);
  const editingEl = view.domAtPos(editingNode!.pos + 1).node as HTMLElement;
  return editingEl.getBoundingClientRect();
}

