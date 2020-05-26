import * as https from 'https';
import * as fs from 'fs';
import * as os from 'os';
import * as unzip from 'unzip';
import * as parser from 'fast-xml-parser';

// This will check the 'age' of the unicode characters and only allow characters
// with an age less than or equal to this age.
const maxUnicodeAge = 6.0;

// The file that should be generated holding the symbol data
const outputFile = './src/behaviors/symbol/insert_symbol-data.json';

// The names of blocks of unicode characters to be scan for characters to include.
// Blocks will only be included if characters from that block are selected (e.g. characters)
// might not meet the maxUnicodeVersion requirement, may be depcrecated or so on).
const includedBlocks  = [
  { 
    alias: "Miscellaneous", 
    blocks: [  
      'Latin-1 Supplement',
      'Enclosed Alphanumerics',
      'Dingbats',
      'Miscellaneous Symbols',
  ]},
  { 
    alias: "Mathematical", 
    blocks: [  
      'Mathematical Operators',
      'Miscellaneous Mathematical Symbols-A',
      'Miscellaneous Mathematical Symbols-B',
      'Supplemental Mathematical Operators',
      'Mathematical Alphanumeric Symbols',
  ]},
  { 
    alias: "Punctuation", 
    blocks: [  
      'Supplemental Punctuation',
  ]},
  { 
    alias: "Technical", 
    blocks: [  
      'Miscellaneous Technical',
  ]},
  { 
    alias: "Arrows", 
    blocks: [  
      'Miscellaneous Symbols and Arrows',
      'Supplemental Arrows-A',
      'Supplemental Arrows-B'      
  ]},
  { 
    alias: "Ancient", 
    blocks: [  
      'Ancient Symbols',
      'Ancient Greek Numbers',
  ]},
  { 
    alias: "Braille", 
    blocks: [  
      'Braille Patterns',
  ]},
  { 
    alias: "Currency", 
    blocks: [  
      'Currency Symbols',
  ]},
  { 
    alias: "Game Symbols", 
    blocks: [  
      'Mahjong Tiles',
      'Domino Tiles',
      'Playing Cards',
      'Chess Symbols',    
  ]},
  { 
    alias: "Emoji", 
    blocks: [  
      'Emoticons',
      'Miscellaneous Symbols and Pictographs',
      'Symbols and Pictographs Extended-A',
      'Transport and Map Symbols',
      'Supplemental Symbols and Pictographs',    
  ]},
  { 
    alias: "Music", 
    blocks: [  
      'Musical Symbols',
  ]},
  { 
    alias: "Geometric Shapes", 
    blocks: [  
      'Geometric Shapes',
      'Geometric Shapes Extended',
  ]},
  { 
    alias: "Ideographic", 
    blocks: [  
      'Ideographic Description Characters',
      'Ideographic Symbols and Punctuation', 
  ]},
];

const excludedChars = [
  160 // no-break space
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
  
  var blockGroups: BlockGroup[] = new Array<BlockGroup>();
  var symbolsToWrite: Symbol[] = new Array<Symbol>();

  message('Parsing',  targetXmlFile);
  var options = {
    ignoreAttributes: false,
    arrayMode: false,
  };
  var tObj = parser.getTraversalObj(fileContents, options);
  const jsonResult = parser.convertToJson(tObj, options);
  var eligibleBlocks: Array<any> = jsonResult.ucd.blocks.block;
  var eligibleSymbols: Array<any> = jsonResult.ucd.repertoire.char; 
  message(eligibleBlocks.length + ' Blocks read');
  message(eligibleSymbols.length + ' Symbols read');
  message('');

  // Read the blocks and filter to the list specified above
  message('Generating Blocks and Aliases'); 
  includedBlocks.forEach(includedBlock => {
    const alias = includedBlock.alias;   
    const blocksAndCodePoints = includedBlock.blocks.map(block => {
      const rawBlock = eligibleBlocks.find(eligibleBlock => eligibleBlock['@_name'] === block);
      if (rawBlock) {
        const firstcp = Number.parseInt(rawBlock['@_first-cp'], 16);
        const lastcp = Number.parseInt(rawBlock['@_last-cp'], 16);
        return { name: block, codepointFirst: firstcp, codepointLast: lastcp };
      }
    })
    blockGroups.push({alias: alias, blocks: blocksAndCodePoints});
  });

  message(blockGroups.length + ' blocks will be scanned for characters');
  message('');

  // Read the symbols and filter out deprecated, characters that are not of the 
  // proper version, and characters that aren't included in eligible block
  message('Generating Symbols');
  eligibleSymbols.forEach(symbol => {

    // no deprecated characters
    const deprecated: string = symbol['@_dep'];
    if (deprecated === 'Y') {
      return;
    }

    // no characters introduced after the maximum age
    const age = Number.parseFloat(symbol['@_age']);
    if (age > maxUnicodeAge) {
      return;
    } 

    // no control characters, private chararacters or other weird types
    const generalCategory = symbol['@_gc'];
    if (['Cc','C','Cf','Cn','Co','Cs'].includes(generalCategory)) {
      return;
    }

    const codepoint = Number.parseInt(symbol['@_cp'], 16);

    // no exluded characters
    if (excludedChars.includes(codepoint))
    {
      return;
    }
    
    // At least one of our blocks owns this character
    const owningBlock = blockGroups.find(blockAlias => 
      {
        return blockAlias.blocks.find(block => 
          {
            return codepoint >= block.codepointFirst && codepoint <= block.codepointLast
          });
      });

    if (owningBlock) {
      symbolsToWrite.push({
        name: symbol['@_na'],
        value: String.fromCodePoint(codepoint),
        codepoint: codepoint,
      });    
    }

    return;
  });

  message(eligibleSymbols.length + ' symbols eligible to be exported');
  message('');

  // Filter out blocks so we only include eligible blocks that include at least one character
  blockGroups = blockGroups.filter(blockAlias => 
    { 
      return blockAlias.blocks.find(block => 
        symbolsToWrite.find(symbol =>
            block.codepointFirst && symbol.codepoint <= block.codepointLast)
  )});
  
  message('Writing json', outputFile);
  const finalObject = {
    blocks: blockGroups,
    symbols: symbolsToWrite,
  };
  const finalJson = JSON.stringify(finalObject, null, 2);
  cleanupFiles([outputFile], false);
  fs.writeFileSync(outputFile, finalJson);
  
  message('');
  message('DONE');
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

interface BlockGroup {
  alias: string;
  blocks: Array<Block>
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
  message.forEach((msg) => console.log(msg));}

function warning(...message: any[]) {
  message.forEach((msg) => console.warn("WARN:", msg));
}

function error(...message: any[]) {
  message.forEach((msg) => console.warn("ERROR:", msg));
}


