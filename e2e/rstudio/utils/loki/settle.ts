/**
 * Waiting for the interface to stop moving, and noticing when it never will.
 *
 * Two problems this solves, both learned the hard way.
 *
 * The first is attribution. RStudio's command dispatch returns as soon as the
 * command is handed off, not when its handler finishes, so a loop that acts as
 * fast as it can runs several handlers at once. An earlier version of this tool
 * managed fifty actions in 1.4 seconds, with steps 81 milliseconds apart, and
 * when an exception arrived there was no telling which action caused it. A
 * finding that cannot be attributed to an action cannot be written down as a
 * step, which is the whole point of the project. So every action is followed by
 * a wait for the screen to stop changing, and only then are exceptions
 * collected: whatever arrives now belongs to the action just performed, because
 * the action before it was also waited out.
 *
 * The second is knowing when the session is gone. The same earlier version
 * dispatched a command that replaced the page, then went on to log "no
 * candidates" thirty-four times against a dead session and reported the run as
 * having finished normally. The fingerprint below includes the page URL and
 * whether the bridge is present precisely so that cannot happen quietly again.
 */

import type { Page } from '@playwright/test';
import { withDeadline } from '../deadline';

/**
 * The cheap signals polled to decide whether the interface has stopped moving.
 * Read in a single page evaluation: a fingerprint that took five round trips
 * would sample a moving target.
 */
export type Fingerprint = {
  url: string;
  bridgePresent: boolean;
  ready: boolean;
  numDialogs: number;
  topDialogLabel: string | null;
  activeDocId: string | null;
  consoleBusy: boolean;
};

/**
 * What was on screen before an action. Steps mean nothing without it, and the
 * report prints it as the finding's precondition.
 */
export type ScreenState = {
  url: string;
  /** Title of the frontmost dialog, in the words shown to a person. */
  dialogTitle: string | null;
  numDialogs: number;
  /** Tab text of the active source document, e.g. "Untitled1". */
  activeDoc: string | null;
  /** Selected tab labels across the panes, e.g. ["Console", "Environment", "Files"]. */
  activeTabs: string[];
  consoleBusy: boolean;
};

/** Defaults, each overridable by an environment setting on the run. */
export const SETTLE = {
  /** Floor between actions. Nothing is sampled before this elapses. */
  paceMs: 400,
  /** Gap between the two fingerprint samples that have to agree. */
  quietMs: 300,
  /** Longest wait for agreement. Exceeding it is recorded, not fatal. */
  timeoutMs: 8000,
  /** How long the bridge may be missing before the run gives up on the session. */
  bridgeGraceMs: 60000,
  /** Bound on a single page evaluation, so a wedged page fails fast. */
  evaluateMs: 10000,
};

function numberFromEnv(name: string, fallback: number): number {
  const raw = process.env[name];
  if (raw === undefined || raw === '')
    return fallback;
  const value = Number(raw);
  if (!Number.isFinite(value) || value < 0)
    throw new Error(`${name}="${raw}" -- expected a non-negative number`);
  return value;
}

export function settleSettings() {
  return {
    paceMs: numberFromEnv('PW_LOKI_PACE_MS', SETTLE.paceMs),
    quietMs: numberFromEnv('PW_LOKI_QUIET_MS', SETTLE.quietMs),
    timeoutMs: numberFromEnv('PW_LOKI_SETTLE_MS', SETTLE.timeoutMs),
    bridgeGraceMs: numberFromEnv('PW_LOKI_BRIDGE_GRACE_MS', SETTLE.bridgeGraceMs),
  };
}

// ---------------------------------------------------------------------------
// Reading the page
// ---------------------------------------------------------------------------

/**
 * One evaluation returning both the fingerprint and the human-readable screen
 * state. They overlap almost entirely, and reading them separately would let
 * the report describe a different moment than the one that was settled.
 */
async function readPage(page: Page): Promise<{ fingerprint: Fingerprint; screen: ScreenState }> {
  return page.evaluate(() => {
    const visible = (el: Element) => {
      const rect = el.getBoundingClientRect();
      return rect.width > 0 && rect.height > 0;
    };

    const dialogs = Array.from(document.querySelectorAll('.gwt-DialogBox')).filter(visible);
    // GWT appends each dialog to the end of the body, so the last visible one
    // is the frontmost.
    const top = dialogs[dialogs.length - 1];
    const topDialogLabel = top ? (top.getAttribute('aria-label') || 'dialog') : null;

    const bridge = window.rstudio;
    const active = bridge?.documents?.active?.() ?? null;

    const sourceTab = document.querySelector(
      "[class*='rstudio_source_panel'] [class*='PanelTab-selected']",
    );
    const activeDoc = sourceTab ? ((sourceTab as HTMLElement).innerText ?? '').trim() : null;

    const activeTabs = Array.from(document.querySelectorAll("[class*='PanelTab-selected']"))
      .filter(visible)
      .map(el => ((el as HTMLElement).innerText ?? '').replace(/\s+/g, ' ').trim())
      .filter(Boolean);

    const consoleInput = document.getElementById('rstudio_console_input');
    const consoleBusy = !!consoleInput
      && consoleInput.classList.contains('rstudio-console-busy');

    return {
      fingerprint: {
        url: window.location.href,
        bridgePresent: !!bridge,
        ready: bridge?.ready === true,
        numDialogs: dialogs.length,
        topDialogLabel,
        activeDocId: active ? active.id : null,
        consoleBusy,
      },
      screen: {
        url: window.location.href,
        dialogTitle: topDialogLabel,
        numDialogs: dialogs.length,
        activeDoc: activeDoc || null,
        activeTabs,
        consoleBusy,
      },
    };
  });
}

/** The fingerprint plus screen state, or a stand-in when the page cannot answer. */
export async function readScreen(
  page: Page,
): Promise<{ fingerprint: Fingerprint; screen: ScreenState }> {
  try {
    return await withDeadline(readPage(page), SETTLE.evaluateMs, 'Agent Loki screen read');
  } catch {
    // A page parked mid-navigation never gives an execution context back. Report
    // that as an absent bridge rather than throwing: the caller's dead-interface
    // check is what should decide the run is over.
    const url = page.url();
    return {
      fingerprint: {
        url,
        bridgePresent: false,
        ready: false,
        numDialogs: 0,
        topDialogLabel: null,
        activeDocId: null,
        consoleBusy: false,
      },
      screen: {
        url,
        dialogTitle: null,
        numDialogs: 0,
        activeDoc: null,
        activeTabs: [],
        consoleBusy: false,
      },
    };
  }
}

// ---------------------------------------------------------------------------
// Crashes the Command Palette swallows
// ---------------------------------------------------------------------------

/**
 * Captions of the dialog the Command Palette raises instead of letting a
 * command's exception escape. English and French are the only translations
 * PaletteConstants ships, so this is the complete set today; a locale added
 * later needs a line here or Agent Loki stops seeing this class of crash.
 */
const COMMAND_FAILURE_CAPTIONS = [
  'Command Execution Failed',
  "Échec de l'exécution de la commande",
];

/**
 * A crash the Command Palette caught and turned into a dialog.
 *
 * This exists because of what AppCommandPaletteItem.invoke does:
 *
 *     try { command_.execute(); }
 *     catch (Exception e) { display.showErrorMessage("Command Execution Failed", ...); }
 *
 * So a command handler that throws synchronously never reaches GWT's uncaught
 * exception handler, and never reaches window.rstudio.errors either. Watching
 * only the recorded exceptions therefore misses every synchronous handler
 * failure invoked this way, which is a large share of them. The dialog is the
 * product's own report of that crash, and it carries the command's label and the
 * exception's message, so it is read here and reported like any other crash.
 *
 * Worth knowing: the other routes do not have this problem. A click on a toolbar
 * button runs through GWT's ordinary event dispatch, where an exception does
 * reach the uncaught handler. The swallowing is specific to the palette.
 */
export type SwallowedFailure = {
  /** The dialog's caption, as shown. */
  caption: string;
  /** The command as the person invoking it saw it named. */
  commandLabel: string;
  /** The exception's own message. */
  detail: string;
  /** A readable one-line summary, and the basis for the crash signature. */
  message: string;
  /** The whole dialog, kept for triage when the parse comes up short. */
  raw: string;
};

/**
 * Patterns matching PaletteConstants' commandExecutionFailedMessage, which reads
 * "The command ''{0}'' could not be executed.\n\n {1}".
 *
 * Parsing against the template rather than scraping the dialog matters more than
 * it looks. The dialog's text also carries the button label and, on this build, a
 * trailing "Warning: screen reader mode not enabled" line. Using the whole box as
 * the crash message made the signature depend on that incidental text, so the
 * same crash hashed differently on a replay and every finding came out
 * unreproducible.
 *
 * The message element itself cannot be selected instead: MessageDialog styles it
 * with a GWT CssResource class, which is obfuscated in a production build.
 */
const COMMAND_FAILURE_BODIES = [
  /The command '(?<label>[^']*)' could not be executed\.?\s*(?<detail>.*)/,
  /La commande '(?<label>[^']*)' n'a pas pu être exécutée\.?\s*(?<detail>.*)/,
];

/**
 * Turn a failure dialog's caption and text into a stable summary.
 *
 * Separate from the page read so it can be tested directly, which matters: the
 * signature-instability bug this parsing exists to fix was invisible until the
 * exact dialog text from a real build was fed through it.
 */
export function parseSwallowedFailure(caption: string, raw: string): SwallowedFailure {
  const collapsed = raw.replace(/\s+/g, ' ').trim();
  const lines = raw.split('\n').map(line => line.trim()).filter(Boolean);

  for (let index = 0; index < lines.length; index++) {
    for (const pattern of COMMAND_FAILURE_BODIES) {
      const match = pattern.exec(lines[index]);
      if (match?.groups === undefined)
        continue;
      const label = match.groups.label ?? '';
      // The template puts the exception's message on a later line when the
      // sentence wraps, so fall through to the next line when this one ends at
      // the full stop.
      const detail = (match.groups.detail || lines[index + 1] || '').trim();
      return {
        caption,
        commandLabel: label,
        detail,
        message: `The command '${label}' could not be executed: `
          + `${detail === '' ? 'no further detail' : detail}`,
        raw: collapsed,
      };
    }
  }

  // The caption matched but the body did not parse, which means the wording
  // changed. Report it rather than dropping a real crash, and let the raw text
  // carry the signature; a reviewer will see the shape is wrong.
  return { caption, commandLabel: '', detail: '', message: collapsed, raw: collapsed };
}

/** Read a Command-Palette failure dialog, if one is showing. */
export async function readSwallowedFailure(page: Page): Promise<SwallowedFailure | null> {
  const found = await page.evaluate((captions) => {
    const boxes = Array.from(document.querySelectorAll('.gwt-DialogBox')).filter((el) => {
      const rect = el.getBoundingClientRect();
      return rect.width > 0 && rect.height > 0;
    });
    for (const box of boxes) {
      const caption = box.getAttribute('aria-label') ?? '';
      if (!captions.includes(caption))
        continue;
      // Line structure is preserved on purpose: the exception's message is the
      // first non-empty line after the template sentence, and collapsing the
      // whitespace would run it into the button row.
      return { caption, raw: (box as HTMLElement).innerText ?? '' };
    }
    return null;
  }, COMMAND_FAILURE_CAPTIONS).catch(() => null);

  if (found === null)
    return null;

  return parseSwallowedFailure(found.caption, found.raw);
}

function sameFingerprint(a: Fingerprint, b: Fingerprint): boolean {
  return a.url === b.url
    && a.bridgePresent === b.bridgePresent
    && a.ready === b.ready
    && a.numDialogs === b.numDialogs
    && a.topDialogLabel === b.topDialogLabel
    && a.activeDocId === b.activeDocId
    && a.consoleBusy === b.consoleBusy;
}

// ---------------------------------------------------------------------------
// Settling
// ---------------------------------------------------------------------------

export type SettleResult = {
  /** False when the interface was still changing at the deadline. */
  settled: boolean;
  fingerprint: Fingerprint;
  screen: ScreenState;
  waitedMs: number;
};

const sleep = (ms: number) => new Promise<void>(resolve => setTimeout(resolve, ms));

/**
 * Wait for two consecutive fingerprints to agree, after a floor delay.
 *
 * Failing to agree is not an error. Some RStudio panes animate or poll
 * indefinitely, and a run that treated that as fatal would stop on its first
 * plot. The step records `settled: false` so a reader knows the attribution for
 * that one action is weaker than usual.
 */
export async function settle(page: Page): Promise<SettleResult> {
  const { paceMs, quietMs, timeoutMs } = settleSettings();
  const startedAt = Date.now();

  await sleep(paceMs);

  let previous = await readScreen(page);
  while (Date.now() - startedAt < timeoutMs) {
    await sleep(quietMs);
    const current = await readScreen(page);
    if (sameFingerprint(previous.fingerprint, current.fingerprint)) {
      return {
        settled: true,
        fingerprint: current.fingerprint,
        screen: current.screen,
        waitedMs: Date.now() - startedAt,
      };
    }
    previous = current;
  }

  return {
    settled: false,
    fingerprint: previous.fingerprint,
    screen: previous.screen,
    waitedMs: Date.now() - startedAt,
  };
}

// ---------------------------------------------------------------------------
// Dead interface
// ---------------------------------------------------------------------------

/**
 * Tracks whether the session is still there to be driven.
 *
 * A missing bridge is not immediately fatal: opening a project or restarting R
 * clears it for a while and it comes back. What is fatal is a bridge that stays
 * missing, or a page that has left the session's own origin. Either one means
 * every later step would act on nothing, and the run has to say so instead of
 * counting empty steps toward its budget.
 */
export class InterfaceWatch {
  private bridgeMissingSince: number | null = null;

  constructor(private readonly origin: string) {}

  /** Build a watch from the page's current location. */
  static from(page: Page): InterfaceWatch {
    try {
      return new InterfaceWatch(new URL(page.url()).origin);
    } catch {
      return new InterfaceWatch(page.url());
    }
  }

  /**
   * Fold in one observation. Returns the reason the interface is unusable, or
   * undefined while it is still fine.
   */
  check(fingerprint: Fingerprint): string | undefined {
    let sameOrigin: boolean;
    try {
      sameOrigin = new URL(fingerprint.url).origin === this.origin;
    } catch {
      sameOrigin = false;
    }

    if (!sameOrigin) {
      return `the page left the session at ${this.origin} and is now showing ${fingerprint.url}`;
    }

    if (fingerprint.bridgePresent) {
      this.bridgeMissingSince = null;
      return undefined;
    }

    const { bridgeGraceMs } = settleSettings();
    if (this.bridgeMissingSince === null) {
      this.bridgeMissingSince = Date.now();
      return undefined;
    }
    const missingFor = Date.now() - this.bridgeMissingSince;
    if (missingFor >= bridgeGraceMs) {
      return `the automation bridge has been absent for ${Math.round(missingFor / 1000)}s, `
        + 'so the session is not coming back';
    }
    return undefined;
  }
}
