import { FixedSizeGrid as Grid, GridChildComponentProps } from 'react-window';

import React from 'react';
import { WidgetProps } from '../../api/widgets/react';

import './insert_symbol-grid-styles.css';
import { SymbolCharacter } from './insert_symbol-data';
import { findNextInputElement, findPreviousInputElement } from '../../api/html';
import { CharacterGridCellItemData, SymbolCharacterCell } from './insert_symbol-grid-cell';

// TODO: Empty state where no matching symbols
// TODO: Selection vs focus - should focus follow hover?
// TODO: If only one item is selected, we should focus that so that enter key will obviously work
interface CharacterGridProps extends WidgetProps {
  height: number;
  width: number;
  numberOfColumns: number;
  symbolCharacters: Array<SymbolCharacter>;
  onSymbolCharacterSelected: (char: string) => void;
  onChangeFocus: (previous: boolean) => void;
}

const SymbolCharacterGrid = React.forwardRef<any, CharacterGridProps>((props, ref) => {
  const columnWidth = Math.floor(props.width / props.numberOfColumns);

  const characterCellData: CharacterGridCellItemData = {
    symbolCharacters: props.symbolCharacters,
    onSymbolCharacterSelected: props.onSymbolCharacterSelected,
    onKeyDown: createKeyDownHandler(props.numberOfColumns, props.onChangeFocus),
    numberOfColumns: props.numberOfColumns,
  };

  return (
    <Grid
      columnCount={props.numberOfColumns}
      rowCount={Math.ceil(props.symbolCharacters.length / props.numberOfColumns)}
      height={props.height}
      width={props.width + 1}
      rowHeight={columnWidth}
      columnWidth={columnWidth}
      itemData={characterCellData}
      className="pm-symbol-grid"
      innerRef={ref}
    >
      {SymbolCharacterCell}
    </Grid>
  );
});

// TODO: page up / down and focus issues
function createKeyDownHandler(
  numberOfColumns: number,
  onChangeFocus: (previous: boolean) => void,
): (event: React.KeyboardEvent) => boolean {
  return (event: React.KeyboardEvent): boolean => {
    const thisElement = event.target as HTMLDivElement;
    switch (event.keyCode) {
      case 9:
        onChangeFocus(event.shiftKey);
        return true;

      case 37: // left
        const previousSibling = thisElement.previousElementSibling as HTMLDivElement;
        if (previousSibling != null) {
          previousSibling.focus();
          return true;
        }
        break;

      case 38: // up
        let previousRowElement = thisElement;
        for (let i = 0; i < numberOfColumns; i++) {
          previousRowElement = previousRowElement.previousElementSibling as HTMLDivElement;
        }

        if (previousRowElement !== null) {
          previousRowElement.focus();
          return true;
        }
        break;

      case 39: //right
        const nextSibling = thisElement.nextElementSibling as HTMLDivElement;
        if (nextSibling != null) {
          nextSibling.focus();
          return true;
        }
        break;
      case 40: // down
        let nextRowElement = thisElement;
        for (let i = 0; i < numberOfColumns; i++) {
          nextRowElement = nextRowElement.nextElementSibling as HTMLDivElement;
        }

        if (nextRowElement !== null) {
          nextRowElement.focus();
          return true;
        }
        break;

      default:
        return false;
    }
    return false;
  };
}

export default SymbolCharacterGrid;
