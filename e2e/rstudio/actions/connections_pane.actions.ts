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
   * Pick a connection type and fill its labeled parameter fields, then verify
   * every field ended up holding what was asked for.
   *
   * Each field is filled with real per-character key events (see the inline
   * comment at the fill site below for why), then re-read to confirm it
   * stuck. The verification pass is the part that matters: the parameter
   * grid is rebuilt whenever a field commits (NewConnectionSnippetHost
   * registers a ChangeHandler that calls updateCodePanel), and a rebuild
   * between resolving a field and writing to it sends the text somewhere
   * else. Losing this race does not throw: characters silently land in the
   * wrong box, and the wizard happily connects to whatever that produced (a
   * stray "pwpostgresql" split into Server "127.0.0.1wpostgresql" and
   * Database "p", for instance). Reading every value back afterwards turns
   * that into an immediate, named failure instead of a puzzling connection
   * error several steps later.
   *
   * Server and Port are verified too, even though the caller never asked to
   * write them: the snippet prefills them, and they are exactly what a
   * misdirected write corrupts. Every attempt (including the first)
   * rewrites them to their expected values along with the caller's fields,
   * not just verifies them -- a corrupted Server/Port needs to be corrected
   * on retry the same way any other field does, or a mismatch there would
   * never clear and every attempt would fail identically.
   *
   * A mismatch retries the whole fill from scratch rather than failing
   * outright: the corrupting write coincides with some async task the wizard
   * runs after the type is selected, and retries a few milliseconds apart
   * (no pause) reliably reproduced the same corruption instead of clearing
   * it, so a pause is what actually gives the next attempt a clean window.
   * Each attempt clears a field before writing it, so redoing an
   * already-correct one is harmless. Only exhausting every attempt is a real
   * failure.
   */
  async fillWizardForTarget(
    target: EffectiveDbTarget,
    overrides: Record<string, string> = {},
  ): Promise<void> {
    // openWizard()'s own wait only confirms the dialog box itself is
    // visible, not that the type list inside it has finished loading --
    // that's the installer-catalog refresh RPC it mentions, which can take
    // just as long. click()'s default actionability timeout is too short
    // for it: every spec that restarts the R session in beforeEach before
    // opening the wizard (connect/explorer/queries) pays that "first open
    // of a session" cost on literally every test, not just once per file
    // the way new_connection_wizard.test.ts (no restart) does. Wait for the
    // entry on its own generous deadline, matching openWizard()'s, before
    // clicking it.
    const typeEntry = this.wizard.typeEntry(target.driverName);
    await typeEntry.waitFor({ state: 'visible', timeout: 20000 });
    await typeEntry.click();
    const written = { ...target.wizardFields, ...overrides };
    // Server and Port only exist on a server target's snippet. A file
    // target's grid is the single Database path, so `written` is the whole
    // set of fields to verify.
    const expected =
      target.kind === 'server'
        ? { Server: target.host, Port: String(target.port), ...written }
        : { ...written };

    const maxAttempts = 3;
    let wrong: string[] = [];
    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      if (attempt > 1) await this.page.waitForTimeout(500);
      // Rewrite every field this attempt verifies (expected), not just the
      // ones the caller asked to write (written): a retry exists because
      // Server/Port can get corrupted by the same race that corrupts a
      // written field (see the class comment above), and only rewriting
      // `written` would leave a corrupted Server/Port uncorrected on every
      // subsequent attempt, guaranteeing the loop exhausts and throws.
      for (const [key, value] of Object.entries(expected)) {
        const field = this.wizard.field(key);
        await field.waitFor({ state: 'visible', timeout: 10000 });
        // Real per-character key events rather than a value assignment. These
        // are input.gwt-TextBox fields in a dialog, which is the category the
        // suite's Playwright guidance says fill() is not enough for: it sets
        // .value and fires a single input event, so any handler keyed on
        // keystrokes never runs.
        //
        // clear() first because pressSequentially appends to whatever is
        // already in the field, where fill() replaced it -- and the retry loop
        // below depends on each attempt starting from empty.
        await field.clear();
        await field.pressSequentially(value);
        // Blur so the wizard commits the value into the generated code.
        // fill() sets the value and fires input events, but the code panel
        // regenerates on blur, and filling the *next* field is what used to
        // supply that blur. The last field of a grid therefore never got one:
        // invisible with the server targets, whose last field is Password and
        // whose generated code is only ever asserted on Database and UID, but
        // fatal for a file target, where Database is the only field and so
        // always the last.
        await field.blur();
      }

      wrong = [];
      for (const [key, value] of Object.entries(expected)) {
        const actual = await this.wizard.field(key).inputValue();
        if (actual !== value) wrong.push(`${key}: expected "${value}", got "${actual}"`);
      }
      // A tripwire for the field-corruption race, which is rare (~2% of fills)
      // and self-healing on retry, so pass/fail alone hides it entirely. Every
      // future run is therefore a free sample: if this ever reappears, the log
      // says so with the corrupted values, rather than the run simply going
      // green and the problem going unmeasured.
      if (wrong.length === 0) {
        if (attempt > 1) {
          console.warn(`[wizard-fill] ${target.id}: recovered on attempt ${attempt}`);
        }
        return;
      }
      console.warn(
        `[wizard-fill] ${target.id}: attempt ${attempt} CORRUPTED -- ${wrong.join('; ')}`,
      );
    }

    throw new Error(
      `New Connection wizard fields did not take the values written to them, after ` +
        `${maxAttempts} attempts (the parameter grid was rebuilt mid-write): ${wrong.join('; ')}`,
    );
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
   * setup connection out of the UI under test.
   *
   * `detail` carries the console's raw output on failure (an R error, most
   * often -- a bad credential, a platform-specific SQL syntax issue in
   * `seedSql`) so a caller's skip reason can name the actual problem instead
   * of a generic "seeding failed", matching the standard `dbAvailability`
   * sets for every other skip reason in this suite. Empty on success.
   */
  async seedDatabase(target: EffectiveDbTarget): Promise<{ ok: boolean; detail: string }> {
    const sqlVector = target.seedSql.map((s) => rStringLiteral(s)).join(', ');
    // The connection arguments mirror the target's own snippet: a server
    // engine needs endpoint plus credentials, a file engine takes the path
    // and nothing else.
    const connectArgs =
      target.kind === 'server'
        ? `Server = ${rStringLiteral(target.host)}, ` +
          `Port = ${target.port}, ` +
          `Database = ${rStringLiteral(target.database)}, ` +
          `UID = ${rStringLiteral(target.user)}, ` +
          `PWD = ${rStringLiteral(target.password)}, `
        : `Database = ${rStringLiteral(target.database)}, `;
    const expr =
      'local({ ' +
      'op <- options(connectionObserver = NULL); on.exit(options(op), add = TRUE); ' +
      'con <- DBI::dbConnect(odbc::odbc(), ' +
      `Driver = ${rStringLiteral(target.driverName)}, ` +
      connectArgs +
      'timeout = 10); ' +
      'on.exit(DBI::dbDisconnect(con), add = TRUE); ' +
      `for (sql in c(${sqlVector})) DBI::dbExecute(con, sql); ` +
      'TRUE })';
    if ((await this.consoleActions.evalRLogical(expr)) === true) {
      return { ok: true, detail: '' };
    }
    const detail = (await this.consoleActions.lastOutputText()).trim();
    return { ok: false, detail };
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
