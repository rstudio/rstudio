/*
 * insert_citation-source-panel-search-latent.tsx
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

import { EditorUI } from "../../../api/ui";
import { TextInput } from "../../../api/widgets/text";
import { WidgetProps } from "../../../api/widgets/react";

import { CitationSourceList } from "./insert_citation-source-panel-list";
import { TextButton } from "../../../api/widgets/button";
import { CitationListEntry } from "../insert_citation-panel";

import './insert_citation-source-panel-latent-search.css';

export interface CitationSourceLatentSearchPanelProps extends WidgetProps {
  height: number;
  citations: CitationListEntry[];
  citationsToAdd: CitationListEntry[];
  addCitation: (citation: CitationListEntry) => void;
  removeCitation: (citation: CitationListEntry) => void;
  doSearch: (searchTerm: string) => void;
  confirm: VoidFunction;
  loading: boolean;
  ui: EditorUI;
  defaultText?: string;
  placeholderText?: string;
}

export const CitationSourceLatentSearchPanel: React.FC<CitationSourceLatentSearchPanelProps> = props => {

  const listContainer = React.useRef<HTMLDivElement>(null);
  const [searchTerm, setSearchTerm] = React.useState<string>('');
  const [searchImmediate, setSearchImmediate] = React.useState<boolean>(false);

  // Track whether this component is mounted so we can safely ignore debounced searches
  // if they return after the component has been unmounted
  const isMountedRef = React.useRef<boolean>(true);
  React.useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const performSearch = (search: string) => {
    if (isMountedRef.current) {
      props.doSearch(search);
      setSearchImmediate(false);
    }
  };

  // Search the user search terms
  const searchChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    const search = e.target.value;
    setSearchTerm(search);
    if (searchImmediate) {
      performSearch(search);
    }
  };

  // Perform first load tasks
  const searchBoxRef = React.useRef<HTMLInputElement>(null);
  const [listHeight, setListHeight] = React.useState<number>(props.height);
  React.useLayoutEffect(() => {

    // Size the list Box
    const searchBoxHeight = searchBoxRef.current?.clientHeight;
    const padding = 10;
    if (searchBoxHeight) {
      setListHeight(props.height - padding - searchBoxHeight);
    }

    // Focus the search box
    if (searchBoxRef.current) {
      searchBoxRef.current.focus();
    }
  }, []);

  // If the user arrows down in the search text box, advance to the list of items
  const handleTextKeyDown = (event: React.KeyboardEvent) => {
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        event.stopPropagation();
        listContainer.current?.focus();
        break;
      case 'Enter':
        event.preventDefault();
        event.stopPropagation();
        performSearch(searchTerm);
        break;
    }
  };

  const handleButtonClick = () => {
    performSearch(searchTerm);
  };

  const onPaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    setSearchImmediate(true);
  };

  const placeholder = props.placeholderText || props.ui.context.translateText('Search for citation');
  const defaultText = searchTerm.length > 0 ? props.ui.context.translateText('No matching results') : props.defaultText || '';

  return (
    <div style={props.style} className='pm-insert-citation-panel-latent-search'>
      <div className='pm-insert-citation-panel-latent-search-textbox-container'>
        <TextInput
          width='100%'
          iconAdornment={props.ui.images.search}
          tabIndex={0}
          className='pm-insert-citation-panel-latent-search-textbox pm-block-border-color'
          placeholder={placeholder}
          onKeyDown={handleTextKeyDown}
          onChange={searchChanged}
          onPaste={onPaste}
          ref={searchBoxRef}
        />

        <TextButton
          title={props.ui.context.translateText(props.loading ? 'Loading' : 'Search')}
          classes={['pm-insert-citation-panel-latent-search-button']}
          onClick={handleButtonClick}
          disabled={props.loading}
        />

      </div>

      <CitationSourceList
        height={listHeight}
        citations={props.citations}
        citationsToAdd={props.citationsToAdd}
        confirm={props.confirm}
        addCitation={props.addCitation}
        removeCitation={props.removeCitation}
        ui={props.ui}
        noResultsText={defaultText}
        classes={['pm-insert-citation-panel-latent-search-list', 'pm-block-border-color', 'pm-background-color']}
        ref={listContainer}
      />
    </div>);
};


