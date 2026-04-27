---
name: rstudio-migrate-tests-selenium-to-playwright
description: Migrate Python Selenium/Selene electron tests to TypeScript/Playwright. Use when converting tests from rstudio-ide-automation/rstudio_server_pro/electron-tests/ to rstudio/e2e/rstudio/tests/.
---

# Migrate Electron Tests to Playwright

Converts Python test files from `rstudio-ide-automation/rstudio_server_pro/electron-tests/` to TypeScript/Playwright tests in `rstudio/e2e/rstudio/tests/`.

## Arguments

The user provides one or more targets:
- A single file: `/rstudio-migrate-tests-selenium-to-playwright test_desktop_console.py`
- A directory: `/rstudio-migrate-tests-selenium-to-playwright GlobalPrefs/`
- A keyword: `/rstudio-migrate-tests-selenium-to-playwright citations`

## Hard Rules

1. **Load skills first** — Always load `rstudio-create-playwright-tests` before doing any conversion work.
2. **Check BRAT for ground truth** — Before writing assertions, check `rstudio/src/cpp/tests/automation/testthat/` for existing R-based tests that cover the same functionality. They contain expected values and test patterns.
3. **Check MIGRATION_FROM_SELENIUM_PROGRESS.md** — Before starting, verify the file hasn't already been converted or partially converted. Located at `e2e/rstudio/docs/MIGRATION_FROM_SELENIUM_PROGRESS.md`.
4. **Tests must work on Desktop and Server** — Use the unified fixture (`@fixtures/rstudio.fixture`). Don't hardcode Desktop-only patterns.
5. **No Claude dependency** — All test infrastructure must be runnable from terminal, CI, and GitHub Actions. Claude subagents are a convenience layer, not a requirement.

## Steps

### Step 1: Resolve the target

- Search for the file under `electron-tests/`
- Read the full Python source — understand test methods, fixtures, imports, page objects
- Check `e2e/rstudio/docs/MIGRATION_FROM_SELENIUM_PROGRESS.md` for current status

### Step 2: Read reference materials

- Load `rstudio-create-playwright-tests` skill
- Check BRAT (`rstudio/src/cpp/tests/automation/testthat/`) for related R-based test automation
- Check `rstudio-ide-automation/rstudio_server_pro/electron-tests/` for other Selenium/Selene electron tests covering the same functionality — useful for test logic, assertions, and expected values
- Check `rstudio-ide-automation/rstudio_server_pro/tests/` for Selenium/Selene server tests covering the same functionality — useful for test logic, assertions, and expected values
- Check `rstudio-pro/e2e/` for Workbench e2e patterns (examples include `.or()` locator chaining, `clickIfVisible()` helpers, input fill with retry, reload-on-hang recovery) -- look for anything else reusable
- Read relevant existing Playwright page objects in `e2e/rstudio/pages/`
- Read relevant existing Playwright actions in `e2e/rstudio/actions/`

### Step 3: Determine the target location

Map the electron-test to the Playwright directory structure:

| Electron Location | Playwright Location |
|-------------------|---------------------|
| Root-level console/terminal tests | `tests/panes/console/` |
| EditorPane/ tests | `tests/panes/editor/` |
| GlobalPrefs/ tests | `tests/panes/preferences/` |
| Licensing/ tests | `tests/licensing/` |
| Projects/ tests | `tests/projects/` |
| Cross-pane tests (autocomplete, etc.) | `tests/panes/misc/` |

Use judgment — the Playwright structure is organized by function, not by the electron-tests layout.

### Step 4: Write the TypeScript test

Follow the rules in the `rstudio-create-playwright-tests` skill (already loaded per Hard Rule 1). That skill covers imports, selector hierarchy, idiomatic patterns, parallel safety, package dependencies, cross-platform considerations, Ace/NES patterns, and CDP setup.

Migration-specific guidance:
- Use `test.fixme()` for tests that can't pass yet -- never comment out or omit.
- If the Selenium source has multiple near-identical methods, consider data-driven restructuring (one `forEach` loop) in the Playwright version.

### Step 5: Add missing page objects or actions

If the test needs locators or methods not yet available:
- Add locators to the appropriate file in `pages/`
- Add interaction methods to the appropriate file in `actions/`
- Follow existing patterns in those files

### Step 6: Run the test

Defer to the `rstudio-run-playwright-tests` skill for the canonical run commands (Desktop and Server). Present the command and wait for user approval before executing.

Quick reference for Desktop:

```bash
cd e2e/rstudio && npx playwright test tests/<path>/<file>.test.ts --project desktop-os-windows
```

If the test fails:
- Review error output
- Fix and re-run (up to 3 attempts)
- If still failing, use `test.fixme()` for the failing test and note the blocker

### Step 7: Reevaluate

After the test passes, review for:

1. **Code clarity** — Remove unnecessary variables, simplify patterns, ensure descriptive test names
2. **Idiomatic patterns** — Check against the list in Step 4
3. **Selector quality** — Check against the hierarchy in Step 4
4. **Playwright + TypeScript conventions** — Verify the code follows standard Playwright and TypeScript conventions (e.g., proper async/await usage, correct type annotations, Playwright's built-in assertions over manual checks, proper use of lifecycle hooks, no floating promises). Consult official Playwright docs and TypeScript best practices if uncertain.
5. **Duplication** — Could any new helpers be shared via actions classes?

Present proposed improvements to the user. Only apply if approved. Re-run after changes.

### Step 8: Update progress tracking

Edit `e2e/rstudio/docs/MIGRATION_FROM_SELENIUM_PROGRESS.md`:
- Update the file's status (Complete, Partial, or Fixme)
- Add the Playwright target path and test count
- Add notes if relevant
- Add any fixme tests to the Fixme Tests table with blocker description

## Server Mode Parallelism

Tests can run against multiple RStudio Server instances in parallel. This works from terminal, CI, or Claude subagents.

**Targeting a server:**
```bash
RSTUDIO_SERVER_URL=https://server1:80 RSTUDIO_EDITION=server \
  npx playwright test tests/panes/misc/autocomplete.test.ts
```

**Parallel execution (5 servers):**
Each process gets its own `RSTUDIO_SERVER_URL` — no shared config files, no conflicts:

```bash
# Terminal / CI / shell script
RSTUDIO_SERVER_URL=https://server1:80 RSTUDIO_EDITION=server npx playwright test tests/file1.test.ts &
RSTUDIO_SERVER_URL=https://server2:80 RSTUDIO_EDITION=server npx playwright test tests/file2.test.ts &
RSTUDIO_SERVER_URL=https://server3:80 RSTUDIO_EDITION=server npx playwright test tests/file3.test.ts &
wait
```

**Claude subagent execution:**
Same commands, but each Agent tool call targets a different server with `run_in_background: true`. Results are collected and aggregated by the parent agent.

**Desktop mode is unaffected** — it doesn't use `RSTUDIO_SERVER_URL`. The env var is only read by the server fixture path.

## Output

After conversion, summarize:
- Source file and methods converted
- Playwright target file and location
- Page objects or actions added/modified
- Test results (passed/fixme with blockers)
- Improvements made during reevaluation
- MIGRATION_FROM_SELENIUM_PROGRESS.md updates
