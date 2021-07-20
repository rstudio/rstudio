
/*
 * cite-completion-search.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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
import Fuse from 'fuse.js';

import { CiteCompletionEntry } from "./cite-completion";

const searchFields: Fuse.FuseOptionKeyObject[] = [
  { name: 'id', weight: 30 },
];

const searchOptions = {
  isCaseSensitive: false,
  shouldSort: true,
  includeMatches: false,
  includeScore: false,
  keys: searchFields,
};

export interface CiteCompletionSearch {
  setEntries: (entries: CiteCompletionEntry[]) => void;
  search: (searchTerm: string) => CiteCompletionEntry[];
  exactMatch: (searchTerm: string) => boolean;
}

export function completionIndex(defaultEntries?: CiteCompletionEntry[]): CiteCompletionSearch {
  // build search index
  const options = {
    ...searchOptions,
    keys: searchFields.map(field => field.name),
  };

  defaultEntries = defaultEntries || [];
  const index = Fuse.createIndex<CiteCompletionEntry>(options.keys, defaultEntries);
  const fuse = new Fuse(defaultEntries, options, index);
  let indexedEntries: CiteCompletionEntry[] = [];
  return {
    setEntries: (entries: CiteCompletionEntry[]) => {
      fuse.setCollection(entries);
      indexedEntries = entries;
    },
    search: (searchTerm: string): CiteCompletionEntry[] => {
      return fuse.search(searchTerm).map(result => result.item);
    },
    exactMatch: (searchTerm: string): boolean => {
      return indexedEntries.some(entry => entry.id === searchTerm);
    }
  };
}