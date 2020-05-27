/*
 * insert_symbol-grid-cell.tsx
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

import { Symbol } from "./insert_symbol-data";

import React from 'react';

export interface CharacterGridCellItemData {
  symbolCharacters: Array<Symbol>;
  numberOfColumns: number;
  selectedIndex: number;
  onSelectionChanged : (selectedIndex: number) => void;
  onSelectionCommitted: VoidFunction;
}

export const SymbolCharacterCell = ({ columnIndex, rowIndex, style, data }: any) => {
  const characterGridCellItemData = data as CharacterGridCellItemData;
  const symbolCharacters = characterGridCellItemData.symbolCharacters;
  const itemIndex = rowIndex * characterGridCellItemData.numberOfColumns + columnIndex;

  if (itemIndex < symbolCharacters.length) {
      const character = symbolCharacters[itemIndex];
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
    
  } else {
    return null;
  }
  

};
