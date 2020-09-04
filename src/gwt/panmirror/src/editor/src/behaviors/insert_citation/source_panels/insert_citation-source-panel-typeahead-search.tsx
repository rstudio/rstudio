/*
 * insert_citation-source-panel-typeahead-search.tsx
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
import { NavigationTreeNode } from "../../../api/widgets/navigation-tree";

import './insert_citation-source-panel-typeahead-search.css';
import { CitationSourceList, CitationSourceListStatus } from "./insert_citation-source-panel-list";
import { CitationListEntry } from "../insert_citation-panel";

export interface CitationSourceTypeaheadSearchPanelProps extends WidgetProps {
  height: number;
  selectedNode?: NavigationTreeNode;
  citations: CitationListEntry[];
  citationsToAdd: CitationListEntry[];
  addCitation: (citation: CitationListEntry) => void;
  removeCitation: (citation: CitationListEntry) => void;
  searchTermChanged: (searchTerm: string) => void;
  confirm: VoidFunction;
  ui: EditorUI;
}

export const CitationSourceTypeheadSearchPanel: React.FC<CitationSourceTypeaheadSearchPanelProps> = props => {

  const listContainer = React.useRef<HTMLDivElement>(null);

  // Search the user search terms
  const searchChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    const search = e.target.value;
    props.searchTermChanged(search);
  };

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

  // If the user arrows down in the search text box, advance to the list of items
  const handleTextKeyDown = (event: React.KeyboardEvent) => {
    switch (event.key) {
      case 'ArrowDown':
        listContainer.current?.focus();
        break;
    }
  };

  return (
    <div style={props.style} className='pm-insert-citation-panel-search pm-block-border-color pm-background-color'>
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
      <CitationSourceList
        height={listHeight}
        citations={props.citations}
        citationsToAdd={props.citationsToAdd}
        confirm={props.confirm}
        addCitation={props.addCitation}
        removeCitation={props.removeCitation}
        status={props.citations.length === 0 ? CitationSourceListStatus.noResults : CitationSourceListStatus.default}
        ui={props.ui}
        ref={listContainer}
      />
    </div>);
};


