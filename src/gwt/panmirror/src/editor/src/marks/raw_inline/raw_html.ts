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
import { PandocCapabilities } from '../../api/pandoc_capabilities';

import { kRawInlineFormat, kRawInlineContent, RawInlineCommand } from './raw_inline';

import { InsertHTMLCommentCommand } from './raw_html_comment';

const extension = (pandocExtensions: PandocExtensions, pandocCapabilities: PandocCapabilities): Extension | null => {
  return {
    marks: [
      {
        name: 'raw_html',
        noInputRules: true,
        spec: {
          inclusive: false,
          excludes: '_',
          parseDOM: [
            {
              tag: "span[class*='raw-html']",
              getAttrs(dom: Node | string) {
                return {};
              },
            },
          ],
          toDOM(mark: Mark) {
            const attr: any = {
              class: 'raw-html pm-fixedwidth-font pm-markup-text-color',
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
      const commands = [new InsertHTMLCommentCommand(schema)];
      if (pandocExtensions.raw_html) {
        commands.push(
          new RawInlineCommand(EditorCommandId.HTMLInline, kHTMLFormat, ui, pandocCapabilities.output_formats),
        );
      }
      return commands;
    },
  };
};

export default extension;
