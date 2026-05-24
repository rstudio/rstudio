# E2E Anti-Pattern Cleanup

Tracking cleanup of anti-patterns surfaced by the 2026-05-24 scan of
`e2e/rstudio/` (102 TypeScript files across `tests/`, `utils/`, `pages/`,
`actions/`, `fixtures/`).

Items are grouped into three tiers:

- **Must** -- documented project rules; every occurrence is a violation.
- **Should** -- structural fixes that pay back across many tests.
- **Nice** -- stylistic polish; address opportunistically.

Initial counts are recorded so progress can be checked against the scan
baseline.

## Initial baseline

| Category                                   | Count           |
|--------------------------------------------|-----------------|
| Blind sleeps (`waitForTimeout` + `sleep`)  | 4 + ~150        |
| Console-based `.rs.*` RPC                  | 8 sites         |
| Focused / unexplained tests                | 0 `.only`; 1 bare `test.skip` |
| Inline `timeout: NNNN`                     | 371 (50 are 30000 / 60000)    |
| XPath / deep CSS selectors                 | 0               |
| `force: true` clicks                       | 16              |
| `page.evaluate` driving UI                 | 72 (4 = desktopHooks command palette) |
| `toBe(true|false)` on locator booleans     | ~200            |
| In-test `try/catch` swallowing             | 9               |
| `console.log` leftovers                    | ~150            |
| `if (await ...)` branches in tests         | 24+             |

---

## Must -- rule violations

### Blind sleeps

Project rule: wait for a measurable condition, not a fixed duration. See
memory `feedback_avoid_blind_sleeps.md`.

Hot-spot test files:

- [ ] `tests/panes/editor/code_suggestions.test.ts` -- 38 `await sleep()`
      calls; 22 are `sleep(5000)`. Single highest-leverage file.
- [ ] `tests/panes/layout/panes.test.ts` -- 21 `sleep(TIMEOUTS.layoutSettle)`
      calls. See the "layout-settle signal" item under **Should**.
- [ ] `tests/panes/misc/s4_unloaded_package.test.ts:168` -- `sleep(5000)`
      after `restartR`; comment notes "replace once restartR has a
      confirmable post-condition".
- [ ] `tests/panes/misc/python_completions.test.ts:56,59` -- `sleep(3000)` /
      `sleep(500)` around Python REPL typing.
- [ ] `tests/panes/posit-assistant-chat/uninstall-posit-ai.test.ts:74,119`
- [ ] `tests/panes/posit-assistant-chat/chat-pane-persistence.test.ts:27,78`
- [ ] `tests/panes/debugger/debugger_extras.test.ts:89` -- `sleep(300)`

Shared infrastructure (each removal cascades through many tests):

- [ ] `actions/chat_pane.actions.ts` -- 15 sleeps in lines 90-230
- [ ] `actions/console_pane.actions.ts` -- 10 sleeps
      (150, 158, 170, 179, 182, 197, 221, 240, 298, 316)
- [ ] `actions/assistant_options.actions.ts` -- 11 sleeps in lines 37-116
- [ ] `actions/autocomplete.actions.ts` -- 9 sleeps
- [ ] `actions/source_pane.actions.ts` -- 5 sleeps (line 242: `sleep(2000)`)
- [ ] `fixtures/desktop.fixture.ts` -- 10 sleeps; worst at line 529
      (`sleep(5000)`) and line 551 (`sleep(3000)`)
- [ ] `fixtures/server.fixture.ts` -- 7 sleeps (line 288: `sleep(2000)`;
      line 300 manual race timeout may be OK)
- [ ] `pages/console_pane.page.ts` -- 6 sleeps in `typeInConsole`,
      `clearConsole`, `goToLine`, `closeAllBuffersWithoutSaving`
- [ ] `pages/modals.page.ts:21,33` -- `sleep(5000)` then `sleep(1000)`
- [ ] `pages/environment_pane.page.ts:65,67`
- [ ] `utils/project.ts` -- 6 sleeps (3000, 2000, 1500, others)

`waitForTimeout` sites (4):

- [ ] `tests/smoke/startup.test.ts:10` -- 30s "stay alive" body; consider
      replacing with an actual readiness assertion
- [ ] `tests/panes/files/files.test.ts:133` -- 1500ms mtime granularity
      guard; has comment, may stay
- [ ] `tests/panes/posit-assistant-chat/chat-user-skills.test.ts:130` --
      5000ms; comment "no bridge-exposed signal for backend shutdown"
- [ ] `tests/panes/posit-assistant-chat/readline-notification.test.ts:60` --
      1000ms absence-of-event window

### Console-based `.rs.*` RPC

Project rule: use `executeCommand(page, ...)` / bridge helpers instead of
typing `.rs.api.*` into the console. See memory
`feedback_executecommand_over_console_api.md`.

Ported:

- [x] `tests/projects/shinytest2.test.ts:78` -- `.rs.api.openProject` ->
      `openProject(page, '${dir}/${basename}.Rproj')`
- [x] `tests/panes/misc/build_pane_testthat.test.ts:69` -- same as above
- [x] `tests/panes/editor/syntax-highlighting.test.ts:62,89` --
      `.rs.writeUserPref("rainbow_fenced_divs", ...)` -> `setPref`
- [x] `tests/projects/project_trust_dialog.test.ts:239,248` --
      `.rs.api.readRStudioPreference(...)` via `captureResult` -> `getPref`
- [x] `tests/projects/shinytest2.test.ts:87` and
      `tests/panes/misc/build_pane_testthat.test.ts:82` --
      `.rs.api.documentOpen(...)` -> `documentOpen(page, path)` via new
      `window.rstudio.documents.open(path, opts?)` bridge method.

Intentionally kept as console RPC (no bridge equivalent or testing the API
itself); each site has an inline comment explaining the reason:

- `tests/projects/shinytest2.test.ts` -- `.rs.api.initializeProject` writes
  the .Rproj file; no bridge for project initialization.
- `tests/panes/misc/session_restart.test.ts` -- the test specifically
  exercises `.rs.api.restartSession`'s `afterRestart` parameter, which the
  `restartR` AppCommand does not expose.

Follow-up: adding `project.initialize(path)` to the automation bridge would
close out the initializeProject site.

### Lone unexplained `test.skip`

- [ ] `tests/panes/editor/rmarkdown.test.ts:211` -- bare
      `test.skip('visual mode go to next and prev chunk', ...)` with no
      comment or linked issue. Either explain, link an issue, or restore.

---

## Should -- structural fixes

### Build a measurable layout-settle signal

`tests/panes/layout/panes.test.ts` has 21 `sleep(TIMEOUTS.layoutSettle)`
calls (and `pane_layout.test.ts` has more). A real signal -- mutation
observer on the pane container, a `data-layout-settled` attribute, or a
busy-class watcher -- would let us strike all of them at once and stop
papering over real races.

- [ ] Design a layout-settle signal in the GWT layout code (or expose an
      existing one through the automation bridge)
- [ ] Replace `sleep(layoutSettle)` calls in `panes.test.ts` and
      `pane_layout.test.ts`

### Clean up `code_suggestions.test.ts`

Single worst file in the suite (38 sleeps, ~40 `console.log` calls, several
`if (await ...)` branches, two `force: true` clicks). Worth a dedicated
pass.

- [ ] Replace sleeps with measurable waits
- [ ] Strip step-narration `console.log` calls (lines 84, 137, 143, 176,
      226, 229, 270, 286, 289, 329, 340, 343, 346, 373, 384, 419, 433, 469,
      483, 486, 518, 536, 538, 587, 593, 597, 619, 628, 634)
- [ ] Collapse `if (await ...)` branches at lines 136, 269, 328, 584
      (especially line 137 which logs a WARNING for a stale gutter icon
      instead of failing -- file the bug or assert it)
- [ ] Document or remove `force: true` click at line 630

### Strip `console.log` debugging leftovers

~150 occurrences total; cleanup hooks that log cleanup errors are fine,
but step-narration inside test bodies should go. Worst (outside
`code_suggestions.test.ts`):

- [ ] `tests/panes/editor/quarto.test.ts:68-83` -- 7 "Checking ..." logs
- [ ] `tests/projects/project_trust_dialog.test.ts:240,311,327,328`
- [ ] `tests/panes/misc/s4_unloaded_package.test.ts:79,180,196,199,212`
- [ ] `tests/panes/layout/pane_location_persistence.test.ts:133,138,141,148`

### Move conditional cleanup out of test bodies

Tests should follow a single deterministic path; conditional cleanup logic
belongs in `afterEach`.

- [ ] `tests/panes/posit-assistant-chat/shiny-app-r.test.ts:48,64,76,89` --
      four conditional cleanup branches in the test body
- [ ] `tests/panes/posit-assistant-chat/open-chat-pane.test.ts:39,57` --
      `chatIframe.isVisible()` branching
- [ ] `tests/panes/layout/panes.test.ts:73,85,228` -- sidebar-presence
      branches
- [ ] `tests/panes/layout/pane_location_persistence.test.ts:38,51,65,74`
- [ ] `tests/panes/console/console_pane.test.ts:141,170` -- find-bar state
      branches
- [ ] `tests/panes/editor/air_formatting.test.ts:120` -- modal-handling
      branch
- [ ] `tests/panes/misc/rstudioapi_show_dialog.test.ts:69` -- iterating
      buttons with `.isVisible().catch(() => false)`
- [ ] `tests/projects/project_id.test.ts:45` -- pre-existing-modal close
- [ ] Eliminate the `.isVisible().catch(() => false)` swallow pattern in
      chat / dialog tests -- it can't distinguish "not present" from
      "selector broke"

### Document or remove undocumented `force: true` clicks

16 sites; the comment at `actions/chat_pane.actions.ts:294` is a good
template ("GWT console DOM elements report overlapping coordinates").

- [ ] `tests/panes/debugger/debugger_extras.test.ts:51`
- [ ] `tests/panes/editor/code_suggestions.test.ts:630` -- on
      `statusBarCompletionReceived`
- [ ] `tests/panes/console/console_pane.test.ts:38,76`
- [ ] `utils/project.ts:109`
- [ ] `pages/console_pane.page.ts:277,284`
- [ ] `actions/source_pane.actions.ts:114,121,144`
- [ ] `actions/autocomplete.actions.ts:103`
- [ ] `fixtures/server.fixture.ts:268`

### Stop bypassing the command bridge for the command palette

- [ ] `tests/panes/posit-assistant-chat/uninstall-posit-ai.test.ts:42,73,87,99`
      -- `page.evaluate("window.desktopHooks.invokeCommand('showCommandPalette')")`
      should be `executeCommand(page, 'showCommandPalette')`

### Remove the `TIMEOUTS` object; wait on observable state

`utils/constants.ts` exports `TIMEOUTS` (`sessionRestart: 30000`,
`fileOpen: 20000`, `consoleReady: 15000`, `settleDelay: 1000`, etc.). Its
existence implies that long blind timeouts on assertions are an acceptable
substitute for waiting on observable state -- they aren't. The right
pattern is to poll a bridge-state predicate
(`window.rstudio.project.path()`, `documents.active()`, the `console-busy`
class, etc.) and let the assertion run at Playwright's default short
timeout. Each `TIMEOUTS` entry callers reach for is a missing or
under-exposed bridge signal.

PR #17761 demonstrates the pattern: `openProject` polls
`window.rstudio.project.path()` against the requested .Rproj, so the
follow-up `expect(PROJECT_MENU).toContainText(...)` runs at the default
5s timeout instead of the previous 30s `TIMEOUTS.sessionRestart` blind
wait.

Goal: shrink `TIMEOUTS` to only the legitimate non-wait constants
(`pollInterval`, `slowKeystroke`) and remove the rest.

- [ ] Audit each `TIMEOUTS.*` use site and identify the observable
      post-condition it's papering over (e.g. `consoleReady` ->
      `#rstudio_console_input` visible + not `.rstudio-console-busy`).
- [ ] Surface the underlying signal in the automation bridge if not
      already exposed.
- [ ] Convert each call site to poll the signal; the assertion that
      follows can run at Playwright's default.
- [ ] Delete the entry from `TIMEOUTS` once all sites are gone.

Volume to chase: 371 inline `timeout: NNNN` occurrences in test code; 50
of them are the 30000 / 60000 cases. Worst files: `panes.test.ts` (47),
`project_trust_dialog.test.ts` (26), `rmarkdown.test.ts` (25),
`code_suggestions.test.ts` (24), `chat_pane.actions.ts` (21),
`create_projects.test.ts` (17), `pane_layout.test.ts` (15).

---

## Nice -- stylistic polish

### Prefer auto-retrying matchers over `toBe(true|false)` on locator booleans

~200 sites. Most are `expect(await isTabChecked(...)).toBe(true)` style in
`tests/panes/layout/pane_layout.test.ts`; `toBeChecked()` or `expect.poll`
would auto-retry and give a better failure message.

- [ ] Audit `pane_layout.test.ts`
- [ ] Audit the rest of `tests/panes/layout/`

### Use specific matchers instead of `toBeTruthy`

- [ ] `tests/panes/misc/s4_unloaded_package.test.ts:42` -- regex match
- [ ] `tests/panes/misc/rs_value_contents.test.ts:28,120`

### Remove in-test `try/catch` swallows

9 sites where catches inside tests hide failure modes (cleanup-hook
catches are fine and excluded):

- [ ] `tests/panes/misc/autocomplete_extras.test.ts:132`
- [ ] `tests/panes/data-viewer/data_viewer.test.ts:165,207,239`
- [ ] `tests/panes/posit-assistant-chat/chat-websocket-url.test.ts:39`
- [ ] `tests/panes/editor/code_suggestions.test.ts:215`
- [ ] `tests/panes/console/console_output.test.ts:77,94,162`
- [ ] `tests/panes/editor/syntax-highlighting.test.ts:63`
- [ ] `tests/panes/editor/editor.test.ts:49`
- [ ] `tests/panes/editor/air_formatting.test.ts:104`

---

## Verified clean (no action needed)

- `test.only` / `describe.only` -- none committed.
- XPath selectors -- none.
- `debugger;` statements -- none.
- `noWaitAfter: true` -- none.
- `#rstudio_*` GWT id selectors (43) -- these are stable, project-preferred
  handles; not a smell.
- Recorded `test.skip` / `test.fixme` -- all but `rmarkdown.test.ts:211`
  have explanations or linked issues.
