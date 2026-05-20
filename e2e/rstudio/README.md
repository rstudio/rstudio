# RStudio Playwright Test Suite

End-to-end tests for [RStudio](https://github.com/rstudio/rstudio) using [Playwright](https://playwright.dev/). Tests are designed to run against **RStudio Desktop** (via Chrome DevTools Protocol) on Windows, macOS, and Linux, and **RStudio Server** (via a private in-tree `rserver-dev`, or against an external server) on Linux. Development and testing have primarily been done on Desktop so far. Server mode is supported but less exercised.

## Setup

From this directory (`e2e/rstudio`):

```bash
npm install
npx playwright install
```

### Ubuntu 26.04 LTS

Playwright doesn't yet ship a browser build or system-deps list for Ubuntu 26.04 (the release renamed SONAME-versioned libraries: `libicu74` -> `libicu78`, `libxml2` -> `libxml2-16`, etc.). Until Playwright catches up, point the installer at the Ubuntu 24.04 browser build and install the renamed system libraries by hand:

```bash
# Intel/AMD64
PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=ubuntu24.04-x64 npx playwright install

# ARM64
PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=ubuntu24.04-arm64 npx playwright install

sudo apt-get install libicu78 libxml2-16 libmanette-0.2-0
```

Remove the override once Playwright publishes an Ubuntu 26.04 build.

### Prerequisites

- **Desktop mode**: RStudio Desktop installed at the default path for your OS
- **Server mode**: By default, an in-tree `rserver-dev` built at `build/src/cpp/server/rserver` (run `cmake --build build` first). To target an external server instead, set `PW_RSTUDIO_SERVER_URL`; credentials are needed only if that server presents a login form.
- **Authentication**: The test suite does not currently automate sign-in to external services. Tests that require authentication (e.g., Posit Assistant, GitHub Copilot) assume the running RStudio instance is already signed in.

## Running Tests

### Pick a mode

The suite has two Playwright projects: `desktop` (default) and `server`. Each filters tests automatically by edition (via `PW_RSTUDIO_EDITION`, default `os`) and by OS -- `desktop` from the host's `os.platform()`, `server` always against Linux.

### Desktop Mode (Default)

No flag needed--`desktop` is the default project.

```bash
# All tests
npx playwright test

# Specific test file
npx playwright test tests/panes/misc/autocomplete.test.ts

# All tests in a folder (recursive)
npx playwright test tests/panes/posit-assistant-chat/

# Specific test by name
npx playwright test -g "test name here"
npx playwright test -g "base function: cat("

# With extra RStudio CLI args
PW_RSTUDIO_EXTRA_ARGS="--my-flag --other-option" npx playwright test

# Pro edition
PW_RSTUDIO_EDITION=pro npx playwright test
```

The desktop fixture automatically launches RStudio with CDP enabled on a random port (9231-9299), connects Playwright, and shuts down gracefully after tests complete. Override the port with `PW_CDP_PORT=9222`.

### Server Mode

Pass `--project=server`. By default the fixture spawns a private in-tree `rserver-dev` per worker (using `build/src/cpp/server/rserver` and `build/src/cpp/conf/rserver-dev.conf`), launches a headed Chromium, and connects to it -- credentials aren't needed because the spawned server uses `--auth-none`. Build the server first with `cmake --build build`. Override the binary and conf paths with `PW_RSERVER_BIN` / `PW_RSERVER_CONF` if needed.

```bash
# Spawn in-tree rserver-dev (default)
npx playwright test --project=server

# All tests in a folder (recursive)
npx playwright test tests/panes/posit-assistant-chat/ --project=server
```

To skip the spawn and target an external server (e.g. CI, or a remote box), set `PW_RSTUDIO_SERVER_URL`. Credentials are required only when the external server presents a login form; `PW_RSTUDIO_SERVER_PORT` overrides the port in the URL when set.

```bash
PW_RSTUDIO_SERVER_URL=http://10.0.0.1 \
  PW_RSTUDIO_SERVER_PORT=80 \
  PW_RSTUDIO_SERVER_USER=myuser \
  PW_RSTUDIO_SERVER_PASSWORD=mypass \
  npx playwright test --project=server

# Pro edition
PW_RSTUDIO_EDITION=pro \
  PW_RSTUDIO_SERVER_URL=http://10.0.0.2 \
  PW_RSTUDIO_SERVER_PORT=80 \
  PW_RSTUDIO_SERVER_USER=myuser \
  PW_RSTUDIO_SERVER_PASSWORD=mypass \
  npx playwright test --project=server
```

`PW_RSTUDIO_MODE=desktop|server` is a fallback when `--project` isn't passed; if both are set, `--project` wins.

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

The unified fixture (`rstudio.fixture.ts`) reads the per-project `mode` option (set in `playwright.config.ts`) and delegates to the appropriate launcher. It provides a shared `rstudioPage` (a Playwright `Page`) scoped to the worker, so all tests in a file share one RStudio session.

- **Desktop**: Kills any process on the CDP port, spawns RStudio with `--remote-debugging-port`, connects via `chromium.connectOverCDP()`, waits for the console to be ready.
- **Server**: When `PW_RSTUDIO_SERVER_URL` is unset, spawns a private in-tree `rserver-dev` per worker with sandboxed env (`--auth-none`, isolated `HOME` / config / data dirs). Otherwise connects to the configured external URL. Either way, launches a headed Chromium, navigates to the server, and -- only if a login form is presented -- fills in `PW_RSTUDIO_SERVER_USER` / `_PASSWORD` before waiting for the IDE to load.

Both fixtures dismiss leftover save dialogs from previous runs.

Before any worker spawns, a Playwright `globalSetup` hook (`fixtures/sandbox-setup.ts`) creates a per-invocation **sandbox** directory under `os.tmpdir()` (or under `PW_SANDBOX_ROOT` if set) and exports its absolute path as the internal `PW_SANDBOX` env var. Every test-side artifact lives inside it. A matching `globalTeardown` (`fixtures/sandbox-teardown.ts`) removes the entire subtree at end of run -- unless a test failed or `PW_SANDBOX_SKIP_CLEANUP` is set, in which case the path is logged and the contents are left in place for triage. The auto-created `pw_sandbox_<rand>` subtree is the only thing that ever gets removed; if you set `PW_SANDBOX_ROOT` yourself, that parent directory is never touched.

The Desktop fixture launches RStudio against per-spec subdirs under the sandbox, set via env vars and the Electron `--user-data-dir` switch. With `<sandbox>` = `$PW_SANDBOX` and `<rand>` = a random suffix:

| Setting | Value |
|----------|-------|
| `RSTUDIO_CONFIG_ROOT` | `<sandbox>/config_<rand>` |
| `RSTUDIO_CONFIG_HOME` | `<sandbox>/config_<rand>/config-home` |
| `RSTUDIO_CONFIG_DIR` | `<sandbox>/config_<rand>/config-dir` |
| `--user-data-dir` (Electron) | `<sandbox>/config_<rand>/electron-userdata` |
| `RSTUDIO_DATA_HOME` | `<sandbox>/data-home` (shared across specs in the invocation) |
| `HOME` / `USERPROFILE` | `<sandbox>/user-home` (shared across specs in the invocation) |

The `rstudio-prefs.json` (under `RSTUDIO_CONFIG_HOME`) is generated by merging defaults from `fixtures/base-prefs.jsonc` with an optional override from `PW_RSTUDIO_PREFS_OVERRIDE`; override values win per-key. The user's real RStudio profile and home dotfiles are untouched.

Because each Desktop launch has its own `--user-data-dir`, RStudio's single-instance attaching no longer applies between worker launches -- each spawn is a fresh, independent IDE process.

The shared `data-home/`, `user-home/`, `HOME`, and `USERPROFILE` are safe under the suite's `workers: 1` setting (see `playwright.config.ts`). If parallel workers are ever enabled, those would need to move back to per-spec subtrees or be partitioned per worker -- they aren't safe for concurrent writers as-is.

`PW_SANDBOX` resolves to a runner-side path. When the rsession runs on the same host as the test runner (Desktop, or Server pointed at `localhost`), the R workdir is created inside `PW_SANDBOX`, so `globalTeardown` removes it as part of the umbrella cleanup. When the rsession runs on a remote host (Server pointed at a non-`localhost` URL), `PW_SANDBOX` doesn't exist on the rsession filesystem, so the R workdir is created under R's own `dirname(tempdir())` instead. That keeps Server tests working against remote rsession, with one caveat: remote R-side workdirs aren't covered by `globalTeardown` and will accumulate on the rsession host across runs.

`HOME` / `USERPROFILE` point at a sandboxed `user-home/`. By default nothing is seeded there, so user dotfiles (`~/.Rprofile`, `~/.Renviron`, `~/.R/`, `~/.gitconfig`, `~/.ssh/`, etc.) are absent for tests, and Posit Assistant and GitHub Copilot tests start unauthenticated. Set `PW_SANDBOX_SEED_POSITAI=1` to seed `~/.positai/` so Posit Assistant tests start signed in, and set `PW_SANDBOX_SEED_COPILOT=1` to seed the real Copilot credentials so Copilot tests start authenticated -- both with the privacy caveat noted in the env var table below.

On Windows, `globalSetup` pre-creates `user-home/AppData/Roaming/` and `user-home/AppData/Local/` because Electron's `app.getPath('appData')` fails at startup if those subdirs don't exist under the redirected `USERPROFILE` (the "Failed to get 'appData' path" popup). The same kind of scaffolding will likely be needed for macOS Desktop (`~/Library/Application Support`) and Linux Desktop (`~/.config`) when those platforms get tested.

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

Specs that create files or directories on disk must route those writes into a **workdir** -- a unique per-spec subdirectory under the per-invocation sandbox at `$PW_SANDBOX` -- so tests don't pollute `~/`, the repo tree, or wherever R's cwd happens to be.

```typescript
import { useSuiteSandbox } from '@utils/sandbox';

const sandbox = useSuiteSandbox();

test('writes land in sandbox', async ({ rstudioPage: page }) => {
  await typeInConsole(page, `writeLines("hi", "${sandbox.dir}/foo.txt")`);
});
```

`useSuiteSandbox()` registers a `beforeAll` hook that creates the workdir under `$PW_SANDBOX` and `setwd()`s into it. `sandbox.dir` is populated before any test runs; use it when you need an absolute path. If you only need relative paths to land in the workdir, calling `useSuiteSandbox();` and discarding the return value is enough -- cwd is already redirected. There is no `afterAll` cleanup: the `globalTeardown` removes the entire `$PW_SANDBOX` subtree at end of run.

**Wizard-driven tests:** the New Project Wizard's parent-directory field is read-only. `setwd()` doesn't redirect it -- override the `default_project_location` preference via `.rs.api.writeRStudioPreference()` and restore it in `afterAll`. See `create_projects.test.ts` for the pattern.

**Tests that exercise a user-home-relative product path** (e.g. `chat-user-skills.test.ts` exercises `~/.positai/skills/`) should resolve `~` against the sandbox's redirected user-home, not against the runner's `os.homedir()`. The rstudio child process and its descendants (Databot, etc.) inherit `HOME` / `USERPROFILE` = `$PW_SANDBOX/user-home`, so the absolute path to use is computed from `process.env.PW_SANDBOX`. Guard against the env var being unset so a setup-ordering regression fails loud rather than producing `undefined/user-home`:

```ts
const PW_SANDBOX = process.env.PW_SANDBOX;
if (!PW_SANDBOX) throw new Error('PW_SANDBOX is not set');
const USER_HOME = path.join(PW_SANDBOX, 'user-home');
// ... then path.join(USER_HOME, '.positai', 'skills', ...)
```

Don't compute paths from Node's `os.homedir()` -- that returns the host home, which the rstudio child can't see.

**Low-level helper:** `createSandbox` is also exported from `@utils/sandbox` if you need to create an extra workdir under `$PW_SANDBOX` without going through `useSuiteSandbox()`.

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
| `@ai` | Test exercises an AI feature (Copilot ghost text / NES, Posit Assistant chat). Useful for skipping AI-dependent suites in offline or no-credential runs. |

### Filtering by Tag

The config defines two **projects** -- `desktop` and `server` -- each with a `grepInvert` computed at config load:

- **OS**: `desktop` excludes OS-only tags that don't match the host (`os.platform()`); `server` always excludes `@windows_only|@macos_only` (server targets Linux).
- **Edition**: `PW_RSTUDIO_EDITION=os` (default) excludes `@pro_only`; `PW_RSTUDIO_EDITION=pro` excludes `@os_only`.
- **Mode**: each project also excludes the opposite mode's tag (`@server_only` from `desktop`, `@desktop_only` from `server`).

Select a project with `--project`:

```bash
npx playwright test --project=desktop
npx playwright test --project=server
```

`PW_RSTUDIO_MODE=desktop|server` is a fallback for shell aliases when `--project` isn't passed. **`--project` wins** when both are set.

You can also filter manually with `--grep` and `--grep-invert`:

```bash
# Run only tests with a specific tag
npx playwright test --grep @desktop_only

# Exclude a single tag
npx playwright test --grep-invert @pro_only

# Exclude multiple tags
npx playwright test --grep-invert "@pro_only|@server_only"

# Skip AI-dependent tests (Copilot/NES + Posit Assistant chat)
npx playwright test --grep-invert @ai
```

You can also exclude tests by file path with `PW_TEST_IGNORE` (whitespace-separated globs):

```bash
# Exclude a single file
PW_TEST_IGNORE="**/code_suggestions.test.ts" npx playwright test

# Exclude an entire directory
PW_TEST_IGNORE="**/posit-assistant-chat/**" npx playwright test

# Combine both
PW_TEST_IGNORE="**/code_suggestions.test.ts **/posit-assistant-chat/**" npx playwright test
```

#### Summary: four ways to filter

|               | Include                                              | Exclude                                                    |
|---------------|------------------------------------------------------|------------------------------------------------------------|
| By file path  | `npx playwright test foo.test.ts` (positional arg)   | `PW_TEST_IGNORE="**/foo.test.ts" npx playwright test`      |
| By test title | `npx playwright test --grep "pattern"`               | `npx playwright test --grep-invert "pattern"`              |

Include sets the candidate pool; exclude trims it. When both apply, exclude wins.

## Environment Variables

| Variable | Mode | Required | Description |
|----------|------|----------|-------------|
| `PW_RSTUDIO_MODE` | Both | No | Fallback for `--project` (`desktop` or `server`); `--project` wins if both set. Default: `desktop`. |
| `PW_RSTUDIO_EDITION` | Both | No | `os` (default) or `pro`. Filters out `@pro_only` or `@os_only` tagged tests. |
| `PW_CDP_PORT` | Desktop | No | Override the CDP port (default: random 9231-9299) |
| `PW_RSTUDIO_SERVER_URL` | Server | No | Full URL, e.g., `http://10.0.0.1:8787`. If unset, a private in-tree `rserver-dev` is spawned per worker. |
| `PW_RSTUDIO_SERVER_PORT` | Server | No | Override the port in the URL |
| `PW_RSTUDIO_SERVER_USER` | Server | Conditional | Login username. Required only when the external server presents a login form; ignored for the in-tree spawn (`--auth-none`). |
| `PW_RSTUDIO_SERVER_PASSWORD` | Server | Conditional | Login password. Same conditional rule as `PW_RSTUDIO_SERVER_USER`. |
| `PW_RSTUDIO_SERVER_LOGIN_TIMEOUT` | Server | No | Post-login wait for the IDE console, in ms (default: 60000) |
| `PW_RSERVER_BIN` | Server | No | Path to the `rserver` binary used by the in-tree spawn (default: `build/src/cpp/server/rserver` in the repo). |
| `PW_RSERVER_CONF` | Server | No | Path to the `rserver-dev.conf` used by the in-tree spawn (default: `build/src/cpp/conf/rserver-dev.conf` in the repo). |
| `PW_RSTUDIO_EXTRA_ARGS` | Desktop | No | Space-separated extra CLI args passed to RStudio (e.g., `--my-flag --other-option`) |
| `PW_RSTUDIO_PREFS_OVERRIDE` | Desktop | No | Path to a JSON/JSONC file whose keys override `fixtures/base-prefs.jsonc` per-key. |
| `PW_TEST_IGNORE` | Both | No | Space-separated globs of test paths to exclude (e.g., `**/foo.test.ts **/posit-assistant-chat/**`). Fills the gap that Playwright has no CLI option for path-based exclusion -- file inclusion uses positional CLI arguments, and title filtering uses `--grep`/`--grep-invert`. |
| `PW_SANDBOX_ROOT` | Both | No | Parent directory under which the per-invocation sandbox is created. Defaults to `os.tmpdir()`. |
| `PW_SANDBOX_ROOT_CREATE` | Both | No | Set to `true`/`1` to auto-create `PW_SANDBOX_ROOT` if missing. Default `false` -- fails loud on typos. |
| `PW_SANDBOX_SKIP_CLEANUP` | Both | No | Set to `true`/`1` to preserve the sandbox at end of run regardless of pass/fail. |
| `PW_SANDBOX_SEED_POSITAI` | Both | No | Set to `true`/`1` to copy the real `~/.positai/` into the sandbox so Posit Assistant tests start signed in. Default unseeded (signed out). Privacy: tokens land inside the sandbox and persist on failed runs until you delete it -- only set this on machines using a dedicated test account. |
| `PW_SANDBOX_SEED_COPILOT` | Both | No | Set to `true`/`1` to copy the real GitHub Copilot credentials into the sandbox (`%LOCALAPPDATA%\github-copilot\` on Windows, `~/.config/github-copilot/` on macOS/Linux) so Copilot tests start authenticated. Default unseeded (unauthenticated). Privacy: tokens land inside the sandbox and persist on failed runs until you delete it -- only set this on machines using a dedicated test account. |

`PW_SANDBOX` itself is internal: it's set by `globalSetup` to the absolute path of the auto-created sandbox subtree and is read by workers, the R workdir helper, and `globalTeardown`. Don't set it manually.

## Variable Helpers

The examples above use shell-prefix syntax (`KEY=val cmd ...`) which works in bash/zsh but not in `cmd.exe` or PowerShell. Two helpers are wired in for cases where you want a per-run setup without permanently changing your shell or Windows user environment.

### `cross-env` (inline, cross-shell)

`cross-env` parses leading `KEY=VAL` tokens on any shell, so the bash-style examples above work verbatim from PowerShell or `cmd.exe`. Combine it with `--project` to pick the project in the same line:

```powershell
npx cross-env PW_RSTUDIO_EDITION=pro npx playwright test --project=desktop
```

On bash/zsh it passes through unchanged; the native `PW_RSTUDIO_EDITION=pro npx playwright test --project=desktop` already works. Its reason for existing is `cmd.exe`/PowerShell, where the prefix syntax otherwise fails.

### `.env.local` (dotenv)

For a saved per-target setup instead of a long command line, `playwright.config.ts` loads `dotenv` before reading any env vars. Create `e2e/rstudio/.env.local` (gitignored) with whatever you want available to the run:

```
PW_RSTUDIO_MODE=server
PW_RSTUDIO_EDITION=pro
PW_RSTUDIO_SERVER_URL=http://10.0.0.5
PW_RSTUDIO_SERVER_USER=myuser
PW_RSTUDIO_SERVER_PASSWORD=mypass
```

Then just run:

```bash
npx playwright test
```

Notes:
- The file is loaded only when Playwright reads its config, so vars never leak into your shell.
- Vars already set in the shell (including via `cross-env`) win over values in the file; the file provides defaults, not overrides.
- Point at a different file with `PW_ENV_FILE=.env.staging npx playwright test`. Useful for keeping per-target files (`.env.10-0-0-5`, `.env.pro-staging`).
- A committed `.env.example` template documents the available keys.

## Claude Skills

Claude skills are available for use when working with this test suite:

- `.claude/skills/rstudio-create-playwright-tests/SKILL.md` — guide for writing tests
- `.claude/skills/rstudio-create-playwright-tests/logic-deep-dive.md` — architectural reasoning
- `.claude/skills/rstudio-run-playwright-tests/SKILL.md` — test execution protocol

---

[*Fear not till Birnam Wood do come to Dunsinane*](https://www.folger.edu/explore/shakespeares-works/macbeth/read/5/5/#:~:text=Fear%C2%A0not%C2%A0till%C2%A0Birnam%C2%A0Wood%0A%C2%A0Do%C2%A0come%C2%A0to%C2%A0Dunsinane)
