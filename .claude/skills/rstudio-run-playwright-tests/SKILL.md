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

No special env vars needed — `PW_RSTUDIO_MODE` defaults to `desktop`.

```bash
# All tests
npx playwright test

# Specific test file
npx playwright test tests/path/to/test.test.ts

# With extra RStudio CLI args
PW_RSTUDIO_EXTRA_ARGS="--my-flag --other-option" npx playwright test
```

- Connects to RStudio Desktop via CDP on port 9222
- The fixture handles launching and shutting down RStudio Desktop automatically
- `PW_RSTUDIO_EXTRA_ARGS` passes space-separated CLI flags to the RStudio process at launch
- `PW_RSTUDIO_PREFS_OVERRIDE` points at a JSON/JSONC file whose keys override the suite-wide RStudio prefs (`fixtures/base-prefs.jsonc`) per-key

## Server Mode

Key env vars (3 required, 3 optional):

| Variable | Required | Description |
|----------|----------|-------------|
| `PW_RSTUDIO_MODE` | Yes | Set to `server` |
| `PW_RSTUDIO_SERVER_URL` | No | Full URL including port if needed, e.g. `http://10.12.227.184:8787` or `https://dev.example.com/s/abc123/` (default: `http://localhost:8787`) |
| `PW_RSTUDIO_SERVER_PORT` | No | Override the port in the URL (no default — only set if you need to override what's in the URL) |
| `PW_RSTUDIO_SERVER_USER` | Yes | Login username |
| `PW_RSTUDIO_SERVER_PASSWORD` | Yes | Login password |
| `PW_RSTUDIO_SERVER_LOGIN_TIMEOUT` | No | Post-login wait for the IDE console, in ms (default: 60000). Increase for slow servers. |

```bash
# All tests
PW_RSTUDIO_MODE=server PW_RSTUDIO_SERVER_URL=http://<ip> PW_RSTUDIO_SERVER_USER=<user> PW_RSTUDIO_SERVER_PASSWORD=<pass> npx playwright test

# Specific test file
PW_RSTUDIO_MODE=server PW_RSTUDIO_SERVER_URL=http://<ip> PW_RSTUDIO_SERVER_USER=<user> PW_RSTUDIO_SERVER_PASSWORD=<pass> npx playwright test tests/path/to/test.test.ts
```

- The fixture launches a headed Chromium browser, navigates to the server URL, and logs in
- Always ask the user for the username and password — never assume credentials

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

The config defines **projects** that automatically exclude tags not applicable to a given environment. Use `PW_PROJECT` to select one:

```bash
# Set once in your shell profile for local development
export PW_PROJECT=desktop-os-windows

# Then just run
npx playwright test

# To switch projects, override PW_PROJECT inline
PW_PROJECT=server-os-linux PW_RSTUDIO_MODE=server npx playwright test
```

Without `PW_PROJECT`, all 8 projects run (each test executes once per project). `PW_PROJECT` and `--project` conflict--don't use both at the same time. `PW_PROJECT` pre-filters the project list, so `--project` can't select anything outside it.

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

## Test Running Protocol

Before executing:
1. Say a variant of "Ready to test!" (vary the phrasing)
2. Show the command
3. Note if it's the same as a previous run or different — **in bold and italics**
4. Wait for user approval before running
