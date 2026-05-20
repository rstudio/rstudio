# BRAT-to-Playwright Migration Progress

Source: `src/cpp/tests/automation/testthat/`
Target: `e2e/rstudio/tests/`

This document tracks the per-file migration of BRAT (R/testthat) automation tests to
Playwright (TypeScript). See `.claude/skills/rstudio-migrate-tests-brat-to-playwright/SKILL.md`
for the migration workflow.

## Status legend

File-level status:

- **Not started** -- no work begun
- **In progress** -- branch open, some tests migrated, file still has BRAT tests
- **Complete** -- all `.rs.test()` blocks ported (or dropped with rationale); BRAT file deleted or noted as residual
- **Drop-and-replace** -- whole file recommended for deletion or replacement by non-Playwright coverage (R/JS unit tests)
- **Dropped** -- intentionally not ported; rationale in Notes
- **Fixme** -- ported test passes none-of-3 attempts; tracked in the Fixme Tests table below

Test-level dispositions used in the per-file tables below:

- **Covered** -- a Playwright test exercises the same user-facing behavior
- **Partial** -- behavior overlaps but a specific assertion/input differs; small delta migration
- **Not covered** -- needs porting
- **Unportable** -- depends on `.rs.*` internal-API behavior with no observable UI effect

## Summary

| Metric | Count |
|--------|------:|
| Total BRAT test files (original) | 32 (+ 1 helper file) |
| BRAT files remaining on disk | 5 (+ 1 helper file) |
| Files Complete | 24 |
| Files Dropped | 3 |
| Files Partial | 4 |
| Files Fixme | 1 |

Phase 1 audit complete (2026-05-19). Phase 2 (per-file migration) underway --
guardrails, quarto, and rmarkdown remain; debugger (2) and edit-suggestions (1)
have intentionally retained BRAT tests (build-cycle and focus-bug respectively).

## Conversion Status

### Editor (8 files, 60 tests)

| BRAT Source | Tests | Counterpart(s) | Status | Per-test (C/P/N/U) | Notes |
|-------------|------:|---------------|--------|-------------------:|-------|
| test-automation-editor.R | 4 | editor.test.ts (4) | Complete | 0/0/4/0 | All ported. Whitespace-on-save + whole-word replace (#16798) + Ace shortcuts (#16973) + findFromSelection (#15863). Added `AceEditor.find/getCursorPosition/insert/navigateLineEnd/focus` and a new `@utils/commands` helper that drives `window.rstudioCallbacks` (Desktop fixture now passes `--automation-agent`). BRAT file deleted |
| test-automation-code-folding.R | 2 | code-folding.test.ts (2) | Complete | 0/0/2/0 | Ported via Playwright -- `AceEditor` wrapper exposes `getFoldWidget`/`getFoldWidgetRange` via `page.evaluate`. BRAT file deleted |
| test-automation-edit-suggestions.R | 5 | edit_suggestions.test.ts (4 + 1 fixme) | Fixme | 0/0/4/0 (+1 fixme) | Ported 4 of 5 via `.rs.api.showEditSuggestion` injected through `typeInConsole`. The fifth ("survive document mutations") `test.fixme`d -- after typing the first char into an editor with an active ghost-text suggestion, subsequent chars route to the console. Cause not pinned down (NES status-bar refresh? Ace blur? worth filing as a focus bug). BRAT counterpart for that one case retained until unblocked |
| test-automation-syntax-highlighting.R | 11 | syntax-highlighting.test.ts (11) | Complete | 0/0/11/0 | All ported via `AceEditor` wrapper (`getTokens`, `getTokenAt`, `getState`, `getFoldWidget`). Color tests assert `bg` on `string.color` tokens directly. BRAT file deleted |
| test-automation-rmarkdown.R | 18 | rmarkdown.test.ts (7), multiline_chunk_execution.test.ts (1), notebook_save_during_execution.test.ts (1) | Partial | 1/1/16/0 | Biggest single porting opportunity: chunk-options popup UI (~6 tests, all DOM-driven), visual-mode round-trips, chunk-widget visibility, error-halt, history-recall, paged-table, patchwork. 9 of 18 are `skip_on_ci()` |
| test-automation-quarto.R | 12 | quarto.test.ts (1), multiline_chunk_execution.test.ts (1) | Partial | 0/1/8/3 | Mirrors Rmd chunk-options gap (`.qmd` variants). Raw HTML/LaTeX block preservation via visual mode is portable. 3 token/fold-widget tests are unportable. 7 of 12 are `skip_on_ci()` |
| test-automation-sweave.R | 2 | sweave.test.ts (2) | Complete | 0/0/2/0 | Ported via `AceEditor.getTokens`/`getMarkers`. BRAT file deleted |
| test-automation-reformat.R | 6 | air_formatting.test.ts (+1), reformat.test.ts (4) | Complete | 0/0/6/0 | All ported. Added "5: checked, air.toml present, save uses Air" to air_formatting.test.ts. New reformat.test.ts covers #5425 (built-in formatter preserves end-of-line comments), styler reformat on save, and the two `@windows_only` #17471 newline-regression tests. Styler tests use `consoleActions.ensurePackages(['styler'])` and `test.skip` when missing. The on-save test opens a project via `createAndOpenProject` so `TextEditingTarget.maybeFormatOnUserInitiatedSave` doesn't bail. Added `closeProjectIfOpen` + `waitForConsoleIdle` to `@utils/project`. Canonicalized the sandbox root in `fixtures/sandbox-setup.ts` so JS-side paths match the canonical form rstudioapi reports (fixes the project-prefix check on macOS where `/var` symlinks to `/private/var`). BRAT file deleted |

### Console, completions, debugger, data viewer (5 files, 41 tests)

| BRAT Source | Tests | Counterpart(s) | Status | Per-test (C/P/N/U) | Notes |
|-------------|------:|---------------|--------|-------------------:|-------|
| test-automation-console.R | 8 | console_output.test.ts (8) | Complete | 0/0/8/0 | All ported as `console_output.test.ts`. Covers `\r` overwriting console output, condition highlighting (errors/warnings/messages spans with `consoleHighlightConditions`), `options(warn = 2)` treating warnings as errors (#16031), CR + annotation (#16038), `consoleLineLengthLimit` truncation, post-`try()` error output (#16337), and Ctrl+Shift+M / Alt+- on the console input (#16973). Shortcuts test uses `activateConsole` via the JS bridge to focus the console input deterministically. BRAT file deleted |
| test-automation-completions.R | 17 | autocomplete.test.ts (12, console + editor), autocomplete_extras.test.ts (11) | Complete | 5/0/11/0 (1 dropped) | The 11 long-tail tests ported in `autocomplete_extras.test.ts`: new local vars, R6 active bindings (#14784), pipe-expression completions at doc start (#13611), .DollarNames (#15115), `code_completion_include_already_used` pref (#13065), dplyr backtick-quoted column names (#15161), column quoting via Tab-accept (#13290), roxygen tags in .R / .Rmd / .qmd (#5809), pipe placeholder `_$` and `_$<prefix>`. `Tab indents multi-line selections` (#15046) intentionally dropped here -- it's an editor-shortcut test, not a completions test; better placed in `editor.test.ts` if added later. Refactored `createAndOpenFile` to use `rStringLiteral` so multi-line content with real `\n` works through the same path (updated 99 call sites in `code_suggestions.test.ts` and `rmarkdown.test.ts` to drop their pre-escaping workaround). BRAT file deleted |
| test-automation-restart.R | 1 | panes/misc/session_restart.test.ts (1) | Complete | 0/0/1/0 | Ported directly via `.rs.api.restartSession('print(x + y)')` from console; asserts `[1] 3` appears after restart. BRAT file deleted |
| test-automation-debugger.R | 8 | debugger.test.ts (~15), debugger_extras.test.ts (4) | Partial | 6/0/0/0 (2 BRAT-only) | The four migration-plan gaps ported in `debugger_extras.test.ts`: multi-line input at Browse[N]> preserves browser state, gutter-click breakpoint toggle (#9450), `Clear All Breakpoints` (#9450, `@server_only` -- Desktop opens a native Electron messagebox), S7 method breakpoints (#16490, S7 installed via `ensurePackages`). The two package-build tests (#15201 and the #9450 package-rebuild half) stay in BRAT -- they require a full devtools build cycle (~minutes) that doesn't fit the Playwright fixture model |
| test-automation-data-viewer.R | 7 | pagination-sorting.test.ts (5), data_viewer.test.ts (7) | Complete | 0/0/7/0 | All ported as `data_viewer.test.ts`. Covers temporary-expression iframe (#14657), search filter + viewerLink cell-explorer hop, three-state sort cycle, pin-icon column reorder, per-object state persistence across `frame.contentWindow.refreshData()`, and HTML-special-character escaping in both cell values and column names. Search input found via `page.getByLabel('Search data table')` (BRAT's `.search` class no longer exists). BRAT file deleted |

### Chat / Posit Assistant (3 files, 36 tests)

| BRAT Source | Tests | Counterpart(s) | Status | Per-test (C/P/N/U) | Notes |
|-------------|------:|---------------|--------|-------------------:|-------|
| test-automation-chat.R | 2 | chat-pane-persistence.test.ts (2) | Complete | 0/0/2/0 | Both ported -- no-refresh on Options dismiss + restart-survival (asserts iframe body is non-empty after `.rs.api.restartSession()`). BRAT file deleted |
| test-automation-chat-satellite.R | 3 | chat-satellite-commands.test.ts (1), detachable-sidebar.test.ts (1, desktop_only) | Complete | 1/1/0/1 | Ported `popOutChat`/`returnChatToMain` command variant; DOM-click case covered by existing detachable-sidebar test; Chrome reload/WindowCloseMonitor test dropped (no user gesture, see dropped tests below). BRAT file deleted |
| test-automation-guardrails.R | 31 | chat-guardrails.test.ts (11), chat-guardrails-paths.test.ts (27) | Partial | 27/0/0/4 (4 BRAT-only) | All 27 path-based tests ported to `chat-guardrails-paths.test.ts` (read denials, write denials, connection denials, error-message structure, system-file denials, path traversal, plus SSH-public-key and normal-file read-allows). Drives `.rs.chat.injectBindings()` / `.rs.chat.restoreBindings()` directly from the console via a one-shot `.rs.test.guardrails(quote(<expr>))` helper installed in `beforeAll`. Each test runs `pre; .rs.test.guardrails(quote(<expr>)); post` as one console submission to batch plant+guard+cleanup, and polls for an R-runtime-generated marker (won't match the input echo) instead of sleeping. The 4 binding-lifecycle tests (`safeEval` auto-restore, `safeEval` blocks writes, double-inject reentrancy guard, `safeEval` restores on error) stay in BRAT -- `.rs.chat.safeEval` returns errors as conditions rather than printing them, so the Playwright "blocked in console" pattern doesn't apply. Also dropped the 2s blind wait in `rstudio.fixture.ts:beforeEach` (`click({ timeout: 2000 })` -> `isVisible()` + `waitFor({ state: 'hidden' })`) which was adding ~2s to every test |

### Workbench panes / misc (10 files, ~22 tests)

| BRAT Source | Tests | Counterpart(s) | Status | Per-test (C/P/N/U) | Notes |
|-------------|------:|---------------|--------|-------------------:|-------|
| test-automation-build-pane.R | 1 | panes/misc/build_pane_testthat.test.ts (1) | Complete | 0/0/1/0 | Drives a package skeleton via `.rs.rpc.package_skeleton`, runs `testTestthatFile`, asserts "Test complete" + xtermColor. `installDepIfPrompted` handles the devtools update modal. BRAT file deleted |
| test-automation-environment-pane.R | 0 | -- | Dropped | -- | Empty file. Deleted 2026-05-19 |
| test-automation-packages-pane.R | 4 | packages_pane.test.ts (2) | Complete | 0/0/2/2 | MASS attach/detach and stats-already-attached ported. renv variant (#16842) dropped here -- skip_on_ci in BRAT and depends on renv install timing; the non-renv path covers the load/unload regression. "We reset state" cleanup dropped (Playwright fixture handles teardown). BRAT file deleted |
| test-automation-files.R | 3 | files.test.ts (3) | Complete | 0/0/3/0 | All ported. The two virtualized Open File dialog tests are `@server_only` and click the listbox first so `typeSlowly` reaches RowTable's prefix-search handler instead of the path text input. Autosave-unchanged-doc (#16329) verifies mtime-equality before edit and inequality after. Added `typeSlowly` helper in `@utils/constants`. BRAT file deleted |
| test-automation-files-endpoint.R | 1 | files_endpoint.test.ts (1) | Complete | 0/0/1/0 | Ported via `page.request` -- the test drives raw HTTP `GET /files/...` with custom `Sec-Fetch-Site` / `Referer` headers and asserts response status. `@server_only` (endpoint only registered in server mode). All 14 assertions kept. BRAT file deleted |
| test-automation-history.R | 1 | console_pane.test.ts (+1) | Complete | 0/0/1/0 | Ported as new "timestamp() adds an entry to console history" case in the existing Console pane describe block. BRAT file deleted |
| test-automation-ignorefiles.R | 1 | projects/ignorefiles.test.ts (1) | Complete | 0/0/1/0 | Wizard-driven port (palette > New Directory > New Project + git checkbox). Asserts pre/post `.positai` state via Node fs reads. BRAT file deleted |
| test-automation-terminal.R | 2 | panes/terminal/terminal.test.ts (2) | Complete | 0/0/2/0 | Both ported via `rstudioapi::terminalCreate()` from console, xterm bounding-box assertion, and `terminalBuffer()` polling for command output. BRAT file deleted |
| test-automation-tabs.R | 5 | tabs.test.ts (3), panes/layout/panes.test.ts (18), panes/layout/pane_layout.test.ts (16) | Complete | 0/3/2/0 | Three fresh tests in `tabs.test.ts` -- core 12-tab existence, aria-selected on click, and `layoutZoomEnvironment` toggle (uses `isCommandChecked` from `@utils/commands`). Two sidebar-width-with-pane-layout tests dropped: skip_on_ci in BRAT, and the width-preservation regression they cover is already exercised end-to-end in panes.test.ts (#16676 cycle). BRAT file deleted |
| test-automation-defer-scope.R | 4 | -- | Dropped | 0/0/0/4 | Tests `.rs.test()` framework + `withr::defer()` semantics. No UI. Deleted 2026-05-19 |

### Projects, runtimes, lifecycle (5 files, 8 tests)

| BRAT Source | Tests | Counterpart(s) | Status | Per-test (C/P/N/U) | Notes |
|-------------|------:|---------------|--------|-------------------:|-------|
| test-automation-projects.R | 3 | create_projects.test.ts (6), project_id.test.ts (1), project_trust_dialog.test.ts (8) | Complete | 1/1/1/0 | Added git-enabled new-project test (verifies VCS tab appears) and the ProjectId regression as its own spec (drives project_user_data_directory pref + restart cycle). BRAT file deleted |
| test-automation-python.R | 1 | panes/misc/python_completions.test.ts (1) | Complete | 0/0/1/0 | Drives `reticulate::repl_python()` from console, presses Tab on `sys.__name` and verifies the unquoted echo + `'sys'` result. `afterEach` exits the REPL even on assertion failure. BRAT file deleted |
| test-automation-shinytest2.R | 1 | projects/shinytest2.test.ts (2) | Complete | 0/0/1/0 | Split into two tests: the toolbar-button + info-dialog branch, and the snapshot_review namespace-stub branch (skipped if shinytest2 isn't installed). BRAT file deleted |
| test-automation-refactoring.R | 1 | panes/editor/rename_in_scope_rmd.test.ts (1) | Complete | 0/0/1/0 | Ported #4961 -- positions cursor at line 7, runs `renameInScope`, asserts the 5 expected selection ranges across sibling chunks via new `AceEditor.getSelectionRanges()`. BRAT file deleted |
| test-automation-smoke.R | 1 | smoke/startup.test.ts (1) | Dropped | 1/0/0/0 | Smoke-tested the BRAT harness itself; Playwright `startup.test.ts` covers RStudio startup. Deleted 2026-05-19 |
| test-automation-suspend.R | 2 | panes/misc/session_suspend.test.ts (2) | Complete | 0/0/2/0 | Both ported and tagged `@server_only`. Server fixture updated to support `--auth-none` mode so local rserver-dev can run them. BRAT file deleted |

### Non-test helper files

| File | Role | Disposition |
|------|------|-------------|
| helper-pane-layout.R | testthat helper sourced by `test-automation-tabs.R` and `test-automation-chat-satellite.R` | Keep until both files are migrated/removed. Playwright equivalents (`findTabCheckbox`, `isTabChecked`, `toggleTab`, etc.) already exist as private helpers in `panes/layout/panes.test.ts` and `panes/layout/pane_layout.test.ts` -- reuse those, no new shared utility needed |

## Recommended migration order

### Done (waves 1-4)

- Wave 1 (2026-05-19): `environment-pane.R`, `smoke.R`, `defer-scope.R` dropped; `code-folding.R`, `syntax-highlighting.R`, `sweave.R` ported via the new `AceEditor` page-object wrapper
- Wave 2 (2026-05-19): `history.R`, `restart.R`, `build-pane.R`, `ignorefiles.R`, `python.R`, `refactoring.R`, `shinytest2.R`. Added `utils/r.ts` (rStringLiteral, rPathLiteral), renamed `utils/test_files.ts` to `utils/files.ts`, added `AceEditor.getSelectionRanges()`
- Wave 3 (2026-05-19/2026-05-20): `chat.R`, `chat-satellite.R`, `projects.R`, `terminal.R`, `suspend.R` (Server fixture updated for `--auth-none`); `files.R`, `packages-pane.R`, `tabs.R`, `editor.R` (Desktop fixture passes `--automation-agent`; `typeSlowly`, multiple `AceEditor` additions, `@utils/commands` helper); `files-endpoint.R`; `edit-suggestions.R` (4 of 5; one `test.fixme` for a focus bug); `reformat.R`
- Wave 4 (2026-05-20): `console.R` -> `console_output.test.ts`; `data-viewer.R` -> `data_viewer.test.ts`; `debugger.R` -> `debugger_extras.test.ts` (4 ported, 2 package-build tests retained); `completions.R` -> `autocomplete_extras.test.ts`

### Remaining (wave 5)

- `guardrails.R` (31): in progress. Most read/write/system path tests are deterministic mechanism tests that don't need the AI in the loop; `chat-guardrails-paths.test.ts` drives them via `.rs.chat.injectBindings()` / `restoreBindings()` directly
- `rmarkdown.R` (18): chunk-options popup UI is the biggest single porting opportunity; visual-mode round-trips, chunk-widget visibility, error-halt, history-recall, paged-table, patchwork. 9 of 18 are `skip_on_ci()` in BRAT
- `quarto.R` (12): mirrors the Rmd chunk-options gap; raw HTML/LaTeX block round-trips via visual mode; 3 token/fold-widget tests likely unportable; 7 of 12 are `skip_on_ci()`

### Wave 6 -- BRAT decommissioning (single final PR)

See "Decommissioning checklist" below.

## Open decisions for the user

These need a call before / during migration:

1. **Unportable tests (~20)**: drop entirely vs port to R/JS unit tests. The skill default is case-by-case; current strong candidates for R unit tests are:
   - 4 binding-lifecycle tests in `guardrails.R` (`injectBindings`/`restoreBindings`/`safeEval`)
   - 2 code-folding tests
   - 5+ syntax-highlighting token-type tests
2. **`skip_on_ci()` BRAT tests (~20+)**: port-and-enable in Playwright, port-and-keep-skipped, or drop? Many are in `rmarkdown.R`, `quarto.R`, `reformat.R`, `tabs.R`
3. **Mechanism differences**: BRAT often sets prefs via `.rs.uiPrefs$*$set()` while Playwright drives the Options dialog. Default to UI-driving Playwright tests, but for tests where the *only* point is the pref behavior (not the dialog), `.rs.api.writeRStudioPreference()` via console is equivalent and faster
4. **`edit-suggestions.R` injection mechanism**: BRAT uses `.rs.api.showEditSuggestion` to deterministically inject suggestions. Playwright tests use the real provider (Copilot / Posit Assistant). Decide whether to add a deterministic injection path in Playwright (via `executeCommand`) or accept reduced determinism

## Fixme Tests

Playwright tests that exist but can't pass against current RStudio (e.g. open bug, missing feature). Tracked here so the BRAT counterpart isn't removed prematurely.

| Playwright File | Test Name | Blocker | Notes |
|-----------------|-----------|---------|-------|
| tests/panes/editor/edit_suggestions.test.ts | ghost text suggestions survive document mutations | Focus bug while ghost text is active | After typing the first char into an editor with an active ghost-text suggestion, subsequent chars route to the console. `typeSlowly` (200ms inter-char delay) and per-char `editor.focus()` re-claim don't help. `editor.insert()` (Ace API) bypasses the focus issue but leaves NES anchor stuck at the original document position, so the gutter-click accept overwrites our typed chars. Worth filing as a real focus bug |

## Dropped / Unportable Tests

BRAT tests intentionally not ported. Requires user approval per the migration skill.

| BRAT File | Test Name | Disposition | Rationale |
|-----------|-----------|-------------|-----------|
| test-automation-environment-pane.R | (file empty) | Drop | File contained no tests |
| test-automation-smoke.R | automation harness smoke test | Drop | `expect_true(TRUE)` smoke-tested the BRAT harness, not RStudio. `e2e/rstudio/tests/smoke/startup.test.ts` covers RStudio startup |
| test-automation-defer-scope.R | per-test defer: register on success | Drop | Tests `.rs.test()`/`withr::defer()` framework semantics. No user-visible behavior |
| test-automation-defer-scope.R | per-test defer: verify fired after success | Drop | Same as above |
| test-automation-defer-scope.R | per-test defer: fires alongside expectation failure | Drop | Same as above |
| test-automation-defer-scope.R | per-test defer: fires when error exits local scope | Drop | Same as above |
| test-automation-chat-satellite.R | satellite window reload does not return chat to main pane | Drop | Chrome-specific reload path (Server only) simulated via `window.open(url, name)` IIFE -- no user gesture. Verifies the WindowCloseMonitor fix from #17259. Regression coverage for this path belongs in a unit/integration test, not UI automation |

## Decommissioning checklist (Phase 3)

Only check off once every BRAT file above is Complete or Dropped.

- [ ] Delete `src/cpp/tests/automation/testthat/`
- [ ] Delete `src/cpp/tests/automation/CLAUDE.md`
- [ ] Delete `src/cpp/session/modules/automation/`
- [ ] Remove `--run-automation`, `--automation-filter`, `--automation-markers` CLI flags (session and rserver)
- [ ] Remove BRAT-related R helpers (`.rs.automation.*`, `.rs.test`, `.rs.markers`, etc.)
- [ ] Update root `CLAUDE.md` and `e2e/rstudio/.claude/CLAUDE.md` to remove BRAT references
- [ ] Update the `rstudio-create-playwright-tests` skill (currently calls BRAT "deepest source of truth")
- [ ] Remove the `reference_brat_automation.md` user-memory pointer
