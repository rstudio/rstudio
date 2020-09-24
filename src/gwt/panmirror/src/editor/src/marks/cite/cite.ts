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

import { Mark, Schema, Fragment, Node as ProsemirrorNode, Slice } from 'prosemirror-model';
import { InputRule } from 'prosemirror-inputrules';
import { EditorState, Transaction, Plugin, PluginKey, Selection } from 'prosemirror-state';
import { setTextSelection } from 'prosemirror-utils';
import { EditorView } from 'prosemirror-view';

import uniqby from 'lodash.uniqby';

import { FocusEvent } from '../../api/event-types';
import { PandocTokenType, PandocToken, PandocOutput, ProsemirrorWriter, PandocServer } from '../../api/pandoc';
import { fragmentText } from '../../api/fragment';
import { markIsActive, splitInvalidatedMarks, getMarkRange } from '../../api/mark';
import { MarkTransaction } from '../../api/transaction';
import { BibliographyManager, BibliographyFile, BibliographySource } from '../../api/bibliography/bibliography';
import { EditorUI } from '../../api/ui';
import { joinPaths, getExtension } from '../../api/path';
import { Extension, ExtensionContext } from '../../api/extension';
import { InsertCiteProps, kAlertTypeError, kAlertTypeWarning } from '../../api/ui-dialogs';
import { CSL, sanitizeForCiteproc } from '../../api/csl';
import { suggestCiteId, formatForPreview } from '../../api/cite';
import { performCompletionReplacement } from '../../api/completion';
import { ensureBibliographyFileForDoc } from '../../api/bibliography/bibliography-provider_local';

import { citationCompletionHandler } from './cite-completion';
import { citeHighlightPlugin } from './cite-highlight';
import { citationDoiCompletionHandler } from './cite-completion_doi';
import { doiFromSlice } from './cite-doi';
import { citePopupPlugin } from './cite-popup';
import { InsertCitationCommand } from './cite-commands';


const kCiteCitationsIndex = 0;

export const kCiteIdPrefixPattern = '-?@';
const kCiteIdOptionalCharsPattern = '[^@;\\[\\]\\s]*';


const kCiteIdCharsPattern = `${kCiteIdOptionalCharsPattern}`;
const kCiteIdPattern = `^${kCiteIdPrefixPattern}${kCiteIdCharsPattern}$`;
const kBeginCitePattern = `(.* ${kCiteIdPrefixPattern}|${kCiteIdPrefixPattern})`;

const kEditingFullCiteRegEx = new RegExp(`\\[${kBeginCitePattern}${kCiteIdOptionalCharsPattern}.*\\]`);

const kCiteIdRegEx = new RegExp(kCiteIdPattern);
const kCiteRegEx = new RegExp(`${kBeginCitePattern}${kCiteIdCharsPattern}.*`);

export const kEditingCiteIdRegEx = new RegExp(`^(${kCiteIdPrefixPattern})(${kCiteIdOptionalCharsPattern}|10.\\d{4,}\\S+)`);

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

const extension = (context: ExtensionContext): Extension | null => {
  const { pandocExtensions, ui } = context;

  // prime bibliography on initial focus
  const bibliographyManager = new BibliographyManager(context.server.pandoc, context.server.zotero);
  const focusUnsubscribe = context.events.subscribe(FocusEvent, (doc) => {
    bibliographyManager.prime(ui, doc!);
    focusUnsubscribe();
  });

  if (!pandocExtensions.citations) {
    return null;
  }

  return {
    marks: [
      {
        name: 'cite_id',
        noSpelling: true,
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
      return [new InsertCitationCommand(ui, context.events, bibliographyManager, context.server)];
    },

    appendMarkTransaction: (schema: Schema) => {
      return [
        {
          // 'break' cite marks if they are no longer valid. note that this will still preserve
          // the mark up to the length that it is valid
          name: 'remove-cite-marks',
          filter: (node: ProsemirrorNode) => node.isTextblock && node.type.allowsMarkType(schema.marks.cite),
          append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
            splitInvalidatedMarks(tr, node, pos, editingCiteLength, schema.marks.cite, (from: number, to: number) => {
              tr.removeMark(from, to, schema.marks.cite);
              tr.removeMark(from, to, schema.marks.cite_id);
            });
          },
        },
        {
          // 'break' cite_id marks if they are no longer valid. note that this will still preserve
          // the mark up to the length that it is valid (so e.g. if a space is inserted within a
          // cite_id this will keep the mark on the part before the space and remove it from the
          // part after the space)
          name: 'remove-cite-id-marks',
          filter: (node: ProsemirrorNode) => node.isTextblock && node.type.allowsMarkType(schema.marks.cite_id),
          append: (tr: MarkTransaction, node: ProsemirrorNode, pos: number) => {
            splitInvalidatedMarks(tr, node, pos, editingCiteIdLength, schema.marks.cite_id);
          },
        },
      ];
    },

    inputRules: (schema: Schema) => {
      return [
        insertCiteInputRule(schema),
        citeBeginBracketInputRule(schema),
        citeEndBracketInputRule(schema),
        citeIdInputRule(schema),
      ];
    },

    completionHandlers: () => [
      citationDoiCompletionHandler(context.ui, bibliographyManager, context.server),
      citationCompletionHandler(context.ui, context.events, bibliographyManager, context.server.pandoc),
    ],

    plugins: (schema: Schema) => {
      return [
        new Plugin(
          {
            key: new PluginKey('paste_cite_doi'),
            props: {
              handlePaste: handlePaste(ui, bibliographyManager, context.server.pandoc),
            }
          }),
        citeHighlightPlugin(schema),
        citePopupPlugin(schema, ui, bibliographyManager, context.server.pandoc)
      ];
    },
  };
};

function handlePaste(ui: EditorUI, bibManager: BibliographyManager, server: PandocServer) {
  return (view: EditorView, _event: Event, slice: Slice) => {

    const schema = view.state.schema;
    if (markIsActive(view.state, schema.marks.cite)) {
      // This is a DOI
      const parsedDOI = doiFromSlice(view.state, slice);
      if (parsedDOI) {

        // First check the local bibliography- if we already have this DOI
        // we can just paste the DOI and allow the completion to handle it
        const source = bibManager.findDoiInLocalBibliography(parsedDOI.token);

        // Insert the DOI text as a placeholder
        const tr = view.state.tr;
        tr.setMeta('paste', true);
        tr.setMeta('uiEvent', 'paste');

        const doiText = schema.text(parsedDOI.token);
        tr.replaceSelectionWith(doiText, true);
        view.dispatch(tr);

        if (!source && bibManager.allowsWrites()) {
          insertCitation(view, parsedDOI.token, bibManager, parsedDOI.pos, ui, server);
        }
        return true;

      } else {
        // This is just content, accept any text and try pasting that
        let text = '';
        slice.content.forEach((node: ProsemirrorNode) => (text = text + node.textContent));
        if (text.length > 0) {
          const tr = view.state.tr;
          tr.setMeta('paste', true);
          tr.setMeta('uiEvent', 'paste');
          tr.replaceSelectionWith(schema.text(text));
          view.dispatch(tr);
          return true;
        } else {
          // There wasn't any text, just allow the paste to be handled by anyone else
          return false;
        }
      }
    } else {
      // We aren't in a citation so let someone else handle the paste
      return false;
    }
  };
}

// automatically create a citation given certain input
function insertCiteInputRule(schema: Schema) {
  return new InputRule(
    new RegExp(`(^|[^\`])\\[${kBeginCitePattern}$`),
    (state: EditorState, match: string[], start: number, end: number) => {
      // only apply if we aren't already in a cite and the preceding text doesn't include an end cite (']')
      const citeMatch = match[0].substr(match[1].length);
      if (!markIsActive(state, schema.marks.cite) && !citeMatch.includes(']')) {
        // determine if we already have an end bracket
        const suffix = findCiteEndBracket(state.selection) === -1 ? ']' : '';

        // create transaction
        const tr = state.tr;

        // insert the @
        tr.insertText('@');

        const startCite = tr.selection.from - citeMatch.length;

        // determine beginning and end

        let endCite = findCiteEndBracket(tr.selection);

        // insert end bracket if we need to
        if (endCite === -1) {
          tr.insertText(']');
          endCite = tr.selection.from;
          setTextSelection(endCite - 1)(tr);
        }

        encloseInCiteMark(tr, startCite, endCite + 1);

        return tr;
      } else {
        return null;
      }
    },
  );
}

function citeBeginBracketInputRule(schema: Schema) {
  return new InputRule(new RegExp(`\\[$`), (state: EditorState, _match: string[], start: number, end: number) => {
    // only apply if we aren't already in a cite
    if (!markIsActive(state, schema.marks.cite)) {
      // find an end ciation bracket in the current text block
      const endCite = findCiteEndBracket(state.selection);
      if (endCite !== -1) {
        const citeText = '[' + state.doc.textBetween(state.selection.head, endCite + 1);

        // validate that the text range is actually a citation
        if (editingCiteLength(citeText) > 0) {
          // insert the begin bracket then calculate the cite start and end
          const tr = state.tr;
          tr.insertText('[');
          const citeStart = tr.selection.head - 1;
          const citeEnd = citeStart + citeText.length;

          // enclose in cite mark
          encloseInCiteMark(tr, citeStart, citeEnd + 1);

          return tr;
        }
      }

      return null;
    } else {
      return null;
    }
  });
}

function citeEndBracketInputRule(schema: Schema) {
  return new InputRule(new RegExp(`\\]$`), (state: EditorState, _match: string[], start: number, end: number) => {
    // only apply if we aren't already in a cite
    if (!markIsActive(state, schema.marks.cite)) {
      // look backwards for balanced begin bracket
      const beginCite = findCiteBeginBracket(state.selection);
      if (beginCite !== -1) {
        // check whether // it's actually valid citation text
        const citeText = state.doc.textBetween(beginCite, state.selection.head) + ']';
        if (editingCiteLength(citeText) > 0) {
          // create the transaction
          const tr = state.tr;

          // insert the end bracket then calculate the cite start and end
          tr.insertText(']');
          const citeStart = tr.selection.head - citeText.length;
          const citeEnd = tr.selection.head;

          // enclose in cite mark
          encloseInCiteMark(tr, citeStart, citeEnd);

          // return the transaction
          return tr;
        }
      }
      return null;
    } else {
      return null;
    }
  });
}

// create a cite_id within a citation when the @ sign is typed
function citeIdInputRule(schema: Schema) {
  return new InputRule(new RegExp(`(-|@)$`), (state: EditorState, match: string[], start: number, end: number) => {
    // only operate within a cite mark
    if (markIsActive(state, schema.marks.cite)) {
      // if we already have an @ 1 character before then this is a backspace
      // (in that case don't insert the match)
      const prefixChar = state.doc.textBetween(state.selection.from - 2, state.selection.from - 1);
      if (prefixChar !== "@") {
        const tr = state.tr;
        tr.insertText(match[1]);
        const beginCite = findCiteBeginBracket(tr.selection);
        const endCite = findCiteEndBracket(tr.selection);
        if (beginCite >= 0 && endCite >= 0) {
          const citeText = tr.doc.textBetween(beginCite, endCite + 1);
          if (editingCiteLength(citeText) > 0) {
            encloseInCiteMark(tr, beginCite, endCite + 1);
          }
        }
        return tr;
      } else {
        return null;
      }
    } else {
      return null;
    }
  });
}

// read pandoc citation, creating requisite cite and cite_id marks as we go
function readPandocCite(schema: Schema) {
  return (writer: ProsemirrorWriter, tok: PandocToken) => {
    const citeMark = schema.marks.cite.create();
    writer.openMark(citeMark);
    writer.writeText('[');
    const citations: Citation[] = tok.c[kCiteCitationsIndex];
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

// look backwards for balanced begin bracket
function findCiteBeginBracket(selection: Selection) {
  const { $head } = selection;
  let beginCite = -1;
  let bracketLevel = 0;
  const { parent, parentOffset } = $head;
  const text = parent.textContent;
  for (let i = parentOffset - 1; i >= 0; i--) {
    const char = text.charAt(i);
    if (char === ']') {
      bracketLevel++;
    } else if (char === '[') {
      if (bracketLevel > 0) {
        bracketLevel--;
        // backtick disqualifies us
      } else if (i > 0 && text.charAt(i - 1) === '`') {
        return -1;
      } else {
        beginCite = i;
        break;
      }
    }
  }
  if (beginCite !== -1) {
    return $head.start($head.depth) + beginCite;
  } else {
    return -1;
  }
}

// look forwards for balanced end bracket
function findCiteEndBracket(selection: Selection) {
  const { $head } = selection;
  let endCite = -1;
  let bracketLevel = 0;
  const { parent, parentOffset } = $head;
  const text = parent.textContent;
  for (let i = parentOffset; i < text.length; i++) {
    const char = text.charAt(i);
    if (char === '[') {
      bracketLevel++;
    } else if (char === ']') {
      if (bracketLevel > 0) {
        bracketLevel--;
      } else {
        endCite = i;
        break;
      }
    }
  }
  if (endCite !== -1) {
    return $head.start($head.depth) + endCite;
  } else {
    return -1;
  }
}

const kCitationIdRegex = new RegExp(`(^\\[| )(${kCiteIdPrefixPattern}${kCiteIdOptionalCharsPattern})`, 'g');

function encloseInCiteMark(tr: Transaction, start: number, end: number) {
  const schema = tr.doc.type.schema;
  const mark = schema.marks.cite.create();

  tr.addMark(start, end, mark);

  // look for valid citation ids inside and mark them
  const citeText = tr.doc.textBetween(start, end);

  kCitationIdRegex.lastIndex = 0;
  let match = kCitationIdRegex.exec(citeText);
  while (match !== null) {
    const pos = start + match.index + match[1].length;
    const idMark = schema.marks.cite_id.create();

    tr.addMark(pos, pos + match[2].length, idMark);
    match = kCitationIdRegex.exec(citeText);
  }
  kCitationIdRegex.lastIndex = 0;
  return tr;
}

// validate that the cite is still valid (just return 0 or the whole length of the string)
function editingCiteLength(text: string) {
  return text.match(kEditingFullCiteRegEx) ? text.length : 0;
}

// up to how many characters of the passed text constitute a valid cite_id in the editor
// (note that the editor tolerates citations ids with just an '@')
function editingCiteIdLength(text: string) {
  const match = text.match(kEditingCiteIdRegEx);
  if (match) {
    return match[0].length;
  } else {
    return 0;
  }
}

export interface ParsedCitation {
  token: string;
  pos: number;
  offset: number;
}

// completions allow spaces in the cite id (multiple search terms)
const kCiteIdCompletionCharsPattern = kCiteIdOptionalCharsPattern.replace('\\s', '');
const kCompletionCiteIdRegEx = new RegExp(`(${kCiteIdPrefixPattern})(${kCiteIdCompletionCharsPattern}|10.\\d{4,}\\S+)$`);

export function parseCitation(context: EditorState | Transaction): ParsedCitation | null {

  // return completions only if we are inside a cite (this allows for completions across
  // cite_id marks and spaces after them (necesary to allow spaces in completion queries)
  const markType = context.doc.type.schema.marks.cite;
  if (!markIsActive(context, markType)) {
    return null;
  }

  // get the range of the full cite mark
  const range = getMarkRange(context.doc.resolve(context.selection.head - 1), markType);
  if (range) {
    // examine text up to the cursor
    const citeText = context.doc.textBetween(range.from, context.selection.head);
    // look for a cite id that terminates at the cursor (including spaces/text after the id,
    // but before any semicolon delimiter)
    const match = citeText.match(kCompletionCiteIdRegEx);
    if (match) {
      const token = match[2];
      const pos = range.from + match.index! + match[1].length;
      return { token, pos, offset: -match[1].length };
    }
  }

  return null;
}

// Replaces the current selection with a resolved citation id
export async function insertCitation(
  view: EditorView,
  doi: string,
  bibManager: BibliographyManager,
  pos: number,
  ui: EditorUI,
  server: PandocServer,
  csl?: CSL,
  provider?: string
) {

  // ensure the bib manager is loaded before proceeding
  await bibManager.load(ui, view.state.doc);

  // We try not call this function if the entry for this DOI is already in the bibliography,
  // but it can happen. So we need to check here if it is already in the bibliography and 
  // if it is, deal with it appropriately.
  const existingEntry = bibManager.findDoiInLocalBibliography(doi);
  if (existingEntry) {
    // Now that we have loaded the bibliography, there is an entry
    // Just write it. Not an ideal experience, but something that
    // should happen only in unusual experiences
    const tr = view.state.tr;

    // This could be called by paste handler, so stop completions
    performCiteCompletionReplacement(tr, tr.mapping.map(pos), existingEntry.id);
    view.dispatch(tr);

  } else {
    // There isn't an entry in the existing bibliography
    // Show the user UI to and ultimately create an entry in the biblography
    // (even creating a bibliography if necessary)

    // Read bibliographies out of the document and pass those alone
    const existingIds = bibManager.localSources().map(source => source.id);

    const citeProps: InsertCiteProps = {
      doi,
      existingIds,
      bibliographyFiles: bibManager.writableBibliographyFiles(view.state.doc, ui).map(writableFile => writableFile.displayPath),
      provider,
      csl,
      citeUI: csl ? {
        suggestedId: csl.id || suggestCiteId(existingIds, csl),
        previewFields: formatForPreview(csl),
      } : undefined
    };

    const result = await ui.dialogs.insertCite(citeProps);
    if (result && result.id.length) {

      if (!result?.csl.title) {
        await ui.dialogs.alert(ui.context.translateText("This citation can't be added to the bibliography because it is missing required fields."), ui.context.translateText("Invalid Citation"), kAlertTypeError);
      } else {

        // Figure out whether this is a project or document level bibliography
        const writableBiblios = bibManager.writableBibliographyFiles(view.state.doc, ui);

        // Sort out the bibliography file into which we should write the entry
        const thisWritableBiblio = writableBiblios.find(writable => writable.displayPath === result.bibliographyFile);
        const project = thisWritableBiblio?.isProject || false;
        const writableBiblioPath = thisWritableBiblio ? thisWritableBiblio.fullPath : joinPaths(ui.context.getDefaultResourceDir(), result.bibliographyFile);
        const bibliographyFile: BibliographyFile = {
          displayPath: result.bibliographyFile,
          fullPath: writableBiblioPath,
          isProject: project,
          writable: true
        };

        // Create the source that holds the id, provider, etc...
        const source: BibliographySource = {
          ...result.csl,
          id: result.id,
          providerKey: provider || '',
        };


        // Start the transaction
        const tr = view.state.tr;

        // Write the source to the bibliography if needed
        const writeCiteId = await ensureSourcesInBibliography(
          tr,
          [source],
          bibliographyFile,
          bibManager,
          view,
          ui,
          server,
        );

        if (writeCiteId) {
          // Write the citeId
          const schema = view.state.schema;
          const idText = schema.text(source.id, [schema.marks.cite_id.create()]);
          performCiteCompletionReplacement(tr, tr.mapping.map(pos), idText);
        }

        // Dispath the transaction
        view.dispatch(tr);
      }
    }
  }
}


// Ensures that the sources are in the specified bibliography file
// and ensures that the bibliography file is properly referenced (either) 
// as a project bibliography or inline in the document YAML
export async function ensureSourcesInBibliography(
  tr: Transaction,
  sources: BibliographySource[],
  bibliographyFile: BibliographyFile,
  bibManager: BibliographyManager,
  view: EditorView,
  ui: EditorUI,
  server: PandocServer,
): Promise<boolean> {
  // Write entry to a bibliography file if it isn't already present
  await bibManager.load(ui, view.state.doc);

  // See if there is a warning for the selected provider. If there is, we may need to surface
  // that to the user. If there is no provider specified, no need to care about warnings.
  const providers = uniqby(sources, (source: BibliographySource) => source.providerKey).map(source => source.providerKey);

  // Find any providers that have warnings
  const providersWithWarnings = providers.filter(prov => bibManager.warningForProvider(prov));

  // Is this a bibtex bibliography?
  const bibliographyFileExtension = getExtension(bibliographyFile.fullPath);
  const isBibTexBibliography = bibliographyFileExtension === 'bib' || bibliographyFileExtension === 'bibtex';

  // If there is a warning message and we're exporting to BibTeX, show the warning
  // message to the user and confirm that they'd like to proceed. This would ideally
  // know more about the warning type and not have this filter here (e.g. it would just
  // always show the warning)
  let proceedWithInsert = true;
  if (providersWithWarnings.length > 0 && ui.prefs.zoteroUseBetterBibtex() && isBibTexBibliography) {
    const results = await Promise.all<boolean>(providersWithWarnings.map(async withWarning => {
      const warning = bibManager.warningForProvider(withWarning);
      if (warning) {
        return await ui.dialogs.yesNoMessage(warning, "Warning", kAlertTypeWarning, ui.context.translateText("Insert Citation Anyway"), ui.context.translateText("Cancel"));
      } else {
        return true;
      }
    }));
    proceedWithInsert = results.every(result => result);
  }

  if (proceedWithInsert) {
    await Promise.all(
      sources.map(async (source, i) => {
        if (source.id) {
          // Crossref sometimes provides invalid json for some entries. Sanitize it for citeproc
          const cslToWrite = sanitizeForCiteproc(source);

          if (!bibManager.findIdInLocalBibliography(source.id)) {
            const sourceAsBibTex = isBibTexBibliography ? await bibManager.generateBibTeX(ui, source.id, cslToWrite, source.providerKey) : undefined;
            await server.addToBibliography(bibliographyFile.fullPath, bibliographyFile.isProject, source.id, JSON.stringify([cslToWrite]), sourceAsBibTex || '');
          }

          if (!bibliographyFile.isProject) {
            ensureBibliographyFileForDoc(tr, bibliographyFile.displayPath, ui);
          }
        }
      }));
  }
  return proceedWithInsert;
}

export function performCiteCompletionReplacement(tr: Transaction, pos: number, replacement: ProsemirrorNode | string) {

  // perform replacement
  performCompletionReplacement(tr, pos, replacement);

  // find the range of the cite and fixup marks
  const range = getMarkRange(tr.selection.$head, tr.doc.type.schema.marks.cite);
  if (range) {
    encloseInCiteMark(tr, range.from, range.to);
  }
}

export default extension;
