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

export interface Bibliography {
  sources: BibliographySource[];
  html: string;
}

export interface BibliographyEntry {
  source: BibliographySource;
  html: string;
}

export interface BibliographySource {
  id: string;
  type: string;
  DOI: string;
  URL: string;
  title: string;
  author: BibliographyAuthor[];
}

export interface BibliographyAuthor {
  family: string;
  given: string;
}

export class BibliographyManager {
  public constructor(server: PandocServer) {
    this.server = server;
  }

  public ensureLoaded(files: BibliographyFiles) {
    // no files have been initialized yet
    if (this.files === undefined) {
      this.files = files;
      this.clear();
      return;
    }

    // The bibliography file or csl has changed
    if (this.files.bibliography !== files.bibliography || this.files.csl !== files.csl) {
      this.files = files;
      this.clear();
    }

    // TODO: Check file timestamps to decide whether we should throw this away
  }

  private files: BibliographyFiles | undefined;
  private server: PandocServer;
  private bibEntries: BibliographyEntry[] | undefined;
  private fuse: Fuse<BibliographyEntry, Fuse.IFuseOptions<any>> | undefined;

  public async entries(): Promise<BibliographyEntry[]> {
    if (this.files === undefined) {
      return Promise.resolve([]);
    }

    if (!this.bibEntries) {
      const bibliography = await this.server.getBibliography(this.files.bibliography, this.files.csl);
      const entries = generateBibliographyEntries(bibliography);
      this.setEntries(entries);
      return entries;
    } else {
      return Promise.resolve(this.bibEntries);
    }
  }

  // TODO: Configure search properly
  public search(query: string): BibliographyEntry[] {
    if (!this.fuse && this.bibEntries) {
      this.refreshIndex(this.bibEntries);
    }

    if (this.fuse) {
      const options = {
        isCaseSensitive: false,
        shouldSort: true,
        includeMatches: false,
        limit: kMaxCitationCompletions,
        keys: [
          { name: 'source.title', weight: 1 },
          { name: 'source.author.family', weight: 2 },
          { name: 'source.author.given', weight: 1 },
        ],
      };
      console.log(query);
      const results = this.fuse.search(query, options);
      console.log(results);
      return results.map((result: { item: any }) => result.item);
    }
    return [];
  }

  private clear() {
    this.bibEntries = undefined;
    this.fuse = undefined;
  }

  private setEntries(entries: BibliographyEntry[]) {
    this.bibEntries = entries;
    this.refreshIndex(this.bibEntries);
  }

  private refreshIndex(entries: BibliographyEntry[]) {
    const options = {
      keys: ['source.title', 'source.author.family', 'source.author.given'],
    };
    const index = Fuse.createIndex(options.keys, entries);
    this.fuse = new Fuse(entries, options, index);
  }
}
const kMaxCitationCompletions = 20;

export function bibliographyFilesFromDoc(doc: ProsemirrorNode, uiContext: EditorUIContext): BibliographyFiles | null {
  // TODO: I guess you could technically have a bibliography entry in another yaml node
  // TODO: references can actually be defined an inline yaml as per pandoc docs
  // TODO: some reassurance that paths are handled correctly

  const yamlNodes = yamlMetadataNodes(doc);
  if (yamlNodes.length > 0) {
    const yamlText = yamlNodes[0].node.textContent;
    const yamlCode = stripYamlDelimeters(yamlText);
    const yaml = parseYaml(yamlCode);
    if (yaml && typeof yaml === 'object' && yaml.bibliography) {
      return {
        bibliography: uiContext.getDefaultResourceDir() + '/' + yaml.bibliography,
        csl: yaml.csl ? uiContext.getDefaultResourceDir() + '/' + yaml.csl : null,
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
