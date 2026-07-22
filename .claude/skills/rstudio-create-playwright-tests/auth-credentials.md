# Auth setup and credential handling

Read this when working on `tests/auth.setup.ts`, `utils/auth.ts`, or anything
that provisions, reads, or scrubs AI credentials in the sandbox.

## `utils/auth.ts` is the single source of truth

`AI_PROVIDERS`, `credentialPathsFor`, and `scrubCredentials` define every
provider's on-disk credential paths. Extend them when adding a provider --
never duplicate a path elsewhere.

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
