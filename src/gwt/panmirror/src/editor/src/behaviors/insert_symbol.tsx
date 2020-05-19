/*
 * insert_symbol.ts
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

import { EditorState, Transaction, Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import React from 'react';

import { Extension } from '../api/extension';
import { ProsemirrorCommand, EditorCommandId } from '../api/command';
import { canInsertNode } from '../api/node';
import { Popup } from '../api/widgets/popup';
import { reactRenderForEditorView, WidgetProps } from '../api/widgets/react';

import "./insert_symbol-styles.css"
import { LinkButton } from '../api/widgets/button';
import ReactDOM from 'react-dom';
import { EditorUI } from '../api/ui';
import { Schema } from 'prosemirror-model';
import { PandocExtensions } from '../api/pandoc';
import { PandocCapabilities } from '../api/pandoc_capabilities';
import { EditorFormat } from '../api/format';
import { EditorOptions } from '../api/options';
import { EditorEvents, EditorEvent } from '../api/events';

// TODO: Could directly import JSON starting with TypeScript 2.9, but requires adding 
// "resolveJsonModule": true, to tsconfig for compiler
// but resolveJsonModule isn't compatible with module=umd, only permitted for
// 'commonjs', 'amd', 'es2015' or 'esNext'
import characters from './insert_symbol-data'

const key = new PluginKey<boolean>("insert_symbol");

class InsertSymbolPlugin extends Plugin<boolean> {

  private popup : HTMLElement | null = null;
  private scrollUnsubscribe : VoidFunction;

  private ui: EditorUI;

  constructor(ui: EditorUI, events: EditorEvents) {
    super({
      key,
      view: () => ({
        update: (view: EditorView, prevState: EditorState) => {    
          this.closePopup();
        },
        destroy:  () => {
          this.closePopup();
          this.scrollUnsubscribe();
        }
      }),
      props: {
        handleDOMEvents: {
          blur: (view: EditorView, event: Event) => {
            
            if (this.popup && window.document.activeElement !== this.popup) {
              // TODO: When clicking a linkButton in the popup, the body becomes focused
              // and consequently the popup closes before its link handler and execute.
              // If you uncomment this, inserting text will no longer work properly.
              //this.closePopup();
            }
            return false;
          }
        }
      }
    });

    this.ui = ui;
    this.closePopup = this.closePopup.bind(this);
    this.scrollUnsubscribe = events.subscribe(EditorEvent.Scroll, this.closePopup);
  }

  public showPopup(view: EditorView) {
    if (!this.popup) {
  
      const selection = view.state.selection;
      const editorRect = view.dom.getBoundingClientRect();

      this.popup = window.document.createElement("div");  
      this.popup.style.position = 'absolute'; 

      // TODO: Confirm that this is correct way to deal with z-Order isses (separator bar appears to have higher z-order)
      this.popup.style.zIndex = '1000';

      // TODO: Confirm preferred way to size and include size in layout
      const height = 350;
      const width = 250;
      
      // TODO: The height and width don't account for padding in the popup. Consequently, subtracting height
      // from window.innerheight still positions the popup too low. How should I deal with padding?
      const kPopupChromeHeight = 25;

      // TODO: Confirm that we're ok not appending 1 -> really want to be sure this is 
      // the position at which a character will be inserted (for example, if choose to use a pointer UI element
      // it should point to the exact position a character will be inserted not +1 position)
      const selectionCoords = view.coordsAtPos(selection.from);     
      
      const maximumTopPosition = Math.min(selectionCoords.bottom, window.innerHeight - height - kPopupChromeHeight);
      const minimumTopPosition = editorRect.y;
      const popupTopPosition = Math.max(minimumTopPosition, maximumTopPosition);
      this.popup.style.top = popupTopPosition + 'px';

      const popupLeftPosition = selectionCoords.right;
      this.popup.style.left = popupLeftPosition + 'px';     
      
      ReactDOM.render(this.insertSymbolPopup(view, [height, width]), this.popup);
      window.document.body.appendChild(this.popup);
    }
  }

  private closePopup() {
    if (this.popup) {
      ReactDOM.unmountComponentAtNode(this.popup);
      this.popup.remove();
      this.popup = null;
    }
  }

  // TODO: Is tuple for size gross?
  private insertSymbolPopup(view: EditorView, size: [Number, Number]) {

    const insertText = (text: string) => {
      const tr = view.state.tr;
      tr.insertText(text);
      view.dispatch(tr);
      view.focus();
    };

    const closePopup = () => {
      this.closePopup();
      view.focus();
    };

    // TODO: What is preferred way to pass something to style like this (height/width)?
    return <InsertSymbolPopup 
              onClose={closePopup} 
              onInsertText={insertText}
              enabled={isEnabled(view.state)}
              style={{"height" : [size[0]] + "px", "width" : [size[1]] + "px"}}
           />;
  }
}

const extension = (  
  pandocExtensions: PandocExtensions,
  _pandocCapabilities: PandocCapabilities,
  ui: EditorUI,
  _format: EditorFormat,
  _options: EditorOptions,
  events: EditorEvents,
) : Extension => {
  return {
    commands: () => {
      return [new ProsemirrorCommand(EditorCommandId.InsertSymbol, ['Ctrl-Shift-/'], insertSymbol)];
    },
    plugins: (_schema: Schema) => {
      return [new InsertSymbolPlugin(ui, events)];
    }
  };
};    

function isEnabled(state: EditorState) {
  return canInsertNode(state, state.schema.nodes.text);
}

export function insertSymbol(state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) {
  if (!isEnabled(state)) {
    return false;
  }

  if (dispatch && view) {
    const plugin = key.get(state) as InsertSymbolPlugin;
    plugin.showPopup(view);
  } 
  return true;
}

interface InsertSymbolPopupProps extends WidgetProps {
  enabled : boolean;
  onInsertText: (text: string) => void;
  onClose : VoidFunction;
}

const InsertSymbolPopup: React.FC<InsertSymbolPopupProps> = props => {

  const height: Number = 350;
  const width: Number = 250;
  

  const onInsertText = () => {
    props.onInsertText('Foo');
  };

  return(
    <Popup classes={['pm-popup-insert-symbol']} style={props.style}>
        <div>Hello world!</div>
        <LinkButton text={props.enabled ? "Close" : "Disabled"} onClick={props.onClose}/>
        <LinkButton text="Insert" onClick={onInsertText}/>
    </Popup>
  ); 
}

export default extension;