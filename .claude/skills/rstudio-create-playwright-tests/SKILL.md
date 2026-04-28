---
name: rstudio-create-playwright-tests
description: Complete guide for creating RStudio Playwright tests in TypeScript. Covers critical rules, stable selectors, code patterns, conventions, and review checklists. Use when writing, reviewing, or migrating Playwright tests for RStudio.
---

# Creating RStudio Playwright Tests

## Critical Rules (Non-Negotiable)

1. **Always clean up stray RStudio processes at script start** – Port 9222 conflicts cause "Target closed" errors
2. **5-second minimum initial wait** before CDP connection – Ensures stable UI initialization
3. **Use stable selectors** from source code, NOT dynamic `gwt-uid-XXXX` IDs – These change on every restart
4. **Use `pressSequentially()` for all GWT text inputs** – Console, editor, and dialog/wizard `input.gwt-TextBox` fields all need character-by-character input. `fill()` doesn't fire GWT key events (can leave OK buttons disabled). Use `keyboard.type()` only for non-GWT inputs like the command palette search box.
5. **Add 0.2-second delay before `keyboard.press("Enter")`** – Critical for reliability. After typing, wait 0.2s before pressing Enter to ensure the keystroke is fully processed.
6. **Graceful shutdown with `q(save = "no")`** – Better than force termination. Use `save = "no"` to skip the "Save workspace?" dialog.
7. **Write cross-platform code** – Tests run on Windows, macOS, and Linux. Use `process.platform` for platform-specific logic. Never hardcode platform-specific paths or commands without platform guards.

   **Keyboard shortcuts — two rules:**

   **Default: use `ControlOrMeta`** for shortcuts that are Cmd on macOS and Ctrl on Windows/Linux. Playwright maps this automatically.

   | `ControlOrMeta` examples | What it does |
   |--------------------------|--------------|
   | `ControlOrMeta+s` | Save |
   | `ControlOrMeta+a` | Select all |
   | `ControlOrMeta+;` | Accept Copilot/NES suggestion |
   | `ControlOrMeta+Shift+m` | Insert pipe |

   **Exception: use plain `Control`** for shortcuts that are literally Ctrl on *every* platform, including macOS. Using `ControlOrMeta` for these will fire Cmd on macOS, which does something else entirely.

   | Always `Control` | Why not `ControlOrMeta` |
   |------------------|------------------------|
   | `Control+Space` | `Cmd+Space` = macOS Spotlight |
   | `Control+l` | Clear console — Ctrl on all platforms |
   | `Control+Enter` | Run line — Ctrl on all platforms |

   **Platform-branched shortcuts:** Some Ace editor navigation keys use completely different key combos on macOS. These need a `process.platform` guard — neither `Control` nor `ControlOrMeta` works for both.

   | Windows/Linux | macOS | Action | Helper |
   |---------------|-------|--------|--------|
   | `Control+End` | `Meta+ArrowDown` | Go to end of document | `sourceActions.goToEnd()` |
   | `Control+Home` | `Meta+ArrowUp` | Go to top of document | `sourceActions.navigateToChunkByIndex(0)` or manual guard |

   **Never use raw `Control+End` or `Control+Home`** in tests — always use the helper or a platform guard.

   **If unsure**, check what RStudio's own shortcut settings say. If it shows "Ctrl" on macOS (not Cmd), use `Control`.
8. **Write tests that work on both Desktop and Server** – Tests connect via CDP on Desktop and via browser login on Server, but test logic should be the same. Use stable element IDs instead of wrapper selectors. Use `PW_RSTUDIO_MODE` env var when branching on mode.
9. **Use `.rs.api.executeCommand()` instead of `window.desktopHooks.invokeCommand()`** – `desktopHooks` only exists in Desktop's Electron shell and will crash on Server. `.rs.api.executeCommand()` works in both modes.

---

## Reference Checking (Before Writing Any Code)

**Understand what already exists before writing anything new.**

1. **Check BRAT** — `src/cpp/tests/automation/testthat/` for existing R-based tests. **Use as reference for expected values and test logic, not as blueprint** (BRAT has internal API access; Playwright must interact like a user).
2. **Check existing Playwright tests** — `e2e/rstudio/tests/` for existing solutions
3. **Check page objects & actions** — `e2e/rstudio/pages/` and `e2e/rstudio/actions/` for available tools

---

## Stable Selectors Reference

| Element | Selector |
|---------|----------|
| Console textarea | `#rstudio_console_input .ace_text-input` |
| File menu (toolbar) | `button[aria-label*='File']` |
| R Script option | `#rstudio_label_r_script_command` |
| Source panel | `.rstudio_source_panel` |
| Active tab | `.gwt-TabLayoutPanelTab-selected` |
| Untitled file tab | `.gwt-TabLayoutPanelTab-selected:has-text('Untitled')` |
| All tabs | `.gwt-TabLayoutPanelTabs .gwt-TabLayoutPanelTab` |

### Selector Hierarchy (prefer in order)

1. `#id` — stable RStudio IDs (e.g., `#rstudio_console_input`)
2. `aria-label` / `getByRole()` — accessible attributes
3. CSS attribute selectors — `[class*=...]`, `[id^=...]` for dynamic IDs
4. XPath — only when CSS can't express it
5. **Never** `gwt-uid-XXXX` — changes on restart

---

## Test Structure & Conventions

### Imports

```typescript
import { test, expect } from '@fixtures/rstudio.fixture';
```

### Idiomatic Playwright Patterns

- `await expect(locator).toBeVisible()` — not manual waits + null checks
- `await expect(locator).toContainText('...')` — not extract text then assert
- `expect(items).toEqual(expect.arrayContaining([...]))` — order-independent lists
- `click({ force: true })` on Ace textareas — overlay intercepts normal clicks
- `pressSequentially()` for GWT text inputs whose behavior depends on incremental key events firing (console, editor, most dialog/wizard `input.gwt-TextBox` fields where typing a character enables the OK button, triggers autocomplete, etc.) — `fill()` doesn't fire GWT key events in those cases. This is guidance, not a universal law: inputs driven by a discrete trigger like `press('Enter')` or a button click (e.g., the console Find/Replace bar) typically work fine with `fill()`, and `fill()` has the advantage of replacing existing text instead of appending. If unsure, start with `fill()`; switch to `pressSequentially()` only when the handler doesn't fire
- `test.fixme()` for failing tests — never comment out

### Waits and Timing

**Default to Playwright's built-in assertions** — they retry automatically until the condition is met or the timeout expires. Reach for `sleep()` only when there is no observable DOM/state change to wait on.

| Situation | Do this | Not this |
|-----------|---------|----------|
| Waiting for an element | `await expect(locator).toBeVisible()` | `await sleep(2000)` |
| Waiting for text | `await expect(locator).toContainText(...)` | `await sleep(1000); check text` |
| Waiting for element to disappear | `await expect(locator).not.toBeVisible()` | `await sleep(3000)` |
| Waiting for page/session load | `await expect(page.locator(sel)).toBeVisible({ timeout: TIMEOUTS.sessionRestart })` | `await sleep(5000)` |

**When `sleep()` is legitimate:**
- The 0.2s pause before `keyboard.press("Enter")` after typing (Critical Rule 5 — GWT keystroke processing)
- Short settling delay after a UI action that has no observable DOM change (e.g., after clicking a menu item before the next element appears — keep it to 200–500ms max)
- Polling loops where you must wait between retry attempts

**Always use named constants, never magic numbers:**

```typescript
// Bad
await sleep(3000);

// Good
await sleep(TIMEOUTS.sessionRestart);
```

Add new keys to `utils/constants.ts` `TIMEOUTS` object rather than scattering raw numbers. If a value is used only once and is truly a one-off, add a comment explaining the constraint.

See `utils/constants.ts` for the full list of `TIMEOUTS` keys.

### Parallel Safety

- Default to `test.describe()` (parallel-safe)
- Use `test.describe.serial()` only when tests depend on shared state (e.g., multi-turn conversation)
- Tag accordingly: `{ tag: ['@parallel_safe'] }` or `{ tag: ['@serial'] }`

### Package Dependencies

- Use `consoleActions.ensurePackages()` in `beforeAll`
- Skip tests if packages can't be installed: `test.skip(missingPackages.length > 0, reason)`
- Never chain `install.packages()` with semicolons

### Page Objects, Actions, and Utilities

- **Page-scoped logic** (locators, element-interaction helpers) → `pages/` and `actions/` files.
- **Cross-cutting helpers** (suite-lifecycle utilities, env/fixture concerns, generic test-infra that isn't tied to a specific pane) → `utils/`.
- Follow existing patterns in the appropriate directory.

### Project Isolation

Before writing a new test file, consider whether it needs a dedicated RStudio project for clean state. Tests that involve chat history, project-scoped config files (`air.toml`, `.Rprofile`, `DESCRIPTION`), Build pane, Git pane, or renv benefit from running inside a fresh project.

When project isolation is needed, use a fixed project name per test suite. The lifecycle:
- **`beforeAll`**: delete the project directory if it exists from a previous run, then create fresh
- **`afterEach`**: delete all files inside except `.Rproj`, close all source docs, reset state
- **No `afterAll` cleanup** — the directory lingers until next run's `beforeAll` cleans it up

### Sandbox (R-side scratch directory)

Any spec that creates files or directories on the R-side filesystem must route those writes into a **sandbox** — a unique per-spec subdirectory under the OS temp parent. This prevents tests from polluting `~/`, the repo tree, or wherever R's cwd happened to be at the time.

**Pattern:** add to every spec that creates files/dirs:

```typescript
import { useSuiteSandbox } from '@utils/sandbox';

const sandbox = useSuiteSandbox();

test('writes land in sandbox', async ({ rstudioPage: page }) => {
  await typeInConsole(page, `writeLines("hi", "${sandbox.dir}/foo.txt")`);
});
```

`useSuiteSandbox()` registers `beforeAll`/`afterAll` hooks that create and remove the directory, and `setwd()`s into it. `sandbox.dir` is populated before any test runs; use it when you need an absolute path. If you only need relative paths to land in the sandbox, calling `useSuiteSandbox();` and discarding the return value is enough -- cwd is already redirected.

**Wizard-driven tests:** the New Project Wizard's parent-directory field is read-only. `setwd()` doesn't redirect it — override the `default_project_location` preference via `.rs.api.writeRStudioPreference()` and restore it in `afterAll`. See `create_projects.test.ts` for the pattern.

**Env vars (optional):**
- `PW_SANDBOX` — override the sandbox root. Unset uses `dirname(tempdir())`.
- `PW_SANDBOX_CREATE` — when `"true"`/`"1"`, auto-create an overridden root if missing. Default `false` (fail loud on typos).

**Exception:** tests that specifically exercise a fixed path in `~/` as part of the product behavior under test (e.g., `chat-user-skills.test.ts` exercises `~/.positai/skills/`) stay as-is. Sandbox is for redirecting incidental filesystem writes, not for rewriting product semantics.

**Low-level helpers** (`createSandbox` / `removeSandbox`) are also exported from `@utils/sandbox` if you need to manage the lifecycle manually.

### Cross-Platform Considerations

```typescript
import { execSync } from 'child_process';

// Process cleanup
if (process.platform === 'win32') {
  execSync('taskkill /F /IM rstudio.exe', { stdio: 'ignore' });
} else if (process.platform === 'darwin') {
  execSync('killall RStudio 2>/dev/null', { stdio: 'ignore' });
} else {
  execSync('killall rstudio rsession 2>/dev/null', { stdio: 'ignore' });
}

// RStudio path
const RSTUDIO_PATH = process.platform === 'win32'
  ? 'C:\\Program Files\\RStudio\\rstudio.exe'
  : process.platform === 'darwin'
    ? '/Applications/RStudio.app/Contents/MacOS/RStudio'
    : '/usr/bin/rstudio';
```

Never hardcode Windows-only commands or Chrome options without platform guards.

---

## Console Commands vs. UI Interactions

**Default to GUI interactions.** If there's a button, menu item, or keyboard shortcut for an action, use it. Clicking a button tests more of the real user path and avoids issues with `page.evaluate` or console timing. **Use console shortcuts only when the UI path is slow, flaky, or tangential to what's being tested.**

**Decision order:** GUI button/shortcut > `typeInConsole(".rs.api.executeCommand(...)")` > `page.evaluate()` (last resort)

**Common patterns:**
- **R output in chunks/tests**: Use `print()` over `cat()` when verifying output in console. `cat()` with `\n` escape characters can garble when chunk content passes through writeLines and the notebook cache.
- **Setup/teardown**: Console is usually fine — `writeLines()` + `file.edit()`, `unlink()`, `saveAllSourceDocs` + `closeAllSourceDocs`
- **Opening dialogs**: `.rs.api.executeCommand('showOptions')` is faster than clicking through menus
- **Setting preferences**: Console for preconditions, UI when testing the Options dialog itself
- **Triggering features under test**: Always interact through the UI — if there's a toolbar button (e.g., `[id^='rstudio_tb_popoutchat']`), click it instead of calling the command programmatically

---

## Process Management

### Constants

Import shared constants — don't hardcode values:

- **`TIMEOUTS`** — from `utils/constants.ts`. Use named keys (e.g., `TIMEOUTS.fileOpen`) instead of magic numbers.
- **`RSTUDIO_PATH`, `CDP_PORT`, `CDP_URL`** — from `fixtures/desktop.fixture.ts`.
- **`RSTUDIO_EXTRA_ARGS`** — from `utils/constants.ts`. Space-separated extra CLI args passed to RStudio Desktop at launch.
- **`CODE_SUGGESTION_PROVIDERS`, `CHAT_PROVIDERS`** — from `utils/constants.ts`.

Check the source files for current keys and values.

### Process Cleanup Template

```typescript
import { chromium } from 'playwright';
import type { Browser, Page, BrowserContext } from 'playwright';
import { spawn, execSync } from 'child_process';

// Clean up any existing RStudio processes
try {
  if (process.platform === 'win32') {
    execSync('taskkill /F /IM rstudio.exe', { stdio: 'ignore' });
  } else if (process.platform === 'darwin') {
    execSync('killall RStudio 2>/dev/null', { stdio: 'ignore' });
  } else {
    execSync('killall rstudio rsession 2>/dev/null', { stdio: 'ignore' });
  }
} catch (error) {
  // Process might not exist
}
await new Promise(resolve => setTimeout(resolve, TIMEOUTS.processCleanup));

// Start RStudio with remote debugging enabled
const process = spawn(RSTUDIO_PATH, [`--remote-debugging-port=${CDP_PORT}`]);
await new Promise(resolve => setTimeout(resolve, TIMEOUTS.rstudioStartup));
```

### CDP Connection

```typescript
browser = await chromium.connectOverCDP(CDP_URL);
const contexts: BrowserContext[] = browser.contexts();
const page: Page = contexts[0].pages()[0];
await page.waitForSelector(CONSOLE_SELECTOR, { timeout: TIMEOUTS.consoleReady });
```

---

## Ace Editor Patterns

### Force Click on Textareas

Ace `textarea.ace_text-input` elements have an `ace_content` div overlay that intercepts clicks. Always use `{ force: true }`:

```typescript
await page.locator('#rstudio_console_input .ace_text-input').click({ force: true });
```

**Always use `click({ force: true })` instead of `focus()` for Ace textareas** — `focus()` is unreliable and keystrokes may go to the wrong pane.

### Copilot Ghost Text: Multiple Elements

Ghost text renders as multiple DOM elements. Use `.first()` for visibility and `.allTextContents()` for full text:

```typescript
const ghostText = page.locator('[class*=ace_ghost_text]');
await expect(ghostText.first()).toBeVisible({ timeout: 30000 });
const parts = await ghostText.allTextContents();
const fullText = parts.join('');

// Verify ghost text disappeared after acceptance:
await expect(ghostText).toHaveCount(0, { timeout: 5000 });
```

### Scoping Selectors to Active Editor Tab

With multiple files open, selectors match all tabs. Scope to the active tabpanel:

```
//div[@role='tabpanel' and not(contains(@style, 'display: none'))]//*[starts-with(@id,'rstudio_source_text_editor')]//textarea[contains(@class,'ace_text-input')]
```

The `not(contains(@style, 'display: none'))` filter excludes background tabs.

### Cursor Positioning

Pass JavaScript as a **string** (not arrow function) to avoid TypeScript needing DOM types:

```typescript
await page.evaluate(`
  var editors = document.querySelectorAll('.ace_editor');
  for (var i = 0; i < editors.length; i++) {
    var env = editors[i].env;
    if (env && env.editor) {
      var editor = env.editor;
      var text = editor.getValue();
      if (text.indexOf('your_search_text') !== -1) {
        editor.focus();
        editor.gotoLine(6, 0);
        var Range = ace.require('ace/range').Range;
        editor.selection.setRange(new Range(5, 0, 5, 6));
        break;
      }
    }
  }
`);
```

Then type via the scoped Ace textarea:

```typescript
const aceInput = page.locator(`xpath=${ACE_TEXT_INPUT}`);  // scoped to active tab
await aceInput.pressSequentially('replacement_text');
```

Use `pressSequentially` instead of `keyboard.type` when NES needs to detect individual keystrokes.

**Critical:** Use `element.env.editor` to get the existing editor instance. **Never** use `ace.edit(element)` — it reinitializes the editor and breaks Copilot/NES.

---

## NES (Next Edit Suggestions) UI Structure

NES does **not** use `ace_ghost_text`. It has its own distinct UI:

| Element | Selector | Description |
|---------|----------|-------------|
| Suggestion highlight | `.ace_next-edit-suggestion-highlight` | Marker layer overlay on the target code |
| Suggestion preview | `.ace_lineWidgetContainer .ace_content` | Embedded read-only Ace editor showing the suggested change |
| Apply link | XPath: `//*[text()='Apply']` | Accepts the NES suggestion |
| Discard link | XPath: `//*[text()='Discard']` | Dismisses the NES suggestion |

**Key differences from regular Copilot suggestions:**
- Regular suggestions: inline ghost text (`ace_ghost_text`), accepted with `Ctrl+;`
- NES suggestions: widget below the code with Apply/Discard links, highlighted region in marker layer
- NES only triggers from **keyboard-driven edits** — programmatic edits via `rstudioapi::modifyRange()` do not trigger NES

---

## Test File Naming: Use Timestamps

```typescript
const fileName = `test_${Date.now()}.R`;
```

**Exception:** Files that must have exact names (`air.toml`, `.Rprofile`, `DESCRIPTION`). Clean these up in `afterEach`.

---

## R Package Installation in Automated Tests

Never chain commands after `install.packages` with a semicolon. Always run as standalone.

**Pattern: Check, install if needed, verify**

```typescript
// 1. Check if package is installed (use unique markers to parse console output)
const marker = `__PKG_${Date.now()}__`;
await clearConsole();
await typeInConsole(`cat("${marker}", requireNamespace("${pkg}", quietly = TRUE), "${marker}")`);
await sleep(1000);
const output = await consoleOutput.innerText();
const match = output.match(new RegExp(`${marker}\\s+(TRUE|FALSE)\\s+${marker}`));
if (match?.[1] === 'TRUE') return true; // already installed

// 2. Install — run ALONE, no semicolons
await typeInConsole(`install.packages("${pkg}", repos = "https://cran.r-project.org")`);

// 3. Wait for completion — send marker as SEPARATE command
const doneMarker = `__WHATS_DONE_IS_DONE_${Date.now()}__`;
await typeInConsole(`cat("${doneMarker}")`);

// 4. Poll for done marker
while (Date.now() - startTime < timeoutMs) {
  await sleep(3000);
  const text = await consoleOutput.innerText();
  if (text.includes(doneMarker)) break;
}

// 5. Verify installation succeeded (same marker pattern as step 1)
```

**Usage in tests:**

```typescript
test.describe('Tests needing dplyr', () => {
  let missingPackages: string[] = [];
  test.beforeAll(async () => {
    missingPackages = await consoleActions.ensurePackages(['dplyr']);
  });
  test('my test', async () => {
    test.skip(missingPackages.length > 0, `Missing: ${missingPackages.join(', ')}`);
    // ... test code ...
  });
});
```

**Reference implementation:** `actions/console_pane.actions.ts` → `ensurePackage()` / `ensurePackages()`

---

## Verifying Rendered Text in Viewer Pane

Use helpers from `pages/viewer_pane.page.ts`:

- **`switchToViewerFrame(page)`** — Switch to the viewer iframe for element interactions.

---

## Common Issue: "Target page, context or browser has been closed"

**Causes:** Stray RStudio on port 9222, too short initial wait, conflicting CDP connection.

**Solutions:**
1. Ensure cleanup runs (e.g., `taskkill /F /IM rstudio.exe` on Windows, `killall RStudio` on macOS)
2. Set initial wait to 5+ seconds
3. Verify no other RStudio instances are running

---

## RStudio-Specific Patterns

**Closing source files:** Save first, then close — prevents "Save changes?" dialogs:

```typescript
await typeInConsole(".rs.api.executeCommand('saveAllSourceDocs')");
await sleep(1000);
await typeInConsole(".rs.api.executeCommand('closeAllSourceDocs')");
```

**Separate constants by domain** when the same concept has different values in different contexts (e.g., `CODE_SUGGESTION_PROVIDERS` vs `CHAT_PROVIDERS` in `utils/constants.ts`).

---

## Testing Workflow

1. **Present command, wait for approval** before running
2. Retry up to 3 times on failure
3. Use `test.fixme()` if still failing after 3 attempts

---

## Post-Pass Review (5-point checklist)

After tests pass, review for:

1. **Code clarity** — Remove unnecessary variables, descriptive names
2. **Idiomatic patterns** — Verify against Playwright conventions above
3. **Selector quality** — Verify against selector hierarchy
4. **Playwright + TypeScript conventions** — proper async/await, type annotations, built-in assertions, lifecycle hooks, no floating promises
5. **Duplication** — Extract shared helpers to actions

---

## RPC Interception (page.route) in Electron/CDP

When intercepting rsession RPCs via `page.route()`:

- **Use regex, not globs** — `page.route(/\/rpc\/method_name/, ...)` handles query strings; glob `**/rpc/method_name` may not.
- **Never use `route.fetch()`** — In Electron CDP, `route.fetch()` returns empty response bodies from rsession. Use `route.fulfill()` directly with the known JSON-RPC format:
  ```typescript
  await route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ result: mockPayload }),
  });
  ```
- **RPC URL pattern** — RStudio RPCs use `http://127.0.0.1:<port>/rpc/<method_name>` (e.g., `/rpc/chat_check_for_updates`).

### Chat Pane / Posit AI Specifics

- **Frontend caches the update check** — `chatCheckForUpdates` fires once at startup. Toggling the chat provider preference does NOT re-trigger it. To force a fresh check, post `retry-manifest` from inside the chat iframe:
  ```typescript
  await chatPane.frame.locator('body').evaluate(() => {
    window.parent.postMessage('retry-manifest', '*');
  });
  ```
  The message **must originate from the iframe** — ChatPresenter filters by `event.source`, so `window.postMessage()` from the main page is silently ignored.

- **Running PAI backend overwrites blocking pages** — GWT's `updateFrameContent()` navigates the iframe to about:blank then writes blocking HTML, but `loadUrl()` (from backend polling, client events, or GWT timers) immediately reloads `ai-chat/index.html` on top. For blocking-state tests, inject the HTML directly via `page.evaluate()` after verifying the RPC interception fired.

- **Options dialog has its own install/update flow** — `setChatProvider()` via the Options dialog triggers update/install dialogs that bypass RPC interception. When you need to control the RPC response, set the preference directly:
  ```typescript
  await consoleActions.typeInConsole('.rs.api.writeRStudioPreference("chat_provider", "posit")');
  ```

---

## BRAT: Deepest Source of Truth

BRAT (`src/cpp/tests/automation/testthat/`) is the deepest source of truth for RStudio's intended behavior. These tests capture edge cases and correct output values that aren't documented anywhere else.

**Use BRAT for the "what" — expected values, edge cases, correctness criteria. Solve the "how" in Playwright terms** — using page locators, keyboard input, `page.evaluate()` for Ace editor access, and `.rs.api.executeCommand()` via the console.

---

For detailed explanations of why these patterns work and architectural reasoning, see `./logic-deep-dive.md`
