import { FixedSizeGrid as Grid, GridChildComponentProps } from 'react-window';

import React from 'react';
import { WidgetProps } from '../api/widgets/react';

import './insert_symbol-grid-styles.css';
import { SymbolCharacter } from './insert_symbol-data';

// TODO: Empty state where no matching symbols
// TODO: Need to implement keyboard handling for arrows, enter key.
// TODO: Ensure tab indexes are properly set for tabbing
// TODO: The added pixel below isn't really working to ensure that the borders are visible, investigate

interface CharacterGridCellItemData {
  symbolCharacters: Array<SymbolCharacter>;
  onCharacterSelected: (char: string) => void;
  numberOfColumns: number;
}

const Cell = ({ columnIndex, rowIndex, style, data }: any) => {
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
      className="pm-symbol-grid-cell pm-block-border-color"
      style={style}
      onClick={event => {
        event.preventDefault();
        event.stopPropagation();
        if (character != undefined) {
          characterGridCellItemData.onCharacterSelected(character.value as string);
        }
      }}
      onMouseDown={event => {
        event.preventDefault();
        event.stopPropagation();
      }}
    >
      {character === undefined ? '' : character.value}
    </div>
  );
};

interface CharacterGridProps extends WidgetProps {
  height: number;
  width: number;
  rowHeight: number;
  columnWidth: number;
  symbolCharacters: Array<SymbolCharacter>;
  onCharacterSelected: (char: string) => void;
}

const CharacterGrid: React.FC<CharacterGridProps> = props => {
  const numberOfColumns = Math.ceil(props.width / props.columnWidth);
  const characterCellData: CharacterGridCellItemData = {
    symbolCharacters: props.symbolCharacters,
    onCharacterSelected: props.onCharacterSelected,
    numberOfColumns: numberOfColumns,
  };

  // TODO: Do we need pm-symbol-grid style?
  return (
    <Grid
      columnCount={numberOfColumns}
      rowCount={Math.ceil(props.symbolCharacters.length / numberOfColumns)}
      height={props.height + 1}
      width={props.width + 1}
      rowHeight={props.rowHeight}
      columnWidth={props.columnWidth}
      itemData={characterCellData}
      className="pm-symbol-grid"
    >
      {Cell}
    </Grid>
  );
};

export default CharacterGrid;
