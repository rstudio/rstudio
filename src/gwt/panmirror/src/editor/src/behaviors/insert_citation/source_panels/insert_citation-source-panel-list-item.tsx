/*
 * insert_citation-source-panel-list-item.ts
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
import { ListChildComponentProps } from "react-window";

import { EditorUI } from "../../../api/ui";
import { OutlineButton } from "../../../api/widgets/button";

import { CitationListEntry } from "../insert_citation-panel";

import './insert_citation-source-panel-list-item.css';

interface CitationSourcePanelListData {
  citations: CitationListEntry[];
  citationsToAdd: CitationListEntry[];
  selectedIndex: number;
  onSelectedIndexChanged: (index: number) => void;
  onAddCitation: (source: CitationListEntry) => void;
  onRemoveCitation: (source: CitationListEntry) => void;
  onConfirm: () => void;
  ui: EditorUI;
  showSeparator?: boolean;
  showSelection?: boolean;
  preventFocus?: boolean;
}

export const CitationSourcePanelListItem = (props: ListChildComponentProps) => {

  const citationListData: CitationSourcePanelListData = props.data;

  const citationEntry = citationListData.citations[props.index];

  // NOTE: Could consider making this length dynamic to account for item width
  const maxIdLength = 30;
  const id = citationEntry.id.length > maxIdLength ? `@${citationEntry.id.substr(0, maxIdLength - 1)}…` : `@${citationEntry.id}`;
  const authorWidth = Math.max(10, 50 - id.length);


  // Wheher this item is selected
  const selected = citationListData.showSelection && props.index === citationListData.selectedIndex;

  // Whether this item is already in the list of items to add
  // If the item is selected, it is always a candidate to be added explicitly to the list
  const alreadyAdded = citationListData.citationsToAdd.map(src => src.id).includes(citationEntry.id) && !selected;

  const onButtonClick = (event: React.MouseEvent) => {
    event.stopPropagation();
    event.preventDefault();

    if (alreadyAdded) {
      citationListData.onRemoveCitation(citationEntry);
    } else {
      citationListData.onAddCitation(citationEntry);
    }
  };

  const onItemClick = () => {
    citationListData.onSelectedIndexChanged(props.index);
  };

  const onDoubleClick = () => {
    citationListData.onAddCitation(citationEntry);
    citationListData.onConfirm();
  };

  // TODO: Localize +/- button

  return (
    <div onClick={onItemClick} onDoubleClick={onDoubleClick} className='pm-insert-citation-source-panel-item' style={props.style}>
      <div className={`pm-insert-citation-source-panel-item-border ${selected ? 'pm-list-item-selected' : ''}`}>
        <div className='pm-insert-citation-source-panel-item-container'>
          <div className='pm-insert-citation-source-panel-item-type'>
            {citationEntry.imageAdornment ? <img className='pm-insert-citation-source-panel-item-adorn pm-block-border-color pm-background-color' src={citationEntry.imageAdornment} /> : undefined}
            <img className='pm-insert-citation-source-panel-item-icon pm-block-border-color' src={citationEntry.image} />
          </div>
          <div className='pm-insert-citation-source-panel-item-summary'>
            <div className='pm-insert-citation-source-panel-item-id'>
              <div className='pm-insert-citation-source-panel-item-title pm-fixedwidth-font pm-text-color'>{id}</div>
              <div className='pm-insert-citation-source-panel-item-detail pm-text-color'>{citationEntry.authors(authorWidth)} {citationEntry.date}</div>
            </div>
            <div className='pm-insert-citation-source-panel-item-subtitle-text pm-text-color'>{citationEntry.title}</div>
          </div>
          <div className='pm-insert-citation-source-panel-item-button'>
            <OutlineButton
              tabIndex={citationListData.preventFocus ? -1 : 0}
              style={{ width: '24px', height: '24px' }}
              title={alreadyAdded ? '-' : '+'}
              onClick={onButtonClick}
            />
          </div>
        </div>
      </div>
    </div>
  );
};