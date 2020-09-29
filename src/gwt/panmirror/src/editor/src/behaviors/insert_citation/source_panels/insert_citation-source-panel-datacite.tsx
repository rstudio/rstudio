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

import { BibliographyManager } from "../../../api/bibliography/bibliography";
import { createUniqueCiteId } from "../../../api/cite";
import { CSL, imageForType } from "../../../api/csl";
import { DataCiteServer, DataCiteRecord, suggestCiteId, DataCiteCreator } from "../../../api/datacite";
import { DOIServer } from "../../../api/doi";
import { NavigationTreeNode } from "../../../api/widgets/navigation-tree";
import { logException } from "../../../api/log";
import { EditorUI } from "../../../api/ui";

import { CitationSourcePanelProps, CitationSourcePanelProvider, CitationListEntry, CitationSourceListStatus, errorForStatus } from "./insert_citation-source-panel";
import { CitationSourceLatentSearchPanel } from "./insert_citation-source-panel-latent-search";

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
    typeAheadSearch: (_searchTerm: string, _selectedNode: NavigationTreeNode, _existingCitationIds: string[]) => {
      return null;
    },
    progressMessage: ui.context.translateText('Searching DataCite....'),
    placeHolderMessage: ui.context.translateText('Enter search terms to search DataCite'),
    search: async (searchTerm: string, _selectedNode: NavigationTreeNode, existingCitationIds: string[]) => {
      try {
        const dataciteResult = await server.search(searchTerm);
        switch (dataciteResult.status) {
          case 'ok':
            if (dataciteResult.message !== null) {
              const records: DataCiteRecord[] = dataciteResult.message;
              const dedupeCitationIds = existingCitationIds;
              const citationEntries = records.map(record => {
                const citationEntry = toCitationListEntry(record, dedupeCitationIds, ui, doiServer);
                if (citationEntry) {
                  // Add this id to the list of existing Ids so future ids will de-duplicate against this one
                  dedupeCitationIds.push(citationEntry.id);
                }
                return citationEntry;
              });
              return Promise.resolve({
                citations: citationEntries,
                status: CitationSourceListStatus.default,
                statusMessage: ''
              });
            } else {
              // No results
              return Promise.resolve({
                citations: [],
                status: CitationSourceListStatus.default,
                statusMessage: ''
              });
            }
          default:
            // Resolve with Error
            return Promise.resolve({
              citations: [],
              status: CitationSourceListStatus.error,
              statusMessage: ui.context.translateText(errorForStatus(ui, dataciteResult.status, 'DataCite'))
            });
        }
      } catch (e) {
        logException(e);
        return Promise.resolve({
          citations: [],
          status: CitationSourceListStatus.error,
          statusMessage: ui.context.translateText('An unknown error occurred. Please try again.')
        });
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
      statusMessage={props.statusMessage}
      ui={props.ui}
      ref={ref}
    />
  );
});

function toCitationListEntry(record: DataCiteRecord, existingIds: string[], ui: EditorUI, doiServer: DOIServer): CitationListEntry {

  const id = createUniqueCiteId(existingIds, suggestCiteId(record));
  const providerKey = 'datacite';
  return {
    id,
    isIdEditable: true,
    title: record.title || '',
    type: record.type || '',
    date: record.publicationYear?.toString() || '',
    journal: record.publisher,
    image: imageForType(ui.images, record.type || '')[0],
    doi: record.doi,
    authors: (length: number) => {
      return formatAuthors(record.creators || [], length);
    },
    toBibliographySource: async (finalId: string) => {
      // Generate CSL using the DOI
      const doiResult = await doiServer.fetchCSL(record.doi, -1);

      const csl = doiResult.message as CSL;
      return { ...csl, id: finalId, providerKey };
    },
    isSlowGeneratingBibliographySource: true
  };
}

function formatAuthors(authors: DataCiteCreator[], length: number) {
  return authors.map(creator => creator.fullName).join(',');
}

