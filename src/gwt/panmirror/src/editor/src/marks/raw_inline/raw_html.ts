/*
 * raw_html.ts
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

import { Mark, Schema, Fragment } from 'prosemirror-model';

import { PandocExtensions, PandocTokenType, PandocToken, ProsemirrorWriter, PandocOutput } from '../../api/pandoc';
import { Extension } from '../../api/extension';
import { isRawHTMLFormat, kHTMLFormat } from '../../api/raw';
import { EditorUI } from '../../api/ui';
import { EditorCommandId } from '../../api/command';

import { kRawInlineFormat, kRawInlineContent, RawInlineCommand } from './raw_inline';
import { PandocCapabilities } from '../../api/pandoc_capabilities';

const extension = (pandocExtensions: PandocExtensions, pandocCapabilities: PandocCapabilities): Extension | null => {
  if (!pandocExtensions.raw_html) {
    return null;
  }

  return {
    marks: [
      {
        name: 'raw_html',
        noInputRules: true,
        spec: {
          inclusive: false,
          excludes: '_',
          attrs: {
            comment: { default: false },
          },
          parseDOM: [
            {
              tag: "span[class*='raw-html']",
              getAttrs(dom: Node | string) {
                const el = dom as Element;
                return {
                  comment: el.getAttribute('data-comment') === '1',
                };
              },
            },
          ],
          toDOM(mark: Mark) {
            const attr: any = {
              class:
                'raw-html pm-fixedwidth-font ' + (mark.attrs.comment ? 'pm-light-text-color' : 'pm-markup-text-color'),
              'data-comment': mark.attrs.comment ? '1' : '0',
            };
            return ['span', attr];
          },
        },
        pandoc: {
          readers: [
            {
              token: PandocTokenType.RawInline,
              match: (tok: PandocToken) => {
                const format = tok.c[kRawInlineFormat];
                return isRawHTMLFormat(format);
              },
              handler: (_schema: Schema) => {
                return (writer: ProsemirrorWriter, tok: PandocToken) => {
                  const text = tok.c[kRawInlineContent];
                  writer.writeInlineHTML(text);
                };
              },
            },
          ],
          writer: {
            priority: 20,
            write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
              output.writeRawMarkdown(parent);
            },
          },
        },
      },
    ],

    // insert command
    commands: (schema: Schema, ui: EditorUI) => {
      return [new RawInlineCommand(EditorCommandId.HTMLInline, kHTMLFormat, ui, pandocCapabilities.output_formats)];
    },
  };
};

export default extension;
