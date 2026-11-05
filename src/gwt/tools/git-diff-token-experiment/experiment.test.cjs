/*
 * Copyright (C) 2026 by Posit Software, PBC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

const assert = require('node:assert/strict');
const { test } = require('node:test');
const { STRATEGIES, createEngine, timedOut, tokenize, validateChunks } = require('./engine.cjs');
const { syntheticCases } = require('./fixtures.cjs');
const engine = createEngine();

test('tokens preserve exact equality across the single-character token limit', () => {
   const lines = Array.from({ length: 21010 }, (_, i) => `statement_${i}()`);
   for (const strategy of STRATEGIES.filter(name => name !== 'current')) {
      const result = tokenize(lines, lines.slice().reverse(), strategy);
      assert.equal(new Set(result.base).size, lines.length);
      assert.deepEqual(result.base.slice().reverse(), result.lines);
      assert(result.base.every(line => !/\s/.test(line)));
   }
});

test('structured tokens preserve trim equality, whitespace, and the short-line threshold', () => {
   const lines = ['', '  ', '}', '  x  ', 'a b', 'long_statement()', '\tlong_statement()  '];
   const encoded = tokenize(lines, lines, 'structured').base;
   assert.equal(encoded[0], '');
   assert.equal(encoded[1], '  ');
   assert.equal(encoded[2].length, 1);
   assert.equal(encoded[3].length, 5);
   assert.equal(encoded[4].replace(/\s/g, '').length, 2);
   assert.equal(encoded[5].length, 5);
   assert.equal(encoded[5].trim(), encoded[6].trim());
   assert.notEqual(encoded[5], encoded[6]);
   assert(encoded[6].startsWith('\t') && encoded[6].endsWith('  '));
});

test('the experiment exposes the bare-token hunk merging regression', () => {
   const fixture = syntheticCases().find(item => item.name.includes('keep_this'));
   const raw = engine.compute(fixture.base, fixture.lines, 'current', { budgetMs: 5000, rawOnly: true });
   assert(!timedOut(raw));
   assert.deepEqual(raw.chunks, fixture.expected);
   const bare = engine.compute(fixture.base, fixture.lines, 'bare', { budgetMs: 5000 });
   assert.deepEqual(bare.chunks, [[0, 7, 0, 7]]);
   for (const strategy of ['padded', 'indent-padded', 'structured']) {
      const result = engine.compute(fixture.base, fixture.lines, strategy, { budgetMs: 5000 });
      assert(!timedOut(result));
      assert.deepEqual(result.chunks, fixture.expected);
   }
});

test('token strategies keep raw character highlights when a review view is attached', () => {
   const base = ['first()', 'x <- 4', 'last()'];
   const lines = ['first()', 'x <- 40', 'last()'];
   const current = engine.compute(base, lines);
   assert(current.characterChanges.some(count => count > 0));
   for (const strategy of STRATEGIES.slice(1)) {
      const ordinary = engine.compute(base, lines, strategy);
      assert(ordinary.characterChanges.every(count => count === 0));
      const review = engine.compute(base, lines, strategy, { splitView: true });
      assert.deepEqual(review.chunks, current.chunks);
      assert.deepEqual(review.characterChanges, current.characterChanges);
   }
});

test('completed fixture diffs cover all changes and revert to the comparison base', () => {
   for (const fixture of syntheticCases().filter(item => !item.name.startsWith('replace-'))) {
      for (const strategy of STRATEGIES) {
         const result = engine.compute(fixture.base, fixture.lines, strategy, { budgetMs: 5000 });
         assert(!timedOut(result) && !result.failed, `${fixture.name}: ${strategy}`);
         validateChunks(fixture.base, fixture.lines, result.chunks);
      }
   }
});

test('coverage validation rejects omitted edits and overlapping chunks', () => {
   assert.throws(() => validateChunks(['a'], ['b'], []));
   assert.throws(() => validateChunks(['a', 'b'], ['c', 'd'], [[0, 2, 0, 2], [1, 2, 1, 2]]));
});

test('the real update path honors the ignore-whitespace preference', () => {
   for (const strategy of STRATEGIES) {
      const base = ['  x <- 1', 'y <- 2  '];
      const lines = ['x<- 1  ', ' y<-2'];
      const strict = engine.compute(base, lines, strategy);
      assert(strict.chunks.length > 0);
      const ignored = engine.compute(base, lines, strategy, { ignoreWhitespace: true });
      assert.deepEqual(ignored.chunks, []);
   }
});

test('Git zero-context hunks map additions and deletions to zero-based ranges', () => {
   const { parseHunks } = require('./run.cjs');
   assert.deepEqual(parseHunks('@@ -0,0 +1,2 @@\n@@ -4 +5,0 @@\n@@ -8,2 +8,3 @@'),
      [[0, 0, 0, 2], [3, 4, 5, 5], [7, 9, 7, 10]]);
});
