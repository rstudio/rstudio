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


import { BibliographyManager } from "../../../api/bibliography/bibliography";
import { suggestCiteId, formatAuthors, formatIssuedDate } from "../../../api/cite";
import { CrossrefWork, imageForCrossrefType, CrossrefServer, prettyType } from "../../../api/crossref";
import { sanitizeForCiteproc, CSL } from "../../../api/csl";
import { DOIServer } from "../../../api/doi";
import { logException } from "../../../api/log";
import { NavigationTreeNode } from "../../../api/widgets/navigation-tree";

import { EditorUI } from "../../../api/ui";

import { CitationSourcePanelProps, CitationSourcePanelProvider, CitationListEntry, CitationSourceListStatus } from "./insert_citation-source-panel";
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
    typeAheadSearch: (_searchTerm: string, _selectedNode: NavigationTreeNode, _existingCitationIds: string[]) => {
      return null;
    },
    progressMessage: ui.context.translateText('Searching Crossref....'),
    placeHolderMessage: ui.context.translateText('Enter search terms to search Crossref'),
    search: async (searchTerm: string, _selectedNode: NavigationTreeNode, existingCitationIds: string[]) => {
      try {
        const works = await server.works(searchTerm);
        const dedupeCitationIds = existingCitationIds;
        const citationEntries = works.items.map(work => {
          const citationEntry = toCitationListEntry(work, dedupeCitationIds, ui, doiServer);
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

export const CrossRefSourcePanel = React.forwardRef<HTMLDivElement, CitationSourcePanelProps>((props: CitationSourcePanelProps, ref) => {
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
      statusMessage={props.statusMessage}
      ui={props.ui}
      ref={ref}
    />
  );
});

function toCitationListEntry(crossrefWork: CrossrefWork, existingIds: string[], ui: EditorUI, doiServer: DOIServer): CitationListEntry {

  const coercedCSL = sanitizeForCiteproc(crossrefWork as unknown as CSL);
  const id = suggestCiteId(existingIds, coercedCSL);
  const providerKey = 'crossref';
  return {
    id,
    isIdEditable: true,
    title: crossrefWorkTitle(crossrefWork, ui),
    type: prettyType(ui, crossrefWork.type),
    date: formatIssuedDate(crossrefWork.issued),
    journal: crossrefWork["container-title"] || crossrefWork["short-container-title"] || crossrefWork.publisher,
    image: imageForCrossrefType(ui, crossrefWork.type)[0],
    doi: crossrefWork.DOI,
    authors: (length: number) => {
      return formatAuthors(coercedCSL.author, length);
    },
    toBibliographySource: async (finalId: string) => {
      // Generate CSL using the DOI
      const doiResult = await doiServer.fetchCSL(crossrefWork.DOI, -1);

      const csl = doiResult.message as CSL;
      return { ...csl, id: finalId, providerKey };
    },
    isSlowGeneratingBibliographySource: true
  };
}


function crossrefWorkTitle(work: CrossrefWork, ui: EditorUI) {
  if (work.title) {
    return work.title[0];
  } else {
    return ui.context.translateText('(Untitled)');
  }
}
