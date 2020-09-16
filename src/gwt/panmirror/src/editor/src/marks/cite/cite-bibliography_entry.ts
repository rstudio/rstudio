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

import { BibliographySource } from '../../api/bibliography/bibliography';
import { EditorUI } from '../../api/ui';
import { CSLDate, CSLName, imageForType } from '../../api/csl';
import { formatAuthors, formatIssuedDate } from '../../api/cite';
import { kZoteroProviderKey } from '../../api/bibliography/bibliography-provider_zotero';

// An entry which includes the source as well
// additional metadata for displaying a bibliograph item
export interface BibliographyEntry {
  source: BibliographySource;
  authorsFormatter: (authors?: CSLName[], maxLength?: number) => string;
  issuedDateFormatter: (issueDate?: CSLDate) => string;
  image?: string;
  imageAdornment?: string;
}

export function entryForSource(source: BibliographySource, ui: EditorUI, forceLightMode?: boolean): BibliographyEntry {
  const authorsFormatter = (authors?: CSLName[], maxLength?: number): string => {
    return formatAuthors(authors, maxLength);
  };

  // Formatter used for shortening and displaying issue dates.
  const issuedDateFormatter = (date?: CSLDate): string => {
    if (date) {
      return formatIssuedDate(date);
    }
    return '';
  };

  // Map the Bibliography Sources to Entries which include additional
  // metadat and functions for display
  return {
    source,
    authorsFormatter,
    issuedDateFormatter,
    image: imageForType(ui.images, source.type)[ui.prefs.darkMode() && !forceLightMode ? 1 : 0],
    imageAdornment: source.providerKey === kZoteroProviderKey ? ui.images.citations?.zoteroOverlay : undefined
  };
}



