/*
 * insert_citation-source-panel-search.tsx
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import React from "react";

import { FixedSizeList } from "react-window";

import { EditorUI } from "../../../api/ui";
import { TextInput } from "../../../api/widgets/text";
import { BibliographySource } from "../../../api/bibliography/bibliography";
import { WidgetProps } from "../../../api/widgets/react";
import { NavigationTreeNode } from "../../../api/widgets/navigation-tree";

import { CitationSourcePanelListItem } from "./insert_citation-source-panel-list-item";

import './insert_citation-source-panel-search.css';

export interface CitationSourceSearchPanelProps extends WidgetProps {
  itemData: BibliographySource[];
  sourcesToAdd: BibliographySource[];
  height: number;
  selectedNode?: NavigationTreeNode;
  addSource: (source: BibliographySource) => void;
  removeSource: (source: BibliographySource) => void;
  searchTermChanged: (searchTerm: string) => void;
  confirm: VoidFunction;
  ui: EditorUI;
}

export const CitationSourceSearchPanel: React.FC<CitationSourceSearchPanelProps> = props => {

  const [selectedIndex, setSelectedIndex] = React.useState<number>(0);
  const [focused, setFocused] = React.useState<boolean>(false);
  const fixedList = React.useRef<FixedSizeList>(null);
  const listContainer = React.useRef<HTMLDivElement>(null);

  // Search the user search terms
  const searchChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    const search = e.target.value;
    props.searchTermChanged(search);
  };

  // Whenever selection changed, ensure that we are scrolled to that item
  React.useLayoutEffect(() => {
    fixedList.current?.scrollToItem(selectedIndex);
  }, [selectedIndex]);

  // Perform first load tasks
  const searchBoxRef = React.useRef<HTMLInputElement>(null);
  const [listHeight, setListHeight] = React.useState<number>(props.height);
  React.useLayoutEffect(() => {

    // Size the list Box
    const searchBoxHeight = searchBoxRef.current?.clientHeight;
    if (searchBoxHeight) {
      setListHeight(props.height - searchBoxHeight);
    }

    // Focus the search box
    if (searchBoxRef.current) {
      searchBoxRef.current.focus();
    }
  }, []);

  React.useLayoutEffect(() => {
    if (searchBoxRef.current) {
      searchBoxRef.current.value = '';
    }
  }, [props.selectedNode]);

  // Reset the index whenever the data changes
  React.useEffect(() => {
    setSelectedIndex(0);
  }, [props.itemData]);

  // Item height and consequently page height
  const itemHeight = 64;
  const itemsPerPage = Math.floor(props.height / itemHeight);

  // Upddate selected item index (this will manage bounds)
  const incrementIndex = (event: React.KeyboardEvent, index: number) => {
    event.stopPropagation();
    event.preventDefault();
    const maxIndex = props.itemData.length - 1;
    setSelectedIndex(Math.min(Math.max(0, index), maxIndex));
  };

  // Toggle the currently selected item as added or removed
  const toggleSelectedSource = (event: React.KeyboardEvent) => {
    event.stopPropagation();
    event.preventDefault();

    const source = props.itemData[selectedIndex];
    if (source) {
      if (props.sourcesToAdd.includes(source)) {
        props.removeSource(source);
      } else {
        props.addSource(source);
      }
    }
  };

  const handleListKeyDown = (event: React.KeyboardEvent) => {
    switch (event.key) {
      case 'ArrowUp':
        incrementIndex(event, selectedIndex - 1);
        break;

      case 'ArrowDown':
        incrementIndex(event, selectedIndex + 1);
        break;

      case 'PageDown':
        incrementIndex(event, selectedIndex + itemsPerPage);
        break;

      case 'PageUp':
        incrementIndex(event, selectedIndex - itemsPerPage);
        break;

      case 'Enter':
        toggleSelectedSource(event);
        props.confirm();
        break;
      case ' ':
        toggleSelectedSource(event);
        break;
    }
  };

  // If the user arrows down in the search text box, advance to the list of items
  const handleTextKeyDown = (event: React.KeyboardEvent) => {
    switch (event.key) {
      case 'ArrowDown':
        listContainer.current?.focus();
        break;
    }
  };

  // Focus / Blur are used to track whether to show selection highlighting
  const onFocus = (event: React.FocusEvent<HTMLDivElement>) => {
    setFocused(true);
  };

  const onBlur = (event: React.FocusEvent<HTMLDivElement>) => {
    setFocused(false);
  };

  return (
    <div style={props.style} className='pm-insert-citation-panel-search'>
      <div className='pm-insert-citation-search-panel-textbox-container'>
        <TextInput
          width='100%'
          iconAdornment={props.ui.images.search}
          tabIndex={0}
          className='pm-insert-citation-panel-search-textbox pm-block-border-color'
          placeholder={props.ui.context.translateText('Search for citation')}
          onKeyDown={handleTextKeyDown}
          onChange={searchChanged}
          ref={searchBoxRef}
        />

      </div>
      {props.itemData.length === 0 ?
        (<div className='pm-insert-citation-panel-search-noresults' style={{ height: listHeight + 'px' }}>
          <div className='pm-insert-citation-panel-search-noresults-text'>{props.ui.context.translateText('No matching results.')}</div>
        </div>) :
        (
          <div tabIndex={0} onKeyDown={handleListKeyDown} onFocus={onFocus} onBlur={onBlur} ref={listContainer}>
            <FixedSizeList
              height={listHeight}
              width='100%'
              itemCount={props.itemData.length}
              itemSize={itemHeight}
              itemData={{
                selectedIndex,
                allSources: props.itemData,
                sourcesToAdd: props.sourcesToAdd,
                addSource: props.addSource,
                removeSource: props.removeSource,
                ui: props.ui,
                showSeparator: true,
                showSelection: focused,
                preventFocus: true
              }}
              ref={fixedList}
            >
              {CitationSourcePanelListItem}
            </FixedSizeList>
          </div>
        )}
    </div>);
};


