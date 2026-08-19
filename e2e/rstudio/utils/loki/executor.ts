/**
 * Performing actions the way a person does, and writing down what was done in
 * the same breath.
 *
 * The rule this file exists to keep: **the automation bridge is an instrument,
 * never an actor.** No action that could produce a finding goes through
 * `window.rstudio`. The bridge reads which commands are enabled, counts dialogs,
 * and collects exceptions; every action is a real key press, a real piece of
 * typed text, or a real click on a visible element.
 *
 * The reason is not purity. An earlier version dispatched commands through the
 * bridge, which meant every reported step was a translation: "the tool
 * dispatched presentation2PresentFromBeginning, and we claim a person would have
 * chosen Present from Beginning". Translation after the fact is exactly where a
 * fabricated set of steps came from. Here there is nothing to translate. Each
 * executor returns the sentence and the machine record together, both built from
 * what was on screen at the moment it acted, so the words a reader follows and
 * the actions a replay repeats are two views of one event.
 *
 * On Desktop the main menu bar is native: DesktopMenuCallback hands the menu
 * structure to Electron, which builds an operating-system menu with nothing in
 * the page. That is why the Command Palette is the primary route. It lists every
 * visible command, is identical on Desktop and Server, and is itself a route a
 * person uses.
 */

import type { Locator, Page } from '@playwright/test';
import { paletteEntryId } from './element-ids';
import type { Machine, Outcome } from './report';

/** Command Palette selectors, from ElementIds.java. */
export const PALETTE_SEARCH = '#rstudio_command_palette_search';
export const PALETTE_LIST = '#rstudio_command_palette_list';

/**
 * Commands.cmd.xml binds showCommandPalette to "Cmd+Shift+P", where RStudio's
 * "Cmd" means Ctrl or Meta. The Firefox-on-Windows alternative in the line below
 * it never applies: this suite runs Chromium.
 */
const PALETTE_KEYS = 'ControlOrMeta+Shift+P';

const TIMEOUT = {
  paletteOpen: 5000,
  entryVisible: 5000,
  paletteClose: 5000,
  // Short on purpose. A row that will not take a click almost never starts
  // taking one, and the config's 10s action timeout spent on each such row would
  // eat a run's budget several actions at a time.
  click: 3000,
};

/** One performed action, in both registers, plus how it went. */
export type Performed = {
  do: string;
  machine: Machine;
  outcome: Outcome;
  detail?: string;
};

/**
 * How a person would write the palette shortcut. macOS keyboards say Cmd; every
 * other platform says Ctrl. This is the only place the tool needs to know, and
 * it affects the words in a step, not the key that gets pressed.
 */
export function humanPaletteShortcut(platform: string): string {
  return platform === 'darwin' ? 'Cmd+Shift+P' : 'Ctrl+Shift+P';
}

/**
 * The accessible name of an element, in the order a screen reader would take it:
 * aria-label, then visible text, then title. Returns an empty string when the
 * element has no name, which is a refusal (see below), not a fallback.
 */
export async function accessibleName(locator: Locator): Promise<string> {
  return locator.evaluate((el) => {
    const aria = el.getAttribute('aria-label');
    if (aria && aria.trim() !== '')
      return aria.trim();
    const text = (el as HTMLElement).innerText;
    if (text && text.trim() !== '')
      return text.replace(/\s+/g, ' ').trim();
    const title = el.getAttribute('title');
    if (title && title.trim() !== '')
      return title.trim();
    return '';
  }).catch(() => '');
}

export class Executor {
  constructor(
    private readonly page: Page,
    private readonly platform: string,
  ) {}

  /**
   * Run a command by finding it in the Command Palette and clicking it.
   *
   * The label is read from the live row immediately before the click, not taken
   * from the generated facts. The facts decide what to type; the page decides
   * what the step says it clicked. If the two ever disagree, the step reports
   * what a person would actually have seen.
   *
   * The entry is never reached by pressing Enter. The highlighted row can be a
   * preference toggle or an R addin rather than the command that was searched
   * for, and invoking one of those would both act on the wrong thing and make
   * the step a lie. The row is addressed by its own id instead.
   */
  async runCommandViaPalette(commandId: string, searchText: string): Promise<Performed> {
    const shortcut = humanPaletteShortcut(this.platform);
    const machine: Machine = { route: 'palette', commandId, typed: searchText };
    const search = this.page.locator(PALETTE_SEARCH);

    await this.page.keyboard.press(PALETTE_KEYS);
    try {
      await search.waitFor({ state: 'visible', timeout: TIMEOUT.paletteOpen });
    } catch {
      return {
        do: `Press ${shortcut} to open the Command Palette`,
        machine,
        outcome: 'skipped',
        detail: 'the Command Palette did not open, so nothing was done',
      };
    }

    await search.pressSequentially(searchText);

    const entry = this.page.locator(`#${paletteEntryId(commandId)}`);
    try {
      await entry.waitFor({ state: 'visible', timeout: TIMEOUT.entryVisible });
    } catch {
      await this.closePalette();
      return {
        do: `Press ${shortcut} to open the Command Palette and type '${searchText}'`,
        machine,
        outcome: 'skipped',
        detail: 'the Command Palette did not offer the command, so nothing was clicked',
      };
    }

    // A row the palette itself shows as unavailable is not something a person
    // can click, so neither does this.
    //
    // Worth spelling out, because the bridge cannot tell you this.
    // AppCommandPaletteEntry.enabled() is `isEnabled() && hasCommandHandlers()`,
    // and the second half is invisible from JavaScript. So a command can report
    // itself enabled through window.rstudio while its palette row renders with
    // aria-disabled="true" -- popoutChat does exactly that when the chat pane has
    // not been built. Clicking it would achieve nothing anyway:
    // AppCommandPaletteItem.invoke answers a disabled command with a "Command
    // Disabled" dialog. Left unchecked, Playwright waits out its whole action
    // timeout on the un-clickable row and the run dies on an unrelated error.
    if (await entry.getAttribute('aria-disabled') === 'true') {
      await this.closePalette();
      return {
        do: `Press ${shortcut} to open the Command Palette and type '${searchText}'`,
        machine,
        outcome: 'skipped',
        detail: 'the Command Palette showed the command as unavailable, so nothing was clicked',
      };
    }

    // The refusal rule: an action the tool cannot describe is an action it does
    // not take. If the row has no readable label there is no way to tell a
    // reader what to click, and the gap between what was done and what can be
    // said is exactly where invention starts.
    const label = await accessibleName(this.page.locator(`#${paletteEntryId(commandId)}_label`));
    if (label === '') {
      await this.closePalette();
      return {
        do: `Press ${shortcut} to open the Command Palette and type '${searchText}'`,
        machine,
        outcome: 'skipped',
        detail: 'unnameable element: the palette row had no readable label',
      };
    }

    try {
      await entry.click({ timeout: TIMEOUT.click });
    } catch {
      // The row was there and named, but would not take a click. Record it as an
      // action that did not happen rather than letting it end the run.
      await this.closePalette();
      return {
        do: `Press ${shortcut} to open the Command Palette, type '${searchText}', `
          + `and click '${label}'`,
        machine,
        outcome: 'timed-out',
        detail: `the '${label}' row did not accept a click`,
      };
    }

    return {
      do: `Press ${shortcut} to open the Command Palette, type '${searchText}', `
        + `and click '${label}'`,
      machine,
      outcome: 'performed',
    };
  }

  /**
   * Close the palette if it is open, and wait for it to go.
   *
   * Recovery, not an action: it can never produce a finding, so it is not
   * logged as a step. The wait matters because a palette still fading out
   * intercepts the next action's clicks.
   */
  async closePalette(): Promise<void> {
    const search = this.page.locator(PALETTE_SEARCH);
    if (!(await search.isVisible().catch(() => false)))
      return;
    await this.page.keyboard.press('Escape').catch(() => {});
    await search.waitFor({ state: 'hidden', timeout: TIMEOUT.paletteClose }).catch(() => {});
  }

  /** Whether the palette is currently up. */
  async paletteIsOpen(): Promise<boolean> {
    return this.page.locator(PALETTE_SEARCH).isVisible().catch(() => false);
  }
}
