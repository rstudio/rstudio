// Summarize a merged Playwright JSON report for the CI PR comments.
//
// Used by both the per-platform e2e-merge jobs (scheduled/standalone runs) and
// the PR run's unified e2e-merge-all job (see os-test-e2e-rstudio-pr.yml), so
// every comment computes its counts from one definition. This script reads the
// merged report's companion test-results.json and emits, to $GITHUB_OUTPUT:
//
//   passed / failed / skipped / flaky  -- overall test counts
//   rate / bar                         -- overall pass-rate percent and a bar
//   platform_table                     -- a markdown table with one row per
//                                         platform (Playwright "project"), so a
//                                         single comment shows which OS failed
//
// Platforms are distinguished by the per-platform project label set via
// PW_PROJECT_LABEL in each platform workflow (e.g. desktop-macos, server-linux).
//
// Usage: node summarize-merged-report.mjs <path-to-test-results.json>

import fs from 'node:fs';

const file = process.argv[2];
const outFile = process.env.GITHUB_OUTPUT;

let data;
try {
  data = JSON.parse(fs.readFileSync(file, 'utf8'));
} catch {
  // No parseable report (e.g. every platform failed to produce a blob). Fall
  // back to an empty result so the caller still renders a comment noting it.
  data = { suites: [] };
}

// Collect every test result across all (possibly nested) suites and specs.
const tests = [];
function walk(suite) {
  for (const spec of suite.specs ?? []) {
    for (const test of spec.tests ?? [])
      tests.push(test);
  }
  for (const child of suite.suites ?? [])
    walk(child);
}
for (const suite of data.suites ?? [])
  walk(suite);

// Map Playwright's per-test outcome to the four buckets the comment reports.
function tally(list) {
  const counts = { passed: 0, failed: 0, flaky: 0, skipped: 0 };
  for (const test of list) {
    if (test.status === 'expected')
      counts.passed++;
    else if (test.status === 'unexpected')
      counts.failed++;
    else if (test.status === 'flaky')
      counts.flaky++;
    else if (test.status === 'skipped')
      counts.skipped++;
  }
  return counts;
}

const overall = tally(tests);

// Per-project (= per-platform) breakdown, one row per distinct project label.
const byProject = new Map();
for (const test of tests) {
  const key = test.projectName || 'unknown';
  if (!byProject.has(key))
    byProject.set(key, []);
  byProject.get(key).push(test);
}

const rows = [...byProject.keys()].sort().map((name) => {
  const c = tally(byProject.get(name));
  const status = c.failed > 0 ? ':x:' : ':white_check_mark:';
  return `| ${status} \`${name}\` | ${c.passed} | ${c.failed} | ${c.skipped} | ${c.flaky} |`;
});

const table = [
  '| Platform | :white_check_mark: Passed | :x: Failed | :warning: Skipped | :repeat: Flaky |',
  '| :--- | :---: | :---: | :---: | :---: |',
  ...(rows.length > 0 ? rows : ['| _no results_ | 0 | 0 | 0 | 0 |']),
].join('\n');

// Pass rate over decided tests only (passed + failed), rendered as a 20-cell bar.
const decided = overall.passed + overall.failed;
const rate = decided > 0 ? Math.floor((overall.passed * 100) / decided) : 0;
const filled = Math.floor((rate * 20) / 100);
const bar = '#'.repeat(filled) + '-'.repeat(20 - filled);

function setOutput(name, value) {
  const delimiter = `__EOF_${name}__`;
  const line = `${name}<<${delimiter}\n${value}\n${delimiter}\n`;
  if (outFile)
    fs.appendFileSync(outFile, line);
}

setOutput('passed', String(overall.passed));
setOutput('failed', String(overall.failed));
setOutput('skipped', String(overall.skipped));
setOutput('flaky', String(overall.flaky));
setOutput('rate', String(rate));
setOutput('bar', bar);
setOutput('platform_table', table);

// Echo to the step log for debugging when the comment looks wrong.
console.log(`Overall: passed=${overall.passed} failed=${overall.failed} skipped=${overall.skipped} flaky=${overall.flaky} rate=${rate}%`);
console.log(table);
