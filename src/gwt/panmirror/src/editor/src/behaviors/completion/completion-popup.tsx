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

import { applyStyles } from '../../api/css';
import { CompletionHandler, CompletionResult } from '../../api/completion';
import { WidgetProps } from "../../api/widgets/react";
import { Popup } from '../../api/widgets/popup';

import './completion-popup.css';

export function createCompletionPopup() : HTMLElement {
  const popup = window.document.createElement('div');
  popup.style.position = 'absolute';
  popup.style.zIndex = '1000';
  return popup;
}

export function renderCompletionPopup(
  view: EditorView, 
  handler: CompletionHandler, 
  result: CompletionResult,
  popup: HTMLElement
) : Promise<boolean> {

  // helper function to show the popup at the specified position
  const renderPopup = (completions: any[]) => {
    // create props
    const props = { handler, completions, selectedIndex: 0 };

    // position popup
    const size = completionPopupSize(props);
    const positionStyles = completionPopupPositionStyles(view, result.pos, size.width, size.height);
    applyStyles(popup, [], positionStyles);
    
    // render popup
    ReactDOM.render(<CompletionPopup {...props} />, popup);
  };
  
  // show completions (resolve promise if necessary)
  if (result.completions instanceof Promise) {
    return result.completions.then(completions => {
      renderPopup(completions);
      return completions.length > 0;
    });
  } else {
    renderPopup(result.completions);
    return Promise.resolve(result.completions.length > 0);
  }
}

export function destroyCompletionPopup(popup: HTMLElement) {
  ReactDOM.unmountComponentAtNode(popup);
  popup.remove();
}

interface CompletionWidgetProps extends WidgetProps {
  handler: CompletionHandler;
  completions: any[];
  selectedIndex: number;
}

const CompletionPopup: React.FC<CompletionWidgetProps> = props => {
  return (
    <Popup 
      style={props.style}
      classes={['pm-completion-popup'].concat(props.classes || [])}
    >
      <CompletionList {...props}/> 
    </Popup>
  );
};

const kDefaultItemHeight = 22;
const kDefaultMaxVisible = 10;
const kDefaultWidth = 180;

const CompletionList: React.FC<CompletionWidgetProps> = props => {

  const { component, itemHeight = kDefaultItemHeight } = props.handler.view;

  const size = completionPopupSize(props);

  return (
    <div className={'pm-completion-list'} style={{ width: size.width + 'px', height: size.height + 'px'}}>
      <table>
      <tbody>
        {props.completions.map((completion, index) => {
          // need to provide key for both wrapper and item
          // https://stackoverflow.com/questions/28329382/understanding-unique-keys-for-array-children-in-react-js#answer-28329550
          const key = props.handler.view.key(completion);
          const item = React.createElement(component, { ...completion, key });
          const className = 'pm-completion-item' + (index === props.selectedIndex ? ' pm-selected-list-item' : '');
          return (
            <tr key={key} style={ {lineHeight: itemHeight + 'px' }} >
              <td 
                className={className} 
                key={key}
              >
                {item}
              </td>
            </tr>
          );
        })}
      </tbody>
      </table>
    </div>
  );
};

// some extra padding to tweak whitespace around popup & list
const kCompletionsVerticalPadding = 8;

function completionPopupSize(props: CompletionWidgetProps) {

  // get view props (apply defaults)
  let { itemHeight = kDefaultItemHeight } = props.handler.view;
  const { maxVisible = kDefaultMaxVisible, width = kDefaultWidth } = props.handler.view;

  // add 2px for the border to item heights
  itemHeight += 2;

  return {
    width,
    height: (itemHeight * Math.min(maxVisible, props.completions.length)) +
            kCompletionsVerticalPadding
  };
}

function completionPopupPositionStyles(view: EditorView, pos: number, width: number, height: number) {

  // some constraints
  const kMinimumPaddingToEdge = 5;

  // default position
  const selectionCoords = view.coordsAtPos(pos);
 
  let top = selectionCoords.bottom + kCompletionsVerticalPadding;
  let left = selectionCoords.left;

  // see if we need to be above
  if ((top + height + kMinimumPaddingToEdge) >= window.innerHeight) {
    top = selectionCoords.top - height - kCompletionsVerticalPadding;
  }

  // see if we need to be to the left (use cursor as pos in this case)
  if ((left + width + kMinimumPaddingToEdge) >= window.innerWidth) {
    const cursorCoords = view.coordsAtPos(view.state.selection.head);
    left = cursorCoords.right - width;
  }

  return {
    left: left + 'px',
    top: top + 'px',
  };
}
