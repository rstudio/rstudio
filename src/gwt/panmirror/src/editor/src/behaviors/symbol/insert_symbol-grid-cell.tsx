import { SymbolCharacter } from "./insert_symbol-data";
import { findPreviousInputElement, findNextInputElement } from "../../api/html";

import React from 'react';

export interface CharacterGridCellItemData {
  symbolCharacters: Array<SymbolCharacter>;
  onKeyDown: (event: React.KeyboardEvent) => boolean;
  onSymbolCharacterSelected: (char: string) => void;
  numberOfColumns: number;
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
      tabIndex={0}
      style={style}
      title={`+U+${character?.codepoint.toString(16)} - ${character?.name.toLowerCase()}`}
      className="pm-symbol-grid-container"
      onClick={event => {
        event.preventDefault();
        event.stopPropagation();
        characterGridCellItemData.onSymbolCharacterSelected(character?.value as string);
      }}
      onMouseDown={event => {
        event.preventDefault();
        event.stopPropagation();
      }}
      onKeyDown={event => {
        if (event.keyCode == 13) {
          characterGridCellItemData.onSymbolCharacterSelected(character?.value as string);
          event.preventDefault();
          event.stopPropagation();            
        }

        if (characterGridCellItemData.onKeyDown(event)) {
          event.preventDefault();
          event.stopPropagation();  
        };
      }}
    >
      <div className="pm-symbol-grid-cell pm-grid-item">
      {character === undefined ? '' : character.value}
      </div>
    </div>
  );
};
