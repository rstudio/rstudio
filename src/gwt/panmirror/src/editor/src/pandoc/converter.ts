/*
 * converter.ts
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

import { Schema, Node as ProsemirrorNode } from 'prosemirror-model';

import {
  PandocEngine,
  PandocTokenReader,
  PandocNodeWriter,
  PandocMarkWriter,
  PandocPreprocessorFn,
  PandocBlockReaderFn,
  PandocCodeBlockFilter,
  PandocPostprocessorFn,
  PandocInlineHTMLReaderFn,
} from '../api/pandoc';

import { pandocFormatWith, PandocFormat, kGfmFormat, kCommonmarkFormat } from '../api/pandoc_format';
import { PandocCapabilities } from '../api/pandoc_capabilities';

import { pandocToProsemirror } from './to_prosemirror';
import { pandocFromProsemirror } from './from_prosemirror';
import { ExtensionManager } from '../extensions';

export interface PandocWriterOptions {
  atxHeaders?: boolean;
  wrapColumn?: boolean | number;
  dpi?: number;
}

export class PandocConverter {
  private readonly schema: Schema;
  private readonly preprocessors: readonly PandocPreprocessorFn[];
  private readonly postprocessors: readonly PandocPostprocessorFn[];
  private readonly readers: readonly PandocTokenReader[];
  private readonly blockReaders: readonly PandocBlockReaderFn[];
  private readonly inlineHTMLReaders: readonly PandocInlineHTMLReaderFn[];
  private readonly codeBlockFilters: readonly PandocCodeBlockFilter[];
  private readonly nodeWriters: readonly PandocNodeWriter[];
  private readonly markWriters: readonly PandocMarkWriter[];
  private readonly pandoc: PandocEngine;
  private readonly pandocCapabilities: PandocCapabilities;

  constructor(
    schema: Schema,
    extensions: ExtensionManager,
    pandoc: PandocEngine,
    pandocCapabilities: PandocCapabilities,
  ) {
    this.schema = schema;

    this.preprocessors = extensions.pandocPreprocessors();
    this.postprocessors = extensions.pandocPostprocessors();
    this.readers = extensions.pandocReaders();
    this.blockReaders = extensions.pandocBlockReaders();
    this.inlineHTMLReaders = extensions.pandocInlineHTMLReaders();
    this.codeBlockFilters = extensions.pandocCodeBlockFilters();
    this.nodeWriters = extensions.pandocNodeWriters();
    this.markWriters = extensions.pandocMarkWriters();

    this.pandoc = pandoc;
    this.pandocCapabilities = pandocCapabilities;
  }

  public async toProsemirror(markdown: string, format: string): Promise<ProsemirrorNode> {
    // adjust format. we always need to *read* raw_html, raw_attribute, and backtick_code_blocks b/c
    // that's how preprocessors hoist content through pandoc into our prosemirror token parser.
    format = this.adjustedFormat(format, ['raw_html', 'raw_attribute', 'backtick_code_blocks']);

    // run preprocessors
    this.preprocessors.forEach(preprocessor => {
      markdown = preprocessor(markdown);
    });

    const ast = await this.pandoc.markdownToAst(markdown, format, []);
    let doc = pandocToProsemirror(
      ast,
      this.schema,
      this.readers,
      this.blockReaders,
      this.inlineHTMLReaders,
      this.codeBlockFilters,
    );

    // run post-processors
    this.postprocessors.forEach(postprocessor => {
      doc = postprocessor(doc);
    });

    // return the doc
    return doc;
  }

  public async fromProsemirror(
    doc: ProsemirrorNode,
    pandocFormat: PandocFormat,
    options: PandocWriterOptions,
  ): Promise<string> {
    // generate pandoc ast
    const output = pandocFromProsemirror(
      doc,
      this.pandocCapabilities.api_version,
      pandocFormat,
      this.nodeWriters,
      this.markWriters,
    );

    // adjust format. we always need to be able to write raw_attribute b/c that's how preprocessors
    // hoist content through pandoc into our prosemirror token parser. since we open this door when
    // reading, users could end up writing raw inlines, and in that case we want them to echo back
    // to the source document just the way they came in.
    let format = this.adjustedFormat(pandocFormat.fullName, ['raw_html', 'raw_attribute']);

    // prepare options
    let pandocOptions: string[] = [];
    if (options.atxHeaders) {
      pandocOptions.push('--atx-headers');
    }
    if (options.dpi) {
      pandocOptions.push('--dpi');
    }
    pandocOptions = pandocOptions.concat(this.wrapColumnOptions(options));

    // format (prefer pipe and grid tables). users can still force the
    // availability of these by adding those format flags but all
    // known markdown variants that support tables also support pipe
    // tables so this seems unlikely to ever be required.
    const disable =
      !format.startsWith(kGfmFormat) && !format.startsWith(kCommonmarkFormat) ? '-simple_tables-multiline_tables' : '';
    format = pandocFormatWith(format, disable, '');

    // render to markdown
    const markdown = await this.pandoc.astToMarkdown(output.ast, format, pandocOptions);

    // normalize newlines (don't know if pandoc uses \r\n on windows)
    return markdown.replace(/\r\n|\n\r|\r/g, '\n');
  }

  private wrapColumnOptions(options: PandocWriterOptions) {
    const pandocOptions: string[] = [];
    if (options.wrapColumn) {
      pandocOptions.push('--wrap=auto');
      pandocOptions.push(`--columns=${options.wrapColumn}`);
    } else {
      pandocOptions.push('--wrap=none');
    }
    return pandocOptions;
  }

  // adjust the specified format (remove options that are never applicable
  // to editing scenarios)
  private adjustedFormat(format: string, extensions: string[]) {
    const kDisabledFormatOptions = '-auto_identifiers-gfm_auto_identifiers';
    let newFormat = pandocFormatWith(format, '', extensions.map(ext => `+${ext}`).join('') + kDisabledFormatOptions);

    // any extension specified needs to not have a - anywhere in the format
    extensions.forEach(ext => {
      newFormat = newFormat.replace('-' + ext, '');
    });

    return newFormat;
  }
}
