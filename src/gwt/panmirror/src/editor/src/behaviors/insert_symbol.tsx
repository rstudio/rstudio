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

const key = new PluginKey<boolean>("insert_symbol");

class InsertSymbolPlugin extends Plugin<boolean> {

  private popup : HTMLElement | null = null;

  private ui: EditorUI;

  constructor(ui: EditorUI) {
    super({
      key,
      view: () => ({
        update: (view: EditorView, prevState: EditorState) => {
          if (this.popup) {
            ReactDOM.render(this.insertSymbolPopup(view), this.popup);
          }
        },
      }),
    });

    this.ui = ui;
    this.closePopup = this.closePopup.bind(this);
  }

  public showPopup(view: EditorView) {
    if (!this.popup) {
      this.popup = window.document.createElement("div");             
      reactRenderForEditorView(this.insertSymbolPopup(view), this.popup, view);
      view.dom.parentNode?.appendChild(this.popup);
    }
  }

  private closePopup() {
    this.popup?.remove();
    this.popup = null;
  }

  private insertSymbolPopup(view: EditorView) {

    const insertText = (text: string) => {
      const tr = view.state.tr;
      tr.insertText(text);
      view.dispatch(tr);
    };

    return <InsertSymbolPopup 
              onClose={this.closePopup} 
              onInsertText={insertText}
              enabled={isEnabled(view.state)}
           />;
  }
}

const extension: Extension = {
  commands: () => {
    return [new ProsemirrorCommand(EditorCommandId.InsertSymbol, ['Ctrl-Shift-/'], insertSymbol)];
  },
  plugins: (_schema: Schema, ui: EditorUI) => {
    return [new InsertSymbolPlugin(ui)];
  }
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