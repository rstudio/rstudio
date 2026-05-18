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

## Desktop Mode (Default)

No flag needed -- `desktop` is the default project.

```bash
# All tests
npx playwright test

# Specific test file
npx playwright test tests/path/to/test.test.ts

# With extra RStudio CLI args
PW_RSTUDIO_EXTRA_ARGS="--my-flag --other-option" npx playwright test

# Pro edition
PW_RSTUDIO_EDITION=pro npx playwright test
```

- Connects to RStudio Desktop via CDP on port 9222
- The fixture handles launching and shutting down RStudio Desktop automatically
- `PW_RSTUDIO_EXTRA_ARGS` passes space-separated CLI flags to the RStudio process at launch
- `PW_RSTUDIO_PREFS_OVERRIDE` points at a JSON/JSONC file whose keys override the suite-wide RStudio prefs (`fixtures/base-prefs.jsonc`) per-key

## Server Mode

Pass `--project=server` and provide credentials.

| Variable | Required | Description |
|----------|----------|-------------|
| `PW_RSTUDIO_SERVER_URL` | No | Full URL including port if needed, e.g. `http://10.12.227.184:8787` or `https://dev.example.com/s/abc123/` (default: `http://localhost:8787`) |
| `PW_RSTUDIO_SERVER_PORT` | No | Override the port in the URL (no default -- only set if you need to override what's in the URL) |
| `PW_RSTUDIO_SERVER_USER` | Yes | Login username |
| `PW_RSTUDIO_SERVER_PASSWORD` | Yes | Login password |
| `PW_RSTUDIO_SERVER_LOGIN_TIMEOUT` | No | Post-login wait for the IDE console, in ms (default: 60000). Increase for slow servers. |
| `PW_RSTUDIO_EDITION` | No | `os` (default) or `pro`. Filters edition-specific tests. |

```bash
# All tests
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
| `PW_PROJECT` | **Deprecated.** Prints a warning and is ignored. Use `--project=desktop|server` or `PW_RSTUDIO_MODE`. |

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
1. Say a variant of "Ready to test!" (vary the phrasing)
2. Show the command
3. Note if it's the same as a previous run or different — **in bold and italics**
4. Wait for user approval before running
