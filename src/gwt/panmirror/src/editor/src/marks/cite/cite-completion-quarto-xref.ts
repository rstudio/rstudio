/*
 * cite-completion-quarto-xref.ts
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
import { Node as ProsemirrorNode } from 'prosemirror-model';
import { EditorView } from 'prosemirror-view';

import { EditorUI } from '../../api/ui';

import { performCiteCompletionReplacement } from './cite';
import { CiteCompletionEntry, CiteCompletionProvider } from './cite-completion';
import { EditorServer } from '../../api/server';
import { XRef } from '../../api/xref';
import { kXRefTypes } from '../xref/xref-completion';

export function quartoXrefCiteCompletionProvider(ui: EditorUI, server: EditorServer): CiteCompletionProvider {
  const referenceEntryForXref = (xref: XRef, forceLightMode?: boolean): CiteCompletionEntry => {

    const type = kXRefTypes[xref.type];
    const image = type?.image(ui) || ui.images.omni_insert?.generic!;

    return {
      id: xref.id,
      type: "citation",
      primaryText: xref.id,
      secondaryText: (len: number) => {
        return xref.file;
      },
      detailText: xref.title || "",
      image,
      imageAdornment: undefined,
      replace: (view: EditorView, pos: number, _server: EditorServer) => {
        // It's already in the bibliography, just write the id
        const tr = view.state.tr;
        const schema = view.state.schema;
        const id = schema.text(xref.id, [schema.marks.cite_id.create()]);
        performCiteCompletionReplacement(tr, pos, id);
        view.dispatch(tr);
        return Promise.resolve();
      }
    };
  };

  let loadedEntries: CiteCompletionEntry[] | undefined;
  return {
    exactMatch: (searchTerm: string) => {
      return loadedEntries?.some(entry => entry.id === searchTerm) || false;
    },
    search: (searchTerm: string, maxCompletions: number) => {
      console.log(`Search: ${searchTerm}`);
      console.log(loadedEntries);
      return loadedEntries?.filter(entry => entry.id.startsWith(searchTerm)) || [];
    },
    currentEntries: () => {
      return loadedEntries;
    },
    streamEntries: (doc: ProsemirrorNode, onStreamReady: (entries: CiteCompletionEntry[]) => void) => {
      const docPath = ui.context.getDocumentPath();
      if (docPath) {
        console.log(docPath);
        server.xref.quartoIndexForFile(docPath).then(xrefs => {
          loadedEntries = xrefs.refs.map(ref => referenceEntryForXref(ref));
          console.log("STREAM");
          console.log(loadedEntries);
          onStreamReady(loadedEntries);
        });
      }
    },
    awaitEntries: async (doc: ProsemirrorNode) => {
      const docPath = ui.context.getDocumentPath();
      if (docPath) {
        const index = await server.xref.quartoIndexForFile(docPath);
        loadedEntries = index.refs.map(ref => referenceEntryForXref(ref));
        console.log("AWAIT");
        console.log(loadedEntries);

        return loadedEntries;
      } else {
        return Promise.resolve([]);
      }
    },
    warningMessage: () => {
      return undefined;
    }

  };
}


