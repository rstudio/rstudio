// Pane toolbars: buttons meet WCAG 2.5.8 Target Size (Minimum) (#19786).
//
// Toolbar buttons were 21px tall and as narrow as 14px, in a 22px row, and
// the Files breadcrumb's project and "..." buttons were 20px wide. axe-core's
// target-size rule reports most of these as Needs Review rather than
// violations, since the targets have tabindex=-1, so the scan fails on either
// result.

import AxeBuilder from '@axe-core/playwright';
import type { NodeResult } from 'axe-core';

import { test, expect } from '@fixtures/rstudio.fixture';

import { executeCommand } from '@utils/commands';

const ENVIRONMENT_PANEL = '#rstudio_workbench_panel_environment';
const FILES_PANEL = '#rstudio_workbench_panel_files';
const CONSOLE_PANEL = '#rstudio_workbench_panel_console';

// Buttons the scan has to have measured, as they appear in axe's selector
// or HTML for the node.
const EXPECTED_TARGETS = [
  '#rstudio_tb_loadworkspace',
  '#rstudio_mb_import_dataset',
  '#rstudio_tb_newfolder',
  'Go to directory',
  '#rstudio_tb_consoleclear',
];

// The one split-button dropdown arrow in these toolbars, beside Environment's
// refresh button. It is still 5x21; split buttons' design and target size
// are tracked separately on #19786.
const SPLIT_BUTTON_ARROW = `${ENVIRONMENT_PANEL} [id^="rstudio_mb_refresh_opts"]`;

function describeNode(node: NodeResult): string {
  const messages = [...node.any, ...node.all, ...node.none].map((check) => check.message);
  return `${node.html.replace(/\s+/g, ' ').slice(0, 120)}: ${messages.join(' | ')}`;
}

test.describe('Pane toolbar target size (#19786)', () => {
  // axe opens a page of its own to merge its results, which Desktop's CDP
  // connection to Electron refuses (Target.createTarget: Not supported).
  test('Environment, Files and Console toolbar buttons are 24px targets', { tag: '@server_only' }, async ({
    rstudioPage: page,
  }) => {
    await executeCommand(page, 'activateEnvironment');
    await executeCommand(page, 'activateFiles');
    // After an earlier test rebuilds a pane, its buttons no longer have
    // exactly these ids, only ids that start with them.
    await expect(page.locator(`${ENVIRONMENT_PANEL} [id^="rstudio_tb_loadworkspace"]`)).toBeVisible();
    await expect(page.locator(`${FILES_PANEL} [id^="rstudio_tb_newfolder"]`)).toBeVisible();

    // The file list has its own scan (files_list_target_size.test.ts).
    const results = await new AxeBuilder({ page })
      .include(ENVIRONMENT_PANEL)
      .include(FILES_PANEL)
      .include(CONSOLE_PANEL)
      .exclude(`${FILES_PANEL} [role="table"]`)
      .exclude(`${ENVIRONMENT_PANEL} [role="table"]`)
      .exclude(SPLIT_BUTTON_ARROW)
      .withRules(['target-size'])
      .analyze();

    const scanned = [...results.passes, ...results.violations, ...results.incomplete]
      .flatMap((rule) => rule.nodes)
      .map((node) => `${node.target.join(' ')} ${node.html}`);
    for (const target of EXPECTED_TARGETS) {
      expect(scanned.filter((node) => node.includes(target)), `${target} scanned`).toHaveLength(1);
    }

    const failing = [...results.violations, ...results.incomplete]
      .flatMap((rule) => rule.nodes)
      .map(describeNode);
    expect(failing, 'targets axe reports as violations or Needs Review').toEqual([]);
  });
});
