/*
 * cite-completion-bibliography.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import { BibliographySource, BibliographyManager } from '../../api/bibliography/bibliography';
import { Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorUI } from '../../api/ui';
import { imageForType } from '../../api/csl';
import { formatAuthors, formatIssuedDate } from '../../api/cite';
import { kZoteroProviderKey } from '../../api/bibliography/bibliography-provider_zotero';
import { EditorView } from 'prosemirror-view';
import { insertCitation as insertSingleCitation, performCiteCompletionReplacement } from './cite';
import { PandocServer } from '../../api/pandoc';
import { ReferenceEntry } from './cite-completion';


export interface CiteCompletionProvider {
  exactMatch: (searchTerm: string) => boolean;
  search: (searchTerm: string, maxCompletions: number) => ReferenceEntry[];
  currentEntries: () => ReferenceEntry[] | undefined;
  streamEntries: (doc: ProsemirrorNode, onStreamReady: (entries: ReferenceEntry[]) => void) => void;
  awaitEntries: (doc: ProsemirrorNode) => Promise<ReferenceEntry[]>;
}


export function bibliographyCiteCompletionProvider(ui: EditorUI, bibliographyManager: BibliographyManager): CiteCompletionProvider {
  return {
    exactMatch: (searchTerm: string) => {
      if (bibliographyManager.localSources().find(source => source.id === searchTerm)) {
        return true;
      } else {
        return false;
      }
    },
    search: (searchTerm: string, maxCompletions: number) => {
      const results = bibliographyManager.searchAllSources(searchTerm, maxCompletions).map(entry =>
        referenceEntryForSource(entry, ui, bibliographyManager),
      );
      return results;
    },
    currentEntries: () => {
      if (bibliographyManager.hasSources()) {
        return bibliographyManager.allSources().map(source => referenceEntryForSource(source, ui, bibliographyManager));
      } else {
        return undefined;
      }
    },
    streamEntries: (doc: ProsemirrorNode, onStreamReady: (entries: ReferenceEntry[]) => void) => {
      bibliographyManager.load(ui, doc).then(() => {
        onStreamReady(bibliographyManager.allSources().map(source => referenceEntryForSource(source, ui, bibliographyManager)));
      });
    },
    awaitEntries: async (doc: ProsemirrorNode) => {
      await bibliographyManager.load(ui, doc);
      return bibliographyManager.allSources().map(source => referenceEntryForSource(source, ui, bibliographyManager));
    }
  };
}

export function referenceEntryForSource(source: BibliographySource, ui: EditorUI, bibManager: BibliographyManager, forceLightMode?: boolean): ReferenceEntry {

  return {
    id: source.id,
    type: "citation",
    primaryText: source.id,
    secondaryText: (len: number) => {
      const authorStr = formatAuthors(source.author, len - source.id.length);
      const date = source.issued ? formatIssuedDate(source.issued) : '';
      const detail = `${authorStr} ${date}`;
      return detail;
    },
    detailText: source.title || source['short-title'] || source['container-title'] || source.type,
    image: imageForType(ui.images, source.type)[ui.prefs.darkMode() && !forceLightMode ? 1 : 0],
    imageAdornment: source.providerKey === kZoteroProviderKey ? ui.images.citations?.zoteroOverlay : undefined,
    replace: (view: EditorView, pos: number, server: PandocServer) => {
      if (source && bibManager.findIdInLocalBibliography(source.id)) {
        // It's already in the bibliography, just write the id
        const tr = view.state.tr;
        const schema = view.state.schema;
        const id = schema.text(source.id, [schema.marks.cite_id.create()]);
        performCiteCompletionReplacement(tr, pos, id);
        view.dispatch(tr);
        return Promise.resolve();
      } else if (source) {
        // It isn't in the bibliography, show the insert cite dialog
        return insertSingleCitation(
          view,
          source.DOI || '',
          bibManager,
          pos,
          ui,
          server,
          source,
          bibManager.providerName(source.providerKey),
        );
      } else {
        return Promise.resolve();
      }
    }
  };
}


