/*
 * format.ts
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

import { Mark, Fragment, Schema } from 'prosemirror-model';

import { Extension } from '../api/extension';
import { PandocOutput, PandocExtensions, ProsemirrorWriter } from '../api/pandoc';
import { PandocCapabilities } from '../api/pandoc_capabilities';
import { EditorUI } from '../api/ui';
import { EditorFormat } from '../api/format';
import { EditorOptions } from '../api/options';
import { matchPandocFormatComment } from '../api/pandoc_format';

const extension = (
  pandocExtensions: PandocExtensions,
  _caps: PandocCapabilities,
  _ui: EditorUI,
  _format: EditorFormat,
  options: EditorOptions,
): Extension | null => {
  if (!options.hideFormatComment) {
    return null;
  }

  return {
    marks: [
      {
        name: 'format_comment',
        spec: {
          parseDOM: [{ tag: "span[class*='format-comment']" }],
          toDOM() {
            return ['span', { class: 'format-comment', style: 'display: none;' }];
          },
        },
        pandoc: {
          readers: [],
          inlineHTMLReader: (schema: Schema, html: string, writer: ProsemirrorWriter) => {
            if (matchPandocFormatComment(html)) {
              const mark = schema.marks.format_comment.create();
              writer.openMark(mark);
              writer.writeText(html);
              writer.closeMark(mark);
              return true;
            } else {
              return false;
            }
          },
          writer: {
            priority: 20,
            write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
              output.writeRawMarkdown(parent);
            },
          },
        },
      },
    ],
  };
};

export default extension;
