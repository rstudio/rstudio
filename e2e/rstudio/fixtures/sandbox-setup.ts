import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import type { FullConfig } from '@playwright/test';
import { prepareRLibs } from './r-libs-setup';
import { launchRStudio, shutdownRStudio } from './desktop.fixture';
import { scrubCredentials } from '../utils/auth';

/**
 * Create a per-invocation sandbox directory and export its path as PW_SANDBOX.
 *
 * The sandbox is the single rooted tree under which every test-side artifact
 * lives: per-spec config trees (each carrying its own RSTUDIO_DATA_HOME --
 * see desktop.fixture's createTempConfig), R workdirs, the sandbox-level
 * data-home (the seeded-pai source; Desktop sessions do not use it as their
 * data home), and the shared user home (HOME/USERPROFILE). The companion
 * sandbox-teardown removes the subtree once the run finishes (unless
 * preserved by the rules in that file).
 *
 * Env vars (all optional):
 *   PW_SANDBOX_ROOT                Parent dir for the sandbox subtree. Defaults
 *                                  to os.tmpdir().
 *   PW_SANDBOX_ROOT_CREATE         "1"/"true" to mkdir PW_SANDBOX_ROOT if
 *                                  missing. Default is to fail loudly on a
 *                                  missing parent so typos surface immediately.
 *   PW_SEED_PAI                    Path to a Posit Assistant install directory
 *                                  (e.g. ~/.local/share/rstudio/pai, as
 *                                  produced by the assistant repo's
 *                                  `npm run deploy:rstudio`). Copied into the
 *                                  sandbox data-home, from where each per-spec
 *                                  data home links it, so tests run against
 *                                  that local build instead of downloading
 *                                  the official package. Only the install
 *                                  shape (bin/package.json present) is
 *                                  validated here; version/protocol
 *                                  compatibility is enforced by the IDE at
 *                                  runtime, which treats an incompatible
 *                                  seeded build as needing an update.
 *   PW_SANDBOX_NO_SEED_CREDENTIALS "1"/"true" is a global host-copy kill-
 *                                  switch, consumed by the auth.setup project
 *                                  (tests/auth.setup.ts): it suppresses the
 *                                  copy of the local Posit AI token store and
 *                                  of the local GitHub Copilot config dir, but
 *                                  not the sign-in flows, which copy nothing
 *                                  from the host. Unprovisioned providers'
 *                                  tests skip with a reason.
 *                                  Privacy note: real OAuth/API tokens are
 *                                  copied into the sandbox during the run.
 *                                  sandbox-teardown scrubs them whenever the
 *                                  sandbox is left on disk (preserved by a
 *                                  failure or PW_SANDBOX_SKIP_CLEANUP, or
 *                                  stranded by a failed delete), so a surviving
 *                                  sandbox should hold no tokens; set this
 *                                  opt-out anyway if the host isn't a dedicated
 *                                  test account, to avoid copying them at all.
 *
 * Sets PW_SANDBOX (internal) to the absolute path of the auto-created
 * subtree. Workers inherit it via the normal child-process env;
 * globalTeardown reads PW_SANDBOX directly from process.env.
 */
export default async function globalSetup(config: FullConfig) {
  // Export the resolved worker count so the Desktop fixture and r-libs helpers
  // can decide whether to partition HOME / R_LIBS_USER per worker. The default
  // (1 worker) keeps the historical shared-state behavior; >1 activates the
  // per-worker subtrees. Honor an explicit override if already set.
  if (!process.env.PW_TOTAL_WORKERS)
    process.env.PW_TOTAL_WORKERS = String(config.workers ?? 1);

  const parent = process.env.PW_SANDBOX_ROOT ?? os.tmpdir();
  const shouldCreate = ['1', 'true'].includes(
    (process.env.PW_SANDBOX_ROOT_CREATE ?? '').toLowerCase(),
  );
  if (!fs.existsSync(parent)) {
    if (!shouldCreate) {
      throw new Error(
        `PW_SANDBOX_ROOT="${parent}" does not exist; set PW_SANDBOX_ROOT_CREATE=1 to auto-create`,
      );
    }
    try {
      fs.mkdirSync(parent, { recursive: true });
    } catch (err) {
      throw new Error(
        `Failed to create PW_SANDBOX_ROOT="${parent}": ${(err as Error).message}`,
      );
    }
  }

  // Resolve symlinks in the sandbox path so every downstream consumer sees the
  // same canonical form. On macOS, os.tmpdir() returns the non-canonical
  // /var/folders/... prefix while /var is a symlink to /private/var; rstudioapi
  // and R-side tempdir() normalize to /private/var/..., so paths the test
  // computes JS-side and paths reported by RStudio would differ otherwise.
  // TextEditingTarget's project-prefix check on save (#16721 / styler reformat
  // on save) is one place that breaks under a path-form mismatch.
  const sandbox = fs.realpathSync(fs.mkdtempSync(path.join(parent, 'pw_sandbox_')));
  fs.mkdirSync(path.join(sandbox, 'data-home'), { recursive: true });

  // Seed a locally built Posit Assistant into the sandbox data-home so tests
  // exercise it instead of downloading the official package. Desktop launches
  // use per-spec data homes; each links data-home/pai from here (see
  // desktop.fixture's seedPaiIntoDataHome).
  const seedPai = process.env.PW_SEED_PAI;
  if (seedPai) {
    if (!fs.existsSync(path.join(seedPai, 'bin', 'package.json'))) {
      throw new Error(
        `PW_SEED_PAI="${seedPai}" does not look like a Posit Assistant install (missing bin/package.json)`,
      );
    }
    fs.cpSync(seedPai, path.join(sandbox, 'data-home', 'pai'), { recursive: true });
    console.log(`[sandbox] seeded data-home/pai from ${seedPai}`);
  }

  const userHome = path.join(sandbox, 'user-home');
  fs.mkdirSync(userHome, { recursive: true });

  // Seed an empty `.here` marker in the sandbox home. The home dir has no
  // project markers (.Rproj/.git/renv.lock/...), so any R code that resolves a
  // project root via the `here` / `rprojroot` packages -- e.g. reticulate's
  // pipenv interpreter probe, which calls an unguarded here::here() (see
  // rstudio/reticulate#1909) -- throws "No root directory found" when run from
  // here. That error has surfaced as a spurious modal (e.g. "Error Listing
  // Packages" on session resume) whose glass overlay then wedges unrelated
  // tests. `.here` is the marker `here` looks for first, so it makes here()
  // resolve to the home dir instead of erroring. It only affects code that
  // actually calls here()/rprojroot; RStudio's own project detection ignores it.
  fs.writeFileSync(path.join(userHome, '.here'), '');

  // On Windows, redirecting USERPROFILE to a directory that doesn't contain
  // an AppData/Roaming subdirectory makes Electron's app.getPath('appData')
  // fail at startup ("Failed to get 'appData' path" popup), which blocks
  // RStudio from launching at all. Pre-create the standard AppData scaffolding
  // inside the sandboxed user-home so Electron's resolution succeeds.
  // TODO: add macOS (~/Library/Application Support) and Linux Desktop
  // (~/.config) equivalents when those platforms are tested against the
  // HOME/USERPROFILE redirect.
  if (process.platform === 'win32') {
    fs.mkdirSync(path.join(userHome, 'AppData', 'Roaming'), { recursive: true });
    fs.mkdirSync(path.join(userHome, 'AppData', 'Local'), { recursive: true });
  }

  // Emergency credential scrub on SIGTERM. Registered before any credential
  // can land in the sandbox (the auth.setup project provisions both AI
  // providers after globalSetup completes), so there is no window where
  // seeded tokens sit unprotected.
  //
  // globalTeardown (which scrubs a preserved sandbox's seeded tokens; see
  // sandbox-teardown) runs on Playwright's completion path, including a
  // graceful Ctrl-C: the runner turns that SIGINT into an "interrupted" stop
  // that still executes its teardown tasks. It does NOT run when the main
  // runner process is terminated by a bare SIGTERM (e.g. a CI job cancelling
  // `npx playwright test`): Node's default disposition kills the process
  // outright with no teardown, stranding the real tokens seeded into the
  // sandbox on disk. Catch SIGTERM here, in the main process, and scrub
  // synchronously (scrubCredentials is all fs.rmSync) before re-raising the
  // signal to exit with the conventional status.
  //
  // Only SIGTERM is handled: Playwright owns SIGINT through its own handler
  // (the runner's FixedNodeSIGINTHandler), and racing it would break that
  // shutdown coordination, the same reason process-reaper.ts stays off both
  // signals in the worker. SIGKILL, OOM kills, and hard crashes are
  // uncatchable by any handler and leave the sandbox for manual cleanup.
  const scrubOnSigterm = () => {
    process.removeListener('SIGTERM', scrubOnSigterm);
    const failures = scrubCredentials(sandbox);
    if (failures.length > 0) {
      console.warn(
        `[sandbox] SIGTERM: could not scrub all credentials from ${sandbox}; left behind:\n  ${failures.join('\n  ')}`,
      );
    } else {
      console.warn(`[sandbox] SIGTERM: credentials scrubbed from ${sandbox}`);
    }
    // Re-raise now that our handler is removed, so the process dies with the
    // conventional 143 (128 + SIGTERM) status instead of exiting 0.
    process.kill(process.pid, 'SIGTERM');
  };
  process.on('SIGTERM', scrubOnSigterm);

  // No credentials are seeded here. The auth.setup project
  // (tests/auth.setup.ts) is the sole authority for both AI providers' sandbox
  // credentials -- a live sign-in flow when the provider's credentials are set
  // (POSIT_EMAIL/POSIT_PASSWORD, GH_COPILOT_USER/GH_COPILOT_PASSWORD), else a
  // copy of the local credential store -- and their tests gate on the on-disk
  // stores it leaves behind (see requireAiCredentials in utils/ai-credentials.ts).

  process.env.PW_SANDBOX = sandbox;
  console.log(`[sandbox] root: ${sandbox}`);

  // Stable per-host R library, lives outside PW_SANDBOX so it survives across
  // runs. Without this the redirected HOME (set by Desktop/Server fixtures)
  // points R at an empty default user library, and the first thing rmarkdown
  // does on save is open a "stringr needs updating" dialog that hangs the
  // test. Pre-populating here is idempotent -- a warm cache is a fast
  // installed.packages() check with no install call.
  //
  // The resolved (token-expanded) path is exported so parallel workers can
  // hardlink-clone this prebuilt library into their own hermetic copies
  // (see workerRLibsUser); single-worker runs use it directly.
  const rLibsTemplate = await prepareRLibs();
  if (rLibsTemplate)
    process.env.PW_R_LIBS_TEMPLATE = rLibsTemplate;

  // Warmup launch: workers are recycled between spec files, so each new file
  // pays the cold-launch cost. Booting once here populates dyld/page caches
  // before any worker tries, dropping subsequent launches well under the
  // GWT-ready deadline. Skipped on Server mode (no IDE binary to warm) and
  // when PW_WARMUP_LAUNCH explicitly opts out. Default on under CI only --
  // a developer running locally has warm caches already.
  const mode = (process.env.PW_RSTUDIO_MODE ?? 'desktop').toLowerCase();
  const warmupOverride = process.env.PW_WARMUP_LAUNCH?.toLowerCase();
  const shouldWarmup =
    mode === 'desktop' &&
    (warmupOverride === '1' || warmupOverride === 'true' ||
     (warmupOverride !== '0' && warmupOverride !== 'false' && !!process.env.CI));
  if (shouldWarmup) {
    console.log('[sandbox] warmup launch (populates RStudio launch caches)');
    const t0 = Date.now();
    try {
      const session = await launchRStudio();
      await shutdownRStudio(session);
      console.log(`[sandbox] warmup launch complete in ${Date.now() - t0}ms`);
    } catch (err) {
      // A warmup failure is not fatal -- the first real test will retry the
      // launch via the in-fixture retry. Log it so a persistent failure mode
      // is visible without burning a whole test slot to surface it.
      console.warn(
        `[sandbox] warmup launch failed after ${Date.now() - t0}ms (continuing): ${(err as Error).message}`,
      );
    }
  }
}
