/*
 * cite.ts
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

import { Mark, Schema, Fragment, Node as ProsemirrorNode } from 'prosemirror-model';
import { InputRule } from 'prosemirror-inputrules';
import { EditorState, TextSelection, Transaction } from 'prosemirror-state';

import { Extension, extensionIfEnabled } from '../../api/extension';
import { EditorUI } from '../../api/ui';
import { PandocTokenType, PandocToken, PandocOutput, ProsemirrorWriter } from '../../api/pandoc';
import { fragmentText } from '../../api/fragment';

import { citeHighlightPlugin } from './cite-highlight';
import { InsertCitationCommand } from './cite-commands';
import { markIsActive, splitInvalidatedMarks } from '../../api/mark';
import { MarkTransaction } from '../../api/transaction';
import { findChildrenByMark } from 'prosemirror-utils';

const CITE_CITATIONS = 0;

const kCiteIdPrefixPattern = "-?@";
const kCiteIdCharsPattern = "[\\w:.#$%&-+?<>~/]+";
const kCiteIdPattern = `^${kCiteIdPrefixPattern}${kCiteIdCharsPattern}$`;
const kBeginCitePattern = `(.* ${kCiteIdPrefixPattern}|${kCiteIdPrefixPattern})`;
const kCiteIdLengthRegEx = new RegExp(`^${kCiteIdPrefixPattern}${kCiteIdCharsPattern}`);
const kCiteIdRegEx = new RegExp(kCiteIdPattern);
const kCiteRegEx = new RegExp(`${kBeginCitePattern}${kCiteIdCharsPattern}.*`);


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

const extension: Extension = {
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
          return ['span', { class: 'cite-id pm-link-text-color' }];
        },
      },
      pandoc: {
        readers: [],
        writer: {
          priority: 13,
          write: (output: PandocOutput, _mark: Mark, parent: Fragment) => {
            const idText = fragmentText(parent);
            if (kCiteIdRegEx.test(idText)) {
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
            
          }
        }
      }
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
          return ['span', { class: 'cite' }];
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
            if (fragmentText(openCite) === '[' && 
                fragmentText(closeCite) === ']'&&
                kCiteRegEx.test(fragmentText(cite))) {
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

  commands: (_schema: Schema, ui: EditorUI) => {
    return [new InsertCitationCommand(ui)];
  },

  appendMarkTransaction: (schema: Schema) => {
    return [
      {
        name: 'remove-cite-id-marks',
        filter: node => node.isTextblock && node.type.allowsMarkType(schema.marks.cite_id),
        append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
          splitInvalidatedMarks(tr, node, pos, citeIdLength, schema.marks.cite_id);
        },
      },
    ];
  },

  inputRules: (schema: Schema) => {
    return [
      citeInputRule(schema),
      citeIdInputRule(schema),
      citeSuppressAuthorInputRule(schema)
    ];
  },

  plugins: (schema: Schema) => {
    return [citeHighlightPlugin(schema)];
  },
};


function citeInputRule(schema: Schema) {
  return new InputRule(new RegExp(`\\[${kBeginCitePattern}$`), (state: EditorState, match: string[], start: number, end: number) => {
    if (!markIsActive(state, schema.marks.cite)) {
      const tr = state.tr;
      const nextChar = state.doc.textBetween(end, end + 1);
      const suffix = nextChar !== ']' ? ']' : '';

      const idPrefixMatch = match[0].endsWith('-@') ? '-@' : '@';
      tr.delete(end - (idPrefixMatch.length - 1), end);

      const kCitePlaceholder = 'cite';
      const citeIdText = idPrefixMatch + kCitePlaceholder + suffix;
      const citeIdMark = schema.marks.cite_id.create();
      const citeId = schema.text(citeIdText, [citeIdMark]);
      tr.replaceSelectionWith(citeId, false);

      const begin = end + 1;
      tr.setSelection(new TextSelection(
        tr.doc.resolve(begin), tr.doc.resolve(begin + kCitePlaceholder.length)
      ));

      const mark = schema.marks.cite.create();
      tr.addMark(start, end + citeIdText.length + 2, mark);
    
      return tr;
    } else {
      return null;
    }
  });
}


function citeIdInputRule(schema: Schema) { 
  return new InputRule(new RegExp(`${kCiteIdPrefixPattern}$`), (state: EditorState, match: string[], start: number, end: number) => {
    if (markIsActive(state, schema.marks.cite)) {

      // screen out if the previous character isn't the beginning of the cite or a space
      const prevChar = state.doc.textBetween(start - 1, start);
      if (prevChar !== "[" && prevChar !== " ") {
        return null;
      }

      const tr = state.tr;

      tr.delete(start, end);
      const citeIdMark = schema.marks.cite_id.create();
      tr.addStoredMark(citeIdMark);
      tr.insertText(match[0]);

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

      if (!extended) {
        const kCitePlaceholder = 'cite';
        tr.insertText(kCitePlaceholder);
        const begin = start + match[0].length;
        tr.setSelection(new TextSelection(
          tr.doc.resolve(begin), tr.doc.resolve(start + kCitePlaceholder.length + 1)
        ));
      }
      return tr;
    } else {
      return null;
    }
  });
}

function citeSuppressAuthorInputRule(schema: Schema) {
  return new InputRule(/-$/, (state: EditorState, match: string[], start: number, end: number) => {
    if (markIsActive(state, schema.marks.cite) && !state.doc.rangeHasMark(start, start, schema.marks.cite_id)) {
      if (state.doc.rangeHasMark(start + 1, start + 2, schema.marks.cite_id)) {
        const nextChar = state.doc.textBetween(start, start + 1);
        if (nextChar !== '-') {
          const tr = state.tr;
          tr.addStoredMark(schema.marks.cite_id.create());
          tr.insertText('-');
          return tr;
        }        
      }
    }

    return null;
  });
}


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


function citeIdLength(text: string) {
  const match = text.match(kCiteIdLengthRegEx);
  if (match) {
    return match[0].length;
  } else {
    return 0;
  }
}


export default extensionIfEnabled(extension, 'citations');
