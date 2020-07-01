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
import { pandocAutoIdentifier } from './pandoc_id';

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
  title: string;
}

export function xrefKey(xref: XRef) {
  return xref.type.length > 0 ? `${xref.type}:${xref.id}` : xref.id;
}

export function xrefPosition(doc: ProsemirrorNode, xref: string): number {

  // alias schema
  const schema = doc.type.schema;

  // get type and id
  const { type, id } = xrefTypeAndId(xref);

  // search all descendents recursively for the xref
  let xrefPos = -1;
  doc.descendants((node, pos) => {

    // bail if we already found it
    if (xrefPos !== -1) {
      return false;
    }

    // see if we have a locator for this type that can handle this node type
    const locator = xrefPositionLocators[type];
    if (locator && locator.nodeTypes.includes(node.type.name)) {
      if (locator.hasXRef(node, id)) {
        xrefPos = pos;
        return false;
      }
    }
  });

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
    return {
      type: 'heading',
      id: xref
    };
  }
}

interface XRefPositionLocator {
  nodeTypes: string[];
  hasXRef: (node: ProsemirrorNode, id: string) => boolean;
}

const xrefPositionLocators: { [key: string]: XRefPositionLocator } = {
  'heading': {
    nodeTypes: ['heading'],
    hasXRef: (node: ProsemirrorNode, id: string) => {
      return node.attrs.id === id || pandocAutoIdentifier(node.textContent) === id;
    }
  },
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
  'thm': thereomLocator('theorem'),
  'lem': thereomLocator('lemma'),
  'cor': thereomLocator('corollary'),
  'prp': thereomLocator('proposition'),
  'cnj': thereomLocator('conjecture'),
  'def': thereomLocator('definition'),
  'exr': thereomLocator('exercise'),
};

function rmdChunkHasXRef(node: ProsemirrorNode, engine: string, label: string, pattern?: RegExp) {
  const match = node.textContent.match(/^\{([a-zA-Z0-9_]+)[\s,]+([a-zA-Z0-9/-]+)/);
  if (match) {
    return match[1].localeCompare(engine, undefined, { sensitivity: 'accent' }) === 0 &&
      match[2] === label &&
      (!pattern || !!node.textContent.match(pattern));
  } else {
    return false;
  }
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




