/*
 * heading.ts
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

import { textblockTypeInputRule } from 'prosemirror-inputrules';
import { Node as ProsemirrorNode, Schema, NodeType, Fragment } from 'prosemirror-model';
import { EditorState } from 'prosemirror-state';
import { findParentNode } from 'prosemirror-utils';

import { PandocOutput, PandocToken, PandocTokenType, PandocExtensions } from '../api/pandoc';
import { BlockCommand, EditorCommandId } from '../api/command';
import { Extension } from '../api/extension';
import { pandocAttrSpec, pandocAttrParseDom, pandocAttrToDomAttr, pandocAttrReadAST } from '../api/pandoc_attr';
import { uuidv4 } from '../api/util';
import { PandocCapabilities } from '../api/pandoc_capabilities';
import { EditorUI } from '../api/ui';
import { EditorFormat } from '../api/format';

const HEADING_LEVEL = 0;
const HEADING_ATTR = 1;
const HEADING_CHILDREN = 2;

const kHeadingLevels = [1, 2, 3, 4, 5, 6];

const extension = (
  pandocExtensions: PandocExtensions,
  _caps: PandocCapabilities,
  _ui: EditorUI,
  format: EditorFormat,
): Extension => {
  const headingAttr = pandocExtensions.header_attributes || pandocExtensions.mmd_header_identifiers;

  return {
    nodes: [
      {
        name: 'heading',
        spec: {
          attrs: {
            level: { default: 1 },
            link: { default: null },
            navigation_id: { default: null },
            ...(headingAttr ? pandocAttrSpec : {}),
          },
          content: 'inline*',
          group: 'block',
          defining: true,
          parseDOM: [
            { tag: 'h1', getAttrs: getHeadingAttrs(1, headingAttr) },
            { tag: 'h2', getAttrs: getHeadingAttrs(2, headingAttr) },
            { tag: 'h3', getAttrs: getHeadingAttrs(3, headingAttr) },
            { tag: 'h4', getAttrs: getHeadingAttrs(4, headingAttr) },
            { tag: 'h5', getAttrs: getHeadingAttrs(5, headingAttr) },
            { tag: 'h6', getAttrs: getHeadingAttrs(6, headingAttr) },
          ],
          toDOM(node) {
            const attr = headingAttr ? pandocAttrToDomAttr(node.attrs) : {};
            attr.class = (attr.class || '').concat(' pm-heading');
            return [
              'h' + node.attrs.level,
              {
                'data-link': node.attrs.link,
                ...attr,
              },

              0,
            ];
          },
        },

        attr_edit: () => {
          if (headingAttr) {
            return {
              type: (schema: Schema) => schema.nodes.heading,
              offset: {
                top: 5,
                right: 5,
              },
            };
          } else {
            return null;
          }
        },

        pandoc: {
          readers: [
            {
              token: PandocTokenType.Header,
              block: 'heading',
              getAttrs: (tok: PandocToken) => ({
                level: tok.c[HEADING_LEVEL],
                navigation_id: uuidv4(),
                ...(headingAttr ? pandocAttrReadAST(tok, HEADING_ATTR) : {}),
              }),
              getChildren: (tok: PandocToken) => tok.c[HEADING_CHILDREN],
            },
          ],
          writer: (output: PandocOutput, node: ProsemirrorNode) => {
            output.writeToken(PandocTokenType.Header, () => {
              output.write(node.attrs.level);
              if (headingAttr) {
                output.writeAttr(node.attrs.id, node.attrs.classes, node.attrs.keyvalue);
              } else {
                output.writeAttr();
              }
              output.writeArray(() => {
                if (node.attrs.level === 1 && format.rmdExtensions.bookdownPart) {
                  writeBookdownH1(output, node);
                } else {
                  output.writeInlines(node.content);
                }
              });
            });
          },
        },
      },
    ],

    commands: (schema: Schema) => {
      return [
        new HeadingCommand(schema, EditorCommandId.Heading1, 1),
        new HeadingCommand(schema, EditorCommandId.Heading2, 2),
        new HeadingCommand(schema, EditorCommandId.Heading3, 3),
        new HeadingCommand(schema, EditorCommandId.Heading4, 4),
        new HeadingCommand(schema, EditorCommandId.Heading5, 5),
        new HeadingCommand(schema, EditorCommandId.Heading6, 6),
      ];
    },

    inputRules: (schema: Schema) => {
      return [
        textblockTypeInputRule(
          new RegExp('^(#{1,' + kHeadingLevels.length + '})\\s$'),
          schema.nodes.heading,
          match => ({
            level: match[1].length,
          }),
        ),
      ];
    },
  };
};

class HeadingCommand extends BlockCommand {
  public readonly nodeType: NodeType;
  public readonly level: number;

  constructor(schema: Schema, id: EditorCommandId, level: number) {
    super(id, ['Shift-Ctrl-' + level], schema.nodes.heading, schema.nodes.paragraph, { level });
    this.nodeType = schema.nodes.heading;
    this.level = level;
  }

  public isActive(state: EditorState) {
    const predicate = (n: ProsemirrorNode) => n.type === this.nodeType && n.attrs.level === this.level;
    const node = findParentNode(predicate)(state.selection);
    return !!node;
  }
}

// function for getting attrs
function getHeadingAttrs(level: number, pandocAttrSupported: boolean) {
  return (dom: Node | string) => {
    const el = dom as Element;
    return {
      level,
      'data-link': el.getAttribute('data-link'),
      ...(pandocAttrSupported ? pandocAttrParseDom(el, {}) : {}),
    };
  };
}

// write a bookdown (PART) H1 w/o spurious \
function writeBookdownH1(output: PandocOutput, node: ProsemirrorNode) {
  // see if this is a (PART\*). note we also match and replay any text
  // before the first ( in case the cursor sentinel ended up there
  const partMatch = node.textContent.match(/^([^()]*)\(PART\\\*\)/);
  if (partMatch) {
    const schema = node.type.schema;
    output.writeInlines(Fragment.from(schema.text(partMatch[1] + '(PART*)')));
    output.writeInlines(node.content.cut(partMatch[0].length));
  } else {
    output.writeInlines(node.content);
  }
}

export default extension;
