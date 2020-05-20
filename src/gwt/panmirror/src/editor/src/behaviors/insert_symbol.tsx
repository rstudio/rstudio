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
import { WidgetProps } from '../api/widgets/react';

import './insert_symbol-styles.css';
import ReactDOM from 'react-dom';
import { EditorUI } from '../api/ui';
import { Schema } from 'prosemirror-model';
import { PandocExtensions } from '../api/pandoc';
import { PandocCapabilities } from '../api/pandoc_capabilities';
import { EditorFormat } from '../api/format';
import { EditorOptions } from '../api/options';
import { EditorEvents, EditorEvent } from '../api/events';

import CharacterGrid from './insert_symbol-grid';
import SymbolDataManager, { SymbolCategory, SymbolCharacter } from './insert_symbol-data';

const key = new PluginKey<boolean>('insert_symbol');
const symbolDataManager = new SymbolDataManager();

class InsertSymbolPlugin extends Plugin<boolean> {
  private popup: HTMLElement | null = null;
  private scrollUnsubscribe: VoidFunction;

  private ui: EditorUI;

  constructor(ui: EditorUI, events: EditorEvents) {
    super({
      key,
      view: () => ({
        update: (view: EditorView, prevState: EditorState) => {
          this.closePopup();
        },
        destroy: () => {
          this.closePopup();
          this.scrollUnsubscribe();
        },
      }),
      props: {
        handleDOMEvents: {
          // TODO: am i going to receive blur event when items in popup are focused?
          // TODO: does this need to be converted to dealing with popup blurring not editorview blurring?
          blur: (view: EditorView, event: Event) => {
            if (this.popup && window.document.activeElement !== this.popup) {
              //this.closePopup();
            }
            return false;
          },
        },
      },
    });

    this.ui = ui;
    this.closePopup = this.closePopup.bind(this);
    this.scrollUnsubscribe = events.subscribe(EditorEvent.Scroll, this.closePopup);
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

      // TODO: Should we actually show a pointer that denotes where the character will be inserted?
      // ^ Question for overall editor design- not sure there are other cases like this that aren't
      // dialogs
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
      <InsertSymbolPopup onClose={closePopup} onInsertText={insertText} enabled={isEnabled(view.state)} size={size} />
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

interface InsertSymbolPopupProps extends WidgetProps {
  enabled: boolean;
  onInsertText: (text: string) => void;
  onClose: VoidFunction;
  size: [number, number];
}

const InsertSymbolPopup: React.FC<InsertSymbolPopupProps> = props => {
  const kPopupChromeHeight = 25;
  const popupHeight = props.size[0] - kPopupChromeHeight;
  const popupWidth = props.size[1];
  const style: React.CSSProperties = {
    ...props.style,
    height: popupHeight + 'px',
    width: popupWidth + 'px',
  };

  const gridHeight = popupHeight - 20;
  const gridWidth = popupWidth - 10;
  const numberOfColumns = 12;
  const columnWidth = gridWidth / numberOfColumns;
  const rowHeight = columnWidth;

  const [filterText, setFilterText] = React.useState<string>('');
  const [selectedCategory, setSelectedCategory] = React.useState(SymbolCategory.All);
  const [symbols, setSymbols] = React.useState<Array<SymbolCharacter>>([]);
  const [filteredSymbols, setFilteredSymbols] = React.useState<Array<SymbolCharacter>>(symbols);

  React.useEffect(() => {
    const symbols: Array<SymbolCharacter> = symbolDataManager.getSymbols(selectedCategory);
    setSymbols(symbols);
  }, [selectedCategory]);

  React.useEffect(() => {
    setFilteredSymbols(symbols.filter(symbol => symbol.name.includes(filterText)));
  }, [filterText, symbols]);

  let options = symbolDataManager.getCategories().map(category => (
    <option key={category} value={category}>
      {category}
    </option>
  ));

  return (
    <Popup classes={['pm-popup-insert-symbol']} style={style}>
      <input
        type="text"
        onChange={event => {
          setFilterText(event.target.value);
        }}
      />
      <select
        name="select-category"
        onChange={event => {
          const value: string = (event.target as HTMLSelectElement).selectedOptions[0].value;
          const selectedCategory: SymbolCategory | undefined = Object.values(SymbolCategory).find(x => x === value);
          if (selectedCategory) {
            setSelectedCategory(selectedCategory);
          }
        }}
      >
        {options}
      </select>

      <CharacterGrid
        symbolCharacters={filteredSymbols}
        onCharacterSelected={(character: string) => {
          props.onInsertText(character);
        }}
        height={gridHeight}
        width={gridWidth}
        rowHeight={rowHeight}
        columnWidth={columnWidth}
      />
    </Popup>
  );
};

export default extension;
