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

import { WidgetProps } from '../../api/widgets/react';
import React, { ChangeEvent } from 'react';
import SymbolDataManager, { kCategoryAll, SymbolCharacter, SymbolCharacterGroup } from './insert_symbol-data';
import { isElementFocused, focusElement } from '../../api/focus';
import { Popup } from '../../api/widgets/popup';
import { TextInput } from '../../api/widgets/text';
import { SelectInput } from '../../api/widgets/select';
import SymbolCharacterGrid, { newIndexForKeyboardEvent } from './insert_symbol-grid';

import './insert_symbol-styles.css';

const symbolDataManager = new SymbolDataManager();

interface InsertSymbolPopupProps extends WidgetProps {
  enabled: boolean;
  size: [number, number];
  searchImage?: string;
  onInsertText: (text: string) => void;
  onClose: VoidFunction;
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

  const gridHeight = popupHeight - 48;
  const gridWidth = popupWidth;
  const kNumberOfcolumns = 12;

  const [filterText, setFilterText] = React.useState<string>('');
  const [selectedSymbolGroup, setSelectedSymbolGroup] = React.useState<string>(kCategoryAll);
  const [selectedSymbolIndex, setSelectedSymbolIndex] = React.useState<number>(0);
  const [symbols, setSymbols] = React.useState<SymbolCharacter[]>([]);
  const [filteredSymbols, setFilteredSymbols] = React.useState<SymbolCharacter[]>(symbols);

  React.useEffect(() => {
    const symbolsForGroup: SymbolCharacter[] = symbolDataManager.getSymbols(selectedSymbolGroup);
    setSymbols(symbolsForGroup);
  }, [selectedSymbolGroup]);

  React.useEffect(() => {
    setFilteredSymbols(symbolDataManager.filterSymbols(filterText, symbols));
  }, [filterText, symbols]);

  const textRef = React.useRef<HTMLInputElement>(null);
  const selectRef = React.useRef<HTMLInputElement>(null);
  const gridRef = React.useRef<HTMLDivElement>(null);

  // Focus the first text box
  React.useEffect(() => {
    focusElement(textRef.current);
  }, []);

  const options = symbolDataManager.symbolGroupNames().map(name => (
    <option key={name} value={name}>
      {name}
    </option>
  ));

  const handleSelectChanged = (event: ChangeEvent<Element>) => {
    const value: string = (event.target as HTMLSelectElement).selectedOptions[0].value;
    const selectedGroupName: string | undefined = symbolDataManager
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

    // Process grid keyboard event if the textbox is focused and has no value
    if (isElementFocused(textRef.current) && textRef.current?.value.length === 0) {
      const newIndex = newIndexForKeyboardEvent(event, selectedSymbolIndex, kNumberOfcolumns, symbols.length);
      if (newIndex) {
        setSelectedSymbolIndex(newIndex);
        event.preventDefault();
      }
    }
  };

  const handleSelectedSymbolChanged = (symbolIndex: number) => {
    setSelectedSymbolIndex(symbolIndex);
  };

  const handleSelectedSymbolCommitted = () => {
    if (filteredSymbols.length > selectedSymbolIndex) {
      props.onInsertText(filteredSymbols[selectedSymbolIndex].value);
    }
  };

  const handleTextChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setFilterText(event?.target.value);
  };

  return (
    <Popup classes={['pm-popup-insert-symbol']} style={style}>
      <div onKeyDown={handleKeyboardEvent}>
        <div className="pm-popup-insert-symbol-search-container" style={{ width: gridWidth }}>
          <TextInput
            widthChars={20}
            iconAdornment={props.searchImage}
            tabIndex={0}
            className="pm-popup-insert-symbol-search-textbox"
            placeholder="keyword or codepoint"
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
          />
          <div
            className="pm-popup-insert-symbol-no-matching pm-light-text-color"
            style={{ display: filteredSymbols.length > 0 ? 'none' : 'block' }}
          >
            No matching symbols
          </div>
        </div>
      </div>
    </Popup>
  );
};
