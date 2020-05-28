/*
 * editor-extensions.ts
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

import { InputRule } from 'prosemirror-inputrules';
import { Schema } from 'prosemirror-model';
import { Plugin } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { EditorOptions } from '../api/options';
import { EditorUI } from '../api/ui';
import { ProsemirrorCommand } from '../api/command';
import { PandocMark } from '../api/mark';
import { PandocNode, CodeViewOptions } from '../api/node';
import { Extension, ExtensionFn } from '../api/extension';
import { BaseKeyBinding } from '../api/basekeys';
import { AppendTransactionHandler, AppendMarkTransactionHandler } from '../api/transaction';
import { FixupFn } from '../api/fixup';
import {
  PandocTokenReader,
  PandocMarkWriter,
  PandocNodeWriter,
  PandocPreprocessorFn,
  PandocPostprocessorFn,
  PandocBlockReaderFn,
  PandocExtensions,
  PandocInlineHTMLReaderFn,
  PandocTokensFilterFn,
} from '../api/pandoc';
import { PandocBlockCapsuleFilter } from '../api/pandoc_capsule';
import { EditorEvents } from '../api/events';
import { PandocCapabilities } from '../api/pandoc_capabilities';
import { EditorFormat } from '../api/format';
import { markInputRuleFilter } from '../api/input_rule';

// required extensions (base non-customiziable pandoc nodes/marks + core behaviors)
import nodeText from '../nodes/text';
import nodeParagraph from '../nodes/paragraph';
import nodeHeading from '../nodes/heading';
import nodeBlockquote from '../nodes/blockquote';
import nodeCodeBlock from '../nodes/code_block';
import nodeLists from '../nodes/list/list';
import nodeImage from '../nodes/image/image';
import nodeFigure from '../nodes/image/figure';
import nodeHr from '../nodes/hr';
import nodeHardBreak from '../nodes/hard_break';
import nodeNull from '../nodes/null';
import markEm from '../marks/em';
import markStrong from '../marks/strong';
import markCode from '../marks/code';
import markLink from '../marks/link/link';
import behaviorHistory from '../behaviors/history';
import behaviorSelectAll from '../behaviors/select_all';
import behaviorCursor from '../behaviors/cursor';
import behaviorFind from '../behaviors/find';
import behaviorClearFormatting from '../behaviors/clear_formatting';

// behaviors
import behaviorSmarty, { reverseSmartQuotesExtension } from '../behaviors/smarty';
import behaviorAttrDuplicateId from '../behaviors/attr_duplicate_id';
import behaviorTrailingP from '../behaviors/trailing_p';
import behaviorEmptyMark from '../behaviors/empty_mark';
import behaviorOutline from '../behaviors/outline';
import beahviorCodeBlockInput from '../behaviors/code_block_input';
import behaviorPasteText from '../behaviors/paste_text';
import behaviorBottomPadding from '../behaviors/bottom_padding';

// marks
import markStrikeout from '../marks/strikeout';
import markSuperscript from '../marks/superscript';
import markSubscript from '../marks/subscript';
import markSmallcaps from '../marks/smallcaps';
import markQuoted from '../marks/quoted';
import markRawInline from '../marks/raw_inline/raw_inline';
import markRawTex from '../marks/raw_inline/raw_tex';
import markRawHTML from '../marks/raw_inline/raw_html';
import markMath from '../marks/math/math';
import markCite from '../marks/cite/cite';
import markSpan from '../marks/span';
import markXRef from '../marks/xref';
import markHTMLComment from '../marks/raw_inline/raw_html_comment';
import markShortcode from '../marks/shortcode';
import markEmoji from '../marks/emoji';

// nodes
import nodeFootnote from '../nodes/footnote/footnote';
import nodeRawBlock from '../nodes/raw_block';
import nodeYamlMetadata from '../nodes/yaml_metadata/yaml_metadata';
import nodeRmdCodeChunk from '../nodes/rmd_chunk/rmd_chunk';
import nodeDiv from '../nodes/div';
import nodeLineBlock from '../nodes/line_block';
import nodeTable from '../nodes/table/table';
import nodeDefinitionList from '../nodes/definition_list/definition_list';
import nodeShortcodeBlock from '../nodes/shortcode_block';

// extension/plugin factories
import { codeMirrorPlugins } from '../optional/codemirror/codemirror';
import { attrEditExtension } from '../behaviors/attr_edit/attr_edit';

export function initExtensions(
  format: EditorFormat,
  options: EditorOptions,
  ui: EditorUI,
  events: EditorEvents,
  extensions: readonly Extension[] | undefined,
  pandocExtensions: PandocExtensions,
  pandocCapabilities: PandocCapabilities,
): ExtensionManager {
  // create extension manager
  const manager = new ExtensionManager(pandocExtensions, pandocCapabilities, format, options, ui, events);

  // required extensions
  manager.register([
    nodeText,
    nodeParagraph,
    nodeHeading,
    nodeBlockquote,
    nodeLists,
    nodeCodeBlock,
    nodeImage,
    nodeFigure,
    nodeHr,
    nodeHardBreak,
    nodeNull,
    markEm,
    markStrong,
    markCode,
    markLink,
    behaviorHistory,
    behaviorSelectAll,
    behaviorCursor,
    behaviorFind,
    behaviorClearFormatting,
  ]);

  // optional extensions
  manager.register([
    // behaviors
    behaviorSmarty,
    behaviorAttrDuplicateId,
    behaviorTrailingP,
    behaviorEmptyMark,
    behaviorOutline,
    beahviorCodeBlockInput,
    behaviorPasteText,
    behaviorBottomPadding,

    // nodes
    nodeDiv,
    nodeFootnote,
    nodeYamlMetadata,
    nodeRmdCodeChunk,
    nodeTable,
    nodeDefinitionList,
    nodeLineBlock,
    nodeRawBlock,
    nodeShortcodeBlock,

    // marks
    markStrikeout,
    markSuperscript,
    markSubscript,
    markSmallcaps,
    markQuoted,
    markHTMLComment,
    markRawTex,
    markRawHTML,
    markRawInline,
    markMath,
    markCite,
    markSpan,
    markXRef,
    markShortcode,
    markEmoji,
  ]);

  // register external extensions
  if (extensions) {
    manager.register(extensions);
  }

  // additional extensions dervied from other extensions
  // (e.g. extensions that have registered attr editors)
  manager.register([
    attrEditExtension(pandocExtensions, manager.attrEditors()),
    reverseSmartQuotesExtension(manager.pandocMarks()),
  ]);

  // additional plugins derived from extensions
  const plugins: Plugin[] = [];
  if (options.codemirror) {
    plugins.push(...codeMirrorPlugins(manager.codeViews(), ui, options));
  }

  // register plugins
  manager.registerPlugins(plugins);

  // return manager
  return manager;
}

export class ExtensionManager {
  private pandocExtensions: PandocExtensions;
  private pandocCapabilities: PandocCapabilities;
  private format: EditorFormat;
  private options: EditorOptions;
  private ui: EditorUI;
  private events: EditorEvents;
  private extensions: Extension[];

  public constructor(
    pandocExtensions: PandocExtensions,
    pandocCapabilities: PandocCapabilities,
    format: EditorFormat,
    options: EditorOptions,
    ui: EditorUI,
    events: EditorEvents,
  ) {
    this.pandocExtensions = pandocExtensions;
    this.pandocCapabilities = pandocCapabilities;
    this.format = format;
    this.options = options;
    this.ui = ui;
    this.events = events;
    this.extensions = [];
  }

  public register(extensions: ReadonlyArray<Extension | ExtensionFn>): void {
    extensions.forEach(extension => {
      if (typeof extension === 'function') {
        const ext = extension(
          this.pandocExtensions,
          this.pandocCapabilities,
          this.ui,
          this.format,
          this.options,
          this.events,
        );
        if (ext) {
          this.extensions.push(ext);
        }
      } else {
        this.extensions.push(extension);
      }
    });
  }

  public registerPlugins(plugins: Plugin[]) {
    this.register([{ plugins: () => plugins }]);
  }

  public pandocMarks(): readonly PandocMark[] {
    return this.collect(extension => extension.marks);
  }

  public pandocNodes(): readonly PandocNode[] {
    return this.collect(extension => extension.nodes);
  }

  public pandocPreprocessors(): readonly PandocPreprocessorFn[] {
    return this.collectFrom({
      node: node => [node.pandoc.preprocessor],
    });
  }

  public pandocPostprocessors(): readonly PandocPostprocessorFn[] {
    return this.pandocReaders().flatMap(reader => (reader.postprocessor ? [reader.postprocessor] : []));
  }

  public pandocTokensFilters(): readonly PandocTokensFilterFn[] {
    return this.collectFrom({
      node: node => [node.pandoc.tokensFilter],
    });
  }

  public pandocBlockReaders(): readonly PandocBlockReaderFn[] {
    return this.collectFrom({
      node: node => [node.pandoc.blockReader],
    });
  }

  public pandocInlineHTMLReaders(): readonly PandocInlineHTMLReaderFn[] {
    return this.collectFrom({
      mark: mark => [mark.pandoc.inlineHTMLReader],
      node: node => [node.pandoc.inlineHTMLReader],
    });
  }

  public pandocBlockCapsuleFilters(): readonly PandocBlockCapsuleFilter[] {
    return this.collectFrom({
      node: node => [node.pandoc.blockCapsuleFilter],
    });
  }

  public pandocReaders(): readonly PandocTokenReader[] {
    return this.collectFrom({
      mark: mark => mark.pandoc.readers,
      node: node => node.pandoc.readers ?? [],
    });
  }

  public pandocMarkWriters(): readonly PandocMarkWriter[] {
    return this.collectFrom({
      mark: mark => [{ name: mark.name, ...mark.pandoc.writer }],
    });
  }

  public pandocNodeWriters(): readonly PandocNodeWriter[] {
    return this.collectFrom({
      node: node => {
        return node.pandoc.writer ? [{ name: node.name, write: node.pandoc.writer! }] : [];
      },
    });
  }

  public commands(schema: Schema, ui: EditorUI): readonly ProsemirrorCommand[] {
    return this.collect<ProsemirrorCommand>(extension => extension.commands?.(schema, ui));
  }

  public codeViews() {
    const views: { [key: string]: CodeViewOptions } = {};
    this.pandocNodes().forEach((node: PandocNode) => {
      if (node.code_view) {
        views[node.name] = node.code_view;
      }
    });
    return views;
  }

  public attrEditors() {
    return this.collectFrom({
      node: node => [node.attr_edit?.()],
    });
  }

  public baseKeys(schema: Schema): readonly BaseKeyBinding[] {
    return this.collect(extension => extension.baseKeys?.(schema));
  }

  public appendTransactions(schema: Schema): readonly AppendTransactionHandler[] {
    return this.collect(extension => extension.appendTransaction?.(schema));
  }

  public appendMarkTransactions(schema: Schema): readonly AppendMarkTransactionHandler[] {
    return this.collect(extension => extension.appendMarkTransaction?.(schema));
  }

  public plugins(schema: Schema, ui: EditorUI): readonly Plugin[] {
    return this.collect(extension => extension.plugins?.(schema, ui));
  }

  public fixups(schema: Schema, view: EditorView): readonly FixupFn[] {
    return this.collect(extension => extension.fixups?.(schema, view));
  }

  // NOTE: return value not readonly b/c it will be fed directly to a
  // Prosemirror interface that doesn't take readonly
  public inputRules(schema: Schema): InputRule[] {
    const markFilter = markInputRuleFilter(schema, this.pandocMarks());
    return this.collect<InputRule>(extension => extension.inputRules?.(schema, markFilter));
  }

  private collect<T>(collector: (extension: Extension) => readonly T[] | undefined) {
    return this.collectFrom({
      extension: extension => collector(extension) ?? [],
    });
  }

  /**
   * Visits extensions in order of registration, providing optional callbacks
   * for extension, mark, and node. The return value of callbacks should be
   * arrays of (T | undefined | null); these will all be concatenated together,
   * with the undefined and nulls filtered out.
   *
   * @param visitor Object containing callback methods for the different
   * extension parts.
   */
  private collectFrom<T>(visitor: {
    extension?: (extension: Extension) => ReadonlyArray<T | undefined | null>;
    mark?: (mark: PandocMark) => ReadonlyArray<T | undefined | null>;
    node?: (node: PandocNode) => ReadonlyArray<T | undefined | null>;
  }): T[] {
    const results: Array<T | undefined | null> = [];

    this.extensions.forEach(extension => {
      if (visitor.extension) {
        results.push(...visitor.extension(extension));
      }
      if (visitor.mark && extension.marks) {
        results.push(...extension.marks.flatMap(visitor.mark));
      }
      if (visitor.node && extension.nodes) {
        results.push(...extension.nodes.flatMap(visitor.node));
      }
    });

    return results.filter(value => typeof value !== 'undefined' && value !== null) as T[];
  }
}
