// Object Explorer tests.
//
// Covers #17937: expanding a list element must show that element's own
// contents even when several elements share the same name. Pre-fix, the
// backend generated access-by-name code (`x[["Passenger"]]`) for named
// children, so expanding any same-named node re-extracted (and displayed)
// the first match.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import { resetSourcePaneState } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';

// The explorer grid renders one outer row per node; the name cell carries a
// div[title="<name>"] and the value cell a div[title="<desc>"] (see
// ObjectExplorerDataGrid.NameCell / ValueCell). A node showing a given
// name/value pair is therefore the unique <tr> containing both divs -- the
// nested name-cell and value-cell tables each hold only one of them.
function nodeWithValue(page: import('@playwright/test').Page, name: string, desc: string) {
  return page.locator(`tr:has(div[title="${name}"]):has(div[title="${desc}"])`);
}

test.describe('Object Explorer', () => {
  let consoleActions: ConsolePaneActions;
  let sourcePane: SourcePane;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourcePane = new SourcePane(page);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // Mirror the data-viewer suite: tear down the explorer tab through
    // resetSourcePaneState so the Source pane never reaches zero tabs
    // (avoids the #17738 HIDE-animation race).
    await resetSourcePaneState(page);
    await expect(sourcePane.selectedTab).toContainText('Untitled', { timeout: 5000 });
  });

  // https://github.com/rstudio/rstudio/issues/17937
  test('same-named list elements expand to their own values (#17937)', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      '{ .rs.dup_names_list <- list(list(Id = 1, Age = 12), list(Id = 2, Age = 22), list(Id = 3, Age = 33)); names(.rs.dup_names_list) <- rep("Passenger", 3); View(.rs.dup_names_list) }',
    );
    try {
      // The explorer opens as a source tab named after the object, with the
      // root pre-expanded: all three Passenger rows are visible.
      await expect(sourcePane.selectedTab).toContainText('.rs.dup_names_list', {
        timeout: TIMEOUTS.fileOpen,
      });
      const passengers = page.locator('div[title="Passenger"]');
      await expect(passengers).toHaveCount(3, { timeout: TIMEOUTS.fileOpen });

      // Expand the SECOND Passenger node. The expand icon sits in the same
      // inner name-cell row as the name div, marked with the source-derived
      // data-action attribute.
      await passengers.nth(1).locator('xpath=ancestor::tr[1]')
        .locator('div[data-action="open"]').click();

      // Its children carry the second element's values. Expanding goes
      // through explorer_inspect_object with extracting code built from the
      // node's access string -- exactly the path that, pre-fix, resolved
      // "Passenger" to the first element and showed 1 / 12 here.
      await expect(nodeWithValue(page, 'Id', '2')).toHaveCount(1, { timeout: TIMEOUTS.fileOpen });
      await expect(nodeWithValue(page, 'Age', '22')).toHaveCount(1);

      // Only the one expanded node contributes children, and none of them
      // shows the first element's values.
      await expect(page.locator('div[title="Id"]')).toHaveCount(1);
      await expect(nodeWithValue(page, 'Id', '1')).toHaveCount(0);

      // Expand the THIRD Passenger node too: not positional luck.
      await passengers.nth(2).locator('xpath=ancestor::tr[1]')
        .locator('div[data-action="open"]').click();
      await expect(nodeWithValue(page, 'Id', '3')).toHaveCount(1, { timeout: TIMEOUTS.fileOpen });
      await expect(nodeWithValue(page, 'Age', '33')).toHaveCount(1);
      await expect(page.locator('div[title="Id"]')).toHaveCount(2);
    } finally {
      await consoleActions.executeInConsole('rm(".rs.dup_names_list", envir = .GlobalEnv)');
    }
  });
});
