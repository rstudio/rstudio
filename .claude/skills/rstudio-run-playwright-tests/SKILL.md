---
name: rstudio-run-playwright-tests
description: How to run RStudio Playwright tests in Desktop and Server modes. Use this skill whenever the user asks to run, execute, or launch Playwright tests against RStudio - whether on Desktop (local) or Server (remote). Also use when the user asks about the test command, environment variables, or how to point tests at a server URL. Trigger on phrases like "run the test", "execute on server", "run it on desktop", "run against <IP>", or any request involving npx playwright test for the RStudio test suite.
---

# Running RStudio Playwright Tests

Run from `e2e/rstudio/`. Setup, env vars, tags, sandbox, external-server config,
and the cross-env / dotenv helpers all live in `e2e/rstudio/README.md` -- open
it when you need any of that.

## The four run modes

| Mode | Command | Platforms |
|------|---------|-----------|
| Installed desktop | `npm run test:desktop` | macOS, Linux, Windows |
| Dev desktop build | `npm run test:desktop-dev` | macOS, Linux, Windows |
| Installed server | `npm run test:server` | Linux |
| Dev server build | `npm run test:server-dev` | macOS, Linux |

Append a test path or Playwright flags after the script name:

```bash
npm run test:desktop -- tests/path/to/test.test.ts
npm run test:server-dev -- -g "test name here"
```

### `test:desktop-dev` prerequisites (from repo root)

1. `cmake --build build` -- C++ session
2. `(cd src/gwt && ant draft)` -- GWT transpile (2-5 min; rerun after Java edits)
3. `(cd src/node/desktop && npm ci)` -- Electron deps

Tests that exercise `doRestart()` (e.g. uninstall Posit Assistant) aren't
supported in this mode.

## Test running protocol

Before the first run in a conversation:

1. Pick a mode -- ask the user which of the four they want (use
   `AskUserQuestion` if available), filtering out modes unsupported by the
   host platform.
2. Say a variant of "Ready to test!" (vary the phrasing).
3. Show the command (prefer `npm run ...` over raw `npx playwright test`).
4. Note whether it's the same as the previous run or different -- ***in bold
   and italics***.
5. Wait for approval.

After the first run, don't re-ask to re-run the same file in the same mode --
just go. Switching files or modes warrants a fresh confirmation.
