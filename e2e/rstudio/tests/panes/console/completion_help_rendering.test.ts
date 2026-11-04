import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import {
  COMPLETION_POPUP,
  setAlwaysShowCompletionPopup,
  waitForCompletionPopup,
} from '@actions/autocomplete.actions';
import { TIMEOUTS } from '@utils/constants';

// Column help previews display cell values as text, not markup (rstudio-pro#12203).
//
// The preview describes a column with str(), which reproduces its cell values,
// so a value containing tag-like text must reach the display verbatim.

const DF = 'df_12203';
const MARKUP = '<b>markup</b>';
// Ids are assigned via ElementIds.assignElementId, which appends a counter if
// an id is already taken, so match on the prefix.
const HELP_POPUP = '[id^="rstudio_popup_completions_help"]:visible';

test.describe('Completion help rendering - rstudio-pro#12203', () => {
  let consoleActions: ConsolePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await new ConsolePaneActions(page).executeInConsole(
      `if (exists("${DF}", envir = .GlobalEnv)) rm(list = "${DF}", envir = .GlobalEnv)`,
      { wait: true },
    );
  });

  test('a cell value containing tag-like text is displayed literally', async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      `${DF} <- data.frame(col = c("ok", '${MARKUP}'), stringsAsFactors = FALSE)`,
      { wait: true },
    );

    await setAlwaysShowCompletionPopup(page, true);
    try {
      await consoleActions.typeInConsole(`${DF}$`);
      await waitForCompletionPopup(page);
      await expect(page.locator(COMPLETION_POPUP)).toBeVisible({ timeout: TIMEOUTS.fileOpen });

      // Scoped to the help popup: the console echo of the command above also
      // puts this text on the page, so a document-wide match would pass even
      // when the popup renders the value as markup.
      const helpPopup = page.locator(HELP_POPUP);
      await expect(helpPopup).toBeVisible({ timeout: TIMEOUTS.fileOpen });
      // Text, not markup -- parsed tags would leave only "markup" behind.
      await expect(helpPopup).toContainText(MARKUP, { timeout: TIMEOUTS.fileOpen });

      await page.keyboard.press('Escape');
      await page.keyboard.press('Escape');
    } finally {
      await setAlwaysShowCompletionPopup(page, false);
    }
  });
});
