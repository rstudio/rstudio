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
import { PandocServer } from './pandoc';

import Fuse from 'fuse.js';

import { EditorUIContext, EditorUI } from './ui';
import { yamlMetadataNodes, parseYaml, stripYamlDelimeters, toYamlCode } from './yaml';

export interface BibliographyFiles {
  bibliography: string[];
}

export interface BibliographyResult {
  etag: string;
  bibliography: Bibliography;
}

export interface Bibliography {
  sources: BibliographySource[];
}

// An entry which includes the source as well as a
// rendered html preview of the citation
export interface BibliographyEntry {
  source: BibliographySource;
  authorsFormatter: (authors: BibliographyAuthor[], maxLength: number) => string;
  issuedDateFormatter: (issueDate: BibliographyDate) => string;
  image: [string?, string?];
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
    const yamlNodes = yamlMetadataNodes(doc);

    const parsedYamlNodes = yamlNodes.map(node => {
      const yamlText = node.node.textContent;
      const yamlCode = stripYamlDelimeters(yamlText);
      return parseYaml(yamlCode);
    });

    // Gather the files from the document
    const files = bibliographyFilesFromDoc(parsedYamlNodes, ui.context);

    // Gather the reference block
    const refBlock = referenceBlockFromYaml(parsedYamlNodes);

    if (files || refBlock) {
      // get the bibliography
      const result = await this.server.getBibliography(
        ui.context.getDocumentPath(),
        files ? files.bibliography : [],
        refBlock,
        this.etag,
      );

      // Read bibliography data from files (via server)
      if (!this.bibEntries || result.etag !== this.etag) {
        this.bibEntries = generateBibliographyEntries(ui, result.bibliography);
        this.reindexEntries();
      }

      // record the etag for future queries
      this.etag = result.etag;
    }

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

  private reindexEntries() {
    // build search index
    const options = {
      keys: kFields.map(field => field.name),
    };
    const index = Fuse.createIndex(options.keys, this.bibEntries);
    this.fuse = new Fuse(this.bibEntries, options, index);
  }
}
const kMaxCitationCompletions = 20;

function referenceBlockFromYaml(yamls: any[]): string {
  const refBlockYamls = yamls.filter(yaml => typeof yaml === 'object' && yaml.references);

  // Pandoc will use the last references node when generating a bibliography.
  // So replicate this and use the last biblography node that we find
  if (refBlockYamls.length > 0) {
    const lastReferenceYaml = refBlockYamls[refBlockYamls.length - 1];
    const referenceYaml = toYamlCode(lastReferenceYaml.references);
    if (referenceYaml) {
      return referenceYaml;
    }
  }

  return '';
}

// TODO: path handling ok here?
function bibliographyFilesFromDoc(yamls: any[], uiContext: EditorUIContext): BibliographyFiles | null {
  const bibliographyYamls = yamls.filter(yaml => typeof yaml === 'object' && yaml.bibliography);

  // Look through any yaml nodes to see whether any contain bibliography information
  if (bibliographyYamls.length > 0) {
    // Pandoc will use the last biblography node when generating a bibliography.
    // So replicate this and use the last biblography node that we find
    const lastBibliographyYaml = bibliographyYamls[bibliographyYamls.length - 1];
    const bibliographyFiles = lastBibliographyYaml.bibliography;
    if (Array.isArray(bibliographyFiles)) {
      // An array of bibliographies
      const bibPaths = bibliographyFiles.map(
        bibliographyFile => uiContext.getDefaultResourceDir() + '/' + bibliographyFile,
      );
      return {
        bibliography: bibPaths,
      };
    } else {
      // A single bibliography
      return {
        bibliography: [uiContext.getDefaultResourceDir() + '/' + bibliographyFiles],
      };
    }
  }
  return null;
}

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
  // Formatter for shortening the author string (formatter will support localization of string
  // template used for shortening the string)
  const authorsFormatter = (authors: BibliographyAuthor[], maxLength: number): string => {
    return formatAuthors(authors, maxLength, ui);
  };

  // Formatter used for shortening and displaying issue dates.
  const issuedDateFormatter = (date: BibliographyDate): string => {
    return formatIssuedDate(date, ui);
  };

  // Map the generated html preview to each source item
  return bibliography.sources.map(source => {
    return {
      source,
      authorsFormatter,
      issuedDateFormatter,
      image: imageForType(ui, source.type),
    };
  });
}

const kEtAl = 'et al.';
function formatAuthors(authors: BibliographyAuthor[], maxLength: number, ui: EditorUI): string {
  // TODO: Needs to support localization of the templated strings
  let formattedAuthorString = '';
  authors
    .map(author => {
      if (author.given.length > 0) {
        // Family and Given name
        return `${author.family}, ${author.given.substring(0, 1)}`;
      } else {
        // Family name only
        return `${author.family}`;
      }
    })
    .every((value, index, values) => {
      // If we'll exceed the maximum length, append 'et al' and stop
      if (formattedAuthorString.length + value.length > maxLength) {
        formattedAuthorString = `${formattedAuthorString}, ${kEtAl}`;
        return false;
      }

      if (index === 0) {
        // The first author
        formattedAuthorString = value;
      } else if (values.length > 1 && index === values.length - 1) {
        // The last author
        formattedAuthorString = `${formattedAuthorString}, and ${value}`;
      } else {
        // Middle authors
        formattedAuthorString = `${formattedAuthorString}, ${value}`;
      }
      return true;
    });
  return formattedAuthorString;
}

function formatIssuedDate(date: BibliographyDate, ui: EditorUI): string {
  // TODO: Needs to support localization of the templated strings
  // No issue date for this
  if (!date) {
    return '';
  }

  switch (date['date-parts'].length) {
    // There is a date range
    case 2:
      return `${date['date-parts'][0]}-${date['date-parts'][1]}`;
    // Only a single date
    case 1:
      return `${date['date-parts'][0]}`;

    // Seems like a malformed date :(
    case 0:
    default:
      return '';
  }
}
