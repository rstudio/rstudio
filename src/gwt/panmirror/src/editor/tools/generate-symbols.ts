/*
 * generate-symbols.ts
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

import * as fetch from 'node-fetch'
import * as fs from 'fs';
import * as os from 'os';
import * as unzip from 'unzip';
import * as parser from 'fast-xml-parser';

// This will check the 'age' of the unicode characters and only allow characters
// with an age less than or equal to this age.
const maxUnicodeAge = 11.0;

// The file that should be generated holding the symbol data
const outputFile = './src/behaviors/symbol/insert_symbol-data.json';

// The names of blocks of unicode characters to be scan for characters to include.
// Blocks will only be included if characters from that block are selected (e.g. characters)
// might not meet the maxUnicodeVersion requirement, may be depcrecated or so on).
const groupToBlockMapping = [
  {
    alias: 'Miscellaneous',
    blocks: ['Latin-1 Supplement', 'Enclosed Alphanumerics', 'Dingbats', 'Miscellaneous Symbols'],
  },
  {
    alias: 'Mathematical',
    blocks: [
      'Mathematical Operators',
      'Miscellaneous Mathematical Symbols-A',
      'Miscellaneous Mathematical Symbols-B',
      'Supplemental Mathematical Operators',
      'Mathematical Alphanumeric Symbols',
    ],
  },
  {
    alias: 'Punctuation',
    blocks: ['Supplemental Punctuation'],
  },
  {
    alias: 'Technical',
    blocks: ['Miscellaneous Technical'],
  },
  {
    alias: 'Arrows',
    blocks: ['Miscellaneous Symbols and Arrows', 'Supplemental Arrows-A', 'Supplemental Arrows-B'],
  },
  {
    alias: 'Ancient',
    blocks: ['Ancient Symbols', 'Ancient Greek Numbers'],
  },
  {
    alias: 'Braille',
    blocks: ['Braille Patterns'],
  },
  {
    alias: 'Currency',
    blocks: ['Currency Symbols'],
  },
  {
    alias: 'Game Symbols',
    blocks: ['Mahjong Tiles', 'Domino Tiles', 'Playing Cards', 'Chess Symbols'],
  },
  {
    alias: 'Music',
    blocks: ['Musical Symbols'],
  },
  {
    alias: 'Geometric Shapes',
    blocks: ['Geometric Shapes', 'Geometric Shapes Extended'],
  },
  {
    alias: 'Ideographic',
    blocks: ['Ideographic Description Characters', 'Ideographic Symbols and Punctuation'],
  },
];

const excludedChars = [
  160, // no-break space
];

// Basic file paths to use when downloading and generating the file. These files will be cleaned up
// upon completion.
const workingDirectory = os.tmpdir();
const targetFileName = 'ucd.nounihan.flat';
const targetZipFile = `${workingDirectory}/${targetFileName}.zip`;
const targetXmlFile = `${workingDirectory}/${targetFileName}.xml`;

// The path that will be used to download the unicode file. This is currently
// set to always downlod the latest. The maxUnicodeVersion attribute of each
// character is used to decide which characters to include rather than the unicode database
// version
const unicodeDownloadPath = `https://www.unicode.org/Public/UCD/latest/ucdxml/${targetFileName}.zip`;

// Remove any orphaned intermediary files
cleanupFiles([targetXmlFile, targetZipFile], true);

fetch(unicodeDownloadPath, {method: 'GET'})
.then((res) => {
  //Download the file
  return new Promise<string>((resolve, reject) => {
    const file = fs.createWriteStream(targetZipFile);
    res.body.on('finish', () => resolve(targetZipFile));
    res.body.pipe(file);
    file.on('error', reject);
  })
})
.then(() => {
  // Unzip the file
  return new Promise((resolve, reject) => {
    message('Unzipping File', targetZipFile);
    const readStream = fs.createReadStream(targetZipFile);
    const writeStream = unzip.Extract({ path: workingDirectory });
    writeStream.on('error', reject);
    writeStream.on('close', () => {
      message('Done unzipping', '');
      resolve(outputFile);     
    });
    readStream.pipe(writeStream);
  });    
})
.then(() => {
  // Parse XML -> Json
  message('Parsing', targetXmlFile);
  const fileContents = fs.readFileSync(targetXmlFile, 'utf8');
  var options = {
    ignoreAttributes: false,
    arrayMode: false,
  };
  const tObj = parser.getTraversalObj(fileContents, options);
  const jsonResult = parser.convertToJson(tObj, options);
  message('Done Parsing', '');
  return jsonResult;
})
.then((jsonResult) => {
  // Read the block from the XML file and generate typed data
  message('Reading Raw Data'); 
  const allIncludedBlocks: Array<Block> = parseBlocks(jsonResult.ucd.blocks.block);
  const allValidSymbols: Array<Symbol> = parseSymbols(jsonResult.ucd.repertoire.char);
  message(' Blocks ' + allIncludedBlocks.length);
  message(' Chars ' + allValidSymbols.length);
  message('');

  message('Generating Output Data');
  var blockGroups: SymbolGroup[] = new Array<SymbolGroup>();
  groupToBlockMapping.forEach(groupToBlockMapping => {
    const groupName = groupToBlockMapping.alias;
    const groupSymbols = allValidSymbols.filter(symbol => {
        // Find the child blocks for this Group and use the codepoint to determine
        // whether this symbol should be included in this group
        const matchingBlockName = groupToBlockMapping.blocks.find(blockName => {
          const matchingBlock = allIncludedBlocks.find(block => block.name === blockName);
          return symbol.codepoint >= matchingBlock.codepointFirst && symbol.codepoint <= matchingBlock.codepointLast;
        });

        return matchingBlockName != null;
    });
    message('Group ' + groupName + ' -> ' + groupSymbols.length + ' symbols');   
    blockGroups.push({ name: groupName, symbols: groupSymbols });
  });
  message('');
  return blockGroups;
})
.then((blockGroups) => {
  // Filter out any groups with no valid characters
  return blockGroups.filter(blockGroup => blockGroup.symbols.length > 0)
})
.then((blockGroups) => {
  // Write the output file
  message('Writing output'), outputFile;
  cleanupFiles([outputFile], false);
  const finalJson = JSON.stringify(blockGroups, null, 2);
  fs.writeFileSync(outputFile, finalJson);
  message('Done');
})
.catch((message) => {
  error(message);
})
.finally(() => {
  // Final cleanup
  cleanupFiles([targetZipFile, targetXmlFile]);
});

function parseBlocks(blockJson) : Block[] {
  return blockJson.map(block => {
    return {
      name: block['@_name'],
      codepointFirst: parseInt(block['@_first-cp'], 16),
      codepointLast: parseInt(block['@_last-cp'], 16),
    };
  })
  .filter(block => {
    return groupToBlockMapping.find(blockGroup => blockGroup.blocks.includes(block.name));
  });
}

function parseSymbols(symbolsJson) : Symbol[] {
  return symbolsJson.filter(rawChar => isValidSymbol(rawChar))
  .map(rawChar => {
    const charpoint = parseInt(rawChar['@_cp'], 16);
    return {
      name: rawChar['@_na'],
      value: String.fromCodePoint(charpoint),
      codepoint: charpoint,
    };
  });
}

function isValidSymbol(rawChar): boolean {
  const deprecated: string = rawChar['@_dep'];
  if (deprecated === 'Y') {
    return false;
  }

  // no characters introduced after the maximum age
  const age = Number.parseFloat(rawChar['@_age']);
  if (age > maxUnicodeAge) {
    return false;
  }

  // no control characters, private chararacters or other weird types
  const generalCategory = rawChar['@_gc'];
  if (['Cc', 'C', 'Cf', 'Cn', 'Co', 'Cs'].includes(generalCategory)) {
    return false;
  }

  // no exluded characters
  const codepoint = Number.parseInt(rawChar['@_cp'], 16);
  if (excludedChars.includes(codepoint)) {
    return false;
  }

  // No emoji, components, or modifiers
  const isEmoji = rawChar['@_Emoji'] === 'Y';
  const isEmojiComponent = rawChar['@_EComp'] === 'Y';
  const isEmojiModifier = rawChar['@_EMod'] === 'Y';
  if (isEmoji || isEmojiComponent || isEmojiModifier) {
    return false;
  }

  // Has a valid codepoint
  const charpoint = rawChar['@_cp'];
  if (!charpoint) {
    return false;
  }

  return true;
}

function cleanupFiles(files: Array<string>, warn?: boolean) {
  files.forEach(file => {
    if (fs.existsSync(file)) {
      fs.unlinkSync(file);
      if (warn) {
        warning('Cleaning up file', file);
      }
    }
  });
}

interface SymbolGroup {
  name: string;
  symbols: Symbol[];
}

interface Block {
  name: string;
  codepointFirst: number;
  codepointLast: number;
}

interface Symbol {
  name: string;
  value: string;
  codepoint: number;
}

function message(...message: any[]) {
  message.forEach(msg => console.log(msg));
}

function warning(...message: any[]) {
  message.forEach(msg => console.warn('WARN:', msg));
}

function error(...message: any[]) {
  message.forEach(msg => console.warn('ERROR:', msg));
}
