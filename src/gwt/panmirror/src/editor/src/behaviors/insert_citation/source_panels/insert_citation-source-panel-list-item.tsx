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
import { entryForSource } from "../../../marks/cite/cite-bibliography_entry";

import { BibliographySource } from "../../../api/bibliography/bibliography";

import './insert_citation-source-panel-list-item.css';

interface CitationSourcePanelListData {
  allSources: BibliographySource[];
  sourcesToAdd: BibliographySource[];
  addSource: (source: BibliographySource) => void;
  removeSource: (source: BibliographySource) => void;
  ui: EditorUI;
  showSeparator?: boolean;
}

export const CitationSourcePanelListItem = (props: ListChildComponentProps) => {

  const citationListData: CitationSourcePanelListData = props.data;
  const source = citationListData.allSources[props.index];

  // An entry contains formatted values for display
  // NOTE: Dark mode is disabled right here since this is currently hosted in a 
  // dialog. It would be nice to use a style or something to signal this
  const entry = entryForSource(source, props.data.ui, false);

  // NOTE: Could consider making this length dynamic to account for item width
  const maxIdLength = 30;
  const id = entry.source.id.length > maxIdLength ? `@${entry.source.id.substr(0, maxIdLength - 1)}…` : `@${entry.source.id}`;
  const authorWidth = Math.max(10, 50 - id.length);

  // Whether this item is already in the list of items to add
  const alreadyAdded = citationListData.sourcesToAdd.map(src => src.id).includes(source.id);

  const onClick = () => {
    if (alreadyAdded) {
      citationListData.removeSource(source);
    } else {
      citationListData.addSource(source);
    }
  };

  return (
    <div>
      <div className='pm-insert-citation-source-panel-item' style={props.style}>
        <div className='pm-insert-citation-source-panel-item-container'>
          <div className='pm-insert-citation-source-panel-item-type'>
            {entry.adornmentImage ? <img className='pm-insert-citation-source-panel-item-adorn pm-block-border-color pm-background-color' src={entry.adornmentImage} /> : undefined}
            <img className='pm-insert-citation-source-panel-item-icon pm-block-border-color' src={entry.image} />
          </div>
          <div className='pm-insert-citation-source-panel-item-summary'>
            <div className='pm-insert-citation-source-panel-item-id'>
              <div className='pm-insert-citation-source-panel-item-title pm-fixedwidth-font pm-text-color'>{id}</div>
              <div className='pm-insert-citation-source-panel-item-detail pm-text-color'>{entry.authorsFormatter(source.author, authorWidth)} {entry.issuedDateFormatter(source.issued)}</div>
            </div>
            <div className='pm-insert-citation-source-panel-item-subtitle-text pm-text-color'>{source.title}</div>
          </div>
          <div className='pm-insert-citation-source-panel-item-button'>
            <OutlineButton
              style={{ width: '70px' }}
              title={alreadyAdded ? 'Remove' : 'Add'}
              onClick={onClick}
            />
          </div>
        </div>
        {
          citationListData.showSeparator ?
            <div className='pm-insert-citation-source-panel-item-separator pm-block-border-color' /> :
            undefined
        }
      </div>
    </div>
  );
};