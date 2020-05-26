import { WidgetProps } from '../../api/widgets/react';
import React, { ChangeEvent, ReactHTML } from 'react';
import SymbolDataManager, { CATEGORY_ALL, SymbolCharacter, SymbolGroup } from './insert_symbol-data';
import { Popup } from '../../api/widgets/popup';
import { TextInput } from '../../api/widgets/text';
import { SelectInput } from '../../api/widgets/select';
import SymbolCharacterGrid from './insert_symbol-grid';

import './insert_symbol-styles.css';

const symbolDataManager = new SymbolDataManager();

interface InsertSymbolPopupProps extends WidgetProps {
  enabled: boolean;
  onInsertText: (text: string) => void;
  onClose: VoidFunction;
  size: [number, number];
  searchImage?: string;
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

  const gridHeight = popupHeight - 45;
  const gridWidth = popupWidth;

  const [filterText, setFilterText] = React.useState<string>('');
  const [selectedSymbolGroup, setSelectedSymbolGroup] = React.useState(CATEGORY_ALL);
  const [symbols, setSymbols] = React.useState<Array<SymbolCharacter>>([]);
  const [filteredSymbols, setFilteredSymbols] = React.useState<Array<SymbolCharacter>>(symbols);

  React.useEffect(() => {
    const symbols: Array<SymbolCharacter> = symbolDataManager.getSymbols(selectedSymbolGroup);
    setSymbols(symbols);
  }, [selectedSymbolGroup]);

  React.useEffect(() => {
    setFilteredSymbols(symbolDataManager.filterSymbols(filterText, symbols));
  }, [filterText, symbols]);

  const textRef = React.useRef<HTMLInputElement>(null);
  const selectRef = React.useRef<HTMLInputElement>(null);
  const gridRef = React.useRef<HTMLDivElement>(null);

  // Focus the first text box
  React.useEffect(() => {
    focusElement(textRef);
  }, []);

  function focusElement(element: React.RefObject<HTMLInputElement>) {
    element!.current!.focus();
  }

  const options = symbolDataManager.getSymbolGroups().map(category => (
    <option key={category.alias} value={category.alias}>
      {category.alias}
    </option>
  ));

  function handleSelectChanged(event: ChangeEvent<Element>) {
    const value: string = (event.target as HTMLSelectElement).selectedOptions[0].value;
    const selectedGroup: SymbolGroup | undefined = symbolDataManager.getSymbolGroups().find(group => group.alias === value);
    if (selectedGroup) {
      setSelectedSymbolGroup(selectedGroup);
    }
  }

  function handleKeyboardEvent(event: React.KeyboardEvent<HTMLElement>) {
    if (event.keyCode == 27) {
      // Esc key - close popup
      props.onClose();
      event.preventDefault();
    } else if (event.keyCode === 13) {
      // Enter key - insert symbol
      if (filteredSymbols.length === 1) {
        props.onInsertText(filteredSymbols[0].value); //
        event.preventDefault();
      }
    } else if (event.keyCode == 9) {
      // Tab key, handle tab out of panel
      if (event.shiftKey && window.document.activeElement === textRef.current) {
        // Shift tab should loop if we're on the first element of the panel
        selectRef.current?.focus();
        event.preventDefault();
      }
      // TODO: Tab
    } 
    // TODO arrows keys, page up / down for grid.
  }

  return (
    <Popup classes={['pm-popup-insert-symbol']} style={style}>
      <div onKeyDown={event => handleKeyboardEvent(event)}>
        <div className="pm-popup-insert-symbol-search-container" style={{ width: gridWidth }}>
          <TextInput
            widthChars={20}
            iconAdornment={props.searchImage}
            tabIndex={0}
            className="pm-popup-insert-symbol-search-textbox"
            placeholder="keyword or codepoint"
            onChange={(event: React.ChangeEvent<HTMLInputElement>) => setFilterText(event.target.value)}
            onKeyDown={(event: React.KeyboardEvent<HTMLInputElement>) => handleKeyboardEvent}
            ref={textRef}
          />
          <SelectInput
            tabIndex={0}
            ref={selectRef}
            className="pm-popup-insert-symbol-select-category"
            onChange={event => handleSelectChanged}
          >
            {options}
          </SelectInput>
        </div>

        <hr className="pm-popup-insert-symbol-separator pm-border-background-color" />
        <div className="pm-popup-insert-symbol-grid-container">
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
