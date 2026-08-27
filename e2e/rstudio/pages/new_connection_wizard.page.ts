/**
 * Page object for the New Connection wizard (a plain GWT modal dialog; only
 * the Shiny-type connection page hosts an iframe, which this suite does not
 * drive yet).
 *
 * Selector notes:
 *  - Each connection type's list entry has a deterministic id assigned with
 *    a raw setId (no uniqueness suffix): see wizardPageId in
 *    utils/connections.ts.
 *  - The OK button id likewise comes from a raw setId. Cancel goes through
 *    assignElementId, hence the prefix form.
 *  - The wizard's Back control is a GWT Label with role=button and text
 *    "Back", no id.
 *  - Snippet parameter fields are FormLabel+TextBox pairs wired with
 *    setFor, so getByLabel("<Key>:") resolves them. A snippet that gives two
 *    placeholders the same order number renders both on one row (the
 *    two-field first row); that changes layout, not labeling, so getByLabel
 *    still works.
 */

import { Locator, Page } from '@playwright/test';
import { PageObject } from './page_object_base_classes';
import { wizardPageId } from '../utils/connections';
import { AceEditorElement } from '../utils/ace';

export const NEW_CONNECTION_DIALOG = '.gwt-DialogBox[aria-label="New Connection"]';
export const WIZARD_OK_BTN = '#rstudio_label_ok_wizard_confirm';
export const WIZARD_CANCEL_BTN = "[id^='rstudio_dlg_cancel']";
export const TEST_RESULTS_DIALOG = '.gwt-DialogBox[aria-label="Test Results"]';

export class NewConnectionWizard extends PageObject {
  public dialog: Locator;
  public okBtn: Locator;
  public cancelBtn: Locator;
  public backBtn: Locator;
  public testBtn: Locator;
  /** Ace editor holding the generated connection code. */
  public codePreview: Locator;
  /** The "Connect from:" <select> (R Console, New R Script, ...). */
  public connectFromDropdown: Locator;
  public testResultsDialog: Locator;

  constructor(page: Page) {
    super(page);
    this.dialog = page.locator(NEW_CONNECTION_DIALOG);
    this.okBtn = this.dialog.locator(WIZARD_OK_BTN);
    this.cancelBtn = this.dialog.locator(WIZARD_CANCEL_BTN);
    this.backBtn = this.dialog.getByRole('button', { name: 'Back', exact: true });
    this.testBtn = this.dialog.getByRole('button', { name: 'Test', exact: true });
    this.codePreview = this.dialog.locator('.ace_content');
    // The wizard pre-builds one page per connection type up front
    // (Wizard.addAndInitializePage), so every driver's code panel and
    // dropdown already exist in the DOM -- only the active page's is
    // visible. Anchor on visibility; it is the panel's only <select>.
    this.connectFromDropdown = this.dialog.locator('select:visible').last();
    this.testResultsDialog = page.locator(TEST_RESULTS_DIALOG);
  }

  /**
   * A connection type's entry in the wizard's first-page list. The
   * deterministic entry id alone can collide: id derivation lowercases, so
   * two catalog entries differing only in case (e.g. a registered "MySQL"
   * driver beside the professional-driver catalog's "Mysql" installer)
   * share one id. Intersect the id with the exact visible name.
   */
  typeEntry(connectionName: string): Locator {
    return this.dialog
      .getByRole('button', { name: connectionName, exact: true })
      .and(this.dialog.locator(wizardPageId(connectionName)));
  }

  /**
   * A snippet parameter field by its label key (e.g. "Database"). Fields are
   * FormLabel+TextBox pairs wired with setFor, resolvable by accessible
   * name -- except the secondary field of a two-field first row (e.g. Port
   * beside Server), whose label carries no association; for that one, fall
   * back to the input in the table cell after the label's cell.
   */
  field(key: string): Locator {
    const byLabel = this.dialog.getByLabel(`${key}:`, { exact: true });
    const byCell = this.dialog.locator(
      `xpath=.//td[normalize-space(.) = "${key}:"]/following-sibling::td[1]//input`,
    );
    // Visible only: every driver's parameter grid already exists in the DOM
    // (see connectFromDropdown for why), so an unfiltered first() match can
    // land in an inactive one -- typing then goes to whichever field
    // occupies that position there, silently corrupting a different
    // parameter.
    return byLabel.or(byCell).filter({ visible: true }).first();
  }

  /**
   * The generated connection code shown in the dialog's Ace preview. Only
   * the visible editor holds the current code (see connectFromDropdown for
   * the code panel's churn), and it is read through the Ace API: innerText
   * on Ace markup returns just the rendered viewport rows, which silently
   * truncates a snippet taller than the preview.
   */
  async previewCode(): Promise<string> {
    const editor = await this.dialog.locator('.ace_editor:visible').last().elementHandle();
    const code = await this.page.evaluate(
      (el) => (el as AceEditorElement).env?.editor?.getValue() ?? '',
      editor,
    );
    return code.trim();
  }
}
