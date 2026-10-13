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
  test('lists the violation lines in a fenced block with the incomplete count', () => {
    const block = stepSummaryBlock('initial state', 'WCAG 2.2 AA', results([violation()], [violation()]));
    expect(block).toContain('initial state');
    expect(block).toContain('WCAG 2.2 AA');
    expect(block).toContain('```\nimage-alt [critical] 1 node(s): Images must have alternative text\n```');
    expect(block).toContain('Needs review (incomplete): 1');
  });

  test('says "No violations" for a clean scan and still reports incomplete', () => {
    const block = stepSummaryBlock('initial state', 'WCAG 2.2 AA', results([], [violation(), violation()]));
    expect(block).toContain('```\nNo violations\n```');
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
