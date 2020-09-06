/*
 * cite-commands.ts
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

import { EditorState, Transaction } from 'prosemirror-state';
import { toggleMark } from 'prosemirror-commands';
import { EditorView } from 'prosemirror-view';

import { setTextSelection } from 'prosemirror-utils';

import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { canInsertNode } from '../../api/node';
import { EditorUI } from '../../api/ui';
import { OmniInsertGroup } from '../../api/omni_insert';
import { EditorEvents } from '../../api/events';
import { EditorServer } from '../../api/server';
import { selectCitations } from '../../behaviors/insert_citation/insert_citation-dialog';
import { BibliographyManager } from '../../api/bibliography/bibliography';

import { ensureSourcesInBibliography } from './cite';
import { NavigationTreeNode } from '../../api/widgets/navigation-tree';

export class InsertCitationCommand extends ProsemirrorCommand {

  private selectedNode: NavigationTreeNode | undefined;

  constructor(ui: EditorUI, events: EditorEvents, bibliographyManager: BibliographyManager, server: EditorServer) {
    super(
      EditorCommandId.Citation,
      ['Shift-Mod-F8'],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {

        // enable/disable command
        const schema = state.schema;
        if (!canInsertNode(state, schema.nodes.text) || !toggleMark(schema.marks.cite)(state)) {
          return false;
        }

        if (dispatch && view) {
          selectCitations(ui, state.doc, bibliographyManager, server, this.selectedNode).then(async result => {
            if (result) {

              // Remember the last tree node that was selected
              this.selectedNode = result.selectedNode;

              // The citations that we should insert
              const citationEntries = result.citations;

              // Convert these to sources to be inserted
              // TODO: Need to potentially deal with progress here as this might be very expensive
              const sources = await Promise.all(citationEntries.map(entry => entry.toBibliographySource()));

              // The bibliography that we should insert sources into (if needed)
              const bibliography = result.bibliography;

              // The transaction that will hold all the changes we'll make
              const tr = state.tr;

              // First, be sure that we add any sources to the bibliography
              // and that the bibliography is properly configured
              await ensureSourcesInBibliography(
                tr,
                sources,
                bibliography,
                bibliographyManager,
                view,
                ui,
                server.pandoc,
              );

              // The starting location of this transaction
              const start = tr.selection.from;

              // Insert the cite mark and text
              const wrapperText = schema.text(`[]`, []);
              tr.insert(tr.selection.from, wrapperText);

              // move the selection into the wrapper
              setTextSelection(tr.selection.from - 1)(tr);

              // insert the CiteId marks and text
              citationEntries.forEach((citation, i) => {
                const citeIdMark = schema.marks.cite_id.create();
                const citeIdText = schema.text(`@${citation.id}`, [citeIdMark]);
                tr.insert(tr.selection.from, citeIdText);
                if (citationEntries.length > 1 && i !== citationEntries.length - 1) {
                  tr.insert(tr.selection.from, schema.text('; ', []));
                }
              });

              // Enclose wrapper in the cite mark
              const endOfWrapper = tr.selection.from + 1;
              const citeMark = schema.marks.cite.create();
              tr.addMark(start, endOfWrapper, citeMark);

              // Move selection to the end of the inserted content
              setTextSelection(endOfWrapper)(tr);

              // commit the transaction
              dispatch(tr);
            }
          });
        }
        return true;
      },
      {
        name: ui.context.translateText('Citation...'),
        description: ui.context.translateText('Reference to a source'),
        group: OmniInsertGroup.References,
        priority: 1,
        image: () => (ui.prefs.darkMode() ? ui.images.omni_insert!.citation_dark! : ui.images.omni_insert!.citation!),
      },
      // false
    );
  }
}