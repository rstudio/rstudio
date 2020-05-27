import * as untypedSymbolData from './insert_symbol-data.json';
import { parseCodepoint } from '../../api/unicode';

export interface SymbolGroup {
  alias: string;
  blocks: SymbolBlock[];
}

export interface SymbolBlock {
  name: string;
  codepointFirst: number;
  codepointLast: number;
}

export interface SymbolCharacter {
  name: string;
  value: string;
  codepoint: number;
}

export const CATEGORY_ALL = {
  alias: 'All',
  blocks: [{ name: 'All', codepointFirst: 0, codepointLast: Number.MAX_VALUE }],
};

class SymbolDataManager {
  constructor() {
    this.symbols = untypedSymbolData.symbols;
    this.symbolGroups = (untypedSymbolData.blocks as Array<SymbolGroup>).sort((a, b) => a.alias.localeCompare(b.alias));
  }
  private symbols: Array<SymbolCharacter>;
  private symbolGroups: Array<SymbolGroup>;

  public getSymbolGroups(): Array<SymbolGroup> {
    return [CATEGORY_ALL, ...this.symbolGroups];
  }

  public getSymbols(symbolGroup: SymbolGroup) {
    if (symbolGroup.alias === CATEGORY_ALL.alias) {
      return this.symbols;
    }
    return this.symbols.filter(symbol => {
      return symbolGroup.blocks.find(block => {
        return symbol.codepoint >= block.codepointFirst && symbol.codepoint <= block.codepointLast;
      });
    });
  }

  public filterSymbols(filterText: string, symbols: SymbolCharacter[]): SymbolCharacter[] {
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

    if (filteredSymbols.length === 0 && codepoint) {
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
