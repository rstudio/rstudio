/*
 * pandoc_attr.ts
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

import { NodeSpec, MarkSpec } from 'prosemirror-model';

import { PandocToken, PandocExtensions } from './pandoc';
import { extensionEnabled, extensionIfEnabled, Extension } from './extension';

export const kPandocAttrId = 0;
export const kPandocAttrClasses = 1;
export const kPandocAttrKeyvalue = 2;

const kDataPmPandocAttr = 'data-pm-pandoc-attr';

export const kWidthAttrib = 'width';
export const kHeightAttrib = 'height';
export const kStyleAttrib = 'style';
export const kAlignAttrib = 'align';

export const kCodeBlockAttr = 0;
export const kCodeBlockText = 1;

export const kSpanAttr = 0;
export const kSpanChildren = 1;


export interface PandocAttr {
  id: string;
  classes: string[];
  keyvalue: Array<[string, string]>;
}

export const pandocAttrSpec = {
  id: { default: null },
  classes: { default: [] },
  keyvalue: { default: [] },
};

export function pandocAttrAvailable(attrs: any) {
  return !!attrs.id || (attrs.classes && attrs.classes.length > 0) || (attrs.keyvalue && attrs.keyvalue.length > 0);
}

export function pandocAttrFrom(attrs: any) {
  const pandocAttr: any = {};
  if (attrs.id) {
    pandocAttr.id = attrs.id;
  }
  if (attrs.classes) {
    pandocAttr.classes = attrs.classes;
  }
  if (attrs.keyvalue) {
    pandocAttr.keyvalue = attrs.keyvalue;
  }

  return pandocAttr;
}

export function pandocAttrInSpec(spec: NodeSpec | MarkSpec) {
  const keys = Object.keys((spec.attrs as object) || {});
  return keys.includes('id') && keys.includes('classes') && keys.includes('keyvalue');
}

export function pandocAttrReadAST(tok: PandocToken, index: number) {
  const pandocAttr = tok.c[index];
  return {
    id: pandocAttr[kPandocAttrId] || undefined,
    classes: pandocAttr[kPandocAttrClasses],
    keyvalue: pandocAttr[kPandocAttrKeyvalue],
  };
}

export function pandocAttrToDomAttr(attrs: any, marker = true) {
  // id and class
  const domAttr: any = {};
  if (attrs.id) {
    domAttr.id = attrs.id;
  }
  if (attrs.classes && attrs.classes.length > 0) {
    domAttr.class = attrs.classes.join(' ');
  }

  // keyvalue pairs
  attrs.keyvalue.forEach((keyvalue: [string, string]) => {
    domAttr[keyvalue[0]] = keyvalue[1];
  });

  // marker
  if (marker) {
    domAttr[kDataPmPandocAttr] = '1';
  }

  return domAttr;
}

export function pandocAttrParseDom(el: Element, attrs: { [key: string]: string | null }, forceAttrs = false) {
  // exclude any keys passed to us
  const excludedNames = Object.keys(attrs);

  // if this isn't from a prosemirror pandoc node then include only src and alt
  const includedNames: string[] = [];
  if (!forceAttrs && !el.hasAttribute(kDataPmPandocAttr)) {
    includedNames.push('src', 'alt');
  }

  // read attributes
  const attr: any = {};
  attr.classes = [];
  attr.keyvalue = [];
  el.getAttributeNames().forEach(name => {
    const value: string = el.getAttribute(name) as string;
    // exclude attributes already parsed and prosemirror internal attributes
    if (excludedNames.indexOf(name) === -1 && !name.startsWith('data-pm')) {
      // if we have an include filter then use it
      if (!includedNames.length || includedNames.includes(name)) {
        if (name === 'id') {
          attr.id = value;
        } else if (name === 'class') {
          attr.classes = value
            .split(/\s+/)
            .filter(val => !!val.length)
            .filter(val => !val.startsWith('pm-'));
        } else {
          attr.keyvalue.push([name, value]);
        }
      }
    }
  });
  return attr;
}

export function pandocAttrParseText(attr: string): PandocAttr | null {

  attr = attr.trim();

  let id = '';
  const classes: string[] = [];
  let remainder = '';

  let current = '';
  const resolveCurrent = () => {

    const resolve = current;
    current = '';

    if (resolve.length === 0) {
      return true;
    } else if (resolve.startsWith('#')) {
      if (id.length === 0 && resolve.length > 1) {
        id = resolve.substr(1);
        return true;
      } else {
        return false;
      }
    } else if (resolve.startsWith('.')) {
      if (resolve.length > 1) {
        classes.push(resolve.substr(1));
        return true;
      } else {
        return false;
      }
    } else {
      remainder = resolve;
      return true;
    }
  };

  for (let i = 0; i < attr.length; i++) {
    let inQuotes = false;
    const ch = attr[i];
    inQuotes = ch === '"' ? !inQuotes : inQuotes;
    if (ch !== ' ' && !inQuotes) {
      current += ch;
    } else if (resolveCurrent()) {

      // if we have a remainder then the rest of the string is the remainder
      if (remainder.length > 0) {
        remainder = remainder + attr.substr(i);
        break;
      }

    } else {
      return null;
    }
  }

  if (resolveCurrent()) {
    if (id.length === 0 && classes.length === 0) {
      remainder = attr;
    }
    return {
      id,
      classes,
      keyvalue: remainder.length > 0 ? pandocAttrKeyvalueFromText(remainder, ' ') : []
    };
  } else {
    return null;
  }

}

export function pandocAttrKeyvalueFromText(text: string, separator: ' ' | '\n'): Array<[string, string]> {

  // if the separator is a space then convert unquoted spaces to newline
  if (separator === ' ') {
    let convertedText = '';
    let inQuotes = false;
    for (let i = 0; i < text.length; i++) {
      let ch = text.charAt(i);
      if (ch === '"') {
        inQuotes = !inQuotes;
      } else if (ch === ' ' && !inQuotes) {
        ch = '\n';
      }
      convertedText += ch;
    }
    text = convertedText;
  }

  const lines = text.trim().split('\n');
  return lines.map(line => {
    const parts = line.trim().split('=');
    return [parts[0], (parts[1] || '').replace(/^"/, '').replace(/"$/, '')];
  });
}


export interface AttrKeyvaluePartitioned {
  base: Array<[string, string]>;
  partitioned: Array<[string, string]>;
}

export function attrPartitionKeyvalue(partition: string[], keyvalue: Array<[string, string]>): AttrKeyvaluePartitioned {
  const base = new Array<[string, string]>();
  const partitioned = new Array<[string, string]>();

  keyvalue.forEach(kv => {
    if (partition.includes(kv[0])) {
      partitioned.push(kv);
    } else {
      base.push(kv);
    }
  });

  return {
    base,
    partitioned,
  };
}


export function extensionIfPandocAttrEnabled(extension: Extension) {
  return extensionIfEnabled(extension, kPandocAttrExtensions);
}

export function pandocAttrEnabled(pandocExtensions: PandocExtensions) {
  return extensionEnabled(pandocExtensions, kPandocAttrExtensions);
}

const kPandocAttrExtensions = [
  'link_attributes',
  'mmd_link_attributes',
  'mmd_header_identifiers',
  'header_attributes',
  'fenced_code_attributes',
  'inline_code_attributes',
  'bracketed_spans',
  'native_spans',
  'fenced_divs',
  'native_divs',
];
