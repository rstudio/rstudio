import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { prepareRLibs } from './r-libs-setup';
import { launchRStudio, shutdownRStudio } from './desktop.fixture';

/**
 * Create a per-invocation sandbox directory and export its path as PW_SANDBOX.
 *
 * The sandbox is the single rooted tree under which every test-side artifact
 * lives: per-spec config trees, R workdirs, the shared data-home
 * (RSTUDIO_DATA_HOME), and the shared user home (HOME/USERPROFILE). The
 * companion sandbox-teardown removes the subtree once the run finishes
 * (unless preserved by the rules in that file).
 *
 * Env vars (all optional):
 *   PW_SANDBOX_ROOT                Parent dir for the sandbox subtree. Defaults
 *                                  to os.tmpdir().
 *   PW_SANDBOX_ROOT_CREATE         "1"/"true" to mkdir PW_SANDBOX_ROOT if
 *                                  missing. Default is to fail loudly on a
 *                                  missing parent so typos surface immediately.
 *   PW_SANDBOX_NO_SEED_CREDENTIALS "1"/"true" to opt out of copying real AI
 *                                  credentials into the sandbox. By default,
 *                                  if `~/.positai/` and/or the GitHub Copilot
 *                                  config dir exist on the host, they're
 *                                  copied into the sandbox user-home so the
 *                                  matching @ai tests start authenticated.
 *                                  AI tests skip when the corresponding
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
 * subtree, and PW_AI_SEEDED_POSITAI / PW_AI_SEEDED_COPILOT to "1" for each
 * provider whose credentials were successfully copied (consumed by
 * requireAiCredentials in @ai test files). Workers inherit them via the
 * normal child-process env; globalTeardown reads PW_SANDBOX directly from
 * process.env.
 */
export default async function globalSetup() {
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

  const userHome = path.join(sandbox, 'user-home');
  fs.mkdirSync(userHome, { recursive: true });

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

  // Seed AI credentials by default when the host has them. The matching @ai
  // tests run requireAiCredentials() which gates each describe on the
  // corresponding PW_AI_SEEDED_* flag set below; unseeded providers surface
  // as a clean skip-with-reason rather than a 5-minute mystery failure
  // waiting for a completion that will never arrive.
  //
  // Clear the flags up front so only values this function sets are honored
  // -- a stray PW_AI_SEEDED_POSITAI=1 inherited from the user's shell or a
  // prior partially-cleaned run could otherwise smuggle past
  // requireAiCredentials() even when no credentials were actually copied.
  delete process.env.PW_AI_SEEDED_POSITAI;
  delete process.env.PW_AI_SEEDED_COPILOT;

  const skipSeeding = ['1', 'true'].includes(
    (process.env.PW_SANDBOX_NO_SEED_CREDENTIALS ?? '').toLowerCase(),
  );

  if (!skipSeeding) {
    const realPositai = path.join(os.homedir(), '.positai');
    if (fs.existsSync(realPositai)) {
      try {
        fs.cpSync(realPositai, path.join(userHome, '.positai'), { recursive: true });
        process.env.PW_AI_SEEDED_POSITAI = '1';
        console.log(`[sandbox] seeded user-home/.positai from ${realPositai}`);
        console.warn(
          `[sandbox] WARNING: Real Posit AI credentials were copied into the sandbox from ~/.positai. Tokens persist if the run is preserved or teardown fails. Set PW_SANDBOX_NO_SEED_CREDENTIALS=1 to opt out.`,
        );
      } catch (err) {
        throw new Error(
          `Failed copying ${realPositai} into sandbox: ${(err as Error).message}`,
        );
      }
    } else {
      console.log(
        `[sandbox] no ~/.positai/ on host; @ai Posit Assistant tests will skip`,
      );
    }

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
        console.warn(
          `[sandbox] WARNING: Real GitHub Copilot credentials were copied into the sandbox from ${realCopilot}. Tokens persist if the run is preserved or teardown fails. Set PW_SANDBOX_NO_SEED_CREDENTIALS=1 to opt out.`,
        );
      } catch (err) {
        throw new Error(
          `Failed copying ${realCopilot} into sandbox: ${(err as Error).message}`,
        );
      }
    } else {
      console.log(
        `[sandbox] no ${realCopilot} on host; @ai Copilot tests will skip`,
      );
    }
  } else {
    console.log('[sandbox] PW_SANDBOX_NO_SEED_CREDENTIALS set; @ai tests will skip');
  }

  process.env.PW_SANDBOX = sandbox;
  console.log(`[sandbox] root: ${sandbox}`);

  // Stable per-host R library, lives outside PW_SANDBOX so it survives across
  // runs. Without this the redirected HOME (set by Desktop/Server fixtures)
  // points R at an empty default user library, and the first thing rmarkdown
  // does on save is open a "stringr needs updating" dialog that hangs the
  // test. Pre-populating here is idempotent -- a warm cache is a fast
  // installed.packages() check with no install call.
  await prepareRLibs();

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
