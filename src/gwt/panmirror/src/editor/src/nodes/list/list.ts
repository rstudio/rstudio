/*
 * list.ts
 *
 * Copyright (C) 2019-20 by RStudio, Inc.
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

import { wrappingInputRule } from 'prosemirror-inputrules';
import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { liftListItem, sinkListItem, splitListItem } from 'prosemirror-schema-list';
import { Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { Extension } from '../../api/extension';
import { BaseKey } from '../../api/basekeys';
import { EditorUI } from '../../api/ui';
import { ListCapabilities } from '../../api/list';
import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { PandocTokenType, PandocExtensions } from '../../api/pandoc';

import { ListCommand, TightListCommand, OrderedListEditCommand } from './list-commands';

import {
  CheckedListItemNodeView,
  checkedListItemInputRule,
  checkedListInputRule,
  CheckedListItemCommand,
  CheckedListItemToggleCommand,
  checkedListItemMarkdownOutputFilter,
} from './list-checked';

import {
  writePandocBulletList,
  writePandocOrderedList,
  readPandocList,
  exampleListPandocMarkdownOutputFilter,
} from './list-pandoc';

import './list-styles.css';

export enum ListNumberStyle {
  DefaultStyle = 'DefaultStyle',
  Decimal = 'Decimal',
  LowerRoman = 'LowerRoman',
  UpperRoman = 'UpperRoman',
  LowerAlpha = 'LowerAlpha',
  UpperAlpha = 'UpperAlpha',
  Example = 'Example',
}

// NOTE: HTML output doesn't currently respect this and it's difficult to
// do with CSS (especially for nested lists). So we allow the user to edit
// it but it isn't reflected in the editor.
export enum ListNumberDelim {
  DefaultDelim = 'DefaultDelim',
  Period = 'Period',
  OneParen = 'OneParen',
  TwoParens = 'TwoParens',
}

const plugin = new PluginKey('list');

const extension = (pandocExtensions: PandocExtensions): Extension => {
  // determine list capabilities based on active format options
  const capabilities: ListCapabilities = {
    tasks: pandocExtensions.task_lists,
    fancy: pandocExtensions.fancy_lists,
    example: pandocExtensions.fancy_lists && pandocExtensions.example_lists,
    order: pandocExtensions.startnum,
  };

  return {
    nodes: [
      {
        name: 'list_item',
        spec: {
          content: 'paragraph block*',
          attrs: {
            checked: { default: null },
          },
          defining: true,
          parseDOM: [
            {
              tag: 'li',
              getAttrs: (dom: Node | string) => {
                const el = dom as Element;
                const attrs: any = {};
                if (capabilities.tasks && el.hasAttribute('data-checked')) {
                  attrs.checked = el.getAttribute('data-checked') === 'true';
                }
                return attrs;
              },
            },
          ],
          toDOM(node) {
            const attrs: any = {};
            if (capabilities.tasks && node.attrs.checked !== null) {
              attrs['data-checked'] = node.attrs.checked ? 'true' : 'false';
            }
            return ['li', attrs, 0];
          },
        },
        pandoc: {},
      },
      {
        name: 'bullet_list',
        spec: {
          content: 'list_item+',
          group: 'block',
          attrs: {
            tight: { default: true },
          },
          parseDOM: [
            {
              tag: 'ul',
              getAttrs: (dom: Node | string) => {
                const el = dom as Element;
                const attrs: any = {};
                if (el.hasAttribute('data-tight')) {
                  attrs.tight = true;
                }
                return attrs;
              },
            },
          ],
          toDOM(node) {
            const attrs: { [key: string]: string } = {};
            if (node.attrs.tight) {
              attrs['data-tight'] = 'true';
            }
            return ['ul', attrs, 0];
          },
        },
        pandoc: {
          readers: [
            {
              token: PandocTokenType.BulletList,
              handler: (schema: Schema) => readPandocList(schema.nodes.bullet_list, capabilities),
            },
          ],
          writer: writePandocBulletList(capabilities),
        },
      },
      {
        name: 'ordered_list',
        spec: {
          content: 'list_item+',
          group: 'block',
          attrs: {
            tight: { default: true },
            order: { default: 1 },
            number_style: { default: ListNumberStyle.DefaultStyle },
            number_delim: { default: ListNumberDelim.DefaultDelim },
          },
          parseDOM: [
            {
              tag: 'ol',
              getAttrs(dom: Node | string) {
                const el = dom as Element;

                const attrs: any = {};
                attrs.tight = el.hasAttribute('data-tight');

                if (capabilities.order) {
                  let order: string | number | null = el.getAttribute('start');
                  if (!order) {
                    order = 1;
                  }
                  attrs.order = order;
                }

                if (capabilities.fancy) {
                  if (capabilities.example && el.getAttribute('data-example')) {
                    attrs.number_style = ListNumberStyle.Example;
                  } else {
                    attrs.number_style = typeToNumberStyle(el.getAttribute('type'));
                  }
                  const numberDelim = el.getAttribute('data-number-delim');
                  if (numberDelim) {
                    attrs.number_delim = numberDelim;
                  }
                }

                return attrs;
              },
            },
          ],
          toDOM(node) {
            const attrs: { [key: string]: string } = {};
            if (node.attrs.tight) {
              attrs['data-tight'] = 'true';
            }
            if (capabilities.order && node.attrs.order !== 1) {
              attrs.start = node.attrs.order;
            }
            if (capabilities.fancy) {
              const type = numberStyleToType(node.attrs.number_style);
              if (type) {
                attrs.type = type;
              }
              if (capabilities.example) {
                if (node.attrs.number_style === ListNumberStyle.Example) {
                  attrs['data-example'] = '1';
                }
              }
              attrs['data-number-delim'] = node.attrs.number_delim;
            }
            return ['ol', attrs, 0];
          },
        },
        pandoc: {
          readers: [
            {
              token: PandocTokenType.OrderedList,
              handler: (schema: Schema) => readPandocList(schema.nodes.ordered_list, capabilities),
            },
          ],
          writer: writePandocOrderedList(capabilities),
          markdownOutputFilter: (markdown: string) => {
            markdown = exampleListPandocMarkdownOutputFilter(markdown);
            if (capabilities.tasks) {
              markdown = checkedListItemMarkdownOutputFilter(markdown);
            }
            return markdown;
          },
        },
      },
    ],

    plugins: (schema: Schema) => {
      const plugins: Plugin[] = [];
      if (capabilities.tasks) {
        plugins.push(
          new Plugin({
            key: plugin,
            props: {
              nodeViews: {
                list_item(node: ProsemirrorNode, view: EditorView, getPos: boolean | (() => number)) {
                  return new CheckedListItemNodeView(node, view, getPos as () => number);
                },
              },
            },
          }),
        );
      }
      return plugins;
    },

    commands: (schema: Schema, ui: EditorUI) => {
      const commands = [
        new ListCommand(EditorCommandId.BulletList, ['Shift-Ctrl-7'], schema.nodes.bullet_list, schema.nodes.list_item),
        new ListCommand(
          EditorCommandId.OrderedList,
          ['Shift-Ctrl-8'],
          schema.nodes.ordered_list,
          schema.nodes.list_item,
        ),
        new ProsemirrorCommand(EditorCommandId.ListItemSink, ['Tab'], sinkListItem(schema.nodes.list_item)),
        new ProsemirrorCommand(EditorCommandId.ListItemLift, ['Shift-Tab'], liftListItem(schema.nodes.list_item)),
        new ProsemirrorCommand(EditorCommandId.ListItemSplit, ['Enter'], splitListItem(schema.nodes.list_item)),
        new TightListCommand(),
      ];
      if (capabilities.fancy) {
        commands.push(new OrderedListEditCommand(ui, capabilities));
      }
      if (capabilities.tasks) {
        commands.push(
          new CheckedListItemCommand(schema.nodes.list_item),
          new CheckedListItemToggleCommand(schema.nodes.list_item),
        );
      }
      return commands;
    },

    baseKeys: (schema: Schema) => {
      return [
        { key: BaseKey.Enter, command: splitListItem(schema.nodes.list_item) },
        { key: BaseKey.Tab, command: sinkListItem(schema.nodes.list_item) },
        { key: BaseKey.ShiftTab, command: liftListItem(schema.nodes.list_item) },
      ];
    },

    inputRules: (schema: Schema) => {
      const rules = [
        wrappingInputRule(/^\s*([-+*])\s$/, schema.nodes.bullet_list),
        wrappingInputRule(
          /^(\d+)\.\s$/,
          schema.nodes.ordered_list,
          match => ({ order: +match[1] }),
          (match, node) => node.childCount + node.attrs.order === +match[1],
        ),
      ];
      if (capabilities.tasks) {
        rules.push(checkedListInputRule(schema), checkedListItemInputRule());
      }
      return rules;
    },
  };
};

function numberStyleToType(style: ListNumberStyle): string | null {
  switch (style) {
    case ListNumberStyle.DefaultStyle:
    case ListNumberStyle.Decimal:
    case ListNumberStyle.Example:
      return 'l';
    case ListNumberStyle.LowerAlpha:
      return 'a';
    case ListNumberStyle.UpperAlpha:
      return 'A';
    case ListNumberStyle.LowerRoman:
      return 'i';
    case ListNumberStyle.UpperRoman:
      return 'I';
    default:
      return null;
  }
}

function typeToNumberStyle(type: string | null): ListNumberStyle {
  switch (type) {
    case 'l':
      return ListNumberStyle.Decimal;
    case 'a':
      return ListNumberStyle.LowerAlpha;
    case 'A':
      return ListNumberStyle.UpperAlpha;
    case 'i':
      return ListNumberStyle.LowerRoman;
    case 'I':
      return ListNumberStyle.UpperRoman;
    default:
      return ListNumberStyle.Decimal;
  }
}

export default extension;
