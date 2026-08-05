# Auth setup and credential handling

Read this when working on `tests/auth.setup.ts`, `utils/auth.ts`, or anything
that provisions, reads, or scrubs AI credentials in the sandbox.

## `utils/auth.ts` is the single source of truth

`AI_PROVIDERS`, `credentialPathsFor`, and `scrubCredentials` define every
provider's on-disk credential paths. Extend them when adding a provider --
never duplicate a path elsewhere. One exception: Copilot's host-side (source)
config dir for copy mode is computed in `tests/auth.setup.ts`, not `auth.ts`,
because it honors `%LOCALAPPDATA%` on Windows.

Both providers auto-detect their source: credentials set (`POSIT_EMAIL`/
`POSIT_PASSWORD`, `COPILOT_USER`/`COPILOT_PASSWORD`) -> live sign-in; else copy
the local store; else skip. Posit AI drives a browser device flow; Copilot
spawns the copilot-language-server and completes the browser half.

## Copilot store: SQLite read in a child process

The Copilot store is `auth.db`, a SQLite file. `isCopilotStoreAuthenticated`
counts its `oauth_tokens` rows, but runs the read in a short-lived child process
(`utils/copilot-store-check.js`), never in the Playwright worker -- so the gate
is async and callers `await` it. Three constraints forced this shape:

- better-sqlite3's native addon intermittently segfaults the worker during
  N-API registration on CI (macOS arm64, Windows). A child-process read means a
  crash kills only the child (retried a few times, then fail closed), not the
  worker.
- It must be real SQLite, not a pure-WASM reader (sql.js): the
  copilot-language-server leaves the `oauth_tokens` table and token in an
  uncheckpointed `-wal` sidecar, which a main-image-only reader can't see.
- The `sqlite3` CLI is not an option -- absent from PATH on Windows.

Fail closed (treat as not signed in) on any persistent read failure: an empty
`auth.db` exists before sign-in, so "file present" is not "signed in".

## Credentialed projects: artifacts off, no retries

Any Playwright project that types real credentials must set
`trace/video/screenshot: 'off'` in its `use` block -- artifacts capture the
login screen and password into the report. Also set `retries: 0`: retrying a
deterministic auth failure re-runs a live bad-credential sign-in and can
launder a fail-loud verdict into green-with-skips.

## SIGTERM strands teardown

`globalTeardown` runs on Ctrl-C (Playwright owns SIGINT) but NOT on a bare
SIGTERM (CI cancel) -- Node dies with no teardown. If globalSetup puts secrets
on disk, register a synchronous SIGTERM handler that scrubs them and
re-raises the signal. Never touch SIGINT.

## Layer timeouts so failures reach cleanup

Long setup flows need their own `setup.setTimeout()` AND a shorter inner
deadline (`withDeadline`) around each step, so a hang flows through the catch
block (skip/status path) instead of the harness timeout -- which skips
cleanup and marks all dependent tests "did not run". Raw-chromium contexts
don't inherit the config's `actionTimeout`; each step gets the 30s library
default.

## login.posit.cloud gotchas

- The password field appears inline via XHR -- no navigation, so
  `waitForLoadState` returns immediately and `waitForURL` hangs. Wait for the
  field itself.
- User-code inputs need `pressSequentially` per character (React `onChange`).
- The Authorize button can pass actionability before the SPA binds its click
  handler -- re-click in a loop with a short click timeout until the button
  detaches.

## github.com device-flow gotchas (Copilot)

- The Authorize button (`.js-oauth-authorize-btn`) renders disabled and only
  arms after a genuine trusted scroll: `page.mouse.wheel(0, 300)` (a real CDP
  event) enables it; a JS scroll does not. Works headless.
- The per-character user-code boxes (`#user-code-*`) need `pressSequentially`;
  `fill()` is ignored by the reactive form.
- 2FA uses a TOTP code generated from `COPILOT_TOTP_SECRET` (base32). A missing
  or malformed secret fails loud (`GitHubLoginError`), not a skip.

## Server mode: how sandbox credentials reach the rsession

`rserver` builds each rsession's environment from scratch and takes `HOME`
from the passwd db (`getpwnam` -> `pw_dir`). Only a short allow-list survives
from the rserver process -- `PATH`, `MANPATH`, `LANG`, `SHELL`, the
`RS_LOG_*` family, and the names `forwardXdgEnvVars` carries
(`core/system/Xdg.cpp`), which is why setting `RSTUDIO_CONFIG_HOME` on
rserver does work. `HOME` and `R_LIBS_USER` are not on that list. For the
spawned in-tree server, the fixture bridges the gap with a wrapper script
passed as `--rsession-path` (`writeRsessionWrapper` in
`fixtures/server.fixture.ts`): it exports the sandbox `HOME` and
`R_LIBS_USER` (plus `GITHUB_COPILOT_AUTH_TOKEN_ENCRYPTION=false`, unsets
`XDG_CONFIG_HOME`, re-exports `DYLD_INSERT_LIBRARIES` on macOS where SIP
strips it across the `/bin/sh` exec) and execs the real rsession. Keep the
wrapper's variable list in sync with what Desktop sets on its child process.
The `@server_only` tripwire in `tests/sandbox.test.ts` asserts that `HOME` and
`R_LIBS_USER` arrive with the expected values and that `XDG_CONFIG_HOME` is
empty -- if the delivery chain breaks, that test is what catches it. Compare
`R_LIBS_USER` through `base:::.expand_R_libs_env_var`, never against the raw
template: R's Rprofile expands the `%p` / `%v` tokens at startup, so the
session never reports back the string the wrapper exported.

**External servers** (`PW_RSTUDIO_SERVER_URL`): the harness cannot control
that server's rsession environment, so a final auth-setup step provisions
the stores through the session instead (`utils/remote-provision.ts`): log
in, upload each sandbox store over HTTP to the same `/upload` endpoint the
Files pane posts to, move it into place with a console command carrying
nothing but file paths, verify size and mode, re-probe the content, then end
the session so tests get a fresh rsession. Never put file contents into a
console command here: RStudio records every submitted command in the
account's history database, so a command carrying a store would leave a
recoverable copy of a live token there.
Key rules baked into that step:

- A store on the remote account counts as pre-existing only when it holds a
  sign-in, since sessions create empty stubs on their own. Only paths
  provisioning created go into the manifest
  (`<sandbox>/remote-provision-manifest.json`), which `auth-teardown` scrubs.
- `requireAiCredentials` additionally requires `remoteProvisioned` in the
  provider's status file on external runs, so a sandbox-only store can't
  false-pass the gate (the remote account's own sign-in state is what the
  session actually sees).
- An unreadable probe is not an absent store. Where existence or content
  can't be determined, the store is left alone and that provider's tests
  skip, rather than pushing over what may be the account's own sign-in.
- Use a dedicated test account. The upload channel keeps token bytes out of
  the console history, but the stores still land in a real account's home,
  and a run that dies before teardown leaves them there.
- A quick experiment for #18348 confirmed the copilot-language-server reads a
  plaintext `auth.db` without `GITHUB_COPILOT_AUTH_TOKEN_ENCRYPTION` set, so
  no remote `~/.Renviron` edit is needed.
