# RStudio End-to-End Tests

End-to-end tests are written in TypeScript with Playwright. The rsession is started with `--automation-agent`, which causes the GWT frontend to install a JS automation bridge at `window.rstudio` (see `ApplicationAutomation.java`). The Playwright command helpers in `utils/commands.ts` drive the IDE through this bridge instead of the R console, avoiding the focus-shift and pane-collapse pitfalls of the console path.

The `window.rstudio` surface includes:

- `window.rstudio.ready` -- boolean flipped to `true` once R's deferred init has run; the canonical "automation can start" signal (reset to `false` on restart or project open/close).
- `window.rstudio.commands.<commandId>()` -- execute an AppCommand; `.isChecked()` / `.isEnabled()` / `.isVisible()` query its state (note `isEnabled()` is `enabled_ && isVisible()`, so an invisible programmatic command reads disabled even when it dispatches fine). `commands.list` is the array of all command ids.
- `window.rstudio.prefs.<camelCaseName>.get()` / `.set(value)` / `.clear()` -- `set` and `clear` return a Promise that resolves once the `setUserPrefs` RPC has landed server-side.
- `window.rstudio.documents` -- `active()`, `activeEditor()` (native Ace editor), `open(path, opts?)`, `closeAllNoSave()`, `resetToUntitled()`.
- `window.rstudio.project` -- `path()`, `name()`, `isActive()`, `open(path)`.
- `window.rstudio.version` -- `{ rstudio, r }` version strings.
- `window.rstudio.dialogs` -- `numShowing()`, `dismissAll()`.
- `window.rstudio.console.promptCount` -- a monotonic counter that advances by one each time R returns to its top-level prompt (i.e. a submitted console command completed). It is driven by the `ConsolePromptEvent`, so it is a race-free completion signal: `executeInConsole({wait:true})` captures it before submitting and waits for it to increase, rather than sampling the `rstudio-console-busy` class (which can be read stale in the submit->busy gap, or miss a fast command's busy flash). Prefer this for awaiting R-side side effects; the busy-class `waitForConsoleIdle` remains only as a fallback for binaries that predate the counter.
- `window.rstudio.layout.reset()` -- end any active pane/column zoom or pane maximize.
- `window.rstudio.completions.setAlwaysShowPopup(force)` -- automation-only override: when `force` is true, a completion request returning exactly one result shows the popup instead of auto-accepting the match (the default RCompletionManager / CompletionManagerBase behavior for explicit requests, e.g. Ctrl+Space). The popup-reading helpers in `actions/autocomplete.actions.ts` wrap their requests in this so item enumeration is deterministic regardless of result count; leave it off (the default) to exercise the real auto-accept path.
- `window.rstudio.errors` -- `list()` / `clear()` the uncaught client exceptions recorded by the agent (message + stack); `simulate(msg)` raises a real one (harness self-test only). Unhandled promise rejections are recorded too (message prefixed with "Unhandled promise rejection:"), since GWT does not route native promise continuations through its uncaught-exception handler. The per-test fixture drains this and fails the test that raised an exception (opt out with `PW_IGNORE_CLIENT_EXCEPTIONS=1`).

Commands and prefs are enumerated up front at agent init, so a missing-by-name lookup is a genuine "doesn't exist" rather than "not yet touched by GWT code".

Suite basics--setup, running, sandbox conventions, tags, environment variables, and failure diagnostics--are documented in `README.md`.

See `.claude/skills/rstudio-create-playwright-tests/SKILL.md` for detailed guidance on writing Playwright tests, and `.claude/skills/rstudio-run-playwright-tests/SKILL.md` for how to run them.
