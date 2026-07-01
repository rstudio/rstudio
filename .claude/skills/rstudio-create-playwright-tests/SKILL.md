---
name: rstudio-create-playwright-tests
description: Patterns and gotchas for authoring RStudio Playwright tests in TypeScript. Use when writing, reviewing, or migrating tests under e2e/rstudio/.
---

# Creating RStudio Playwright Tests

Most of what you need is in `e2e/rstudio/README.md` (basic structure,
conventions, selector hierarchy, Ace interactions, cross-platform shortcuts,
sandbox, tags, package deps) and in the existing tests under
`e2e/rstudio/tests/`. The fixture (`@fixtures/rstudio.fixture`) handles
RStudio launch/shutdown, save-dialog dismissal, and buffer cleanup -- author
tests as if the IDE starts clean.

This file covers RStudio-specific gotchas that aren't in the README.

## Before writing

- Skim `e2e/rstudio/tests/` for an existing similar test.
- Skim `e2e/rstudio/pages/`, `actions/`, and `utils/` for existing helpers.

## Universal rules

1. **`pressSequentially()` for GWT text inputs whose handlers fire per
   keystroke** -- console, editor, and most `input.gwt-TextBox` fields in
   dialogs/wizards (where typing a character enables OK, triggers
   autocomplete, etc.). `fill()` doesn't fire GWT key events in those cases.
   Inputs driven by a discrete trigger like `press('Enter')` or a button click
   (e.g., the console Find bar) work fine with `fill()`, which has the bonus
   of replacing text instead of appending. Start with `fill()`; switch to
   `pressSequentially()` only if the handler doesn't fire.

2. **Force-click Ace textareas:** `await locator.click({ force: true })`. An
   `ace_content` div overlays the hidden textarea and intercepts normal
   clicks. `focus()` is also unreliable -- keystrokes can land in the wrong
   pane.

3. **Derive selectors from source, never use `gwt-uid-XXXX`** (those change
   every restart):
   - `src/gwt/.../commands/Commands.cmd.xml` -- command IDs map to menu
     items at `#rstudio_label_<sanitized_id>` and toolbar buttons at
     `#rstudio_tb_<sanitized_id>` (see `ElementIds.java` for the sanitizer).
   - `src/gwt/.../core/client/theme/DocTabLayoutPanel.java` -- tab structure
     (`.gwt-TabLayoutPanelTab`, `.gwt-TabLayoutPanelTab-selected`, etc.).

4. **Invoke RStudio commands and prefs via the `window.rstudio` bridge** --
   import helpers from `@utils/commands` (`executeCommand`, `setPref`, etc.).
   Don't use `.rs.api.executeCommand(...)` (slow console roundtrip) or
   `window.desktopHooks.invokeCommand(...)` (Electron-only, crashes on Server).

5. **Decision order for triggering an action:** GUI button/menu/shortcut, then
   `executeCommand(page, id)`, then `page.evaluate()` as a last resort.
   Clicking a button tests the real user path; the helper is for
   setup/teardown or when the UI path is slow/flaky/tangential to what's
   being tested.

6. **RStudio binds some shortcuts to plain Ctrl on every platform**
   (including macOS) -- e.g., `Ctrl+Enter` Run Line, `Ctrl+L` Clear Console,
   `Ctrl+Space` Autocomplete. Use Playwright's plain `Control` for those, not
   `ControlOrMeta`. If you're unsure, check what RStudio's keyboard-shortcut
   UI shows for the binding.

7. **Tests must work on Desktop and Server.** Prefer stable IDs over wrapper
   selectors. Tag mode-specific tests `@desktop_only` or `@server_only`
   rather than runtime-branching on `testInfo.project.name`.

## Feature-specific patterns

When working in these areas, also read the corresponding file:

- **Code suggestions / Copilot / NES** (`tests/panes/editor/code_suggestions.test.ts`,
  `edit_suggestions.test.ts`): see `code-suggestions.md`.
- **Chat pane / Posit Assistant / RPC interception**
  (`tests/panes/posit-assistant-chat/`): see `chat-pane.md`.
- **Visual editor / citations** (`tests/panes/editor/citations.test.ts`, and any
  test that drives the panmirror visual editor): see `visual-editor.md`.
