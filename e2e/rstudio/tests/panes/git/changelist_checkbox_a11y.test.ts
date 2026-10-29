// Regression test for issue #19328: the Git pane's Staged column was drawn as a
// bare <img alt="">, so screen readers skipped it. Each cell must now be exposed
// as a checkbox named for its file, with aria-checked conveying staged, unstaged,
// and partially staged ("mixed") files, and must stay operable by click. The
// Status column's icons are likewise named by their status description.
import { test, expect } from '@fixtures/rstudio.fixture';
import { executeInConsole } from '@pages/console_pane.page';
import { executeCommand, openProject } from '@utils/commands';
import { heredoc } from '@utils/heredoc';
import { closeProjectIfOpen } from '@utils/project';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';

const GIT_PANEL = '#rstudio_workbench_panel_git';
const PROJECT_NAME = 'GitChangelistA11y';

test.describe('Git changelist checkboxes (#19328)', () => {
  const sandbox = useSuiteSandbox();

  test.beforeAll(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page).catch((err) => {
      console.warn(`[changelist_checkbox_a11y] project close failed: ${(err as Error).message}`);
    });
  });

  test('staged column exposes named tri-state checkboxes', async ({ rstudioPage: page }) => {
    test.setTimeout(180_000);

    const projectDir = `${sandbox.dir}/${PROJECT_NAME}`.replace(/\\/g, '/');
    const rprojPath = `${projectDir}/${PROJECT_NAME}.Rproj`;
    const gitC = `"-C", shQuote(${rPathLiteral(projectDir)})`;
    const gitWho = `"-c", "user.name=rstudio-e2e", "-c", "user.email=rstudio-e2e@posit.co"`;

    // One file in each state: partial.R is staged and then modified again
    // ("MM"), staged.R is newly added ("A "), untracked.R is untracked ("??").
    // All file and git work goes through the R console so this also works in
    // Server mode, where the rsession may not share the runner's filesystem.
    await executeInConsole(
      page,
      heredoc`
        {
          stopifnot(dir.create(${rPathLiteral(projectDir)}, recursive = TRUE))
          setwd(${rPathLiteral(projectDir)})
          writeLines("Version: 1.0", ${rPathLiteral(rprojPath)})
          writeLines("x <- 1", "partial.R")
          s <- c(
            system2("git", c(${gitC}, "init", "--quiet")),
            system2("git", c(${gitC}, "add", "-A")),
            system2("git", c(${gitC}, ${gitWho}, "commit", "-m", "seed", "--quiet"))
          )
          writeLines("x <- 2", "partial.R")
          writeLines("y <- 1", "staged.R")
          s <- c(s, system2("git", c(${gitC}, "add", "partial.R", "staged.R")))
          writeLines("x <- 3", "partial.R")
          writeLines("z <- 1", "untracked.R")
          if (any(s != 0))
            stop("git seed failed (exit status: ", paste(s, collapse = "/"), ")")
        }
      `,
      { wait: true, timeout: 60000 },
    );

    await openProject(page, rprojPath);
    await executeCommand(page, 'activateVcs');
    const panel = page.locator(GIT_PANEL);
    await expect(panel).toBeVisible({ timeout: 15000 });

    const partial = panel.getByRole('checkbox', { name: 'Stage partial.R', exact: true });
    const staged = panel.getByRole('checkbox', { name: 'Stage staged.R', exact: true });
    const untracked = panel.getByRole('checkbox', { name: 'Stage untracked.R', exact: true });

    await expect(partial).toHaveAttribute('aria-checked', 'mixed', { timeout: 30000 });
    await expect(staged).toHaveAttribute('aria-checked', 'true');
    await expect(untracked).toHaveAttribute('aria-checked', 'false');

    // The status icons have empty alt text; their description is the name.
    // A has: locator resolves inside the row, so it must not be panel-rooted.
    const statusOf = (file: string) =>
      panel
        .getByRole('row')
        .filter({ has: page.getByRole('checkbox', { name: `Stage ${file}`, exact: true }) })
        .getByRole('img');
    await expect(statusOf('partial.R')).toHaveAccessibleName('Modified/Modified');
    await expect(statusOf('staged.R')).toHaveAccessibleName('Added');
    await expect(statusOf('untracked.R')).toHaveAccessibleName('Untracked');

    // Clicking stages the file, and the re-rendered cell reports the new state.
    await untracked.click();
    await expect(untracked).toHaveAttribute('aria-checked', 'true', { timeout: 30000 });
  });
});
