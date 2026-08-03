/**
 * Higher-level Connections pane operations composing the pane and wizard
 * page objects: drive the New Connection wizard end to end, seed the
 * throwaway database through DBI, and walk the connection explorer.
 */

import { Page } from '@playwright/test';
import { ConnectionsPane } from '../pages/connections_pane.page';
import { NewConnectionWizard } from '../pages/new_connection_wizard.page';
import { ConsolePaneActions } from './console_pane.actions';
import { YES_BTN } from '../pages/modals.page';
import { executeCommand } from '../utils/commands';
import { rStringLiteral } from '../utils/r';
import { EffectiveDbTarget } from '../utils/db-targets';

export class ConnectionsPaneActions {
  readonly page: Page;
  readonly pane: ConnectionsPane;
  readonly wizard: NewConnectionWizard;
  private readonly consoleActions: ConsolePaneActions;

  constructor(page: Page) {
    this.page = page;
    this.pane = new ConnectionsPane(page);
    this.wizard = new NewConnectionWizard(page);
    this.consoleActions = new ConsolePaneActions(page);
  }

  async activatePane(): Promise<void> {
    // Bridge command instead of clicking the tab: avoids actionability
    // failures when another element overlaps the tab in full-suite runs.
    await executeCommand(this.page, 'activateConnections');
  }

  /**
   * Open the New Connection wizard and wait for its type list. The first
   * open of a session also triggers the installer-catalog refresh RPC, so
   * the deadline is generous.
   */
  async openWizard(): Promise<void> {
    await executeCommand(this.page, 'newConnection');
    await this.wizard.dialog.waitFor({ state: 'visible', timeout: 20000 });
  }

  /**
   * Pick a connection type and fill its labeled parameter fields.
   * pressSequentially, not fill: the wizard's GWT text boxes update the
   * snippet preview through per-keystroke handlers.
   */
  async fillWizardForTarget(target: EffectiveDbTarget): Promise<void> {
    await this.wizard.typeEntry(target.driverName).click();
    for (const [key, value] of Object.entries(target.wizardFields)) {
      const field = this.wizard.field(key);
      await field.waitFor({ state: 'visible', timeout: 10000 });
      await field.pressSequentially(value);
    }
  }

  /**
   * Click Test and report whether the wizard's Test Results dialog announced
   * success. Dismisses the results dialog either way.
   */
  async testConnection(): Promise<boolean> {
    await this.wizard.testBtn.click();
    await this.wizard.testResultsDialog.waitFor({ state: 'visible', timeout: 30000 });
    const text = (await this.wizard.testResultsDialog.innerText()) ?? '';
    await this.wizard.testResultsDialog.locator("[id^='rstudio_dlg_ok']").click();
    await this.wizard.testResultsDialog.waitFor({ state: 'hidden', timeout: 5000 });
    return text.includes('Success!');
  }

  /**
   * Confirm the wizard (Connect from: R Console is the product default) and
   * wait until the pane reports the connection as being explored: the
   * disconnect toolbar button only exists in that state.
   */
  async confirmWizardAndWaitConnected(): Promise<void> {
    await this.wizard.okBtn.click();
    await this.wizard.dialog.waitFor({ state: 'hidden', timeout: 10000 });
    await this.pane.disconnectBtn.waitFor({ state: 'visible', timeout: 30000 });
  }

  /**
   * Seed the target database through DBI from the rsession, invisibly to the
   * pane: the odbc package announces connections through the
   * connectionObserver option, so nulling it for the duration keeps this
   * setup connection out of the UI under test. Returns false when any part
   * of the seeding errored.
   */
  async seedDatabase(target: EffectiveDbTarget): Promise<boolean> {
    const sqlVector = target.seedSql.map((s) => rStringLiteral(s)).join(', ');
    const expr =
      'local({ ' +
      'op <- options(connectionObserver = NULL); on.exit(options(op), add = TRUE); ' +
      'con <- DBI::dbConnect(odbc::odbc(), ' +
      `Driver = ${rStringLiteral(target.driverName)}, ` +
      `Server = ${rStringLiteral(target.host)}, ` +
      `Port = ${target.port}, ` +
      `Database = ${rStringLiteral(target.database)}, ` +
      `UID = ${rStringLiteral(target.user)}, ` +
      `PWD = ${rStringLiteral(target.password)}, timeout = 10); ` +
      'on.exit(DBI::dbDisconnect(con), add = TRUE); ' +
      `for (sql in c(${sqlVector})) DBI::dbExecute(con, sql); ` +
      'TRUE })';
    return (await this.consoleActions.evalRLogical(expr)) === true;
  }

  /**
   * Disconnect via the toolbar, answering the confirmation prompt, then
   * return to the connection list. The pane stays in the explorer view
   * after a disconnect, and the RStudio session persists across spec files
   * within a worker, so leaving the explorer up would strand the next spec
   * in front of a toolbar with no New Connection button.
   */
  async disconnect(): Promise<void> {
    await this.pane.disconnectBtn.click();
    const yes = this.page.locator(YES_BTN);
    await yes.waitFor({ state: 'visible', timeout: 10000 });
    await yes.click();
    await this.pane.panel.getByText('(Not connected)').waitFor({ state: 'visible', timeout: 15000 });
    await this.pane.backToConnectionsBtn.click();
    await this.pane.newConnectionBtn.waitFor({ state: 'visible', timeout: 10000 });
  }

  /** Open the explorer for a listed connection row. */
  async exploreConnection(rowText: string): Promise<void> {
    await this.pane.exploreButton(rowText).first().click();
    await this.pane.backToConnectionsBtn.waitFor({ state: 'visible', timeout: 10000 });
  }

  /**
   * Reconnect the currently explored (disconnected) connection by running
   * its stored code from the explorer toolbar's Connect menu.
   */
  async reconnectExplored(): Promise<void> {
    await this.pane.connectMenuButton.click();
    await this.page.getByRole('menuitem', { name: 'R Console' }).click();
    await this.pane.disconnectBtn.waitFor({ state: 'visible', timeout: 30000 });
  }

  /**
   * Remove the currently explored, disconnected connection (Remove is only
   * offered in that state), answering the confirmation; lands back on the
   * connection list.
   */
  async removeExploredConnection(): Promise<void> {
    await this.pane.removeConnectionBtn.click();
    const yes = this.page.locator(YES_BTN);
    await yes.waitFor({ state: 'visible', timeout: 10000 });
    await yes.click();
    await this.pane.newConnectionBtn.waitFor({ state: 'visible', timeout: 15000 });
  }

  /**
   * Remove every listed connection whose row matches the text. Connections
   * from different engines to the same database share a display name
   * ("pwtest - pwtest@127.0.0.1"), so tests that must select a specific
   * row start from a clean list instead of guessing.
   */
  async removeMatchingConnections(rowText: string): Promise<void> {
    while (await this.pane.exploreButton(rowText).count()) {
      await this.exploreConnection(rowText);
      await this.removeExploredConnection();
    }
  }

  /**
   * Expand object-tree containers from the explorer root down to the
   * target's seeded table, waiting for each next level to render before
   * toggling deeper. For catalog-rooted engines (PostgreSQL) the first
   * expansion is the database node itself.
   */
  async drillExplorer(target: EffectiveDbTarget): Promise<void> {
    const path = target.explorerRootIsCatalog
      ? [target.database, ...target.explorerPath]
      : target.explorerPath;
    for (const node of path) {
      const toggle = this.pane.expandToggle(node);
      await toggle.first().waitFor({ state: 'visible', timeout: 15000 });
      await toggle.first().click();
    }
  }
}
