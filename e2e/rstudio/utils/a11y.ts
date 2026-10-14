import AxeBuilder from '@axe-core/playwright';
import type { AxeResults, NodeResult, Result } from 'axe-core';
import type { Page, TestInfo } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { envFlag } from './env';

/**
 * All of axe's WCAG Level A and AA tags (there is no wcag22a). Selecting by
 * tag also runs target-size, which axe's default rule set leaves off -- that
 * is how WCAG 2.2 stays in scope.
 */
export const WCAG_22_AA_TAGS: readonly string[] = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

/**
 * Files each state test leaves in its output dir, read by utils/a11y-report.ts.
 * The fixture writes the marker before the session starts, so a state whose
 * test ends before its scan still shows up in the report, as not scanned.
 */
export const STATE_MARKER = 'a11y-state.json';
export const RESULTS_FILE = 'axe-results.json';
export const SCREENSHOT_FILE = 'a11y-state.png';

const SNIPPET_MAX = 300;

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

export function wcagTags(v: Result): string {
  return v.tags.filter((t) => t.startsWith('wcag')).join(', ');
}

export function formatNode(n: NodeResult): string {
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

/** A state is named after its test file: tests/a11y/packages-pane.test.ts is "packages-pane". */
export function a11yStateName(testFile: string): string {
  return path.basename(testFile).replace(/\.test\.ts$/, '');
}

/** Mark the test's output dir as holding one accessibility state. */
export function writeStateMarker(testInfo: TestInfo): void {
  fs.mkdirSync(testInfo.outputDir, { recursive: true });
  fs.writeFileSync(testInfo.outputPath(STATE_MARKER), JSON.stringify({ state: a11yStateName(testInfo.file) }));
}

/**
 * Scan the page as it is now and report the results on the test: a
 * screenshot of what was scanned, rule-set and incomplete-count annotations,
 * the raw results, and a readable violations list. Files go to the test's
 * output dir, where CI's merge job reads them for the cross-state report.
 */
export async function scanA11y(page: Page, testInfo: TestInfo): Promise<AxeResults> {
  const experimental = envFlag('PW_A11Y_EXPERIMENTAL');
  const ruleSet = ruleSetLabel(experimental);

  const screenshot = testInfo.outputPath(SCREENSHOT_FILE);
  await page.screenshot({ path: screenshot });
  await testInfo.attach(SCREENSHOT_FILE, { path: screenshot, contentType: 'image/png' });

  const results = await runAxe(page, { experimental });

  testInfo.annotations.push(
    { type: 'a11y-rules', description: ruleSet },
    { type: 'a11y-incomplete', description: String(results.incomplete.length) },
  );

  const json = testInfo.outputPath(RESULTS_FILE);
  fs.writeFileSync(json, JSON.stringify({
    ruleSet,
    url: results.url,
    timestamp: results.timestamp,
    testEngine: results.testEngine,
    testEnvironment: results.testEnvironment,
    violations: results.violations,
    incomplete: results.incomplete,
  }, null, 2));
  await testInfo.attach(RESULTS_FILE, { path: json, contentType: 'application/json' });

  const text = testInfo.outputPath('axe-violations.txt');
  fs.writeFileSync(text, formatViolations(results));
  await testInfo.attach('axe-violations.txt', { path: text, contentType: 'text/plain' });
  return results;
}
