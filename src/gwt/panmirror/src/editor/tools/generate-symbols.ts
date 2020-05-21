import * as https from 'https';
import * as fs from 'fs';
import * as os from 'os';
import * as unzip from 'unzip';
import * as parser from 'fast-xml-parser';

// This will check the 'age' of the unicode characters and only allow characters
// with an age less than or equal to this age.
const maxUnicodeAge = 3.0;

// The file that should be generated holding the symbol data
const outputFile = './src/behaviors/insert_symbol-data.json';


// TODO: Whats up with musical symbols - lots of boxes. Is there a char i can filter?

// The names of blocks of unicode characters to be scan for characters to include.
// Blocks will only be included if characters from that block are selected (e.g. characters)
// might not meet the maxUnicodeVersion requirement, may be depcrecated or so on).
const includedBlockNames = [
  'Emoticons',
  'Latin-1 Supplement',
  'Enclosed Alphanumerics',
  'Mahjong Tiles',
  'Braille Patterns',
  'Currency Symbols',
  'Mathematical Operators',
  'Miscellaneous Technical',
  'Box Drawing',
  'Block Elements',
  'Control Pictures',
  'Geometric Shapes',
  'Miscellaneous Symbols',
  'Dingbats',
  'Miscellaneous Mathematical Symbols-A',
  'Supplemental Arrows-A',
  'Supplemental Arrows-B',
  'Miscellaneous Mathematical Symbols-B',
  'Supplemental Mathematical Operators',
  'Miscellaneous Symbols and Arrows',
  'Supplemental Punctuation',
  'Ideographic Description Characters',
  'Variation Selectors',
  'Vertical Forms',
  'Ancient Greek Numbers',
  'Ancient Symbols',
  'Ideographic Symbols and Punctuation',
  'Musical Symbols',
  'Mathematical Alphanumeric Symbols',
  'Domino Tiles',
  'Playing Cards',
  'Miscellaneous Symbols and Pictographs',
  'Transport and Map Symbols',
  'Alchemical Symbols',
  'Geometric Shapes Extended',
  'Geometric Shapes Extended',
  'Supplemental Symbols and Pictographs',
  'Chess Symbols',
  'Symbols and Pictographs Extended-A',
  'Emoticons',
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
  
  var blocksToWrite: Block[] = new Array<Block>();
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
  message('Generating Blocks'); 
  eligibleBlocks.forEach(block => {
    const name = block['@_name'];
    const firstcp = parseInt(block['@_first-cp'], 16);
    const lastcp = parseInt(block['@_last-cp'], 16);
    if (includedBlockNames.includes(name)) {
      blocksToWrite.push({ name: name, codepointFirst: firstcp, codepointLast: lastcp });
    }
  });
  message(blocksToWrite.length + ' blocks will be scanned for characters');
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
    var block: Block = undefined;
    if (codepoint) {
      block = blocksToWrite.find(block => {
        if (codepoint >= block.codepointFirst && codepoint <= block.codepointLast) {
          symbolsToWrite.push({
            name: symbol['@_na'],
            value: String.fromCodePoint(codepoint),
            codepoint: codepoint,
          });    
        }
      });
    }
    return;
  });

  message(eligibleSymbols.length + ' symbols eligible to be exported');
  message('');

  // Filter out blocks so we only include eligible blocks that include at least one character
  blocksToWrite = blocksToWrite.filter(block => {
    return symbolsToWrite.find(symbol => {
      return symbol.codepoint >= block.codepointFirst && symbol.codepoint <= block.codepointLast;
    });  });


  message('Writing json', outputFile);
  const finalObject = {
    blocks: blocksToWrite,
    symbols: symbolsToWrite,
  };
  const finalJson = JSON.stringify(finalObject, null, 2);
  cleanupFiles([outputFile], false);
  fs.writeFileSync(outputFile, finalJson);
  
  message('');
  allgood('DONE');
}

function cleanupFiles(files: Array<string>, warn?: boolean) {
  files.forEach(file => {
    if (fs.existsSync(file)) {
      fs.unlinkSync(file);
      if (warn) {
        warning('WARNING - cleaning up file ' + file);
      }
    }
  });
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

const fgGreen = '\x1b[32m';
const fgYellow = '\x1b[33m';
const fgRed = '\x1b[31m';
const reset = '\x1b[0m';

function message(...message: any[]) {
  message.forEach((msg) => console.log(msg));}

function warning(...message: any[]) {
  message.forEach((msg) => console.warn(fgYellow, msg, reset));
}

function error(...message: any[]) {
  message.forEach((msg) => console.warn(fgRed, msg, reset));
}

function allgood(...message: any[]) {
  message.forEach((msg) => console.log(fgGreen, msg, reset));
}

