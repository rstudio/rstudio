import * as https from 'https';
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
    alias: 'Emoji',
    blocks: [
      'Emoticons',
      'Miscellaneous Symbols and Pictographs',
      'Symbols and Pictographs Extended-A',
      'Transport and Map Symbols',
      'Supplemental Symbols and Pictographs',
    ],
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
const targetPath = os.tmpdir();
const targetFileName = 'ucd.nounihan.flat';
const targetZipFile = `${targetPath}/${targetFileName}.zip`;
const targetXmlFile = `${targetPath}/${targetFileName}.xml`;

// The path that will be used to download the unicode file. This is currently
// set to always downlod the latest. The maxUnicodeVersion attribute of each
// character is used to decide which characters to include rather than the unicode database
// version
const unicodeDownloadPath = `https://www.unicode.org/Public/UCD/latest/ucdxml/${targetFileName}.zip`;

// Remove any orphaned intermediary files
cleanupFiles([targetXmlFile, targetZipFile], true);

// Download the file
message('Downloading data', unicodeDownloadPath);
https.get(unicodeDownloadPath, function(response) {
  if (response.statusCode !== 200) {
    error(`Error ${response.statusCode} downloading data.`);
    return;
  }
  message('');

  message('Writing', targetZipFile);
  const file = fs.createWriteStream(targetZipFile);

  // Write the response data to a file
  response.pipe(file);
  message('');

  file.on('finish', () => {
    message('Unzipping File', targetZipFile);

    // When finished, unzip the file
    const readStream = fs.createReadStream(targetZipFile);
    readStream.pipe(unzip.Extract({ path: targetPath })).on('close', () => {
      message('');

      // Read the file and emit the JSON
      readXmlFileAndGenerateJsonFile(targetXmlFile, outputFile);
      cleanupFiles([targetZipFile, targetXmlFile]);
    });
  });
});

function readXmlFileAndGenerateJsonFile(targetXmlFile: string, outputFile: string) {
  const fileContents = fs.readFileSync(targetXmlFile, 'utf8');

  message('Parsing', targetXmlFile);
  var options = {
    ignoreAttributes: false,
    arrayMode: false,
  };
  const tObj = parser.getTraversalObj(fileContents, options);
  const jsonResult = parser.convertToJson(tObj, options);

  // Read the block from the XML file and filter it into the typed blocks
  const allIncludedBlocks: Array<Block> = jsonResult.ucd.blocks.block.map(block => {
    return {
      name: block['@_name'],
      codepointFirst: parseInt(block['@_first-cp'], 16),
      codepointLast: parseInt(block['@_last-cp'], 16),
    };
  })
  .filter(block => {
    return groupToBlockMapping.find(blockGroup => blockGroup.blocks.includes(block.name));
  });

  // Read the symbols from the XML file and filter it into the typed blocks
  const allValidSymbols: Array<Symbol> = jsonResult.ucd.repertoire.char
    .filter(rawChar => isValidSymbol(rawChar))
    .map(rawChar => {
      const charpoint = parseInt(rawChar['@_cp'], 16);
      return {
        name: rawChar['@_na'],
        value: String.fromCodePoint(charpoint),
        codepoint: charpoint,
        isEmoji: rawChar['@_Emoji'] === 'Y',
        allowModifier: rawChar['@_EBase'] === 'Y'
      };
    });
  message(allIncludedBlocks.length + ' Blocks read', allValidSymbols.length + ' Symbols read', '');

  message('Generating definitions');
  var blockGroups: SymbolGroup[] = new Array<SymbolGroup>();
  groupToBlockMapping.forEach(groupToBlockMapping => {
    const groupName = groupToBlockMapping.alias;
    const groupSymbols = allValidSymbols.filter(symbol => {
      const matchingBlockName = groupToBlockMapping.blocks.find(blockName => {
        const matchingBlock = allIncludedBlocks.find(block => block.name === blockName);
        return symbol.codepoint >= matchingBlock.codepointFirst && symbol.codepoint <= matchingBlock.codepointLast;
      });

      if (matchingBlockName != null) {
        return symbol;
      }
    });

    message('Group ' + groupName + ' -> ' + groupSymbols.length + ' symbols');
    blockGroups.push({ name: groupName, symbols: groupSymbols });
  });
  message('');

  // Filter out blocks so we only include eligible blocks that include at least one character
  blockGroups = blockGroups.filter(blockGroup => blockGroup.symbols.length > 0);

  message('Writing json', outputFile);
  const finalJson = JSON.stringify(blockGroups, null, 2);
  cleanupFiles([outputFile], false);
  fs.writeFileSync(outputFile, finalJson);

  message('');
  message('DONE');
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

  // No emoji components or modifiers
  const isEmojiComponent = rawChar['@_EComp'] === 'Y';
  const isEmojiModifier = rawChar['@_EMod'] === 'Y';
  if (isEmojiComponent || isEmojiModifier) {
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
  isEmoji: boolean;
  allowModifier: boolean;
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
