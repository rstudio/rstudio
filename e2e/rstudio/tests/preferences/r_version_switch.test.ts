import { test, expect } from '@fixtures/rstudio.fixture';
import { NO_BTN, YES_BTN } from '@pages/modals.page';
import {
  GENERAL_TAB,
  OPTIONS_APPLY,
  OPTIONS_OK,
  closeGlobalOptions,
  openGlobalOptions,
} from '@pages/global_options.page';
import { executeCommand, getVersion } from '@utils/commands';
import { waitForSessionRestart } from '@utils/project';
import { installedRVersions, isEmulated, isOrthogonal, majorMinor, compareVersions } from '@utils/r-installations';
import type { InstalledR } from '@utils/r-installations';
import type { Page } from 'playwright';

const R_VERSION_FIELD = '#rstudio_tbb_text_r_version';
const R_VERSION_CHANGE = '#rstudio_tbb_button_r_version';
const R_VERSION_LIST = '#rstudio_choose_r_version_list';

interface RDescription {
  path: string;
  version: string;
}

// what the desktop reports: the R in use, the R chosen in RStudio (empty
// when RStudio follows the default R), and the default R
interface RChoice {
  current: RDescription;
  chosen: RDescription;
  default: RDescription;
}

async function rChoice(page: Page): Promise<RChoice> {
  return page.evaluate(
    () =>
      new Promise<RChoice>((resolve) => {
        const desktop = (window as unknown as {
          desktop: { getRChoice(callback: (choice: RChoice) => void): void };
        }).desktop;
        desktop.getRChoice(resolve);
      }),
  );
}

// The newest installed R from a series other than the current one that the
// session can switch to without asking anything first: it runs as itself
// and natively.
function switchableR(current: string): InstalledR | undefined {
  return installedRVersions()
    .filter((entry) => majorMinor(entry.version) !== majorMinor(current))
    .filter((entry) => isOrthogonal(entry.binary) && !isEmulated(entry.binary))
    .sort((a, b) => compareVersions(b.version, a.version))[0];
}

// Pick the entry of the Choose R Version dialog whose label starts with the
// given text, and accept it with the given button.
async function chooseR(page: Page, labelPrefix: string, button: string): Promise<void> {
  const dialog = page.getByRole('dialog', { name: /^(Choose R Version|Switch R Version)$/ });
  const list = dialog.locator(R_VERSION_LIST);
  await expect(list).toBeVisible({ timeout: 60000 });

  const labels = await list.locator('option').allTextContents();
  const label = labels.find((text) => text.startsWith(labelPrefix));
  expect(label, `no entry starting with "${labelPrefix}" in ${JSON.stringify(labels)}`).toBeDefined();

  await list.selectOption({ label: label! });
  await dialog.getByRole('button', { name: button, exact: true }).click();
  await expect(dialog).toBeHidden();
}

// Switch back to the default R, when another was chosen.
async function useDefaultR(page: Page): Promise<void> {
  if (!(await rChoice(page)).chosen.path)
    return;

  await executeCommand(page, 'switchRVersion');
  await chooseR(page, 'Default: ', 'Switch');
  await waitForSessionRestart(page);
}

// Switching R relaunches the session process, which only the desktop can do,
// and needs a second installed R (found through rig), so these skip elsewhere.
// Each spec's desktop options live in its own sandbox, so the choice kept for
// later launches doesn't outlive the spec.
test.describe('switching the version of R', { tag: ['@desktop_only'] }, () => {
  // each test starts from the default R, with nothing chosen
  test.beforeEach(async ({ rstudioPage: page }) => {
    await useDefaultR(page);
  });

  // and the shared instance is left on it for the next spec
  test.afterAll(async ({ rstudioPage: page }) => {
    try {
      await useDefaultR(page);
    } catch (err) {
      console.warn('[r_version_switch] unable to switch back to the default R:', err);
    }
  });

  test('Global Options switches R and keeps the choice for later launches', async ({ rstudioPage: page }) => {
    test.setTimeout(180000);

    const current = (await getVersion(page)).r;
    const target = switchableR(current);
    test.skip(!target, 'needs rig and a second installed R version that runs as itself');

    await openGlobalOptions(page);
    await page.locator(GENERAL_TAB).click();

    // with nothing chosen, RStudio follows the default R
    const field = page.locator(R_VERSION_FIELD);
    await expect(field).toHaveValue(/^Default: R /, { timeout: 15000 });

    await page.locator(R_VERSION_CHANGE).click();
    const list = page.locator(R_VERSION_LIST);
    await expect(list).toBeVisible({ timeout: 60000 });
    await expect(list.locator('option', { hasText: / - in use$/ })).not.toHaveCount(0);

    await chooseR(page, `R ${target!.version} (`, 'OK');
    await expect(field).toHaveValue(new RegExp(`^R ${target!.version.replace(/\./g, '\\.')} \\(`));

    // applying keeps the choice and offers to restart with it now
    await page.locator(OPTIONS_OK).click();
    await expect(page.getByText(`RStudio will use R ${target!.version}`)).toBeVisible({ timeout: 60000 });
    expect((await rChoice(page)).chosen.version).toBe(target!.version);

    await page.locator(YES_BTN).click();
    await waitForSessionRestart(page);
    await expect.poll(async () => (await getVersion(page)).r).toBe(target!.version);

    const choice = await rChoice(page);
    expect(choice.current.version).toBe(target!.version);
    expect(choice.chosen.version).toBe(target!.version);
  });

  // Declining the update leaves the installation as it was (nothing on the
  // machine changes), and the choice pending in the dialog.
  test('Global Options asks again after the update a chosen R needs is declined', async ({ rstudioPage: page }) => {
    test.setTimeout(120000);

    const current = (await getVersion(page)).r;
    const target = installedRVersions()
      .filter((entry) => majorMinor(entry.version) !== majorMinor(current))
      .filter((entry) => !isOrthogonal(entry.binary) && !isEmulated(entry.binary))
      .sort((a, b) => compareVersions(b.version, a.version))[0];
    test.skip(!target, "needs a second macOS framework version of R that isn't orthogonal");

    await openGlobalOptions(page);
    await page.locator(GENERAL_TAB).click();
    await page.locator(R_VERSION_CHANGE).click();
    await chooseR(page, `R ${target!.version} (`, 'OK');

    const updatePrompt = page.getByText('This changes the installation for all users of this Mac', { exact: false });
    for (let attempt = 0; attempt < 2; attempt++) {
      await page.locator(OPTIONS_APPLY).click();
      await expect(updatePrompt).toBeVisible({ timeout: 30000 });
      await page.locator(NO_BTN).click();
      await expect(updatePrompt).toBeHidden();
    }

    // nothing was kept, and the session keeps its R
    expect((await rChoice(page)).chosen.path).toBe('');
    await closeGlobalOptions(page);
    expect((await getVersion(page)).r).toBe(current);
  });

  test('Session > Switch R Version switches R, and back to the default R', async ({ rstudioPage: page }) => {
    test.setTimeout(240000);

    const current = (await getVersion(page)).r;
    const target = switchableR(current);
    test.skip(!target, 'needs rig and a second installed R version that runs as itself');

    const defaultR = (await rChoice(page)).default;
    test.skip(!defaultR.version, 'needs a default R whose version the desktop knows');

    await executeCommand(page, 'switchRVersion');
    await chooseR(page, `R ${target!.version} (`, 'Switch');
    await waitForSessionRestart(page);
    await expect.poll(async () => (await getVersion(page)).r).toBe(target!.version);
    expect((await rChoice(page)).chosen.version).toBe(target!.version);

    // going back to the default R follows the default from then on
    await executeCommand(page, 'switchRVersion');
    await chooseR(page, 'Default: ', 'Switch');
    await waitForSessionRestart(page);
    await expect.poll(async () => (await getVersion(page)).r).toBe(defaultR.version);
    expect((await rChoice(page)).chosen.path).toBe('');
  });
});
