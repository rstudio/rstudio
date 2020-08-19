/*
 * insert_symbol-plugin.tsx
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
import ReactDOM from 'react-dom';

import { Node as ProsemirrorNode } from 'prosemirror-model';

import { EditorUI } from '../../api/ui';
import { ZoteroCollectionSpec } from '../../api/zotero';
import { BibliographyManager } from '../../api/bibliography/bibliography';

import { InsertCitationPanel } from './insert_citation-panel';


interface CitationSourceProvider {
  name: string;
}

interface InsertCitationDataProvider {
  getProviders(): CitationSourceProvider[];
  getCollections(): ZoteroCollectionSpec[];
}


export async function showInsertCitationPopup(ui: EditorUI, doc: ProsemirrorNode, bibliographyManager: BibliographyManager) {

  // The citations that the user would like to insert
  let citations: string[] = [];
  const onCitationChanged = (cites: string[]) => {
    citations = cites;
  };

  // Render the element into the window
  const performInsert = await ui.dialogs.htmlDialog(
    "Insert Citation",
    "Insert",
    (containerWidth: number, containerHeight: number, confirm: VoidFunction, cancel: VoidFunction) => {

      const kMaxHeight = 600;
      const kMaxHeightProportion = .9;
      const kWidthProportion = 1.33;

      const windowHeight = containerHeight;
      const windowWidth = containerWidth;

      const height = Math.min(kMaxHeight, windowHeight * kMaxHeightProportion);
      const width = Math.min(kWidthProportion * height, windowWidth * .9);

      const container = window.document.createElement('div');

      container.style.height = height + 'px';
      container.style.width = width + 'px';
      ReactDOM.render(
        <InsertCitationPanel
          doc={doc}
          ui={ui}
          bibliographyManager={bibliographyManager}
          onCitationChanged={onCitationChanged}
          height={height}
          width={width} />
        , container);
      return container;

    },
    () => {
      // TODO: Focus the correct control (text filtering)?
    },
    () => {
      if (citations.length === 0) {
        return "Please select a citation to insert.";
      }
      return null;
    });

  if (performInsert && citations.length > 0) {
    console.log(citations);
    window.alert('Inserting ' + citations.length + ' citations');
  }
}


