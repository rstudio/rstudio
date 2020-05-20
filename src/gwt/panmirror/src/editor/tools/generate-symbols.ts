import * as https from 'https';
import * as fs from 'fs';
import * as os from 'os';
import * as unzip from 'unzip';
import * as parser from 'fast-xml-parser';

const unicodeVersion = '13.0.0';
const targetFileName = 'ucd.nounihan.flat';
const unicodeDownloadPath = `https://www.unicode.org/Public/${unicodeVersion}/ucdxml/${targetFileName}.zip`;

const includedBlocks = ['Arrows', 'Chess_Symbols', 'Alchemical', 'Dingbats', 'Geometric_Shapes'];

const targetPath = os.tmpdir();
const targetZipFile = `${targetPath}/${targetFileName}.zip`;
const targetXmlFile = `${targetPath}/${targetFileName}.xml`;
const targetTestFile = `${targetPath}/ucd.test.xml`;

cleanupFiles([targetXmlFile, targetZipFile], true);

// Download the unicode no han file in flat format
console.log('Downloading data from ' + unicodeDownloadPath);
const file = fs.createWriteStream(targetZipFile);
const request = https.get(unicodeDownloadPath, function(response) {
  if (response.statusCode !== 200) {
    console.error(`Error ${response.statusCode} downloading data`);
    return;
  }

  // Write the response data to a file
  response.pipe(file);
  file.on('finish', () => {
    console.log('Unzipping file');
    // When finished, unzip the file
    const readStream = fs.createReadStream(targetZipFile);
    readStream
      .pipe(unzip.Parse())
      .on('entry', function(entry) {
        var entryFileName = entry.path;
        if (entryFileName === `${targetFileName}.xml`) {
          entry.pipe(fs.createWriteStream(targetXmlFile));
        } else {
          entry.autodrain();
        }
      })
      .on('finish', () => {
        const fileContents = fs.readFileSync(targetXmlFile, 'utf8');

        console.log('Parsing file');
        var options = {
          ignoreAttributes: false,
          arrayMode: false,
        };

        var tObj = parser.getTraversalObj(fileContents, options);

        const jsonResult = parser.convertToJson(tObj, options);

        var possibleSymbols: Array<any> = jsonResult.ucd.repertoire.char;

        console.log(possibleSymbols.length + " possible symbols");
        possibleSymbols = possibleSymbols.filter(symbol => {
          const deprecated: string = symbol['@_dep'];
          if (deprecated === 'Y') {
            return false;
          }

          const block: string = symbol['@_blk'];
          if (!includedBlocks.includes(block)) {
            return false;
          }

          return true;
        });
        console.log(possibleSymbols.length + " symbols to be included");


        possibleSymbols.map((symbol) => {
          // TODO: emit symbols
        });


        console.log('Writing json');
      });

    cleanupFiles([targetZipFile, targetXmlFile]);
  });
});

function cleanupFiles(files: Array<string>, warn?: boolean) {
  files.forEach(file => {
    if (fs.existsSync(file)) {
      fs.unlinkSync(file);
      if (warn) {
        console.warn('WARNING - cleaning up file ' + file);
      }
    }
  });
}
