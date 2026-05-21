---
name: rstudio-run-playwright-tests
description: How to run RStudio Playwright tests in Desktop and Server modes. Use this skill whenever the user asks to run, execute, or launch Playwright tests against RStudio - whether on Desktop (local) or Server (remote). Also use when the user asks about the test command, environment variables, or how to point tests at a server URL. Trigger on phrases like "run the test", "execute on server", "run it on desktop", "run against <IP>", or any request involving npx playwright test for the RStudio test suite.
---

# Running RStudio Playwright Tests

All commands run from the Playwright project directory:
```
e2e/rstudio
```

## Setup

From the Playwright project directory (`e2e/rstudio`):

```bash
npm install
npx playwright install
```

## The Four Run Modes

Tests can run against four different RStudio targets. Each has a dedicated npm script.

| Mode | Command | Platforms |
|------|---------|-----------|
| Installed desktop | `npm run test:desktop` | macOS, Linux, Windows |
| Development desktop build | `npm run test:desktop-dev` | macOS, Linux, Windows |
| Installed server | `npm run test:server` | Linux only |
| Development server build | `npm run test:server-dev` | macOS, Linux (not Windows) |

**Before running tests, always ask the user which of these four modes they want** (use `AskUserQuestion` if available; filter out modes that aren't supported on the host platform). The first three steps of the test protocol below cover this.

You can append a specific test path or any Playwright flag after the script name:

```bash
# Specific test file
npm run test:desktop -- tests/path/to/test.test.ts

# Match by name
npm run test:server-dev -- -g "test name here"

# Pro edition
PW_RSTUDIO_EDITION=pro npm run test:desktop

# Extra RStudio CLI args (desktop modes only)
PW_RSTUDIO_EXTRA_ARGS="--my-flag --other-option" npm run test:desktop
```

### Installed desktop (`npm run test:desktop`)

Launches the installed RStudio Desktop at the OS-default path:
- macOS: `/Applications/RStudio.app/Contents/MacOS/RStudio`
- Linux: `/usr/bin/rstudio`
- Windows: `C:\Program Files\RStudio\rstudio.exe`

Connects via CDP. The fixture handles launching and teardown automatically. The installed binary won't reflect uncommitted edits to GWT/Java, C++ session, or Electron code -- use the dev build for that.

### Development desktop build (`npm run test:desktop-dev`)

Sets `PW_RSTUDIO_DEV=1` and launches the in-tree dev build via `npm run start` (electron-forge) inside `src/node/desktop`, so the test sees your uncommitted changes.

Prerequisites (run from the **repo root**, not `e2e/rstudio`):

1. C++ session built -- `cmake --build build` (one-time, plus after C++ edits)
2. GWT transpiled to JS -- `(cd src/gwt && ant draft)` (re-run after Java edits; takes 2-5 min)
3. Electron deps installed -- `(cd src/node/desktop && npm ci)` (one-time)

The first `npm run start` also runs a webpack compile (~2 min). The fixture extends its CDP-connect deadline to 3 minutes on this path. Subsequent starts are faster.

Note: tests that exercise the `doRestart()` flow (e.g. uninstall Posit Assistant) aren't fully supported in this mode because Electron's relaunch spawns the same dev executable rather than a fresh CDP-enabled session.

### Installed server (`npm run test:server`)

**Linux only.** Spawns a private instance of the installed `rserver` binary at `/usr/lib/rstudio-server/bin/rserver` using `/etc/rstudio/rserver.conf`. The fixture still passes `--auth-none=1` and isolated dirs, so it doesn't collide with a systemd-managed instance.

Prerequisite: `rstudio-server` package installed (e.g. via `.deb` / `.rpm`).

If you instead want to connect to an already-running server (CI, remote VM, or a real systemd instance with auth enabled), see [Targeting an external server](#targeting-an-external-server) below.

### Development server build (`npm run test:server-dev`)

**Not on Windows.** Spawns a private `rserver-dev` per worker, using the binary at `build/src/cpp/server/rserver` and the config at `build/src/cpp/conf/rserver-dev.conf`. Runs with `--auth-none`, so no credentials are needed.

Prerequisite: build the server first from the **repo root** with `cmake --build build`. Override the binary or config paths with `PW_RSERVER_BIN` / `PW_RSERVER_CONF` if you need to point at a different build.

## Targeting an external server

To skip the spawn and connect to an external server (CI, a remote VM, a real systemd `rstudio-server` instance, etc.), set `PW_RSTUDIO_SERVER_URL`. Credentials are needed only when the external server presents a login form.

| Variable | Required | Description |
|----------|----------|-------------|
| `PW_RSTUDIO_SERVER_URL` | When targeting an external server | Full URL including port if needed, e.g. `http://10.12.227.184:8787` or `https://dev.example.com/s/abc123/` |
| `PW_RSTUDIO_SERVER_PORT` | No | Override the port in the URL (only set if you need to override what's in the URL) |
| `PW_RSTUDIO_SERVER_USER` | When login is required | Login username |
| `PW_RSTUDIO_SERVER_PASSWORD` | When login is required | Login password |
| `PW_RSTUDIO_SERVER_LOGIN_TIMEOUT` | No | Post-login wait for the IDE console, in ms (default: 60000). Increase for slow servers. |
| `PW_RSTUDIO_EDITION` | No | `os` (default) or `pro`. Filters edition-specific tests. |

```bash
# External server with login
PW_RSTUDIO_SERVER_URL=http://<ip> PW_RSTUDIO_SERVER_USER=<user> PW_RSTUDIO_SERVER_PASSWORD=<pass> npx playwright test --project=server

# Specific test file
PW_RSTUDIO_SERVER_URL=http://<ip> PW_RSTUDIO_SERVER_USER=<user> PW_RSTUDIO_SERVER_PASSWORD=<pass> npx playwright test tests/path/to/test.test.ts --project=server

# Pro edition
PW_RSTUDIO_EDITION=pro PW_RSTUDIO_SERVER_URL=http://<ip> PW_RSTUDIO_SERVER_USER=<user> PW_RSTUDIO_SERVER_PASSWORD=<pass> npx playwright test --project=server
```

- The fixture launches a headed Chromium browser, navigates to the server URL, and logs in
- Always ask the user for the username and password -- never assume credentials
- `PW_RSTUDIO_MODE=desktop|server` is a fallback when `--project` isn't passed; if both are set, `--project` wins

## Sandbox

Every invocation gets a per-run sandbox directory (created by `fixtures/sandbox-setup.ts`). The sandbox holds a temporary `user-home`, isolating tests from the user's real config.

| Variable | Description |
|----------|-------------|
| `PW_SANDBOX` | Set internally by `sandbox-setup.ts` to the absolute path of the sandbox. Tests read it; you normally do not set it. |
| `PW_SANDBOX_ROOT` | Parent dir under which the sandbox subtree is created. Defaults to `os.tmpdir()`. |
| `PW_SANDBOX_ROOT_CREATE` | `1`/`true` to `mkdir` `PW_SANDBOX_ROOT` if it doesn't exist (otherwise sandbox-setup errors out). |
| `PW_SANDBOX_SKIP_CLEANUP` | `1`/`true` to preserve the sandbox after the run (also preserved automatically on test failure). |
| `PW_SANDBOX_SEED_POSITAI` | `1`/`true` to copy the real `~/.positai/` into the sandbox so Posit Assistant tests start signed in. Default is unseeded. |
| `PW_SANDBOX_SEED_COPILOT` | `1`/`true` to copy the real GitHub Copilot config (`~/.config/github-copilot/` on macOS/Linux, `%LOCALAPPDATA%\github-copilot\` on Windows) into the sandbox so Copilot tests start authenticated. Default is unseeded. |

**Security note:** the seed flags copy real tokens into the sandbox. Tokens persist if the run is preserved or teardown fails. Only use on machines with a dedicated test account.

## Other Environment Variables

| Variable | Description |
|----------|-------------|
| `PW_ENV_FILE` | Path to a dotenv file loaded by `playwright.config.ts` at startup. Variables already set in the shell win over file values. |
| `PW_CDP_PORT` | Override the Chrome DevTools Protocol port used for Desktop mode. Defaults to a random port in 9231-9299. |
| `PW_PROJECT` | **Deprecated.** Prints a warning and is ignored. Use `--project=desktop` or `--project=server`, or `PW_RSTUDIO_MODE`. |

## Common Options

- **Run a specific test by name:** `npx playwright test -g "test name here"`
- **Don't use `-x`** — without it, all test blocks run even if one fails, maximizing coverage per run
- **Retries:** config has `retries: 0` by default; add `--retries 1` if flakiness is expected

## Tags

Tests use Playwright tags to indicate platform, edition, and product tier constraints.

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

The config defines two **projects** -- `desktop` (default) and `server` -- each with a `grepInvert` computed at config load:

- **OS**: `desktop` excludes OS-only tags that don't match the host (`os.platform()`); `server` always excludes `@windows_only|@macos_only` (server targets Linux).
- **Edition**: `PW_RSTUDIO_EDITION=os` (default) excludes `@pro_only`; `PW_RSTUDIO_EDITION=pro` excludes `@os_only`.
- **Mode**: each project also excludes the opposite mode's tag.

Select a project with `--project=desktop` or `--project=server`. `PW_RSTUDIO_MODE=desktop|server` is a fallback when `--project` isn't passed; if both are set, `--project` wins.

You can also filter manually with `--grep` and `--grep-invert`:

```bash
# Run only tests with a specific tag
npx playwright test --grep @desktop_only

# Exclude a single tag
npx playwright test --grep-invert @pro_only

# Exclude multiple tags
npx playwright test --grep-invert "@pro_only|@server_only"
```

## Test Running Protocol

Before executing:
1. **Pick a run mode.** Unless the user has already named one, ask which of the four they want (`test:desktop`, `test:desktop-dev`, `test:server`, `test:server-dev`). Filter out modes unsupported by the host platform (server modes off Windows; `test:server` off macOS). Use `AskUserQuestion` if available.
2. Say a variant of "Ready to test!" (vary the phrasing)
3. Show the command -- prefer the `npm run` form (e.g. `npm run test:desktop-dev -- tests/path/to/test.test.ts`) over raw `npx playwright test`.
4. Note if it's the same as a previous run or different -- **in bold and italics**
5. Wait for user approval before running

After the first run in a conversation, don't ask for approval again to re-run the same file in the same mode -- just go. Switching modes or files warrants a fresh confirmation.
