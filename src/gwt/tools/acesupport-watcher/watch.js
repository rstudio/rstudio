#!/usr/bin/env node

/**
 * Acesupport File Watcher
 *
 * Watches for changes in src/gwt/acesupport and automatically
 * concatenates JavaScript files into acesupport.js.
 *
 * This provides automatic rebuilding during development without
 * needing to manually run `ant acesupport`.
 */

const chokidar = require('chokidar');
const fs = require('fs');
const path = require('path');

// Resolve paths relative to this script's location
const scriptDir = __dirname;
const gwtDir = path.resolve(scriptDir, '../..');
const acesupportSourceDir = path.resolve(gwtDir, 'acesupport');
const acesupportOutputFile = path.resolve(
   gwtDir,
   'src/org/rstudio/studio/client/workbench/views/source/editors/text/ace/acesupport.js'
);
const pidFile = path.join(scriptDir, 'watcher.pid');
const logFile = path.join(scriptDir, 'watcher.log');

// Debounce delay in milliseconds
const DEBOUNCE_DELAY = 200;

// Set up logging to file
const logStream = fs.createWriteStream(logFile, { flags: 'a' });

function log(message) {
   const timestamp = new Date().toISOString();
   const line = `[${timestamp}] ${message}`;
   console.log(line);
   logStream.write(line + '\n');
}

// Write PID file for process management
fs.writeFileSync(pidFile, process.pid.toString(), 'utf8');
log(`Started with PID ${process.pid}`);

/**
 * Check if a file path is a JavaScript file we care about
 */
function isWatchedJsFile(filePath) {
   const fileName = path.basename(filePath);
   return filePath.endsWith('.js') && fileName !== 'extern.js';
}

/**
 * Recursively get all .js files in a directory, sorted for consistent output
 */
function getJsFiles(dir) {
   const files = [];

   function walk(currentDir) {
      const entries = fs.readdirSync(currentDir, { withFileTypes: true });
      for (const entry of entries) {
         const fullPath = path.join(currentDir, entry.name);
         if (entry.isDirectory()) {
            walk(fullPath);
         } else if (entry.isFile() && isWatchedJsFile(fullPath)) {
            files.push(fullPath);
         }
      }
   }

   walk(dir);
   return files.sort();
}

/**
 * Concatenate all acesupport JS files into a single output file
 */
function concatenateFiles() {
   const startTime = Date.now();

   try {
      const files = getJsFiles(acesupportSourceDir);
      const contents = files.map((file) => fs.readFileSync(file, 'utf8'));
      const concatenated = contents.join('\n');

      fs.writeFileSync(acesupportOutputFile, concatenated, 'utf8');

      const elapsed = Date.now() - startTime;
      log(`Rebuilt acesupport.js (${files.length} files, ${elapsed}ms)`);
   } catch (err) {
      log(`Error: ${err.message}`);
   }
}

/**
 * Debounced rebuild - batches rapid file changes
 */
let debounceTimer = null;
let pendingChanges = [];

function scheduleRebuild(event, filePath) {
   const relativePath = path.relative(acesupportSourceDir, filePath);
   pendingChanges.push(`${event}: ${relativePath}`);

   if (debounceTimer) {
      clearTimeout(debounceTimer);
   }

   debounceTimer = setTimeout(() => {
      log(`Files changed: ${pendingChanges.join(', ')}`);
      pendingChanges = [];
      debounceTimer = null;
      concatenateFiles();
   }, DEBOUNCE_DELAY);
}

/**
 * Handle a file change event
 */
function handleFileChange(event, filePath) {
   if (!isWatchedJsFile(filePath)) {
      return;
   }
   scheduleRebuild(event, filePath);
}

log('Starting...');
log(`Watching: ${acesupportSourceDir}`);
log(`Output: ${acesupportOutputFile}`);
log(`Log file: ${logFile}`);

// Watch the entire acesupport directory (chokidar v4 doesn't support globs)
const watcher = chokidar.watch(acesupportSourceDir, {
   persistent: true,
   ignoreInitial: true,
   recursive: true,
});

watcher
   .on('add', (filePath) => handleFileChange('added', filePath))
   .on('change', (filePath) => handleFileChange('changed', filePath))
   .on('unlink', (filePath) => handleFileChange('removed', filePath))
   .on('error', (error) => {
      log(`Error: ${error.message}`);
   })
   .on('ready', () => {
      log('Ready and watching for changes');
   });

// Clean up PID file on shutdown
function cleanup() {
   try {
      fs.unlinkSync(pidFile);
   } catch (e) {
      // Ignore errors removing PID file
   }
   logStream.end();
}

// Handle graceful shutdown
process.on('SIGINT', () => {
   log('Shutting down (SIGINT)...');
   cleanup();
   watcher.close().then(() => process.exit(0));
});

process.on('SIGTERM', () => {
   log('Shutting down (SIGTERM)...');
   cleanup();
   watcher.close().then(() => process.exit(0));
});
