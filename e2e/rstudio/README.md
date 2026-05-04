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

### Pick a project first

Bare `npx playwright test` runs the suite against all 8 projects sequentially--hours of test time. Always pass `--project=<name>` (or set `PW_PROJECT`) to scope to one configuration.

Available projects: `desktop-os-windows`, `desktop-os-macos`, `desktop-os-linux`, `desktop-pro-windows`, `desktop-pro-macos`, `desktop-pro-linux`, `server-os-linux`, `server-pro-linux`.

### Desktop Mode (Default)

No special environment variables needed--`PW_RSTUDIO_MODE` defaults to `desktop`.

```bash
# All tests
npx playwright test --project=desktop-os-macos

# Specific test file
npx playwright test tests/panes/misc/autocomplete.test.ts --project=desktop-os-windows

# Specific test by name
npx playwright test -g "test name here" --project=desktop-os-linux
npx playwright test -g "base function: cat(" --project=desktop-pro-macos

# With extra RStudio CLI args
PW_RSTUDIO_EXTRA_ARGS="--my-flag --other-option" npx playwright test --project=desktop-pro-windows

# Pro on Linux
npx playwright test --project=desktop-pro-linux
```

The desktop fixture automatically launches RStudio with CDP enabled on a random port (9231-9299), connects Playwright, and shuts down gracefully after tests complete. Override the port with `PW_CDP_PORT=9222`.

### Server Mode

Set `PW_RSTUDIO_MODE=server` and provide credentials. `PW_RSTUDIO_SERVER_URL` defaults to `http://localhost:8787`; `PW_RSTUDIO_SERVER_PORT` overrides the port in the URL when set (no default--if unset, the port comes from the URL).

```bash
PW_RSTUDIO_MODE=server \
  PW_RSTUDIO_SERVER_URL=http://10.0.0.1 \
  PW_RSTUDIO_SERVER_PORT=80 \
  PW_RSTUDIO_SERVER_USER=myuser \
  PW_RSTUDIO_SERVER_PASSWORD=mypass \
  npx playwright test --project=server-os-linux

# Pro on Linux
PW_RSTUDIO_MODE=server \
  PW_RSTUDIO_SERVER_URL=http://10.0.0.2 \
  PW_RSTUDIO_SERVER_PORT=80 \
  PW_RSTUDIO_SERVER_USER=myuser \
  PW_RSTUDIO_SERVER_PASSWORD=mypass \
  npx playwright test --project=server-pro-linux
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

The unified fixture (`rstudio.fixture.ts`) checks `PW_RSTUDIO_MODE` and delegates to the appropriate launcher. It provides a shared `rstudioPage` (a Playwright `Page`) scoped to the worker, so all tests in a file share one RStudio session.

- **Desktop**: Kills any process on the CDP port, spawns RStudio with `--remote-debugging-port`, connects via `chromium.connectOverCDP()`, waits for the console to be ready.
- **Server**: Launches a headed Chromium browser, navigates to the server URL, fills in credentials, waits for the IDE to load.

Both fixtures dismiss leftover save dialogs from previous runs. The Desktop fixture additionally boots RStudio against an isolated, ephemeral config directory under `os.tmpdir()`, plumbed via the `RSTUDIO_CONFIG_HOME`, `RSTUDIO_CONFIG_DIR`, `RSTUDIO_CONFIG_ROOT`, and `RSTUDIO_DATA_HOME` environment variables. Each launch creates a fresh tree (with `<TMP>` = `os.tmpdir()` and `<rand>` = a random suffix):

| Env var | Value |
|---------|-------|
| `RSTUDIO_CONFIG_ROOT` | `<TMP>/pw_rstudio_config_<rand>` |
| `RSTUDIO_CONFIG_HOME` | `<TMP>/pw_rstudio_config_<rand>/config-home` |
| `RSTUDIO_CONFIG_DIR` | `<TMP>/pw_rstudio_config_<rand>/config-dir` |
| `RSTUDIO_DATA_HOME` | `<TMP>/pw_rstudio_config_<rand>/data-home` |

The directory's `rstudio-prefs.json` (under `RSTUDIO_CONFIG_HOME`) is generated by merging defaults from `fixtures/base-prefs.jsonc` with an optional override file from `PW_RSTUDIO_PREFS_OVERRIDE`; override values take precedence per-key. The whole tree is removed on shutdown, so the user's real RStudio profile is untouched.

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
- **Cleanup**: prefer a `beforeAll` calling `consoleActions.closeAllBuffersWithoutSaving()` over per-test cleanup. Use `saveAllSourceDocs` → `closeAllSourceDocs` only when the test must flush in-editor edits to disk.
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

### Sandbox (R-side scratch directory)

Specs that create files or directories on disk must route those writes into a **sandbox** — a unique per-spec subdirectory under the OS temp parent — so tests don't pollute `~/`, the repo tree, or wherever R's cwd happens to be.

```typescript
import { useSuiteSandbox } from '@utils/sandbox';

const sandbox = useSuiteSandbox();

test('writes land in sandbox', async ({ rstudioPage: page }) => {
  await typeInConsole(page, `writeLines("hi", "${sandbox.dir}/foo.txt")`);
});
```

`useSuiteSandbox()` registers `beforeAll`/`afterAll` hooks that create and remove the directory, and `setwd()`s into it. `sandbox.dir` is populated before any test runs; use it when you need an absolute path. If you only need relative paths to land in the sandbox, calling `useSuiteSandbox();` and discarding the return value is enough -- cwd is already redirected. Override the root with `PW_SANDBOX` and `PW_SANDBOX_CREATE`.

**Wizard-driven tests:** the New Project Wizard's parent-directory field is read-only. `setwd()` doesn't redirect it -- override the `default_project_location` preference via `.rs.api.writeRStudioPreference()` and restore it in `afterAll`. See `create_projects.test.ts` for the pattern.

**Exception:** tests that specifically exercise a fixed path in `~/` as part of the product behavior under test (e.g., `chat-user-skills.test.ts` exercises `~/.positai/skills/`) stay as-is. Sandbox is for redirecting incidental filesystem writes, not for rewriting product semantics.

**Low-level helpers:** `createSandbox` / `removeSandbox` are also exported from `@utils/sandbox` if you need to manage the lifecycle manually.

### Overriding RStudio prefs

Suite-wide RStudio prefs come from `fixtures/base-prefs.jsonc`. To override one for a test run without editing that file, point `PW_RSTUDIO_PREFS_OVERRIDE` at a JSON or JSONC file containing the keys to override. The override merges per-key on top of the base, with override values winning.

```bash
PW_RSTUDIO_PREFS_OVERRIDE=/path/to/my-prefs.json npx playwright test ...
```

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
npx playwright test --project=desktop-os-windows
npx playwright test --project=server-os-linux
```

To avoid passing `--project` every time, set `PW_PROJECT` in your shell profile:

```bash
export PW_PROJECT=desktop-os-windows
```

With `PW_PROJECT` set, bare `npx playwright test` runs only that project. To switch projects, override `PW_PROJECT` inline:

```bash
PW_PROJECT=server-os-linux PW_RSTUDIO_MODE=server npx playwright test
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
| `PW_PROJECT` | Both | No | Select a single project (e.g., `desktop-os-windows`). Trumps `--project`. |
| `PW_RSTUDIO_MODE` | Both | No | `desktop` (default) or `server` |
| `PW_CDP_PORT` | Desktop | No | Override the CDP port (default: random 9231-9299) |
| `PW_RSTUDIO_SERVER_URL` | Server | No | Full URL, e.g., `http://10.0.0.1:8787` (default: `http://localhost:8787`) |
| `PW_RSTUDIO_SERVER_PORT` | Server | No | Override the port in the URL |
| `PW_RSTUDIO_SERVER_USER` | Server | Yes | Login username |
| `PW_RSTUDIO_SERVER_PASSWORD` | Server | Yes | Login password |
| `PW_RSTUDIO_SERVER_LOGIN_TIMEOUT` | Server | No | Post-login wait for the IDE console, in ms (default: 60000) |
| `PW_RSTUDIO_EXTRA_ARGS` | Desktop | No | Space-separated extra CLI args passed to RStudio (e.g., `--my-flag --other-option`) |
| `PW_RSTUDIO_PREFS_OVERRIDE` | Desktop | No | Path to a JSON/JSONC file whose keys override `fixtures/base-prefs.jsonc` per-key. |
| `PW_SANDBOX` | Both | No | Override the sandbox root (absolute path). Unset uses `dirname(tempdir())`. |
| `PW_SANDBOX_CREATE` | Both | No | Set to `true`/`1` to auto-create an overridden sandbox root if it's missing. Default `false` — fails loud on typos. |

## Claude Skills

Claude skills are available for use when working with this test suite:

- `.claude/skills/rstudio-create-playwright-tests/SKILL.md` — guide for writing tests
- `.claude/skills/rstudio-create-playwright-tests/logic-deep-dive.md` — architectural reasoning
- `.claude/skills/rstudio-run-playwright-tests/SKILL.md` — test execution protocol

---

[*Fear not till Birnam Wood do come to Dunsinane*](https://www.folger.edu/explore/shakespeares-works/macbeth/read/5/5/#:~:text=Fear%C2%A0not%C2%A0till%C2%A0Birnam%C2%A0Wood%0A%C2%A0Do%C2%A0come%C2%A0to%C2%A0Dunsinane)
