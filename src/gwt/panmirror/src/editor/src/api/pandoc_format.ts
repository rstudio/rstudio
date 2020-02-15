/*
 * pandoc_format.ts
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

import { EditorState } from "prosemirror-state";

import { PandocEngine, PandocExtensions } from "./pandoc";

export interface PandocFormat {
  baseName: string;
  fullName: string;
  extensions: PandocExtensions;
  warnings: PandocFormatWarnings;
}

export interface PandocFormatWarnings {
  invalidFormat: string;
  invalidOptions: string[];
}

export interface PandocFormatComment {
  mode?: string;
  extensions?: string;
  fillColumn?: number;
}

export async function resolvePandocFormat(pandoc: PandocEngine, format: string) {
  // setup warnings
  let warnings: PandocFormatWarnings = { invalidFormat: '', invalidOptions: [] };

  // split out base format from options
  const split = splitFormat(format);
  let baseName = split.format;
  let options = split.options;

  // validate the base format (fall back to markdown if it's not known)
  if (
    ![
      'markdown',
      'markdown_phpextra',
      'markdown_github',
      'markdown_mmd',
      'markdown_strict',
      'gfm',
      'commonmark',
    ].includes(baseName)
  ) {
    warnings.invalidFormat = baseName;
    baseName = 'markdown';
  }

  // format options we will be building
  let formatOptions: string;

  // if the base format is commonmark or gfm then it's expressed as a set of
  // deltas on top of markdown
  let validOptions: string = '';
  if (['gfm', 'commonmark'].includes(baseName)) {
    // query for available options then disable them all by default
    formatOptions = await pandoc.listExtensions('markdown');
    formatOptions = formatOptions.replace(/\+/g, '-');

    // layer on gfm or commonmark
    let extraOptions = (validOptions = await pandoc.listExtensions(baseName));
    formatOptions = formatOptions + extraOptions;
  } else {
    // query for format options
    formatOptions = validOptions = await pandoc.listExtensions(baseName);
  }

  // active pandoc extensions
  const pandocExtensions: { [key: string]: boolean } = {};

  // first parse extensions for format
  parseExtensions(formatOptions).forEach(option => {
    pandocExtensions[option.name] = option.enabled;
  });

  // now parse extensions for user options (validate and build format name)
  let validOptionNames = parseExtensions(validOptions).map(option => option.name);
  let fullName = baseName;
  parseExtensions(options).forEach(option => {
    // validate that the option is valid
    if (validOptionNames.includes(option.name)) {
      // add option
      fullName += (option.enabled ? '+' : '-') + option.name;
      pandocExtensions[option.name] = option.enabled;
    } else {
      warnings.invalidOptions.push(option.name);
    }
  });

  // return format name, enabled extensiosn, and warnings
  return {
    baseName,
    fullName,
    extensions: (pandocExtensions as unknown) as PandocExtensions,
    warnings,
  };
}

function parseExtensions(options: string) {
  // remove any linebreaks
  options = options.split('\n').join();

  // parse into separate entries
  const extensions: Array<{ name: string; enabled: boolean }> = [];
  const re = /([+-])([a-z_]+)/g;
  let match = re.exec(options);
  while (match) {
    extensions.push({ name: match[2], enabled: match[1] === '+' });
    match = re.exec(options);
  }

  return extensions;
}

export function pandocFormatWith(format: string, prepend: string, append: string) {
  const split = splitFormat(format);
  return `${split.format}${prepend}${split.options}${append}`;
}


export function pandocFormatFromCode(code: string) {
  const variables = {
    mode: 'markdown',
    extensions: '',
    ...pandocFormatCommentFromCode(code)
  };
  return pandocFormatWith(variables.mode, '', variables.extensions);
}

export function pandocFormatCommentFromCode(code: string) : PandocFormatComment {
  const magicCommentRegEx = /^<!--\s+-\*-([\s\S]*?)-\*-\s+-->\s*$/m;
  const keyValueRegEx = /^([^:]+):\s*(.*)$/;
  const match = code.match(magicCommentRegEx);
  if (match) {
    const comment = match[1];
    // split into semicolons
    const fields = comment.split(/\s*;\s/).map(field => field.trim());
    const variables: { [key: string]: string } = {};
    fields.forEach(field => {
      const keyValueMatch = field.match(keyValueRegEx);
      if (keyValueMatch) {
        variables[keyValueMatch[1].trim()] = keyValueMatch[2].trim();
      }
    });    
    const formatComment : PandocFormatComment = {};
    if (variables.mode) {
      formatComment.mode = variables.mode;
    }
    if (variables.extensions) {
      formatComment.extensions = variables.extensions;
    }
    if (variables['fill-column']) {
      formatComment.fillColumn = parseInt(variables['fill-column']) || undefined;
    }
    return formatComment;
  } else {
    return {};
  }
}

export function pandocFormatCommentFromState(state: EditorState) : PandocFormatComment {
  let comment: PandocFormatComment = {};
  let foundFirstRawInline = false;
  state.doc.descendants((node, pos) => {
    // don't search once we've found our target
    if (foundFirstRawInline) {
      return false;
    }
    // if it's a text node with a raw-inline then scan it for the format comment
    if (node.isText && state.schema.marks.raw_inline.isInSet(node.marks)) {
      foundFirstRawInline = true;
      comment = pandocFormatCommentFromCode(node.textContent);
      return false;
    } else {
      return true;
    }
  });
  return comment;
}

export function splitFormat(format: string) {
    // split out base format from options
    let optionsPos = format.indexOf('-');
    if (optionsPos === -1) {
      optionsPos = format.indexOf('+');
    }
    const base = optionsPos === -1 ? format : format.substr(0, optionsPos);
    const options = optionsPos === -1 ? '' : format.substr(optionsPos);
    return {
      format: base,
      options,
    };
  }
  
  