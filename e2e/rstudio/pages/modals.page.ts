import type { Page } from 'playwright';
import { expect } from '@playwright/test';
import { sleep } from '../utils/constants';

// Selectors
export const CONFIRM_BTN = '#rstudio_dlg_ok';
export const CANCEL_BTN = '#rstudio_dlg_cancel';
export const YES_BTN = '#rstudio_dlg_yes';
export const NO_BTN = '#rstudio_dlg_no';
export const RMARKDOWN_MODAL = 'div.gwt-DialogBox[aria-label="New R Markdown"]';
export const TEMPLATE_OPTION = "//span[contains(.,'From Template')]";
export const TEMPLATE_LIST = '#rstudio_new_rmd_template';
export const SPELLING_MODAL = "div.gwt-DialogBox[aria-label='Check Spelling']";

// Actions
export async function installDepIfPrompted(page: Page, detectTimeout: number = 5000, installTimeout: number = 500000): Promise<void> {
  try {
    await page.locator(YES_BTN).click({ timeout: detectTimeout });
    console.log('Clicked Yes to install dependencies');
    // Wait for interrupt button flicker to settle
    await sleep(5000);
    // Wait for interrupt button to disappear (install complete)
    await expect(page.locator("[id^='rstudio_tb_interruptr']")).not.toBeVisible({ timeout: installTimeout });
  } catch {
    // No dependency install dialog appeared
  }
}

export async function clickConfirmIfVisible(page: Page, timeout: number = 15000): Promise<void> {
  try {
    await page.locator(CONFIRM_BTN).click({ timeout });
    console.log('Clicked OK on confirmation dialog');
    await sleep(1000);
  } catch {
    // No confirmation dialog appeared
  }
}

/**
 * Dismiss any modal dialogs currently blocking the UI, returning the labels of
 * whatever was dismissed (empty array if the stack was already clear).
 *
 * A GWT modal renders a `gwt-PopupPanelGlass` overlay that intercepts pointer
 * events across the whole application. When an *out-of-band* dialog appears --
 * one the test did not open -- the next click a test makes fails with an opaque
 * "<div class=\"gwt-PopupPanelGlass\"></div> intercepts pointer events" timeout
 * that points at the click site, not the real culprit. The canonical example is
 * a spurious "Error Listing Packages" dialog that surfaces from the packages-
 * pane refresh right after a session resume: its glass then wedges the next
 * `executeInConsole`.
 *
 * Call this after operations that can surface such a dialog (session
 * suspend/resume, restart) and before the next pane interaction. It only acts
 * when a modal is actually up (checked via the automation bridge's
 * `numShowing()`), and logs what it dismissed so a stray dialog stays visible
 * in the test output even though clearing it lets the test proceed.
 */
export async function dismissBlockingModals(page: Page): Promise<string[]> {
  const dismissed = await page.evaluate(() => {
    const r = window.rstudio;
    if (!r || r.dialogs.numShowing() === 0)
      return [];

    // Capture labels before dismissing so the caller can report them. Prefer
    // the accessible name; fall back to a truncated text snippet.
    const nodes = document.querySelectorAll(
      '.gwt-DialogBox, [role="alertdialog"], [role="dialog"]',
    );
    const labels = Array.from(nodes).map((el) =>
      el.getAttribute('aria-label') ||
      (el.textContent ?? '').trim().slice(0, 120) ||
      '(unlabeled dialog)',
    );

    r.dialogs.dismissAll();
    return labels;
  });

  if (dismissed.length > 0)
    console.warn(`dismissBlockingModals: dismissed ${dismissed.length} modal(s): ${dismissed.join(' | ')}`);

  return dismissed;
}

/**
 * Dismiss the "Save workspace image to <path>?" prompt that a session quit --
 * Close Project, or a project switch -- can put up, choosing "Don't Save".
 * Resolves to true if a prompt was dismissed.
 *
 * The prompt is a race, not a preference problem. The e2e sessions all run
 * with `save_workspace: never`, but the client reaches that value only when
 * the backend pushes a SaveActionChangedEvent; until then ApplicationQuit's
 * `saveAction_` holds its SaveAction.saveAsk() default, and a quit decided in
 * that window prompts. Tests that open and close projects back to back (e.g.
 * restart-under-agent) quit sessions that are seconds old, so they land in it.
 *
 * Polls rather than waiting a fixed interval: the prompt appears only after
 * the quit's unsaved-changes round trip, and in the common no-prompt case the
 * quit proceeds instead -- which flips `window.rstudio.ready` to false (or
 * tears the execution context down entirely, in Server mode where the page
 * navigates). Either is the signal to stop looking, so a session that is not
 * going to prompt costs one poll, not the timeout.
 */
export async function dismissSaveWorkspacePrompt(page: Page, timeout: number = 15000): Promise<boolean> {
  const deadline = Date.now() + timeout;

  // The same selector set the fixtures use for the startup sweep. It isn't
  // scoped to this dialog's caption, but the only modal that can be up in
  // the moment between dispatching a quit and the quit starting is the
  // quit's own prompt.
  const dontSave = page.locator(
    "button:has-text('Don\\'t Save'), button:has-text('Do not Save'), " + NO_BTN,
  ).first();

  for (;;) {
    if (await dontSave.isVisible().catch(() => false)) {
      await dontSave.click();
      console.log('dismissSaveWorkspacePrompt: chose "Don\'t Save" on the quit prompt');
      return true;
    }

    // `ready === true` means the workbench is still up, i.e. the quit has not
    // begun; anything else (false, missing bridge, destroyed context) means it
    // has, so no prompt is coming.
    const quitStarted = await page
      .evaluate(() => window.rstudio?.ready !== true)
      .catch(() => true);
    if (quitStarted || Date.now() >= deadline)
      return false;

    await sleep(100);
  }
}
