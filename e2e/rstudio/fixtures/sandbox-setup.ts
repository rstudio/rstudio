import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import type { FullConfig } from '@playwright/test';
import { prepareRLibs } from './r-libs-setup';
import { launchRStudio, shutdownRStudio } from './desktop.fixture';
import { noSeedCredentials } from '../utils/auth';

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
 *                                  switch. By default, if the GitHub Copilot
 *                                  config dir exists on the host it's copied
 *                                  into the sandbox user-home so the Copilot
 *                                  tests start authenticated; setting this
 *                                  suppresses that copy. It also suppresses the
 *                                  Posit AI "seed" copy (handled by the
 *                                  auth.setup project), but not the Posit AI
 *                                  sign-in flow, which copies nothing from the
 *                                  host. Copilot tests skip when their
 *                                  credentials are unseeded.
 *                                  Privacy note: real OAuth/API tokens are
 *                                  copied into the sandbox. If the run is
 *                                  preserved (test failure or
 *                                  PW_SANDBOX_SKIP_CLEANUP=1), the copied
 *                                  tokens persist there until the sandbox is
 *                                  removed. Set this opt-out if the host
 *                                  isn't a dedicated test account.
 *
 * Sets PW_SANDBOX (internal) to the absolute path of the auto-created
 * subtree, and PW_AI_SEEDED_COPILOT to "1" when GitHub Copilot credentials
 * were successfully copied (consumed by requireAiCredentials in Copilot
 * test files). Workers inherit it via the normal child-process env;
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

  // Seed GitHub Copilot credentials by default when the host has them. The
  // matching Copilot tests run requireAiCredentials(test, 'copilot'),
  // which gates each describe on PW_AI_SEEDED_COPILOT (set below); an unseeded
  // provider surfaces as a clean skip-with-reason rather than a 5-minute
  // mystery failure waiting for a completion that will never arrive.
  //
  // Posit AI is NOT seeded here -- the auth.setup project (tests/auth.setup.ts)
  // is the sole authority for Posit AI credentials (sign-in flow by default, or
  // a whole-tree host copy under PW_SANDBOX_POSITAI_AUTH=seed), and its
  // tests gate on the on-disk token store rather than an env flag.
  //
  // Clear the flag up front so only a value this function sets is honored -- a
  // stray PW_AI_SEEDED_COPILOT=1 inherited from the user's shell or a prior
  // partially-cleaned run could otherwise smuggle past requireAiCredentials()
  // even when no credentials were actually copied.
  delete process.env.PW_AI_SEEDED_COPILOT;

  const skipSeeding = noSeedCredentials();

  if (!skipSeeding) {
    const isWindows = process.platform === 'win32';
    const realCopilot = isWindows
      ? path.join(process.env.LOCALAPPDATA ?? path.join(os.homedir(), 'AppData', 'Local'), 'github-copilot')
      : path.join(os.homedir(), '.config', 'github-copilot');
    const destCopilot = isWindows
      ? path.join(userHome, 'AppData', 'Local', 'github-copilot')
      : path.join(userHome, '.config', 'github-copilot');
    if (fs.existsSync(realCopilot)) {
      try {
        fs.cpSync(realCopilot, destCopilot, { recursive: true });
        process.env.PW_AI_SEEDED_COPILOT = '1';
        console.log(`[sandbox] seeded user-home github-copilot from ${realCopilot}`);
        console.log(
          `[sandbox] real GitHub Copilot tokens now live in the sandbox and persist if the run is preserved or teardown fails.`,
        );
      } catch (err) {
        throw new Error(
          `Failed copying ${realCopilot} into sandbox: ${(err as Error).message}`,
        );
      }
    } else {
      console.log(
        `[sandbox] no ${realCopilot} on host; Copilot tests will skip`,
      );
    }
  } else {
    console.log('[sandbox] PW_SANDBOX_NO_SEED_CREDENTIALS set; GitHub Copilot not seeded (Copilot tests will skip). This global kill-switch also suppresses Posit AI seed; the sign-in flow (PW_SANDBOX_POSITAI_AUTH=flow) is unaffected.');
  }

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
