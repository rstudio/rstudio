/*
 * locdiff.js
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import fs from 'fs';
import path from 'path';
import glob from 'glob';
import readline from 'readline';
import { createObjectCsvWriter } from 'csv-writer'

/**
 * This command-line tool is used to generate a CSV (spreadsheet) showing all localized strings
 * in the rstudio repo (taken from the GWT/Java property files, and the Electron json string 
 * files).
 *
 * It works against two copies of the repo which must be in subfolders named "new" and "old"
 * contained in the same folder as this script. Check out "old" to the previously localized
 * shipped version, and "new" to the current code you want to compare with.
 * 
 * The spreadsheet shows where the string is defined (which file), its unique ID,
 * its status, it's previous (old) and current (new) English values, and its' previous (old)
 * and current (new) French values, if any.
 * 
 * The status is one of:
 * 
 *    "added"     -- this string is in "new" but not "old"
 *    "deleted"   -- this string was in "old" but no longer in "new"
 *    "unchanged" -- no changes between "old" and "new" (in English)
 *    "changed"    - English value changed between "old" and "new"
 */

type StringStatus = 'added' | 'deleted' | 'unchanged' | 'changed' ;

/**
 * Information gathered on one localized string
 */
interface LocString {
  fileId: string,
  stringId: string,
  status: StringStatus,
  old: string,
  new: string,
  oldFR: string,
  currentFR: string
}

type PropFileStatus = 'added' | 'deleted' | 'unchanged';

/**
 * Information gathered on one localized string file
 */
interface PropFile {
  path: string,
  status: PropFileStatus,
  strings: Map<string, string>
}

/**
 * Map of property files keyed by a unique ID that is the same between "old" and "new"
 */
type PropFileMap = Map<string, PropFile>;

/**
 * main (entrypoint) - writes a csv showing loc strings diffs between two repos
 */
await main();
async function main() {

  // check for required cloned repos in 'old' and 'new'
  const oldRepo = path.join('.', 'old');
  const newRepo = path.join('.', 'new');
  if (!isRStudioRepo(oldRepo) || !isRStudioRepo(newRepo)) {
    console.error('See README.md for instructions');
    process.exit(1);
  }

  // discover property files and load their strings into memory
  const [oldENPropFiles, newENPropFiles, oldFRPropFiles, newFRPropFiles] = await loadPropertyFiles(oldRepo, newRepo);

  // compute the results
  const report: LocString[] = [];
  for (const [fileId, newENPropFile] of newENPropFiles) {
    const oldENPropFile = oldENPropFiles.get(fileId);
    const oldFRPropFile = oldFRPropFiles.get(fileId);
    const newFRPropFile = newFRPropFiles.get(fileId);
    for (const [stringId, stringValue] of newENPropFile.strings) {
     
      // assume this is a newly added string
      let status: StringStatus = 'added';
      let oldENString: string = '';
      let newENString: string = stringValue;
      let oldFRString: string = '';
      let currentFRString: string = '';

      if (oldENPropFile !== undefined) {
        // did this string exist before?
        if (oldENPropFile.strings.has(stringId)) {
          oldENString = oldENPropFile.strings.get(stringId) ?? '';

          // are English strings the same?
          if (oldENString === newENString) {
            status = 'unchanged';
            // continue; // uncomment to exclude unchanged strings
          } else {
            status = 'changed';
          }
        }

        // get the French values
        if (oldFRPropFile?.strings.has(stringId)) {
          oldFRString = oldFRPropFile.strings.get(stringId) ?? '';
        }
        if (newFRPropFile?.strings.has(stringId)) {
          currentFRString = newFRPropFile.strings.get(stringId) ?? '';
        }
      }
      report.push({
        fileId: fileId,
        stringId: stringId,
        status: status,
        old: oldENString ?? '',
        new: newENString,
        oldFR: oldFRString,
        currentFR: currentFRString
      });
    }
  }

  const createCsvWriter = createObjectCsvWriter;
  const csvWriter = createCsvWriter({
    path: 'locdiff.csv',
    header: [
      { id: 'fileId', title: 'FILE ID' },
      { id: 'stringId', title: 'STRING ID' },
      { id: 'status', title: 'STATUS' },
      { id: 'old', title: 'OLD ENGLISH VALUE' },
      { id: 'new', title: 'NEW ENGLISH VALUE' },
      { id: 'oldFR', title: 'OLD FRENCH VALUE'},
      { id: 'currentFR', title: 'CURRENT FRENCH VALUE' }
    ]
  });

  await csvWriter.writeRecords(report);
  console.log(`Wrote ${report.length} items to locdiff.csv`);
}

/**
 * @param repoPath Repository root
 * @returns true if this looks like a cloned rstudio repo
 */
function isRStudioRepo(repoPath: string) {
  if (fs.existsSync(repoPath) &&
    fs.existsSync(path.join(repoPath, 'src', 'gwt', 'src', 'org', 'rstudio')) &&
    fs.existsSync(path.join(repoPath, 'src', 'node', 'desktop', 'src'))
  ) {
    return true;
  }

  console.error(`Error: RStudio repo not found at ${repoPath}`);
  return false;
}

/**
 * Return all Java/GWT string property files for the given locale, in a map
 * keyed by a unique ID that is the same whether in "new" or "old".
 */
function getJavaPropFiles(repoRoot: string, locale: string): PropFileMap {
  const files = glob.sync(
    `${repoRoot}/**/*_${locale}.properties`,
    { ignore: `${repoRoot}/src/gwt/tools/prefs/*` });

  // sort by filename, use relative path excluding leading new/old and trailing 
  let sortedFiles = [];
  for (const file of files) {
    sortedFiles.push({
      path: file,
      id: file.slice( // strip off "new"/"old" prefix so IDs are the same in both
        'new/src/gwt/src/org/rstudio/'.length, // 'new/' has same length as 'old/'
        -'_en.properties'.length), 
    });
  }
  sortedFiles.sort((a, b) => {
    return a.path.localeCompare(b.path); 
  });

  let result: PropFileMap = new Map();
  for (const file of sortedFiles) {
    // CoreClient was renamed to CoreClientConstants; use the new name as the key 
    const key = file.id === 'core/client/CoreClient' ?  'core/client/CoreClientConstants' : file.id;
    result.set(key, { path: file.path, status: 'unchanged', strings: new Map() });
  }

  return result;
}

/**
 * Return Electron string property file for given locale. There is currently only one property 
 * file per locale for Electron strings, and we use "electron" as the unique file key.
 */
function getElectronPropFile(repoRoot: string, locale: string): [string, PropFile] {
  let path = `${repoRoot}/src/node/desktop/src/assets/locales/${locale}.json`;
  return ["electron", { path: path, status: 'unchanged', strings: new Map() }];
}

/**
 * Identify newly added property files
 */
function markNewFiles(oldEnglishPropFiles: PropFileMap, newEnglishPropFiles: PropFileMap) {
  for (const newPropertyFile of newEnglishPropFiles.entries()) {
    if (!oldEnglishPropFiles.has(newPropertyFile[0])) {
      newPropertyFile[1].status = 'added';
    }
  }
}

/**
 * Identify deleted property files
 */
function markDeletedFiles(oldEnglishPropFiles: PropFileMap, newEnglishPropFiles: PropFileMap) {
  for (const oldPropFile of oldEnglishPropFiles.entries()) {
    if (!newEnglishPropFiles.has(oldPropFile[0])) {
      oldPropFile[1].status = 'deleted';
    }
  }
}

/**
 * Parse string property file (GWT/Java) and return a map of strings keyed by their unique IDs.
 */
async function readPropertyFileStrings(propFile: string): Promise<Map<string, string>> {
  return new Promise((resolve, reject) => {

    const rl = readline.createInterface({
      input: fs.createReadStream(propFile),
      output: process.stdout,
      terminal: false
    });

    const strings: Map<string, string> = new Map();
    rl.on('line', (line) => {
      line = line.trimStart();
      if (line.charAt(0) === '#') {
        return;
      }
      if (line.trimEnd().length === 0) {
        return;
      }
      let [key, ...value] = line.split('=', 2);
      key = key.trim();
      if (key.length === 0) {
        throw new Error(`Empty key found in ${propFile}`);
      }
      if (value.length > 1) {
        throw new Error(`Too many strings found in ${key}`);
      }
      if (strings.has(key)) {
        throw new Error(`Duplicate key '${key}' in ${propFile}`);
      }
      strings.set(key, value[0] ?? '');
    });
    rl.on('close', () => resolve(strings));
    rl.on('error', reject);
  });
}

/**
 * Parse JSON property file (Electron) and return a map of strings keyed by their unique IDs.
 */
async function readJSONFileStrings(propFile: string): Promise<Map<string, string>> {
  let results = new Map();
  try {
    let fileContents = await fs.promises.readFile(propFile, 'utf8');
    const jsonStrings = JSON.parse(fileContents);
    
    // strings are stored in a two-level hierarchy, we combine these to create a unique key
    for (let parentKey in jsonStrings) {
      for (let subKey in jsonStrings[parentKey]) {
        const uniqueKey = `${parentKey}.${subKey}`;
        const strValue = jsonStrings[parentKey][subKey];
        results.set(uniqueKey, strValue);
      }
    }

  } catch (err) {
    console.error(err);
  }
  return results;
}

/**
 * Locate all GWT/Java property files and load them into memory (including their strings)
 */
async function loadJavaPropertyFiles(repo: string, locale: string): Promise<PropFileMap> {
  const propFiles = getJavaPropFiles(repo, locale);
  for (const file of propFiles.values()) {
    file.strings = await readPropertyFileStrings(file.path);
  }
  return propFiles;
}

/**
 * Locate the Electron property file and load into memory (including strings)
 */
async function loadElectronPropertyFile(repo: string, locale: string): Promise<[string, PropFile]> {
  const [key, propFile] = getElectronPropFile(repo, locale);
  propFile.strings = await readJSONFileStrings(propFile.path);
  return [key, propFile];
}

/**
 * Locate and load all strings from old and new for each language.
 */
async function loadPropertyFiles(oldRepo: string, newRepo: string)
      : Promise<[PropFileMap, PropFileMap, PropFileMap, PropFileMap]> {
  
  // locate and load contents of Java/GWT property files (English)
  const oldEnglishPropFiles = await loadJavaPropertyFiles(oldRepo, 'en');
  const newEnglishPropFiles = await loadJavaPropertyFiles(newRepo, 'en');
  markNewFiles(oldEnglishPropFiles, newEnglishPropFiles);
  markDeletedFiles(oldEnglishPropFiles, newEnglishPropFiles);

  // locate and load contents of Java/GWT property files (French)
  const oldFrenchPropFiles = await loadJavaPropertyFiles(oldRepo, 'fr');
  const newFrenchPropFiles = await loadJavaPropertyFiles(newRepo, 'fr');
  markNewFiles(oldFrenchPropFiles, newFrenchPropFiles);
  markDeletedFiles(oldFrenchPropFiles, newEnglishPropFiles);

  // add in the Electron property files (one each for 'en' and 'fr')
  const [oldEnglishKey, oldEnglishElectronPropFile] = await loadElectronPropertyFile(oldRepo, 'en');
  oldEnglishPropFiles.set(oldEnglishKey, oldEnglishElectronPropFile);

  const [newEnglishKey, newEnglishElectronPropFile] = await loadElectronPropertyFile(newRepo, 'en');
  newEnglishPropFiles.set(newEnglishKey, newEnglishElectronPropFile);

  const [oldFrenchKey, oldFrenchElectronPropFile] = await loadElectronPropertyFile(oldRepo, 'fr');
  oldFrenchPropFiles.set(oldFrenchKey, oldFrenchElectronPropFile);

  const [newFrenchKey, newFrenchElectronPropFile] = await loadElectronPropertyFile(newRepo, 'fr');
  newFrenchPropFiles.set(newFrenchKey, newFrenchElectronPropFile);

  return [oldEnglishPropFiles, newEnglishPropFiles, oldFrenchPropFiles, newFrenchPropFiles];
}
