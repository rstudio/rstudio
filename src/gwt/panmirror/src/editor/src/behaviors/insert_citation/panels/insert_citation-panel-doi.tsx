/*
 * insert_citation-panel-doi.ts
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

import { CitationPanelProps, CitationPanel } from "../insert_citation-picker";
import { EditorUI } from "../../../api/ui";

import './insert_citation-panel-doi.css';

export function doiPanel(ui: EditorUI): CitationPanel {
  return {
    key: '76561E2A-8FB7-4D4B-B235-9DD8B8270EA1',
    panel: CitationDOIPanel,
    treeNode: {
      key: 'DOI',
      name: "Find DOI",
      image: ui.images.citations?.doi,
      type: kDOIType,
      children: [],
      expanded: true
    }
  };
}

export const CitationDOIPanel: React.FC<CitationPanelProps> = props => {
  return (<div>DOI TOWN!</div>);
};
export const kDOIType = 'DOI Search';
