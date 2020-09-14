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
import { createUniqueCiteId, imageForType } from "../../../api/cite";
import { CSL } from "../../../api/csl";
import { NavigationTreeNode } from "../../../api/widgets/navigation-tree";
import { BibliographyManager } from "../../../api/bibliography/bibliography";
import { DOIServer } from "../../../api/doi";

import { CitationSourcePanelProps, CitationSourcePanelProvider, CitationListEntry } from "./insert_citation-source-panel";
import { CitationSourceLatentSearchPanel } from "./insert_citation-source-panel-latent-search";
import { DataCiteServer, DataCiteRecord, suggestCiteId, DataCiteCreator } from "../../../api/datacite";


export function dataciteSourcePanel(ui: EditorUI,
  bibliographyManager: BibliographyManager,
  server: DataCiteServer,
  doiServer: DOIServer): CitationSourcePanelProvider {

  const kDataCiteType = 'Datacite';
  return {
    key: '66A6EADB-22AE-4DDD-BCD5-70BC0DEB8FB3',
    panel: DataCiteSourcePanel,
    treeNode: () => {
      return {
        key: 'DataCite',
        name: ui.context.translateText('DataCite'),
        image: ui.images.citations?.datacite,
        type: kDataCiteType,
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
        const dataciteResult = await server.search(searchTerm);
        if (dataciteResult.status === 'ok') {
          if (dataciteResult.message === null) {
            // No results
            return Promise.resolve([]);
          } else {
            const records: DataCiteRecord[] = dataciteResult.message;
            const existingIds = bibliographyManager.localSources().map(src => src.id);
            const citationEntries = records.map(record => {
              const citationEntry = toCitationEntry(record, existingIds, ui, doiServer);
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

export const DataCiteSourcePanel = React.forwardRef<HTMLDivElement, CitationSourcePanelProps>((props: CitationSourcePanelProps, ref) => {
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
      searchPlaceholderText={props.ui.context.translateText('Search DataCite for Citations')}
      status={props.status}
      statusText={
        {
          placeholder: props.ui.context.translateText('Enter terms to search DataCite'),
          progress: props.ui.context.translateText('Searching DataCite...'),
          noResults: props.ui.context.translateText('No matching items'),
          error: props.ui.context.translateText('An error occurred while searching DataCite'),
        }
      }
      ui={props.ui}
      ref={ref}
    />
  );
});

function toCitationEntry(record: DataCiteRecord, existingIds: string[], ui: EditorUI, doiServer: DOIServer): CitationListEntry {

  const id = createUniqueCiteId(existingIds, suggestCiteId(record));
  const providerKey = 'datacite';
  return {
    id,
    title: record.title || '',
    providerKey,
    authors: (length: number) => {
      return formatAuthors(record.creators || [], length);
    },
    type: '',
    date: record.publicationYear?.toString() || '',
    journal: record.publisher,
    image: imageForType(ui, record.type || '')[0],
    doi: record.doi,
    toBibliographySource: async (finalId: string) => {
      // Generate CSL using the DOI
      const doiResult = await doiServer.fetchCSL(record.doi, -1);

      const csl = doiResult.message as CSL;
      return { ...csl, id: finalId, providerKey };
    },
    showProgress: true
  };
}

function formatAuthors(authors: DataCiteCreator[], length: number) {
  return authors.map(creator => creator.fullName).join(',');
}

