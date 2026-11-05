/*
 * Copyright (C) 2026 by Posit Software, PBC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

const assert = require('node:assert/strict');
const { test } = require('node:test');
const { STRATEGIES, TOKEN_STRATEGIES, createEngine, timedOut, tokenize, validateChunks } = require('./engine.cjs');
const { syntheticCases } = require('./fixtures.cjs');
const engine = createEngine();

test('tokens preserve exact equality across the single-character token limit', () => {
   const lines = Array.from({ length: 21010 }, (_, i) => `statement_${i}()`);
   for (const strategy of TOKEN_STRATEGIES) {
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

test('experimental strategies keep exact raw character ranges when review is attached', () => {
   const base = ['first()', 'x <- 4', 'last()'];
   const lines = ['first()', 'x <- 40', 'last()'];
   const current = engine.compute(base, lines);
   assert(current.characterChanges.some(count => count > 0));
   for (const strategy of STRATEGIES.slice(1)) {
      const ordinary = engine.compute(base, lines, strategy);
      if (strategy !== 'bounded')
         assert(ordinary.characterChanges.every(count => count === 0));
      const review = engine.compute(base, lines, strategy, { splitView: true });
      assert.deepEqual(review.chunks, current.chunks);
      assert.deepEqual(review.characterChanges, current.characterChanges);
      assert.deepEqual(review.characterRanges, current.characterRanges);
   }
});

test('bounded refinement restores separate hunks across short unchanged separators', () => {
   for (const separator of ['}', '', '    ']) {
      const fixture = syntheticCases().find(item => item.name === `separated-blocks-${JSON.stringify(separator)}`);
      const raw = engine.compute(fixture.base, fixture.lines, 'current', { budgetMs: 5000, rawOnly: true });
      const gutter = engine.compute(fixture.base, fixture.lines, 'bounded', { budgetMs: 5000 });
      const review = engine.compute(fixture.base, fixture.lines, 'bounded', { budgetMs: 5000, splitView: true });
      assert(!raw.failed && !gutter.failed && !review.failed);
      assert.deepEqual(gutter.chunks, [[0, 3, 0, 3], [4, 7, 4, 7]]);
      assert.deepEqual(gutter.chunks, raw.chunks);
      assert.deepEqual(review.chunks, gutter.chunks);
      assert.deepEqual(review.characterRanges, raw.characterRanges);
   }
});

test('bounded work exhaustion retains valid hunks and review cannot repartition them', () => {
   const fixture = syntheticCases().find(item => item.name === 'separated-blocks-"}"');
   const options = { budgetMs: 5000, boundedLimits: { groupReads: 10, totalReads: 15 } };
   const gutter = engine.compute(fixture.base, fixture.lines, 'bounded', options);
   const review = engine.compute(fixture.base, fixture.lines, 'bounded', { ...options, splitView: true });
   assert(!gutter.failed && !review.failed && !timedOut(gutter) && !timedOut(review));
   assert(gutter.boundedGroups.some(group => group.reason === 'work'));
   assert(gutter.boundedGroups.reduce((sum, group) => sum + group.reads, 0) <= 15);
   assert(gutter.boundedGroups.every(group => group.reads <= 10));
   assert.deepEqual(gutter.chunks, [[0, 7, 0, 7]]);
   assert.deepEqual(review.chunks, gutter.chunks);
   assert(review.characterChanges.some(count => count > 0));
   validateChunks(fixture.base, fixture.lines, gutter.chunks);
   const toggled = engine.compute(fixture.base, fixture.lines, 'bounded', {
      ...options, reviewTransitions: [true, false, true]
   });
   for (const transition of toggled.transitions) {
      assert.equal(transition.pending, false);
      assert.deepEqual(transition.chunks, gutter.chunks);
      if (transition.visible)
         assert.deepEqual(transition.delivered.characterRanges, review.characterRanges);
   }
});

test('ambiguous groups above the small-group limit get bounded refinement', () => {
   const base = [...Array.from({ length: 9 }, (_, i) => `old_a${i} <- ${i}`), '}',
      ...Array.from({ length: 9 }, (_, i) => `old_b${i} <- ${i}`)];
   const lines = base.map(line => line.replace('old_', 'new_'));
   const raw = engine.compute(base, lines, 'current', { budgetMs: 5000, rawOnly: true });
   const bounded = engine.compute(base, lines, 'bounded', { budgetMs: 5000 });
   assert(!raw.failed && !bounded.failed && !timedOut(raw) && !timedOut(bounded));
   assert(bounded.boundedGroups.some(group => group.reason === 'ambiguous' && group.refined));
   assert.deepEqual(bounded.chunks, raw.chunks);
});

test('refinement budgets are shared across groups and isolated from review decoration', () => {
   const base = ['a <- 1', 'first_unchanged_statement()', 'b <- 2', 'second_unchanged_statement()', 'c <- 3'];
   const lines = base.map(line => line.replace('<-', '='));
   const options = { boundedLimits: { groupReads: 10, totalReads: 15, totalCharacters: 40 } };
   const gutter = engine.compute(base, lines, 'bounded', options);
   const review = engine.compute(base, lines, 'bounded', { ...options, splitView: true });
   assert(gutter.boundedGroups.length > 1);
   assert(gutter.boundedGroups.reduce((n, group) => n + group.reads, 0) <= 15);
   assert(gutter.boundedGroups.reduce((n, group) => n + group.characters, 0) <= 40);
   const canonical = result => result.boundedGroups.map(group => [group.old, group.new, group.reason, group.reads, group.characters]);
   assert.deepEqual(canonical(review), canonical(gutter));
   assert.deepEqual(review.chunks, gutter.chunks);
   const noAllocation = engine.compute(base, lines, 'bounded', { boundedLimits: { totalCharacters: 0 } });
   assert.equal(noAllocation.characterRefinementCalls, 0);
   validateChunks(base, lines, noAllocation.chunks);
});

test('review can decorate an oversized group without changing its bounded hunk', () => {
   const base = ['x'.repeat(3000) + ' a'];
   const lines = ['x'.repeat(3000) + ' b'];
   const raw = engine.compute(base, lines, 'current', { budgetMs: 5000, rawOnly: true });
   const gutter = engine.compute(base, lines, 'bounded', { budgetMs: 5000 });
   const review = engine.compute(base, lines, 'bounded', { budgetMs: 5000, splitView: true });
   assert(gutter.boundedGroups.some(group => group.reason === 'size'));
   assert(gutter.characterChanges.every(count => count === 0));
   assert(review.boundedGroups.some(group => group.highlighted));
   assert.deepEqual(review.chunks, gutter.chunks);
   assert.deepEqual(review.characterRanges, raw.characterRanges);
});

test('the provider bypass preserves line alignment heuristics and skips character refinement', () => {
   for (const fixture of syntheticCases().filter(item => !item.name.startsWith('replace-'))) {
      const raw = engine.compute(fixture.base, fixture.lines, 'current', { budgetMs: 5000, rawOnly: true });
      const candidate = engine.compute(fixture.base, fixture.lines, 'line-only', { budgetMs: 5000 });
      assert(!timedOut(raw) && !timedOut(candidate));
      assert.deepEqual(candidate.lineAlignments, raw.lineAlignments, fixture.name);
      assert.equal(candidate.characterRefinementCalls, 0, fixture.name);
      assert(candidate.characterRanges.every(ranges => ranges.length === 0));
      validateChunks(fixture.base, fixture.lines, candidate.chunks);
   }
   const review = engine.compute(['x <- 1'], ['x <- 2'], 'line-only', { splitView: true });
   assert(review.characterRefinementCalls > 0);
});

test('skipping refinement retains short-line groups that the character pass splits apart', () => {
   for (const separator of ['}', '', '    ']) {
      const fixture = syntheticCases().find(item => item.name === `separated-blocks-${JSON.stringify(separator)}`);
      const candidate = engine.compute(fixture.base, fixture.lines, 'line-only', { budgetMs: 5000 });
      const review = engine.compute(fixture.base, fixture.lines, 'line-only', { budgetMs: 5000, splitView: true });
      assert(!candidate.failed && !review.failed && !timedOut(candidate) && !timedOut(review));
      assert.deepEqual(candidate.lineAlignments, review.lineAlignments);
      assert.deepEqual(candidate.chunks, [[0, 7, 0, 7]]);
      assert.deepEqual(review.chunks, [[0, 3, 0, 3], [4, 7, 4, 7]]);
   }
});

test('opening review refreshes cached line-only chunks without a pending edit', () => {
   const fixture = syntheticCases().find(item => item.name === 'separated-blocks-"}"');
   const raw = engine.compute(fixture.base, fixture.lines, 'current', { budgetMs: 5000, rawOnly: true });
   const lineOnly = engine.compute(fixture.base, fixture.lines, 'line-only', { budgetMs: 5000 });
   const toggled = engine.compute(fixture.base, fixture.lines, 'line-only', {
      budgetMs: 5000, reviewTransitions: [true, false, true]
   });
   assert(!raw.failed && !lineOnly.failed && !toggled.failed);
   assert(!timedOut(raw) && !timedOut(lineOnly) && !timedOut(toggled));
   assert(raw.characterChanges.some(count => count > 0));
   for (const transition of toggled.transitions) {
      assert.equal(transition.pending, false);
      const expected = transition.visible ? raw : lineOnly;
      assert.deepEqual(transition.chunks, expected.chunks);
      assert.deepEqual(transition.characterRanges, expected.characterRanges);
      if (transition.visible) {
         assert.deepEqual(transition.delivered.chunks, raw.chunks);
         assert.deepEqual(transition.delivered.characterRanges, raw.characterRanges);
      }
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
