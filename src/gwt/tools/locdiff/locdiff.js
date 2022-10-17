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

await main();
async function main() {

  // check for required cloned repos in 'old' and 'new'
  const oldRepo = path.join('.', 'old');
  const newRepo = path.join('.', 'new');
  if (!isRStudioRepo(oldRepo) || !isRStudioRepo(newRepo)) {
    console.error('See README.md for instructions');
    process.exit(1);
  }

  // load property files
  const [oldENPropFiles, newENPropFiles, oldFRPropFiles, newFRPropFiles] =
    await loadPropertyFiles(oldRepo, newRepo);

  // compute the results
  const report = [];
  for (const [fileId, newPropFile] of newENPropFiles) {
    const oldPropFile = oldENPropFiles.get(fileId);
    const newFRPropFile = newFRPropFiles.get(fileId);
    for (const [stringId, stringValue] of newPropFile.strings) {
      
      // assume this is a newly added property file
      let status = 'added';
      let oldString = '';
      let newString = stringValue;
      let currentFRString = '';

      if (oldPropFile !== undefined) {
        // did this string exist before?
        if (oldPropFile.strings.has(stringId)) {
          oldString = oldPropFile.strings.get(stringId);

          // get the current French value
          if (newFRPropFile.strings.has(stringId)) {
            currentFRString = newFRPropFile.strings.get(stringId);
          }

          // are English strings the same?
          if (oldString === newString) {
            status = 'unchanged';
            continue; // don't output unchanged strings
          } else {
            status = 'changed';
          }
        }
      } 
      report.push({
        fileId: fileId,
        stringId: stringId,
        status: status,
        old: oldString,
        new: newString,
        currentFR: currentFRString
      });
    }
  }

  const createCsvWriter = createObjectCsvWriter;
  const csvWriter = createCsvWriter({
    path: 'locdiff-GWT.csv',
    header: [
      { id: 'fileId', title: 'FILE ID' },
      { id: 'stringId', title: 'STRING ID' },
      { id: 'status', title: 'STATUS' },
      { id: 'old', title: 'OLD VALUE' },
      { id: 'new', title: 'NEW VALUE' },
      { id: 'currentFR', title: 'CURRENT FRENCH STRING' }
    ]
  });

  await csvWriter.writeRecords(report);
  console.log(`Wrote ${report.length} items to locdiff-GWT.csv`);
}

function gwtRoot(repoRoot) {
  return path.join(repoRoot, 'src', 'gwt', 'src', 'org', 'rstudio');
}

function electronRoot(repoRoot) {
  return path.join(repoRoot, 'src', 'node', 'desktop', 'src');
}

function isRStudioRepo(repoPath) {
  if (fs.existsSync(repoPath) &&
    fs.existsSync(gwtRoot(repoPath)) &&
    fs.existsSync(electronRoot(repoPath))
  ) {
    return true;
  }

  console.error(`Error: RStudio repo not found at ${repoPath}`);
  return false;
}

function getJavaPropFiles(repoRoot, locale) {
  const files = glob.sync(
    `${repoRoot}/**/*_${locale}.properties`,
    { ignore: `${repoRoot}/src/gwt/tools/prefs/*` });

  // sort by filename, use relative path excluding leading new/old and trailing 
  let sortedFiles = [];
  for (const file of files) {
    sortedFiles.push({
      path: file,
      id: file.slice(
        'new/src/gwt/src/org/rstudio/'.length, // 'new/' has same length as 'old/'
        -'_en.properties'.length), 
    });
  }
  sortedFiles.sort((a, b) => {
    return a.path.localeCompare(b.path); 
  });

  let result = new Map();
  for (const file of sortedFiles) {
    // CoreClient was renamed to CoreClientConstants; use the new name as the key 
    const key = file.id === 'core/client/CoreClient' ?  'core/client/CoreClientConstants' : file.id;
    result.set(key, { path: file.path, status: '', strings: undefined });
  }

  return result;
}

/**
 * Identify newly added property files
 */
function markNewFiles(oldEnglishPropFiles, newEnglishPropFiles) {
  for (const newKey of newEnglishPropFiles.keys()) {
    if (!oldEnglishPropFiles.has(newKey)) {
      newEnglishPropFiles.get(newKey).status = 'added';
    }
  }
}

/**
 * Identify deleted property files
 */
function markDeletedFiles(oldEnglishPropFiles, newEnglishPropFiles) {
  for (const oldKey of oldEnglishPropFiles.keys()) {
    if (!newEnglishPropFiles.has(oldKey)) {
      oldEnglishPropFiles.get(oldKey).status = 'deleted';
    }
  }
}

async function readStrings(propFile) {
  return new Promise((resolve, reject) => {

    const rl = readline.createInterface({
      input: fs.createReadStream(propFile.path),
      output: process.stdout,
      terminal: false
    });

    const strings = new Map();
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
        throw new Error(`Empty key found in ${propFile.path}`);
      }
      if (value.length > 1) {
        throw new Error(`Too many strings found in ${key}`);
      }
      if (strings.has(key)) {
        throw new Error(`Duplicate key '${key}' in ${propFile.path}`);
      }
      strings.set(key, value[0] ?? '');
    });
    rl.on('close', () => resolve(strings));
    rl.on('error', reject);
  });
}

async function loadPropertyFile(repo, locale) {
  const propFiles = getJavaPropFiles(repo, locale);
  for (const file of propFiles.values()) {
    file.strings = await readStrings(file);
  }
  return propFiles;
}

async function loadPropertyFiles(oldRepo, newRepo) {
  const oldEnglishPropFiles = await loadPropertyFile(oldRepo, 'en');
  const newEnglishPropFiles = await loadPropertyFile(newRepo, 'en');
  markNewFiles(oldEnglishPropFiles, newEnglishPropFiles);
  markDeletedFiles(oldEnglishPropFiles, newEnglishPropFiles);

  const oldFrenchPropFiles = await loadPropertyFile(oldRepo, 'fr');
  const newFrenchPropFiles = await loadPropertyFile(newRepo, 'fr');
  markNewFiles(oldFrenchPropFiles, newFrenchPropFiles);
  markDeletedFiles(oldFrenchPropFiles, newEnglishPropFiles);

  return [oldEnglishPropFiles, newEnglishPropFiles, oldFrenchPropFiles, newFrenchPropFiles];
}
