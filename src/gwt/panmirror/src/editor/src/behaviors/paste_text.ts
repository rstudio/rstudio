/*
 * plain_text_paste.ts
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

import { ResolvedPos, Schema, Fragment, Slice } from 'prosemirror-model';
import { Plugin, PluginKey } from 'prosemirror-state';

import { Extension } from '../api/extension';

const extension: Extension = {
  plugins: (schema: Schema) => [pasteTextPlugin(schema)],
};

function pasteTextPlugin(schema: Schema) {
  return new Plugin({
    key: new PluginKey('paste-text'),

    props: {
      clipboardTextParser: (text: string, $context: ResolvedPos): any => {
        // if it's a single line then create a slice w/ marks from the context active
        if (text.indexOf('\n') === -1) {
          const marks = $context.marks();
          const textNode = schema.text(text, marks);
          return new Slice(Fragment.from(textNode), 0, 0);
        } else {
          return null;
        }
      },
    },
  });
}

export default extension;
