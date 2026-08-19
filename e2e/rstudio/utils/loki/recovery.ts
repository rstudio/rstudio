/**
 * Getting unstuck.
 *
 * Agent Loki's whole job is to leave RStudio in states nobody designed for, so
 * it will regularly put itself somewhere it cannot act. The commonest case is
 * plain: an action opens a modal dialog, and from then on the Command Palette
 * shortcut goes nowhere, because a GWT modal throws a glass panel over the page
 * and swallows the keystroke.
 *
 * That failure is quiet, which is what makes it worth a file of its own. In a
 * 25-action run, action 2 opened the Import Dataset dialog and the remaining 23
 * actions all reported "the Command Palette did not open". The run ended as a
 * clean, complete budget having invoked two commands. Nothing failed; the tool
 * simply stopped doing anything and said nothing about it.
 *
 * So: consecutive actions that could not be performed escalate through the ladder
 * below, and a run that cannot be recovered ends as `wedged` rather than counting
 * empty actions toward its budget.
 *
 * Recovery may use the automation bridge freely. The rule that the bridge is
 * never an actor is about actions that could produce a finding, and recovery
 * cannot: nothing it does is recorded as a step, and any exception it stirs up
 * becomes a lead with no steps attached.
 */

import type { Page } from '@playwright/test';
import { dismissAllModals, drainClientExceptions, resetLayoutZoom } from '../commands';
import type { ClientException } from '../commands';
import { closeProjectIfOpen } from '../project';
import { resetForNextTest } from '../test-reset';
import { dismissBlockingModals } from '../../pages/modals.page';
import type { Executor } from './executor';

/**
 * How many actions in a row may fail before each rung is tried.
 *
 * The first rung fires after a single failure, deliberately. A modal dialog is by
 * far the commonest reason an action cannot happen, pressing Escape costs
 * milliseconds, and waiting for a second failure would throw away one action out
 * of every two while a dialog sat there.
 */
const LADDER_AT = { escape: 1, dismissAll: 3, closeProject: 5, fullReset: 7, giveUp: 9 };

export type RecoveryAttempt = {
  /** Which rung was used, in words a log can print. */
  rung: string;
  /** Exceptions stirred up while recovering. These can never carry steps. */
  raised: ClientException[];
};

/**
 * Tracks consecutive failures to act and escalates.
 *
 * The rungs run cheapest first, and each is a superset of the damage the last
 * one does: pressing Escape costs nothing, a full reset throws away the state the
 * run had built up. Going straight to the reset would work and would also
 * destroy every interesting state Agent Loki had reached.
 */
export class Recovery {
  private consecutiveFailures = 0;

  constructor(
    private readonly page: Page,
    private readonly executor: Executor,
  ) {}

  /** Note that an action was performed. Resets the ladder. */
  succeeded(): void {
    this.consecutiveFailures = 0;
  }

  /**
   * Note that an action could not be performed, and recover if it is time to.
   * Returns what was done, or null when nothing was warranted yet.
   */
  async failed(): Promise<RecoveryAttempt | null> {
    this.consecutiveFailures++;
    const n = this.consecutiveFailures;

    if (n === LADDER_AT.escape)
      return this.run('pressed Escape and dismissed any blocking dialog', async () => {
        await this.executor.closePalette();
        await this.page.keyboard.press('Escape').catch(() => {});
        await dismissBlockingModals(this.page).catch(() => []);
      });

    if (n === LADDER_AT.dismissAll)
      return this.run('dismissed every dialog and ended any pane zoom', async () => {
        await dismissAllModals(this.page).catch(() => {});
        await resetLayoutZoom(this.page).catch(() => {});
      });

    if (n === LADDER_AT.closeProject)
      return this.run('closed the open project', async () => {
        await closeProjectIfOpen(this.page).catch(() => {});
      });

    if (n === LADDER_AT.fullReset)
      return this.run('reset the session to a clean state', async () => {
        await resetForNextTest(this.page).catch(() => {});
      });

    return null;
  }

  /** True once the ladder is exhausted and the run should end as wedged. */
  isWedged(): boolean {
    return this.consecutiveFailures >= LADDER_AT.giveUp;
  }

  /** How many actions in a row have failed. */
  get failures(): number {
    return this.consecutiveFailures;
  }

  private async run(rung: string, action: () => Promise<void>): Promise<RecoveryAttempt> {
    await action();
    // Drain here so the next action's exceptions are its own. Anything recovery
    // stirred up belongs to no action at all.
    const raised = await drainClientExceptions(this.page).catch(() => [] as ClientException[]);
    return { rung, raised };
  }
}
