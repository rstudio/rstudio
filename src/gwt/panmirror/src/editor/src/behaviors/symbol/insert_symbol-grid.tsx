import { FixedSizeGrid } from 'react-window';

import React from 'react';
import { WidgetProps } from '../../api/widgets/react';

import './insert_symbol-grid-styles.css';
import { Symbol } from './insert_symbol-data';
import { CharacterGridCellItemData, SymbolCharacterCell } from './insert_symbol-grid-cell';

interface CharacterGridProps extends WidgetProps {
  height: number;
  width: number;
  numberOfColumns: number;
  symbolCharacters: Array<Symbol>;
  selectedIndex: number;
  onSelectionChanged: (selectedIndex: number) => void;
  onSelectionCommitted: VoidFunction;
}

const SymbolCharacterGrid = React.forwardRef<any, CharacterGridProps>((props, ref) => {
  const columnWidth = Math.floor(props.width / props.numberOfColumns);

  const characterCellData: CharacterGridCellItemData = {
    symbolCharacters: props.symbolCharacters,
    numberOfColumns: props.numberOfColumns,
    selectedIndex: props.selectedIndex,
    onSelectionChanged: props.onSelectionChanged,
    onSelectionCommitted: props.onSelectionCommitted,
  };

  // Improve scroll performance per
  // https://developer.mozilla.org/en-US/docs/Web/API/Document/scroll_event
  let ticking = false;
  const gridRef = React.useRef<FixedSizeGrid>(null);
  React.useEffect(() => {
    if (!ticking) {
      window.setTimeout(() => {
        gridRef.current?.scrollToItem({ rowIndex: Math.floor(props.selectedIndex / props.numberOfColumns) });
        ticking = false;
      }, 5);
      ticking = true;
    }
  }, [props.selectedIndex]);

  return (
    <div onKeyDown={event => handleKeyDown(event, props)} tabIndex={0} ref={ref}>
      <FixedSizeGrid
        columnCount={props.numberOfColumns}
        rowCount={Math.ceil(props.symbolCharacters.length / props.numberOfColumns)}
        height={props.height}
        width={props.width + 1}
        rowHeight={columnWidth}
        columnWidth={columnWidth}
        itemData={characterCellData}
        className="pm-symbol-grid"
        ref={gridRef}
      >
        {SymbolCharacterCell}
      </FixedSizeGrid>
    </div>
  );
});


const handleKeyDown = (event: React.KeyboardEvent, props: CharacterGridProps) => {
  const newIndex = newIndexForKeyboardEvent(event, props.selectedIndex, props.numberOfColumns, props.symbolCharacters.length);
  if (newIndex) {
    props.onSelectionChanged(newIndex);
    event.preventDefault();
  }
};

function previous(currentIndex: number, numberOfColumns: number, numberOfCells: number): number {
  const newIndex = currentIndex - 1;
  return Math.max(0, newIndex);
}
function next(currentIndex: number, numberOfColumns: number, numberOfCells: number): number {
  const newIndex = currentIndex + 1;
  return Math.min(numberOfCells - 1, newIndex);
}
function prevRow(currentIndex: number, numberOfColumns: number, numberOfCells: number): number {
  const newIndex = currentIndex - numberOfColumns;
  return Math.max(0, newIndex);
}
function nextRow(currentIndex: number, numberOfColumns: number, numberOfCells: number): number {
  const newIndex = currentIndex + numberOfColumns;
  return Math.min(numberOfCells - 1, newIndex);
}
function nextPage(currentIndex: number, numberOfColumns: number, numberOfCells: number): number {
  const newIndex = currentIndex + 6 * numberOfColumns;
  return Math.min(numberOfCells - 1, newIndex);
}
function prevPage(currentIndex: number, numberOfColumns: number, numberOfCells: number): number {
  const newIndex = currentIndex - 6 * numberOfColumns;
  return Math.max(0, newIndex);
}

export const newIndexForKeyboardEvent = (
  event: React.KeyboardEvent,
  selectedIndex: number,
  numberOfColumns: number,
  numberOfCells: number,
): number | undefined => {
  switch (event.key) {
    case 'ArrowLeft': // left
      return previous(selectedIndex, numberOfColumns, numberOfCells);

    case 'ArrowUp': // up
      return prevRow(selectedIndex, numberOfColumns, numberOfCells);

    case 'ArrowRight': //right
      return next(selectedIndex, numberOfColumns, numberOfCells);

    case 'ArrowDown': // down
      return nextRow(selectedIndex, numberOfColumns, numberOfCells);

    case 'PageDown':
      return nextPage(selectedIndex, numberOfColumns, numberOfCells);

    case 'PageUp':
      return prevPage(selectedIndex, numberOfColumns, numberOfCells);

    case 'Home':
      return 0;

    case 'End':
      return numberOfCells - 1;

    default:
      return undefined;
  }
};


export default SymbolCharacterGrid;
