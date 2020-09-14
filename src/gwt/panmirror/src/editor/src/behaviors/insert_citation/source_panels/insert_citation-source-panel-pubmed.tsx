/*
 * insert_citation-source-panel-pubmed.tsx
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
import { createUniqueCiteId } from "../../../api/cite";
import { CSL } from "../../../api/csl";
import { NavigationTreeNode } from "../../../api/widgets/navigation-tree";
import { BibliographyManager } from "../../../api/bibliography/bibliography";
import { DOIServer } from "../../../api/doi";

import { PubMedServer, PubMedDocument, suggestCiteId, imageForType } from "../../../api/pubmed";

import { CitationSourcePanelProps, CitationSourcePanelProvider, CitationListEntry } from "./insert_citation-source-panel";
import { CitationSourceLatentSearchPanel } from "./insert_citation-source-panel-latent-search";


export function pubmedSourcePanel(ui: EditorUI,
  bibliographyManager: BibliographyManager,
  server: PubMedServer,
  doiServer: DOIServer): CitationSourcePanelProvider {

  const kPubmedType = 'Pubmed';
  return {
    key: 'EF556233-05B0-4678-8216-38061908463F',
    panel: PubmedSourcePanel,
    treeNode: () => {
      return {
        key: 'PubMed',
        name: ui.context.translateText('PubMed'),
        image: ui.images.citations?.pubmed,
        type: kPubmedType,
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
        const pubMedResult = await server.search(searchTerm);
        if (pubMedResult.status === 'ok') {
          if (pubMedResult.message === null) {
            // No results
            return Promise.resolve([]);
          } else {
            const docs: PubMedDocument[] = pubMedResult.message;
            const existingIds = bibliographyManager.localSources().map(src => src.id);
            const citationEntries = docs.map(doc => {
              const citationEntry = toCitationEntry(doc, existingIds, ui, doiServer);
              if (citationEntry) {
                // Add this id to the list of existing Ids so future ids will de-duplicate against this one
                existingIds.push(citationEntry.id);
              }
              return citationEntry;
            });
            return Promise.resolve(citationEntries);
          }
        } else {
          // TODO: Error
          return Promise.resolve([]);
        }
      } catch {
        // TODO: return citationentries or string (error)
        return Promise.resolve([]);
      }
    }
  };
}

export const PubmedSourcePanel = React.forwardRef<HTMLDivElement, CitationSourcePanelProps>((props: CitationSourcePanelProps, ref) => {
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
      searchPlaceholderText={props.ui.context.translateText('Search PubMed for Citations')}
      status={props.status}
      statusText={
        {
          placeholder: props.ui.context.translateText('Enter terms to search PubMed'),
          progress: props.ui.context.translateText('Searching PubMed...'),
          noResults: props.ui.context.translateText('No matching items'),
          error: props.ui.context.translateText('An error occurred while searching PubMed'),
        }
      }
      ui={props.ui}
      ref={ref}
    />
  );
});

function toCitationEntry(doc: PubMedDocument, existingIds: string[], ui: EditorUI, doiServer: DOIServer): CitationListEntry {

  const id = createUniqueCiteId(existingIds, suggestCiteId(doc));
  const providerKey = 'crossref';
  return {
    id,
    isIdEditable: true,
    title: doc.title || '',
    authors: (length: number) => {
      return formatAuthors(doc.authors || [], length);
    },
    type: '',
    date: doc.pubDate || '',
    journal: doc.source,
    image: imageForType(ui, doc.pubTypes)[0],
    doi: doc.doi,
    toBibliographySource: async (finalId: string) => {
      // Generate CSL using the DOI
      const doiResult = await doiServer.fetchCSL(doc.doi, -1);

      const csl = doiResult.message as CSL;
      return { ...csl, id: finalId, providerKey };
    },
    showProgress: true
  };
}

function formatAuthors(authors: string[], length: number) {
  return authors.join(',');
}

