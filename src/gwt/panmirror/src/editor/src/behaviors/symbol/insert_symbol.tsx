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

import { Extension } from '../../api/extension';
import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { canInsertNode } from '../../api/node';

import ReactDOM from 'react-dom';
import { EditorUI } from '../../api/ui';
import { Schema } from 'prosemirror-model';
import { PandocExtensions } from '../../api/pandoc';
import { PandocCapabilities } from '../../api/pandoc_capabilities';
import { EditorFormat } from '../../api/format';
import { EditorOptions } from '../../api/options';
import { EditorEvents, EditorEvent } from '../../api/events';

import { InsertSymbolPopup } from './insert_symbol-popup';

const key = new PluginKey<boolean>('insert_symbol');

class InsertSymbolPlugin extends Plugin<boolean> {
  private popup: HTMLElement | null = null;
  private scrollUnsubscribe: VoidFunction;

  private ui: EditorUI;

  constructor(ui: EditorUI, events: EditorEvents) {
    super({
      key,
      view: () => ({
        update: () => {
          this.closePopup();
        },
        destroy: () => {
          this.closePopup();
          this.scrollUnsubscribe();
          window.document.removeEventListener('focusin', this.focusChanged);
        },
      }),
    });

    this.ui = ui;
    this.closePopup = this.closePopup.bind(this);
    this.scrollUnsubscribe = events.subscribe(EditorEvent.Scroll, this.closePopup);

    this.focusChanged = this.focusChanged.bind(this);
    window.document.addEventListener('focusin', this.focusChanged);
  }

  public showPopup(view: EditorView) {
    if (!this.popup) {
      const height = 300;
      const width = 370;

      const selection = view.state.selection;
      const editorRect = view.dom.getBoundingClientRect();

      this.popup = window.document.createElement('div');
      this.popup.tabIndex = 0;
      this.popup.style.position = 'absolute';
      this.popup.style.zIndex = '1000';
      const selectionCoords = view.coordsAtPos(selection.from);

      const maximumTopPosition = Math.min(selectionCoords.bottom, window.innerHeight - height);
      const minimumTopPosition = editorRect.y;
      const popupTopPosition = Math.max(minimumTopPosition, maximumTopPosition);
      this.popup.style.top = popupTopPosition + 'px';

      const popupLeftPosition = selectionCoords.right;
      this.popup.style.left = popupLeftPosition + 'px';

      ReactDOM.render(this.insertSymbolPopup(view, [height, width]), this.popup);
      window.document.body.appendChild(this.popup);
    }
  }

  private focusChanged() {
      if (window.document.activeElement !== this.popup && !this.popup?.contains(window.document.activeElement)) {
        this.closePopup();
    }
  }

  private closePopup() {
    if (this.popup) {
      ReactDOM.unmountComponentAtNode(this.popup);
      this.popup.remove();
      this.popup = null;
    }
  }

  private insertSymbolPopup(view: EditorView, size: [number, number]) {
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

    return (
      <InsertSymbolPopup
        onClose={closePopup}
        onInsertText={insertText}
        enabled={isEnabled(view.state)}
        size={size}
        searchImage={this.ui.images.search}
      />
    );
  }
}

const extension = (
  pandocExtensions: PandocExtensions,
  _pandocCapabilities: PandocCapabilities,
  ui: EditorUI,
  _format: EditorFormat,
  _options: EditorOptions,
  events: EditorEvents,
): Extension => {
  return {
    commands: () => {
      return [new ProsemirrorCommand(EditorCommandId.InsertSymbol, ['Ctrl-Shift-/'], insertSymbol)];
    },
    plugins: (_schema: Schema) => {
      return [new InsertSymbolPlugin(ui, events)];
    },
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

export default extension;
