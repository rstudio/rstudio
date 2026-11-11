// Git pane > Switch branch menu: keyboard and screen reader behavior (#19555).
//
// The search box sat inside the menu, so a typed space reached the menu and
// checked out the highlighted branch, and the box inherited the separator row's
// aria-disabled. It now sits above the menu as a combobox: it passes the
// navigation keys to the menu, and its aria-activedescendant names the
// highlighted branch.

import AxeBuilder from '@axe-core/playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import type { Locator, Page } from '@playwright/test';

import { executeInConsoleChecked } from '@pages/console_pane.page';
import { ruleSetTags, violationSummary } from '@utils/a11y';
import { executeCommand, openProject } from '@utils/commands';
import { heredoc } from '@utils/heredoc';
import { closeProjectIfOpen } from '@utils/project';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';
import { expectHoldsFor } from '@utils/expect-holds';

// ElementIds.TB_GIT_BRANCH
const BRANCH_BUTTON = '#rstudio_tb_git_branch';
const SEARCH_LABEL = 'Search by branch name';
const PROJECT_NAME = 'BranchMenuDemo';

function branchPopup(page: Page): Locator {
  return page.locator('.popupContent:visible').filter({
    has: page.getByLabel(SEARCH_LABEL, { exact: true }),
  });
}

// Waits for the button to show `currentBranch`: the menu has nothing to list
// until the pane's first status refresh has completed.
async function openBranchMenu(page: Page, currentBranch: string): Promise<Locator> {
  await executeCommand(page, 'activateVcs');
  const button = page.locator(BRANCH_BUTTON);
  await expect(button).toContainText(currentBranch, { timeout: 30000 });
  const popup = branchPopup(page);
  if (await popup.count() > 0)
    await page.keyboard.press('Escape');
  await button.click();
  await expect(popup.getByRole('menuitem').first()).toBeVisible({ timeout: 10000 });
  return popup;
}

function highlightedItem(popup: Locator): Locator {
  return popup.locator('[role=menuitem].gwt-MenuItem-selected');
}

// Whether `el` lies wholly within the visible part of the menu's scroll panel.
// Throws if the menu doesn't scroll, since then the check proves nothing.
async function inScrolledView(el: Locator): Promise<boolean> {
  return el.evaluate((node) => {
    const scrolls = (el: Element) =>
      ['auto', 'scroll'].includes(getComputedStyle(el).overflowY) &&
      el.scrollHeight > el.clientHeight;
    let scroller = node.parentElement;
    while (scroller && !scrolls(scroller))
      scroller = scroller.parentElement;
    if (!scroller)
      throw new Error('the branch menu does not scroll; add branches to the test repo');
    const r = node.getBoundingClientRect();
    const c = scroller.getBoundingClientRect();
    return r.top >= c.top && r.bottom <= c.bottom;
  });
}

// Records the branches checked out, letting the checkout itself go through
function recordCheckouts(page: Page): string[] {
  const checkouts: string[] = [];
  page.on('request', (request) => {
    if (request.url().endsWith('/rpc/git_checkout'))
      checkouts.push((request.postDataJSON() as { params: string[] }).params[0]);
  });
  return checkouts;
}

test.describe.serial('Git branch menu (#19555)', () => {
  const sandbox = useSuiteSandbox();

  test.beforeAll(async ({ rstudioPage: page }) => {
    test.setTimeout(180_000);
    await closeProjectIfOpen(page);

    const projectDir = `${sandbox.dir}/${PROJECT_NAME}`.replace(/\\/g, '/');
    const rprojPath = `${projectDir}/${PROJECT_NAME}.Rproj`;
    await executeInConsoleChecked(
      page,
      heredoc`
        {
          stopifnot(dir.create(${rPathLiteral(projectDir)}, recursive = TRUE))
          writeLines("Version: 1.0", ${rPathLiteral(rprojPath)})
        }
      `,
    );

    // Local branches, plus remote-tracking refs for an "origin" heading.
    // CI runners have no global git config, hence the inline -c identity.
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
            system2("git", c(${gitC}, "branch", "alpha-one")),
            system2("git", c(${gitC}, "branch", "alpha-two")),
            system2("git", c(${gitC}, "branch", "beta")),
            system2("git", c(${gitC}, "update-ref", "refs/remotes/origin/main", "HEAD")),
            system2("git", c(${gitC}, "update-ref", "refs/remotes/origin/release", "HEAD")),
            vapply(sprintf("refs/remotes/origin/topic-%02d", 1:20), function(ref)
              system2("git", c(${gitC}, "update-ref", ref, "HEAD")), integer(1))
          )
          if (any(s != 0))
            stop("git seed failed (exit status: ", paste(s, collapse = "/"), ")")
        }
      `,
      { timeout: 60000 },
    );

    await openProject(page, rprojPath);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await closeProjectIfOpen(page);
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    if (await branchPopup(page).count() > 0)
      await page.keyboard.press('Escape');
  });

  test('a typed space edits the search; Enter checks out the highlighted branch', async ({
    rstudioPage: page,
  }) => {
    const checkouts = recordCheckouts(page);
    const popup = await openBranchMenu(page, 'main');
    const search = popup.getByRole('combobox', { name: SEARCH_LABEL });
    await expect(search).toBeFocused();

    await page.keyboard.press('ArrowDown');
    await expect(highlightedItem(popup)).toHaveCount(1);
    await page.keyboard.type(' ');
    await expectHoldsFor(500, async () => {
      expect(checkouts, 'a space typed into the search box must not check out a branch')
        .toEqual([]);
    });
    await expect(popup).toBeVisible();
    await expect(search).toHaveValue(' ');

    // Typed and entered within the search's 200ms delay: Enter acts on the
    // filtered list, whose first match it highlights
    await search.clear();
    await page.keyboard.type('alpha-t');
    await page.keyboard.press('Enter');
    await expect.poll(() => checkouts).toEqual(['alpha-two']);
    await expect(page.locator(BRANCH_BUTTON)).toContainText('alpha-two', { timeout: 30000 });

    // The checkout's progress dialog stays open after success
    await page.evaluate(() => window.rstudio?.dialogs.dismissAll());
  });

  test('a space that leaves the search unchanged keeps the highlight', async ({
    rstudioPage: page,
  }) => {
    const popup = await openBranchMenu(page, 'alpha-two');
    const search = popup.getByRole('combobox', { name: SEARCH_LABEL });
    await search.pressSequentially('alpha');
    await expect(highlightedItem(popup)).toHaveText('alpha-one');
    await page.keyboard.press('ArrowDown');
    await expect(highlightedItem(popup)).toHaveText('alpha-two');

    await page.keyboard.type(' ');
    await expectHoldsFor(1000, async () => {
      expect(await highlightedItem(popup).textContent()).toBe('alpha-two');
    });
  });

  test('the search box names the highlighted branch to screen readers', async ({
    rstudioPage: page,
  }) => {
    const popup = await openBranchMenu(page, 'alpha-two');
    const search = popup.getByRole('combobox', { name: SEARCH_LABEL });
    await expect(search).toBeFocused();
    await expect(search).toBeEnabled();
    await expect(search).toHaveAttribute('aria-expanded', 'true');
    const controls = await search.getAttribute('aria-controls');
    const menu = page.locator(`#${controls}`);
    await expect(menu).toHaveAttribute('role', 'menu');
    await expect(menu).toHaveAccessibleName('Switch branch');

    // Nothing is highlighted on opening, so screen readers announce the search box
    await expect(highlightedItem(popup)).toHaveCount(0);
    await expect(search).not.toHaveAttribute('aria-activedescendant');

    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('ArrowDown');
    const highlighted = highlightedItem(popup);
    await expect(highlighted).toHaveCount(1);
    const id = await highlighted.getAttribute('id');
    await expect(search).toHaveAttribute('aria-activedescendant', id!);

    // From nothing highlighted, Up starts at the end
    await page.keyboard.press('Escape');
    await openBranchMenu(page, 'alpha-two');
    await page.keyboard.press('ArrowUp');
    const lastId = await popup.getByRole('menuitem').last().getAttribute('id');
    await expect(search).toHaveAttribute('aria-activedescendant', lastId!);

    // Group headings are text; screen readers announce a separator as a splitter
    await expect(menu.getByText('(local branches)', { exact: true })).toBeVisible();
    await expect(menu.getByText('(Remote: origin)', { exact: true })).toBeVisible();
    await expect(menu.getByRole('separator').filter({ hasText: /\S/ })).toHaveCount(0);

    // The arrow keys skip headings, so each group's first item names its group
    const item = (name: string) => menu.getByRole('menuitem', { name, exact: true });
    await expect(item('(local branches): alpha-one')).toBeVisible();
    await expect(item('alpha-two')).toBeVisible();
    await expect(item('(Remote: origin): main')).toBeVisible();
    await expect(item('release')).toBeVisible();

    // With nothing left to highlight, the search box must not name a removed item
    await search.pressSequentially('zzz-no-such-branch');
    await expect(popup.getByText('(no branches available)')).toBeVisible();
    await expect(search).not.toHaveAttribute('aria-activedescendant');
  });

  test('moving up to a group\'s first branch scrolls its heading into view', async ({
    rstudioPage: page,
  }) => {
    const popup = await openBranchMenu(page, 'alpha-two');
    const menu = popup.getByRole('menu');
    const localHeading = menu.getByText('(local branches)', { exact: true });
    const remoteHeading = menu.getByText('(Remote: origin)', { exact: true });
    const highlightedName = () => highlightedItem(popup).getAttribute('aria-label');

    // From the end of the list, both headings have scrolled out of view
    await page.keyboard.press('End');
    await expect(highlightedItem(popup)).toHaveText('topic-20');
    expect(await inScrolledView(localHeading)).toBe(false);
    expect(await inScrolledView(remoteHeading)).toBe(false);

    // Up through the remote branches, to the first
    for (let i = 0; i < 25 && await highlightedName() !== '(Remote: origin): main'; i++)
      await page.keyboard.press('ArrowUp');
    expect(await highlightedName()).toBe('(Remote: origin): main');
    expect(await inScrolledView(remoteHeading)).toBe(true);

    // Up through the local branches, to the first
    for (let i = 0; i < 10 && await highlightedName() !== '(local branches): alpha-one'; i++)
      await page.keyboard.press('ArrowUp');
    expect(await highlightedName()).toBe('(local branches): alpha-one');
    expect(await inScrolledView(localHeading)).toBe(true);
  });

  test('hovering a group\'s first branch leaves it under the pointer', async ({
    rstudioPage: page,
  }) => {
    const popup = await openBranchMenu(page, 'alpha-two');
    const menu = popup.getByRole('menu');
    const remoteHeading = menu.getByText('(Remote: origin)', { exact: true });
    const firstRemote = menu.getByRole('menuitem', { name: '(Remote: origin): main', exact: true });

    // Scrolled, as with the mouse wheel, to put the item at the top and its heading out of view
    await firstRemote.evaluate((node) => {
      let scroller = node.parentElement;
      while (scroller && getComputedStyle(scroller).overflowY !== 'auto')
        scroller = scroller.parentElement;
      if (!scroller)
        throw new Error('no scroll panel above the menu item');
      scroller.scrollTop +=
        node.getBoundingClientRect().top - scroller.getBoundingClientRect().top;
    });
    expect(await inScrolledView(remoteHeading)).toBe(false);
    const before = (await firstRemote.boundingBox())!.y;

    // Hovering highlights the item; revealing the heading would move it from the pointer
    await firstRemote.hover();
    await expect(highlightedItem(popup)).toHaveAttribute('aria-label', '(Remote: origin): main');
    await expectHoldsFor(300, async () => {
      expect((await firstRemote.boundingBox())!.y).toBe(before);
    });
  });

  test('Shift+Home selects the search text instead of moving the highlight', async ({
    rstudioPage: page,
  }) => {
    const popup = await openBranchMenu(page, 'alpha-two');
    const search = popup.getByRole('combobox', { name: SEARCH_LABEL });
    await search.pressSequentially('beta');
    await page.keyboard.press('Shift+Home');
    expect(await search.evaluate((el: HTMLInputElement) =>
      [el.selectionStart, el.selectionEnd])).toEqual([0, 4]);
  });

  test('closing the menu returns focus to the branch button', async ({
    rstudioPage: page,
  }) => {
    const button = page.locator(BRANCH_BUTTON);
    const popup = await openBranchMenu(page, 'alpha-two');
    await page.keyboard.press('Escape');
    await expect(popup).toBeHidden();
    await expect(button).toBeFocused();

    // Tab moves on from the button, not from the start of the page
    type Marked = Window & { __pwNext?: Element | null };
    await button.focus();
    await page.keyboard.press('Tab');
    await page.evaluate(() => { (window as Marked).__pwNext = document.activeElement; });
    await openBranchMenu(page, 'alpha-two');
    await page.keyboard.press('Tab');
    await expect(popup).toBeHidden();
    expect(await page.evaluate(() =>
      document.activeElement !== document.body &&
      document.activeElement === (window as Marked).__pwNext)).toBe(true);
  });

  test('retyping a search after reopening the menu filters again', async ({
    rstudioPage: page,
  }) => {
    // A one-letter search, since typing a longer one reports each prefix as a change
    let popup = await openBranchMenu(page, 'alpha-two');
    await popup.getByRole('combobox', { name: SEARCH_LABEL }).pressSequentially('b');
    await expect(popup.getByRole('menuitem')).toHaveText(['beta']);
    await page.keyboard.press('Escape');

    popup = await openBranchMenu(page, 'alpha-two');
    const search = popup.getByRole('combobox', { name: SEARCH_LABEL });
    await expect(search).toHaveValue('');
    await search.pressSequentially('b');
    await expect(popup.getByRole('menuitem')).toHaveText(['beta']);
  });

  // axe opens a page of its own to merge its results, which Desktop's CDP
  // connection to Electron refuses (Target.createTarget: Not supported).
  test('the menu has no accessibility violations', { tag: '@server_only' }, async ({
    rstudioPage: page,
  }) => {
    const popup = await openBranchMenu(page, 'alpha-two');
    await page.keyboard.press('ArrowDown');
    await expect(highlightedItem(popup)).toHaveCount(1);

    // The whole popup, so the scan covers the search box as well as the menu
    await popup.evaluate((el) => el.setAttribute('data-pw-axe', 'branch-menu'));
    const results = await new AxeBuilder({ page })
      .include('[data-pw-axe=branch-menu]')
      .withTags(ruleSetTags({ level: 'AA', version: '2.2' }))
      .analyze();
    expect(violationSummary(results)).toEqual([]);
  });
});
