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
import { suggestCiteId, formatAuthors, formatIssuedDate } from "../../../api/cite";
import { sanitizeForCiteproc, CSL } from "../../../api/csl";
import { NavigationTreeNode } from "../../../api/widgets/navigation-tree";
import { BibliographyManager } from "../../../api/bibliography/bibliography";
import { CrossrefWork, imageForCrossrefType, CrossrefServer, prettyType } from "../../../api/crossref";
import { DOIServer } from "../../../api/doi";

import { CitationSourcePanelProps, CitationSourcePanelProvider, CitationListEntry } from "./insert_citation-source-panel";
import { CitationSourceLatentSearchPanel } from "./insert_citation-source-panel-latent-search";

export function crossrefSourcePanel(ui: EditorUI,
  bibliographyManager: BibliographyManager,
  server: CrossrefServer,
  doiServer: DOIServer): CitationSourcePanelProvider {

  const kCrossrefType = 'Crossref';
  return {
    key: 'E38370AA-78AE-450B-BBE8-878E1C817C04',
    panel: CrossRefSourcePanel,
    treeNode: () => {
      return {
        key: 'CrossRef',
        name: ui.context.translateText('Crossref'),
        image: ui.images.citations?.crossref,
        type: kCrossrefType,
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
        const works = await server.works(searchTerm);
        const existingIds = bibliographyManager.localSources().map(src => src.id);
        const citationEntries = works.items.map(work => {
          const citationEntry = toCitationEntry(work, existingIds, ui, doiServer);
          if (citationEntry) {
            // Add this id to the list of existing Ids so future ids will de-duplicate against this one
            existingIds.push(citationEntry.id);
          }
          return citationEntry;
        });

        return Promise.resolve(citationEntries);
      } catch {
        // TODO: return citationentries or string (error)
        return Promise.resolve([]);
      }
    }
  };
}

export const CrossRefSourcePanel = React.forwardRef<HTMLDivElement, CitationSourcePanelProps>((props: CitationSourcePanelProps, ref) => {

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
      searchPlaceholderText={props.ui.context.translateText('Search Crossref for Citations')}
      status={props.status}
      statusText={
        {
          placeholder: props.ui.context.translateText('Enter terms to search Crossref'),
          progress: props.ui.context.translateText('Searching Crossref...'),
          noResults: props.ui.context.translateText('No matching items'),
          error: props.ui.context.translateText('An error occurred while searching Crossref'),
        }
      }
      ui={props.ui}
      ref={ref}
    />
  );
});

function toCitationEntry(crossrefWork: CrossrefWork, existingIds: string[], ui: EditorUI, doiServer: DOIServer): CitationListEntry {

  const coercedCSL = sanitizeForCiteproc(crossrefWork as unknown as CSL);
  const id = suggestCiteId(existingIds, coercedCSL);
  const providerKey = 'crossref';
  return {
    id,
    title: crossrefWorkTitle(crossrefWork, ui),
    providerKey,
    authors: (length: number) => {
      return formatAuthors(coercedCSL.author, length);
    },
    type: prettyType(ui, crossrefWork.type),
    date: formatIssuedDate(crossrefWork.issued),
    journal: crossrefWork["container-title"] || crossrefWork["short-container-title"] || crossrefWork.publisher,
    image: imageForCrossrefType(ui, crossrefWork.type)[0],
    doi: crossrefWork.DOI,
    toBibliographySource: async () => {

      // Generate CSL using the DOI
      return doiServer.fetchCSL(crossrefWork.DOI, 1000).then(doiResult => {
        const csl = doiResult.message as CSL;
        return { id, providerKey, ...csl };
      });
    }
  };
}


function crossrefWorkTitle(work: CrossrefWork, ui: EditorUI) {
  if (work.title) {
    return work.title[0];
  } else {
    return ui.context.translateText('(Untitled)');
  }
}
