/*
 * insert_citation-source-panel-crossref.tsx
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

import debounce from "lodash.debounce";

import { EditorUI } from "../../../api/ui";
import { BibliographySource } from "../../../api/bibliography/bibliography";
import { suggestCiteId } from "../../../api/cite";
import { sanitizeForCiteproc, CSL } from "../../../api/csl";

import { CitationSourcePanelProps, CitationSourcePanel } from "../insert_citation-panel";
import { CitationSourceSearchPanel } from "./insert_citation-source-panel-search";

export function crossrefSourcePanel(ui: EditorUI): CitationSourcePanel {

  const kCrossrefType = 'DOI Search';
  return {
    key: 'E38370AA-78AE-450B-BBE8-878E1C817C04',
    panel: CrossRefSourcePanel,
    treeNode: {
      key: 'CrossRef',
      name: ui.context.translateText('Crossref'),
      image: ui.images.citations?.crossref,
      type: kCrossrefType,
      children: [],
      expanded: true
    }
  };
}

export const CrossRefSourcePanel: React.FC<CitationSourcePanelProps> = props => {
  const [itemData, setItemData] = React.useState<BibliographySource[]>([]);
  const [searchTerm, setSearchTerm] = React.useState<string>();

  React.useEffect(debounce(() => {
    async function loadData() {
      if (props.selectedNode) {
        if (searchTerm) {
          const works = await props.server.crossref.works(searchTerm);
          const bibSources = works.items.map(item => {
            const sanitizedItem = sanitizeForCiteproc(item as unknown as CSL);
            const source = {
              ...sanitizedItem,
              id: suggestCiteId(props.bibliographyManager.allSources().map(src => src.id), sanitizedItem),
              providerKey: 'crossref',
              collectionKeys: [],
            };
            return source;
          });
          setItemData(bibSources);
        } else {
          setItemData([]);
        }
      }
    }
    loadData();

    // load the right panel
  }, 250), [searchTerm]);

  // Search the user search terms
  const searchChanged = (term: string) => {
    setSearchTerm(term);
  };

  return (
    <CitationSourceSearchPanel
      height={props.height}
      itemData={itemData}
      selectedNode={props.selectedNode}
      sourcesToAdd={props.sourcesToAdd}
      searchTermChanged={searchChanged}
      addSource={props.addSource}
      removeSource={props.removeSource}
      confirm={props.confirm}
      ui={props.ui}
    />
  );
};


