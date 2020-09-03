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

import { EditorUI } from "../../../api/ui";
import { BibliographySource } from "../../../api/bibliography/bibliography";
import { suggestCiteId } from "../../../api/cite";
import { sanitizeForCiteproc, CSL } from "../../../api/csl";

import { CitationSourcePanelProps, CitationSourcePanel } from "../insert_citation-panel";
import { CitationSourceLatentSearchPanel } from "./insert_citation-source-panel-latent-search";

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
  const [loading, setLoading] = React.useState<boolean>(false);
  const [searchTerm, setSearchTerm] = React.useState<string>('');

  React.useEffect(() => {
    let mounted = true;
    const performSearch = async () => {
      if (mounted) {
        setLoading(true);
      }
      const works = await props.server.crossref.works(searchTerm);

      // Get the list of ids already in the bibliography
      const existingIds = props.bibliographyManager.allSources().map(src => src.id);

      const bibSources = works.items.map(item => {
        const sanitizedItem = sanitizeForCiteproc(item as unknown as CSL);
        const suggestedId = suggestCiteId(existingIds, sanitizedItem);

        // Add this id to the list of existing Ids so future ids will de-duplicate against this one
        existingIds.push(suggestedId);
        const source = {
          ...sanitizedItem,
          id: suggestedId,
          providerKey: 'crossref',
          collectionKeys: [],
        };
        return source;
      });
      if (mounted) {
        setItemData(bibSources);
        setLoading(false);
      }
    };

    // Either do the search, or if the search is empty, clear the results
    if (searchTerm.length > 0) {
      performSearch();
    } else {
      setItemData([]);
    }

    return () => { mounted = false; };

  }, [searchTerm]);

  const doSearch = (search: string) => {
    setSearchTerm(search);
  };

  return (
    <CitationSourceLatentSearchPanel
      height={props.height}
      itemData={itemData}
      sourcesToAdd={props.sourcesToAdd}
      doSearch={doSearch}
      addSource={props.addSource}
      removeSource={props.removeSource}
      confirm={props.confirm}
      loading={loading}
      defaultText={props.ui.context.translateText('Enter terms to search Crossref')}
      placeholderText={props.ui.context.translateText('Search Crossref for Citations')}
      ui={props.ui}
    />
  );
};


