/*
 * converter.ts
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

import { Schema, Node as ProsemirrorNode } from 'prosemirror-model';

import {
  PandocEngine,
  PandocTokenReader,
  PandocNodeWriter,
  PandocApiVersion,
  PandocMarkWriter,
  PandocAst,
  PandocPreprocessorFn,
  PandocBlockReaderFn,
  PandocCodeBlockFilter,
  PandocAstOutputFilter,
  PandocMarkdownOutputFilter,
  pandocFormatWith,
  PandocPostprocessorFn,
} from '../api/pandoc';

import { pandocToProsemirror } from './to_prosemirror';
import { pandocFromProsemirror } from './from_prosemirror';
import { ExtensionManager } from '../extensions';

export interface PandocConverterOptions {
  reader: {};
  writer: {
    atxHeaders?: boolean;
    wrapColumn?: boolean | number;
  };
}

export class PandocConverter {
  private readonly schema: Schema;
  private readonly preprocessors: readonly PandocPreprocessorFn[];
  private readonly postprocessors: readonly PandocPostprocessorFn[];
  private readonly readers: readonly PandocTokenReader[];
  private readonly blockReaders: readonly PandocBlockReaderFn[];
  private readonly codeBlockFilters: readonly PandocCodeBlockFilter[];
  private readonly nodeWriters: readonly PandocNodeWriter[];
  private readonly markWriters: readonly PandocMarkWriter[];
  private readonly astOutputFilters: readonly PandocAstOutputFilter[];
  private readonly markdownOutputFilters: readonly PandocMarkdownOutputFilter[];
  private readonly pandoc: PandocEngine;
  private readonly format: string;
  private readonly options: PandocConverterOptions;

  private apiVersion: PandocApiVersion | null;

  constructor(
    schema: Schema,
    extensions: ExtensionManager,
    pandoc: PandocEngine,
    format: string,
    options: PandocConverterOptions,
  ) {
    this.schema = schema;

    this.preprocessors = extensions.pandocPreprocessors();
    this.postprocessors = extensions.pandocPostprocessors();
    this.readers = extensions.pandocReaders();
    this.blockReaders = extensions.pandocBlockReaders();
    this.codeBlockFilters = extensions.pandocCodeBlockFilters();
    this.nodeWriters = extensions.pandocNodeWriters();
    this.markWriters = extensions.pandocMarkWriters();
    this.astOutputFilters = extensions.pandocAstOutputFilters();
    this.markdownOutputFilters = extensions.pandocMarkdownOutputFilters();

    // markdown format (for editing it never makes sense to automaticallly inject identifiers)
    const kDisabledFormatOptions = '-auto_identifiers-gfm_auto_identifiers';
    this.format = pandocFormatWith(format, '', kDisabledFormatOptions);

    this.pandoc = pandoc;
    this.options = options;
    this.apiVersion = null;
  }

  public async toProsemirror(markdown: string): Promise<ProsemirrorNode> {
    // run preprocessors
    this.preprocessors.forEach(preprocessor => {
      markdown = preprocessor(markdown);
    });

    const ast = await this.pandoc.markdownToAst(markdown, this.format, []);
    this.apiVersion = ast['pandoc-api-version'];
    let doc = pandocToProsemirror(ast, this.schema, this.readers, this.blockReaders, this.codeBlockFilters);

    // run post-processors
    this.postprocessors.forEach(postprocessor => {
      doc = postprocessor(doc);
    });

    // return the doc
    return doc;
  }

  public async fromProsemirror(doc: ProsemirrorNode): Promise<string> {
    if (!this.apiVersion) {
      throw new Error('API version not available (did you call toProsemirror first?)');
    }

    // generate pandoc ast
    const output = pandocFromProsemirror(doc, this.apiVersion, this.nodeWriters, this.markWriters);

    // run ast filters
    let ast = await this.applyAstOutputFilters(output.ast);

    // prepare options
    let options: string[] = [];
    if (this.options.writer.atxHeaders) {
      options.push('--atx-headers');
    }
    options = options.concat(this.wrapColumnOptions());

    // format (prefer pipe and grid tables). users can still force the
    // availability of these by adding those format flags but all
    // known markdown variants that support tables also support pipe
    // tables so this seems unlikely to ever be required.
    const disable =
      !this.format.startsWith('gfm') && !this.format.startsWith('commonmark') ? '-simple_tables-multiline_tables' : '';
    let format = pandocFormatWith(this.format, disable, '');

    // render to markdown
    let markdown = await this.pandoc.astToMarkdown(ast, format, options);

    // apply markdown filters
    markdown = this.applyMarkdownOutputFilters(markdown);

    // substitue raw markdown blocks
    Object.keys(output.rawMarkdown).forEach(id => {
      markdown = markdown.replace(id, output.rawMarkdown[id]);
    });

    // return markdown
    return markdown;
  }

  private async applyAstOutputFilters(ast: PandocAst) {
    let filteredAst = ast;

    for (let i = 0; i < this.astOutputFilters.length; i++) {
      const filter = this.astOutputFilters[i];
      filteredAst = await filter(filteredAst, {
        astToMarkdown: (ast: PandocAst, format_options: string) => {
          return this.pandoc.astToMarkdown(ast, this.format + format_options, []);
        },
        markdownToAst: (markdown: string) => {
          return this.pandoc.markdownToAst(markdown, this.format, this.wrapColumnOptions());
        },
      });
    }

    return filteredAst;
  }

  private applyMarkdownOutputFilters(markdown: string) {
    this.markdownOutputFilters.forEach(filter => {
      markdown = filter(markdown);
    });
    return markdown;
  }

  private wrapColumnOptions() {
    const options: string[] = [];
    if (this.options.writer.wrapColumn) {
      options.push('--wrap=auto');
      options.push(`--columns=${this.options.writer.wrapColumn}`);
    } else {
      options.push('--wrap=none');
    }
    return options;
  }
}
