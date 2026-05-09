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
6. **A test isn't migrated until it passes.** Run the converted TypeScript test against a live RStudio instance (Desktop or Server, depending on target). Retry up to 3 times. If it still fails, mark it `test.fixme()` with a blocker note and track it as Fixme in `MIGRATION_FROM_SELENIUM_PROGRESS.md`. Do not list a fixme test as migrated in summaries, PR descriptions, or progress tracking.

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

**For large files or batch migrations:** if the source has many methods (say 10+) or you're migrating a directory, consider launching a general-purpose subagent to analyze the Python source(s) and return a brief covering: test methods to convert, pytest markers / skip reasons, fixtures used, page objects referenced, parallel safety hints, and any unusual patterns. This keeps the main context lean for the writing and running phases. For typical single-file migrations with a handful of methods, do the analysis inline.

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
- When the Python source has `@pytest.mark.skip` or `pytest.skip()`:
  1. Read the skip reason. Check if any linked issue is still open.
  2. If the reason is Selenium-specific, stale, or resolved → write the test like any other and verify it passes.
  3. If the reason still applies (open RStudio bug, missing feature) → `test.fixme()` with the reason / URL preserved as a comment directly above:
     ```typescript
     // @skip: https://github.com/rstudio/rstudio/issues/12345
     test.fixme("test name", async () => {});
     ```
     If the Python skip has only a reason string (no URL), preserve the string verbatim. If the Python source has bare `@skip` with no reason, write `// @skip: no reason given in Python source`.

  Don't auto-fixme based on the marker alone.

### Step 5: Add missing page objects or actions

If the test needs locators or methods not yet available:
- Add locators to the appropriate file in `pages/`
- Add interaction methods to the appropriate file in `actions/`
- Follow existing patterns in those files

### Step 6: Run the test

**Hard gate (Hard Rule 6):** a test doesn't count as migrated until it passes here.

Defer to the `rstudio-run-playwright-tests` skill for the canonical run commands (Desktop and Server). Present the command and wait for user approval before executing.

Quick reference for Desktop:

```bash
cd e2e/rstudio && npx playwright test tests/<path>/<file>.test.ts
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

### Step 9: Commit and submit

Each substep requires explicit approval before proceeding. Ron may stop at any point -- not every migration goes all the way to merge.

**9.1 Check branch.** Run `git branch --show-current`, report the branch, ask Ron to confirm. Branch prefix for Playwright migration work: `test/ron/` (e.g., `test/ron/migrate-debugger`). Kebab-case for the descriptive part. Never commit directly to main.

**9.2 Review changes.** Run `git fetch origin`. Determine the base branch -- never assume `main`. Check `git log --oneline` or ask. For short-lived solo `test/ron/...` branches, defer the base-branch merge to 9.5. Run `git status` (never `-uall`), `git diff`, `git log --oneline -5`. Summarize what changed. Flag anything that shouldn't be committed (secrets, scratch files, unrelated changes).

**9.3 Stage and commit.** Stage specific files by name (never `git add -A`). Draft a commit message focused on the "why" -- concise, single line preferred, uppercase first letter, no Co-Authored-By. Show the message, wait for approval. Commit, then `git status`.

**9.4 Push.** Show the push command, wait for approval, push with `-u`. Never push to main directly.

**9.5 Create PR.** If `origin/<base>` has moved since the last sync, propose `git merge origin/<base>` and wait for approval before running; then push the merge commit. Draft PR title (under 70 chars) and summary. Summary only -- no "Test plan" section, no AI attribution. Ask about reviewers; aliases: Kevin → `kevinushey`, Gary → `gtritchie`, JonV → `jonvanausdeln`, AT → `astayleraz`. Show full PR details, wait for approval. Create with `gh pr create` (use `--reviewer` if specified). Add the `automation` label. Return the PR URL.

**9.6 Merge** (only if Ron asks to merge this session). Check and report source + target branches, confirm both, wait for approval, merge.

**9.7 Branch cleanup** (only after merge). Offer to delete the local branch, wait for explicit approval.

**Style throughout:** never ` - ` (space-hyphen-space) -- use `--` or em dash, no spaces. Never use `#N` numbering in GitHub comments (auto-links to wrong issues) -- use `1.`, `(1)`, or `Finding 1`.

### Step 10: Roborev review and assessment

Invoke the `roborev-review` skill on the commit to request a code review.

When findings come back, **assess them before presenting to Ron**:
- Read each finding in the actual code context
- Verify the finding is real and not a false positive
- Judge real severity; don't blindly trust roborev's label, and don't overstate
- Note any findings you'd skip and why

Present the assessed findings with recommendations. Wait for approval before editing any files. Severity policy:
- Fix High and Medium findings
- Fix Low findings only if they're real bugs or silent failures
- Skip cosmetic Lows and "add tests for simple scripts" suggestions

Apply fixes manually. **Do not invoke `roborev-fix`** -- Ron prefers manual fixes through the migration flow. Re-run the test (Hard Rule 6 still applies). Commit per Step 9. Re-run `roborev-review` on the new commit. Repeat until no actionable findings remain.

Close reviewed findings via the `roborev-respond` skill once they're addressed.

## Compact Instructions

If context compacts mid-migration, preserve:
- **Source and target paths** for each file being migrated
- **Per-method status**: which methods passed Step 6, which ended up `test.fixme()` and why
- **Page object / action additions** made during the migration (so they're not duplicated)
- **Open roborev findings** in progress: job ID, finding being fixed, severity assessment
- **Open blockers** flagged for Ron (issues to file, upstream bugs found)

If a step has been completed, that fact also survives -- don't redo work Ron has already approved.

## Server Mode Parallelism

Tests can run against multiple RStudio Server instances in parallel. This works from terminal, CI, or Claude subagents.

**Targeting a server:**
```bash
PW_RSTUDIO_SERVER_URL=https://server1:80 \
  npx playwright test tests/panes/misc/autocomplete.test.ts --project=server
```

**Parallel execution (5 servers):**
Each process gets its own `PW_RSTUDIO_SERVER_URL` — no shared config files, no conflicts:

```bash
# Terminal / CI / shell script
PW_RSTUDIO_SERVER_URL=https://server1:80 npx playwright test tests/file1.test.ts --project=server &
PW_RSTUDIO_SERVER_URL=https://server2:80 npx playwright test tests/file2.test.ts --project=server &
PW_RSTUDIO_SERVER_URL=https://server3:80 npx playwright test tests/file3.test.ts --project=server &
wait
```

**Claude subagent execution:**
Same commands, but each Agent tool call targets a different server with `run_in_background: true`. Results are collected and aggregated by the parent agent.

**Desktop mode is unaffected** — it doesn't use `PW_RSTUDIO_SERVER_URL`. The env var is only read by the server fixture path.

## Output

After conversion, summarize:
- Source file and methods converted
- Playwright target file and location
- Page objects or actions added/modified
- Test results (passed/fixme with blockers)
- Improvements made during reevaluation
- MIGRATION_FROM_SELENIUM_PROGRESS.md updates
