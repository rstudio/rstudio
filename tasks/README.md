# tasks

Repo-level developer tasks, run through the top-level `package.json`:

```sh
npm run rserver-dev       # start a development RStudio Server
npm run rserver-status    # report what is running
npm run rserver-stop      # stop it
npm run startup-timing    # measure RStudio Desktop startup
```

The tasks are TypeScript run directly by `node` (built-in type stripping, Node
22.18+). There are no dependencies and no build step -- nothing here may import
anything outside the `node:` builtins. `tasks/package.json` marks this
directory as ESM so the repo root keeps its default CommonJS resolution.

## rserver-dev

Starts a dev server for an RStudio checkout or git worktree:

```sh
npm run rserver-dev                                  # this checkout
npm run rserver-dev -- ../rstudio-worktree           # somewhere else
npm run rserver-dev -- --gwt=draft --port=8790
```

It:

1. Configures `<checkout>/build` if it has never been configured, then runs an
   incremental `cmake --build` so `rserver` and `rsession` are current
   (`--no-build` to skip).
2. Picks free ports -- the first free port at or above 8787 for `rserver`, and
   at or above 9876 for the GWT code server.
3. Starts `rserver` with a private `server-data-dir` and secure-cookie key,
   bound to `127.0.0.1` and running `--auth-none`.
4. Starts `ant devmode` for the front end (`--gwt=draft` for a one-shot
   `ant draft` instead, `--gwt=none` to reuse `src/gwt/www` as-is).
5. Waits for both to answer, then prints the URL to open.

Both processes are detached and outlive the task. `--help` lists every option.

Because the ports, data dir and cookie key are per instance, several worktrees
can serve at the same time. An instance is keyed by checkout, though: a single
`src/gwt/www` can only host one devmode, so starting twice in one checkout
reports the running instance instead (use `--restart` to replace it).

### Notes

- `rserver` is spawned directly rather than through `build/src/cpp/rserver-dev`.
  That wrapper hardcodes the port in its banner, deletes the shared
  `/tmp/rstudio-server`, and on exit sends `SIGUSR2` to *every* `rsession` on
  the machine -- which would suspend other worktrees' sessions and any running
  desktop RStudio. The flags passed here otherwise reproduce the wrapper.
- With `--gwt=devmode`, the first page load compiles the app on demand and can
  take a couple of minutes; after that, Java edits are picked up on reload.
  `ant devmode` also opens GWT's Swing "Development Mode" window, exactly as it
  does when run by hand.
- The server runs with the real `HOME`, so it sees your usual R libraries and
  RStudio preferences. Two instances therefore share user preferences, the same
  way two desktop RStudio windows do.
- State lives in `<checkout>/.rstudio-dev/` (gitignored): `instance.json` plus
  `rserver.log` and `gwt.log`, which are the first place to look if a start
  fails.

## startup-timing

Measures where RStudio Desktop spends its startup time:

```sh
npm run startup-timing                                   # dev build, 3 launches
npm run startup-timing -- --runs=5 --no-splash
npm run startup-timing -- --app=/Applications/RStudio.app/Contents/MacOS/RStudio
npm run startup-timing -- --report=.rstudio-dev/startup-timing   # re-print
```

Each launch runs with `RSTUDIO_STARTUP_TIMING=<file>`, which makes the three
processes involved append checkpoints to one JSON-lines file:

- the Electron main process (`src/node/desktop/src/main/startup-timing.ts`):
  app ready, R detection, rsession spawn, window creation, page load,
  workbench initialized;
- the rsession process (`src/cpp/core/StartupTiming.cpp`, called from
  `SessionMain.cpp`, `RInit.cpp` and friends): options, prefs, HTTP listener,
  R initialization, module initialization, `client_init`, deferred init;
- the GWT client, which records `performance.mark("rstudio:...")` at its own
  milestones (`org.rstudio.core.client.StartupTiming`); the desktop copies
  those marks plus the navigation and resource timings into the file once the
  workbench is up.

The report prints the merged timeline (medians across runs, relative to the
Electron process creation time) followed by the longest measured spans, such
as the R module files sourced during session init and the largest downloads.
The per-run files under `<checkout>/.rstudio-dev/startup-timing/` also hold
the launch log and, unless `--user-config` was given, the throwaway RStudio
config and data homes the run used. The Electron profile (`--user-data-dir`)
is shared by the runs of one measurement and emptied before the first, so
run 1 behaves like a first launch and later runs like everyday launches.

Measuring the dev build goes through `npm run start` (electron-forge) each
time, so a run takes a while to start; the timeline is unaffected because it
begins at Electron process creation, after webpack has finished.

## Windows

RStudio Server is not built or supported on Windows, so these tasks are
macOS/Linux only and exit with a message there.
