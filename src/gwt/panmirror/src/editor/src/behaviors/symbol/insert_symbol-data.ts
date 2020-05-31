/*
 * insert_symbol-data.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

import untypedSymbolData from './insert_symbol-data.json';
import { parseCodepoint } from '../../api/unicode';

// JJA I get this tslint warning here:
//  Don't use 'Symbol' as a type. Avoid using the `Symbol` type. Did you mean `symbol`? (ban-types)tslint(1)

// JJA: note that you can see tslint warnings for all open files in the VSCode 'Problems' tab

export interface SymbolGroup {
  name: string;
  symbols: Symbol[];
}

export interface Symbol {
  name: string;
  value: string;
  codepoint: number;
}

// JJA by convention constant take the form e.g. kCategoryAll
export const CATEGORY_ALL = 'All';

class SymbolDataManager {
  constructor() {
    // JJA tslint: Array type using 'Array<T>' is forbidden for simple types. Use 'T[]' instead. (array-type)tslint(1)
    this.symbolGroups = (untypedSymbolData as Array<SymbolGroup>).sort((a, b) => a.name.localeCompare(b.name));
  }
  // JJA: make this readonly
  private symbolGroups: Array<SymbolGroup>;

  // JJA: In general I have dropping the 'get' prefix on accessor functions like these
  // JJA: Array<string> should be string[] ()
  public getSymbolGroupNames(): Array<string> {
    return [CATEGORY_ALL, ...this.symbolGroups.map(symbolGroup => symbolGroup.name)];
  }

  // JJA: reformatted code in this function to use line-per-transformation idiom
  public getSymbols(groupName: string) {
    if (groupName === CATEGORY_ALL) {
      return this.symbolGroups
        .map((symbolGroup) => symbolGroup.symbols)
        .flat()
        .sort((a,b) => a.codepoint - b.codepoint);
    }
    return this.symbolGroups
            .filter((symbolGroup) => groupName === symbolGroup.name)
            .map((symbolGroup) => symbolGroup.symbols)
            .flat();
  }

  public filterSymbols(filterText: string, symbols: Symbol[]): Symbol[] {
    const codepoint = parseCodepoint(filterText);
    const filteredSymbols = symbols.filter(symbol => {
      // Search by name
      if (symbol.name.includes(filterText.toUpperCase())) {
        return true;
      }

      // Search by codepoint
      if (codepoint && symbol.codepoint === codepoint) {
        return true;
      }

      return false;
    });

    if (filteredSymbols.length === 0 && codepoint !== undefined) {
      return [
        {
          name: codepoint.toString(16),
          value: String.fromCodePoint(codepoint),
          // JJA: tslint for this you can just use 'codepoint'
          codepoint: codepoint,
        },
      ];
    }
    return filteredSymbols;
  }
}

export default SymbolDataManager;
