/*
 * table-contextmenu.ts
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

import { Plugin, PluginKey, Transaction, EditorState } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { Schema } from 'prosemirror-model';
import { isInTable } from 'prosemirror-tables';

import { EditorUI } from '../../api/ui';
import { EditorCommandId } from '../../api/command';

export class TableContextMenuPlugin extends Plugin {
  private menuVisible = false;

  constructor(_schema: Schema, ui: EditorUI) {
    super({
      key: new PluginKey('table-contextmenu'),

      props: {
        handleDOMEvents: {
          contextmenu: (view: EditorView, event: Event) => {
            // only trigger when in table
            if (!isInTable(view.state)) {
              return false;
            }

            const asyncShowTableContextMenu = async () => {
              const menu = [
                { command: EditorCommandId.TableAddRowBefore },
                { command: EditorCommandId.TableAddRowAfter },
                { separator: true },
                { command: EditorCommandId.TableAddColumnBefore },
                { command: EditorCommandId.TableAddColumnAfter },
                { separator: true },
                { command: EditorCommandId.TableDeleteRow },
                { command: EditorCommandId.TableDeleteColumn },
                { separator: true },
                { command: EditorCommandId.TableDeleteTable },
                { separator: true },
                {
                  text: ui.context.translateText('Align Column'),
                  subMenu: {
                    items: [
                      { command: EditorCommandId.TableAlignColumnLeft },
                      { command: EditorCommandId.TableAlignColumnCenter },
                      { command: EditorCommandId.TableAlignColumnRight },
                      { separator: true },
                      { command: EditorCommandId.TableAlignColumnDefault },
                    ],
                  },
                },
                { separator: true },
                { command: EditorCommandId.TableToggleHeader },
                { command: EditorCommandId.TableToggleCaption },
              ];

              const { clientX, clientY } = event as MouseEvent;
              await ui.display.showContextMenu!(menu, clientX, clientY);
              this.menuVisible = false;
            };

            if (ui.display.showContextMenu) {
              this.menuVisible = true;
              asyncShowTableContextMenu();
              event.stopPropagation();
              event.preventDefault();
              return true;
            } else {
              return false;
            }
          },
        },
      },

      // prevent selection while the context menu is visible (the right-click
      // that invokes the context menu ends up resetting the selection, which
      // makes the selection-based commands behave incorrectly when multiple
      // rows or columns are selected)
      filterTransaction: (tr: Transaction, state: EditorState) => {
        if (this.menuVisible && isInTable(state)) {
          return !(tr.selectionSet && !tr.docChanged && !tr.storedMarksSet);
        } else {
          return true;
        }
      },
    });
  }
}
