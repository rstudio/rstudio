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
import { NodeWithPos } from 'prosemirror-utils';
import { EditorView } from 'prosemirror-view';

import yaml from 'js-yaml';

import { findTopLevelBodyNodes } from './node';
import { logException } from './log';

export const kYamlMetadataTitleRegex = /\ntitle:(.*)\n/;

// return yaml front matter (w/o enclosing --)
export function yamlFrontMatter(doc: ProsemirrorNode) {
  const firstYaml = firstYamlNode(doc);
  if (firstYaml) {
    return stripYamlDelimeters(firstYaml.node.textContent);
  } else {
    return '';
  }
}

// set yaml front matter (w/o enclosing ---)
export function applyYamlFrontMatter(view: EditorView, yamlText: string) {
  const schema = view.state.schema;
  const updatedYaml = `---\n${yamlText}---`;
  const updatedYamlNode = schema.nodes.yaml_metadata.createAndFill({}, schema.text(updatedYaml));
  const tr = view.state.tr;
  const firstYaml = firstYamlNode(view.state.doc);
  if (firstYaml) {
    tr.replaceRangeWith(firstYaml.pos, firstYaml.pos + firstYaml.node.nodeSize, updatedYamlNode);
  } else {
    tr.insert(1, updatedYamlNode);
  }
  view.dispatch(tr);
}

export function yamlMetadataNodes(doc: ProsemirrorNode) {
  return findTopLevelBodyNodes(doc, isYamlMetadataNode);
}

export function isYamlMetadataNode(node: ProsemirrorNode) {
  return node.type === node.type.schema.nodes.yaml_metadata;
}

export function titleFromYamlMetadataNode(node: ProsemirrorNode) {
  const titleMatch = node.textContent.match(kYamlMetadataTitleRegex);
  if (titleMatch) {
    let title = titleMatch[1].trim();
    title = title.replace(/^["']|["']$/g, '');
    title = title.replace(/\\"/g, '"');
    title = title.replace(/''/g, "'");
    return title;
  } else {
    return null;
  }
}

export function valueFromYamlText(name: string, yamlText: string) {
  // Must start and end with either a new line or the start/end of line
  const yamlMetadataNameValueRegex = new RegExp(`(?:\\n|^)${name}:(.*)(?:\\n|$)`);

  // Find the name and value
  const valueMatch = yamlText.match(yamlMetadataNameValueRegex);
  if (valueMatch) {
    // Read the matched value
    const valueStr = valueMatch[1].trim();

    // Parse the value (could be string, array, etc...)
    const value = parseYaml(valueStr);
    return value;
  } else {
    return null;
  }
}

const kFirstYamlBlockRegex = /\s*---[ \t]*\n(?![ \t]*\n)([\W\w]*?)\n[\t >]*(?:---|\.\.\.)[ \t]*/m;

export function firstYamlBlock(code: string): { [key: string]: any } | null {
  const match = code.match(kFirstYamlBlockRegex);
  if (match && match.index === 0) {
    const yamlCode = match[1];
    const yamlParsed = parseYaml(yamlCode);
    if (typeof yamlParsed === 'object') {
      return yamlParsed;
    } else {
      return null;
    }
  } else {
    return null;
  }
}

export function parseYaml(yamlCode: string) {
  try {
    const yamlParsed = yaml.safeLoad(yamlCode, {
      onWarning: logException,
    });
    return yamlParsed;
  } catch (e) {
    logException(e);
    return null;
  }
}

export function toYamlCode(obj: any): string | null {
  try {
    const yamlCode = yaml.safeDump(obj);
    return yamlCode;
  } catch (e) {
    logException(e);
    return null;
  }
}

export function stripYamlDelimeters(yamlCode: string) {
  return yamlCode
    .replace(/^[\s-]+/, '')
    .replace(/[\s-\.]+$/, '');
}

export interface ParsedYaml {
  yamlCode: string;
  yaml: any;
  node: NodeWithPos;
}

export function parseYamlNodes(doc: ProsemirrorNode): ParsedYaml[] {
  const yamlNodes = yamlMetadataNodes(doc);

  const parsedYamlNodes = yamlNodes.map<ParsedYaml>(node => {
    const yamlText = node.node.textContent;
    const yamlCode = stripYamlDelimeters(yamlText);
    return { yamlCode, yaml: parseYaml(yamlCode), node };
  });
  return parsedYamlNodes;
}

function firstYamlNode(doc: ProsemirrorNode) {
  const yamlNodes = yamlMetadataNodes(doc);
  if (yamlNodes && yamlNodes.length > 0) {
    return yamlNodes[0];
  } else {
    return '';
  }
}




