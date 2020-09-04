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

import { CitationSourcePanelProps, CitationSourcePanel, CitationListEntry } from "../insert_citation-panel";
import { CitationSourceLatentSearchPanel } from "./insert_citation-source-panel-latent-search";
import { CrossrefWork, imageForCrossrefType } from "../../../api/crossref";
import { CitationSourceListStatus } from "./insert_citation-source-panel-list";

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
  const [citations, setCitations] = React.useState<CitationListEntry[]>([]);
  const [searchTerm, setSearchTerm] = React.useState<string>('');
  const [status, setStatus] = React.useState<CitationSourceListStatus>(CitationSourceListStatus.default);

  React.useEffect(() => {
    let mounted = true;
    const performSearch = async () => {
      if (mounted) {
        setStatus(CitationSourceListStatus.loading);
      }
      const works = await props.server.crossref.works(searchTerm);

      // Get the list of ids already in the bibliography
      const existingIds = props.bibliographyManager.allSources().map(src => src.id);

      const citationEntries = works.items.map(work => {
        const citationEntry = toCitationEntry(work, existingIds, props.ui);
        if (citationEntry) {
          // Add this id to the list of existing Ids so future ids will de-duplicate against this one
          existingIds.push(citationEntry.id);
        }
        return citationEntry;
      });

      if (mounted) {
        setCitations(citationEntries);
        setStatus(citationEntries.length > 0 ? CitationSourceListStatus.default : CitationSourceListStatus.noResults);
      }
    };

    // Either do the search, or if the search is empty, clear the results
    if (searchTerm.length > 0) {
      performSearch();
    } else {
      setCitations([]);
    }

    return () => { mounted = false; };

  }, [searchTerm]);

  const doSearch = (search: string) => {
    setSearchTerm(search);
  };

  return (
    <CitationSourceLatentSearchPanel
      height={props.height}
      citations={citations}
      citationsToAdd={props.citationsToAdd}
      addCitation={props.addCitation}
      removeCitation={props.removeCitation}
      doSearch={doSearch}
      confirm={props.confirm}
      status={status}
      defaultText={props.ui.context.translateText('Enter terms to search Crossref')}
      placeholderText={props.ui.context.translateText('Search Crossref for Citations')}
      ui={props.ui}
    />
  );
};

function toCitationEntry(crossrefWork: CrossrefWork, existingIds: string[], ui: EditorUI): CitationListEntry {

  // TODO: need to implement a better toBibliographySource
  const coercedCSL = sanitizeForCiteproc(crossrefWork as unknown as CSL);
  const id = suggestCiteId(existingIds, coercedCSL);
  const providerKey = 'crossref';
  return {
    id,
    title: crossrefWork.title[0] || '',
    providerKey,
    authors: (length: number) => {
      return formatAuthors(coercedCSL.author, length);
    },
    date: formatIssuedDate(crossrefWork.issued),
    journal: '',
    image: imageForCrossrefType(ui, crossrefWork.type)[0],
    toBibliographySource: () => {
      return Promise.resolve({ id, providerKey, ...coercedCSL });
    }
  };
}

