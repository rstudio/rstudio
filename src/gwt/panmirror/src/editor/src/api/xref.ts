/*
 * xref.ts
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


import { Node as ProsemirrorNode } from 'prosemirror-model';

import { findChildrenByMark } from 'prosemirror-utils';

import { pandocAutoIdentifier } from './pandoc_id';
import { rmdChunkEngineAndLabel } from './rmd';

export interface XRefServer {
  indexForFile: (file: string) => Promise<XRefs>;
  xrefForId: (file: string, id: string) => Promise<XRefs>;
}

export interface XRefs {
  baseDir: string;
  refs: XRef[];
}

export interface XRef {
  file: string;
  type: string;
  id: string;
  suffix: string;
  title?: string;
}

export function xrefKey(xref: XRef) {

  // headings don't include their type in the key
  const key = /^h\d$/.test(xref.type)
    ? xref.id
    // no colon if there is no type
    : xref.type.length > 0 ? `${xref.type}:${xref.id}` : xref.id;

  // return key with suffix
  return key + xref.suffix;
}

export function xrefPosition(doc: ProsemirrorNode, xref: string): number {

  // -1 if not found
  let xrefPos = -1;

  // get type and id
  const xrefInfo = xrefTypeAndId(xref);
  if (xrefInfo) {

    const { type, id } = xrefInfo;
    const locator = xrefPositionLocators[type];
    if (locator) {
      // if this locator finds by mark then look at doc for marks
      if (locator.markType) {
        const schema = doc.type.schema;
        const markType = schema.marks[locator.markType];
        const markedNodes = findChildrenByMark(doc, markType, true);
        markedNodes.forEach(markedNode => {
          // bail if we already found it
          if (xrefPos !== -1) {
            return false;
          }
          // see if we can locate the xref
          if (locator.hasXRef(markedNode.node, id)) {
            xrefPos = markedNode.pos;
          }
        });

      } else if (locator.nodeTypes) {
        // otherwise recursively examine nodes to find the xref
        doc.descendants((node, pos) => {
          // bail if we already found it
          if (xrefPos !== -1) {
            return false;
          }
          // see if we can locate the xref
          if (locator.nodeTypes!.includes(node.type.name) &&
            locator.hasXRef(node, id)) {
            xrefPos = pos;
            return false;
          }
        });
      }




    }

  }

  // return the position
  return xrefPos;
}



function xrefTypeAndId(xref: string) {
  const colonPos = xref.indexOf(':');
  if (colonPos !== -1) {
    return {
      type: xref.substring(0, colonPos),
      id: xref.substring(colonPos + 1)
    };
  } else {
    return null;
  }
}

interface XRefPositionLocator {
  markType?: string;
  nodeTypes?: string[];
  hasXRef: (node: ProsemirrorNode, id: string) => boolean;
}

const xrefPositionLocators: { [key: string]: XRefPositionLocator } = {
  'h1': headingLocator(),
  'h2': headingLocator(),
  'h3': headingLocator(),
  'h4': headingLocator(),
  'h5': headingLocator(),
  'h6': headingLocator(),
  'fig': {
    nodeTypes: ['rmd_chunk'],
    hasXRef: (node: ProsemirrorNode, id: string) => rmdChunkHasXRef(node, 'r', id, /^\{.*[ ,].*fig\.cap\s*=.*\}\s*\n/m)
  },
  'tab': {
    nodeTypes: ['rmd_chunk', 'table_container'],
    hasXRef: (node: ProsemirrorNode, id: string) => {
      if (node.type.name === 'rmd_chunk') {
        return rmdChunkHasXRef(node, 'r', id, /kable\s*\([\s\S]*caption/);
      } else if (node.type.name === 'table_container') {
        const caption = node.child(1);
        const match = caption.textContent.match(/^\s*\(#tab\:([a-zA-Z0-9\/-]+)\)\s*(.*)$/);
        return !!match && match[1].localeCompare(id, undefined, { sensitivity: 'accent' }) === 0;
      } else {
        return false;
      }
    }
  },
  'eq': {
    markType: 'math',
    hasXRef: (node: ProsemirrorNode, id: string) => {
      const match = node.textContent.match(/^.*\(\\#eq:([a-zA-Z0-9\/-]+)\).*$/m);
      return !!match && match[1].localeCompare(id, undefined, { sensitivity: 'accent' }) === 0;
    }
  },
  'thm': thereomLocator('theorem'),
  'lem': thereomLocator('lemma'),
  'cor': thereomLocator('corollary'),
  'prp': thereomLocator('proposition'),
  'cnj': thereomLocator('conjecture'),
  'def': thereomLocator('definition'),
  'exr': thereomLocator('exercise'),
};

function rmdChunkHasXRef(node: ProsemirrorNode, engine: string, label: string, pattern?: RegExp) {
  const chunk = rmdChunkEngineAndLabel(node.textContent);
  const match = node.textContent.match(/^\{([a-zA-Z0-9_]+)[\s,]+([a-zA-Z0-9/-]+)/);
  if (chunk) {
    return chunk.engine.localeCompare(engine, undefined, { sensitivity: 'accent' }) === 0 &&
      chunk.label === label &&
      (!pattern || !!node.textContent.match(pattern));
  } else {
    return false;
  }
}

function headingLocator() {
  return {
    nodeTypes: ['heading'],
    hasXRef: (node: ProsemirrorNode, id: string) => {
      // note we use default pandoc auto id semantics here no matter what the documnet
      // happens to use b/c our xref indexing code also does this (so only ids generated
      // using the 'standard' rules will be in the index)
      return node.attrs.id === id || pandocAutoIdentifier(node.textContent, false) === id;
    }
  };
}

function thereomLocator(engine: string) {
  return {
    nodeTypes: ['rmd_chunk'],
    hasXRef: (node: ProsemirrorNode, id: string) => {
      // look for conventional engine/label
      if (rmdChunkHasXRef(node, engine, id)) {
        return true;

      } else {
        // look for explicit label= syntax
        const match = node.textContent.match(/^\{([a-zA-Z0-9_]+)[\s,]+label\s*=\s*['"]([^"']+)['"].*\}/);
        return !!match &&
          match[1].localeCompare(engine, undefined, { sensitivity: 'accent' }) === 0 &&
          match[2] === id;
      }
    }
  };
}




