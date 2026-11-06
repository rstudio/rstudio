/*
 * Copyright (C) 2026 by Posit Software, PBC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

const assert = require('node:assert/strict');
const { execFileSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const { performance } = require('node:perf_hooks');
const vm = require('node:vm');
const { BOUNDED_LIMITS, installBoundedRefinement } = require('./bounded.cjs');

const ROOT = path.resolve(__dirname, '../../../..');
const TOKEN_STRATEGIES = ['bare', 'indent', 'padded', 'indent-padded', 'structured'];
const STRATEGIES = ['current', ...TOKEN_STRATEGIES, 'line-only', 'bounded'];
const BASELINE = require('./history.json').selectedAt;
const baselineSources = new Map();
function readBaseline(file) {
   const relative = path.relative(ROOT, file);
   if (!baselineSources.has(relative))
      baselineSources.set(relative, execFileSync('git', ['show', `${BASELINE}:${relative}`], {
         cwd: ROOT, encoding: 'utf8', maxBuffer: 16 * 1024 * 1024
      }));
   return baselineSources.get(relative);
}

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

function describeChunks(chunks) {
   // Convert out of the VM so assertions use the host Array prototype.
   return {
      chunks: Array.from(chunks, chunk => [
         chunk.old.start.row, chunk.old.end.row, chunk.new.start.row, chunk.new.end.row
      ]),
      characterChanges: Array.from(chunks, chunk => chunk.charChanges?.length || 0),
      characterRanges: Array.from(chunks, chunk => Array.from(chunk.charChanges || [], change => [
         change.old.start.row, change.old.start.column, change.old.end.row, change.old.end.column,
         change.new.start.row, change.new.start.column, change.new.end.row, change.new.end.column
      ]))
   };
}

function createEngine() {
   const state = {};
   const context = vm.createContext({ console, setTimeout, clearTimeout, performance, experiment: state });
   context.window = context;
   const aceDir = path.join(ROOT, 'src/gwt/src/org/rstudio/studio/client/workbench/views/source/editors/text/ace');
   vm.runInContext(readBaseline(path.join(aceDir, 'ace-uncompressed.js')), context);
   context.require('ace/lib/dom').useStrictCSP(true);

   // Instrument only the in-memory provider. Preserve its algorithms and
   // surface the timeout phase that the public provider currently discards.
   let provider = readBaseline(path.join(aceDir, 'ext-diff-uncompressed.js'));
   provider = replaceOnce(
      provider,
      'var hitTimeout = lineAlignmentResult.hitTimeout;',
      'var hitTimeout = lineAlignmentResult.hitTimeout; experiment.alignmentTimeouts.push(hitTimeout);'
   );
   provider = replaceOnce(
      provider,
      'lineAlignments = removeVeryShortMatchingLinesBetweenDiffs(sequence1, sequence2, lineAlignments);',
      `lineAlignments = removeVeryShortMatchingLinesBetweenDiffs(sequence1, sequence2, lineAlignments);
        experiment.lineAlignments.push(lineAlignments.map(function(diff) {
            return [diff.seq1Range.start, diff.seq1Range.endExclusive,
                    diff.seq2Range.start, diff.seq2Range.endExclusive];
        }));`
   );
   // Keep the original line alignment, placement, joining, and whitespace
   // scanning. Only replace character refinement with its full line mapping.
   provider = replaceOnce(
      provider,
      'var rangeMapping = lineRangeMapping.toRangeMapping2(originalLines, modifiedLines);',
      `var rangeMapping = lineRangeMapping.toRangeMapping2(originalLines, modifiedLines);
        if (options.skipCharacterRefinement) {
            return { mappings: [rangeMapping], hitTimeout: false };
        }`
   );
   provider = replaceOnce(
      provider,
      'var innerChanges = changes.innerChanges;',
      `var innerChanges = options.skipCharacterRefinement ? undefined : changes.innerChanges;
        var boundedCoarse = options.boundedRefinement && innerChanges && innerChanges.some(function(mapping) {
            return mapping.boundedHighlights !== undefined;
        });
        if (options.boundedRefinement && innerChanges) {
            innerChanges = innerChanges.flatMap(function(mapping) {
                return mapping.boundedHighlights === undefined ? [mapping] : mapping.boundedHighlights;
            });
            // An empty array suppresses BaseDiffView's line-position fallback.
            if (!innerChanges.length)
                innerChanges = undefined;
        }`
   );
   // Partial decorations do not form a complete character-position mapping.
   // Keep line-based selection mapping for any hunk containing a coarse group,
   // including when Ace merges it with a neighboring refined whitespace edit.
   provider = replaceOnce(
      provider,
      'origStart: originalStartLineNumber,',
      'origStart: originalStartLineNumber, boundedCoarse: !!boundedCoarse,'
   );
   provider = replaceOnce(
      provider,
      'else if (chunk.charChanges) {',
      'else if (chunk.charChanges && !chunk.boundedCoarse) {'
   );
   // Preserve the existing timeout status at the provider boundary. Bounded
   // character work degrades to coarse mappings, so a failure here is a line
   // alignment failure, independently of time spent on review decorations.
   provider = replaceOnce(
      provider,
      'return result === null || result === void 0 ? void 0 : result.changes.map(function (changes) {',
      'var chunks = result === null || result === void 0 ? void 0 : result.changes.map(function (changes) {'
   );
   provider = replaceOnce(
      provider,
      '    });\n}\nexports.computeDiff = computeDiff;',
      '    });\n    chunks = chunks || [];\n    chunks.hitTimeout = !!(result && result.hitTimeout);\n    return chunks;\n}\nexports.computeDiff = computeDiff;'
   );
   provider = replaceOnce(
      provider,
      'return chunks.map(function (c) { return new DiffChunk(new AceRange(c.origStart, 0, c.origEnd, 0), new AceRange(c.editStart, 0, c.editEnd, 0), c.charChanges); });',
      `var mapped = chunks.map(function (c) {
            var chunk = new DiffChunk(new AceRange(c.origStart, 0, c.origEnd, 0), new AceRange(c.editStart, 0, c.editEnd, 0), c.charChanges);
            chunk.boundedCoarse = c.boundedCoarse;
            return chunk;
        });
        mapped.hitTimeout = chunks.hitTimeout;
        return mapped;`
   );
   provider = replaceOnce(
      provider,
      'exports.computeDiff = computeDiff;',
      'exports.computeDiff = computeDiff; exports.experimentComputer = DefaultLinesDiffComputer; exports.experimentCharSequence = LinesSliceCharSequence;'
   );
   vm.runInContext(provider, context);
   const Computer = context.require('ace/ext/diff/providers/default').experimentComputer;
   const refine = Computer.prototype.refineDiff;
   Computer.prototype.refineDiff = function(...args) {
      const started = performance.now();
      if (!args[5].skipCharacterRefinement)
         state.characterRefinementCalls++;
      try {
         const result = refine.apply(this, args);
         state.refinementTimeouts += result.hitTimeout ? 1 : 0;
         return result;
      } finally {
         state.refinementMs += performance.now() - started;
      }
   };
   vm.runInContext(`(${installBoundedRefinement.toString()})(require("ace/ext/diff/providers/default"), experiment);`, context);

   vm.runInContext(`var experimentTokenize = ${tokenize.toString()};`, context);
   let gutter = readBaseline(path.join(ROOT, 'src/gwt/acesupport/git_diff_gutter.js'));
   gutter = replaceOnce(
      gutter,
      'if (chunk.charChanges)',
      'result.boundedCoarse = chunk.boundedCoarse;\n      if (chunk.charChanges)'
   );
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
         if (experiment.strategy === "bounded") {
            options.boundedRefinement = experiment.boundedLimits;
            options.includeBoundedHighlights = !!this.splitView;
            var boundedStarted = Date.now();
            var boundedChunks = new DiffProvider().compute(base, lines, options);
            this.lastDiffMs = Date.now() - boundedStarted;
            if (boundedChunks.hitTimeout) {
               this.diffFailed = true;
               return [];
            }
            return offsetChunks(boundedChunks, common.prefix);
         }
         if (experiment.strategy === "line-only" && !this.splitView) {
            options.skipCharacterRefinement = true;
            var lineStarted = Date.now();
            var lineChunks = new DiffProvider().compute(base, lines, options);
            this.lastDiffMs = Date.now() - lineStarted;
            if (this.lastDiffMs >= MAX_COMPUTATION_MS && isWholeDocument(lineChunks, base, lines)) {
               this.diffFailed = true;
               return [];
            }
            return offsetChunks(lineChunks, common.prefix);
         }
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
   // Attaching review normally flushes only a pending edit. A cached line-only
   // result also needs recomputing, even when the document has not changed.
   gutter = replaceOnce(
      gutter,
      'this.splitView = view;',
      `var refinementModeChanged = (experiment.strategy === "line-only" || experiment.strategy === "bounded") && !!this.splitView !== !!view;
      this.splitView = view;
      if (refinementModeChanged) {
         this.updatePending = true;
         if (!view)
            this.flushUpdate();
      }`
   );
   vm.runInContext(gutter, context);
   return createRunner(context, state);
}

// Load the shipped source without rewriting either algorithm or gutter code.
// Only test-controlled provider options and timeout observation are wrapped.
function createProductionEngine({ minified = false } = {}) {
   const state = {};
   const context = vm.createContext({ console, setTimeout, clearTimeout, performance });
   context.window = context;
   const aceDir = path.join(ROOT, 'src/gwt/src/org/rstudio/studio/client/workbench/views/source/editors/text/ace');
   const suffix = minified ? '.js' : '-uncompressed.js';
   vm.runInContext(fs.readFileSync(path.join(aceDir, 'ace' + suffix), 'utf8'), context);
   context.require('ace/lib/dom').useStrictCSP(true);
   vm.runInContext(fs.readFileSync(path.join(aceDir, 'ext-diff' + suffix), 'utf8'), context);
   const Provider = context.require('ace/ext/diff/providers/default').DiffProvider;
   const compute = Provider.prototype.compute;
   Provider.prototype.compute = function(base, lines, options) {
      const chunks = compute.call(this, base, lines, {
         ...options, maxComputationTimeMs: state.budgetMs,
         ...(state.customBoundedLimits ? { boundedRefinement: state.boundedLimits } : {})
      });
      state.alignmentTimeouts.push(chunks.hitTimeout);
      return chunks;
   };
   vm.runInContext(fs.readFileSync(path.join(ROOT, 'src/gwt/acesupport/git_diff_gutter.js'), 'utf8'), context);
   return createRunner(context, state, true);
}

function createRunner(context, state, production = false) {
   const Gutter = context.require('rstudio/git_diff_gutter').GitDiffGutter;

   function compute(base, lines, strategy = 'current', options = {}) {
      assert(STRATEGIES.includes(strategy), `Unknown strategy: ${strategy}`);
      const budgetMs = options.budgetMs ?? 250;
      assert(Number.isInteger(budgetMs) && budgetMs > 0);
      const boundedLimits = { ...BOUNDED_LIMITS, ...options.boundedLimits };
      for (const value of Object.values(boundedLimits))
         assert(Number.isSafeInteger(value) && value >= 0);
      Object.assign(state, {
         strategy, budgetMs, rawOnly: !!options.rawOnly,
         alignmentTimeouts: [], lineAlignments: [], refinementTimeouts: 0,
         refinementMs: 0, characterRefinementCalls: 0,
         boundedGroups: [], boundedLimits, customBoundedLimits: options.boundedLimits
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
      const transitions = [];
      if (options.reviewTransitions) {
         // Use the real setSplitView / flushUpdate methods with only DOM and
         // event plumbing stubbed. The document remains unchanged throughout.
         const noop = () => {};
         function view() {
            return {
               baseEditor: { on: noop, off: noop, commands: { addCommand: noop, removeCommand: noop } },
               setBaseLines: noop,
               setChunks: result => { model.deliveredChunks = result; }
            };
         }
         Object.assign(model, {
            chunks, updatePending: false,
            splitView: options.splitView ? view() : null,
            update: Gutter.prototype.update,
            flushUpdate: Gutter.prototype.flushUpdate,
            closePeek: noop, notifyStatus: noop,
            closePeekCommand: () => ({ name: 'closeGitPeek' }),
            setChunks(result) {
               chunks = this.chunks = result;
               if (this.splitView)
                  this.splitView.setChunks(result);
            }
         });
         for (const visible of options.reviewTransitions) {
            assert.equal(typeof visible, 'boolean');
            model.deliveredChunks = undefined;
            Gutter.prototype.setSplitView.call(model, visible ? view() : null);
            transitions.push({
               visible, pending: model.updatePending,
               ...describeChunks(chunks),
               delivered: model.deliveredChunks ? describeChunks(model.deliveredChunks) : null
            });
         }
      }
      let transformedPositions;
      if (options.transformPositions) {
         const BaseDiffView = context.require('ace/ext/diff/base_diff_view').BaseDiffView;
         const EditSession = context.require('ace/edit_session').EditSession;
         const view = Object.assign(Object.create(BaseDiffView.prototype), {
            chunks,
            sessionA: new EditSession(base.join('\n')),
            sessionB: new EditSession(lines.join('\n'))
         });
         transformedPositions = options.transformPositions.map(({ row, column, isOriginal }) =>
            ({ ...view.transformPosition({ row, column }, isOriginal) }));
         view.sessionA.destroy();
         view.sessionB.destroy();
      }
      return {
         ms: performance.now() - started,
         transitions,
         transformedPositions,
         refinementMs: production ? null : state.refinementMs,
         characterRefinementCalls: production ? null : state.characterRefinementCalls,
         boundedGroups: JSON.parse(JSON.stringify(state.boundedGroups)),
         alignmentTimeouts: state.alignmentTimeouts.slice(),
         lineAlignments: JSON.parse(JSON.stringify(state.lineAlignments)),
         refinementTimeouts: state.refinementTimeouts,
         failed: model.diffFailed,
         ...describeChunks(chunks)
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

module.exports = { ROOT, STRATEGIES, TOKEN_STRATEGIES, createEngine, createProductionEngine, timedOut, tokenize, validateChunks };
