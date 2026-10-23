import AxeBuilder from '@axe-core/playwright';
import type { AxeResults, NodeResult, Result } from 'axe-core';
import type { Page, TestInfo } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';
import { envChoice, envFlag } from './env';

export const A11Y_LEVELS = ['A', 'AA', 'AAA'] as const;
export const WCAG_VERSIONS = ['2.1', '2.2'] as const;
export type A11yLevel = typeof A11Y_LEVELS[number];
export type WcagVersion = typeof WCAG_VERSIONS[number];

export interface RuleSet {
  level: A11yLevel;
  version: WcagVersion;
  bestPractices: boolean;
  experimental: boolean;
}

// axe's tag for each WCAG version and level it has rules for; it has no
// wcag22a, wcag21aaa or wcag22aaa, and logs a warning for unknown tags.
const AXE_WCAG_TAGS: Record<'2.0' | WcagVersion, Partial<Record<A11yLevel, string>>> = {
  '2.0': { A: 'wcag2a', AA: 'wcag2aa', AAA: 'wcag2aaa' },
  '2.1': { A: 'wcag21a', AA: 'wcag21aa' },
  '2.2': { AA: 'wcag22aa' },
};

/**
 * axe's tags for every success criterion at or below `level`, from WCAG 2.0
 * through `version`. Selecting by tag also runs target-size (2.2 AA), which
 * axe's default rule set leaves off -- that is how WCAG 2.2 stays in scope.
 */
export function ruleSetTags({ level, version }: Pick<RuleSet, 'level' | 'version'>): string[] {
  const levels = A11Y_LEVELS.slice(0, A11Y_LEVELS.indexOf(level) + 1);
  const versions = (['2.0', ...WCAG_VERSIONS] as const).slice(0, WCAG_VERSIONS.indexOf(version) + 2);
  return versions.flatMap((v) => levels.flatMap((l) => AXE_WCAG_TAGS[v][l] ?? []));
}

/**
 * The rule set picked by PW_A11Y_LEVEL, PW_A11Y_WCAG_VERSION,
 * PW_A11Y_BEST_PRACTICES and PW_A11Y_EXPERIMENTAL.
 */
export function ruleSetFromEnv(): RuleSet {
  return {
    level: envChoice('PW_A11Y_LEVEL', A11Y_LEVELS, 'AA'),
    version: envChoice('PW_A11Y_WCAG_VERSION', WCAG_VERSIONS, '2.2'),
    bestPractices: envFlag('PW_A11Y_BEST_PRACTICES'),
    experimental: envFlag('PW_A11Y_EXPERIMENTAL'),
  };
}

/**
 * Files each state test leaves in its output dir, read by utils/a11y-report.ts.
 * The fixture writes the marker before the session starts, so a state whose
 * test ends before its scan still shows up in the report, as not scanned.
 */
export const STATE_MARKER = 'a11y-state.json';
export const RESULTS_FILE = 'axe-results.json';
export const SCREENSHOT_FILE = 'a11y-state.png';

const SNIPPET_MAX = 300;

export function ruleSetLabel({ level, version, bestPractices, experimental }: RuleSet): string {
  const label = `WCAG ${version} ${level}`;
  const extras = [bestPractices && 'best practices', experimental && 'experimental'].filter(Boolean);
  return [label, ...extras].join(' + ');
}

/**
 * Scan the page (frames included) with the rule set's WCAG tags, plus axe's
 * best-practice rules with `bestPractices`. With `experimental`, axe's
 * experimental rules are added; tag selection is OR-only, so results are then
 * filtered back to the selected tags to drop the experimental rules outside them.
 */
export async function runAxe(page: Page, ruleSet: RuleSet): Promise<AxeResults> {
  const selected = [...ruleSetTags(ruleSet), ...(ruleSet.bestPractices ? ['best-practice'] : [])];
  const tags = ruleSet.experimental ? [...selected, 'experimental'] : selected;
  const results = await new AxeBuilder({ page }).withTags(tags).analyze();
  if (!ruleSet.experimental)
    return results;
  const inScope = (r: Result) => r.tags.some((t) => selected.includes(t));
  return {
    ...results,
    violations: results.violations.filter(inScope),
    incomplete: results.incomplete.filter(inScope),
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

/** The rule's WCAG tags, and `best-practice` for axe's best-practice rules. */
export function ruleTags(v: Result): string {
  return v.tags.filter((t) => t.startsWith('wcag') || t === 'best-practice').join(', ');
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
    `  tags: ${ruleTags(v)}`,
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
  const selected = ruleSetFromEnv();
  const ruleSet = ruleSetLabel(selected);

  const screenshot = testInfo.outputPath(SCREENSHOT_FILE);
  await page.screenshot({ path: screenshot });
  await testInfo.attach(SCREENSHOT_FILE, { path: screenshot, contentType: 'image/png' });

  const results = await runAxe(page, selected);

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
