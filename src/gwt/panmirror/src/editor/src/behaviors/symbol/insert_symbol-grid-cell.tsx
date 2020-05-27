import { SymbolCharacter } from "./insert_symbol-data";

import React from 'react';

export interface CharacterGridCellItemData {
  symbolCharacters: Array<SymbolCharacter>;
  numberOfColumns: number;
  selectedIndex: number;
  onSelectionChanged : (selectedIndex: number) => void;
  onSelectionCommitted: VoidFunction;
}

export const SymbolCharacterCell = ({ columnIndex, rowIndex, style, data }: any) => {
  const characterGridCellItemData = data as CharacterGridCellItemData;
  const symbolCharacters = characterGridCellItemData.symbolCharacters;
  const itemIndex = rowIndex * characterGridCellItemData.numberOfColumns + columnIndex;

  var character: SymbolCharacter | undefined = undefined;
  if (itemIndex < symbolCharacters.length) {
    character = symbolCharacters[itemIndex];
  }

  return (
    <div
      tabIndex={-1}
      style={style}
      title={`U+${character?.codepoint.toString(16)} - ${character?.name.toLowerCase()}`}
      className="pm-symbol-grid-container"
      onClick={event => {
        event.preventDefault();
        event.stopPropagation();
        characterGridCellItemData.onSelectionCommitted();
      }}
      onMouseDown={event => {
        event.preventDefault();
        event.stopPropagation();
      }}
      onMouseOver={event => {
        characterGridCellItemData.onSelectionChanged(itemIndex);
      }}
    >
      <div className={`pm-symbol-grid-cell pm-grid-item ${characterGridCellItemData.selectedIndex == itemIndex ? 'pm-grid-item-selected' : ''}`}>
      {character === undefined ? '' : character.value} 
      </div>
    </div>
  );
};
