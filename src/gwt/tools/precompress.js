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

const root = process.argv[2];
if (!root || !fs.existsSync(root)) {
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
