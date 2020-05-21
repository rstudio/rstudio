/*
 * yaml.ts
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

import { findTopLevelBodyNodes } from './node';

import yaml from 'js-yaml';

export function yamlMetadataNodes(doc: ProsemirrorNode) {
  return findTopLevelBodyNodes(doc, isYamlMetadataNode);
}

export function isYamlMetadataNode(node: ProsemirrorNode) {
  return node.type === node.type.schema.nodes.yaml_metadata;
}

export const kYamlBlocksRegex = /^([\t >]*)(---[ \t]*\n(?![ \t]*\n)[\W\w]*?\n[\t >]*(?:---|\.\.\.))([ \t]*)$/gm;

export function firstYamlBlock(code: string): { [key: string]: any } | null {
  kYamlBlocksRegex.lastIndex = 0;
  const match = kYamlBlocksRegex.exec(code);
  kYamlBlocksRegex.lastIndex = 0;
  if (match) {
    // strip trailing marker (--- or ...)
    const yamlCode = match[2].replace(/[\t >]*(?:---|\.\.\.)$/, '');
    try {
      const yamlParsed = yaml.safeLoad(yamlCode, {
        onWarning: logException,
      });
      if (typeof yamlParsed === 'object') {
        return yamlParsed;
      } else {
        return null;
      }
    } catch (e) {
      logException(e);
      return null;
    }
  } else {
    return null;
  }
}

function logException(e: Error) {
  // TODO: log exceptions (we don't want to use console.log in production code, so this would
  // utilize some sort of external logging facility)
}
