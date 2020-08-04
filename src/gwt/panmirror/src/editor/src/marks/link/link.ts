/*
 * link.ts
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

import { Fragment, Mark, Schema } from 'prosemirror-model';
import { PluginKey, Plugin } from 'prosemirror-state';

import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { PandocToken, PandocOutput, PandocTokenType } from '../../api/pandoc';
import {
  pandocAttrSpec,
  pandocAttrParseDom,
  pandocAttrToDomAttr,
  pandocAttrReadAST,
  PandocAttr,
} from '../../api/pandoc_attr';
import { Extension, ExtensionContext } from '../../api/extension';
import { kLinkTarget, kLinkTargetUrl, kLinkTargetTitle, kLinkAttr, kLinkChildren } from '../../api/link';
import { hasShortcutHeadingLinks } from '../../api/pandoc_format';

import { linkCommand, removeLinkCommand, linkOmniInsert } from './link-command';
import { linkInputRules, linkPasteHandler } from './link-auto';
import { linkHeadingsPostprocessor, syncHeadingLinksAppendTransaction } from './link-headings';
import { linkPopupPlugin } from './link-popup';

import './link-styles.css';


const extension = (context: ExtensionContext): Extension => {
  const { pandocExtensions, ui, navigation } = context;

  const capabilities = {
    headings: hasShortcutHeadingLinks(pandocExtensions),
    attributes: pandocExtensions.link_attributes,
    text: true,
  };
  const linkAttr = pandocExtensions.link_attributes;
  const autoLink = pandocExtensions.autolink_bare_uris;
  const headingLink = hasShortcutHeadingLinks(pandocExtensions);

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
                const target = tok.c[kLinkTarget];
                return {
                  href: target[kLinkTargetUrl],
                  title: target[kLinkTargetTitle] || null,
                  ...(linkAttr ? pandocAttrReadAST(tok, kLinkAttr) : {}),
                };
              },
              getChildren: (tok: PandocToken) => tok.c[kLinkChildren],

              postprocessor: hasShortcutHeadingLinks(pandocExtensions) ? linkHeadingsPostprocessor : undefined,
            },
          ],

          writer: {
            priority: 12,
            write: (output: PandocOutput, mark: Mark, parent: Fragment) => {
              if (mark.attrs.heading) {
                output.writeRawMarkdown('[');
                output.writeInlines(parent);
                output.writeRawMarkdown(']');
              } else {
                output.writeLink(
                  mark.attrs.href,
                  mark.attrs.title,
                  linkAttr ? (mark.attrs as PandocAttr) : null,
                  () => {
                    output.writeInlines(parent);
                  },
                );
              }
            },
          },
        },
      },
    ],

    commands: (schema: Schema) => {
      return [
        new ProsemirrorCommand(
          EditorCommandId.Link,
          ['Mod-k'],
          linkCommand(schema.marks.link, ui.dialogs.editLink, capabilities),
          linkOmniInsert(ui),
        ),
        new ProsemirrorCommand(EditorCommandId.RemoveLink, [], removeLinkCommand(schema.marks.link)),
      ];
    },

    inputRules: linkInputRules(autoLink, headingLink),

    appendTransaction: (schema: Schema) =>
      pandocExtensions.implicit_header_references ? [syncHeadingLinksAppendTransaction()] : [],

    plugins: (schema: Schema) => {
      const plugins = [
        linkPopupPlugin(
          schema,
          ui,
          navigation,
          linkCommand(schema.marks.link, ui.dialogs.editLink, capabilities),
          removeLinkCommand(schema.marks.link),
        ),
      ];
      if (autoLink) {
        plugins.push(
          new Plugin({
            key: new PluginKey('link-auto'),
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
