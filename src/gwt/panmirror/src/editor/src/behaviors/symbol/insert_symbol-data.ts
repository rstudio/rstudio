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

export interface SymbolGroup {
  name: string;
  symbols: Symbol[];
}

export interface Symbol {
  name: string;
  value: string;
  codepoint: number;
}

export const CATEGORY_ALL = 'All';

class SymbolDataManager {
  constructor() {
    this.symbolGroups = (untypedSymbolData as Array<SymbolGroup>).sort((a, b) => a.name.localeCompare(b.name));
  }
  private symbolGroups: Array<SymbolGroup>;

  public getSymbolGroupNames(): Array<string> {
    return [CATEGORY_ALL, ...this.symbolGroups.map(symbolGroup => symbolGroup.name)];
  }

  public getSymbols(groupName: string) {
    if (groupName === CATEGORY_ALL) {
      return this.symbolGroups.map((symbolGroup) => symbolGroup.symbols).flat().sort((a,b) => a.codepoint - b.codepoint);
    }
    return this.symbolGroups.filter((symbolGroup) => groupName === symbolGroup.name).map((symbolGroup) => symbolGroup.symbols).flat();
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
          codepoint: codepoint,
        },
      ];
    }
    return filteredSymbols;
  }
}

export default SymbolDataManager;
