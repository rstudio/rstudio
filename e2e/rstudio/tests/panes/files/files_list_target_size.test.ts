// Files pane: the file list's sort headers and row checkboxes meet WCAG 2.5.8
// Target Size (Minimum) (#19786).
//
// The header row was 19px, with 13px-tall sort buttons, and rows were 22.5px,
// so neighboring 13x13 checkboxes were closer than 24px. axe-core's target-size
// rule reports these as Needs Review rather than violations, since the targets
// have tabindex=-1, so the scan fails on either result.

import AxeBuilder from '@axe-core/playwright';
import type { NodeResult } from 'axe-core';

import { test, expect } from '@fixtures/rstudio.fixture';

import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';

const FILES_PANEL = '#rstudio_workbench_panel_files';
const FILE_LIST = `${FILES_PANEL} [role="table"]`;
const FILE_COUNT = 5;

function describeNode(node: NodeResult): string {
  const messages = [...node.any, ...node.all, ...node.none].map((check) => check.message);
  return `${node.html.replace(/\s+/g, ' ').slice(0, 120)}: ${messages.join(' | ')}`;
}

test.describe('Files pane file list target size (#19786)', () => {
  // axe opens a page of its own to merge its results, which Desktop's CDP
  // connection to Electron refuses (Target.createTarget: Not supported).
  test('sort headers and row checkboxes are 24px targets', { tag: '@server_only' }, async ({
    rstudioPage: page,
  }) => {
    const consoleActions = new ConsolePaneActions(page);
    const dir = 'file.path(tempdir(), "files-target-size")';

    try {
      await consoleActions.executeInConsole(
        `local({ d <- ${dir}; dir.create(d, showWarnings = FALSE); ` +
        `invisible(file.create(file.path(d, sprintf("file-%d.txt", 1:${FILE_COUNT})))); ` +
        '.rs.api.filesPaneNavigate(d) })',
        { wait: true },
      );
      await executeCommand(page, 'activateFiles');
      await expect(page.locator(`${FILES_PANEL} div[title="file-${FILE_COUNT}.txt"]`)).toBeVisible();

      const results = await new AxeBuilder({ page })
        .include(FILE_LIST)
        .withRules(['target-size'])
        .analyze();

      // The scan has to have measured the targets this test is about.
      const scanned = [...results.passes, ...results.violations, ...results.incomplete]
        .flatMap((rule) => rule.nodes)
        .map((node) => node.html);
      expect(scanned.filter((html) => html.includes('type="checkbox"')), 'row checkboxes scanned')
        .toHaveLength(FILE_COUNT);
      expect(scanned.filter((html) => html.includes('role="button"')), 'sort headers scanned')
        .toHaveLength(4);

      const failing = [...results.violations, ...results.incomplete]
        .flatMap((rule) => rule.nodes)
        .map(describeNode);
      expect(failing, 'targets axe reports as violations or Needs Review').toEqual([]);
    } finally {
      await consoleActions.executeInConsole(
        `unlink(${dir}, recursive = TRUE); .rs.api.filesPaneNavigate(path.expand("~"))`,
        { wait: true },
      ).catch((cleanupErr) => console.warn('files_list_target_size cleanup failed:', cleanupErr));
    }
  });
});
