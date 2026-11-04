// Build the cross-state accessibility report for an a11y run's merge job, from
// the files the state tests left under test-results/ (the merge job downloads
// the shards' test-results artifacts there; a local run writes them there too).
//
//   screenshots  write a11y-screenshots.html, every state's screenshot inline
//   summary      write one entry per distinct problem to the job summary (or
//                stdout outside Actions), linking state names to
//                A11Y_SCREENSHOTS_URL when it is set
//
// Two commands because the summary links to the screenshots page, whose URL
// only exists once a workflow step has uploaded it. Paths are fixed rather
// than taken as arguments, as in summarize-merged-report.mjs.
//
// Usage: npx tsx scripts/a11y-report.ts screenshots|summary

import * as fs from 'fs';
import { readStateScans, screenshotsHtml, summaryMarkdown } from '../utils/a11y-report';

const RESULTS_DIR = 'test-results';
const SCREENSHOTS_FILE = 'a11y-screenshots.html';

function main(command: string | undefined): void {
  if (command !== 'screenshots' && command !== 'summary')
    throw new Error(`Unknown command "${command ?? ''}"; usage: npx tsx scripts/a11y-report.ts screenshots|summary`);

  const scans = readStateScans(RESULTS_DIR);
  if (scans.length === 0) {
    throw new Error(`No accessibility states under ${RESULTS_DIR}/: no test used the a11yPage fixture, ` +
      'or the shards\' test-results artifacts were not downloaded there');
  }

  if (command === 'screenshots') {
    fs.writeFileSync(SCREENSHOTS_FILE, screenshotsHtml(scans));
    console.log(`Wrote ${SCREENSHOTS_FILE} (${scans.length} state(s))`);
    return;
  }

  const markdown = summaryMarkdown(scans, process.env.A11Y_SCREENSHOTS_URL || undefined);
  const summaryFile = process.env.GITHUB_STEP_SUMMARY;
  if (summaryFile)
    fs.appendFileSync(summaryFile, markdown);
  else
    process.stdout.write(markdown);
}

try {
  main(process.argv[2]);
} catch (err) {
  console.error(`::error::accessibility report: ${(err as Error).message}`);
  process.exit(1);
}
