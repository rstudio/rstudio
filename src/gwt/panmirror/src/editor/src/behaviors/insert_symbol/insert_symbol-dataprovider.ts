/*
 * insert_symbol-dataprovider.ts
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

export interface SymbolDataProvider {
  symbolGroupNames(): string[];
  getSymbols(groupName: string | undefined): SymbolCharacter[];
  filterSymbols(filterText: string, symbols: SymbolCharacter[]): SymbolCharacter[];
  readonly filterPlaceholderHint: string;
}

export interface SymbolCharacterGroup {
  name: string;
  symbols: SymbolCharacter[];
}

export interface SymbolCharacter {
  name: string;
  value: string;
  codepoint?: number;

  aliases?: string[];
  description?: string;
}
