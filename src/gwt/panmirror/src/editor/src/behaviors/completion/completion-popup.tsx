/*
 * completion-popup.tsx
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

import React from 'react';
import ReactDOM from 'react-dom';

import { FixedSizeList, ListChildComponentProps } from "react-window";

import { WidgetProps } from "../../api/widgets/react";
import { Popup } from '../../api/widgets/popup';

import { CompletionHandler, CompletionResult } from '../../api/completion';
import { applyStyles } from '../../api/css';

export function createCompletionPopup() : HTMLElement {
  const popup = window.document.createElement('div');
  popup.tabIndex = 0;
  popup.style.position = 'absolute';
  popup.style.zIndex = '1000';
  return popup;
}

export function renderCompletionPopup(
  view: EditorView, 
  handler: CompletionHandler, 
  result: CompletionResult,
  popup: HTMLElement
) : Promise<void> {

  // helper function to show the popup at the specified position
  const showPopup = (completions: any[]) => {
    
    const positionStyles = panelPositionStylesForPosition(view, result.pos, 200, 200);
    applyStyles(popup, [], positionStyles);
    
    ReactDOM.render(
      <CompletionPopup 
      completions={completions} 
      completionView={handler.completionView} />,
      popup,
    );
  };
  
  // show completions (resolve promise if necessary)
  if (result.items instanceof Promise) {
    return result.items.then(completions => {
      showPopup(completions);
    });
  } else {
    showPopup(result.items);
    return Promise.resolve();
  }
}

export function destroyCompletionPopup(popup: HTMLElement) {
  ReactDOM.unmountComponentAtNode(popup);
  popup.remove();
}

interface CompletionPopupProps extends WidgetProps {
  completions: any[];
  completionView: React.FC | React.ComponentClass;
  rowHeight?: number;
  height?: number;
}

const kCompletionPopupWidth = 200;

const CompletionPopup: React.FC<CompletionPopupProps> = props => {
  
  const { rowHeight = 25, height = 200 } = props;

  return (
    <Popup 
      style={props.style}
      classes={props.classes}
    >
      <FixedSizeList
        itemCount={props.completions.length}
        itemSize={rowHeight}
        height={height}
        width={kCompletionPopupWidth}
        itemData={props.completions}
      >
        {listChildComponent(props.completionView)}
      </FixedSizeList>   
    </Popup>
  );
};

const listChildComponent = (completionView: React.FC | React.ComponentClass) => {
  return (props: ListChildComponentProps) => {
    const item = React.createElement(completionView, props.data[props.index]);
    return (
      <div>
        {item}
      </div>
    );
  };
};

const kMinimumPanelPaddingToEdgeOfView = 5;
function panelPositionStylesForPosition(view: EditorView, pos: number, height: number, width: number) {
  const editorRect = view.dom.getBoundingClientRect();

  const selectionCoords = view.coordsAtPos(pos);

  const maximumTopPosition = Math.min(
    selectionCoords.bottom,
    window.innerHeight - height - kMinimumPanelPaddingToEdgeOfView,
  );
  const minimumTopPosition = editorRect.y;
  const popupTopPosition = Math.max(minimumTopPosition, maximumTopPosition);

  const maximumLeftPosition = Math.min(
    selectionCoords.right,
    window.innerWidth - width - kMinimumPanelPaddingToEdgeOfView,
  );
  const minimumLeftPosition = editorRect.x;
  const popupLeftPosition = Math.max(minimumLeftPosition, maximumLeftPosition);

  // styles we'll return
  const styles = {
    top: popupTopPosition + 'px',
    left: popupLeftPosition + 'px',
  };

  return styles;
}
