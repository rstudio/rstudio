// The Git pane's branch menu lists the repository's worktrees and lets one be
// opened as a project, and More > New Worktree... checks a branch out into a
// new worktree. The repo and its worktree are built R-side so this also works
// in Server mode, where the rsession may not share a filesystem with the test
// runner.
import { test, expect } from '@fixtures/rstudio.fixture';
import type { Page } from '@playwright/test';
import { CONSOLE_OUTPUT, executeInConsole } from '@pages/console_pane.page';
import { CONFIRM_BTN } from '@pages/modals.page';
import { executeCommand, openProject } from '@utils/commands';
import { heredoc } from '@utils/heredoc';
import { closeProjectIfOpen } from '@utils/project';
import { useSuiteSandbox } from '@utils/sandbox';
import { rPathLiteral } from '@utils/r';

// -- Selectors ----------------------------------------------------------------

// The branch dropdown in the Git pane toolbar (ElementIds.TB_GIT_BRANCH), and
// its twin in the Review Changes window (ElementIds.TB_GIT_REVIEW_BRANCH).
const BRANCH_BUTTON = '#rstudio_tb_git_branch';
const REVIEW_BRANCH_BUTTON = '#rstudio_tb_git_review_branch';

// The project menu button at the top right, whose label is the project name.
const PROJECT_MENU = '#rstudio_project_menubutton_toolbar';

// New Worktree dialog inputs (ElementIds.NEW_WORKTREE_*).
const WORKTREE_BRANCH_INPUT = '#rstudio_new_worktree_branch';
const WORKTREE_DIRECTORY_INPUT = '#rstudio_new_worktree_directory';

// New Branch button and dialog (ElementIds.TB_GIT_NEW_BRANCH, NEW_BRANCH_*).
// GWT renders a CheckBox as a span around the input, so the id lands on the span.
const NEW_BRANCH_BUTTON = '#rstudio_tb_git_new_branch';
const NEW_BRANCH_NAME_INPUT = '#rstudio_new_branch_name';
const NEW_BRANCH_WORKTREE_CHECKBOX = '#rstudio_new_branch_worktree input';
// TextBoxWithButton ids are a fixed prefix plus the TextBoxButtonId value.
const NEW_BRANCH_WORKTREE_PARENT_INPUT = 'input[id$="_new_branch_worktree_parent"]';

// Remove Worktree dialog (ElementIds.REMOVE_WORKTREE_*).
const REMOVE_WORKTREE_SELECT = '#rstudio_remove_worktree_select';

const PROJECT_NAME = 'WorktreeDemo';
const LINKED_WORKTREE = 'WorktreeDemo-feature';
const LINKED_BRANCH = 'feature/a';
const NEW_BRANCH = 'feature/new-worktree';
const BRANCH_DIALOG_BRANCH = 'feature/from-branch-dialog';

/**
 * executeInConsole only confirms that R reached a new prompt, which an R error
 * does just as readily as a success; scrape the output this command produced
 * for an error header so a failed setup step fails here, not as a later
 * unrelated-looking timeout.
 */
async function executeInConsoleChecked(
  page: Page,
  command: string,
  opts: { timeout?: number } = {},
): Promise<void> {
  const before = await page.locator(CONSOLE_OUTPUT).innerText();
  await executeInConsole(page, command, { wait: true, timeout: opts.timeout });
  const after = await page.locator(CONSOLE_OUTPUT).innerText();

  const emitted = after.startsWith(before) ? after.slice(before.length) : after;
  const errMatch = emitted.match(/(?:Error in|Error:)[^\n]*(?:\n[ \t]+[^\n]*)*/);
  if (errMatch) {
    throw new Error(
      `R error running "${command.slice(0, 120)}": ${errMatch[0].replace(/\n+/g, ' | ').trim()}`,
    );
  }
}

/**
 * Open the Git pane's branch menu and return its item texts. Waits for the
 * button to show `currentBranch` first: the pane refreshes its status when
 * selected, and the menu has nothing to list until that has completed.
 */
async function openBranchMenu(page: Page, currentBranch: string): Promise<string[]> {
  await executeCommand(page, 'activateVcs');
  const button = page.locator(BRANCH_BUTTON);
  await expect(button).toContainText(currentBranch, { timeout: 30000 });
  await button.click();
  const items = menuItems(page);
  await expect(items.first()).toBeVisible({ timeout: 10000 });

  // innerText keeps the label/path layout whitespace; collapse it so items
  // can be compared as single lines
  const texts = (await items.allInnerTexts()).map((text) => text.replace(/\s+/g, ' ').trim());
  console.log(`[worktrees] branch menu: ${JSON.stringify(texts)}`);
  return texts;
}

// Items of the open branch popup; it is the only menu open while the test
// reads it (the top menubar's entries are menuitems too, but not in a menu).
function menuItems(page: Page) {
  return page.getByRole('menu').getByRole('menuitem');
}

test.describe.serial('Git pane worktrees', () => {
  const sandbox = useSuiteSandbox();

  test.beforeAll(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page).catch((err) => {
      console.warn(`[worktrees] project close failed: ${(err as Error).message}`);
    });
  });

  test('lists worktrees and opens one as a project', async ({ rstudioPage: page }) => {
    test.setTimeout(180_000);

    const projectDir = `${sandbox.dir}/${PROJECT_NAME}`.replace(/\\/g, '/');
    const linkedDir = `${sandbox.dir}/${LINKED_WORKTREE}`.replace(/\\/g, '/');
    const rprojPath = `${projectDir}/${PROJECT_NAME}.Rproj`;

    // -- a repo with one commit (so the .Rproj is in every worktree), one
    //    linked worktree on its own branch, and a spare local branch --
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          stopifnot(dir.create(${rPathLiteral(projectDir)}, recursive = TRUE))
          writeLines("Version: 1.0", ${rPathLiteral(rprojPath)})
          writeLines("x <- 1", ${rPathLiteral(`${projectDir}/script.R`)})
        }
      `,
    );

    // CI runners have no global git config, hence the inline -c identity
    const gitC = `"-C", shQuote(${rPathLiteral(projectDir)})`;
    const gitWho = `"-c", "user.name=rstudio-e2e", "-c", "user.email=rstudio-e2e@posit.co"`;
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          s <- c(
            system2("git", c(${gitC}, "init", "--quiet")),
            system2("git", c(${gitC}, "checkout", "--quiet", "-B", "main")),
            system2("git", c(${gitC}, "add", "-A")),
            system2("git", c(${gitC}, ${gitWho}, "commit", "-m", "seed", "--quiet")),
            system2("git", c(${gitC}, "worktree", "add", "--quiet", shQuote(${rPathLiteral(linkedDir)}), "-b", ${JSON.stringify(LINKED_BRANCH)})),
            system2("git", c(${gitC}, "branch", "extra-local"))
          )
          if (any(s != 0))
            stop("git seed failed (exit status: ", paste(s, collapse = "/"), ")")
        }
      `,
      { timeout: 60000 },
    );

    await openProject(page, rprojPath);

    // -- the menu has a worktree section: the current worktree, and the linked
    //    one with its path; the linked branch is routed to its worktree rather
    //    than offered for checkout. Paths are matched by their last segment:
    //    the session reports resolved paths (/private/var/... on macOS) where
    //    the sandbox path goes through the symlink. --
    const items = await openBranchMenu(page, 'main');
    expect(items.some((text) => text.startsWith('main') && text.endsWith(`/${PROJECT_NAME}`))).toBe(true);
    expect(items.some((text) => text.startsWith(LINKED_BRANCH) && text.endsWith(`/${LINKED_WORKTREE}`))).toBe(true);
    expect(items).toContain(`${LINKED_BRANCH} (worktree)`);
    expect(items).toContain('extra-local');
    expect(items).not.toContain(LINKED_BRANCH);

    // -- selecting the linked worktree opens it as a project --
    await menuItems(page).filter({ hasText: new RegExp(`/${LINKED_WORKTREE}$`) }).click();

    // The switch restarts the session (and, in Server mode, reloads the page),
    // so the old page's bridge can still answer for a while: poll for the new
    // project path rather than for readiness, tolerating the bridge being
    // absent or the execution context being torn down mid-navigation.
    await expect
      .poll(
        () =>
          page
            .evaluate(() => window.rstudio?.project?.path?.() ?? null)
            .catch(() => null),
        { timeout: 60000 },
      )
      .toContain(LINKED_WORKTREE);
    await page.waitForFunction(() => window.rstudio?.ready === true, null, {
      timeout: 60000,
      polling: 100,
    });
    // the project label keeps the primary checkout's name and adds the worktree
    await expect(page.locator(PROJECT_MENU)).toContainText(
      `${PROJECT_NAME} [${LINKED_WORKTREE}]`,
      { timeout: 30000 },
    );

    // ...and the recent projects list shows that label on its own rather than
    // prefixing it with the directory name again
    await page.locator(PROJECT_MENU).click();
    await expect
      .poll(
        async () =>
          (await menuItems(page).allInnerTexts()).map((text) => text.replace(/\s+/g, ' ').trim()),
        { timeout: 10000 },
      )
      .toContain(`${PROJECT_NAME} [${LINKED_WORKTREE}]`);
    const projectMenuItems = await menuItems(page).allInnerTexts();
    expect(projectMenuItems.some((text) => text.startsWith(`${LINKED_WORKTREE} (`))).toBe(false);
    await page.keyboard.press('Escape');

    // from here the main worktree is the "other" one
    const itemsAfter = await openBranchMenu(page, LINKED_BRANCH);
    expect(itemsAfter.some((text) => text.startsWith('main') && text.endsWith(`/${PROJECT_NAME}`))).toBe(true);
    expect(itemsAfter).toContain('main (worktree)');
    await page.keyboard.press('Escape');
  });

  test('creates a new worktree from the dialog', async ({ rstudioPage: page }) => {
    test.setTimeout(120_000);

    await executeCommand(page, 'vcsNewWorktree');
    const branchInput = page.locator(WORKTREE_BRANCH_INPUT);
    await expect(branchInput).toBeVisible({ timeout: 10000 });

    // the directory name follows the branch name, with path separators
    // flattened; the parent defaults to the main worktree's parent, which is
    // the sandbox here
    await branchInput.pressSequentially(NEW_BRANCH);
    await expect(page.locator(WORKTREE_DIRECTORY_INPUT)).toHaveValue('feature-new-worktree');
    await page.locator(CONFIRM_BTN).click();

    // success offers to open the new worktree; decline and verify instead
    const prompt = page.getByRole('alertdialog', { name: 'New Worktree' }).filter({ hasText: 'Open it as a project' });
    await expect(prompt).toBeVisible({ timeout: 60000 });
    await prompt.getByRole('button', { name: 'No' }).click();

    const items = await openBranchMenu(page, LINKED_BRANCH);
    expect(items.some((text) => text.startsWith(NEW_BRANCH) && text.endsWith('/feature-new-worktree'))).toBe(true);
    expect(items).toContain(`${NEW_BRANCH} (worktree)`);
    await page.keyboard.press('Escape');
  });

  test('creates a worktree from the New Branch dialog', async ({ rstudioPage: page }) => {
    test.setTimeout(120_000);

    await executeCommand(page, 'activateVcs');
    await page.locator(NEW_BRANCH_BUTTON).click();
    const branchInput = page.locator(NEW_BRANCH_NAME_INPUT);
    await expect(branchInput).toBeVisible({ timeout: 10000 });
    await branchInput.pressSequentially(BRANCH_DIALOG_BRANCH);

    // the worktree option reveals the parent directory, which defaults to the
    // directory holding the repository's linked worktrees (the sandbox)
    await page.locator(NEW_BRANCH_WORKTREE_CHECKBOX).check();
    await expect(page.locator(NEW_BRANCH_WORKTREE_PARENT_INPUT)).toBeVisible();
    await page.locator(CONFIRM_BTN).click();

    const prompt = page.getByRole('alertdialog', { name: 'New Worktree' }).filter({ hasText: 'Open it as a project' });
    await expect(prompt).toBeVisible({ timeout: 60000 });
    await prompt.getByRole('button', { name: 'No' }).click();

    // this window is still on the linked worktree's branch; the new branch
    // lives in its own worktree rather than having been checked out here
    const items = await openBranchMenu(page, LINKED_BRANCH);
    expect(items.some((text) => text.startsWith(BRANCH_DIALOG_BRANCH) && text.endsWith('/feature-from-branch-dialog'))).toBe(true);
    expect(items).toContain(`${BRANCH_DIALOG_BRANCH} (worktree)`);
    await page.keyboard.press('Escape');
  });

  test('removes a worktree from the dialog', async ({ rstudioPage: page }) => {
    test.setTimeout(120_000);

    await executeCommand(page, 'vcsRemoveWorktree');
    const select = page.locator(REMOVE_WORKTREE_SELECT);
    await expect(select).toBeVisible({ timeout: 10000 });

    // options are keyed by path; pick the worktree the New Worktree dialog made
    const value = await select.locator('option').evaluateAll(
      (options) => (options as HTMLOptionElement[]).map((o) => o.value).find((v) => v.endsWith('/feature-new-worktree')),
    );
    expect(value).toBeTruthy();
    await select.selectOption(value as string);
    await page.locator(CONFIRM_BTN).click();

    const confirm = page.getByRole('alertdialog', { name: 'Remove Worktree' });
    await expect(confirm).toBeVisible({ timeout: 10000 });
    await confirm.getByRole('button', { name: 'Yes' }).click();

    // The progress dialog closes itself once git exits successfully. Wait for
    // that before touching the keyboard: Escape on that dialog means Stop, and
    // would interrupt the removal.
    await expect(page.getByRole('dialog', { name: 'Git Worktree Remove' })).toBeHidden({ timeout: 60000 });

    // the menu drops the worktree once the status refresh has run; the branch
    // itself survives as a plain branch
    await expect
      .poll(
        async () => {
          await page.keyboard.press('Escape');
          return openBranchMenu(page, LINKED_BRANCH);
        },
        { timeout: 60000 },
      )
      .not.toContain(`${NEW_BRANCH} (worktree)`);
    const items = await menuItems(page).allInnerTexts();
    expect(items.map((text) => text.replace(/\s+/g, ' ').trim())).toContain(NEW_BRANCH);
    expect(items.some((text) => text.endsWith('/feature-new-worktree'))).toBe(false);
    await page.keyboard.press('Escape');
  });

  test('opens a worktree from the Review Changes window', async ({ rstudioPage: page }) => {
    test.setTimeout(180_000);

    // The Review Changes window has the same branch menu, but only the main
    // window can switch projects, so the satellite hands the request over to
    // it. Still on the linked worktree here; pick the main one.
    const satellitePromise = page.context().waitForEvent('page', { timeout: 60000 });
    await executeCommand(page, 'vcsCommit');
    const satellite = await satellitePromise;
    try {
      await satellite.waitForLoadState('domcontentloaded');
      expect(satellite.url()).toContain('view=review_changes');

      const button = satellite.locator(REVIEW_BRANCH_BUTTON);
      await expect(button).toContainText(LINKED_BRANCH, { timeout: 60000 });
      await button.click();
      const mainWorktree = menuItems(satellite).filter({ hasText: new RegExp(`/${PROJECT_NAME}$`) });
      await expect(mainWorktree).toBeVisible({ timeout: 10000 });
      await mainWorktree.click();

      // the main window switches project (see the first test for why this
      // polls through the reload rather than waiting for readiness)
      await expect
        .poll(
          () =>
            page
              .evaluate(() => window.rstudio?.project?.path?.() ?? null)
              .catch(() => null),
          { timeout: 60000 },
        )
        .toMatch(new RegExp(`/${PROJECT_NAME}(/|$)`));
      await page.waitForFunction(() => window.rstudio?.ready === true, null, {
        timeout: 60000,
        polling: 100,
      });
      await expect(page.locator(PROJECT_MENU)).toContainText(PROJECT_NAME, { timeout: 30000 });
      await expect(page.locator(PROJECT_MENU)).not.toContainText('[');
    } finally {
      await satellite.close().catch(() => {});
    }
  });
});
