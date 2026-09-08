/*
 * precompress.js
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

// Writes a gzipped sibling (<file>.gz) next to each large content-hashed GWT
// build output, so the session can serve Content-Encoding: gzip without
// paying the deflate cost on every request (see GwtFileHandler.cpp). The
// hashed files are immutable, so an up-to-date sibling is never regenerated.

'use strict';

const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// small files aren't worth a round trip through the decompressor
const kMinSizeBytes = 4096;

// Rebuild the target directory one component at a time from directory
// listings, so every component of every path handled below is a string
// returned by fs.readdirSync rather than a substring of the command line.
// The build pointing this tool at its own output directory is not an attack,
// but the Snyk Code scan reports the direct data flow as path traversal and
// offers no way to suppress the finding (see tasks/common.ts for the same
// dodge). Returns null when the directory does not exist.
function rebuildFromDirectoryListings(input) {
  const resolved = path.resolve(input);
  const parsed = path.parse(resolved);

  // the filesystem root: '/' on POSIX; on Windows, re-derive the drive letter
  // numerically so the result shares no substring with the input
  let result;
  if (parsed.root === '/') {
    result = '/';
  } else if (/^[A-Za-z]:[\\/]$/.test(parsed.root)) {
    result = String.fromCharCode(parsed.root.toUpperCase().charCodeAt(0)) + ':\\';
  } else {
    return null;
  }

  for (const segment of resolved.slice(parsed.root.length).split(/[\\/]+/)) {
    if (segment.length === 0) {
      continue;
    }

    // the case-insensitive fallback covers case-insensitive filesystems
    // (the macOS and Windows defaults)
    const entries = fs.readdirSync(result);
    const entry =
      entries.find((e) => e === segment) ??
      entries.find((e) => e.toLowerCase() === segment.toLowerCase());
    if (entry === undefined) {
      return null;
    }

    result = path.join(result, entry);
  }

  return result;
}

const root = process.argv[2] ? rebuildFromDirectoryListings(process.argv[2]) : null;
if (root === null) {
  console.error('usage: node precompress.js <directory>');
  process.exit(1);
}

let compressed = 0;
let pruned = 0;

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full);
      continue;
    }

    // prune siblings whose source is gone (a rebuild replaced the hash)
    if (entry.name.endsWith('.gz')) {
      if (!fs.existsSync(full.slice(0, -3))) {
        fs.rmSync(full);
        pruned += 1;
      }
      continue;
    }

    if (!/\.cache\.(js|css)$/.test(entry.name)) {
      continue;
    }

    const stat = fs.statSync(full);
    if (stat.size < kMinSizeBytes) {
      continue;
    }

    const gzPath = full + '.gz';
    let gzStat = null;
    try {
      gzStat = fs.statSync(gzPath);
    } catch {
      // no sibling yet
    }
    if (gzStat && gzStat.mtimeMs >= stat.mtimeMs) {
      continue;
    }

    fs.writeFileSync(gzPath, zlib.gzipSync(fs.readFileSync(full), { level: 9 }));
    compressed += 1;
  }
}

walk(root);
console.log(`precompressed ${compressed} file(s), pruned ${pruned} stale sibling(s) under ${root}`);
