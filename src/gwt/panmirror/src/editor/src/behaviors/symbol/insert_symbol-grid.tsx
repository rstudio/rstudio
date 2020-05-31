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

// JJA: react libraries after react
import { FixedSizeGrid } from 'react-window';

import React from 'react';
import { WidgetProps } from '../../api/widgets/react';

// JJA: css style imports after all other imports
import './insert_symbol-grid-styles.css';
import { Symbol } from './insert_symbol-data';
import { CharacterGridCellItemData, SymbolCharacterCell } from './insert_symbol-grid-cell';
import { SymbolPreview } from './insert_symbol-grid-preview';

interface CharacterGridProps extends WidgetProps {
  height: number;
  width: number;
  numberOfColumns: number;
  symbolCharacters: Array<Symbol>;
  selectedIndex: number;
  onSelectionChanged: (selectedIndex: number) => void;
  onSelectionCommitted: VoidFunction;
}

// JJA: use e.g. kPreviewHeight
const previewHeight = 120;
const previewWidth = 140;
const selectedItemClassName = 'pm-grid-item-selected';

const SymbolCharacterGrid = React.forwardRef<any, CharacterGridProps>((props, ref) => {
  const columnWidth = Math.floor(props.width / props.numberOfColumns);

  const characterCellData: CharacterGridCellItemData = {
    symbolCharacters: props.symbolCharacters,
    numberOfColumns: props.numberOfColumns,
    selectedIndex: props.selectedIndex,
    onSelectionChanged: props.onSelectionChanged,
    onSelectionCommitted: props.onSelectionCommitted,
    selectedItemClassName: selectedItemClassName,
  };

  // Improve scroll performance per
  // https://developer.mozilla.org/en-US/docs/Web/API/Document/scroll_event
  let scrolling = false;
  const scrollWait = 5;
  const gridRef = React.useRef<FixedSizeGrid>(null);
  React.useEffect(() => {
    if (!scrolling) {
      window.setTimeout(() => {
        gridRef.current?.scrollToItem({ rowIndex: Math.floor(props.selectedIndex / props.numberOfColumns) });
        scrolling = false;
      }, scrollWait);
      scrolling = true;
    }
  }, [props.selectedIndex]);

  // JJA: it seems like both of these uses of setTimeout have the same form, perhaps break
  // out a helper that takes function into an 'api' module?
  let previewing = false;
  const mouseMoveWait = 25;
  function handleMouseMove(event: React.MouseEvent) {
    if (!previewing) {
      window.setTimeout(() => {
        maybeShowPreview();
        previewing = false;
      }, mouseMoveWait);
      previewing = true;
    }
  }

  const [previewPosition, setPreviewPosition] = React.useState<[number, number]>([0, 0]);
  const [showPreview, setShowPreview] = React.useState<boolean>(false);

  // JJA: I don't think we want an NodeJS types in our codebase?
  const timerRef = React.useRef<NodeJS.Timeout>();

  React.useEffect(() => {
    updatePreviewPosition();
  }, [props.selectedIndex]);

  React.useEffect(() => {
    if (props.symbolCharacters.length < 1) {
      setShowPreview(false);
    }
  }, [props.symbolCharacters]);

  // JJA: kWaitToShowPreviewMs
  const waitToShowPreviewMs = 1500;
  function maybeShowPreview() {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    timerRef.current = setTimeout(() => {
      if (props.symbolCharacters.length > 0) {
        updatePreviewPosition();
        setShowPreview(true);
      }
    }, waitToShowPreviewMs);
  }

  function hidePreview() {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    setShowPreview(false);
  }

  function updatePreviewPosition() {
    // JJA: it seems like we scope this search more narrowly? Do we have access
    // to the root element of the symbol popup or the symbol grid?
    const selectedCells = window.document.getElementsByClassName(selectedItemClassName);
    if (selectedCells.length === 1) {
      const cellRect = selectedCells.item(0)?.getBoundingClientRect();
      if (cellRect) {
        let top = cellRect.bottom + 1;
        if (top + previewHeight > window.innerHeight) {
          top = cellRect.top - previewHeight - 1;
        }
        let left = cellRect.left + (cellRect.right - cellRect.left) / 2;
        if (left + previewWidth > window.innerWidth) {
          left = left - previewWidth;
        }
        setPreviewPosition([left, top]);
      }
    }
  }

  // JJA: tslint no lambdas in JSX attributes
  return (
    <div
      onKeyDown={event => handleKeyDown(event, props)}
      onMouseLeave={event => hidePreview()}
      onMouseMove={event => handleMouseMove(event)}
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
      >
        {SymbolCharacterCell}
      </FixedSizeGrid>
      {showPreview && (
        <SymbolPreview
          left={previewPosition[0]}
          top={previewPosition[1]}
          height={previewHeight}
          width={previewWidth}
          symbol={props.symbolCharacters[props.selectedIndex]}
        />
      )}
    </div>
  );
});

const handleKeyDown = (event: React.KeyboardEvent, props: CharacterGridProps) => {
  const newIndex = newIndexForKeyboardEvent(
    event,
    props.selectedIndex,
    props.numberOfColumns,
    props.symbolCharacters.length,
  );
  if (newIndex !== undefined) {
    props.onSelectionChanged(newIndex);
    // JJA: does this also need to stopPropagation? (I have no idea, just raising the question)
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
