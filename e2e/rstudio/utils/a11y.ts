import AxeBuilder from '@axe-core/playwright';
import type { AxeResults, NodeResult, Result } from 'axe-core';
import type { Page, TestInfo } from '@playwright/test';
import * as fs from 'fs';
import { envFlag } from './env';

/**
 * All of axe's WCAG Level A and AA tags (there is no wcag22a). Selecting by
 * tag also runs target-size, which axe's default rule set leaves off -- that
 * is how WCAG 2.2 stays in scope.
 */
export const WCAG_22_AA_TAGS: readonly string[] = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

const SNIPPET_MAX = 300;
const SUMMARY_NODES_MAX = 10;

const isWcagAA = (r: Result) => r.tags.some((t) => WCAG_22_AA_TAGS.includes(t));

function ruleSetLabel(experimental: boolean): string {
  return experimental ? 'WCAG 2.2 AA + experimental' : 'WCAG 2.2 AA';
}

/**
 * Scan the page (frames included) with the WCAG A/AA rule set. With
 * `experimental`, axe's experimental rules are added; tag selection is OR-only,
 * so results are then filtered back to WCAG A/AA tags to drop the non-WCAG
 * experimental rules it brings in.
 */
export async function runAxe(page: Page, { experimental }: { experimental: boolean }): Promise<AxeResults> {
  const tags = experimental ? [...WCAG_22_AA_TAGS, 'experimental'] : [...WCAG_22_AA_TAGS];
  const results = await new AxeBuilder({ page }).withTags(tags).analyze();
  if (!experimental)
    return results;
  return {
    ...results,
    violations: results.violations.filter(isWcagAA),
    incomplete: results.incomplete.filter(isWcagAA),
  };
}

function violationLine(v: Result): string {
  return `${v.id} [${v.impact ?? 'unknown'}] ${v.nodes.length} node(s): ${v.help}`;
}

/** One line per violation, for assertion messages. */
export function violationSummary(results: AxeResults): string[] {
  return results.violations.map(violationLine);
}

function trimSnippet(html: string): string {
  const flat = html.replace(/\s+/g, ' ').trim();
  return flat.length > SNIPPET_MAX ? `${flat.slice(0, SNIPPET_MAX)}...` : flat;
}

function wcagTags(v: Result): string {
  return v.tags.filter((t) => t.startsWith('wcag')).join(', ');
}

function formatNode(n: NodeResult): string {
  return [
    `  - ${trimSnippet(n.html)}`,
    ...(n.failureSummary ?? '').split('\n').filter((l) => l.trim()).map((l) => `      ${l.trim()}`),
    `      target: ${JSON.stringify(n.target)}`,
  ].join('\n');
}

/**
 * Human-readable violations. Each node leads with its HTML snippet because
 * CI builds obfuscate GWT CSS class names, which makes the target selector
 * the least readable part.
 */
export function formatViolations(results: AxeResults): string {
  if (results.violations.length === 0)
    return 'No violations\n';
  const blocks = results.violations.map((v) => [
    `${v.id} [${v.impact ?? 'unknown'}] ${v.help}`,
    `  ${v.helpUrl}`,
    `  tags: ${wcagTags(v)}`,
    ...v.nodes.map(formatNode),
  ].join('\n'));
  return `${blocks.join('\n\n')}\n`;
}

function escapeHtml(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

/**
 * One collapsible section per rule. Node lines are indented, so none can
 * close the code fence early. The node cap keeps many states under GitHub's
 * 1 MiB per-step summary limit, past which the whole summary is dropped.
 */
function summaryDetails(v: Result): string {
  const shown = v.nodes.slice(0, SUMMARY_NODES_MAX);
  const hidden = v.nodes.length - shown.length;
  return [
    '<details>',
    // Raw HTML: help text can contain markup, e.g. "<html> element must have a lang attribute".
    `<summary>${escapeHtml(violationLine(v))}</summary>`,
    '',
    `[Rule help](${v.helpUrl}) - ${wcagTags(v)}`,
    '',
    '```',
    ...shown.map(formatNode),
    '```',
    ...(hidden > 0 ? ['', `${hidden} more node(s) in axe-violations.txt`] : []),
    '',
    '</details>',
  ].join('\n');
}

/** The block scanA11y adds to the GitHub job summary for one scan. */
export function stepSummaryBlock(title: string, ruleSet: string, results: AxeResults): string {
  return [
    `### Accessibility: ${title}`,
    '',
    `Rules: ${ruleSet}`,
    '',
    ...(results.violations.length > 0 ? results.violations.map(summaryDetails) : ['No violations']),
    '',
    `Needs review (incomplete): ${results.incomplete.length}`,
    '',
    '',
  ].join('\n');
}

/**
 * Best-effort append to the job summary: sharded CI runs replace every
 * reporter that would surface violations on the run's Summary page, so this
 * is how they get there. A write failure must never fail the test.
 */
export function appendStepSummary(block: string, file: string | undefined): void {
  if (!file)
    return;
  try {
    fs.appendFileSync(file, block);
  } catch (err) {
    console.warn(`WARNING: could not write the accessibility step summary: ${(err as Error).message}`);
  }
}

/**
 * Scan the page as it is now and report the results on the test: a
 * screenshot of what was scanned, rule-set and incomplete-count annotations,
 * the raw results, a readable violations list, and the job summary block.
 * Files go to the test's output dir, so local runs can compare them.
 */
export async function scanA11y(page: Page, testInfo: TestInfo): Promise<AxeResults> {
  const experimental = envFlag('PW_A11Y_EXPERIMENTAL');
  const ruleSet = ruleSetLabel(experimental);

  const screenshot = testInfo.outputPath('a11y-state.png');
  await page.screenshot({ path: screenshot });
  await testInfo.attach('a11y-state.png', { path: screenshot, contentType: 'image/png' });

  const results = await runAxe(page, { experimental });

  testInfo.annotations.push(
    { type: 'a11y-rules', description: ruleSet },
    { type: 'a11y-incomplete', description: String(results.incomplete.length) },
  );

  const json = testInfo.outputPath('axe-results.json');
  fs.writeFileSync(json, JSON.stringify({
    ruleSet,
    url: results.url,
    timestamp: results.timestamp,
    testEngine: results.testEngine,
    testEnvironment: results.testEnvironment,
    violations: results.violations,
    incomplete: results.incomplete,
  }, null, 2));
  await testInfo.attach('axe-results.json', { path: json, contentType: 'application/json' });

  const text = testInfo.outputPath('axe-violations.txt');
  fs.writeFileSync(text, formatViolations(results));
  await testInfo.attach('axe-violations.txt', { path: text, contentType: 'text/plain' });

  appendStepSummary(stepSummaryBlock(testInfo.title, ruleSet, results), process.env.GITHUB_STEP_SUMMARY);
  return results;
}
