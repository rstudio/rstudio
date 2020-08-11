/*
 * code.ts
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

import { Plugin, PluginKey } from 'prosemirror-state';

import { CodeViewOptions, canInsertNodeAtPos } from "./node";
import { ResolvedPos, Slice, Fragment } from 'prosemirror-model';

export const kCodeAttr = 0;
export const kCodeText = 1;

export function codeNodeSpec() {
  return {
    content: 'text*',
    group: 'block',
    marks: '',
    code: true,
    defining: true,
    isolating: true,
  };
}


export function codeViewClipboardPlugin(codeViews: { [key: string]: CodeViewOptions }) {
  return new Plugin({
    key: new PluginKey('code-view-clipboard'),
    props: {
      clipboardTextParser: (text: string, $context: ResolvedPos): any => {
        // see if any of the code views want to handle this text
        for (const codeViewType of Object.keys(codeViews)) {
          const codeView = codeViews[codeViewType];
          if (codeView.createFromPastePattern && codeView.createFromPastePattern.test(text)) {
            const schema = $context.node().type.schema;
            const nodeType = schema.nodes[codeViewType];
            if (canInsertNodeAtPos($context, nodeType)) {
              const textNode = schema.text(text);
              const codeNode = nodeType.createAndFill({}, textNode);
              return new Slice(Fragment.from(codeNode), 0, 0);
            }
          }
        }
        return null;
      },
    },
  });
}