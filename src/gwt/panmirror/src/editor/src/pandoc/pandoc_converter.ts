/*
 * pandoc_converter.ts
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

import { Schema, Node as ProsemirrorNode } from 'prosemirror-model';

import {
  PandocServer,
  PandocTokenReader,
  PandocNodeWriter,
  PandocMarkWriter,
  PandocPreprocessorFn,
  PandocBlockReaderFn,
  PandocPostprocessorFn,
  PandocInlineHTMLReaderFn,
  PandocWriterOptions,
  PandocTokensFilterFn,
} from '../api/pandoc';

import { haveTableCellsWithInlineRcode } from '../api/rmd';

import { pandocFormatWith, PandocFormat, kGfmFormat, kCommonmarkFormat } from '../api/pandoc_format';
import { PandocCapabilities } from '../api/pandoc_capabilities';
import { PandocBlockCapsuleFilter, pandocMarkdownWithBlockCapsules } from '../api/pandoc_capsule';

import { ExtensionManager } from '../editor/editor-extensions';

import { pandocToProsemirror } from './pandoc_to_prosemirror';
import { pandocFromProsemirror } from './pandoc_from_prosemirror';

export type PandocLineWrapping = "none" | "column" | "sentence";

export interface PandocToProsemirrorResult {
  doc: ProsemirrorNode;
  line_wrapping: PandocLineWrapping;
  unrecognized: string[];
  unparsed_meta: { [key: string]: any };
}

export class PandocConverter {
  private readonly schema: Schema;
  private readonly preprocessors: readonly PandocPreprocessorFn[];
  private readonly postprocessors: readonly PandocPostprocessorFn[];
  private readonly readers: readonly PandocTokenReader[];
  private readonly tokensFilters: readonly PandocTokensFilterFn[];
  private readonly blockReaders: readonly PandocBlockReaderFn[];
  private readonly inlineHTMLReaders: readonly PandocInlineHTMLReaderFn[];
  private readonly blockCapsuleFilters: readonly PandocBlockCapsuleFilter[];
  private readonly nodeWriters: readonly PandocNodeWriter[];
  private readonly markWriters: readonly PandocMarkWriter[];
  private readonly pandoc: PandocServer;
  private readonly pandocCapabilities: PandocCapabilities;

  constructor(
    schema: Schema,
    extensions: ExtensionManager,
    pandoc: PandocServer,
    pandocCapabilities: PandocCapabilities,
  ) {
    this.schema = schema;

    this.preprocessors = extensions.pandocPreprocessors();
    this.postprocessors = extensions.pandocPostprocessors();
    this.readers = extensions.pandocReaders();
    this.tokensFilters = extensions.pandocTokensFilters();
    this.blockReaders = extensions.pandocBlockReaders();
    this.inlineHTMLReaders = extensions.pandocInlineHTMLReaders();
    this.blockCapsuleFilters = extensions.pandocBlockCapsuleFilters();
    this.nodeWriters = extensions.pandocNodeWriters();
    this.markWriters = extensions.pandocMarkWriters();

    this.pandoc = pandoc;
    this.pandocCapabilities = pandocCapabilities;
  }

  public async toProsemirror(markdown: string, format: PandocFormat): Promise<PandocToProsemirrorResult> {
    // save original markdown (for aligning capsule positions)
    const original = markdown;

    // adjust format. we always need to *read* raw_html, raw_attribute, and backtick_code_blocks b/c
    // that's how preprocessors hoist content through pandoc into our prosemirror token parser.
    // we always need to read with auto_identifiers so we can catch any auto-generated ids 
    // required to fulfill links inside the document (we will strip out heading ids that
    // aren't explicit or a link target using the heading_ids returned with the ast). we also 
    // disable 'smart' b/c that causes pandoc to insert non-breaking spaces before selected
    // abbreviations like e.g. rather, we do our own implementation of 'smart' when we read
    // PandocTokenType.Str from the ast

    // determine type of auto_ids
    const autoIds = format.extensions.gfm_auto_identifiers ? 'gfm_auto_identifiers' : 'auto_identifiers';
    const targetFormat = adjustedFormat(format.fullName, ['raw_html', 'raw_attribute', 'backtick_code_blocks', autoIds], ['smart']);

    // run preprocessors
    this.preprocessors.forEach(preprocessor => {
      markdown = preprocessor(markdown);
    });

    // create source capsules
    this.blockCapsuleFilters.forEach(filter => {
      markdown = pandocMarkdownWithBlockCapsules(original, markdown, filter);
    });

    const ast = await this.pandoc.markdownToAst(markdown, targetFormat, []);
    const result = pandocToProsemirror(
      ast,
      this.schema,
      format.extensions,
      this.readers,
      this.tokensFilters,
      this.blockReaders,
      this.inlineHTMLReaders,
      this.blockCapsuleFilters,
    );

    // run post-processors
    this.postprocessors.forEach(postprocessor => {
      result.doc = postprocessor(result.doc);
    });

    // return the doc
    return result;
  }

  // NOTE: For a plain markdown file, this is the closest we can come to cannonicalizing w/ just pandoc:
  //
  //   pandoc MANUAL.md --to markdown-auto_identifiers-smart -o MANUAL.md --self-contained --atx-headers --wrap=none
  //
  // For R Mardown files, we would need to pull out the Rmd chunks before sending to pandoc.
  //

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
    // to the source document just the way they came in. for writing markdown from pm we don't 
    // ever want to generate auto identifiers so we disable them here. we also disable smart b/c
    // we do this manually above in pandocFromProsemirror (so we can avoid pandoc's insertion of 
    // nbsp's after abbreviations, which is more approriate for final output than editing)
    let format = adjustedFormat(pandocFormat.fullName,
      ['raw_html', 'raw_attribute'],                 // always enable
      ['auto_identifiers', 'gfm_auto_identifiers', 'smart']); // always disable

    // disable selected format options
    format = pandocFormatWith(format, disabledFormatOptions(format, doc), '');

    // prepare pandoc options
    let pandocOptions: string[] = [];
    if (options.atxHeaders) {
      pandocOptions.push('--atx-headers');
    }
    if (options.dpi) {
      pandocOptions.push('--dpi');
    }
    // default to block level references (validate known types)
    let referenceLocation = 'block';
    if (options.references?.location) {
      referenceLocation = ['block', 'section', 'document'].includes(options.references.location)
        ? options.references.location
        : 'block';
    }
    pandocOptions.push(`--reference-location=${referenceLocation}`);

    // references prefix (if any)
    if (options.references?.prefix) {
      pandocOptions.push('--id-prefix', options.references.prefix);
    }

    // provide wrap options
    pandocOptions = pandocOptions.concat(wrapOptions(options));

    // render to markdown
    const markdown = await this.pandoc.astToMarkdown(output.ast, format, pandocOptions);

    // normalize newlines (don't know if pandoc uses \r\n on windows)
    return markdown.replace(/\r\n|\n\r|\r/g, '\n');
  }
}

// adjust the specified format
function adjustedFormat(format: string, extensions: string[], disabled: string[]) {

  let newFormat = pandocFormatWith(format, '',
    extensions.map(ext => `+${ext}`).join('') + disabled.map(ext => `-${ext}`).join('')
  );

  // any extension specified needs to not have a - anywhere in the format
  extensions.forEach(ext => {
    newFormat = newFormat.replace('-' + ext, '');
  });

  return newFormat;
}

function disabledFormatOptions(format: string, doc: ProsemirrorNode) {
  // (prefer pipe and grid tables). users can still force the availability of these by
  // adding those format flags but all known markdown variants that support tables also
  // support pipe tables so this seems unlikely to ever be required.
  let disabledTableTypes = '-simple_tables-multiline_tables';

  // if there are tables with inline R code then disable grid tables (as the inline
  // R code will mess up the column boundaries)
  if (haveTableCellsWithInlineRcode(doc)) {
    disabledTableTypes += '-grid_tables';
  }

  // gfm and commonmark variants don't allow simple/multiline/grid tables (just pipe tables)
  // and it's an error to even include these in the markdown format specifier -- so for
  // these modes we just nix the disabling
  if (format.startsWith(kGfmFormat) || format.startsWith(kCommonmarkFormat)) {
    disabledTableTypes = '';
  }

  // return
  return disabledTableTypes;
}

function wrapOptions(options: PandocWriterOptions) {
  const pandocOptions: string[] = [];
  if (options.wrap) {
    if (options.wrap === 'none' || options.wrap === 'sentence') {
      pandocOptions.push('--wrap=none');
    } else {
      const column = parseInt(options.wrap, 10);
      if (column) {
        pandocOptions.push('--wrap=auto');
        pandocOptions.push(`--columns=${column}`);
      } else {
        pandocOptions.push('--wrap=none');
      }
    }
  } else {
    pandocOptions.push('--wrap=none');
  }
  return pandocOptions;
}
