/*
 * insert_symbol-grid.tsx
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

import React from 'react';
import { FixedSizeGrid } from 'react-window';

import debounce from 'lodash.debounce';

import { CharacterGridCellItemData, SymbolCharacterCell } from './insert_symbol-grid-cell';
import { SymbolCharacter } from './insert_symbol-dataprovider';
import { SymbolPreview } from './insert_symbol-grid-preview';
import { WidgetProps } from '../../api/widgets/react';

import './insert_symbol-grid-styles.css';

interface CharacterGridProps extends WidgetProps {
  height: number;
  width: number;
  numberOfColumns: number;
  symbolCharacters: SymbolCharacter[];
  selectedIndex: number;
  onSelectionChanged: (selectedIndex: number) => void;
  onSelectionCommitted: VoidFunction;
}

const kPreviewHeight = 120;
const kPreviewWidth = 140;
const selectedItemClassName = 'pm-grid-item-selected';


const SymbolCharacterGrid = React.forwardRef<any, CharacterGridProps>((props, ref) => {

  const columnWidth = Math.floor(props.width / props.numberOfColumns);
  const characterCellData: CharacterGridCellItemData = {
    symbolCharacters: props.symbolCharacters,
    numberOfColumns: props.numberOfColumns,
    selectedIndex: props.selectedIndex,
    onSelectionChanged: props.onSelectionChanged,
    onSelectionCommitted: props.onSelectionCommitted,
    selectedItemClassName,
  };

  const gridRef = React.useRef<FixedSizeGrid>(null);
  const handleScroll = debounce(() => {
    gridRef.current?.scrollToItem({ rowIndex: Math.floor(props.selectedIndex / props.numberOfColumns) });
  }, 5);

  React.useEffect(handleScroll, [props.selectedIndex]);

  const handleMouseLeave = (event: React.MouseEvent) => {
    setMayShowPreview(false);
    setShowPreview(false);
  };

  const handleMouseEnter = (event: React.MouseEvent) => {
    setMayShowPreview(true);
  };

  const handleKeyDown = (event: React.KeyboardEvent) => {
    const newIndex = newIndexForKeyboardEvent(
      event,
      props.selectedIndex,
      props.numberOfColumns,
      props.symbolCharacters.length,
    );
    if (newIndex !== undefined) {
      props.onSelectionChanged(newIndex);
      event.preventDefault();
      setMayShowPreview(true);
    }
  };

  const [previewPosition, setPreviewPosition] = React.useState<[number, number]>([0, 0]);
  const [showPreview, setShowPreview] = React.useState<boolean>(false);
  const [mayShowPreview, setMayShowPreview] = React.useState<boolean>(false);

  const selectedCharacter = React.useMemo(() => {
    if (props.selectedIndex < props.symbolCharacters.length) {
      return props.symbolCharacters[props.selectedIndex];
    }
    return null;
  }, [props.selectedIndex, props.symbolCharacters]);

  React.useEffect(() => {
    if (mayShowPreview) {
      updatePreviewPosition();
      maybeShowPreview();
    }
  }, [props.selectedIndex, mayShowPreview]);

  React.useEffect(() => {
    if (props.symbolCharacters.length < 1) {
      setShowPreview(false);
    }
  }, [props.symbolCharacters, props.selectedIndex]);
  
  // Show the preview window after a short delay
  function maybeShowPreview() {
    if (previewTimer.current) {
      window.clearTimeout(previewTimer.current);
      previewTimer.current = undefined;
    }
    if (mayShowPreview && !showPreview) {
      previewTimer.current = window.setTimeout(() => {
        setShowPreview(true);
      }, kWaitToShowPreviewMs);
    }
  }  
  const kWaitToShowPreviewMs = 750;
  const previewTimer = React.useRef<number>();

  const gridInnerRef = React.useRef<HTMLElement>();
  function updatePreviewPosition() {
    const selectedCells = gridInnerRef.current?.getElementsByClassName(selectedItemClassName);
    if (selectedCells?.length === 1) {
      const cellRect = selectedCells.item(0)?.getBoundingClientRect();
      if (cellRect) {
        let top = cellRect.bottom + 1;
        if (top + kPreviewHeight > window.innerHeight) {
          top = cellRect.top - kPreviewHeight - 1;
        }
        let left = cellRect.left + (cellRect.right - cellRect.left) / 2;
        if (left + kPreviewWidth > window.innerWidth) {
          left = left - kPreviewWidth;
        }
        setPreviewPosition([left, top]);
      }
    }
  }

  return (
    <div
      onKeyDown={handleKeyDown}
      onMouseLeave={handleMouseLeave}
      onMouseEnter={handleMouseEnter}
      tabIndex={0}
      ref={ref}
    >
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
        innerRef={gridInnerRef}
      >
        {SymbolCharacterCell}
      </FixedSizeGrid>
      {showPreview && selectedCharacter && (
        <SymbolPreview
          left={previewPosition[0]}
          top={previewPosition[1]}
          height={kPreviewHeight}
          width={kPreviewWidth}
          symbolCharacter={selectedCharacter}
        />
      )}
    </div>
  );
});


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
  return newIndex >= 0 ? newIndex : currentIndex;
}
function nextRow(currentIndex: number, numberOfColumns: number, numberOfCells: number): number {
  const newIndex = currentIndex + numberOfColumns;
  return newIndex < numberOfCells ? newIndex : currentIndex;
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

    case 'ArrowRight': // right
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
