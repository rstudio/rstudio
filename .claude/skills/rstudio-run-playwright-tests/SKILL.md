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

No special env vars needed — `RSTUDIO_EDITION` defaults to `desktop`.

```bash
# All tests
npx playwright test

# Specific test file
npx playwright test tests/path/to/test.test.ts
```

- Connects to RStudio Desktop via CDP on port 9222
- The fixture handles launching and shutting down RStudio Desktop automatically

## Server Mode

Requires four env vars:

| Variable | Required | Description |
|----------|----------|-------------|
| `RSTUDIO_EDITION` | Yes | Set to `server` |
| `RSTUDIO_SERVER_URL` | No | Full URL including port if needed, e.g. `http://10.12.227.184:8787` or `https://dev.example.com/s/abc123/` (default: `http://localhost:8787`) |
| `RSTUDIO_SERVER_PORT` | No | Override the port in the URL (no default — only set if you need to override what's in the URL) |
| `RSTUDIO_USER` | Yes | Login username |
| `RSTUDIO_PASSWORD` | Yes | Login password |
| `RSTUDIO_LOAD_TIMEOUT` | No | IDE load timeout in ms (default: 60000). Increase for slow servers. |

```bash
# All tests
RSTUDIO_EDITION=server RSTUDIO_SERVER_URL=http://<ip> RSTUDIO_USER=<user> RSTUDIO_PASSWORD=<pass> npx playwright test

# Specific test file
RSTUDIO_EDITION=server RSTUDIO_SERVER_URL=http://<ip> RSTUDIO_USER=<user> RSTUDIO_PASSWORD=<pass> npx playwright test tests/path/to/test.test.ts
```

- The fixture launches a headed Chromium browser, navigates to the server URL, and logs in
- Always ask the user for the username and password — never assume credentials

## Common Options

- **Run a specific test by name:** `npx playwright test -g "test name here"`
- **Don't use `-x`** — without it, all test blocks run even if one fails, maximizing coverage per run
- **Retries:** config has `retries: 0` by default; add `--retries 1` if flakiness is expected

## Test Running Protocol

Before executing:
1. Say a variant of "Ready to test!" (vary the phrasing)
2. Show the command
3. Note if it's the same as a previous run or different — **in bold and italics**
4. Wait for user approval before running
