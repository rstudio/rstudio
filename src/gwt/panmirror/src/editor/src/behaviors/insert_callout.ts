/*
 * insert_tabset.ts
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

import { EditorState, Transaction } from "prosemirror-state";
import { EditorView } from "prosemirror-view";
import { setTextSelection } from "prosemirror-utils";

import { ExtensionContext, Extension } from "../api/extension";
import { kQuartoDocType } from "../api/format";
import { EditorCommandId, ProsemirrorCommand, toggleWrap } from "../api/command";
import { EditorUI } from "../api/ui";
import { OmniInsertGroup } from "../api/omni_insert";
import { createDiv } from "../api/div";
import { pandocAttrEnsureClass } from "../api/pandoc_attr";

const extension = (context: ExtensionContext): Extension | null => {
  const { pandocExtensions, format, ui } = context;

  if (!format.docTypes.includes(kQuartoDocType) || !pandocExtensions.fenced_divs) {
    return null;
  }

  return {
    commands: () => [
      new ProsemirrorCommand(EditorCommandId.Callout, [], insertCalloutCommandFn(ui), {
        name: ui.context.translateText('Callout'),
        description: ui.context.translateText('Content framed for special emphasis'),
        group: OmniInsertGroup.Content,
        priority: 2,
        image: () => ui.images.omni_insert?.generic!,
      }),
    ]
  };
};

function insertCalloutCommandFn(ui: EditorUI) {
  return (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
    
    const schema = state.schema;
    if (!toggleWrap(schema.nodes.div)(state)) {
      return false;
    }

    async function asyncInsertCallout() {
      if (dispatch) {
        // default 
        const props = {
          attr: {},
          callout: {
            type: "note",
            appearance: "default",
            icon: true,
            caption: ""
          }
        };
        await createDiv(ui, state, dispatch, props, (result, tr, div) => {
          // set div props from callout
          const attr = result.attr as any;
          pandocAttrEnsureClass(attr, `callout-${props.callout.type}`);
          attr.keyvalue = attr.keyvalue || [];
          if (result.callout?.appearance !== "default") {
            attr.keyvalue.push(["appearance", props.callout.appearance]);
          }
          if (result.callout?.icon === false) {
            attr.keyvalue.push(["icon", "false"]);
          }
          tr.setNodeMarkup(div.pos, div.node.type, attr);

          // insert caption if one is specified
          if (result.callout?.caption) {
            const calloutContent = [
              schema.nodes.heading.create(
                { level: 2 },
                schema.text(result.callout?.caption)
              ),
              schema.nodes.paragraph.create()
            ];
            tr.replaceWith(div.start, div.start + 1, calloutContent);
            setTextSelection(div.start + calloutContent[0].nodeSize)(tr);
          }
        });
        if (view) {
          view.focus();
        }
        
      }
    }
    asyncInsertCallout();

    return true;
  };
}

export default extension;


