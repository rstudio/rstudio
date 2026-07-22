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
- Extend `PageObject` (panes) or `FramePageObject` (iframe-hosted UI) from
  `pages/page_object_base_classes.ts` when adding a page object.

## Universal rules

1. **`pressSequentially()` for GWT text inputs whose handlers fire per
   keystroke** -- console, editor, and most `input.gwt-TextBox` fields in
   dialogs/wizards (where typing a character enables OK, triggers
   autocomplete, etc.). `fill()` doesn't fire GWT key events in those cases.
   Inputs driven by a discrete trigger like `press('Enter')` or a button click
   (e.g., the console Find bar) work fine with `fill()`, which has the bonus
   of replacing text instead of appending. Start with `fill()`; switch to
   `pressSequentially()` only if the handler doesn't fire. For GWT type-ahead
   widgets (e.g. the Open File dialog's file list) even `pressSequentially()`
   can outrace the per-keystroke handler -- use `typeSlowly`
   (`@utils/constants`, 200ms/char).

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
   The full bridge surface is documented in `e2e/rstudio/CLAUDE.md`.

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

## Waits and markers that lie

1. **Console markers can false-pass off the command echo.** The command you
   type travels the same output/event stream you're checking, so
   `output.includes(marker)` can match the echo of `cat("<marker>")` even when
   R never ran it. Split the marker across `cat()` args
   (`cat("[pw:", "ready]", sep = "")`) or build probe strings with `paste0()`.

2. **Closing a project on Server reloads the page.** `waitForLoadState`,
   console-visible, and console-idle can all satisfy against the *old* page;
   the late reload then breaks the next test. Use `closeProjectIfOpen`
   (`@utils/project`), which blocks on `window.rstudio.project.isActive() ===
   false` -- a signal only the post-reload page can produce. Never hand-roll it.

3. **Focus needs re-dispatch, not dispatch-once + poll.** Other UI (e.g. the
   Assistant iframe reloading after a project open) can steal focus *after*
   your command ran. `focusConsole(page)` (`pages/console_pane.page.ts`)
   re-issues `activateConsole` inside its poll loop. Retrying helpers are for
   setup; keep raw one-shot dispatch only where single-dispatch behavior is
   itself under test.

4. **Session restarts: use the helpers, respect the timing.** rserver can hold
   undeliverable console input up to ~30s, and a suspended session's relaunch
   can exceed 30s. `waitForSessionRestart` / `restartSessionWithSentinel`
   (`@utils/project`) already encode both -- reuse them.
   `ConsolePaneActions.restartSession()` is the third option, for when the
   test needs `.rs.api.restartSession()` itself (e.g. its `clean:` argument).

5. **Size timeouts to what gates the UI.** The Python-interpreters modal opens
   only after a machine-wide scan (60s), not a flat 15s. Before tagging a test
   `@desktop_only` for "server-specific" behavior, check whether it's just slow.

6. **External services: skip on service error, fail on silent nothing.** Poll
   a three-state outcome (matched/error/pending); `test.skip` on error, but a
   timeout with neither must still fail so a regression that renders nothing
   isn't masked. Worked example in `visual-editor.md`.

7. **Document paths come back home-aliased** (`~/sub/file.R`) when the file is
   under the rsession's home. Reuse `openFile` / `waitForActiveDocument`, which
   handle it -- don't compare `doc.path` to an absolute path yourself.

8. **Bracket async-dispatched console jobs with `waitForConsoleBusy`.** A
   command dispatched off-tick (e.g. `executeCurrentChunk`) may not have
   started when a follow-up idle-wait samples the console, which then reads
   spuriously idle. Wait for busy first (`waitForConsoleBusy`,
   `pages/console_pane.page.ts`), then for idle.

9. **A stray modal reads as "intercepts pointer events".** Any GWT modal
   renders a `gwt-PopupPanelGlass` overlay, so an unexpected dialog (e.g.
   "Error Listing Packages" after resume) surfaces as an opaque
   `gwt-PopupPanelGlass intercepts pointer events` timeout at an unrelated
   click. Call `dismissBlockingModals` (`pages/modals.page.ts`) after
   suspend/resume/restart; stable dialog-button ids are exported there
   (`CONFIRM_BTN` = `#rstudio_dlg_ok`, `YES_BTN`, `NO_BTN`, `CANCEL_BTN`).

## Server mode and the sandbox

1. **Use the `@utils/files` helpers for test-file operations, not raw Node
   `fs`.** `writeAndOpenFile` / `seedSandboxFile` / `removeSandboxFile` /
   `closeAndDeleteSandboxFiles` auto-detect whether the workdir is writable
   by the test process and fall back to R-console `writeLines`/`unlink` when
   it isn't (Server's rsession runs as a different user; remote rsessions
   have a different filesystem), staying byte-identical to `fs.writeFileSync`
   (`sep="", useBytes=TRUE`). Git operations go through the R console, with
   inline `-c user.name=... -c user.email=...` since CI runners have no
   global git config. When interpolating a test-computed value into an R
   command, use `rStringLiteral` / `rPathLiteral` (`@utils/r`) -- never
   hand-build `"${path}"`. `@utils/heredoc` sends multi-line content cleanly.

2. **Components that store secrets via the OS keychain need it disabled in
   the fixture.** Under the sandboxed HOME, macOS has no login keychain, so a
   keytar-based write throws a blocking "Keychain Not Found" modal; on
   Windows the write would instead land in the *host's real* Credential
   Manager, leaking test state onto the developer's machine.

## Techniques

1. **Asserting on real RPC/event traffic is a legitimate technique, not just
   mocking it.** `page.waitForResponse` can pin that a specific RPC fired and
   inspect its body (e.g. confirm `asyncHandle` is set, proving it registered
   as async rather than blocking). `page.on('response')` filtered to
   `get_events` URLs, with `expect.poll` on the body text, can confirm *how*
   the backend delivered something (e.g. one batched event vs. many
   per-file events) -- detach the listener in `finally`. When
   substring-matching event names, include the JSON quotes
   (`'"files_changed"'`), since an unquoted match can also hit a
   similarly-named event.

2. **Fixture setup should fail loudly, not as a mysterious timeout.** Verify
   both the subprocess's exit status AND the setup script's own boolean
   result (a COM `Save()` or an AppleScript bookmark write can fail with exit
   code 0), then assert a unique sentinel line in console output. This turns
   an environment problem into one clear setup-failure message instead of a
   visibility timeout in every downstream test.

3. **Replay a hidden-tab/iframe race deterministically instead of chasing
   timing.** Dispatch synthetic events into the hidden iframe
   (`el.dispatchEvent(new Event('scroll'))`) or call the component's lifecycle
   hook directly from `page.evaluate` (e.g. `iframe.contentWindow.onActivate()`)
   to reproduce the race on demand, with no flaky timing needed.

4. **Capturing a satellite (popout) window:** register
   `page.context().waitForEvent('page')` *before* issuing the command that
   opens it, then assert the new page's URL contains the expected `view=<name>`
   marker. Works the same way for Desktop (Electron satellite) and Server.

5. **Drive Ace through the `AceEditor` page object**
   (`pages/ace_editor.page.ts`) rather than ad-hoc `page.evaluate`. An empty
   marker (`new AceEditor(page, '')`) resolves the *active* editor via the
   bridge -- prefer it. A non-empty marker does a `.ace_editor` DOM walk that
   can land on stale editors left after a tab close; use it only to target a
   non-active tab. Typed Ace bindings live in `@utils/ace` -- extend them
   there instead of casting at the call site.

6. **Probe R-side state with `evalRLogical`**
   (`actions/console_pane.actions.ts`): runs an R expression returning one
   logical and reads `TRUE`/`FALSE` back from the console -- e.g.
   `evalRLogical('requireNamespace("dplyr", quietly = TRUE)')`.

## Feature-specific patterns

When working in these areas, also read the corresponding file:

- **Code suggestions / Copilot / NES** (`tests/panes/editor/code_suggestions.test.ts`,
  `edit_suggestions.test.ts`): see `code-suggestions.md`.
- **Chat pane / Posit Assistant / RPC interception**
  (`tests/panes/posit-assistant-chat/`): see `chat-pane.md`.
- **Visual editor / citations** (`tests/panes/editor/citations.test.ts`, and any
  test that drives the panmirror visual editor): see `visual-editor.md`.
- **Files pane / Open File dialog** (`tests/panes/files/`): see `files-pane.md`.
- **Terminal pane** (`tests/panes/terminal/`): see `terminal.md`.
- **Auth setup / AI credentials** (`tests/auth.setup.ts`, `utils/auth.ts`, or
  anything touching credential provisioning): see `auth-credentials.md`.
