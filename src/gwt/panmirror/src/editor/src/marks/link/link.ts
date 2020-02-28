/*
 * link.ts
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

import { Fragment, Mark, Schema } from 'prosemirror-model';
import { PluginKey, Plugin } from 'prosemirror-state';

import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { PandocToken, PandocOutput, PandocTokenType, PandocExtensions } from '../../api/pandoc';
import { pandocAttrSpec, pandocAttrParseDom, pandocAttrToDomAttr, pandocAttrReadAST } from '../../api/pandoc_attr';
import { EditorUI } from '../../api/ui';
import { Extension } from '../../api/extension';
import { EditorOptions } from '../../api/options';

import { linkCommand } from './link-command';
import { linkInputRules, linkPasteHandler } from './link-auto';
import { linkHeadingsPostprocessor, syncHeadingLinksAppendTransaction } from './link-headings';
import { LinkPopupPlugin } from './link-popup';

import './link-styles.css';

const TARGET_URL = 0;
const TARGET_TITLE = 1;

const LINK_ATTR = 0;
const LINK_CHILDREN = 1;
const LINK_TARGET = 2;

const kHeadingLinkSentinel = '#2E93950F-D8CF-4551-88E0-F496194FDEE8';
const kHeadingLinkSentinelRegex = new RegExp(`\\(${kHeadingLinkSentinel}\\)`, 'g');

const extension = (pandocExtensions: PandocExtensions, options: EditorOptions): Extension => {
  const capabilities = {
    headings: pandocExtensions.implicit_header_references,
    attributes: pandocExtensions.link_attributes,
  };
  const linkAttr = pandocExtensions.link_attributes;
  const autoLink = pandocExtensions.autolink_bare_uris;
  const headingLink = pandocExtensions.implicit_header_references;

  return {
    marks: [
      {
        name: 'link',
        spec: {
          attrs: {
            href: {},
            heading: { default: null },
            title: { default: null },
            ...(linkAttr ? pandocAttrSpec : {}),
          },
          inclusive: false,
          parseDOM: [
            {
              tag: 'a[href]',
              getAttrs(dom: Node | string) {
                const el = dom as Element;
                const attrs: { [key: string]: string | null } = {
                  href: el.getAttribute('href'),
                  title: el.getAttribute('title'),
                  heading: el.getAttribute('data-heading'),
                };
                return {
                  ...attrs,
                  ...(linkAttr ? pandocAttrParseDom(el, attrs) : {}),
                };
              },
            },
          ],
          toDOM(mark: Mark) {
            const linkClasses = 'pm-link pm-link-text-color';

            let extraAttr: any = {};
            if (linkAttr) {
              extraAttr = pandocAttrToDomAttr({
                ...mark.attrs,
                classes: [...mark.attrs.classes, linkClasses],
              });
            } else {
              extraAttr = { class: linkClasses };
            }

            return [
              'a',
              {
                href: mark.attrs.href,
                title: mark.attrs.title,
                'data-heading': mark.attrs.heading,
                ...extraAttr,
              },
            ];
          },
        },
        pandoc: {
          readers: [
            {
              token: PandocTokenType.Link,
              mark: 'link',
              getAttrs: (tok: PandocToken) => {
                const target = tok.c[LINK_TARGET];
                return {
                  href: target[TARGET_URL],
                  title: target[TARGET_TITLE] || null,
                  ...(linkAttr ? pandocAttrReadAST(tok, LINK_ATTR) : {}),
                };
              },
              getChildren: (tok: PandocToken) => tok.c[LINK_CHILDREN],

              postprocessor: pandocExtensions.implicit_header_references ? linkHeadingsPostprocessor : undefined,
            },
          ],

          writer: {
            priority: 15,
            write: (output: PandocOutput, mark: Mark, parent: Fragment) => {
              output.writeToken(PandocTokenType.Link, () => {
                // write attributes if the current format supports that
                if (linkAttr) {
                  output.writeAttr(mark.attrs.id, mark.attrs.classes, mark.attrs.keyvalue);
                } else {
                  output.writeAttr();
                }

                // write content
                output.writeArray(() => {
                  output.writeInlines(parent);
                });

                // write href (but if this is a heading link write a special sentinel that
                // we will remove in the postprocessor)
                const href = mark.attrs.heading ? kHeadingLinkSentinel : mark.attrs.href;
                output.write([href || '', mark.attrs.title || '']);
              });
            },
          },

          markdownOutputFilter: (markdown: string) => {
            return markdown.replace(kHeadingLinkSentinelRegex, '');
          },
        },
      },
    ],

    commands: (schema: Schema, ui: EditorUI) => {
      return [
        new ProsemirrorCommand(
          EditorCommandId.Link,
          ['Mod-k'],
          linkCommand(schema.marks.link, ui.dialogs.editLink, capabilities),
        ),
      ];
    },

    inputRules: linkInputRules(autoLink, headingLink),

    appendTransaction: (schema: Schema) =>
      pandocExtensions.implicit_header_references ? [syncHeadingLinksAppendTransaction()] : [],

    plugins: (schema: Schema) => {
      const plugins = [new LinkPopupPlugin()];
      if (autoLink) {
        plugins.push(
          new Plugin({
            key: new PluginKey('link'),
            props: {
              transformPasted: linkPasteHandler(schema),
            },
          }),
        );
      } 
      return plugins;
    },
  };
};

export default extension;
