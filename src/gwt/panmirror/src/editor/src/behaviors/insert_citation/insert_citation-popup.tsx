/*
 * insert_citation-popup.tsx
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
import { BibliographyManager, BibliographySource } from '../../api/bibliography/bibliography';

import { InsertCitationPicker } from './insert_citation-picker';
import { EditorServer } from '../../api/server';


export async function showInsertCitationPopup(
  ui: EditorUI,
  doc: ProsemirrorNode,
  bibliographyManager: BibliographyManager,
  server: EditorServer,
) {

  // The citations that the user would like to insert
  let sources: BibliographySource[] = [];
  const onSourceChanged = (srcs: BibliographySource[]) => {
    sources = srcs;
  };

  // Render the element into the window
  const performInsert = await ui.dialogs.htmlDialog(
    "Insert Citation",
    "Insert",
    (containerWidth: number, containerHeight: number, confirm: VoidFunction, cancel: VoidFunction) => {

      const kMaxHeight = 600;
      const kMaxWidth = 900;
      const kMaxHeightProportion = .9;
      const kdialogPaddingIncludingButtons = 70;

      const windowHeight = containerHeight;
      const windowWidth = containerWidth;

      const height = Math.min(kMaxHeight, windowHeight * kMaxHeightProportion - kdialogPaddingIncludingButtons);
      const width = Math.min(kMaxWidth, windowWidth * .9);

      const container = window.document.createElement('div');

      container.style.height = height + 'px';
      container.style.width = width + 'px';
      ReactDOM.render(
        <InsertCitationPicker
          doc={doc}
          ui={ui}
          bibliographyManager={bibliographyManager}
          server={server}
          onSourceChanged={onSourceChanged}
          height={height}
          width={width} />
        , container);
      return container;

    },
    () => {
      // TODO: Focus the correct control (text filtering)?
    },
    () => {
      if (sources.length === 0) {
        return "Please select a citation to insert.";
      }
      return null;
    });

  if (performInsert && sources.length > 0) {
    window.alert('Inserting ' + sources.length + ' citations');
  }
}


