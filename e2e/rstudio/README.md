# RStudio Playwright Test Suite

End-to-end tests for [RStudio](https://github.com/rstudio/rstudio) using [Playwright](https://playwright.dev/). Tests are designed to run against **RStudio Desktop** (via Chrome DevTools Protocol) on Windows, macOS, and Linux, and **RStudio Server** (via a private in-tree `rserver-dev`, or against an external server) on Linux. Development and testing have primarily been done on Desktop so far. Server mode is supported but less exercised.

## Setup

From this directory (`e2e/rstudio`):

```bash
npm install
npx playwright install
```

### Ubuntu 26.04 LTS

Playwright 1.61 and later natively support Ubuntu 26.04 (Resolute), so this section is only needed for earlier versions of Playwright.

Before 1.61, Playwright did not ship a browser build or system-deps list for Ubuntu 26.04 (the release renamed SONAME-versioned libraries: `libicu74` -> `libicu78`, `libxml2` -> `libxml2-16`, etc.). On those versions, point the installer at the Ubuntu 24.04 browser build and install the renamed system libraries by hand:

```bash
# Intel/AMD64
PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=ubuntu24.04-x64 npx playwright install

# ARM64
PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=ubuntu24.04-arm64 npx playwright install

sudo apt-get install libicu78 libxml2-16 libmanette-0.2-0
```

On pre-1.61 Playwright, remove the override after upgrading to 1.61 or later.

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

The desktop fixture automatically launches RStudio with CDP enabled on a deterministic port (base 9231 + a per-checkout offset + the worker index, so parallel workers and concurrent runs from other checkouts/worktrees never collide), connects Playwright, and shuts down gracefully after tests complete. Override the port with `PW_CDP_PORT=9222`.

#### Against the in-tree dev build

Set `PW_RSTUDIO_DEV=1` to launch the dev build via `npm run start` in `src/node/desktop` instead of the installed RStudio binary. The fixture resolves `src/node/desktop` from this directory using the standard repo layout, so no path needs to be provided.

```bash
PW_RSTUDIO_DEV=1 npx playwright test
PW_RSTUDIO_DEV=1 npx playwright test tests/panes/misc/autocomplete.test.ts
```

Assumes the rest of the product (gwt, the C++ session, etc.) is already built such that `npm run start` in `src/node/desktop` would launch a working IDE. The CDP-wait deadline is extended to 3 minutes on this path to accommodate the first-run webpack compile; subsequent starts are faster. Tests that exercise the doRestart() flow (e.g. uninstall Posit Assistant) aren't fully supported in dev mode because the Electron relaunch spawns the same dev executable rather than a fresh CDP-enabled session.

### Server Mode

Pass `--project=server`. By default the fixture spawns a private in-tree `rserver-dev` per worker (using `build/src/cpp/server/rserver` and `build/src/cpp/conf/rserver-dev.conf`), launches a headed Chromium, and connects to it -- credentials aren't needed because the spawned server uses `--auth-none`. Build the server first with `cmake --build build`. Override the binary and conf paths with `PW_RSERVER_BIN` / `PW_RSERVER_CONF` if needed.

```bash
# Spawn in-tree rserver-dev (default)
npx playwright test --project=server

# All tests in a folder (recursive)
npx playwright test tests/panes/posit-assistant-chat/ --project=server
```

To skip the spawn and target an external server (e.g. CI, or a remote box), set `PW_RSTUDIO_SERVER_URL`. Credentials are required only when the external server presents a login form; `PW_RSTUDIO_SERVER_PORT` overrides the port in the URL when set.

**Prerequisite for external servers**: the rsession processes the target server spawns must run with `--automation-agent`, otherwise `window.rstudio` is never installed and the very first step of `launchServer()` (which calls `setPref()` through the bridge) will time out. The in-tree spawn handles this by passing `--automation-agent=1` to `rserver-dev`, which forwards the flag to every rsession it launches. External servers have to be configured explicitly -- either start `rserver` with `--automation-agent=1`, or add `automation-agent=1` to `rserver.conf`. Servers that aren't dedicated test instances will not have this set by default.

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
npx playwright show-report   # or: npm run test:report
```

Each run (via the `npm run test:*` scripts) writes its HTML report to a
timestamped folder, `playwright-report-<timestamp>/`, so re-running tests never
clobbers an earlier report. `playwright-report` is a symlink kept pointing at
the most recent run, which is what `show-report` opens by default. The runners
print the full path to both on exit. All of these are gitignored; prune old
ones with `rm -rf playwright-report-*` (and `rm -rf test-results/*` for traces
and screenshots).

### Profiling a slow test

When a test passes but is slower than expected, find out *which step* is slow
rather than guessing. A full trace records every action (clicks, `waitFor`,
`expect`, `evaluate`) with begin/end timestamps.

Traces are retained automatically on failure (see `use.trace` in
`playwright.config.ts`), so a failing test already has one. For a passing test,
force one with `--trace on`:

```bash
npm run test:desktop-dev -- tests/path/to/test.test.ts -g "test name" --trace on
```

The fastest way to read per-action timing headlessly (no GUI) is to unzip the
trace and pair the `before`/`after` events in `test.trace` by `callId`:

```bash
TRACE=$(find test-results -name trace.zip | tail -1)
mkdir -p /tmp/tx && unzip -o "$TRACE" -d /tmp/tx >/dev/null
node -e '
const fs = require("fs");
const ev = fs.readFileSync("/tmp/tx/test.trace", "utf8")
  .split("\n").filter(Boolean).map(JSON.parse);
const open = {}, dur = [];
for (const e of ev) {
  if (e.type === "before") open[e.callId] = { t: e.startTime, title: e.title || e.method };
  else if (e.type === "after" && open[e.callId])
    dur.push({ ms: (e.endTime - open[e.callId].t) | 0, title: open[e.callId].title });
}
dur.sort((a, b) => b.ms - a.ms);
for (const r of dur.slice(0, 15)) console.log(String(r.ms).padStart(6), r.title);
'
```

`startTime`/`endTime` are milliseconds. The one-time app launch shows up as
multi-second `Before Hooks` / `beforeAll hook` / `Fixture "rstudioPage"`
entries -- ignore those and look at the actions inside the test body. A common
gotcha this surfaces: calling `locator.inputValue()` (or other
value-reading actions) on an element that isn't in the DOM yet blocks on the
action timeout; probe with `locator.count()` (instant) instead, and open the
editor/popup before reading from it.

For an interactive view (DOM snapshots, network, console per step), use the
GUI trace viewer instead:

```bash
npx playwright show-trace "$TRACE"
```

### Configuration

The Playwright config (`playwright.config.ts`) runs tests **sequentially** (1 worker, no parallel) with a 5-minute timeout per test. Retries default to 0 locally and to 1 under CI (controlled by the standard `CI` env var) -- one retry absorbs a transient launch flake on a fresh runner without rerunning the suite by hand. Override on the CLI with `--retries N`.

## Project Structure

```
e2e/rstudio/
├── fixtures/               # Test fixtures (session lifecycle, sandbox setup/teardown)
├── pages/                  # Page objects (locators and low-level helpers)
├── actions/                # Higher-level actions (multi-step operations)
├── utils/                  # Shared utilities (timeouts, constants, helpers)
├── tests/                  # Test specs, organized by feature area
├── scripts/                # Dev wrappers for the in-tree dev build (test:desktop-dev, test:server-dev)
├── docs/                   # Supplementary documentation
├── DESCRIPTION             # R-style package metadata (lets RStudio treat this dir as a project)
├── playwright.config.ts
├── tsconfig.json
└── package.json
```

The `package.json` exposes a few convenience npm scripts on top of `npx playwright test`:

- `npm test` / `npm run test:desktop` -- runs `--project=desktop`
- `npm run test:server` -- runs `--project=server` pointing `PW_RSERVER_BIN` / `PW_RSERVER_CONF` at an installed Server build
- `npm run test:desktop-dev` / `npm run test:server-dev` -- runs an incremental `cmake --build` so the in-tree C++ session is current, then checks that a usable GWT build is available (devmode process running, or a precompiled bootstrap from `ant draft`) and launches against the in-tree dev build. The GWT check is a probe only -- if neither is present, the wrapper prints a warning telling you to run `ant devmode` or `ant draft` and continues; bring up GWT yourself before invoking these. See `scripts/`.
- `npm run test:report` -- opens the HTML report
- `npm run typecheck` -- `tsc --noEmit`

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

- **Desktop**: Kills any process still *listening* on this worker's CDP port (a leftover RStudio from a prior interrupted run), spawns RStudio with `--remote-debugging-port` and `--automation-agent`, connects via `chromium.connectOverCDP()`, waits for the console to be ready.
- **Server**: When `PW_RSTUDIO_SERVER_URL` is unset, spawns a private in-tree `rserver-dev` per worker with sandboxed env (`--auth-none`, `--automation-agent`, isolated `HOME` / config / data dirs). Otherwise connects to the configured external URL. Either way, launches a headed Chromium, navigates to the server, and -- only if a login form is presented -- fills in `PW_RSTUDIO_SERVER_USER` / `_PASSWORD` before waiting for the IDE to load.

Both fixtures dismiss leftover save dialogs from previous runs.

`--automation-agent` is the flag that makes the IDE testable: rsession's `ApplicationAutomation` only installs `window.rstudio` (the command, preference, and document bridge driven from `@utils/commands`) when this is set. The Desktop fixture polls for `window.rstudio.ready === true` rather than a fixed sleep, which transparently rides out the splash screen and (in GWT super dev mode) the transient "Compiling RStudio" page.

In CI mode, `globalSetup` does a warmup launch+shutdown of Desktop RStudio before any worker starts, so the first real test doesn't pay the cold-cache GWT-ready cost. This adds one extra launch to a CI run; set `PW_WARMUP_LAUNCH=0` to opt out, or `PW_WARMUP_LAUNCH=1` to force-enable it locally. Skipped for Server mode regardless.

Before any worker spawns, a Playwright `globalSetup` hook (`fixtures/sandbox-setup.ts`) creates a per-invocation **sandbox** directory under `os.tmpdir()` (or under `PW_SANDBOX_ROOT` if set) and exports its absolute path as the internal `PW_SANDBOX` env var. Every test-side artifact lives inside it. A matching `globalTeardown` (`fixtures/sandbox-teardown.ts`) removes the entire subtree at end of run -- unless a test failed or `PW_SANDBOX_SKIP_CLEANUP` is set, in which case the path is logged and the contents are left in place for triage. The auto-created `pw_sandbox_<rand>` subtree is the only thing that ever gets removed; if you set `PW_SANDBOX_ROOT` yourself, that parent directory is never touched.

The Desktop fixture launches RStudio against per-spec subdirs under the sandbox, set via env vars and the Electron `--user-data-dir` switch. With `<sandbox>` = `$PW_SANDBOX` and `<rand>` = a random suffix:

| Setting | Value |
|----------|-------|
| `RSTUDIO_CONFIG_ROOT` | `<sandbox>/config_<rand>` |
| `RSTUDIO_CONFIG_HOME` | `<sandbox>/config_<rand>/config-home` |
| `RSTUDIO_CONFIG_DIR` | `<sandbox>/config_<rand>/config-dir` |
| `--user-data-dir` (Electron) | `<sandbox>/config_<rand>/electron-userdata` |
| `RSTUDIO_DATA_HOME` | `<sandbox>/data-home` (shared across specs in the invocation) |
| `HOME` / `USERPROFILE` | `<sandbox>/user-home` (single worker); `<sandbox>/user-home-<parallelIndex>` (parallel) |
| `R_LIBS_USER` | per-host template library (single worker); a per-worker hardlink clone of it (parallel) |

The `rstudio-prefs.json` (under `RSTUDIO_CONFIG_HOME`) is generated by merging defaults from `fixtures/base-prefs.jsonc` with an optional override from `PW_RSTUDIO_PREFS_OVERRIDE`; override values win per-key. The user's real RStudio profile and home dotfiles are untouched.

Because each Desktop launch has its own `--user-data-dir`, RStudio's single-instance attaching no longer applies between worker launches -- each spawn is a fresh, independent IDE process.

Running in parallel: the suite defaults to `workers: 1` (see `playwright.config.ts`), but you can raise it (`npm run test:desktop-dev -- --workers=4`). `fullyParallel` stays off, so parallelism is at the file level -- each worker owns whole spec files and runs them serially. The per-worker CDP / dev-server / logger ports are already derived to avoid collisions; the two shared writers are partitioned per worker only when `workers > 1` (resolved into `PW_TOTAL_WORKERS` by `globalSetup`):

- `HOME` / `USERPROFILE` get a per-worker copy of the seeded template home (`user-home-<parallelIndex>`), so RStudio user state, command history, and seeded AI credentials don't collide.
- `R_LIBS_USER` becomes a per-worker *hermetic* hardlink clone of the prebuilt template library (not layered with it). Each worker can install or remove packages independently -- including the uninstall/reinstall tests -- without racing on a shared library or leaking changes across workers. Clones persist beside the template (`<template>-w<N>`); delete them to force a refresh after changing `REQUIRED_PACKAGES`.

`RSTUDIO_DATA_HOME` and `RSTUDIO_CONFIG_*` are already per-spec, and the sandbox `data-home/pai` is only ever read (symlinked into each spec), so those need no further partitioning. Each worker is a full Electron IDE + R session, so the practical worker ceiling is roughly half the host's cores before contention erodes the gains.

`PW_SANDBOX` resolves to a runner-side path. When the rsession runs on the same host as the test runner (Desktop, or Server pointed at `localhost`), the R workdir is created inside `PW_SANDBOX`, so `globalTeardown` removes it as part of the umbrella cleanup. When the rsession runs on a remote host (Server pointed at a non-`localhost` URL), `PW_SANDBOX` doesn't exist on the rsession filesystem, so the R workdir is created under R's own `dirname(tempdir())` instead. That keeps Server tests working against remote rsession, with one caveat: remote R-side workdirs aren't covered by `globalTeardown` and will accumulate on the rsession host across runs.

`HOME` / `USERPROFILE` point at a sandboxed `user-home/`. By default nothing is seeded there, so user dotfiles (`~/.Rprofile`, `~/.Renviron`, `~/.R/`, `~/.gitconfig`, `~/.ssh/`, etc.) are absent for tests. AI provider credentials are the exception: if the Posit Assistant state dir (`~/.posit/assistant`, or legacy `~/.positai/`) and/or the GitHub Copilot config directory exist on the host they're copied into the sandbox automatically so the `@ai` tests can drive the providers. Tests that need credentials check the matching `PW_AI_SEEDED_POSITAI` / `PW_AI_SEEDED_COPILOT` flag (set by `globalSetup` after a successful copy) via `requireAiCredentials()` and skip with a clean reason when unseeded -- so a missing credential surfaces as "skipped: no credentials seeded" rather than a 5-minute timeout. Set `PW_SANDBOX_NO_SEED_CREDENTIALS=1` to opt out of the auto-copy entirely (useful when the host isn't a dedicated test account); the `@ai` tests then skip across the board.

The Posit Assistant `@ai` tests normally download the official assistant package into the sandbox `data-home`. To run them against a locally built assistant instead, set `PW_SEED_PAI` to an install directory and `globalSetup` seeds it into `data-home/pai` (see Environment Variables).

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
- **Cleanup**: prefer a `beforeAll` calling `consoleActions.resetSourcePane()` over per-test cleanup. That helper leaves the source pane in a single-Untitled-tab state -- it avoids the no-tabs/has-tabs HIDE animation race (#17738) that `closeAllBuffersWithoutSaving` triggers, and gives every test a known starting state. Use `saveAllSourceDocs` → `closeAllSourceDocs` only when the test must flush in-editor edits to disk.
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

`globalSetup` pre-installs a baseline manifest of CRAN packages into the shared `R_LIBS_USER` cache (see *R package pre-population* under Environment Variables). For tests using a package that's already in `REQUIRED_PACKAGES`, no extra setup is required.

For tests using a package outside the manifest, add it via `ensurePackages` in `beforeAll`. The helper installs into the same cache, so subsequent runs are no-ops:

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

If you find yourself adding the same package to many tests, promote it into `REQUIRED_PACKAGES` so it gets pre-installed once at setup time.

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
| `@smoke` | Long, low-information liveness check. Excluded by default (redundant alongside the full suite); opt in with `PW_RUN_SMOKE=1`. |

### Filtering by Tag

The config defines two **projects** -- `desktop` and `server` -- each with a `grepInvert` computed at config load:

- **OS**: `desktop` excludes OS-only tags that don't match the host (`os.platform()`); `server` always excludes `@windows_only|@macos_only` (server targets Linux).
- **Edition**: `PW_RSTUDIO_EDITION=os` (default) excludes `@pro_only`; `PW_RSTUDIO_EDITION=pro` excludes `@os_only`.
- **Mode**: each project also excludes the opposite mode's tag (`@server_only` from `desktop`, `@desktop_only` from `server`).
- **Smoke**: `@smoke` is excluded from both projects by default; set `PW_RUN_SMOKE=1` to include it.

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
| `PW_CDP_PORT` | Desktop | No | Override the CDP port (default: 9231 + per-checkout offset + worker index) |
| `PW_RSTUDIO_DEV` | Desktop | No | Set to `1`/`true` to launch the in-tree dev build via `npm run start` in `src/node/desktop` instead of the installed RStudio binary. Assumes the rest of the product is already built. |
| `PW_RSTUDIO_SERVER_URL` | Server | No | Full URL, e.g., `http://10.0.0.1:8787`. If unset, a private in-tree `rserver-dev` is spawned per worker. |
| `PW_RSTUDIO_SERVER_PORT` | Server | No | Override the port in the URL |
| `PW_RSTUDIO_SERVER_USER` | Server | Conditional | Login username. Required only when the external server presents a login form; ignored for the in-tree spawn (`--auth-none`). |
| `PW_RSTUDIO_SERVER_PASSWORD` | Server | Conditional | Login password. Same conditional rule as `PW_RSTUDIO_SERVER_USER`. |
| `PW_RSTUDIO_SERVER_LOGIN_TIMEOUT` | Server | No | Post-login wait for the IDE console, in ms (default: 60000) |
| `PW_RSERVER_BIN` | Server | No | Path to the `rserver` binary used by the in-tree spawn (default: `build/src/cpp/server/rserver` in the repo). |
| `PW_RSERVER_CONF` | Server | No | Path to the `rserver-dev.conf` used by the in-tree spawn (default: `build/src/cpp/conf/rserver-dev.conf` in the repo). |
| `RSTUDIO_PROJECT_ROOT` | Server | No | Override the repo-root path passed into the in-tree `rserver-dev`'s environment. Defaults to the resolved repository root. |
| `RS_DB_MIGRATIONS_PATH` | Server | No | Override the server database migrations directory passed to the in-tree `rserver-dev`. Defaults to `src/cpp/server/db` under the repo root. |
| `PW_RSTUDIO_EXTRA_ARGS` | Desktop | No | Space-separated extra CLI args passed to RStudio (e.g., `--my-flag --other-option`) |
| `PW_RSTUDIO_PREFS_OVERRIDE` | Desktop | No | Path to a JSON/JSONC file whose keys override `fixtures/base-prefs.jsonc` per-key. |
| `PW_TEST_IGNORE` | Both | No | Space-separated globs of test paths to exclude (e.g., `**/foo.test.ts **/posit-assistant-chat/**`). Fills the gap that Playwright has no CLI option for path-based exclusion -- file inclusion uses positional CLI arguments, and title filtering uses `--grep`/`--grep-invert`. |
| `PW_SANDBOX_ROOT` | Both | No | Parent directory under which the per-invocation sandbox is created. Defaults to `os.tmpdir()`. |
| `PW_SANDBOX_ROOT_CREATE` | Both | No | Set to `true`/`1` to auto-create `PW_SANDBOX_ROOT` if missing. Default `false` -- fails loud on typos. |
| `PW_SANDBOX_SKIP_CLEANUP` | Both | No | Set to `true`/`1` to preserve the sandbox at end of run regardless of pass/fail. |
| `PW_SEED_PAI` | Both | No | Path to a locally built Posit Assistant install directory (e.g. `~/.local/share/rstudio/pai`, as produced by the assistant repo's `npm run deploy:rstudio`). `globalSetup` copies it into the sandbox `data-home/pai`, so tests exercise that local build instead of downloading the official package. Fails loud if the path doesn't contain `bin/package.json`. The seeded build's `package.json` version and `protocol.json` must satisfy the IDE under test, or it will be treated as needing an update. |
| `PW_SANDBOX_NO_SEED_CREDENTIALS` | Both | No | Set to `true`/`1` to opt out of copying real AI credentials into the sandbox. By default, if the Posit Assistant state dir (`~/.posit/assistant`, or legacy `~/.positai/`) or the GitHub Copilot config directory (`%LOCALAPPDATA%\github-copilot\` on Windows, `~/.config/github-copilot/` on macOS/Linux) exists on the host, it's copied into the sandbox so `@ai` tests start authenticated. When this is set (or the source directory is absent), `@ai` tests skip with a clear "no credentials seeded" reason. Privacy: tokens land inside the sandbox and persist on failed runs until you delete it -- set this opt-out on machines that aren't dedicated test accounts. |
| `PW_RSTUDIO_R_LIBS_USER` | Both | No | Override the R user-library template path (passed to rsession as `R_LIBS_USER`). Defaults to `~/.cache/rstudio-playwright/r-libs/%p/%v` on macOS/Linux and `%LOCALAPPDATA%\rstudio-playwright\r-libs\%p\%v` on Windows. R expands `%p` (platform) and `%v` (R x.y) at startup. The library lives outside the per-run sandbox so packages persist between runs. |
| `PW_RSTUDIO_R_LIBS_SKIP_PREP` | Both | No | Set to `true`/`1` to skip globalSetup's pre-population of the user library. Useful when running against an R install that already has everything, or to reproduce the empty-library popup behavior on purpose. |
| `PW_WARMUP_LAUNCH` | Desktop | No | `1`/`true` to force a warmup launch in globalSetup; `0`/`false` to skip it. Default: on under CI, off locally. Skipped entirely in Server mode. |
| `PW_LAUNCH_ATTEMPTS` | Desktop | No | Number of attempts the in-fixture launch retry will make before giving up (default: `2`, i.e. one retry). |
| `PW_GWT_READY_TIMEOUT_MS` | Desktop | No | Override how long the fixture waits for `window.rstudio.ready === true` after CDP connects. Default: `60000` on CI, `30000` locally. |
| `PW_DEBUG_LAUNCH` | Desktop | No | Set to `1`/`true` to emit `[launch-timing]` traces (post-CDP wait steps) during startup. Diagnostic only; enabled on CI so launch failures show which phase exceeded the deadline. |
| `PW_DEBUG_PAGES` | Desktop | No | Set to `1`/`true` to emit `[debug-launch]` page-lifecycle traces (page created/navigated/load/close, console errors) for every page for the whole run, including popups like plot zoom. Diagnostic only. |
| `PW_ENV_FILE` | Both | No | Path (relative to `e2e/rstudio/`) to a dotenv file loaded by `playwright.config.ts` before any env reads. Default: `.env.local`. Setting this to a missing file is a hard error (so typos surface immediately). See *`.env.local` (dotenv)* below. |
| `PW_RUN_SMOKE` | Both | No | Set to `1`/`true` to include `@smoke` tests, which are excluded by default (they idle long enough to be flaky under parallel load). |
| `PW_TRACE` | Both | No | Playwright trace mode (`on`, `off`, `retain-on-failure`, etc.). Default: `retain-on-failure`. Validated against an allow-list so a typo fails loud instead of silently disabling capture. |
| `PW_SCREENSHOT` | Both | No | Playwright screenshot mode (`on`, `off`, `only-on-failure`). Default: `only-on-failure`. Same allow-list validation as `PW_TRACE`. |
| `PW_DEBUG` | Both | No | Set to `1`/`true` to enable the interactive debug harness: `waitForUserConsoleInput()` pauses the test with DevTools open for profiling instead of running through. |
| `PW_DEBUG_TIMEOUT_MS` | Both | No | Fallback timeout (ms) before a `PW_DEBUG` pause auto-resumes. Default: `3600000` (1 hour). |
| `PW_IGNORE_CLIENT_EXCEPTIONS` | Both | No | Set to `1`/`true` to stop the fixture from failing a test on uncaught client-side (GWT) exceptions. |
| `PW_HEARTBEAT_TIMEOUT_SECONDS` | Both | No | Idle window (no output), in seconds, after which the heartbeat run-wrapper treats the run as hung and kills it. Default: `300`. |
| `PW_STOP_GRACE_MS` | Both | No | Grace period (ms) for a spawned process to exit on stop before it's force-killed. Default: `30000`. |
| `PW_PROJECT_LABEL` | Both | No | Label for this run in report/blob file names (e.g. `desktop-macos`, `server-linux`). Defaults to the resolved mode. |
| `PW_PROJECT` | Both | No | **Deprecated.** No longer used; it logs a warning and is ignored. Use `--project=desktop\|server` or `PW_RSTUDIO_MODE` instead. |

`PW_SANDBOX` itself is internal: it's set by `globalSetup` to the absolute path of the auto-created sandbox subtree and is read by workers, the R workdir helper, and `globalTeardown`. Don't set it manually. `PW_AI_SEEDED_POSITAI` / `PW_AI_SEEDED_COPILOT` are also internal: `globalSetup` sets them to `1` for each provider whose credentials were successfully copied; `requireAiCredentials()` reads them to decide whether to skip an `@ai` test. Don't set those manually either.

### R package pre-population

Because the Desktop and Server fixtures redirect `HOME` / `USERPROFILE` into the per-run sandbox, R computes an empty default user library and won't see the host user's installed packages. `globalSetup` works around this by pointing `R_LIBS_USER` at a stable per-host cache (see `PW_RSTUDIO_R_LIBS_USER` above) and pre-installing a manifest of packages that tests use -- the list lives in `REQUIRED_PACKAGES` inside `fixtures/r-libs-setup.ts`. The check is idempotent (`installed.packages()` then `setdiff`), so a warm cache adds almost no startup time; the first run installs ~23 packages from CRAN (or PPM on Linux) and may take a few minutes.

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
- `.claude/skills/rstudio-run-playwright-tests/SKILL.md` — test execution protocol

---

[*Fear not till Birnam Wood do come to Dunsinane*](https://www.folger.edu/explore/shakespeares-works/macbeth/read/5/5/#:~:text=Fear%C2%A0not%C2%A0till%C2%A0Birnam%C2%A0Wood%0A%C2%A0Do%C2%A0come%C2%A0to%C2%A0Dunsinane)
