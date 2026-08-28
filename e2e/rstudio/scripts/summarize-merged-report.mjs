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
// It also reports, for each engine in the merged report, the build that engine
// ran against -- once as a run annotation and once in the job summary. Both live
// here, in the merge job, for two reasons: a job summary is per job, so a sharded
// engine writing its own would repeat itself once per shard (six times on
// Windows), and only the merge job sees every engine at once.
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


// "Run under test": what each engine in this report actually ran against. The
// harness records one metadata entry per engine, keyed by OS and edition, with
// the versions as the value (e2e/rstudio/utils/versions.ts). Playwright adds
// metadata of its own -- ci, gitCommit, gitDiff, actualWorkers -- so entries are
// picked by value shape rather than by excluding Playwright's key names, which
// would let a newly added Playwright key leak into the output.
//
// Reported twice, because the two placements answer different questions. The
// annotation sits at the top of the run's Summary page, above the job list and
// unfolded, so a reader sees it without clicking into anything -- annotations are
// single lines and GitHub caps how many it shows, hence one line per engine
// (four on a PR run). The summary entry is attached to this job, so anyone who
// does open the merge job finds the same facts in place. Annotations must go to
// stdout -- that is where GitHub parses workflow commands from.
const engines = Object.entries(data.config?.metadata ?? {})
  .filter(([, value]) => typeof value === 'string' && value.startsWith('RStudio '))
  .sort(([a], [b]) => a.localeCompare(b));

for (const [name, value] of engines) {
  const line = `${name} -- ${value}`;
  console.log(process.env.GITHUB_ACTIONS ? `::notice title=Run under test::${line}` : `Run under test: ${line}`);
}

// One-line renderings for workflow steps to use as their `name:`, so the build
// under test is readable in a job's step list without expanding anything. Emitted
// as run_under_test_1..N, one per engine in the merged report, because a step name
// is a single line: a caller declares one step per output it expects and skips the
// ones that come back empty (a per-engine merge job uses only _1; the PR run's
// combined job covers its four engines).
//
// The pieces are recombined rather than reused verbatim: the metadata key pairs
// the OS with the edition ("Ubuntu 24 (x86_64) Desktop", built by runVersionsKey
// in e2e/rstudio/utils/versions.ts) and the value leads with "RStudio", so the
// edition is split off the end of the key and folded into the product name. Both
// halves are generated by our own code, which is what makes the split safe.
const MAX_STEP_NAMES = 4;
for (let i = 0; i < MAX_STEP_NAMES; i++) {
  let stepName = '';
  if (i < engines.length) {
    const [key, value] = engines[i];
    const cut = key.lastIndexOf(' ');
    const os = key.slice(0, cut);
    const edition = key.slice(cut + 1);
    stepName = `${value.replace(/^RStudio /, `RStudio ${edition} `)} \u00b7 ${os}`;
  }
  setOutput(`run_under_test_${i + 1}`, stepName);
}
if (engines.length > MAX_STEP_NAMES)
  console.warn(`WARNING: ${engines.length} engines in this report but only ${MAX_STEP_NAMES} step names are emitted; the rest are reported only as annotations.`);

const summaryFile = process.env.GITHUB_STEP_SUMMARY;
if (summaryFile && engines.length > 0) {
  const block = [
    '### Run under test',
    '',
    '| Engine | Versions |',
    '| :--- | :--- |',
    ...engines.map(([name, value]) => `| **${name}** | ${value} |`),
    '',
    '',
  ].join('\n');
  try {
    fs.appendFileSync(summaryFile, block);
  } catch (err) {
    console.warn(`WARNING: could not write the run-under-test summary: ${err.message}`);
  }
}

// Echo to the step log for debugging when the comment looks wrong.
console.log(`Overall: passed=${overall.passed} failed=${overall.failed} skipped=${overall.skipped} flaky=${overall.flaky} rate=${rate}%`);
console.log(table);
