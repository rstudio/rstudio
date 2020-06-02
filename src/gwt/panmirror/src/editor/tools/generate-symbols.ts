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

import * as fetch from 'node-fetch';
import * as fs from 'fs';
import * as os from 'os';
import * as unzip from 'unzip';
import * as parser from 'fast-xml-parser';

// This will enforce the 'age' of the unicode characters and only allow characters
// with an age less than or equal to this age.
const maxUnicodeAge = 6.0;

// The file that should be generated holding the symbol data
const outputFile = './src/behaviors/insert_symbol/symbols.json';

// The names of blocks of unicode characters to be scan for characters to include.
// Blocks will only be included if characters from that block are selected (e.g. characters)
// might not meet the maxUnicodeVersion requirement, may be depcrecated or so on).
const groupToBlockMapping = [
  {
    alias: 'Miscellaneous',
    blocks: ['Latin-1 Supplement', 'Enclosed Alphanumerics', 'Dingbats', 'Miscellaneous Symbols', 'Letterlike Symbols'],
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
  }
];

// These characters are excluded because they don't render properly in the default font. 
// Consider re-enabling them as address font issues with unicode.
const excludedChars = [
  160, // no-break space
  65860,65861,65910,65911,65912,65923,65927,65928,65929,65931,65932,65933,65934, // Ancient Characters
  11094,11095,11096,11097, // Arrows
  10190,10191,120778,120779, // Mathematical
  9885,9886,9887,9907,9908,9909,9910,9911,9912,9913,9914,9915,9916,9919,9920,9921,
  9922,9923,9926,9927,9929,9930,9932,9933,9936,9938,9941,9942,9943,9944,9945,9946,
  9947,9948,9949,9950,9951,9952,9953,9955,9956,9957,9958,9959,9960,9963,9964,9965,
  9966,9967,9974,9979,9980,9982,9983,10079,10080, // Miscellaneous
  9192, // technical
  11801, // punctuation
  119049,119050,119051,119052,119053,119054,119055,119059,119060,119061,119062,119063,
  119064,119065,119066,119067,119068,119069,119071,119072,119075,119076,119077,119078,
  119081,119084,119085,119086,119087,119088,119089,119090,119091,119092,119093,119094,
  119095,119096,119097,119098,119099,119100,119101,119102,119103,119104,119105,119106,
  119107,119108,119109,119110,119111,119112,119113,119114,119115,119116,119117,119118,
  119119,119120,119121,119122,119123,119124,119125,119126,119127,119128,119129,119130,
  119131,119132,119133,119134,119135,119136,119137,119138,119139,119140,119141,119142,
  119143,119144,119145,119146,119147,119148,119149,119150,119151,119152,119153,119154,
  119163,119164,119165,119166,119167,119168,119169,119170,119171,119172,119173,119174,
  119175,119176,119177,119178,119179,119180,119181,119182,119183,119184,119185,119188,
  119189,119190,119191,119192,119193,119194,119195,119196,119197,119198,119199,119200,
  119201,119202,119203,119204,119205,119209,119210,119211,119212,119213,119214,
  119215,119216,119217,119218,119219,119220,119221,119222,119223,119224,119225,119226,
  119227,119228,119229,119230,119231,119232,119233,119234,119235,119236,119237,119238,
  119247,119248,119249,119250,119251,119252,119253,119254,119255,119256,119257,119258,
  119259,119260,119261, // musical symbols


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
  // Download the file
  return new Promise<string>((resolve, reject) => {
    const file = fs.createWriteStream(targetZipFile);
    res.body.on('finish', () => resolve(targetZipFile));
    res.body.pipe(file);
    file.on('error', reject);
  });
})
.then(() => {
  // Unzip the file
  return new Promise((resolve, reject) => {
    info('Unzipping File', targetZipFile);
    const readStream = fs.createReadStream(targetZipFile);
    const writeStream = unzip.Extract({ path: workingDirectory });
    writeStream.on('error', reject);
    writeStream.on('close', () => {
      info('Done unzipping', '');
      resolve(outputFile);     
    });
    readStream.pipe(writeStream);
  });    
})
.then(() => {
  // Parse XML -> Json
  info('Parsing', targetXmlFile);
  const fileContents = fs.readFileSync(targetXmlFile, 'utf8');
  const options = {
    ignoreAttributes: false,
    arrayMode: false,
  };
  const tObj = parser.getTraversalObj(fileContents, options);
  const jsonResult = parser.convertToJson(tObj, options);
  info('Done Parsing', '');
  return jsonResult;
})
.then((jsonResult) => {
  // Read the block from the XML file and generate typed data
  info('Reading Raw Data'); 
  const allIncludedBlocks: Block[] = parseBlocks(jsonResult.ucd.blocks.block);
  const allValidSymbols: Character[] = parseSymbols(jsonResult.ucd.repertoire.char);
  info(' Blocks ' + allIncludedBlocks.length);
  info(' Chars ' + allValidSymbols.length);
  info('');

  info('Generating Output Data');
  const symbolGroups: Group[] = new Array<Group>();
  groupToBlockMapping.forEach(mapping => {
    const groupName = mapping.alias;
    const groupSymbols = allValidSymbols.filter(symbol => {
        // Find the child blocks for this Group and use the codepoint to determine
        // whether this symbol should be included in this group
        const matchingBlockName = mapping.blocks.find(blockName => {
          const matchingBlock = allIncludedBlocks.find(block => block.name === blockName);
          return symbol.codepoint >= matchingBlock.codepointFirst && symbol.codepoint <= matchingBlock.codepointLast;
        });

        return matchingBlockName != null;
    });
    info('Group ' + groupName + ' -> ' + groupSymbols.length + ' symbols');   
    symbolGroups.push({ name: groupName, symbols: groupSymbols });
  });
  info('');
  return symbolGroups;
})
.then((symbolGroups) => {
  // Filter out any groups with no valid characters
  return symbolGroups.filter(blockGroup => blockGroup.symbols.length > 0);
})
.then((symbolGroups) => {
  // Write the output file
  info('Writing output', outputFile);
  cleanupFiles([outputFile], false);
  const finalJson = JSON.stringify(symbolGroups, null, 2);
  fs.writeFileSync(outputFile, finalJson);


  const countSymbols = symbolGroups.reduce((count, symbolGroup) => {
    return count + symbolGroup.symbols.length;
  }, 0);
  info(countSymbols + " total symbols generated");
  info('Done', '');
})
.catch((message: any) => {
  error(message);
})
.finally(() => {
  // Final cleanup
  cleanupFiles([targetZipFile, targetXmlFile]);
});

function parseBlocks(blockJson: any[]) : Block[] {
  return blockJson.map((block: { [x: string]: string; }) => {
    return {
      name: block['@_name'],
      codepointFirst: parseInt(block['@_first-cp'], 16),
      codepointLast: parseInt(block['@_last-cp'], 16),
    };
  })
  .filter((block: { name: string; }) => {
    return groupToBlockMapping.find(blockGroup => blockGroup.blocks.includes(block.name));
  });
}

function parseSymbols(symbolsJson: any[]) : Character[] {
  return symbolsJson.filter((rawChar: any) => isValidSymbol(rawChar))
  .map((rawChar: { [x: string]: any; }) => {
    const charpoint = parseInt(rawChar['@_cp'], 16);
    return {
      name: rawChar['@_na'],
      value: String.fromCodePoint(charpoint),
      codepoint: charpoint,
    };
  });
}

function isValidSymbol(rawChar: { [x: string]: any; }): boolean {
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

function cleanupFiles(files: string[], warn?: boolean) {
  files.forEach(file => {
    if (fs.existsSync(file)) {
      fs.unlinkSync(file);
      if (warn) {
        warning('Cleaning up file', file);
      }
    }
  });
}

interface Group {
  name: string;
  symbols: Character[];
}

interface Block {
  name: string;
  codepointFirst: number;
  codepointLast: number;
}

interface Character {
  name: string;
  value: string;
  codepoint: number;
}

function info(...message: any[]) {
  // tslint:disable-next-line: no-console
  message.forEach(msg => console.log(msg));
}

function warning(...message: any[]) {
  // tslint:disable-next-line: no-console
  message.forEach(msg => console.warn('WARN:', msg));
}

function error(...message: any[]) {
  // tslint:disable-next-line: no-console
  message.forEach(msg => console.warn('ERROR:', msg));
}
