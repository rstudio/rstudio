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
| Total BRAT test files | 32 (+ 1 helper file) |
| Total `.rs.test()` blocks | ~169 |
| Tests Covered (eligible for BRAT removal after spot-check) | ~17 |
| Tests Partial (small delta porting) | ~30 |
| Tests Not covered (full port) | ~98 |
| Tests Unportable (drop or convert to unit test) | ~20 |
| Files Complete | 21 |
| Files Dropped | 3 |
| Files In progress | 0 |
| Files Not started | 8 |

Phase 1 audit complete (2026-05-19). Phase 2 (per-file migration) underway.

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
| test-automation-console.R | 8 | console_pane.test.ts (10), console_command_effects.test.ts (8), ansi_erase_in_line.test.ts (8), execute_from_editor.test.ts (2) | Partial | 0/2/6/0 | Biggest gaps in this slice: condition highlighting (errors/warnings/messages spans), `consoleLineLengthLimit` truncation, post-error output (#16337), multi-message annotation, Ctrl+Shift+M/Alt+- shortcuts in console input |
| test-automation-completions.R | 17 | autocomplete.test.ts (12, runs each test in console + editor) | Partial | 5/1/11/0 | Cores covered (#13196, #13291, #12678). Long tail not covered: roxygen tag completions (.R/.Rmd/.qmd), pipebind placeholder, dollar-names types, column-name quoting, pref toggle, R6 active bindings (#14784), multi-line Tab indent. Playwright adds extras (data.table $, Unicode column names) without BRAT counterpart |
| test-automation-restart.R | 1 | panes/misc/session_restart.test.ts (1) | Complete | 0/0/1/0 | Ported directly via `.rs.api.restartSession('print(x + y)')` from console; asserts `[1] 3` appears after restart. BRAT file deleted |
| test-automation-debugger.R | 8 | debugger.test.ts (~15) | Partial | 3/2/3/0 | Core stepping / breakpoints covered (Playwright actually adds Step Into/Out, Continue chains, recover-on-error, env locals, traceback). Gaps: S7 method breakpoints (#16490), package-build paths (#15201 partial), `debugClearBreakpoints` (Desktop messagebox blocker), multi-line `Browse[N]>` regression |
| test-automation-data-viewer.R | 7 | pagination-sorting.test.ts (5) | Partial | 0/2/5/0 | Complementary, not overlapping. Playwright tests pagination; BRAT tests filter/pin/state-persist/XSS-escaping. Big porting opportunity: filter toolbar, pin icon, refresh state persistence, 3-state sort cycle, HTML-special cell/column escaping |

### Chat / Posit Assistant (3 files, 36 tests)

| BRAT Source | Tests | Counterpart(s) | Status | Per-test (C/P/N/U) | Notes |
|-------------|------:|---------------|--------|-------------------:|-------|
| test-automation-chat.R | 2 | chat-pane-persistence.test.ts (2) | Complete | 0/0/2/0 | Both ported -- no-refresh on Options dismiss + restart-survival (asserts iframe body is non-empty after `.rs.api.restartSession()`). BRAT file deleted |
| test-automation-chat-satellite.R | 3 | chat-satellite-commands.test.ts (1), detachable-sidebar.test.ts (1, desktop_only) | Complete | 1/1/0/1 | Ported `popOutChat`/`returnChatToMain` command variant; DOM-click case covered by existing detachable-sidebar test; Chrome reload/WindowCloseMonitor test dropped (no user gesture, see dropped tests below). BRAT file deleted |
| test-automation-guardrails.R | 31 | chat-guardrails.test.ts (11) | Partial | 3/7/17/4 | Largest delta. Port missing read paths (`~/.aws/credentials`, `~/.ssh/config`, `~/.netrc`, `id_rsa`, `.env.local`); missing write paths (`~/.ssh`, `file.create`, `file.remove`, `unlink`, `file.copy`); /etc/passwd, /etc/shadow, /etc/passwd write; path-traversal; structural error message; SSH public-key allow case. 4 binding-lifecycle tests (`injectBindings`/`restoreBindings`/`safeEval`/double-inject) are Unportable -- consider R-level testthat |

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

Phase 2 ordering (one PR per file per Hard Rule):

### Wave 1 -- removals and trivial files (low risk, fast)

1. ~~`environment-pane.R`~~ -- empty file, dropped 2026-05-19
2. ~~`smoke.R`~~ -- covered by `smoke/startup.test.ts`, dropped 2026-05-19
3. ~~`defer-scope.R`~~ -- framework-internal, dropped 2026-05-19
4. ~~`code-folding.R`, `syntax-highlighting.R`, `sweave.R`~~ -- ported 2026-05-19 via new `AceEditor` page-object wrapper (Ace internals accessible from Playwright via `page.evaluate`)

### Wave 2 -- single-test files (~~one PR per file~~ -- bundled as one PR, ~30 min each)

5. ~~`history.R`, `restart.R`, `build-pane.R`, `ignorefiles.R`, `python.R`, `refactoring.R`, `shinytest2.R`~~ -- all ported 2026-05-19. Added `utils/r.ts` (rStringLiteral, rPathLiteral), renamed `utils/test_files.ts` to `utils/files.ts`, added `AceEditor.getSelectionRanges()`

### Wave 3 -- small partial files

8. ~~`chat.R`, `chat-satellite.R`, `projects.R`, `terminal.R`, `suspend.R`~~ -- ported 2026-05-19. Server fixture updated to support `--auth-none`; `createProjectInNewDir` extended with `withGit` flag
9. ~~`files.R`, `packages-pane.R`, `tabs.R`, `editor.R`~~ -- ported 2026-05-20. Desktop fixture passes `--automation-agent` so tests can drive `window.rstudioCallbacks`; added `typeSlowly`, `AceEditor.find`/`focus`/`insert`/`navigateLineEnd`/`getCursorPosition`, and a `@utils/commands` helper
10. `files-endpoint.R` (1) -- still pending; treat as backend integration test
11. ~~`edit-suggestions.R` (5)~~ -- ported 2026-05-20 as `edit_suggestions.test.ts`; 4 pass and 1 is `test.fixme` (focus bug, see Fixme table). `reformat.R` was completed on its own branch. `console.R` (8) still pending

### Wave 4 -- large delta migrations

11. `data-viewer.R` (7), `debugger.R` (8)
12. `completions.R` (17)
13. `quarto.R` (12), `rmarkdown.R` (18) -- chunk-options popup work likely a shared `actions/` helper

### Wave 5 -- largest

14. `guardrails.R` (31) -- consider splitting into multiple PRs by category (read denials, write denials, system files, path traversal, error messages, binding lifecycle)

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
