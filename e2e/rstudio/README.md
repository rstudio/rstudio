# RStudio Playwright Test Suite

End-to-end tests for [RStudio](https://github.com/rstudio/rstudio) using [Playwright](https://playwright.dev/). Tests are designed to run against **RStudio Desktop** (via Chrome DevTools Protocol) on Windows, macOS, and Linux, and **RStudio Server** (via browser login) on Linux. Development and testing have primarily been done on Desktop so far. Server mode is supported but less exercised.

## Setup

From this directory (`e2e/rstudio`):

```bash
npm install
npx playwright install
```

### Prerequisites

- **Desktop mode**: RStudio Desktop installed at the default path for your OS
- **Server mode**: A running RStudio Server instance with valid credentials
- **Authentication**: The test suite does not currently automate sign-in to external services. Tests that require authentication (e.g., Posit Assistant, GitHub Copilot) assume the running RStudio instance is already signed in.

## Running Tests

### Desktop Mode (Default)

No special environment variables needed — `RSTUDIO_EDITION` defaults to `desktop`.

```bash
# All tests
npx playwright test

# Specific test file
npx playwright test tests/panes/misc/autocomplete.test.ts

# Specific test by name
npx playwright test -g "test name here"
npx playwright test -g "base function: cat("

# With extra RStudio CLI args
RSTUDIO_EXTRA_ARGS="--my-flag --other-option" npx playwright test
```

The desktop fixture automatically launches RStudio with CDP enabled on a random port (9231-9299), connects Playwright, and shuts down gracefully after tests complete. Override the port with `CDP_PORT=9222`.

### Server Mode

Set `RSTUDIO_EDITION=server` and provide credentials:

```bash
RSTUDIO_EDITION=server \
  RSTUDIO_SERVER_URL=http://10.0.0.1:8787 \
  RSTUDIO_USER=myuser \
  RSTUDIO_PASSWORD=mypass \
  npx playwright test
```

### Viewing the Report

After a test run, open the HTML report with:

```bash
npx playwright show-report
```

### Configuration

The Playwright config (`playwright.config.ts`) runs tests **sequentially** (1 worker, no parallel) with a 5-minute timeout per test and no retries by default. Add `--retries 1` if flakiness is expected.

## Project Structure

```
e2e/rstudio/
├── fixtures/               # Test fixtures (session lifecycle)
├── pages/                  # Page objects (locators and low-level helpers)
├── actions/                # Higher-level actions (multi-step operations)
├── utils/                  # Shared utilities (timeouts, constants, helpers)
├── tests/                  # Test specs, organized by feature area
├── playwright.config.ts
├── tsconfig.json
└── package.json
```

### Path Aliases

The `tsconfig.json` defines path aliases so imports stay clean:

| Alias | Directory |
|-------|-----------|
| `@fixtures/*` | `fixtures/*` |
| `@pages/*` | `pages/*` |
| `@actions/*` | `actions/*` |
| `@utils/*` | `utils/*` |

## Architecture

### Fixtures

The unified fixture (`rstudio.fixture.ts`) checks `RSTUDIO_EDITION` and delegates to the appropriate launcher. It provides a shared `rstudioPage` (a Playwright `Page`) scoped to the worker, so all tests in a file share one RStudio session.

- **Desktop**: Kills any process on the CDP port, spawns RStudio with `--remote-debugging-port`, connects via `chromium.connectOverCDP()`, waits for the console to be ready.
- **Server**: Launches a headed Chromium browser, navigates to the server URL, fills in credentials, waits for the IDE to load.

Both fixtures dismiss leftover save dialogs and set `save_workspace` to `"never"` to prevent prompts on shutdown.

### Page Objects

Page objects define **locators and low-level helpers** — where elements are and how to interact with them at the DOM level. Example: `typeInConsole(page, command)`.

### Actions

Actions define **higher-level operations** that compose multiple page-object calls. Example: `consoleActions.ensurePackages(['dplyr'])` checks for a package, installs it if missing, and verifies success.

## Writing Tests

### Basic Structure

```typescript
import { test, expect } from '@fixtures/rstudio.fixture';

test.describe('Feature name', () => {
  test('does something', async ({ rstudioPage: page }) => {
    // test code
  });
});
```

### Conventions

- **Parallel safety**: Default to `test.describe()`. Use `test.describe.serial()` only when tests share state (e.g., multi-turn chat conversations).
- **Failing tests**: Use `test.fixme()` — never comment tests out.
- **File naming**: Test files use snake_case: `feature_name.test.ts`.
- **Temporary files**: Use timestamps (`test_${Date.now()}.R`) to avoid collisions.

### Selector Hierarchy

Prefer selectors in this order:

1. **Stable RStudio IDs** — e.g., `#rstudio_console_input`
2. **ARIA / role-based** — `getByRole()`, `aria-label`
3. **CSS attribute selectors** — `[class*=...]`, `[id^=...]`
4. **XPath** — only when CSS can't express it
5. **Never** `gwt-uid-XXXX` — these change on every restart

### Ace Editor Interactions

The console and source editor use Ace, which has a hidden textarea behind an overlay:

- **Always** `click({ force: true })` on `.ace_text-input` — the overlay intercepts normal clicks
- **Use `pressSequentially()`** for typing in the console/editor — ensures character-by-character detection
- **Never** use `ace.edit(element)` — it reinitializes the editor and breaks Copilot/NES

### Cross-Platform

Tests run on Windows, macOS, and Linux. Key rules:

- Use `ControlOrMeta` for shortcuts that are Cmd on macOS and Ctrl elsewhere (e.g., `ControlOrMeta+s` for Save).
- Use plain `Control` for shortcuts that are literally Ctrl on every platform, including macOS (e.g., `Control+Enter` for Run Line, `Control+l` for Clear Console). Using `ControlOrMeta` for these fires Cmd on macOS, which does something else entirely.
- Use `process.platform` guards for platform-branched keys like `Control+End` (Windows/Linux) vs `Meta+ArrowDown` (macOS).

### Package Dependencies

```typescript
test.describe('Tests needing packages', () => {
  let missingPackages: string[] = [];
  test.beforeAll(async ({ rstudioPage: page }) => {
    missingPackages = await consoleActions.ensurePackages(['dplyr']);
  });
  test('my test', async ({ rstudioPage: page }) => {
    test.skip(missingPackages.length > 0, `Missing: ${missingPackages.join(', ')}`);
    // ...
  });
});
```

## Tags

Tests use [Playwright tags](https://playwright.dev/docs/test-annotations#tag-tests) to indicate platform, edition, and product tier constraints. Apply tags via the `tag` option on `test()` or `test.describe()`:

```typescript
test.describe('Feature', { tag: ['@desktop_only'] }, () => { ... });
test('specific test', { tag: ['@macos_only'] }, async ({ rstudioPage: page }) => { ... });
```

### Available Tags

| Tag | Meaning |
|-----|---------|
| `@parallel_safe` | Test can run in parallel without interfering with other tests |
| `@serial` | Test must run sequentially (modifies shared state, restarts sessions, etc.) |
| `@macos_only` | Test only applies on macOS |
| `@windows_only` | Test only applies on Windows |
| `@linux_only` | Test only applies on Linux |
| `@desktop_only` | Test only applies to RStudio Desktop |
| `@server_only` | Test only applies to RStudio Server |
| `@pro_only` | Test requires RStudio Pro |
| `@os_only` | Test only applies to open-source RStudio |

### Filtering by Tag

The config defines **projects** that automatically exclude tags not applicable to a given environment. Use `--project` to select one:

```bash
npx playwright test --project=desktop-pro-windows
npx playwright test --project=server-pro-linux
```

To avoid passing `--project` every time, set `PW_PROJECT` in your shell profile:

```bash
export PW_PROJECT=desktop-pro-windows
```

With `PW_PROJECT` set, bare `npx playwright test` runs only that project. To switch projects, override `PW_PROJECT` inline:

```bash
PW_PROJECT=server-pro-linux RSTUDIO_EDITION=server npx playwright test
```

Without `PW_PROJECT` or `--project`, all 8 projects run.

**Note:** `PW_PROJECT` and `--project` conflict--don't use both at the same time. `PW_PROJECT` pre-filters the project list, so `--project` can't select anything outside it. To use `--project`, unset `PW_PROJECT` first.

Available projects: `desktop-os-windows`, `desktop-os-macos`, `desktop-os-linux`, `desktop-pro-windows`, `desktop-pro-macos`, `desktop-pro-linux`, `server-os-linux`, `server-pro-linux`.

You can also filter manually with `--grep` and `--grep-invert`:

```bash
# Run only tests with a specific tag
npx playwright test --grep @desktop_only

# Exclude a single tag
npx playwright test --grep-invert @pro_only

# Exclude multiple tags
npx playwright test --grep-invert "@pro_only|@server_only"
```

## Environment Variables

| Variable | Mode | Required | Description |
|----------|------|----------|-------------|
| `PW_PROJECT` | Both | No | Select a single project (e.g., `desktop-pro-windows`). Trumps `--project`. |
| `RSTUDIO_EDITION` | Both | No | `desktop` (default) or `server` |
| `CDP_PORT` | Desktop | No | Override the CDP port (default: random 9231-9299) |
| `RSTUDIO_SERVER_URL` | Server | No | Full URL, e.g., `http://10.0.0.1:8787` (default: `http://localhost:8787`) |
| `RSTUDIO_SERVER_PORT` | Server | No | Override the port in the URL |
| `RSTUDIO_USER` | Server | Yes | Login username |
| `RSTUDIO_PASSWORD` | Server | Yes | Login password |
| `RSTUDIO_LOAD_TIMEOUT` | Server | No | IDE load timeout in ms (default: 60000) |
| `RSTUDIO_EXTRA_ARGS` | Desktop | No | Space-separated extra CLI args passed to RStudio (e.g., `--my-flag --other-option`) |

## Further Reading

For detailed patterns, edge cases, and the reasoning behind these conventions, see the Claude skill files:

- `.claude/skills/rstudio-create-playwright-tests/SKILL.md` — comprehensive guide to writing tests
- `.claude/skills/rstudio-create-playwright-tests/logic-deep-dive.md` — architectural reasoning
- `.claude/skills/rstudio-run-playwright-tests/SKILL.md` — test execution protocol

---

[*Fear not till Birnam Wood do come to Dunsinane*](https://www.folger.edu/explore/shakespeares-works/macbeth/read/5/5/#:~:text=Fear%C2%A0not%C2%A0till%C2%A0Birnam%C2%A0Wood%0A%C2%A0Do%C2%A0come%C2%A0to%C2%A0Dunsinane)
