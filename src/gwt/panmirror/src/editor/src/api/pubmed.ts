/*
 * pubmed.ts
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

import { EditorUI } from "./ui";


export interface PubMedResult {
  status: "ok" | "notfound" | "nohost" | "error";
  message: PubMedDocument[] | null;
  error: string;
}

export interface PubMedDocument {
  doi: string;
  pubTypes?: string[];
  authors?: string[];
  sortFirstAuthor?: string;
  title?: string;
  source?: string;
  volume?: string;
  issue?: string;
  pubDate?: string;
}

export interface PubMedServer {
  search: (query: string) => Promise<PubMedResult>;
}

export function suggestCiteId(doc: PubMedDocument): string {

  // Try to get the last name. Prefer the sort first author,
  // but otherwise just use the first author (if any)
  let suggestedId = '';
  if (doc.sortFirstAuthor) {
    suggestedId = doc.sortFirstAuthor.split(' ')[0];
  } else if (doc.authors && doc.authors.length > 0) {
    suggestedId = doc.authors[0].split(' ')[0];
  }

  // Try to read the year
  if (doc.pubDate) {
    suggestedId = suggestedId + doc.pubDate.split(' ')[0];
  }
  return suggestedId;
}

export function imageForType(ui: EditorUI, pubTypes?: string[]): [string?, string?] {
  const type = pubTypes && pubTypes.length > 0 ? pubTypes[0] : '';
  switch (type) {
    case 'Journal Article':
    case 'Historical Article':
    case 'Classical Article':
    case 'Introductory Journal Article':
    case 'paper-conference':
      return [ui.images.citations?.article, ui.images.citations?.article_dark];
    case 'Legal Case':
    case 'Legislation':
      return [ui.images.citations?.legal, ui.images.citations?.legal_dark];
    case 'Dataset':
      return [ui.images.citations?.data, ui.images.citations?.data_dark];
    case 'Video-Audio Media':
      return [ui.images.citations?.movie, ui.images.citations?.movie_dark];
    case 'Webcasts':
      return [ui.images.citations?.web, ui.images.citations?.web_dark];
    default:
      return [ui.images.citations?.other, ui.images.citations?.other_dark];
  }
}


