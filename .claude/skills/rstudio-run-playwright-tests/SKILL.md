---
name: rstudio-run-playwright-tests
description: "Run RStudio Playwright tests in Desktop and Server modes. Use when running, executing, or launching Playwright tests against RStudio on Desktop (local) or Server (remote), configuring test environment variables, or pointing tests at a server URL. Also covers npm run test:desktop, npm run test:server, and npx playwright test commands for the RStudio e2e test suite."
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

Before the first run in a conversation, pick a mode -- ask the user which of
the four they want (use `AskUserQuestion` if available), filtering out modes
unsupported by the host platform. If a `build/` directory exists at the repo
root, list `test:desktop-dev` first in the options. Then run the tests
directly; don't prompt for approval before kicking off the run. Prefer
`npm run ...` over raw `npx playwright test`.

After the first run, don't re-ask to re-run the same file in the same mode --
just go. Switching modes (not files) warrants a fresh mode-pick.

## Verifying results

Check the Playwright exit code: 0 means all tests passed. On failure, read the
test output for assertion errors or timeouts. Re-run a single failing test in
isolation before investigating further:

```bash
npm run test:desktop -- tests/path/to/failing.test.ts --reporter=list
```
