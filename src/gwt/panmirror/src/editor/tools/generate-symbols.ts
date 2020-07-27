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

import * as fs from 'fs';
import * as os from 'os';
import * as unzip from 'unzip';
import * as parser from 'fast-xml-parser';
import * as https from 'https';

// This will enforce the 'age' of the unicode characters and only allow characters
// with an age less than or equal to this age.
const maxUnicodeAge = 6.0;

// The file that should be generated holding the symbol data
const outputFile = './src/behaviors/insert_symbol/symbols.ts';

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
    blocks: [
      'General Punctuation',
      'Supplemental Punctuation'
    ],
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

// These characters are excluded because they don't render properly in the default font. 
// Consider re-enabling them as address font issues with unicode.
const excludedChars = [
  65860, 65861, 65910, 65911, 65912, 65923, 65927, 65928, 65929, 65931, 65932, 65933, 65934, // Ancient Characters
  11094, 11095, 11096, 11097, // Arrows
  10190, 10191, 120778, 120779, // Mathematical
  9885, 9886, 9887, 9907, 9908, 9909, 9910, 9911, 9912, 9913, 9914, 9915, 9916, 9919, 9920, 9921,
  9922, 9923, 9926, 9927, 9929, 9930, 9932, 9933, 9936, 9938, 9941, 9942, 9943, 9944, 9945, 9946,
  9947, 9948, 9949, 9950, 9951, 9952, 9953, 9955, 9956, 9957, 9958, 9959, 9960, 9963, 9964, 9965,
  9966, 9967, 9974, 9979, 9980, 9982, 9983, 10079, 10080, // Miscellaneous
  9192, // technical
  11801, // punctuation
  119049, 119050, 119051, 119052, 119053, 119054, 119055, 119059, 119060, 119061, 119062, 119063,
  119064, 119065, 119066, 119067, 119068, 119069, 119071, 119072, 119075, 119076, 119077, 119078,
  119081, 119084, 119085, 119086, 119087, 119088, 119089, 119090, 119091, 119092, 119093, 119094,
  119095, 119096, 119097, 119098, 119099, 119100, 119101, 119102, 119103, 119104, 119105, 119106,
  119107, 119108, 119109, 119110, 119111, 119112, 119113, 119114, 119115, 119116, 119117, 119118,
  119119, 119120, 119121, 119122, 119123, 119124, 119125, 119126, 119127, 119128, 119129, 119130,
  119131, 119132, 119133, 119134, 119135, 119136, 119137, 119138, 119139, 119140, 119141, 119142,
  119143, 119144, 119145, 119146, 119147, 119148, 119149, 119150, 119151, 119152, 119153, 119154,
  119163, 119164, 119165, 119166, 119167, 119168, 119169, 119170, 119171, 119172, 119173, 119174,
  119175, 119176, 119177, 119178, 119179, 119180, 119181, 119182, 119183, 119184, 119185, 119188,
  119189, 119190, 119191, 119192, 119193, 119194, 119195, 119196, 119197, 119198, 119199, 119200,
  119201, 119202, 119203, 119204, 119205, 119209, 119210, 119211, 119212, 119213, 119214,
  119215, 119216, 119217, 119218, 119219, 119220, 119221, 119222, 119223, 119224, 119225, 119226,
  119227, 119228, 119229, 119230, 119231, 119232, 119233, 119234, 119235, 119236, 119237, 119238,
  119247, 119248, 119249, 119250, 119251, 119252, 119253, 119254, 119255, 119256, 119257, 119258,
  119259, 119260, 119261, // musical symbols
  8192, 8193, 8194, 8195, 8196, 8197, 8198, 8199, 8200, 8201, 8202, // en/em and other multicharacter spaces
  8232, 8233 // separator characters
];

const excludedEmoji = [
  'frowning_face',
  'smiling_face_with_tear',
  'relaxed',
  'disguised_face',
  'pinched_fingers',
  'anatomical_heart',
  'lungs',
  'ninja',
  'people_hugging',
  'bison',
  'mammoth',
  'beaver',
  'dodo',
  'feather',
  'seal',
  'beetle',
  'cockroach',
  'worm',
  'fly',
  'potted_plant',
  'blueberries',
  'olive',
  'bell_pepper',
  'flatbread',
  'tamale',
  'fondue',
  'teapot',
  'bubble_tea',
  'rock',
  'wood',
  'hut',
  'roller_skate',
  'pickup_truck',
  'magic_wand',
  'nesting_dolls',
  'pi_ata',
  'sewing_needle',
  'knot',
  'thong_sandal',
  'military_helmet',
  'accordion',
  'coin',
  'boomerang',
  'hook',
  'screwdriver',
  'ladder',
  'carpentry_saw',
  'elevator',
  'mirror',
  'window',
  'plunger',
  'toothbrush',
  'bucket',
  'mouse_trap',
  'headstone',
  'placard',
];

// These emoji do support skintone, but the default font we use doesn't render
// them properly.
const skinToneUnsupportedEmoji = [
  'raised_hand_with_fingers_splayed',
  'v',
  'point_up',
  'writing_hand',
  'red_haired_man',
  'curly_haired_man',
  'white_haired_man',
  'bald_man',
  'red_haired_woman',
  'person_red_hair',
  'curly_haired_woman',
  'person_curly_hair',
  'white_haired_woman',
  'person_white_hair',
  'bald_woman',
  'person_bald',
  'blond_haired_woman',
  'blond_haired_man',
  'frowning_man',
  'frowning_woman',
  'pouting_man',
  'pouting_woman',
  'no_good_man',
  'no_good_woman',
  'ok_man',
  'ok_woman',
  'tipping_hand_man',
  'tipping_hand_woman',
  'raising_hand_man',
  'raising_hand_woman',
  'deaf_man',
  'deaf_woman',
  'bowing_man',
  'bowing_woman',
  'man_facepalming',
  'woman_facepalming',
  'man_shrugging',
  'woman_shrugging',
  'health_worker',
  'man_health_worker',
  'woman_health_worker',
  'student',
  'man_student',
  'woman_student',
  'teacher',
  'man_teacher',
  'woman_teacher',
  'judge',
  'man_judge',
  'woman_judge',
  'farmer',
  'man_farmer',
  'cook',
  'man_cook',
  'woman_cook',
  'mechanic',
  'man_mechanic',
  'woman_mechanic',
  'factory_worker',
  'man_factory_worker',
  'woman_factory_worker',
  'office_worker',
  'man_office_worker',
  'woman_office_worker',
  'scientist',
  'man_scientist',
  'woman_scientist',
  'technologist',
  'man_technologist',
  'woman_technologist',
  'singer',
  'man_singer',
  'woman_singer',
  'artist',
  'man_artist',
  'woman_artist',
  'pilot',
  'man_pilot',
  'woman_pilot',
  'astronaut',
  'man_astronaut',
  'woman_astronaut',
  'firefighter',
  'man_firefighter',
  'woman_firefighter',
  'policeman',
  'policewoman',
  'detective',
  'male_detective',
  'female_detective',
  'guardsman',
  'guardswoman',
  'construction_worker_man',
  'construction_worker_woman',
  'man_with_turban',
  'woman_with_turban',
  'superhero_man',
  'superhero_woman',
  'supervillain_man',
  'supervillain_woman',
  'mage_man',
  'mage_woman',
  'fairy_man',
  'fairy_woman',
  'vampire_man',
  'vampire_woman',
  'merman',
  'mermaid',
  'elf_man',
  'elf_woman',
  'massage_man',
  'massage_woman',
  'haircut_man',
  'haircut_woman',
  'walking_man',
  'walking_woman',
  'standing_man',
  'standing_woman',
  'kneeling_man',
  'kneeling_woman',
  'person_with_probing_cane',
  'man_with_probing_cane',
  'woman_with_probing_cane',
  'person_in_motorized_wheelchair',
  'man_in_motorized_wheelchair',
  'woman_in_motorized_wheelchair',
  'person_in_manual_wheelchair',
  'man_in_manual_wheelchair',
  'woman_in_manual_wheelchair',
  'running_man',
  'running_woman',
  'business_suit_levitating',
  'sauna_man',
  'sauna_woman',
  'climbing_man',
  'climbing_woman',
  'golfing',
  'golfing_man',
  'golfing_woman',
  'surfing_man',
  'surfing_woman',
  'rowing_man',
  'rowing_woman',
  'swimming_man',
  'swimming_woman',
  'bouncing_ball_person',
  'bouncing_ball_man',
  'bouncing_ball_woman',
  'weight_lifting',
  'weight_lifting_man',
  'weight_lifting_woman',
  'biking_man',
  'biking_woman',
  'mountain_biking_man',
  'mountain_biking_woman',
  'man_cartwheeling',
  'woman_cartwheeling',
  'man_playing_water_polo',
  'woman_playing_water_polo',
  'man_playing_handball',
  'woman_playing_handball',
  'man_juggling',
  'woman_juggling',
  'lotus_position_man',
  'lotus_position_woman',
  'people_holding_hands',
  'woman_farmer',
  'man_feeding_baby',
  'woman_feeding_baby',
  'person_feeding_baby',
  'man_in_tuxedo',
  'woman_in_tuxedo',
  'man_with_veil',
  'woman_with_veil',
  'mx_claus',
];

// Basic file paths to use when downloading and generating the file. These files will be cleaned up
// upon completion.
const workingDirectory = os.tmpdir();
const targetFileName = 'ucd.nounihan.flat';
const targetXmlFileName = `${targetFileName}.xml`;
const targetZipFile = `${workingDirectory}/${targetFileName}.zip`;
const targetXmlFile = `${workingDirectory}/${targetXmlFileName}`;

// The path that will be used to download the unicode file. This is currently
// set to always downlod the latest. The maxUnicodeVersion attribute of each
// character is used to decide which characters to include rather than the unicode database
// version
const unicodeDownloadUrl = `https://www.unicode.org/Public/UCD/latest/ucdxml/${targetFileName}.zip`;

// The set of emoji + metadata used by Github
const emojiDownloadUrl = 'https://raw.githubusercontent.com/github/gemoji/master/db/emoji.json';
const emojiPath = './src/api/emojis-all.ts';

// The set of emoji that has markdown rendering in pandoc.
import kPandocEmojis from './emojis-pandoc.json';


// Remove any orphaned intermediary files
cleanupFiles([targetXmlFile, targetZipFile], true);


// The core sequence of steps for generating the symbols file
heading("GENERATING SYMBOLS");
downloadFile(unicodeDownloadUrl, targetZipFile)
  .then((zipFile: string) => unzipSingleZipFile(zipFile, targetXmlFileName, workingDirectory))
  .then((unzippedfile: string) => xmlToJson(unzippedfile))
  .then(jsonResult => jsonToSymbolGroups(jsonResult))
  .then(symbolGroups => symbolGroups.filter(blockGroup => blockGroup.symbols.length > 0))
  .then(symbolGroups => writeSymbolsFile(symbolGroups))
  .then(() => heading("GENERATING EMOJI"))
  .then(() => downloadFile(emojiDownloadUrl, emojiPath))
  .then((targetFile) => filterEmoji(targetFile))
  .catch((message: any) => {
    error(message);
    process.exit(1);
  })
  .finally(() => cleanupFiles([targetZipFile, targetXmlFile]));




function parseBlocks(blockJson: any[]): Block[] {
  return blockJson
    .map((block: { [x: string]: string }) => {
      return {
        name: block['@_name'],
        codepointFirst: parseInt(block['@_first-cp'], 16),
        codepointLast: parseInt(block['@_last-cp'], 16),
      };
    })
    .filter((block: { name: string }) => {
      return groupToBlockMapping.find(blockGroup => blockGroup.blocks.includes(block.name));
    });
}

function parseSymbols(symbolsJson: any[]): Character[] {
  return symbolsJson
    .filter((rawChar: any) => isValidSymbol(rawChar))
    .map((rawChar: { [x: string]: any }) => {
      const charpoint = parseInt(rawChar['@_cp'], 16);
      return {
        name: rawChar['@_na'],
        value: String.fromCodePoint(charpoint),
        codepoint: charpoint,
      };
    });
}

function isValidSymbol(rawChar: { [x: string]: any }): boolean {
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

function downloadFile(url: string, targetFile: string): Promise<string> {
  info('', 'Downloading file', url);
  const targetStream = fs.createWriteStream(targetFile);
  return new Promise((resolve, reject) => {
    https
      .get(url, response => {
        response.pipe(targetStream);
        targetStream.on('finish', () => {
          targetStream.close();
          info('done', '');
          resolve(targetFile);
        });
      })
      .on('error', err => {
        reject(err);
      });
  });
}

function unzipSingleZipFile(zipFile: string, fileToExtract: string, outputDirectory: string): Promise<string> {
  // Unzip the file
  return new Promise((resolve, reject) => {
    info('Unzipping File', zipFile);

    const readStream = fs.createReadStream(zipFile);
    readStream.pipe(unzip.Parse()).on('entry', (entry: unzip.Entry) => {
      const fileName = entry.path;
      if (fileName === fileToExtract && entry.type === 'File') {
        const outputFilePath = `${outputDirectory}/${fileToExtract}`;
        const outputStream = fs.createWriteStream(outputFilePath);
        outputStream.on('error', reject);
        outputStream.on('close', () => {
          info('done', '');
          resolve(outputFilePath);
        });


        entry.pipe(outputStream);
      } else {
        entry.autodrain();
      }
    });
  });
}

function xmlToJson(unzippedFile: string): any {
  // Parse XML -> Json
  info('Parsing', unzippedFile);
  const fileContents = fs.readFileSync(unzippedFile, 'utf8');
  const options = {
    ignoreAttributes: false,
    arrayMode: false,
  };
  const tObj = parser.getTraversalObj(fileContents, options);
  const jsonResult = parser.convertToJson(tObj, options);
  info('done', '');
  return jsonResult;
}

function jsonToSymbolGroups(jsonResult: any) {
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
        if (matchingBlock !== undefined) {
          return symbol.codepoint >= matchingBlock.codepointFirst && symbol.codepoint <= matchingBlock.codepointLast;
        } else {
          return null;
        }
      });

      return matchingBlockName != null;
    });
    info('Group ' + groupName + ' -> ' + groupSymbols.length + ' symbols');
    symbolGroups.push({ name: groupName, symbols: groupSymbols });
  });
  info('done', '');
  return symbolGroups;
}

function writeSymbolsFile(symbolGroups: Group[]) {
  // Write the output file
  info('Writing output', outputFile);
  cleanupFiles([outputFile], false);
  const finalJson = JSON.stringify(symbolGroups, null, 2);

  fs.writeFileSync(outputFile, `import { SymbolCharacterGroup } from \"./insert_symbol-dataprovider\";

const symbols: SymbolCharacterGroup[] = ${finalJson};

export default symbols;
`);

  const countSymbols = symbolGroups.reduce((count, symbolGroup) => {
    return count + symbolGroup.symbols.length;
  }, 0);
  info(countSymbols + ' total symbols generated');
  info('done', '');
}

function filterEmoji(filePath: string) {
  info('Writing emoji', filePath);
  const allEmoji = JSON.parse(fs.readFileSync(filePath, 'utf8'));

  // Remove any emoji that don't render properly
  const filteredEmoji: Array<{
    emoji: string;
    description: string;
    category: string;
    aliases: string[];
    tags?: string[];
    unicode_version?: string;
    ios_version?: string;
    skin_tones?: boolean;
  }> = allEmoji.filter((emoji: any) => !excludedEmoji.includes(emoji.aliases[0]));

  // Remove emoji metadata that we don't need
  const thinnedEmoji = filteredEmoji.map(emoji => {
    const supportsSkinTone = emoji.skin_tones && !skinToneUnsupportedEmoji.includes(emoji.aliases[0]);
    return {
      emojiRaw: emoji.emoji,
      description: emoji.description,
      category: emoji.category,
      aliases: emoji.aliases,
      supportsSkinTone,
      hasMarkdownRepresentation: kPandocEmojis.find(pandocEmoji => pandocEmoji.emoji === emoji.emoji) !== undefined,
    };
  });

  info(thinnedEmoji.length + ' emoji generated');
  const finalJson = JSON.stringify(thinnedEmoji, null, 2);

  fs.writeFileSync(emojiPath, `import { EmojiRaw } from "./emoji";

const emjois: EmojiRaw[] = ${finalJson};

export default emjois;
`);
  info('done');
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

function heading(message: string) {
  info('', '', '****************************************************************', message, '****************************************************************');
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
