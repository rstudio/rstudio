import * as untypedSymbolData from './insert_symbol-data.json';

export interface SymbolCategory {
  name: string;
  codepointFirst: number;
  codepointLast: number;
}

export interface SymbolCharacter {
  name: string;
  value: string;
  codepoint: number;
}

export const CATEGORY_ALL = {name: "All", codepointFirst: 0, codepointLast: Number.MAX_VALUE};

class SymbolDataManager {

  constructor() {
    this.symbolData = untypedSymbolData.symbols;
    this.blockData = (untypedSymbolData.blocks as Array<SymbolCategory>).sort((a, b) => a.name.localeCompare(b.name));
  }
  private symbolData: Array<SymbolCharacter>;
  private blockData: Array<SymbolCategory>;

  public getCategories(): Array<SymbolCategory> {
    return [CATEGORY_ALL, ...this.blockData];
  }

  public getSymbols(symbolCategory: SymbolCategory) {
    if (symbolCategory.name === CATEGORY_ALL.name) {
      return this.symbolData;
    }
    return this.symbolData.filter((symbol) => {
      return symbol.codepoint >= symbolCategory.codepointFirst && symbol.codepoint <= symbolCategory.codepointLast;
    });
  }
}

export default SymbolDataManager;