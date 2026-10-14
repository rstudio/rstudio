import { test, expect } from '@playwright/test';
import * as fs from 'fs';
import type { AxeResults, NodeResult, Result } from 'axe-core';
import {
  appendStepSummary,
  formatViolations,
  stepSummaryBlock,
  violationSummary,
} from '@utils/a11y';

/**
 * Harness self-test for the accessibility report formatting in utils/a11y.ts.
 * Pure functions over hand-built axe results; no browser, no IDE.
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

test.describe('stepSummaryBlock', () => {
  test('gives each rule a collapsible section with its help link and nodes', () => {
    const block = stepSummaryBlock('initial state', 'WCAG 2.2 AA', results([violation()], [violation()]));
    expect(block).toContain('### Accessibility: initial state');
    expect(block).toContain('Rules: WCAG 2.2 AA');
    expect(block).toContain(
      '<summary>image-alt [critical] 1 node(s): Images must have alternative text</summary>');
    expect(block).toContain(
      '[Rule help](https://dequeuniversity.com/rules/axe/4.13/image-alt) - wcag2a, wcag111');
    expect(block).toContain('```\n  - <img src="logo.png">\n');
    expect(block).toContain('      Element does not have an alt attribute\n');
    expect(block).toContain('      target: ["img"]\n```');
    expect(block).toContain('Needs review (incomplete): 1');
  });

  test('closes every section it opens, one per rule', () => {
    const block = stepSummaryBlock('initial state', 'WCAG 2.2 AA', results([
      violation(),
      violation({ id: 'target-size', help: 'Targets must be big enough' }),
    ]));
    expect(block.match(/<details>/g)).toHaveLength(2);
    expect(block.match(/<\/details>/g)).toHaveLength(2);
    expect(block.match(/^```$/gm)).toHaveLength(4);
  });

  test('escapes markup in the rule help so it shows as text', () => {
    const block = stepSummaryBlock('initial state', 'WCAG 2.2 AA', results([violation({
      id: 'html-has-lang', help: '<html> element must have a lang attribute',
    })]));
    expect(block).toContain('&lt;html&gt; element must have a lang attribute</summary>');
  });

  test('caps the nodes shown per rule and points to the full list', () => {
    const nodes = Array.from({ length: 12 }, (_, i) => node({ html: `<img id="n${i}">` }));
    const block = stepSummaryBlock('initial state', 'WCAG 2.2 AA', results([violation({ nodes })]));
    expect(block).toContain('12 node(s)');
    expect(block).toContain('<img id="n9">');
    expect(block).not.toContain('<img id="n10">');
    expect(block).toContain('2 more node(s) in axe-violations.txt');
  });

  test('omits the overflow note when every node fits', () => {
    const block = stepSummaryBlock('initial state', 'WCAG 2.2 AA', results([violation()]));
    expect(block).not.toContain('more node(s)');
  });

  test('lists incomplete results in one collapsed section after the violations', () => {
    const block = stepSummaryBlock('initial state', 'WCAG 2.2 AA', results([violation()], [
      violation({ id: 'th-has-data-cells', help: 'Table headers must refer to data cells',
        nodes: [node({ html: '<table>' })] }),
      violation({ id: 'color-contrast', help: 'Elements must meet minimum contrast',
        nodes: [node({ html: '<span>' })] }),
    ]));
    const section = block.slice(block.indexOf('<summary>Needs review (incomplete): 2</summary>'));
    expect(section).not.toBe(block);
    expect(section).not.toContain('Images must have alternative text');
    expect(section).toContain(
      '**th-has-data-cells [critical] 1 node(s): Table headers must refer to data cells**');
    expect(section).toContain(
      '**color-contrast [critical] 1 node(s): Elements must meet minimum contrast**');
    expect(section).toContain('```\n  - <table>\n');
    expect(section.match(/<\/details>/g)).toHaveLength(1);
  });

  test('points incomplete overflow at axe-results.json', () => {
    const nodes = Array.from({ length: 11 }, () => node());
    const block = stepSummaryBlock('initial state', 'WCAG 2.2 AA',
      results([], [violation({ nodes })]));
    expect(block).toContain('1 more node(s) in axe-results.json');
    expect(block).not.toContain('axe-violations.txt');
  });

  test('escapes markup in incomplete rule lines', () => {
    const block = stepSummaryBlock('initial state', 'WCAG 2.2 AA', results([], [violation({
      id: 'html-has-lang', help: '<html> element must have a lang attribute',
    })]));
    expect(block).toContain('**html-has-lang [critical] 1 node(s): &lt;html&gt; element must');
  });

  test('gives a plain zero count, not a section, when nothing needs review', () => {
    const block = stepSummaryBlock('initial state', 'WCAG 2.2 AA', results([violation()]));
    expect(block).toContain('</details>\n\nNeeds review (incomplete): 0\n');
    expect(block.match(/<details>/g)).toHaveLength(1);
  });

  test('says "No violations" for a clean scan and still reports incomplete', () => {
    const block = stepSummaryBlock('initial state', 'WCAG 2.2 AA', results([], [violation(), violation()]));
    expect(block).toContain(
      'Rules: WCAG 2.2 AA\n\nNo violations\n\n<details>\n<summary>Needs review');
    expect(block).toContain('Needs review (incomplete): 2');
  });
});

test.describe('appendStepSummary', () => {
  test('appends to the given file', async ({}, testInfo) => {
    const file = testInfo.outputPath('summary.md');
    fs.writeFileSync(file, 'existing\n');
    appendStepSummary('block\n', file);
    expect(fs.readFileSync(file, 'utf-8')).toBe('existing\nblock\n');
  });

  test('does nothing when no summary file is configured', () => {
    expect(() => appendStepSummary('block\n', undefined)).not.toThrow();
  });

  test('warns instead of throwing when the file cannot be written', async ({}, testInfo) => {
    const dir = testInfo.outputPath('not-a-file');
    fs.mkdirSync(dir, { recursive: true });
    const warnings: string[] = [];
    const original = console.warn;
    console.warn = (msg: string) => { warnings.push(msg); };
    try {
      expect(() => appendStepSummary('block\n', dir)).not.toThrow();
    } finally {
      console.warn = original;
    }
    expect(warnings.join('\n')).toContain('could not write the accessibility step summary');
  });
});
