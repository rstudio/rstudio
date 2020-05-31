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
  selectedItemClassName: string;
  onSelectionChanged : (selectedIndex: number) => void;
  onSelectionCommitted: VoidFunction;
}

export const SymbolCharacterCell = ({ columnIndex, rowIndex, style, data }: any) => {
  // JJA: could you define an interface type for the argument here (and then make 'data' typed
  // within that interface). My worry here is that w/ the cast types are not checked
  // at all by the compiler.
  const characterGridCellItemData = data as CharacterGridCellItemData;
  const symbolCharacters = characterGridCellItemData.symbolCharacters;
  const itemIndex = rowIndex * characterGridCellItemData.numberOfColumns + columnIndex;

  if (itemIndex < symbolCharacters.length) {
      // JJA maybe shorted to 'ch'?
      const character = symbolCharacters[itemIndex];
      return (
        <div
          tabIndex={-1}
          style={style}
          className="pm-symbol-grid-container"
          // JJA: I get tslint warning here that lambdas are forbidden in JSX attributes
          // (for performance reasons). Let's a move these out. 
          onClick={event => {
            event.preventDefault();
            event.stopPropagation();
            characterGridCellItemData.onSelectionCommitted();
          }}
          // JJA: maybe add a brief comment as to why we are masking this event
          onMouseDown={event => {
            event.preventDefault();
            event.stopPropagation();
          }}
          onMouseOver={event => {
            characterGridCellItemData.onSelectionChanged(itemIndex);
          }}
        >
          <div className={`pm-symbol-grid-cell pm-grid-item ${characterGridCellItemData.selectedIndex === itemIndex ? characterGridCellItemData.selectedItemClassName : ''}`}>
          {character === undefined ? '' : character.value} 
          </div>
        </div>
        // JJA: I think you can equivilantly write the above as:
        //   character.value || ''
      );
    
  } else {
    return null;
  }
  

};
