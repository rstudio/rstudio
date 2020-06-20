/*
 * cite-bibliography_entry.ts
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

import { BibliographySource, BibliographyAuthor, BibliographyDate } from '../../api/bibliography';
import { EditorUI } from '../../api/ui';

// An entry which includes the source as well
// additional metadata for displaying a bibliograph item
export interface BibliographyEntry {
  source: BibliographySource;
  authorsFormatter: (authors?: BibliographyAuthor[], maxLength?: number) => string;
  issuedDateFormatter: (issueDate?: BibliographyDate) => string;
  image?: string;
}

export function entryForSource(source: BibliographySource, ui: EditorUI): BibliographyEntry {
  const authorsFormatter = (authors?: BibliographyAuthor[], maxLength?: number): string => {
    return formatAuthors(authors, maxLength);
  };

  // Formatter used for shortening and displaying issue dates.
  const issuedDateFormatter = (date?: BibliographyDate): string => {
    if (date) {
      return formatIssuedDate(date, ui);
    }
    return '';
  };

  // Map the Bibliography Sources to Entries which include additional
  // metadat and functions for display
  return {
    source,
    authorsFormatter,
    issuedDateFormatter,
    image: imageForType(ui, source.type)[ui.prefs.darkMode() ? 1 : 0],
  };
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

// TODO: Needs to support localization of the templated strings
const kEtAl = 'et al.';
function formatAuthors(authors?: BibliographyAuthor[], maxLength?: number): string {
  // No author(s) specified
  if (!authors) {
    return '';
  }

  return authors
    .map(author => {
      if (author.literal?.length) {
        return author.literal;
      } else if (author.given?.length) {
        // Family and Given name
        return `${author.family}, ${author.given.substring(0, 1)}`;
      } else {
        // Family name only
        return `${author.family}`;
      }
    })
    .reduce((previous, current, index, array) => {
      // Ignore any additional authors if the string
      // exceeds the maximum length
      if ((maxLength && previous.length >= maxLength) || previous.endsWith(kEtAl)) {
        return previous;
      }

      if (index === 0) {
        // Too long, truncate
        if (maxLength && current.length > maxLength) {
          return `${current.substring(0, maxLength - 1)}…`;
        }
        // The first author
        return current;
      } else if (index > 0 && index === array.length - 1) {
        return addAuthorOrEtAl(previous, `${previous}, and ${current}`, maxLength);
      } else {
        // Middle authors
        return addAuthorOrEtAl(previous, `${previous}, ${current}`, maxLength);
      }
    });
}

function addAuthorOrEtAl(previousAuthorStr: string, newAuthorStr: string, maxLength?: number) {
  // if adding the string would make it too long, truncate
  if (maxLength && newAuthorStr.length > maxLength) {
    return etAl(previousAuthorStr, maxLength);
  }
  return newAuthorStr;
}

function etAl(authorStr: string, maxLength: number) {
  // First try just using et al., then shorten existing
  // author to accomodate
  const etAlStr = `${authorStr} ${kEtAl}`;
  if (maxLength && etAlStr.length > maxLength) {
    // First try to truncate to a space
    const lastSpace = authorStr.lastIndexOf(' ');
    if (lastSpace) {
      return `${authorStr.substr(0, lastSpace)} ${kEtAl}`;
    } else {
      // As a last resort, truncate with ellipsis
      const excessLength = etAlStr.length - maxLength - 1;
      return `${authorStr.substr(0, authorStr.length - excessLength)}… ${kEtAl}`;
    }
  }
  return etAlStr;
}

// TODO: Needs to support localization of the templated strings
function formatIssuedDate(date: BibliographyDate, ui: EditorUI): string {
  // No issue date for this
  if (!date) {
    return '';
  }

  const dateParts = date['date-parts'];
  if (dateParts) {
    switch (dateParts.length) {
      // There is a date range
      case 2:
        return `${dateParts[0][0]}-${dateParts[1][0]}`;
      // Only a single date
      case 1:
        return `${dateParts[0][0]}`;

      // Seems like a malformed date :(
      case 0:
      default:
        return '';
    }
  }
  return '';
}
