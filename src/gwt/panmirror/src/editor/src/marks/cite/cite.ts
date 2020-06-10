/*
 * cite.ts
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

import { Mark, Schema, Fragment, Node as ProsemirrorNode } from 'prosemirror-model';
import { InputRule } from 'prosemirror-inputrules';
import { EditorState, TextSelection } from 'prosemirror-state';

import { EditorUI } from '../../api/ui';
import { PandocTokenType, PandocToken, PandocOutput, ProsemirrorWriter, PandocExtensions } from '../../api/pandoc';
import { fragmentText } from '../../api/fragment';

import { citeHighlightPlugin } from './cite-highlight';
import { InsertCitationCommand } from './cite-commands';
import { markIsActive, splitInvalidatedMarks } from '../../api/mark';
import { MarkTransaction } from '../../api/transaction';
import { PandocCapabilities } from '../../api/pandoc_capabilities';
import { Extension } from '../../api/extension';

const CITE_CITATIONS = 0;

const kCiteIdPrefixPattern = '-?@';
const kCiteIdCharsPattern = '\\w[\\w:\\.#\\$%&\\-\\+\\?<>~/]*';
const kCiteIdPattern = `^${kCiteIdPrefixPattern}${kCiteIdCharsPattern}$`;
const kBeginCitePattern = `(.* ${kCiteIdPrefixPattern}|${kCiteIdPrefixPattern})`;

const kCiteIdRegEx = new RegExp(kCiteIdPattern);
const kCiteIdLengthRegEx = new RegExp(`^${kCiteIdPrefixPattern}${kCiteIdCharsPattern}`);
const kCiteRegEx = new RegExp(`${kBeginCitePattern}${kCiteIdCharsPattern}.*`);
const kFullCiteRegEx = new RegExp(`\\[${kBeginCitePattern}${kCiteIdCharsPattern}.*\\]`);

enum CitationMode {
  NormalCitation = 'NormalCitation',
  AuthorInText = 'AuthorInText',
  SuppressAuthor = 'SuppressAuthor',
}

interface Citation {
  citationHash: number;
  citationId: string;
  citationMode: {
    t: CitationMode;
  };
  citationNoteNum: number;
  citationPrefix: PandocToken[];
  citationSuffix: PandocToken[];
}

const extension = (
  pandocExtensions: PandocExtensions,
  _pandocCapabilities: PandocCapabilities,
  ui: EditorUI,
): Extension | null => {
  if (!pandocExtensions.citations) {
    return null;
  }

  const citePlaceholder = ui.context.translateText('cite');

  return {
    marks: [
      {
        name: 'cite_id',
        spec: {
          attrs: {},
          inclusive: true,
          parseDOM: [
            {
              tag: "span[class*='cite-id']",
            },
          ],
          toDOM(mark: Mark) {
            return ['span', { class: 'cite-id pm-markup-text-color pm-fixedwidth-font' }];
          },
        },
        pandoc: {
          readers: [],
          writer: {
            priority: 13,
            write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
              const idText = fragmentText(parent);
              // only write as a citation id (i.e. don't escape @) if it is still
              // a valid citation id. note that this in principle is also taken care
              // of by the application of splitInvalidatedMarks below (as the
              // mark would have been broken by this if it wasn't valid). this
              // code predates that, and we leave it in for good measure in case that
              // code is removed or changes in another unexpected way.
              if (idText.match(kCiteIdRegEx)) {
                const prefixMatch = idText.match(/^-?@/);
                if (prefixMatch) {
                  output.writeRawMarkdown(prefixMatch.input!);
                  output.writeInlines(parent.cut(prefixMatch.input!.length));
                } else {
                  output.writeInlines(parent);
                }
              } else {
                output.writeInlines(parent);
              }
            },
          },
        },
      },
      {
        name: 'cite',
        spec: {
          attrs: {},
          inclusive: false,
          parseDOM: [
            {
              tag: "span[class*='cite']",
            },
          ],
          toDOM(mark: Mark) {
            return { '0': 'span', '1': { class: 'cite' } };
          },
        },
        pandoc: {
          readers: [
            {
              token: PandocTokenType.Cite,
              handler: readPandocCite,
            },
          ],

          writer: {
            priority: 14,
            write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
              // divide out delimiters from body
              const openCite = parent.cut(0, 1);
              const cite = parent.cut(1, parent.size - 1);
              const closeCite = parent.cut(parent.size - 1, parent.size);

              // proceed if the citation is still valid
              if (
                fragmentText(openCite) === '[' &&
                fragmentText(closeCite) === ']' &&
                fragmentText(cite).match(kCiteRegEx)
              ) {
                output.writeRawMarkdown('[');
                output.writeInlines(cite);
                output.writeRawMarkdown(']');
              } else {
                output.writeInlines(parent);
              }
            },
          },
        },
      },
    ],

    commands: (_schema: Schema) => {
      return [new InsertCitationCommand(ui)];
    },

    appendMarkTransaction: (schema: Schema) => {
      return [
        {
          // 'break' cite_id marks if they are no longer valid. note that this will still preserve
          // the mark up to the length that it is valid (so e.g. if a space is inserted within a
          // cite_id this will keep the mark on the part before the space and remove it from the
          // part after the space)
          name: 'remove-cite-id-marks',
          filter: (node: ProsemirrorNode) => node.isTextblock && node.type.allowsMarkType(schema.marks.cite_id),
          append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
            splitInvalidatedMarks(tr, node, pos, citeIdLength, schema.marks.cite_id);
          },
        },
        {
          // 'break' cite marks if they are no longer valid.
          name: 'remove-cite-marks',
          filter: (node: ProsemirrorNode) => node.isTextblock && node.type.allowsMarkType(schema.marks.cite),
          append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
            splitInvalidatedMarks(tr, node, pos, citeLength, schema.marks.cite);
          },
        },
      ];
    },

    inputRules: (schema: Schema) => {
      return [citeInputRule(schema, citePlaceholder), citeIdInputRule(schema, citePlaceholder)];
    },

    plugins: (schema: Schema) => {
      return [citeHighlightPlugin(schema)];
    },
  };
};

// automatically create a citation given certain input
function citeInputRule(schema: Schema, citePlaceholder: string) {
  return new InputRule(
    new RegExp(`\\[${kBeginCitePattern}$`),
    (state: EditorState, match: string[], start: number, end: number) => {
      // only apply if we aren't already in a cite and the preceding text doesn't include an end cite (']')
      if (!markIsActive(state, schema.marks.cite) && !match[0].includes(']')) {
        // create transaction
        const tr = state.tr;

        // check to see whether we need to insert an end bracket
        const nextChar = state.doc.textBetween(end, end + 1);
        const suffix = nextChar !== ']' ? ']' : '';

        // remove whatever cite prefix caused this match to occur
        const idPrefixMatch = match[0].endsWith('-@') ? '-@' : '@';
        tr.delete(end - (idPrefixMatch.length - 1), end);

        // insert a cite_id with placeholder text
        const citeIdText = idPrefixMatch + citePlaceholder + suffix;
        const citeIdMark = schema.marks.cite_id.create();
        const citeId = schema.text(citeIdText, [citeIdMark]);
        tr.replaceSelectionWith(citeId, false);

        // select the cite_id placeholder
        const begin = end + 1;
        tr.setSelection(new TextSelection(tr.doc.resolve(begin), tr.doc.resolve(begin + citePlaceholder.length)));

        // mark the entire region as a citation
        const mark = schema.marks.cite.create();
        tr.addMark(start, end + citeIdText.length + 2, mark);

        return tr;
      } else {
        return null;
      }
    },
  );
}

// create a cite_id within a citation when the @ sign is typed
function citeIdInputRule(schema: Schema, citePlaceholder: string) {
  return new InputRule(
    new RegExp(`${kCiteIdPrefixPattern}$`),
    (state: EditorState, match: string[], start: number, end: number) => {
      // only operate within a cite mark
      if (markIsActive(state, schema.marks.cite)) {
        // screen out if the previous character isn't the beginning of the cite or a space
        const prevChar = state.doc.textBetween(start - 1, end);
        if (prevChar !== '[' && prevChar !== ' ') {
          return null;
        }

        // create transaction
        const tr = state.tr;

        // mark the the cite_id
        const citeIdMark = schema.marks.cite_id.create();
        tr.addMark(start, end, citeIdMark);
        tr.addStoredMark(citeIdMark);
        tr.insertText(match[0]);

        // figure out if the prefix is right before valid id text, in that
        // case extend the mark to encompass that text
        let extended = false;
        const { parent, parentOffset } = tr.selection.$head;
        const text = parent.textContent.slice(parentOffset - 1);
        if (text.length > 0) {
          const length = citeIdLength(text);
          if (length > 1) {
            const startTex = tr.selection.from - 1;
            tr.addMark(startTex, startTex + length, citeIdMark);
            extended = true;
          }
        }

        // if we didn't extend the mark to existing id text then instaed
        // insert placeholder text and select it
        if (!extended) {
          tr.insertText(citePlaceholder);
          const begin = start + match[0].length;
          tr.setSelection(new TextSelection(tr.doc.resolve(begin), tr.doc.resolve(start + citePlaceholder.length + 1)));
        }

        // return transaction
        return tr;
      } else {
        return null;
      }
    },
  );
}

// read pandoc citation, creating requisite cite and cite_id marks as we go
function readPandocCite(schema: Schema) {
  return (writer: ProsemirrorWriter, tok: PandocToken) => {
    const citeMark = schema.marks.cite.create();
    writer.openMark(citeMark);
    writer.writeText('[');
    const citations: Citation[] = tok.c[CITE_CITATIONS];
    citations.forEach((citation: Citation, index: number) => {
      // add delimiter
      if (index !== 0) {
        writer.writeText('; ');
      }

      // prefix
      writer.writeTokens(citation.citationPrefix);
      if (citation.citationPrefix.length) {
        writer.writeText(' ');
      }

      // id
      const suppress = citation.citationMode.t === CitationMode.SuppressAuthor ? '-' : '';
      const citeIdMark = schema.marks.cite_id.create();
      writer.openMark(citeIdMark);
      writer.writeText(suppress + '@' + citation.citationId);
      writer.closeMark(citeIdMark);

      // suffix
      if (citation.citationMode.t === CitationMode.AuthorInText && citation.citationSuffix.length) {
        writer.writeText(' ');
      }
      writer.writeTokens(citation.citationSuffix);
    });

    writer.writeText(']');
    writer.closeMark(citeMark);
  };
}

// validate that the cite is still valid (just return 0 or the whole length of the string)
function citeLength(text: string) {
  return text.match(kFullCiteRegEx) ? text.length : 0;
}

// up to how many characters of the passed text constitute a valid cite_id
function citeIdLength(text: string) {
  const match = text.match(kCiteIdLengthRegEx);
  if (match) {
    return match[0].length;
  } else {
    return 0;
  }
}

export default extension;
