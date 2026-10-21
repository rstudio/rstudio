import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import type { AxeResults, NodeResult, Result } from 'axe-core';
import * as path from 'path';
import {
  a11yStateName,
  formatViolations,
  RESULTS_FILE,
  ruleSetLabel,
  SCREENSHOT_FILE,
  STATE_MARKER,
  violationSummary,
} from '@utils/a11y';
import {
  groupProblems,
  readStateScans,
  screenshotsHtml,
  summaryMarkdown,
  type StateScan,
} from '@utils/a11y-report';

/**
 * Harness self-test for the accessibility report formatting in utils/a11y.ts
 * and the cross-state report in utils/a11y-report.ts. Hand-built axe results
 * and files; no browser, no IDE.
 */

function node(overrides: Partial<NodeResult> = {}): NodeResult {
  return {
    html: '<img src="logo.png">',
    target: ['img'],
    failureSummary: 'Fix any of the following:\n  Element does not have an alt attribute',
    any: [], all: [], none: [],
    impact: 'critical',
    ...overrides,
  } as NodeResult;
}

function violation(overrides: Partial<Result> = {}): Result {
  return {
    id: 'image-alt',
    impact: 'critical',
    help: 'Images must have alternative text',
    helpUrl: 'https://dequeuniversity.com/rules/axe/4.13/image-alt',
    description: '',
    tags: ['cat.text-alternatives', 'wcag2a', 'wcag111'],
    nodes: [node()],
    ...overrides,
  } as Result;
}

function results(violations: Result[], incomplete: Result[] = []): AxeResults {
  return { violations, incomplete, passes: [], inapplicable: [] } as unknown as AxeResults;
}

function scan(state: string, violations: Result[], incomplete: Result[] = []): StateScan {
  return { state, results: { ruleSet: 'WCAG 2.2 AA', violations, incomplete } };
}

const URL = 'https://github.com/o/r/actions/runs/1/artifacts/2';

test.describe('violationSummary', () => {
  test('gives one line per violation with id, impact, node count and help', () => {
    const r = results([
      violation(),
      violation({ id: 'target-size', impact: 'serious', help: 'Targets must be big enough',
        nodes: [node(), node()] }),
    ]);
    expect(violationSummary(r)).toEqual([
      'image-alt [critical] 1 node(s): Images must have alternative text',
      'target-size [serious] 2 node(s): Targets must be big enough',
    ]);
  });

  test('is empty for a clean scan', () => {
    expect(violationSummary(results([]))).toEqual([]);
  });
});

test.describe('formatViolations', () => {
  test('leads each node with its snippet, then the failure summary, then the target', () => {
    const text = formatViolations(results([violation()]));
    expect(text).toContain('image-alt [critical] Images must have alternative text');
    expect(text).toContain('https://dequeuniversity.com/rules/axe/4.13/image-alt');
    expect(text).toContain('tags: wcag2a, wcag111');
    expect(text).not.toContain('cat.text-alternatives');
    const snippet = text.indexOf('<img src="logo.png">');
    const summary = text.indexOf('Element does not have an alt attribute');
    const target = text.indexOf('target: ["img"]');
    expect(snippet).toBeGreaterThan(-1);
    expect(summary).toBeGreaterThan(snippet);
    expect(target).toBeGreaterThan(summary);
  });

  test('tags best-practice rules as such', () => {
    const text = formatViolations(results([violation({
      id: 'region', tags: ['cat.keyboard', 'best-practice'],
    })]));
    expect(text).toContain('tags: best-practice');
    expect(text).not.toContain('cat.keyboard');
  });

  test('collapses and trims a huge snippet to one bounded line', () => {
    const html = `<div id="rstudio_container">\n${'<span>x</span>\n'.repeat(500)}</div>`;
    const text = formatViolations(results([violation({ nodes: [node({ html })] })]));
    const lines = text.split('\n');
    expect(lines.filter((l) => l.includes('<span>'))).toHaveLength(1);
    const line = lines.find((l) => l.includes('rstudio_container'))!;
    expect(line.trim().length).toBeLessThanOrEqual(2 + 300 + 3); // "- " + snippet + "..."
    expect(line.trimEnd().endsWith('...')).toBe(true);
  });

  test('renders frame and shadow-DOM targets', () => {
    const text = formatViolations(results([violation({
      nodes: [
        node({ target: ['iframe#rstudio_help_frame', '#topic-title'] }),
        node({ target: [['#host', 'button']] }),
      ],
    })]));
    expect(text).toContain('target: ["iframe#rstudio_help_frame","#topic-title"]');
    expect(text).toContain('target: [["#host","button"]]');
    expect(text).not.toContain('[object Object]');
  });

  test('says so when there are no violations', () => {
    expect(formatViolations(results([]))).toBe('No violations\n');
  });
});

test.describe('ruleSetLabel', () => {
  test('names the WCAG version and level, then any extra rule groups', () => {
    const base = { level: 'AA', version: '2.2', bestPractices: false, experimental: false } as const;
    expect(ruleSetLabel(base)).toBe('WCAG 2.2 AA');
    expect(ruleSetLabel({ ...base, bestPractices: true })).toBe('WCAG 2.2 AA + best practices');
    expect(ruleSetLabel({ ...base, experimental: true })).toBe('WCAG 2.2 AA + experimental');
    expect(ruleSetLabel({ ...base, bestPractices: true, experimental: true }))
      .toBe('WCAG 2.2 AA + best practices + experimental');
  });
});

test.describe('a11yStateName', () => {
  test('is the test file name without its .test.ts suffix', () => {
    expect(a11yStateName('/repo/e2e/rstudio/tests/a11y/packages-pane.test.ts')).toBe('packages-pane');
  });
});

test.describe('groupProblems', () => {
  test('merges the same rule and target across states into one problem', () => {
    const groups = groupProblems([
      scan('initial-state', [violation()]),
      scan('packages-pane', [violation()]),
    ], 'violations');
    expect(groups).toHaveLength(1);
    expect(groups[0].problems).toHaveLength(1);
    expect(groups[0].problems[0].states).toEqual(['initial-state', 'packages-pane']);
  });

  test('keeps the same rule on different targets apart', () => {
    const groups = groupProblems([
      scan('initial-state', [violation({ nodes: [node({ target: ['#a'] })] })]),
      scan('packages-pane', [violation({ nodes: [node({ target: ['#b'] })] })]),
    ], 'violations');
    expect(groups[0].problems.map((p) => p.states)).toEqual([['initial-state'], ['packages-pane']]);
  });

  test('matches on the target, not the snippet, so layout-dependent markup still merges', () => {
    const groups = groupProblems([
      scan('initial-state', [violation({ nodes: [node({ html: '<div style="left: 500px">' })] })]),
      scan('packages-pane', [violation({ nodes: [node({ html: '<div style="left: 620px">' })] })]),
    ], 'violations');
    expect(groups[0].problems).toHaveLength(1);
    expect(groups[0].problems[0].node.html).toBe('<div style="left: 500px">');
  });

  test('keeps different rules on the same target apart, ordered by rule id', () => {
    const groups = groupProblems([scan('initial-state', [
      violation({ id: 'target-size' }),
      violation({ id: 'aria-allowed-attr' }),
    ])], 'violations');
    expect(groups.map((g) => g.rule.id)).toEqual(['aria-allowed-attr', 'target-size']);
  });

  test('reports the highest impact any state saw for a rule', () => {
    const groups = groupProblems([
      scan('initial-state', [violation({ impact: 'moderate' })]),
      scan('packages-pane', [violation({ impact: 'serious' })]),
      scan('untitled-r-script', [violation({ impact: null })]),
    ], 'violations');
    expect(groups[0].impact).toBe('serious');
  });

  test('groups incomplete results separately from violations', () => {
    const scans = [scan('initial-state', [violation()], [violation({ id: 'color-contrast' })])];
    expect(groupProblems(scans, 'violations').map((g) => g.rule.id)).toEqual(['image-alt']);
    expect(groupProblems(scans, 'incomplete').map((g) => g.rule.id)).toEqual(['color-contrast']);
  });

  test('skips states that were not scanned', () => {
    expect(groupProblems([{ state: 'packages-pane' }], 'violations')).toEqual([]);
  });
});

test.describe('summaryMarkdown', () => {
  test('heads with the distinct problem and state counts and the rule set', () => {
    const md = summaryMarkdown([
      scan('initial-state', [violation(), violation({ id: 'target-size' })]),
      scan('packages-pane', [violation()]),
    ], URL);
    expect(md).toContain('### Accessibility: 2 distinct violation(s) across 2 state(s)\n');
    expect(md).toContain('Rules: WCAG 2.2 AA\n');
  });

  test('links every state name to the screenshots page', () => {
    const md = summaryMarkdown([scan('initial-state', []), scan('packages-pane', [])], URL);
    expect(md).toContain(`States (screenshots): [initial-state](${URL}), [packages-pane](${URL})\n`);
  });

  test('names states without links when there is no screenshots page', () => {
    const md = summaryMarkdown([scan('initial-state', [violation()])], undefined);
    expect(md).toContain('States: initial-state\n');
    expect(md).toContain('**Seen in:** initial-state\n');
    expect(md).not.toContain('[initial-state]');
  });

  test('gives each problem the states it was seen in, then its node', () => {
    const md = summaryMarkdown([
      scan('initial-state', [violation()]),
      scan('packages-pane', [violation()]),
    ], URL);
    expect(md).toContain(
      '<summary>image-alt [critical] 1 distinct node(s): Images must have alternative text</summary>');
    expect(md).toContain(
      '[Rule help](https://dequeuniversity.com/rules/axe/4.13/image-alt) - wcag2a, wcag111');
    expect(md).toContain(
      `**Seen in:** [initial-state](${URL}), [packages-pane](${URL})\n\n\`\`\`\n  - <img src="logo.png">\n`);
    expect(md).toContain('      target: ["img"]\n```');
    expect(md.match(/<img src="logo.png">/g)).toHaveLength(1);
  });

  test('closes every section it opens', () => {
    const md = summaryMarkdown([scan('initial-state',
      [violation(), violation({ id: 'target-size' })], [violation()])], URL);
    expect(md.match(/<details>/g)).toHaveLength(3);
    expect(md.match(/<\/details>/g)).toHaveLength(3);
    expect(md.match(/^```$/gm)!.length % 2).toBe(0);
  });

  test('escapes markup in the rule help so it shows as text', () => {
    const md = summaryMarkdown([scan('initial-state', [violation({
      id: 'html-has-lang', help: '<html> element must have a lang attribute',
    })])], URL);
    expect(md).toContain('&lt;html&gt; element must have a lang attribute</summary>');
  });

  test('caps the problems shown per rule and says where the rest are', () => {
    const nodes = Array.from({ length: 12 }, (_, i) => node({ html: `<img id="n${i}">`, target: [`#n${i}`] }));
    const md = summaryMarkdown([scan('initial-state', [violation({ nodes })])], URL);
    expect(md).toContain('12 distinct node(s)');
    expect(md).toContain('<img id="n9">');
    expect(md).not.toContain('<img id="n10">');
    expect(md).toContain("2 more node(s): see each state's axe-violations.txt in the test-results artifact");
  });

  test('lists incomplete results in one collapsed section, pointing overflow at the JSON', () => {
    const nodes = Array.from({ length: 11 }, (_, i) => node({ target: [`#n${i}`] }));
    const md = summaryMarkdown([scan('initial-state', [violation()], [
      violation({ id: 'color-contrast', help: 'Elements must meet minimum contrast', nodes }),
    ])], URL);
    const section = md.slice(md.indexOf('<summary>Needs review (incomplete): 11 distinct node(s)</summary>'));
    expect(section).not.toBe(md);
    expect(section).not.toContain('Images must have alternative text');
    expect(section).toContain('**color-contrast [critical] 11 distinct node(s): Elements must meet minimum contrast**');
    expect(section).toContain("1 more node(s): see each state's axe-results.json in the test-results artifact");
  });

  test('gives a plain zero count, not a section, when nothing needs review', () => {
    const md = summaryMarkdown([scan('initial-state', [violation()])], URL);
    expect(md).toContain('</details>\n\nNeeds review (incomplete): 0\n');
  });

  test('says "No violations" when every scanned state is clean', () => {
    const md = summaryMarkdown([scan('initial-state', []), scan('packages-pane', [])], URL);
    expect(md).toContain('### Accessibility: no violations across 2 state(s)\n');
    expect(md).not.toContain('<details>');
  });

  test('names states that were not scanned and leaves them out of the counts', () => {
    const md = summaryMarkdown([scan('initial-state', [violation()]), { state: 'packages-pane' }], URL);
    expect(md).toContain('across 1 state(s)');
    expect(md).toContain(
      '**Not scanned:** packages-pane (the test ended before its scan finished; see its test result)\n');
  });
});

test.describe('screenshotsHtml', () => {
  function png(testInfo: { outputPath: (...p: string[]) => string }, name: string, bytes: number[]): string {
    const file = testInfo.outputPath(name);
    fs.writeFileSync(file, Buffer.from(bytes));
    return file;
  }

  test('gives a table of contents and one section per state with its screenshot inline', async ({}, testInfo) => {
    const html = screenshotsHtml([
      { ...scan('initial-state', []), screenshot: png(testInfo, 'a.png', [1, 2, 3]) },
      { ...scan('packages-pane', []), screenshot: png(testInfo, 'b.png', [4, 5, 6]) },
    ]);
    expect(html).toContain('<html lang="en">');
    expect(html).toContain('<li><a href="#state-initial-state">initial-state</a></li>');
    expect(html).toContain('<li><a href="#state-packages-pane">packages-pane</a></li>');
    expect(html).toContain('<section id="state-initial-state">');
    expect(html).toContain(`src="data:image/png;base64,${Buffer.from([1, 2, 3]).toString('base64')}"`);
    expect(html).toContain(`src="data:image/png;base64,${Buffer.from([4, 5, 6]).toString('base64')}"`);
    expect(html).toContain('alt="RStudio in the packages-pane state, as scanned"');
  });

  test('says when a state has no screenshot or no scan', () => {
    const html = screenshotsHtml([{ state: 'packages-pane' }]);
    expect(html).toContain('<p>No screenshot: the test ended before taking it.</p>');
    expect(html).toContain('<p>Not scanned: the test ended before its scan finished.</p>');
    expect(html).not.toContain('<img');
  });

  test('escapes state names', () => {
    const html = screenshotsHtml([{ state: 'a<b' }]);
    expect(html).toContain('<h2>a&lt;b</h2>');
    expect(html).not.toContain('a<b');
  });
});

test.describe('readStateScans', () => {
  function stateDir(root: string, dir: string, state: string, files: { results?: object; screenshot?: boolean } = {}): string {
    const full = path.join(root, dir);
    fs.mkdirSync(full, { recursive: true });
    fs.writeFileSync(path.join(full, STATE_MARKER), JSON.stringify({ state }));
    if (files.results)
      fs.writeFileSync(path.join(full, RESULTS_FILE), JSON.stringify(files.results));
    if (files.screenshot)
      fs.writeFileSync(path.join(full, SCREENSHOT_FILE), 'png');
    return full;
  }

  const scanned = { ruleSet: 'WCAG 2.2 AA', violations: [violation()], incomplete: [] };

  test('reads each marked state with its results and screenshot, sorted by state', async ({}, testInfo) => {
    const root = testInfo.outputPath('results');
    const b = stateDir(root, 'a11y-packages-pane-xyz', 'packages-pane', { results: scanned, screenshot: true });
    stateDir(root, 'nested/a11y-initial-state-abc', 'initial-state', { results: scanned });
    fs.mkdirSync(path.join(root, 'unrelated-test'), { recursive: true });
    const scans = readStateScans(root);
    expect(scans.map((s) => s.state)).toEqual(['initial-state', 'packages-pane']);
    expect(scans[0].screenshot).toBeUndefined();
    expect(scans[1].screenshot).toBe(path.join(b, SCREENSHOT_FILE));
    expect(scans[1].results?.violations.map((v) => v.id)).toEqual(['image-alt']);
  });

  test('keeps a marked state without results as not scanned', async ({}, testInfo) => {
    const root = testInfo.outputPath('results');
    stateDir(root, 'a', 'packages-pane', { screenshot: true });
    expect(readStateScans(root)[0].results).toBeUndefined();
  });

  test('is empty when nothing is marked', async ({}, testInfo) => {
    const root = testInfo.outputPath('results');
    fs.mkdirSync(root, { recursive: true });
    expect(readStateScans(root)).toEqual([]);
  });

  test('fails on a missing directory, naming it', async ({}, testInfo) => {
    const root = testInfo.outputPath('absent');
    expect(() => readStateScans(root)).toThrow(root);
  });

  test('fails when two tests claim the same state, naming both', async ({}, testInfo) => {
    const root = testInfo.outputPath('results');
    stateDir(root, 'one', 'initial-state');
    stateDir(root, 'two', 'initial-state');
    expect(() => readStateScans(root)).toThrow(/state "initial-state".*one.*two/s);
  });

  test('fails on an unreadable marker or results file, naming it', async ({}, testInfo) => {
    const root = testInfo.outputPath('results');
    const dir = stateDir(root, 'a', 'initial-state');
    fs.writeFileSync(path.join(dir, RESULTS_FILE), '{ not json');
    expect(() => readStateScans(root)).toThrow(path.join(dir, RESULTS_FILE));
    fs.writeFileSync(path.join(dir, STATE_MARKER), JSON.stringify({ state: '' }));
    expect(() => readStateScans(root)).toThrow(path.join(dir, STATE_MARKER));
  });

  test('fails on results without violation and incomplete lists', async ({}, testInfo) => {
    const root = testInfo.outputPath('results');
    const dir = stateDir(root, 'a', 'initial-state', { results: { ruleSet: 'WCAG 2.2 AA' } });
    expect(() => readStateScans(root)).toThrow(path.join(dir, RESULTS_FILE));
  });
});
