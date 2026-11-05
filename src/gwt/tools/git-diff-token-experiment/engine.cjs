/*
 * Copyright (C) 2026 by Posit Software, PBC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { performance } = require('node:perf_hooks');
const vm = require('node:vm');

const ROOT = path.resolve(__dirname, '../../../..');
const STRATEGIES = ['current', 'bare', 'indent', 'padded', 'indent-padded', 'structured'];

// Standalone so the same function can run inside Ace's VM. The first four
// token variants key on complete lines, like the existing fallback. The
// structured variant also preserves Ace's trim-equality relationship.
function tokenize(base, lines, strategy) {
   const ids = new Map();
   function encode(line) {
      const structured = strategy === 'structured';
      const key = structured ? line.trim() : line;
      if (!ids.has(key))
         ids.set(key, ids.size);

      let id = ids.get(key);
      let token = '';
      do {
         token += String.fromCharCode(0x4E00 + id % 20992);
         id = Math.floor(id / 20992);
      } while (id > 0);

      const padded = strategy.includes('padded') || structured;
      if (padded) {
         // Blank lines must still contribute zero non-whitespace characters.
         if (!line.trim())
            return line;
         token = token.padEnd(Math.min(5, line.replace(/\s/g, '').length), '.');
      }
      if (structured)
         return line.match(/^\s*/)[0] + token + line.match(/\s*$/)[0];
      if (strategy === 'indent' || strategy === 'indent-padded')
         return line.match(/^[ \t]*/)[0] + token;
      return token;
   }
   return { base: base.map(encode), lines: lines.map(encode) };
}

function replaceOnce(source, before, after) {
   assert.equal(source.split(before).length, 2, `Expected one instrumentation site: ${before}`);
   return source.replace(before, after);
}

function createEngine() {
   const state = {};
   const context = vm.createContext({ console, setTimeout, clearTimeout, performance, experiment: state });
   context.window = context;
   const aceDir = path.join(ROOT, 'src/gwt/src/org/rstudio/studio/client/workbench/views/source/editors/text/ace');
   vm.runInContext(fs.readFileSync(path.join(aceDir, 'ace-uncompressed.js'), 'utf8'), context);
   context.require('ace/lib/dom').useStrictCSP(true);

   // Instrument only the in-memory provider. Preserve its algorithms and
   // surface the timeout phase that the public provider currently discards.
   let provider = fs.readFileSync(path.join(aceDir, 'ext-diff-uncompressed.js'), 'utf8');
   provider = replaceOnce(
      provider,
      'var hitTimeout = lineAlignmentResult.hitTimeout;',
      'var hitTimeout = lineAlignmentResult.hitTimeout; experiment.alignmentTimeouts.push(hitTimeout);'
   );
   provider = replaceOnce(
      provider,
      'exports.computeDiff = computeDiff;',
      'exports.computeDiff = computeDiff; exports.experimentComputer = DefaultLinesDiffComputer;'
   );
   vm.runInContext(provider, context);
   const Computer = context.require('ace/ext/diff/providers/default').experimentComputer;
   const refine = Computer.prototype.refineDiff;
   Computer.prototype.refineDiff = function(...args) {
      const started = performance.now();
      const result = refine.apply(this, args);
      state.refinementMs += performance.now() - started;
      state.refinementTimeouts += result.hitTimeout ? 1 : 0;
      return result;
   };

   vm.runInContext(`var experimentTokenize = ${tokenize.toString()};`, context);
   let gutter = fs.readFileSync(path.join(ROOT, 'src/gwt/acesupport/git_diff_gutter.js'), 'utf8');
   gutter = replaceOnce(
      gutter,
      'this.computeChunks = function(base, lines) {',
      'this.computeChunks = function(base, lines) { MAX_COMPUTATION_MS = experiment.budgetMs;'
   );
   // Insert the candidate after the real trimming / empty-input handling.
   // A side-by-side view keeps the current path and its character ranges.
   gutter = replaceOnce(
      gutter,
      'var options = { maxComputationTimeMs: MAX_COMPUTATION_MS };',
      `var options = { maxComputationTimeMs: MAX_COMPUTATION_MS };
         if (experiment.strategy !== "current" && !this.splitView) {
            var tokenStarted = Date.now();
            var encoded = experimentTokenize(base, lines, experiment.strategy);
            var tokenChunks = dropCharChanges(new DiffProvider().compute(encoded.base, encoded.lines, options));
            this.lastDiffMs = Date.now() - tokenStarted;
            if (this.lastDiffMs >= MAX_COMPUTATION_MS && isWholeDocument(tokenChunks, base, lines)) {
               this.diffFailed = true;
               return [];
            }
            return offsetChunks(tokenChunks, common.prefix);
         }`
   );
   gutter = replaceOnce(
      gutter,
      'if (this.lastDiffMs >= MAX_COMPUTATION_MS && isWholeDocument(chunks, base, lines))',
      'if (!experiment.rawOnly && this.lastDiffMs >= MAX_COMPUTATION_MS && isWholeDocument(chunks, base, lines))'
   );
   vm.runInContext(gutter, context);
   const Gutter = context.require('rstudio/git_diff_gutter').GitDiffGutter;

   function compute(base, lines, strategy = 'current', options = {}) {
      assert(STRATEGIES.includes(strategy), `Unknown strategy: ${strategy}`);
      const budgetMs = options.budgetMs ?? 250;
      assert(Number.isInteger(budgetMs) && budgetMs > 0);
      Object.assign(state, {
         strategy, budgetMs, rawOnly: !!options.rawOnly,
         alignmentTimeouts: [], refinementTimeouts: 0, refinementMs: 0
      });
      let chunks;
      const model = {
         lastDiffMs: 0, diffFailed: false, splitView: options.splitView ? {} : null,
         baseLines: base, ignoreWhitespace: !!options.ignoreWhitespace,
         editor: { session: { doc: { getAllLines: () => lines.length ? lines : [''] } } },
         computeChunks: Gutter.prototype.computeChunks,
         setChunks: result => { chunks = result; }
      };
      const started = performance.now();
      Gutter.prototype.update.call(model);
      const ms = performance.now() - started;
      return {
         ms,
         refinementMs: state.refinementMs,
         alignmentTimeouts: state.alignmentTimeouts.slice(),
         refinementTimeouts: state.refinementTimeouts,
         failed: model.diffFailed,
         // Convert out of the VM so assertions use the host Array prototype.
         chunks: Array.from(chunks, chunk => [
            chunk.old.start.row, chunk.old.end.row,
            chunk.new.start.row, chunk.new.end.row
         ]),
         characterChanges: Array.from(chunks, chunk => chunk.charChanges?.length || 0)
      };
   }
   return { compute };
}

function timedOut(result) {
   return result.alignmentTimeouts.some(Boolean) || result.refinementTimeouts > 0;
}

// Check coverage and ordering before trusting a boundary comparison. Reverting
// all chunks must reconstruct the exact comparison input, including blank lines.
function validateChunks(base, lines, chunks) {
   let oldEnd = 0;
   let newEnd = 0;
   for (const [a, b, c, d] of chunks) {
      assert([a, b, c, d].every(Number.isInteger));
      assert(a >= oldEnd && b >= a && b <= base.length);
      assert(c >= newEnd && d >= c && d <= lines.length);
      assert.deepEqual(base.slice(oldEnd, a), lines.slice(newEnd, c));
      oldEnd = b;
      newEnd = d;
   }
   assert.deepEqual(base.slice(oldEnd), lines.slice(newEnd));
   // Avoid spreading arbitrarily large chunks into splice's argument list.
   let restored = lines.slice();
   for (const [a, b, c, d] of chunks.slice().reverse())
      restored = restored.slice(0, c).concat(base.slice(a, b), restored.slice(d));
   assert.deepEqual(restored, base);
}

module.exports = { ROOT, STRATEGIES, createEngine, timedOut, tokenize, validateChunks };
