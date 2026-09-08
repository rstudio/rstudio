import { test, expect } from '@fixtures/rstudio.fixture';
import { clearPref, dismissAllModals, executeCommand, getPref, resetLayoutZoom, setPref } from '@utils/commands';
import { executeInConsole } from '@pages/console_pane.page';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { useSuiteSandbox } from '@utils/sandbox';
import { DARK_THEME, LIGHT_THEME, expectThemeStylesheet } from '@utils/theme';
import type { Locator, Page } from 'playwright';

const PREF = 'highlight_active_tabs';
const LABEL = 'Highlight active document and pane tabs';
const FILES = ['highlight_first.R', 'highlight_second.R'];

function sourceTab(page: Page, filename: string): Locator {
  return page.locator('.gwt-TabLayoutPanelTab').filter({ has: page.getByText(filename, { exact: true }) });
}

async function expectHighlight(tab: Locator, enabled: boolean): Promise<void> {
  await expect(tab.locator('table.rstheme_tabLayoutCenter')).toHaveCSS('box-shadow', enabled ? /inset/ : 'none');
  await expect(tab.locator('.gwt-Label')).toHaveCSS('-webkit-text-stroke-width', enabled ? '0.4px' : '0px');
}

async function openBasicOptions(page: Page): Promise<Locator> {
  await executeCommand(page, 'showOptions');
  const dialog = page.getByRole('dialog', { name: 'Options', exact: true });
  await expect(dialog).toBeVisible();
  await dialog.locator('#rstudio_label_general_options').click();
  await dialog.locator('#rstudio_general_basic_prefs_tab').click();
  await expect(dialog.locator('#rstudio_general_basic_prefs_panel').getByRole('checkbox', { name: LABEL })).toBeVisible();
  return dialog;
}

async function reloadAndWait(page: Page): Promise<void> {
  await page.reload();
  await page.waitForFunction(() => window.rstudio?.ready === true, null, { timeout: 30000 });
}

test.describe.serial('Active tab highlighting preference', () => {
  const sandbox = useSuiteSandbox();
  const additionalFiles: string[] = [];
  let originalPref: boolean;
  let originalTheme: string;
  let originalGlobalTheme: string;

  test.beforeAll(async ({ rstudioPage: page }) => {
    originalPref = await getPref(page, PREF) as boolean;
    originalTheme = await getPref(page, 'editor_theme') as string;
    originalGlobalTheme = await getPref(page, 'global_theme') as string;
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await dismissAllModals(page);
    await resetLayoutZoom(page);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await setPref(page, PREF, originalPref);
    await setPref(page, 'global_theme', originalGlobalTheme);
    await executeInConsole(page, `.rs.api.applyTheme(${JSON.stringify(originalTheme)})`, { wait: true });
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [...FILES, ...additionalFiles]);
  });

  test('defaults on; Cancel preserves it; Apply and reload persist the choice', async ({ rstudioPage: page }, testInfo) => {
    await clearPref(page, PREF);
    expect(await getPref(page, PREF)).toBe(true);
    for (const filename of FILES)
      await writeAndOpenFile(page, sandbox.dir, filename, '# Active tab preference\n');

    const environment = page.locator('#rstudio_workbench_tab_environment');
    await expectHighlight(environment, true);
    await expectHighlight(sourceTab(page, FILES[1]), true);
    await expectHighlight(sourceTab(page, FILES[0]), false);

    let dialog = await openBasicOptions(page);
    const checkbox = dialog.getByRole('checkbox', { name: LABEL });
    await expect(checkbox).toBeChecked();
    await checkbox.uncheck();
    await dialog.locator('#rstudio_dlg_cancel').click();
    expect(await getPref(page, PREF)).toBe(true);
    await expectHighlight(environment, true);

    dialog = await openBasicOptions(page);
    await dialog.getByRole('checkbox', { name: LABEL }).uncheck();
    const saved = page.waitForResponse(response => response.url().endsWith('/rpc/set_user_prefs'));
    await dialog.locator('#rstudio_dlg_apply').click();
    await saved;
    await expect.poll(() => getPref(page, PREF)).toBe(false);
    await expectHighlight(environment, false);
    await expectHighlight(sourceTab(page, FILES[1]), false);
    await dialog.locator('#rstudio_preferences_confirm').click();

    await reloadAndWait(page);
    expect(await getPref(page, PREF)).toBe(false);
    await expectHighlight(environment, false);
    dialog = await openBasicOptions(page);
    await expect(dialog.getByRole('checkbox', { name: LABEL })).not.toBeChecked();
    await page.screenshot({ path: testInfo.outputPath('active-tab-preference.png') });
    await dialog.getByRole('checkbox', { name: LABEL }).check();
    await dialog.locator('#rstudio_preferences_confirm').click();
    await expectHighlight(environment, true);
  });

  test('selection preserves tab widths and filename truncation', async ({ rstudioPage: page }) => {
    await setPref(page, 'global_theme', 'default');
    await executeInConsole(page, `.rs.api.applyTheme(${JSON.stringify(LIGHT_THEME)})`, { wait: true });
    await reloadAndWait(page);
    await expectThemeStylesheet(page, 'textmate');
    await setPref(page, PREF, true);
    for (const filename of FILES)
      await writeAndOpenFile(page, sandbox.dir, filename, '# Stable tab widths\n');

    // Pick a filename that fits at regular weight but crosses the 200px cap
    // at 600 weight, so the old selection rule also changes its truncation.
    const borderlineFile = await sourceTab(page, FILES[0]).locator('.gwt-Label').evaluate(label => {
      const style = getComputedStyle(label);
      const context = document.createElement('canvas').getContext('2d')!;
      for (let length = 1; length < 100; length++) {
        for (let suffix = 0; suffix < 5; suffix++) {
          const filename = `analysis_${'m'.repeat(length)}${'i'.repeat(suffix)}.R`;
          context.font = `400 ${style.fontSize} ${style.fontFamily}`;
          const regular = context.measureText(filename).width;
          context.font = `600 ${style.fontSize} ${style.fontFamily}`;
          if (regular < 199 && context.measureText(filename).width > 200)
            return filename;
        }
      }
      throw new Error('Could not find a filename near the truncation boundary');
    });
    const longFile = `highlight_${'analysis_'.repeat(8)}.R`;
    additionalFiles.push(borderlineFile, longFile);
    for (const filename of [borderlineFile, longFile])
      await writeAndOpenFile(page, sandbox.dir, filename, '# Stable truncation\n');

    const tabMetrics = (tab: Locator) => tab.evaluate(element => {
      const label = element.querySelector('.gwt-Label') as HTMLElement;
      return {
        width: element.getBoundingClientRect().width,
        labelWidth: label.clientWidth,
        textWidth: label.scrollWidth,
      };
    });
    const filenames = [...FILES, borderlineFile, longFile];
    const widths = await Promise.all(filenames.map(filename => tabMetrics(sourceTab(page, filename))));
    expect(widths[2].textWidth).toBe(widths[2].labelWidth);
    expect(widths[3].textWidth).toBeGreaterThan(widths[3].labelWidth);
    for (const filename of filenames) {
      await sourceTab(page, filename).click();
      await expectHighlight(sourceTab(page, filename), true);
      await expect.poll(() => Promise.all(filenames.map(name => tabMetrics(sourceTab(page, name))))).toEqual(widths);
    }

    const environment = page.locator('#rstudio_workbench_tab_environment');
    const history = page.locator('#rstudio_workbench_tab_history');
    const paneWidths = await Promise.all([environment, history].map(tabMetrics));
    await history.click();
    await expectHighlight(history, true);
    expect(await Promise.all([environment, history].map(tabMetrics))).toEqual(paneWidths);
    await environment.click();
    await expectHighlight(environment, true);
    expect(await Promise.all([environment, history].map(tabMetrics))).toEqual(paneWidths);
  });

  for (const { name, editor, global, href, accent, inactiveWeight } of [
    { name: 'Modern light', editor: LIGHT_THEME, global: 'default', href: 'textmate', accent: 'rgb(52, 101, 164)', inactiveWeight: '700' },
    { name: 'Modern dark', editor: DARK_THEME, global: 'default', href: 'cobalt', accent: 'rgb(138, 180, 248)', inactiveWeight: '400' },
    { name: 'Sky', editor: LIGHT_THEME, global: 'alternate', href: 'textmate', accent: 'rgb(52, 101, 164)', inactiveWeight: '700' },
  ]) {
    test(`toggle and selection follow ${name} styling`, async ({ rstudioPage: page }, testInfo) => {
      await setPref(page, 'global_theme', global);
      await executeInConsole(page, `.rs.api.applyTheme(${JSON.stringify(editor)})`, { wait: true });
      await reloadAndWait(page);
      await expectThemeStylesheet(page, href);
      await setPref(page, PREF, true);
      for (const filename of FILES)
        await writeAndOpenFile(page, sandbox.dir, filename, '# Active tab preference\n');

      const environment = page.locator('#rstudio_workbench_tab_environment');
      const history = page.locator('#rstudio_workbench_tab_history');
      await expectHighlight(environment, true);
      await expect(environment.locator('table.rstheme_tabLayoutCenter')).toHaveCSS('box-shadow', new RegExp(accent.replace(/[()]/g, '\\$&')));
      await history.click();
      await expectHighlight(history, true);
      await expectHighlight(environment, false);
      await expect(environment.locator('.gwt-Label')).toHaveCSS('font-weight', '400');
      await sourceTab(page, FILES[0]).click();
      await expectHighlight(sourceTab(page, FILES[0]), true);
      await expectHighlight(sourceTab(page, FILES[1]), false);

      await setPref(page, PREF, false);
      await expectHighlight(history, false);
      await expectHighlight(sourceTab(page, FILES[0]), false);
      await expect(environment.locator('.gwt-Label')).toHaveCSS('font-weight', inactiveWeight);
      await setPref(page, PREF, true);
      await expectHighlight(history, true);
      await expectHighlight(sourceTab(page, FILES[0]), true);

      await page.screenshot({ path: testInfo.outputPath('active-tabs.png') });
      await executeCommand(page, 'maximizeTabSet2');
      await expect(environment).toBeHidden();
      const minimizedEnvironment = page.locator('.rstheme_minimizedWindowObject .gwt-TabLayoutPanelTab')
        .filter({ has: page.getByText('Environment', { exact: true }) });
      await expect(minimizedEnvironment).toBeVisible();
      for (const enabled of [true, false, true]) {
        await setPref(page, PREF, enabled);
        await expectHighlight(minimizedEnvironment, false);
        await expect(minimizedEnvironment.locator('.gwt-Label')).toHaveCSS('font-weight', inactiveWeight);
      }
    });
  }
});
