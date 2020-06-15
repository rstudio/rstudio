/*
 * bibliography.ts
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
import { yamlMetadataNodes, parseYaml, stripYamlDelimeters } from './yaml';
import { EditorUIContext } from './ui';
import { PandocServer } from './pandoc';
import Fuse from 'fuse.js';

export interface BibliographyFiles {
  bibliography: string;
  csl: string | null;
}

export interface BibliographyResult {
  etag: string;
  bibliography: Bibliography;
}

export interface Bibliography {
  sources: BibliographySource[];
  html: string;
}

// An entry which includes the source as well as a
// rendered html preview of the citation
export interface BibliographyEntry {
  source: BibliographySource;
  html: string;
}

// The individual bibliographic source
export interface BibliographySource {
  id: string;
  type: string;
  DOI: string;
  URL: string;
  title: string;
  author: BibliographyAuthor[];
  issued: BibliographyDate;
}

// Author
export interface BibliographyAuthor {
  family: string;
  given: string;
}

// Used for issue dates
export interface BibliographyDate {
  'date-parts': number[][];
  raw?: string;
}

// The fields and weights that will indexed and searched
// when searching bibliographic entries
const kFields: Fuse.FuseOptionKeyObject[] = [
  { name: 'source.id', weight: 10 },
  { name: 'source.author.family', weight: 10 },
  { name: 'source.author.given', weight: 1 },
  { name: 'source.title', weight: 1 },
  { name: 'source.issued', weight: 1 },
];

export class BibliographyManager {
  private server: PandocServer;
  private etag: string;
  private bibEntries: BibliographyEntry[];
  private fuse: Fuse<BibliographyEntry, Fuse.IFuseOptions<any>> | undefined;

  public constructor(server: PandocServer) {
    this.server = server;
    this.etag = '';
    this.bibEntries = [];
  }

  public async entries(files?: BibliographyFiles): Promise<BibliographyEntry[]> {
    // no files means no entries
    if (files === undefined) {
      return Promise.resolve([]);
    }

    // get the bibliography
    const result = await this.server.getBibliography(files.bibliography, files.csl, this.etag);

    // update bibliography if necessary
    if (!this.bibEntries || result.etag !== this.etag) {
      this.update(result.bibliography);
    }

    // record the etag for future queries
    this.etag = result.etag;

    // return entries
    return this.bibEntries;
  }

  public search(query: string): BibliographyEntry[] {
    if (this.fuse) {
      const options = {
        isCaseSensitive: false,
        shouldSort: true,
        includeMatches: false,
        limit: kMaxCitationCompletions,
        keys: kFields,
      };
      const results = this.fuse.search(query, options);
      return results.map((result: { item: any }) => result.item);
    } else {
      return [];
    }
  }

  private update(bibliography: Bibliography) {
    // generate entries
    this.bibEntries = generateBibliographyEntries(bibliography);

    // build search index
    const options = {
      keys: kFields.map(field => field.name),
    };
    const index = Fuse.createIndex(options.keys, this.bibEntries);
    this.fuse = new Fuse(this.bibEntries, options, index);
  }
}
const kMaxCitationCompletions = 20;

export function bibliographyFilesFromDoc(doc: ProsemirrorNode, uiContext: EditorUIContext): BibliographyFiles | null {
  // TODO: I guess you could technically have a bibliography entry in another yaml node
  // TODO: references can actually be defined an inline yaml as per pandoc docs
  // TODO: some reassurance that paths are handled correctly
  // TODO: What about global references
  const yamlNodes = yamlMetadataNodes(doc);
  if (yamlNodes.length > 0) {
    // Nodes that contain a bibliography entry
    let biblioYaml: any = null;

    // Find the first node that contains a bibliography, then stop
    // looking at further nodes.
    yamlNodes.some(node => {
      const yamlText = node.node.textContent;
      const yamlCode = stripYamlDelimeters(yamlText);
      const yaml = parseYaml(yamlCode);
      if (yaml && typeof yaml === 'object' && yaml.bibliography) {
        biblioYaml = yaml;
        return true;
      }
      return false;
    });

    // If we found at least one node with a bibliography, read that
    if (biblioYaml) {
      return {
        bibliography: uiContext.getDefaultResourceDir() + '/' + biblioYaml.bibliography,
        csl: biblioYaml.csl ? uiContext.getDefaultResourceDir() + '/' + biblioYaml.csl : null,
      };
    } else {
      return null;
    }
  } else {
    return null;
  }
}

const kHangingIndentIdentifier = 'hanging-indent';

export function generateBibliographyEntries(bibliography: Bibliography): BibliographyEntry[] {
  const parser = new window.DOMParser();
  const doc = parser.parseFromString(bibliography.html, 'text/html');

  // The root refs element may contain style information that we should forward to
  // the individual items
  const root = doc.getElementById('refs');
  const hangingIndent = root?.className.includes(kHangingIndentIdentifier);

  // Map the generated html preview to each source item
  return bibliography.sources.map(source => {
    const id = source.id;

    // Find the generated html in the bibliography preview
    // and use that
    const element = doc.getElementById(`ref-${id}`);
    if (element) {
      // If the refs element uses hanging indents, forward this on to the
      // individual child elements
      if (hangingIndent) {
        element.firstElementChild?.classList.add(kHangingIndentIdentifier);
      }
      return {
        source,
        html: element.innerHTML,
      };
    }

    // No preview html was available for this item, return a placeholder rendering instead.
    // TODO: Select a placeholder or decide to error when the preview is invalid
    // For example, if user directs us to a malformed csl file file that results in no preview
    return {
      source,
      html: `<p>${source.author} ${source.title}</p>`,
    };
  });
}
