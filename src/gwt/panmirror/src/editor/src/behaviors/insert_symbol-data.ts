import * as untypedSymbolData from './insert_symbol-data.json';

export enum SymbolCategory {
  All = 'all',
  Letter = 'letters',
  Number = 'numbers',
  Stars = 'stars',
  Symbols = 'symbols',
  Emoji = 'emoji',
}

export interface SymbolCharacter {
  name: string;
  value: string;
  category: SymbolCategory;
}

class SymbolDataManager {
  private symbolData: Array<SymbolCharacter> = untypedSymbolData.symbols;

  public getCategories(): Array<SymbolCategory> {
    var symbolCategories: Array<SymbolCategory> = [SymbolCategory.All];
    this.symbolData.forEach(symbol => {
      if (!symbolCategories.includes(symbol.category)) {
        symbolCategories.push(symbol.category);
      }
    });
    return symbolCategories;
  }

  public getSymbols(symbolCategory: SymbolCategory) {
    if (symbolCategory === SymbolCategory.All) {
      return this.symbolData;
    }
    return this.symbolData.filter(symbol => symbol.category === symbolCategory);
  }
}

export default SymbolDataManager;
