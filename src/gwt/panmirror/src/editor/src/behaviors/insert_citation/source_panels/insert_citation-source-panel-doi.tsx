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

import { EditorUI } from "../../../api/ui";

import { CSL } from "../../../api/csl";
import { BibliographyManager } from "../../../api/bibliography/bibliography";
import { DOIServer } from "../../../api/doi";
import { NavigationTreeNode } from "../../../api/widgets/navigation-tree";
import { suggestCiteId, formatAuthors, formatIssuedDate, imageForType } from "../../../api/cite";

import { CitationSourcePanelProps, CitationSourcePanelProvider, CitationListEntry } from "./insert_citation-source-panel";
import { CitationSourceLatentSearchPanel } from "./insert_citation-source-panel-latent-search";

import './insert_citation-source-panel-doi.css';

const kDOIType = 'DOI Search';

export function doiSourcePanel(ui: EditorUI, bibliographyManager: BibliographyManager, server: DOIServer): CitationSourcePanelProvider {
  return {
    key: '76561E2A-8FB7-4D4B-B235-9DD8B8270EA1',
    panel: DOISourcePanel,
    treeNode: () => {
      return {
        key: 'DOI',
        name: ui.context.translateText('From DOI'),
        image: ui.images.citations?.doi,
        type: kDOIType,
        children: [],
        expanded: true
      };
    },
    typeAheadSearch: (_searchTerm: string, _selectedNode: NavigationTreeNode) => {
      return null;
    },
    search: async (searchTerm: string, _selectedNode: NavigationTreeNode) => {
      // TODO: Error handling (try / catch)
      try {
        const result = await server.fetchCSL(searchTerm, 1000);
        if (result.status === 'ok') {
          // Form the entry
          const csl = result.message;
          const citation = toCitationEntry(csl, bibliographyManager, ui);
          return Promise.resolve(citation ? [citation] : null);
        } else {
          return Promise.resolve(null);
        }
      } catch {
        // TODO: return citationentries or string (error)
        return Promise.resolve(null);
      }
    }

  };
}

export const DOISourcePanel = React.forwardRef<HTMLDivElement, CitationSourcePanelProps>((props: CitationSourcePanelProps, ref) => {

  // Track whether we are mounted to allow a latent search that returns after the 
  // component unmounts to nmot mutate state further
  return (
    <CitationSourceLatentSearchPanel
      height={props.height}
      citations={props.citations}
      citationsToAdd={props.citationsToAdd}
      searchTerm={props.searchTerm}
      onSearchTermChanged={props.onSearchTermChanged}
      executeSearch={props.onExecuteSearch}
      onAddCitation={props.onAddCitation}
      onRemoveCitation={props.onRemoveCitation}
      selectedIndex={props.selectedIndex}
      onSelectedIndexChanged={props.onSelectedIndexChanged}
      onConfirm={props.onConfirm}
      searchPlaceholderText={props.ui.context.translateText('Paste a DOI to search')}
      status={props.status}
      statusText={
        {
          placeholder: props.ui.context.translateText('Paste a DOI to load data from Crossref, DataCite, or mEDRA.'),
          progress: props.ui.context.translateText('Fetching data for DOI...'),
          noResults: props.ui.context.translateText('Sorry, data for that DOI couldn\'t be found'),
          error: props.ui.context.translateText('An error occurred while searching for this DOI'),
        }
      }
      ui={props.ui}
      ref={ref}
    />
  );
});



function toCitationEntry(csl: CSL | undefined, bibliographyManager: BibliographyManager, ui: EditorUI): CitationListEntry | undefined {
  if (csl) {
    const suggestedId = suggestCiteId(bibliographyManager.localSources().map(source => source.id), csl);
    const providerKey = 'doi';
    return {
      id: suggestedId,
      type: csl.type,
      title: csl.title || '',
      providerKey,
      authors: (length: number) => {
        return formatAuthors(csl.author, length);
      },
      date: formatIssuedDate(csl.issued),
      journal: csl["container-title"] || csl["short-container-title"] || csl.publisher,
      doi: csl.DOI,
      image: imageForType(ui, csl.type)[0],
      toBibliographySource: (finalId: string) => {
        return Promise.resolve({ ...csl, id: finalId, providerKey });
      },
      showProgress: false
    };
  }
  return undefined;
}