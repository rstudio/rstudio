import type { Page, Locator } from 'playwright';
import { PageObject } from './page_object_base_classes';

// Active source-pane scope — matches the visible (non-hidden) source tab,
// the same pattern used by source_pane.page.ts. Gutter cells, breakpoint
// markers, and the executing-line highlight all live inside the active
// editor's Ace DOM.
const ACTIVE_EDITOR =
  "xpath=//div[@role='tabpanel' and not(contains(@style, 'display: none'))]" +
  "//*[starts-with(@id,'rstudio_source_text_editor')]";

// The secondary console toolbar gets aria-label="Console Tab Debug" while
// debug mode is active (ConsolePane.java:371 → consoleTabDebugLabel).
const DEBUG_TOOLBAR = '[role="toolbar"][aria-label="Console Tab Debug"]';

// Toolbar-button title attributes are built from `desc` + the keyboard
// shortcut (AppCommand.getTooltip). desc strings come from
// Commands.cmd.xml and are stable across releases. Match by prefix so the
// shortcut suffix doesn't break the selector when keybindings change.
const DEBUG_BUTTON_TITLE_PREFIX = {
  step: 'Execute the next line of code',
  stepInto: 'Step into the current function call',
  finish: 'Execute the remainder of the current function or loop',
  continue: 'Continue execution until the next breakpoint is encountered',
  stop: 'Exit debug mode',
} as const;

export class DebuggerPage extends PageObject {
  // Editor-side debug visuals (Ace classes — stable, not GWT-obfuscated).
  public gutterCells: Locator;
  public breakpoints: Locator;
  public pendingBreakpoints: Locator;
  public inactiveBreakpoints: Locator;
  // Matches breakpoints in any of the three states (active/pending/inactive).
  // Use this when waiting for a breakpoint to appear without caring which
  // state it landed in — RStudio may transition through pending before
  // settling on active or inactive depending on whether the function is in
  // scope at click time.
  public anyBreakpointMarker: Locator;
  public activeDebugLine: Locator;
  public executingLineGutter: Locator;

  // Debug toolbar + buttons.
  public debugToolbar: Locator;
  public stepBtn: Locator;
  public stepIntoBtn: Locator;
  public finishBtn: Locator;
  public continueBtn: Locator;
  public stopBtn: Locator;

  // Console-side error widget — "Rerun with Debug" link.
  public rerunWithDebugLink: Locator;

  constructor(page: Page) {
    super(page);

    this.gutterCells = page.locator(`${ACTIVE_EDITOR}//*[contains(@class,'ace_gutter-cell')]`);
    this.breakpoints = page.locator(`${ACTIVE_EDITOR}//*[contains(@class,'ace_breakpoint')]`);
    this.pendingBreakpoints = page.locator(`${ACTIVE_EDITOR}//*[contains(@class,'ace_pending-breakpoint')]`);
    this.inactiveBreakpoints = page.locator(`${ACTIVE_EDITOR}//*[contains(@class,'ace_inactive-breakpoint')]`);
    this.anyBreakpointMarker = page.locator(
      `${ACTIVE_EDITOR}//*[contains(@class,'ace_breakpoint')` +
      ` or contains(@class,'ace_pending-breakpoint')` +
      ` or contains(@class,'ace_inactive-breakpoint')]`,
    );
    this.activeDebugLine = page.locator(`${ACTIVE_EDITOR}//*[contains(@class,'ace_active_debug_line')]`);
    this.executingLineGutter = page.locator(`${ACTIVE_EDITOR}//*[contains(@class,'ace_executing-line')]`);

    this.debugToolbar = page.locator(DEBUG_TOOLBAR);
    this.stepBtn = this.debugToolbar.locator(`[title^="${DEBUG_BUTTON_TITLE_PREFIX.step}"]`);
    this.stepIntoBtn = this.debugToolbar.locator(`[title^="${DEBUG_BUTTON_TITLE_PREFIX.stepInto}"]`);
    this.finishBtn = this.debugToolbar.locator(`[title^="${DEBUG_BUTTON_TITLE_PREFIX.finish}"]`);
    this.continueBtn = this.debugToolbar.locator(`[title^="${DEBUG_BUTTON_TITLE_PREFIX.continue}"]`);
    this.stopBtn = this.debugToolbar.locator(`[title^="${DEBUG_BUTTON_TITLE_PREFIX.stop}"]`);

    // The "Rerun with Debug" link lives in ConsoleError.ui.xml within the
    // .consoleErrorCommands container. Like Environment-pane classes, the
    // CSS class names are GWT-obfuscated, so locate by the literal anchor
    // text (stable, baked into ConsoleError.ui.xml:90).
    this.rerunWithDebugLink = page.locator('#rstudio_workbench_panel_console').getByText('Rerun with Debug');
  }

  /** Locate the gutter cell for `line` by its rendered line-number text.
   *  Filtering by text instead of `nth(line - 1)` survives Ace's gutter
   *  virtualization in long / scrolled editors, where the Nth DOM cell
   *  doesn't necessarily correspond to source line N+1. */
  gutterCellForLine(line: number): Locator {
    return this.gutterCells.filter({
      hasText: new RegExp(`^\\s*${line}\\s*$`),
    });
  }
}
