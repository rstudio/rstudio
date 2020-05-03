/*
 * table-contextmenu.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

import { Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { EditorUI } from '../../api/ui';
import { EditorCommandId } from '../../api/command';
import { findParentNodeOfType } from 'prosemirror-utils';
import { Schema } from 'prosemirror-model';

export function tableContextMenu(schema: Schema, ui: EditorUI) {
  return new Plugin({
    key: new PluginKey('table-contextmenu'),
    props: {
      
      handleDOMEvents: {
        contextmenu: (view: EditorView, event: Event) => {
          
          // only trigger when in table
          if (!findParentNodeOfType(schema.nodes.table)(view.state.selection)) {
            return false;
          }

          if (ui.display.showContextMenu) {

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
              { subMenu: {
                text: ui.context.translateText('Align Column'),
                items: [
                  { command: EditorCommandId.TableAlignColumnLeft },
                  { command: EditorCommandId.TableAlignColumnCenter },
                  { command: EditorCommandId.TableAlignColumnRight },
                  { separator: true },
                  { command: EditorCommandId.TableAlignColumnDefault }
                ]
              }},
              { separator: true },
              { command: EditorCommandId.TableToggleHeader },
              { command: EditorCommandId.TableToggleCaption },
            ];

            const { clientX, clientY } = event as MouseEvent;
            ui.display.showContextMenu(menu, clientX, clientY);

            event.stopPropagation();
            event.preventDefault();
            return true;
          } else {
            return false;
          }
        }
      },
    },
  });
}


