import type { NodeResult, Result } from 'axe-core';
import * as fs from 'fs';
import * as path from 'path';
import { formatNode, RESULTS_FILE, SCREENSHOT_FILE, STATE_MARKER, ruleTags } from './a11y';

/**
 * The cross-state accessibility report: one entry per distinct problem, with
 * the states it was seen in, built from the files each state test leaves
 * behind (see scanA11y). Most problems -- splitters, pane labels -- show up in
 * nearly every state, so a per-state report would repeat each one per state.
 */

const SUMMARY_PROBLEMS_MAX = 10;
const IMPACTS = ['minor', 'moderate', 'serious', 'critical'];

export interface ScanResults {
  ruleSet: string;
  violations: Result[];
  incomplete: Result[];
}

/** One state test's output. `results` is absent when the test ended before its scan. */
export interface StateScan {
  state: string;
  screenshot?: string;
  results?: ScanResults;
}

export type ResultKind = 'violations' | 'incomplete';

/** One element failing one rule, and every state it failed in. */
export interface Problem {
  node: NodeResult;
  states: string[];
}

export interface RuleProblems {
  rule: Result;
  impact: string;
  problems: Problem[];
}

function readJson(file: string): unknown {
  try {
    return JSON.parse(fs.readFileSync(file, 'utf-8'));
  } catch (err) {
    throw new Error(`Could not read accessibility scan file ${file}: ${(err as Error).message}`);
  }
}

function readResults(file: string): ScanResults {
  const data = readJson(file) as Partial<ScanResults>;
  if (!Array.isArray(data.violations) || !Array.isArray(data.incomplete))
    throw new Error(`Accessibility scan file ${file} has no violations/incomplete lists; was it written by scanA11y?`);
  return { ruleSet: data.ruleSet ?? 'unknown', violations: data.violations, incomplete: data.incomplete };
}

function readScan(markerFile: string): StateScan {
  const state = (readJson(markerFile) as { state?: unknown }).state;
  if (typeof state !== 'string' || state === '')
    throw new Error(`Accessibility state marker ${markerFile} has no state name`);
  const dir = path.dirname(markerFile);
  const resultsFile = path.join(dir, RESULTS_FILE);
  const screenshot = path.join(dir, SCREENSHOT_FILE);
  return {
    state,
    ...(fs.existsSync(screenshot) ? { screenshot } : {}),
    ...(fs.existsSync(resultsFile) ? { results: readResults(resultsFile) } : {}),
  };
}

/**
 * Every state marked under `root` (any depth), sorted by state name. Two
 * markers naming one state are an error: each would hide the other's results.
 */
export function readStateScans(root: string): StateScan[] {
  if (!fs.existsSync(root))
    throw new Error(`No accessibility results directory at ${root}`);
  const markers = fs.readdirSync(root, { recursive: true, encoding: 'utf-8' })
    .filter((f) => path.basename(f) === STATE_MARKER)
    .map((f) => path.join(root, f))
    .sort();
  const byState = new Map<string, string>();
  const scans: StateScan[] = [];
  for (const marker of markers) {
    const scan = readScan(marker);
    const other = byState.get(scan.state);
    if (other) {
      throw new Error(`Two tests scanned state "${scan.state}" (${path.dirname(other)} and ` +
        `${path.dirname(marker)}). A state is named after its test file; keep one state test per file.`);
    }
    byState.set(scan.state, marker);
    scans.push(scan);
  }
  return scans.sort((a, b) => a.state.localeCompare(b.state));
}

function higherImpact(a: string, b: string | null | undefined): string {
  return IMPACTS.indexOf(b ?? '') > IMPACTS.indexOf(a) ? b! : a;
}

/**
 * Problems grouped by rule, ordered by rule id. A problem is a rule and a
 * target selector: the HTML snippet can carry layout (a splitter's position),
 * so the same element would differ between states.
 */
export function groupProblems(scans: StateScan[], kind: ResultKind): RuleProblems[] {
  const rules = new Map<string, { rule: Result; impact: string; problems: Map<string, Problem> }>();
  for (const scan of scans) {
    for (const rule of scan.results?.[kind] ?? []) {
      let group = rules.get(rule.id);
      if (!group) {
        group = { rule, impact: 'unknown', problems: new Map() };
        rules.set(rule.id, group);
      }
      group.impact = higherImpact(group.impact, rule.impact);
      for (const node of rule.nodes) {
        const key = JSON.stringify(node.target);
        const problem = group.problems.get(key);
        if (!problem)
          group.problems.set(key, { node, states: [scan.state] });
        else if (!problem.states.includes(scan.state))
          problem.states.push(scan.state);
      }
    }
  }
  return [...rules.values()]
    .sort((a, b) => a.rule.id.localeCompare(b.rule.id))
    .map((g) => ({ rule: g.rule, impact: g.impact, problems: [...g.problems.values()] }));
}

/**
 * For rule lines in the summary: help text can contain markup, e.g.
 * "<html> element must have a lang attribute".
 */
function escapeHtml(text: string): string {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function ruleLine(g: RuleProblems): string {
  return escapeHtml(`${g.rule.id} [${g.impact}] ${g.problems.length} distinct node(s): ${g.rule.help}`);
}

function stateList(states: string[], url: string | undefined): string {
  return states.map((s) => (url ? `[${s}](${url})` : s)).join(', ');
}

/**
 * A rule's help link and problems, each led by the states it was seen in.
 * Node lines are indented, so none can close the code fence early. The cap
 * keeps many states under GitHub's 1 MiB per-step summary limit, past which
 * the whole summary is dropped.
 */
function ruleBody(g: RuleProblems, url: string | undefined, fullList: string): string[] {
  const shown = g.problems.slice(0, SUMMARY_PROBLEMS_MAX);
  const hidden = g.problems.length - shown.length;
  return [
    `[Rule help](${g.rule.helpUrl}) - ${ruleTags(g.rule)}`,
    '',
    ...shown.flatMap((p) => [
      `**Seen in:** ${stateList(p.states, url)}`,
      '',
      '```',
      formatNode(p.node),
      '```',
      '',
    ]),
    ...(hidden > 0
      ? [`${hidden} more node(s): see each state's ${fullList} in the test-results artifact`, '']
      : []),
  ];
}

function violationDetails(g: RuleProblems, url: string | undefined): string {
  return [
    '<details>',
    `<summary>${ruleLine(g)}</summary>`,
    '',
    ...ruleBody(g, url, 'axe-violations.txt'),
    '</details>',
  ].join('\n');
}

/** One collapsed section for all incomplete results, apart from the violations. */
function needsReviewSection(groups: RuleProblems[], url: string | undefined): string {
  const count = groups.reduce((n, g) => n + g.problems.length, 0);
  if (count === 0)
    return 'Needs review (incomplete): 0';
  return [
    '<details>',
    `<summary>Needs review (incomplete): ${count} distinct node(s)</summary>`,
    '',
    ...groups.flatMap((g) => [`**${ruleLine(g)}**`, '', ...ruleBody(g, url, RESULTS_FILE)]),
    '</details>',
  ].join('\n');
}

/**
 * The job summary for the whole run. State names link to the screenshots
 * page when `screenshotsUrl` is given; GitHub drops a #fragment on artifact
 * links, so they all point at its top, which lists the states.
 */
export function summaryMarkdown(scans: StateScan[], screenshotsUrl: string | undefined): string {
  const scanned = scans.filter((s) => s.results);
  const notScanned = scans.filter((s) => !s.results).map((s) => s.state);
  const violations = groupProblems(scans, 'violations');
  const distinct = violations.reduce((n, g) => n + g.problems.length, 0);
  const ruleSets = [...new Set(scanned.map((s) => s.results!.ruleSet))].join(', ');
  const heading = distinct > 0
    ? `${distinct} distinct violation(s) across ${scanned.length} state(s)`
    : `no violations across ${scanned.length} state(s)`;
  return [
    `### Accessibility: ${heading}`,
    '',
    `Rules: ${ruleSets || 'none (no state was scanned)'}`,
    '',
    screenshotsUrl
      ? `States (screenshots): ${stateList(scans.map((s) => s.state), screenshotsUrl)}`
      : `States: ${stateList(scans.map((s) => s.state), undefined)}`,
    '',
    ...(notScanned.length > 0
      ? [`**Not scanned:** ${notScanned.join(', ')} (the test ended before its scan finished; see its test result)`, '']
      : []),
    ...violations.flatMap((g) => [violationDetails(g, screenshotsUrl), '']),
    needsReviewSection(groupProblems(scans, 'incomplete'), screenshotsUrl),
    '',
  ].join('\n');
}

function stateSection(scan: StateScan): string {
  const name = escapeHtml(scan.state);
  const image = scan.screenshot
    ? `<img alt="RStudio in the ${name} state, as scanned" ` +
      `src="data:image/png;base64,${fs.readFileSync(scan.screenshot).toString('base64')}">`
    : '<p>No screenshot: the test ended before taking it.</p>';
  return [
    `<section id="state-${name}">`,
    `<h2>${name}</h2>`,
    ...(scan.results ? [] : ['<p>Not scanned: the test ended before its scan finished.</p>']),
    image,
    '</section>',
  ].join('\n');
}

/**
 * One self-contained page of every state's screenshot, for upload as an
 * unzipped artifact. Images are inlined because the page is served alone;
 * the table of contents stands in for the #fragments GitHub drops.
 */
export function screenshotsHtml(scans: StateScan[]): string {
  return [
    '<!doctype html>',
    '<html lang="en">',
    '<head>',
    '<meta charset="utf-8">',
    '<title>Accessibility scan screenshots</title>',
    '<style>body { font-family: sans-serif; } img { max-width: 100%; border: 1px solid #555; }</style>',
    '</head>',
    '<body>',
    '<h1>Accessibility scan screenshots</h1>',
    '<nav aria-label="States">',
    '<ul>',
    ...scans.map((s) => `<li><a href="#state-${escapeHtml(s.state)}">${escapeHtml(s.state)}</a></li>`),
    '</ul>',
    '</nav>',
    '<main>',
    ...scans.map(stateSection),
    '</main>',
    '</body>',
    '</html>',
    '',
  ].join('\n');
}
