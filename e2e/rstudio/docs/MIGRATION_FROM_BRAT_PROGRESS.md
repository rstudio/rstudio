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
| Files Complete | 3 |
| Files Dropped | 3 |
| Files In progress | 0 |
| Files Not started | 26 |

Phase 1 audit complete (2026-05-19). Phase 2 (per-file migration) underway.

## Conversion Status

### Editor (8 files, 60 tests)

| BRAT Source | Tests | Counterpart(s) | Status | Per-test (C/P/N/U) | Notes |
|-------------|------:|---------------|--------|-------------------:|-------|
| test-automation-editor.R | 4 | -- | Not started | 0/0/4/0 | Whitespace-on-save, whole-word replace, Ace shortcuts in console, findFromSelection |
| test-automation-code-folding.R | 2 | code-folding.test.ts (2) | Complete | 0/0/2/0 | Ported via Playwright -- `AceEditor` wrapper exposes `getFoldWidget`/`getFoldWidgetRange` via `page.evaluate`. BRAT file deleted |
| test-automation-edit-suggestions.R | 5 | code_suggestions.test.ts, copilot_ghost_text.test.ts | Partial | 0/2/3/0 | Accept/persist cases overlap; 3 token-introspection tests need fresh UI-level ports. BRAT injects via internal `.rs.api.showEditSuggestion`; consider preserving that deterministic mechanism via `executeCommand` in Playwright |
| test-automation-syntax-highlighting.R | 11 | syntax-highlighting.test.ts (11) | Complete | 0/0/11/0 | All ported via `AceEditor` wrapper (`getTokens`, `getTokenAt`, `getState`, `getFoldWidget`). Color tests assert `bg` on `string.color` tokens directly. BRAT file deleted |
| test-automation-rmarkdown.R | 18 | rmarkdown.test.ts (7), multiline_chunk_execution.test.ts (1), notebook_save_during_execution.test.ts (1) | Partial | 1/1/16/0 | Biggest single porting opportunity: chunk-options popup UI (~6 tests, all DOM-driven), visual-mode round-trips, chunk-widget visibility, error-halt, history-recall, paged-table, patchwork. 9 of 18 are `skip_on_ci()` |
| test-automation-quarto.R | 12 | quarto.test.ts (1), multiline_chunk_execution.test.ts (1) | Partial | 0/1/8/3 | Mirrors Rmd chunk-options gap (`.qmd` variants). Raw HTML/LaTeX block preservation via visual mode is portable. 3 token/fold-widget tests are unportable. 7 of 12 are `skip_on_ci()` |
| test-automation-sweave.R | 2 | sweave.test.ts (2) | Complete | 0/0/2/0 | Ported via `AceEditor.getTokens`/`getMarkers`. BRAT file deleted |
| test-automation-reformat.R | 6 | air_formatting.test.ts (10) | Partial | 2/1/3/0 | Air-formatter cases well covered. Styler tests (3) and Windows-specific newline regression (#17471) need porting. Note BRAT sets prefs via `.rs.uiPrefs$*$set()`; Playwright drives the Options dialog (the real user path) |

### Console, completions, debugger, data viewer (5 files, 41 tests)

| BRAT Source | Tests | Counterpart(s) | Status | Per-test (C/P/N/U) | Notes |
|-------------|------:|---------------|--------|-------------------:|-------|
| test-automation-console.R | 8 | console_pane.test.ts (10), console_command_effects.test.ts (8), ansi_erase_in_line.test.ts (8), execute_from_editor.test.ts (2) | Partial | 0/2/6/0 | Biggest gaps in this slice: condition highlighting (errors/warnings/messages spans), `consoleLineLengthLimit` truncation, post-error output (#16337), multi-message annotation, Ctrl+Shift+M/Alt+- shortcuts in console input |
| test-automation-completions.R | 17 | autocomplete.test.ts (12, runs each test in console + editor) | Partial | 5/1/11/0 | Cores covered (#13196, #13291, #12678). Long tail not covered: roxygen tag completions (.R/.Rmd/.qmd), pipebind placeholder, dollar-names types, column-name quoting, pref toggle, R6 active bindings (#14784), multi-line Tab indent. Playwright adds extras (data.table $, Unicode column names) without BRAT counterpart |
| test-automation-restart.R | 1 | -- | Not started | 0/0/1/0 | `.rs.api.restartSession('print(x + y)')` -- exercise the equivalent via `restartR` command + post-restart assertion |
| test-automation-debugger.R | 8 | debugger.test.ts (~15) | Partial | 3/2/3/0 | Core stepping / breakpoints covered (Playwright actually adds Step Into/Out, Continue chains, recover-on-error, env locals, traceback). Gaps: S7 method breakpoints (#16490), package-build paths (#15201 partial), `debugClearBreakpoints` (Desktop messagebox blocker), multi-line `Browse[N]>` regression |
| test-automation-data-viewer.R | 7 | pagination-sorting.test.ts (5) | Partial | 0/2/5/0 | Complementary, not overlapping. Playwright tests pagination; BRAT tests filter/pin/state-persist/XSS-escaping. Big porting opportunity: filter toolbar, pin icon, refresh state persistence, 3-state sort cycle, HTML-special cell/column escaping |

### Chat / Posit Assistant (3 files, 36 tests)

| BRAT Source | Tests | Counterpart(s) | Status | Per-test (C/P/N/U) | Notes |
|-------------|------:|---------------|--------|-------------------:|-------|
| test-automation-chat.R | 2 | open-chat-pane.test.ts, enable-assistant.test.ts, settings-button.test.ts, uninstall-posit-ai.test.ts | Partial | 0/1/1/0 | Port the "no-refresh on Options dismiss" test; add focused restart-survival assertion (uninstall flow covers it partially) |
| test-automation-chat-satellite.R | 3 | detachable-sidebar.test.ts (1, desktop_only) | Partial | 1/1/0/1 | Desktop pop-out/return covered. Missing: command-based variant (`popOutChat`/`returnChatToMain` invoked via UI command). Server-only Chrome-reload-WindowCloseMonitor test is Unportable in spirit (no user gesture) |
| test-automation-guardrails.R | 31 | chat-guardrails.test.ts (11) | Partial | 3/7/17/4 | Largest delta. Port missing read paths (`~/.aws/credentials`, `~/.ssh/config`, `~/.netrc`, `id_rsa`, `.env.local`); missing write paths (`~/.ssh`, `file.create`, `file.remove`, `unlink`, `file.copy`); /etc/passwd, /etc/shadow, /etc/passwd write; path-traversal; structural error message; SSH public-key allow case. 4 binding-lifecycle tests (`injectBindings`/`restoreBindings`/`safeEval`/double-inject) are Unportable -- consider R-level testthat |

### Workbench panes / misc (10 files, ~22 tests)

| BRAT Source | Tests | Counterpart(s) | Status | Per-test (C/P/N/U) | Notes |
|-------------|------:|---------------|--------|-------------------:|-------|
| test-automation-build-pane.R | 1 | -- | Not started | 0/0/1/0 | `testTestthatFile` command + "Test complete" assertion; skipped on CI |
| test-automation-environment-pane.R | 0 | -- | Dropped | -- | Empty file. Deleted 2026-05-19 |
| test-automation-packages-pane.R | 4 | -- | Not started | 0/0/4/0 | MASS attach/detach via pane checkbox; renv variant (#16842). 4th "we reset state" can fold into setup/teardown |
| test-automation-files.R | 3 | -- | Not started | 0/0/3/0 | Virtualized Open File dialog (Server-only, 10k temp files); non-virt variant; autosave-unchanged-doc (#16329) |
| test-automation-files-endpoint.R | 1 | -- | Partial / Unportable-as-UI | 0/1/0/0 | Protocol-level `/files/` cross-site test. Server-only. Better as backend integration test, or use Playwright's `request.fetch()` |
| test-automation-history.R | 1 | -- | Not started | 0/0/1/0 | `timestamp()` console history recall + Up arrow |
| test-automation-ignorefiles.R | 1 | -- | Not started | 0/0/1/0 | Regression for `89f6cef5d8`: `.positai` added to `.gitignore` only after directory exists; wizard-driven |
| test-automation-terminal.R | 2 | -- | Not started | 0/0/2/0 | Create terminal + visibility, run command + `sendTerminalToEditor` |
| test-automation-tabs.R | 5 | panes/layout/panes.test.ts (18), panes/layout/pane_layout.test.ts (16) | Partial | 0/3/2/0 | Three partial overlaps (core tab list, sidebar-width add/remove tab, layoutZoom variants). Two need fresh ports: tab selection aria-selected, `layoutZoomEnvironment`. Uses helper-pane-layout.R |
| test-automation-defer-scope.R | 4 | -- | Dropped | 0/0/0/4 | Tests `.rs.test()` framework + `withr::defer()` semantics. No UI. Deleted 2026-05-19 |

### Projects, runtimes, lifecycle (5 files, 8 tests)

| BRAT Source | Tests | Counterpart(s) | Status | Per-test (C/P/N/U) | Notes |
|-------------|------:|---------------|--------|-------------------:|-------|
| test-automation-projects.R | 3 | create_projects.test.ts (5), project_trust_dialog.test.ts (8) | Partial | 1/1/1/0 | Default-no-git path conceptually covered. Need: git-checkbox path (assert VCS tab appears), ProjectId-on-demand regression |
| test-automation-python.R | 1 | -- | Not started | 0/0/1/0 | `reticulate::repl_python()` + Tab completion + dunder-attribute quoting (#14560); skipped on CI in BRAT |
| test-automation-shinytest2.R | 1 | -- | Partial | 0/1/0/0 | Toolbar-button branch is straightforward; snapshot-review namespace-stub branch is brittle |
| test-automation-refactoring.R | 1 | -- | Partial | 0/1/0/0 | renameInScope across Rmd chunks (#4961). Needs `editor.selection` shim via `page.evaluate` |
| test-automation-smoke.R | 1 | smoke/startup.test.ts (1) | Dropped | 1/0/0/0 | Smoke-tested the BRAT harness itself; Playwright `startup.test.ts` covers RStudio startup. Deleted 2026-05-19 |
| test-automation-suspend.R | 2 | -- | Not started | 0/0/2/0 | Server-mode suspend/resume; loadedNamespaces + attached datasets preserved. Likely `@server_only` |

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

### Wave 2 -- single-test files (one PR per file, ~30 min each)

5. `build-pane.R`, `history.R`, `ignorefiles.R`, `python.R`, `restart.R`, `refactoring.R`, `shinytest2.R`

### Wave 3 -- small partial files

8. `chat.R` (2), `chat-satellite.R` (3), `projects.R` (3), `terminal.R` (2), `suspend.R` (2)
9. `files.R` (3), `files-endpoint.R` (1), `packages-pane.R` (4), `tabs.R` (5), `editor.R` (4)
10. `edit-suggestions.R` (5), `reformat.R` (6), `console.R` (8)

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
| -- | -- | -- | None tracked yet |

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
