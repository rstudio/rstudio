---
name: rstudio-migrate-tests-brat-to-playwright
description: Migrate R/testthat BRAT automation tests to TypeScript/Playwright. Use when converting tests from rstudio/src/cpp/tests/automation/testthat/ to rstudio/e2e/rstudio/tests/.
---

# Migrate BRAT Tests to Playwright

Converts R/testthat BRAT tests from `src/cpp/tests/automation/testthat/` to TypeScript/Playwright tests in `e2e/rstudio/tests/`. Sister skill to `rstudio-migrate-tests-selenium-to-playwright`; many process steps are shared.

## Arguments

The user provides one or more targets:
- A single file: `/rstudio-migrate-tests-brat-to-playwright test-automation-debugger.R`
- A keyword: `/rstudio-migrate-tests-brat-to-playwright completions`
- A status keyword: `/rstudio-migrate-tests-brat-to-playwright next` (pick the next Not Started file from the progress doc)

## Hard Rules

1. **Load skills first** -- Always load `rstudio-create-playwright-tests` before any conversion work.
2. **BRAT is reference, not a blueprint.** BRAT calls internal `.rs.*` APIs and `remote$*` automation methods that have no direct UI equivalent. Use BRAT as the source of truth for *expected values, edge cases, and what behavior to verify* -- then drive the same behavior through the UI in Playwright.
3. **Check the progress doc** -- Before starting, verify the file hasn't already been converted. Located at `e2e/rstudio/docs/MIGRATION_FROM_BRAT_PROGRESS.md`.
4. **Tests must work on Desktop and Server** -- Use the unified fixture (`@fixtures/rstudio.fixture`). Tag with `@desktop_only` / `@server_only` only when the behavior genuinely differs.
5. **A test isn't migrated until it passes.** Run the converted Playwright test against a live RStudio instance. Retry up to 3 times. If still failing, mark it `test.fixme()` with a blocker note and track it as Fixme in the progress doc. Do not list a fixme test as migrated in summaries.
6. **Don't delete BRAT tests until their Playwright counterparts pass.** Removal happens in the same PR, after the new test is green. Never delete first.
7. **No Claude dependency** -- Test infrastructure must run from terminal, CI, and GitHub Actions. Claude subagents are convenience, not required.

## Unportable BRAT tests

Some BRAT tests cannot be driven from the UI -- they call `.rs.*` APIs that have no user-observable effect, or they verify internal state directly. For each:

1. Read the BRAT test carefully. What user-facing behavior, if any, is being verified?
2. Pick a disposition and record it in the progress doc:
   - **Port via UI** -- find the equivalent user interaction and assert on observable outcomes. Default choice.
   - **Convert to C++/R unit test** -- if the logic is pure, port it to the appropriate `src/cpp/tests/testthat/` or Google Test file. Note the new location.
   - **Drop** -- if the behavior isn't user-visible and isn't covered by another layer of testing, delete the BRAT test with a rationale. Requires user approval.

Don't default to Drop. Aim to preserve the behavior coverage.

## Steps

### Step 1: Resolve the target

- Find the file under `src/cpp/tests/automation/testthat/`.
- Read the full R source. Identify each `.rs.test("name", { ... })` block and its expected outcomes.
- Check `MIGRATION_FROM_BRAT_PROGRESS.md` for current status and any known overlap with existing Playwright tests.

### Step 2: Read reference materials

- Load the `rstudio-create-playwright-tests` skill.
- Read the existing Playwright counterpart (if any) -- the progress doc lists likely candidates.
- Read relevant `e2e/rstudio/pages/` and `e2e/rstudio/actions/` helpers.
- For complex behavior (e.g. R Markdown chunk execution, debugger stepping), also check the BRAT helper file `src/cpp/tests/automation/testthat/helper-pane-layout.R` if relevant.
- Check `e2e/rstudio/docs/MIGRATION_FROM_SELENIUM_PROGRESS.md` -- a Selenium test on the same feature may have been ported and tell you what shape the Playwright version should take.

**For large BRAT files (10+ tests):** consider launching a general-purpose subagent to map each `.rs.test()` block to an existing Playwright test (or no-counterpart). The subagent returns a per-test plan that keeps the main context lean for the writing phase.

### Step 3: Determine the target location

Map the BRAT test to the Playwright directory:

| BRAT file (theme) | Playwright location |
|-------------------|---------------------|
| Editor / file types / R Markdown / Quarto / Sweave / refactoring / syntax | `tests/panes/editor/` |
| Console / completions / restart / history | `tests/panes/console/` or `tests/panes/misc/` |
| Debugger | `tests/panes/debugger/` |
| Data viewer | `tests/panes/data-viewer/` |
| Build / packages / environment / files / terminal | `tests/panes/<area>/` (create if missing) |
| Chat / guardrails / chat satellite | `tests/panes/posit-assistant-chat/` |
| Projects | `tests/projects/` |
| Smoke / startup | `tests/smoke/` |

Use judgment. The Playwright structure is organized by user-facing area, not by BRAT file layout.

### Step 4: Categorize each BRAT test

For each `.rs.test()` block, decide:

- **Already covered** -- a Playwright test exercises the same user-facing behavior. Verify by reading the candidate. If genuinely covered, mark the BRAT test for deletion (no porting work).
- **Partial** -- behavior is covered but a specific assertion is missing. Add the missing assertion to the existing Playwright test or write a focused new one.
- **Not covered** -- port from scratch.
- **Unportable** -- pick a disposition per the section above.

Record each decision in the progress doc.

### Step 5: Write the TypeScript test(s)

Follow the rules in `rstudio-create-playwright-tests`.

BRAT-specific translation guidance:

| BRAT | Playwright |
|------|------------|
| `remote$console.execute("expr")` | `consoleActions.typeInConsole(page, "expr")` |
| `remote$console.executeExpr({ ... })` | Build the R expression as a string, then `typeInConsole`. Use template literals; keep indentation minimal |
| `remote$console.getOutput()` | Read `#rstudio_console_output` text via locator |
| `remote$editor.openWithContents(".R", contents, cb)` | Create file in sandbox, `File > Open` or use the `editorActions` helpers |
| `remote$commands.execute("commandName")` | Prefer the UI path (button, menu, keyboard shortcut). Fall back to `typeInConsole(`.rs.api.executeCommand("commandName")`)` when UI is tangential to the test |
| `remote$dom.clickElement("#sel")` | `await page.locator('#sel').click()` |
| `remote$dom.elementExists("#sel")` | `await expect(page.locator('#sel')).toBeVisible()` (or `.toHaveCount(0)` for absence) |
| `remote$dom.waitForElement("#sel")` | `await expect(page.locator('#sel')).toBeVisible({ timeout: TIMEOUTS.foo })` |
| `remote$keyboard.insertText("text", "<Enter>")` | `pressSequentially("text")` then `sleep(200)` + `keyboard.press("Enter")` |
| `remote$keyboard.executeShortcut("Cmd + Shift + P")` | `keyboard.press("ControlOrMeta+Shift+P")` -- see the create-playwright skill for the Cmd vs Ctrl rules |
| `remote$completions.request("stats::rn")` | Drive completions via UI: type into editor, wait for popup, read items |
| `remote$session.restart()` | UI: `Session > Restart R` or `keyboard.press("ControlOrMeta+Shift+F10")`; assert with `consoleActions.waitForSessionReady()` |
| `remote$project.create(...)` | New Project wizard via `projectActions` |
| `remote$satellites.switchTo("...")` | Open a new Playwright Page handle for the satellite window |
| `.rs.uiPrefs$foo$set(value)` | `.rs.api.writeRStudioPreference("foo", value)` via console, with `clear()` translated to writing the default in `afterAll` |
| `.rs.heredoc("...")` | Plain backtick template literal |
| `.rs.waitUntil("reason", predicate)` | Built-in `expect().toBeVisible()` / `toContainText()` retry. Use polling helpers only when no DOM signal exists |
| `withr::defer(...)` (per-test cleanup) | `afterEach` hook, or per-test cleanup via `await using` patterns |
| `withr::defer(.rs.automation.deleteRemote())` (file-level) | No equivalent needed -- the fixture handles teardown |
| `.rs.markers("wip", "editor")` | `test.fixme()` for wip; tags for categorization (`{ tag: ['@editor'] }`) |

If the BRAT source has several near-identical `.rs.test()` blocks, consider data-driven restructuring (one `forEach` loop) in the Playwright version.

### Step 6: Add missing page objects or actions

If the test needs locators or methods not yet available:
- Add locators to the appropriate file in `pages/`
- Add interaction methods to the appropriate file in `actions/`
- Follow existing patterns

### Step 7: Run the test

**Hard gate (Hard Rule 5):** a test doesn't count as migrated until it passes here.

Defer to `rstudio-run-playwright-tests` for the canonical run commands (Desktop and Server). Present the command and wait for user approval before executing.

Quick reference for Desktop:

```bash
cd e2e/rstudio && npx playwright test tests/<path>/<file>.test.ts
```

If the test fails:
- Review error output
- Fix and re-run (up to 3 attempts)
- If still failing, `test.fixme()` the failing test and note the blocker in the progress doc

### Step 8: Delete the BRAT test(s)

Once the Playwright counterpart is green:

- Delete the migrated `.rs.test()` block from the BRAT file.
- If the BRAT file is now empty (no remaining `.rs.test()` blocks), delete the whole file.
- If the file becomes empty after this PR but you want to defer the deletion, that's fine -- track it in the progress doc and clean up later.

Never leave a migrated BRAT test in place "for safety" -- the goal is full deprecation.

### Step 9: Reevaluate

After the test passes, review for:

1. **Code clarity** -- Remove unnecessary variables, simplify patterns
2. **Idiomatic patterns** -- Check against the list in the create-playwright skill
3. **Selector quality** -- Check against the selector hierarchy
4. **Playwright + TypeScript conventions** -- async/await, type annotations, built-in assertions, lifecycle hooks
5. **Duplication** -- Could any new helpers move to actions/?

Present proposed improvements to the user. Apply only if approved. Re-run after changes.

### Step 10: Update progress tracking

Edit `e2e/rstudio/docs/MIGRATION_FROM_BRAT_PROGRESS.md`:
- Update the file's status (Complete, Partial, Dropped, or Fixme)
- Add the Playwright target path(s) and test count
- Add notes for unportable tests (disposition + reasoning)
- Add fixme tests to the Fixme Tests table with blocker description

### Step 11: Commit and submit

Each substep requires explicit approval before proceeding.

**11.1 Check branch.** Run `git branch --show-current`. Branch prefix: `test/migrate-brat-`. Kebab-case for the descriptive part (e.g., `test/migrate-brat-debugger`). Never commit directly to main.

**11.2 Review changes.** Run `git fetch origin`. Run `git status` (never `-uall`), `git diff`, `git log --oneline -5`. Summarize what changed. Flag anything that shouldn't be committed.

**11.3 Stage and commit.** Stage specific files by name (never `git add -A`). Draft a commit message focused on the "why" -- concise, single line preferred, lowercase first letter (per project convention), no Co-Authored-By. Show the message, wait for approval. Commit, then `git status`.

**11.4 Push.** Show the push command, wait for approval, push with `-u`. Never push to main directly.

**11.5 Create PR.** Draft PR title (under 70 chars, lowercase) and summary. Include an `## Intent` section if the migration addresses a specific issue. Ask about reviewers. Show full PR details, wait for approval. Create with `gh pr create`. Add the `automation` label. Return the PR URL.

**11.6 Merge** (only if the user asks to merge this session).

### Step 12: Roborev review and assessment

Invoke the `roborev-review` skill on the commit. Assess findings before presenting:
- Read each finding in actual code context
- Judge real severity; don't blindly trust roborev's label
- Fix High and Medium findings
- Fix Low findings only if they're real bugs or silent failures
- Skip cosmetic Lows and "add tests for simple scripts" suggestions

Apply fixes manually. Do not invoke `roborev-fix`. Re-run the Playwright test after fixes. Re-run `roborev-review` on the new commit. Repeat until no actionable findings remain. Close reviewed findings via `roborev-respond`.

## Compact Instructions

If context compacts mid-migration, preserve:
- **Source and target paths** for the file being migrated
- **Per-test status**: which `.rs.test()` blocks ported (passed Step 7), which became `test.fixme()` and why, which were dropped/converted
- **Page object / action additions** made during the migration
- **Open roborev findings** in progress: job ID, finding being fixed, severity assessment
- **Open blockers** flagged for the user (issues to file, upstream bugs found)

If a step has been completed, that fact also survives -- don't redo work the user has already approved.

## Output

After conversion, summarize:
- BRAT source file and `.rs.test()` blocks handled
- Playwright target file(s) and location
- Page objects or actions added/modified
- Test results (passed / fixme with blockers / dropped with rationale)
- BRAT lines removed and whether the BRAT file is now empty
- Improvements made during reevaluation
- MIGRATION_FROM_BRAT_PROGRESS.md updates
