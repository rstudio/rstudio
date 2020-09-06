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
import { EditorServer } from '../../api/server';
import { BibliographyManager, BibliographyFile } from '../../api/bibliography/bibliography';

import { InsertCitationPanel, CitationListEntry } from './insert_citation-panel';
import { NavigationTreeNode } from '../../api/widgets/navigation-tree';


// When the dialog has completed, it will return this result
// If the dialog is canceled no result will be returned
export interface InsertCitationDialogResult {
  citations: CitationListEntry[];
  bibliography: BibliographyFile;
  selectedNode?: NavigationTreeNode;
}

export async function selectCitations(
  ui: EditorUI,
  doc: ProsemirrorNode,
  bibliographyManager: BibliographyManager,
  server: EditorServer,
  selectedNode?: NavigationTreeNode,
): Promise<InsertCitationDialogResult | undefined> {

  // The citations that the user would like to insert
  let citations: CitationListEntry[] = [];
  const onCitationsChanged = (c: CitationListEntry[]) => {
    citations = c;
  };

  // The bibliography into which entries should be written
  let bibliography: BibliographyFile | undefined;
  const onBibliographyChanged = (bibliographyFile: BibliographyFile) => {
    bibliography = bibliographyFile;
  };

  let lastSelectedNode: NavigationTreeNode | undefined;
  const onSelectedNodeChanged = (node: NavigationTreeNode) => {
    lastSelectedNode = node;
  };

  // Render the element into the window
  const performInsert = await ui.dialogs.htmlDialog(
    "Insert Citation",
    "Insert",
    (containerWidth: number, containerHeight: number, confirm: VoidFunction, cancel: VoidFunction) => {

      const kMaxHeight = 650;
      const kMaxWidth = 900;
      const kMaxHeightProportion = .9;
      const kdialogPaddingIncludingButtons = 70;

      const windowHeight = containerHeight;
      const windowWidth = containerWidth;

      const height = Math.min(kMaxHeight, windowHeight * kMaxHeightProportion - kdialogPaddingIncludingButtons);
      const width = Math.max(Math.min(kMaxWidth, windowWidth * .9), 550);

      const container = window.document.createElement('div');
      container.className = 'pm-default-theme';

      container.style.width = width + 'px';
      ReactDOM.render(
        <InsertCitationPanel
          doc={doc}
          ui={ui}
          bibliographyManager={bibliographyManager}
          server={server}
          onCitationsChanged={onCitationsChanged}
          onBibliographyChanged={onBibliographyChanged}
          onSelectedNodeChanged={onSelectedNodeChanged}
          selectedNode={selectedNode}
          onOk={confirm}
          onCancel={cancel}
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

  if (performInsert && citations.length > 0 && bibliography) {
    return {
      citations,
      bibliography,
      selectedNode: lastSelectedNode
    };
  } else {
    return undefined;
  }
}