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
import { EditorUIContext, EditorUI } from './ui';
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
  image: [string?, string?];
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

  public async entries(ui: EditorUI, doc: ProsemirrorNode): Promise<BibliographyEntry[]> {
    // no files means no entries
    const files = bibliographyFilesFromDoc(doc, ui.context);
    if (files === null) {
      return Promise.resolve([]);
    }

    // get the bibliography
    const result = await this.server.getBibliography(files.bibliography, files.csl, this.etag);

    // TODO: Parse any inline references and merge them into entries
    // referencesFromDoc(doc, ui.context);

    // update bibliography if necessary
    if (!this.bibEntries || result.etag !== this.etag) {
      this.update(ui, result.bibliography);
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

  private update(ui: EditorUI, bibliography: Bibliography) {
    // generate entries
    this.bibEntries = generateBibliographyEntries(ui, bibliography);

    // build search index
    const options = {
      keys: kFields.map(field => field.name),
    };
    const index = Fuse.createIndex(options.keys, this.bibEntries);
    this.fuse = new Fuse(this.bibEntries, options, index);
  }
}
const kMaxCitationCompletions = 20;

// TODO: Figure out what Pandoc does for multiple nodes, nodes + inline, etc...
// TODO: Figure out what Pandoc does for duplicate keys (which does it u)

function bibliographyFilesFromDoc(doc: ProsemirrorNode, uiContext: EditorUIContext): BibliographyFiles | null {
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

function referencesFromDoc(doc: ProsemirrorNode, uiContext: EditorUIContext): BibliographySource[] {
  const yamlNodes = yamlMetadataNodes(doc);
  yamlNodes.map(node => {
    const yamlText = node.node.textContent;
    const yamlCode = stripYamlDelimeters(yamlText);
    const yaml = parseYaml(yamlCode);
    if (yaml && typeof yaml === 'object' && yaml.references) {
      // TODO: What does pandoc do?
    }
  });
  return [];
}

const kHangingIndentIdentifier = 'hanging-indent';

function imageForType(ui: EditorUI, type: string): [string?, string?] {
  switch (type) {
    case 'article':
    case 'article-journal':
    case 'article-magazine':
    case 'article-newspaper':
    case 'paper-conference':
    case 'review':
    case 'review-book':
    case 'techreport':
      return [ui.images.citations?.article, ui.images.citations?.article_dark];
    case 'bill':
    case 'legislation':
    case 'legal_case':
    case 'patent':
    case 'treaty':
      return [ui.images.citations?.legal, ui.images.citations?.legal_dark];
    case 'book':
    case 'booklet':
    case 'chapter':
    case 'inbook':
    case 'incollection':
    case 'manuscript':
    case 'manual':
    case 'thesis':
    case 'masterthesis':
    case 'phdthesis':
      return [ui.images.citations?.book, ui.images.citations?.book_dark];
    case 'broadcast':
      return [ui.images.citations?.broadcast, ui.images.citations?.broadcast_dark];
    case 'data':
    case 'data-set':
      return [ui.images.citations?.data, ui.images.citations?.data_dark];
    case 'entry':
    case 'entry-dictionary':
    case 'entry-encyclopedia':
      return [ui.images.citations?.entry, ui.images.citations?.entry_dark];
    case 'figure':
    case 'graphic':
      return [ui.images.citations?.image, ui.images.citations?.image_dark];
    case 'map':
      return [ui.images.citations?.map, ui.images.citations?.map_dark];
    case 'motion_picture':
      return [ui.images.citations?.movie, ui.images.citations?.movie_dark];
    case 'musical_score':
    case 'song':
      return [ui.images.citations?.song, ui.images.citations?.song_dark];
    case 'post':
    case 'post-weblog':
    case 'webpage':
      return [ui.images.citations?.web, ui.images.citations?.web_dark];
    case 'conference':
    case 'inproceedings':
    case 'proceedings':
    case 'interview':
    case 'pamphlet':
    case 'personal_communication':
    case 'report':
    case 'speech':
    case 'misc':
    case 'unpublished':
    default:
      return [ui.images.citations?.other, ui.images.citations?.other_dark];
  }
}

export function generateBibliographyEntries(ui: EditorUI, bibliography: Bibliography): BibliographyEntry[] {
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
        image: imageForType(ui, source.type),
        html: element.innerHTML,
      };
    }

    // No preview html was available for this item, return a placeholder rendering instead.
    // TODO: Select a placeholder or decide to error when the preview is invalid
    // For example, if user directs us to a malformed csl file file that results in no preview
    return {
      source,
      image: [ui.images.citations?.other, ui.images.citations?.other_dark],
      html: `<p>${source.author} ${source.title}</p>`,
    };
  });
}
