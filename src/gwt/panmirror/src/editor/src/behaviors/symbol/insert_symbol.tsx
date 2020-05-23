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

import React, { ChangeEvent, ReactHTML } from 'react';

import { Extension } from '../../api/extension';
import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { canInsertNode } from '../../api/node';
import { Popup } from '../../api/widgets/popup';
import { WidgetProps } from '../../api/widgets/react';

import './insert_symbol-styles.css';
import ReactDOM from 'react-dom';
import { EditorUI } from '../../api/ui';
import { Schema } from 'prosemirror-model';
import { PandocExtensions } from '../../api/pandoc';
import { PandocCapabilities } from '../../api/pandoc_capabilities';
import { EditorFormat } from '../../api/format';
import { EditorOptions } from '../../api/options';
import { EditorEvents, EditorEvent } from '../../api/events';
import * as unicode from '../../api/unicode';

import SymbolCharacterGrid from './insert_symbol-grid';
import SymbolDataManager, { SymbolCategory, SymbolCharacter, CATEGORY_ALL } from './insert_symbol-data';
import { TextInput } from '../../api/widgets/textInput';
import { SelectInput } from '../../api/widgets/selectInput';

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

interface InsertSymbolPopupProps extends WidgetProps {
  enabled: boolean;
  onInsertText: (text: string) => void;
  onClose: VoidFunction;
  size: [number, number];
  searchImage?: string;
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

  const gridHeight = popupHeight - 45;
  const gridWidth = popupWidth;

  const [filterText, setFilterText] = React.useState<string>('');
  const [selectedCategory, setSelectedCategory] = React.useState(CATEGORY_ALL);
  const [symbols, setSymbols] = React.useState<Array<SymbolCharacter>>([]);
  const [filteredSymbols, setFilteredSymbols] = React.useState<Array<SymbolCharacter>>(symbols);

  React.useEffect(() => {
    const symbols: Array<SymbolCharacter> = symbolDataManager.getSymbols(selectedCategory);
    setSymbols(symbols);
  }, [selectedCategory]);

  React.useEffect(() => {
    const codepoint = unicode.parseCodepoint(filterText);
    var filteredSymbols = symbols.filter(symbol => {
      // Search by name
      if (symbol.name.includes(filterText.toUpperCase())) {
        return true;
      }

      // Search by codepoint
      if (codepoint && symbol.codepoint === codepoint) {
        return true;
      }

      return false;
    });

    if (filteredSymbols.length === 0 && codepoint) {
      // If the filter doesn't match and this could be a code point, just
      // emit a matching character
      setFilteredSymbols([
        {
          name: codepoint.toString(16),
          value: String.fromCodePoint(codepoint),
          codepoint: codepoint,
        },
      ]);
    } else {
      // Show filtered results
      setFilteredSymbols(filteredSymbols);
    }
  }, [filterText, symbols]);

  const textRef = React.useRef(null);
  const selectRef = React.useRef(null);
  const gridRef = React.useRef(null);

  // Focus the first text box
  React.useEffect(() => {
    focusElement(textRef);
  }, []);

  function focusElement(element: React.RefObject<HTMLInputElement>) {
    element!.current!.focus();
  }

  let options = symbolDataManager.getCategories().map(category => (
    <option key={category.name} value={category.name}>
      {category.name}
    </option>
  ));

  return (
    <Popup classes={['pm-popup-insert-symbol']} style={style}>
      <div className="pm-popup-insert-symbol-search-container" style={{width: gridWidth}}>
        <TextInput
          widthChars={20}
          iconAdornment={props.searchImage}
          tabIndex={0}
          className='pm-popup-insert-symbol-search-textbox'
          placeholder="keyword or codepoint"
          onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
            setFilterText(event.currentTarget.value);
          }}
          onKeyDown={(event: React.KeyboardEvent<HTMLInputElement>) => {
            if (event.keyCode == 9 && event.shiftKey) {
              focusElement(gridRef);
            }
          }}
          onKeyUp={(event: React.KeyboardEvent<HTMLInputElement>) => {
            if (event.keyCode === 13) {
              if (filteredSymbols.length === 1) {
                props.onInsertText(filteredSymbols[0].value);
              }
            }
          }}
          ref={textRef}
        />
        <SelectInput
          tabIndex={0}
          ref={selectRef}
          className='pm-popup-insert-symbol-select-category'
          onChange={(event: React.ChangeEvent<HTMLSelectElement>) => {
            const value: string = (event.target as HTMLSelectElement).selectedOptions[0].value;
            const selectedCategory: SymbolCategory | undefined = symbolDataManager
              .getCategories()
              .find(category => category.name === value);
            if (selectedCategory) {
              setSelectedCategory(selectedCategory);
            }
          }}
        >
          {options}
        </SelectInput>
      </div>

      <hr className="pm-popup-insert-symbol-separator pm-border-background-color"/>
      <SymbolCharacterGrid
        symbolCharacters={filteredSymbols}
        onSymbolCharacterSelected={(character: string) => {
          props.onInsertText(character);
        }}
        onChangeFocus={(previous: boolean) => focusElement(previous ? selectRef : textRef)}
        height={gridHeight}
        width={gridWidth}
        numberOfColumns={12}
        ref={gridRef}
      />
    </Popup>
  );
};

export default extension;
