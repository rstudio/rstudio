/*
 * insert_symbol-popup.tsx
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

import React, { ChangeEvent } from 'react';

import { EditorUI } from '../../api/ui';
import { isElementFocused, focusElement } from '../../api/focus';
import { Popup } from '../../api/widgets/popup';
import { SelectInput } from '../../api/widgets/select';
import { TextInput } from '../../api/widgets/text';
import { WidgetProps } from '../../api/widgets/react';

import { SymbolDataProvider, SymbolCharacter } from './insert_symbol-dataprovider';
import SymbolCharacterGrid, { newIndexForKeyboardEvent } from './insert_symbol-grid';

import './insert_symbol-styles.css';
import { SymbolPreview } from './insert_symbol-grid-preview';

interface InsertSymbolPopupProps extends WidgetProps {
  symbolDataProvider: SymbolDataProvider;
  enabled: boolean;
  size: [number, number];
  onInsertSymbol: (symbolCharacter: SymbolCharacter, searchTerm: string) => void;
  onClose: VoidFunction;
  ui: EditorUI;
  searchImage?: string;
  searchPlaceholder? : string;
}

export const InsertSymbolPopup: React.FC<InsertSymbolPopupProps> = props => {
  const kPopupChromeHeight = 25;
  const popupHeight = props.size[0] - kPopupChromeHeight;
  const popupWidth = props.size[1];
  const style: React.CSSProperties = {
    ...props.style,
    height: popupHeight + 'px',
    width: popupWidth + 'px',
  };
  const classNames = ['pm-popup-insert-symbol'].concat(props.classes || []);

  const gridHeight = popupHeight - 108;
  const gridWidth = popupWidth;
  const kNumberOfcolumns = 11;

  const [filterText, setFilterText] = React.useState<string>('');
  const [selectedSymbolGroup, setSelectedSymbolGroup] = React.useState<string>();
  const [selectedSymbolIndex, setSelectedSymbolIndex] = React.useState<number>(0);

  const symbols = React.useMemo(
    () => props.symbolDataProvider.getSymbols(selectedSymbolGroup),
    [selectedSymbolGroup]
  ); 
  const filteredSymbols = React.useMemo(
    () => props.symbolDataProvider.filterSymbols(filterText, symbols),
    [filterText, symbols]
  );

  // If the symbol list gets shorter than the selected index, move
  // selected cell into range
  React.useEffect(() => {
    if (selectedSymbolIndex > filteredSymbols.length) {
      setSelectedSymbolIndex(Math.max(filteredSymbols.length - 1, 0));
    }
  }, [filteredSymbols]);

  const textRef = React.useRef<HTMLInputElement>(null);
  const selectRef = React.useRef<HTMLSelectElement>(null);
  const gridRef = React.useRef<HTMLDivElement>(null);

  // Focus the first text box
  React.useEffect(() => {
    focusElement(textRef.current);
  }, []);

  const options = props.symbolDataProvider.symbolGroupNames().map(name => (
    <option key={name} value={name}>
      {props.ui.context.translateText(name)}
    </option>
  ));

  const handleSelectChanged = (event: ChangeEvent<Element>) => {
    const value: string = (event.target as HTMLSelectElement).selectedOptions[0].value;
    const selectedGroupName: string | undefined = props.symbolDataProvider
      .symbolGroupNames()
      .find(name => name === value);
    if (selectedGroupName) {
      setSelectedSymbolGroup(selectedGroupName);
    }
  };

  const handleKeyboardEvent = (event: React.KeyboardEvent<HTMLElement>) => {
    // Global Key Handling
    switch (event.key) {
      case 'Escape':
        // Esc key - close popup
        props.onClose();
        event.preventDefault();
        break;

      case 'Tab':
        if (event.shiftKey && isElementFocused(textRef.current)) {
          focusElement(gridRef.current);
          event.preventDefault();
        } else if (!event.shiftKey && isElementFocused(gridRef.current)) {
          focusElement(textRef.current);
          event.preventDefault();
        }
        break;

      case 'Enter':
        handleSelectedSymbolCommitted();
        event.preventDefault();
        break;

      default: 
        break;
    }

    if (textRef.current && isElementFocused(textRef.current)) {
      if (textRef.current?.value.length > 0) {

        // If the textbox is focused, figure out whether the grid can
        // still take arrow / page events. If there is no text or the cursor
        // is at the end of the input, we can handle arrows (see conditions).
        const numberOfCharacters = textRef.current?.value.length;
        const cursorPosition = textRef.current?.selectionStart;
        if (cursorPosition === 0 && event.key === 'ArrowLeft') { 
            processGridKeyboardEvent(event); // can handle left
        } else if (cursorPosition === numberOfCharacters && event.key === 'ArrowRight') {  
          processGridKeyboardEvent(event); // can handle right
        } else if (event.key === 'ArrowUp' || event.key === 'ArrowDown' || event.key === 'PageUp' || event.key === 'PageDown') {
          processGridKeyboardEvent(event);  // can always handle up / down, page up / down
        }       
      } else {
        processGridKeyboardEvent(event);
      }
    }
  };

  function processGridKeyboardEvent(event: React.KeyboardEvent) {
    const newIndex = newIndexForKeyboardEvent(event, selectedSymbolIndex, kNumberOfcolumns, filteredSymbols.length);
    if (newIndex !== undefined && newIndex >= 0) {
      setSelectedSymbolIndex(newIndex);
      event.preventDefault();
    }
  }

  const handleSelectedSymbolChanged = (symbolIndex: number) => {
    setSelectedSymbolIndex(symbolIndex);
  };

  const handleSelectedSymbolCommitted = () => {
    if (filteredSymbols.length > selectedSymbolIndex) {
      props.onInsertSymbol(filteredSymbols[selectedSymbolIndex], textRef.current?.value || '');
    }
  };

  const handleTextChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setFilterText(event?.target.value);
  };

  return (
    <Popup classes={classNames} style={style}>
      <div onKeyDown={handleKeyboardEvent}>
        <div className="pm-popup-insert-symbol-search-container" style={{ width: gridWidth }}>
          <TextInput
            widthChars={20}
            iconAdornment={props.searchImage}
            tabIndex={0}
            className="pm-popup-insert-symbol-search-textbox"
            placeholder={props.searchPlaceholder}
            onChange={handleTextChange}
            ref={textRef}
          />
          <SelectInput
            tabIndex={0}
            ref={selectRef}
            className="pm-popup-insert-symbol-select-category"
            onChange={handleSelectChanged}
          >
            {options}
          </SelectInput>
        </div>

        <hr className="pm-popup-insert-symbol-separator pm-border-background-color" />
        <div className="pm-popup-insert-symbol-grid-container">
          <SymbolCharacterGrid
            symbolCharacters={filteredSymbols}
            selectedIndex={selectedSymbolIndex}
            onSelectionChanged={handleSelectedSymbolChanged}
            onSelectionCommitted={handleSelectedSymbolCommitted}
            height={gridHeight}
            width={gridWidth}
            numberOfColumns={kNumberOfcolumns}
            ref={gridRef}
            ui={props.ui}
          />
          <div
            className="pm-popup-insert-symbol-no-matching pm-light-text-color"
            style={{ display: filteredSymbols.length > 0 ? 'none' : 'block' }}
          >
            No matching symbols
          </div>
        </div>
        <hr className="pm-popup-insert-symbol-separator pm-border-background-color" />
        {filteredSymbols[selectedSymbolIndex] &&
        <SymbolPreview
          symbolCharacter={filteredSymbols[selectedSymbolIndex]}
          symbolPreviewStyle={props.symbolDataProvider.symbolPreviewStyle}
          ui={props.ui}
          />
        }
      </div>
    </Popup>
  );
};
