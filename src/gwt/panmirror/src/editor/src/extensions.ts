/*
 * extensions.ts
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

import { InputRule } from 'prosemirror-inputrules';
import { Schema } from 'prosemirror-model';
import { Plugin, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { EditorOptions } from './api/options';
import { EditorUI } from './api/ui';
import { ProsemirrorCommand } from './api/command';
import { PandocMark } from './api/mark';
import { PandocNode, CodeViewOptions } from './api/node';
import { Extension, ExtensionFn } from './api/extension';
import { BaseKeyBinding } from './api/basekeys';
import { AppendTransactionHandler, AppendMarkTransactionHandler } from './api/transaction';
import {
  PandocTokenReader,
  PandocMarkWriter,
  PandocNodeWriter,
  PandocPreprocessorFn,
  PandocPostprocessorFn,
  PandocBlockReaderFn,
  PandocCodeBlockFilter,
  PandocAstOutputFilter,
  PandocMarkdownOutputFilter,
  PandocExtensions,
} from './api/pandoc';

// required extensions (base non-customiziable pandoc nodes/marks + core behaviors)
import nodeText from './nodes/text';
import nodeParagraph from './nodes/paragraph';
import nodeHeading from './nodes/heading';
import nodeBlockquote from './nodes/blockquote';
import nodeCodeBlock from './nodes/code_block';
import nodeLists from './nodes/list/list';
import nodeImage from './nodes/image/image';
import nodeHr from './nodes/hr';
import nodeHardBreak from './nodes/hard_break';
import nodeNull from './nodes/null';
import markEm from './marks/em';
import markStrong from './marks/strong';
import markCode from './marks/code';
import markLink from './marks/link/link';
import behaviorHistory from './behaviors/history';
import behaviorSelectAll from './behaviors/select_all';
import behaviorCursor from './behaviors/cursor';
import behaviorFind from './behaviors/find';

// behaviors

import behaviorSmarty from './behaviors/smarty';
import behaviorAttrEdit from './behaviors/attr_edit';
import behaviorAttrDuplicateId from './behaviors/attr_duplicate_id';
import behaviorTrailingP from './behaviors/trailing_p';
import behaviorOutline from './behaviors/outline';
import behaviorBraceMatch from './behaviors/bracematch';
import behaviorTextFocus from './behaviors/text_focus';

// marks

import markStrikeout from './marks/strikeout';
import markSuperscript from './marks/superscript';
import markSubscript from './marks/subscript';
import markSmallcaps from './marks/smallcaps';
import markQuoted from './marks/quoted';
import markRawInline from './marks/raw_inline/raw_inline';
import markMath from './marks/math/math';
import markCite from './marks/cite/cite';
import markSpan from './marks/span';

// nodes
import nodeFootnote from './nodes/footnote/footnote';
import nodeRawBlock from './nodes/raw_block';
import nodeYamlMetadata from './nodes/yaml_metadata/yaml_metadata';
import nodeRmdCodeChunk from './nodes/rmd_chunk/rmd_chunk';
import nodeFigure from './nodes/image/figure';
import nodeDiv from './nodes/div';
import nodeLineBlock from './nodes/line_block';
import nodeTable from './nodes/table/table';
import nodeDefinitionList from './nodes/definition_list/definition_list';

import { codeMirrorPlugins } from './optional/codemirror/codemirror';

export function initExtensions(
  options: EditorOptions,
  extensions: readonly Extension[] | undefined,
  pandocExtensions: PandocExtensions,
): ExtensionManager {
  // create extension manager
  const manager = new ExtensionManager(options, pandocExtensions);

  // required extensions
  manager.register([
    nodeText,
    nodeParagraph,
    nodeHeading,
    nodeBlockquote,
    nodeCodeBlock,
    nodeLists,
    nodeImage,
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
  ]);

  // optional extensions
  manager.register([
    // behaviors
    behaviorSmarty,
    behaviorAttrEdit,
    behaviorAttrDuplicateId,
    behaviorTrailingP,
    behaviorOutline,
    behaviorBraceMatch,
    behaviorTextFocus,

    // marks
    markStrikeout,
    markSuperscript,
    markSubscript,
    markSmallcaps,
    markQuoted,
    markRawInline,
    markMath,
    markCite,
    markSpan,

    // nodes
    nodeDiv,
    nodeFootnote,
    nodeFigure,
    nodeRawBlock,
    nodeYamlMetadata,
    nodeRmdCodeChunk,
    nodeTable,
    nodeDefinitionList,
    nodeLineBlock,
  ]);

  // optional codemirror embedded editor
  if (options.codemirror) {
    manager.register([{ plugins: () => codeMirrorPlugins(manager.codeViews()) }]);
  }

  // register external extensions
  if (extensions) {
    manager.register(extensions);
  }

  // return manager
  return manager;
}

export class ExtensionManager {
  private options: EditorOptions;
  private pandocExtensions: PandocExtensions;
  private extensions: Extension[];

  public constructor(options: EditorOptions, pandocExtensions: PandocExtensions) {
    this.options = options;
    this.pandocExtensions = pandocExtensions;
    this.extensions = [];
  }

  public register(extensions: ReadonlyArray<Extension | ExtensionFn>): void {
    extensions.forEach(extension => {
      if (typeof extension === 'function') {
        const ext = extension(this.pandocExtensions, this.options);
        if (ext) {
          this.extensions.push(ext);
        }
      } else {
        this.extensions.push(extension);
      }
    });
  }

  public pandocMarks(): readonly PandocMark[] {
    return this.collect<PandocMark>((extension: Extension) => extension.marks);
  }

  public pandocNodes(): readonly PandocNode[] {
    return this.collect<PandocNode>((extension: Extension) => extension.nodes);
  }

  public pandocPreprocessors(): readonly PandocPreprocessorFn[] {
    const preprocessors: PandocPreprocessorFn[] = [];

    this.pandocNodes().forEach((node: PandocNode) => {
      if (node.pandoc.preprocessor) {
        preprocessors.push(node.pandoc.preprocessor);
      }
      if (node.pandoc.codeBlockFilter) {
        preprocessors.push(node.pandoc.codeBlockFilter.preprocessor);
      }
    });

    return preprocessors;
  }

  public pandocPostprocessors(): readonly PandocPostprocessorFn[] {
    const postprocessors: PandocPostprocessorFn[] = [];

    this.pandocReaders().forEach((reader: PandocTokenReader) => {
      if (reader.postprocessor) {
        postprocessors.push(reader.postprocessor);
      }
    });

    return postprocessors;
  }

  public pandocBlockReaders(): readonly PandocBlockReaderFn[] {
    const blockReaders: PandocBlockReaderFn[] = [];
    this.pandocNodes().forEach((node: PandocNode) => {
      if (node.pandoc.blockReader) {
        blockReaders.push(node.pandoc.blockReader);
      }
    });
    return blockReaders;
  }

  public pandocCodeBlockFilters(): readonly PandocCodeBlockFilter[] {
    const codeBlockFilters: PandocCodeBlockFilter[] = [];
    this.pandocNodes().forEach((node: PandocNode) => {
      if (node.pandoc.codeBlockFilter) {
        codeBlockFilters.push(node.pandoc.codeBlockFilter);
      }
    });
    return codeBlockFilters;
  }

  public pandocReaders(): readonly PandocTokenReader[] {
    const readers: PandocTokenReader[] = [];
    this.pandocMarks().forEach((mark: PandocMark) => {
      readers.push(...mark.pandoc.readers);
    });
    this.pandocNodes().forEach((node: PandocNode) => {
      if (node.pandoc.readers) {
        readers.push(...node.pandoc.readers);
      }
    });
    return readers;
  }

  public pandocMarkWriters(): readonly PandocMarkWriter[] {
    return this.pandocMarks().map((mark: PandocMark) => {
      return {
        name: mark.name,
        ...mark.pandoc.writer,
      };
    });
  }

  public pandocNodeWriters(): readonly PandocNodeWriter[] {
    const writers: PandocNodeWriter[] = [];
    this.pandocNodes().forEach((node: PandocNode) => {
      if (node.pandoc.writer) {
        writers.push({
          name: node.name,
          write: node.pandoc.writer,
        });
      }
    });
    return writers;
  }

  public pandocAstOutputFilters(): readonly PandocAstOutputFilter[] {
    const filters: PandocAstOutputFilter[] = [];
    this.pandocMarks().forEach((mark: PandocMark) => {
      if (mark.pandoc.astOutputFilter) {
        filters.push(mark.pandoc.astOutputFilter);
      }
    });
    this.pandocNodes().forEach((node: PandocNode) => {
      if (node.pandoc.astOutputFilter) {
        filters.push(node.pandoc.astOutputFilter);
      }
    });
    return filters;
  }

  public pandocMarkdownOutputFilters(): readonly PandocMarkdownOutputFilter[] {
    const filters: PandocMarkdownOutputFilter[] = [];
    this.pandocMarks().forEach((mark: PandocMark) => {
      if (mark.pandoc.markdownOutputFilter) {
        filters.push(mark.pandoc.markdownOutputFilter);
      }
    });
    this.pandocNodes().forEach((node: PandocNode) => {
      if (node.pandoc.markdownOutputFilter) {
        filters.push(node.pandoc.markdownOutputFilter);
      }
    });
    return filters;
  }

  public commands(schema: Schema, ui: EditorUI, mac: boolean): readonly ProsemirrorCommand[] {
    return this.collect<ProsemirrorCommand>((extension: Extension) => {
      if (extension.commands) {
        return extension.commands(schema, ui, mac);
      } else {
        return undefined;
      }
    });
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

  public baseKeys(schema: Schema) {
    return this.collect<BaseKeyBinding>((extension: Extension) => {
      if (extension.baseKeys) {
        return extension.baseKeys(schema);
      } else {
        return undefined;
      }
    });
  }

  public appendTransactions(schema: Schema) {
    return this.collect<AppendTransactionHandler>((extension: Extension) => {
      if (extension.appendTransaction) {
        return extension.appendTransaction(schema);
      } else {
        return undefined;
      }
    });
  }

  public appendMarkTransactions(schema: Schema) {
    return this.collect<AppendMarkTransactionHandler>((extension: Extension) => {
      if (extension.appendMarkTransaction) {
        return extension.appendMarkTransaction(schema);
      } else {
        return undefined;
      }
    });
  }

  public plugins(schema: Schema, ui: EditorUI, mac: boolean): readonly Plugin[] {
    return this.collect<Plugin>((extension: Extension) => {
      if (extension.plugins) {
        return extension.plugins(schema, ui, mac);
      } else {
        return undefined;
      }
    });
  }

  public layoutFixups(schema: Schema, view: EditorView) {
    return this.collect<(tr: Transaction) => Transaction>((extension: Extension) => {
      if (extension.layoutFixups) {
        return extension.layoutFixups(schema, view);
      } else {
        return undefined;
      }
    });
  }

  // NOTE: return value not readonly b/c it will be fed directly to a
  // Prosemirror interface that doesn't take readonly
  public inputRules(schema: Schema): InputRule[] {
    return this.collect<InputRule>((extension: Extension) => {
      if (extension.inputRules) {
        return extension.inputRules(schema);
      } else {
        return undefined;
      }
    });
  }

  private collect<T>(collector: (extension: Extension) => readonly T[] | undefined) {
    let items: T[] = [];
    this.extensions.forEach(extension => {
      const collected: readonly T[] | undefined = collector(extension);
      if (collected !== undefined) {
        items = items.concat(collected);
      }
    });
    return items;
  }
}
