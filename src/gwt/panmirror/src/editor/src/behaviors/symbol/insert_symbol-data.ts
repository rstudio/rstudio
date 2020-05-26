import * as untypedSymbolData from './insert_symbol-data.json';

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
}

export default SymbolDataManager;
