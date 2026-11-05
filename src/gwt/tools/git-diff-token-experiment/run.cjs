#!/usr/bin/env node
/*
 * Copyright (C) 2026 by Posit Software, PBC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

const assert = require('node:assert/strict');
const { execFileSync, spawnSync } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { ROOT, STRATEGIES, createEngine, timedOut, validateChunks } = require('./engine.cjs');
const { syntheticCases } = require('./fixtures.cjs');
const manifest = require('./history.json');

const git = (...args) => execFileSync('git', args, { cwd: ROOT, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 });
const same = (a, b) => JSON.stringify(a) === JSON.stringify(b);
const split = text => text === '' ? [] : text.replace(/\u001b/g, '').split(/\r\n|\r|\n/);

function parseHunks(diff) {
   return Array.from(diff.matchAll(/^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@/gm), match => {
      const oldCount = Number(match[2] ?? 1);
      const newCount = Number(match[4] ?? 1);
      const oldStart = Number(match[1]) - (oldCount ? 1 : 0);
      const newStart = Number(match[3]) - (newCount ? 1 : 0);
      return [oldStart, oldStart + oldCount, newStart, newStart + newCount];
   });
}

function gitChunks(base, lines, directory) {
   const before = path.join(directory, 'before');
   const after = path.join(directory, 'after');
   fs.writeFileSync(before, base.join('\n'));
   fs.writeFileSync(after, lines.join('\n'));
   const result = spawnSync(
      'git',
      ['diff', '--no-index', '--no-ext-diff', '--no-textconv', '--no-color', '--unified=0',
         '--diff-algorithm=myers', '--indent-heuristic', '--', before, after],
      { cwd: ROOT, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 }
   );
   if (result.error || ![0, 1].includes(result.status))
      throw result.error || new Error(result.stderr);
   return parseHunks(result.stdout);
}

function historicalCases() {
   return manifest.cases.map(item => {
      assert(/^[0-9a-f]{40}$/.test(item.before) && /^[0-9a-f]{40}$/.test(item.after));
      return {
         name: `${item.after.slice(0, 10)}:${item.path}`,
         kind: 'history',
         base: split(git('show', `${item.before}:${item.path}`)),
         lines: split(git('show', `${item.after}:${item.path}`))
      };
   });
}

function statistics(samples) {
   const sorted = samples.map(result => result.ms).sort((a, b) => a - b);
   return {
      medianMs: sorted[Math.floor(sorted.length / 2)],
      minMs: sorted[0],
      maxMs: sorted.at(-1),
      failedRuns: samples.filter(result => result.failed).length,
      alignmentTimeoutRuns: samples.filter(result => result.alignmentTimeouts.some(Boolean)).length,
      refinementTimeoutRuns: samples.filter(result => result.refinementTimeouts > 0).length
   };
}

function main(args) {
   let output = path.join(ROOT, '.cache/git-diff-token-experiment');
   let sampleCount = 3;
   for (let i = 0; i < args.length; i++) {
      if (args[i] === '--output') {
         assert(args[i + 1], '--output requires a directory');
         output = path.resolve(args[++i]);
      } else if (args[i] === '--samples') {
         sampleCount = Number(args[++i]);
         assert(Number.isInteger(sampleCount) && sampleCount > 0 && sampleCount <= 20);
      } else {
         throw new Error(`Unknown argument: ${args[i]}`);
      }
   }

   const engine = createEngine();
   const cases = syntheticCases().concat(historicalCases());
   const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'rstudio-token-diff-'));
   const report = {
      baseline: manifest.selectedAt,
      checkout: git('rev-parse', 'HEAD').trim(),
      node: process.version,
      platform: `${process.platform}/${process.arch}`,
      cpu: os.cpus()[0]?.model,
      samples: sampleCount,
      budgetMs: 250,
      referenceBudgetMs: 5000,
      cases: []
   };
   try {
      for (const fixture of cases) {
         const { base, lines } = fixture;
         // A longer budget keeps truncation out of accuracy comparisons.
         // Even this reference can time out; such comparisons stay null.
         const raw = engine.compute(base, lines, 'current', { budgetMs: 5000, rawOnly: true });
         const rawComplete = !raw.failed && !timedOut(raw);
         const gitResult = gitChunks(base, lines, temp);
         const sameFinalNewline = (base.at(-1) === '') === (lines.at(-1) === '');
         const entry = {
            name: fixture.name, kind: fixture.kind,
            oldLines: base.length, newLines: lines.length,
            expected: fixture.expected,
            raw, rawComplete,
            // Ace has an extra empty row for a final newline; Git does not.
            // Keep those cases in coverage tests, but exclude direct Git equality.
            git: gitResult, gitComparable: sameFinalNewline,
            strategies: {}
         };
         if (rawComplete)
            validateChunks(base, lines, raw.chunks);
         for (const strategy of STRATEGIES) {
            const accurate = strategy === 'current' ? raw :
               engine.compute(base, lines, strategy, { budgetMs: 5000 });
            const complete = !accurate.failed && !timedOut(accurate);
            if (complete)
               validateChunks(base, lines, accurate.chunks);
            entry.strategies[strategy] = {
               accurate, complete,
               matchesRaw: complete && rawComplete ? same(accurate.chunks, raw.chunks) : null,
               matchesLineAlignments: complete && rawComplete ? same(accurate.lineAlignments, raw.lineAlignments) : null,
               matchesGit: complete && sameFinalNewline ? same(accurate.chunks, gitResult) : null,
               matchesExpected: complete && fixture.expected ? same(accurate.chunks, fixture.expected) : null,
               samples: []
            };
            if (strategy === 'line-only') {
               const review = engine.compute(base, lines, strategy, {
                  budgetMs: 5000, rawOnly: true, splitView: true
               });
               const reviewComplete = !review.failed && !timedOut(review);
               if (reviewComplete)
                  validateChunks(base, lines, review.chunks);
               Object.assign(entry.strategies[strategy], {
                  review,
                  reviewMatchesRawChunks: reviewComplete && rawComplete ? same(review.chunks, raw.chunks) : null,
                  reviewMatchesRawCharacterRanges: reviewComplete && rawComplete ? same(review.characterRanges, raw.characterRanges) : null,
                  matchesReviewBoundaries: reviewComplete && complete ? same(accurate.chunks, review.chunks) : null
               });
            }
            engine.compute(base, lines, strategy); // warm-up, excluded from timing
         }
         // Rotate order across samples to reduce systematic warm-up/load bias.
         for (let sample = 0; sample < sampleCount; sample++) {
            for (let offset = 0; offset < STRATEGIES.length; offset++) {
               const strategy = STRATEGIES[(sample + offset) % STRATEGIES.length];
               const result = engine.compute(base, lines, strategy);
               if (!result.failed)
                  validateChunks(base, lines, result.chunks);
               entry.strategies[strategy].samples.push(result);
            }
         }
         for (const strategy of STRATEGIES)
            entry.strategies[strategy].timing = statistics(entry.strategies[strategy].samples);
         report.cases.push(entry);
         console.log(`${report.cases.length}/${cases.length} ${fixture.name}`);
      }
   } finally {
      fs.rmSync(temp, { recursive: true, force: true });
   }
   const summary = {};
   for (const strategy of STRATEGIES) {
      const history = report.cases.filter(item => item.kind === 'history').map(item => item.strategies[strategy]);
      summary[strategy] = {
         historyRawComparable: history.filter(item => item.matchesRaw !== null).length,
         historyRawMismatches: history.filter(item => item.matchesRaw === false).length,
         historyLineAlignmentMismatches: history.filter(item => item.matchesLineAlignments === false).length,
         historyGitComparable: history.filter(item => item.matchesGit !== null).length,
         historyGitMismatches: history.filter(item => item.matchesGit === false).length,
         expectedMismatches: report.cases.filter(item => item.strategies[strategy].matchesExpected === false).map(item => item.name),
         failedTimingRuns: report.cases.reduce((n, item) => n + item.strategies[strategy].timing.failedRuns, 0)
      };
      if (strategy === 'line-only') {
         const all = report.cases.map(item => item.strategies[strategy]);
         Object.assign(summary[strategy], {
            reviewRawComparable: all.filter(item => item.reviewMatchesRawCharacterRanges !== null).length,
            reviewRawCharacterRangeMismatches: all.filter(item => item.reviewMatchesRawCharacterRanges === false).length,
            reviewRawChunkMismatches: all.filter(item => item.reviewMatchesRawChunks === false).length,
            reviewBoundaryChanges: report.cases.filter(item => item.strategies[strategy].matchesReviewBoundaries === false).map(item => item.name),
            characterRefinementCalls: all.reduce((n, item) => n + item.accurate.characterRefinementCalls, 0)
         });
      }
   }
   fs.mkdirSync(output, { recursive: true });
   fs.writeFileSync(path.join(output, 'report.json'), JSON.stringify(report, null, 2) + '\n');
   fs.writeFileSync(path.join(output, 'summary.json'), JSON.stringify(summary, null, 2) + '\n');
   console.log(JSON.stringify(summary, null, 2));
   console.log(`Full results: ${path.join(output, 'report.json')}`);
}

if (require.main === module)
   main(process.argv.slice(2));
module.exports = { parseHunks };
