import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { prepareRLibs } from './r-libs-setup';

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
 *   PW_SANDBOX_ROOT          Parent dir for the sandbox subtree. Defaults to
 *                            os.tmpdir().
 *   PW_SANDBOX_ROOT_CREATE   "1"/"true" to mkdir PW_SANDBOX_ROOT if missing.
 *                            Default is to fail loudly on a missing parent so
 *                            typos surface immediately.
 *   PW_SANDBOX_SEED_POSITAI  "1"/"true" to copy the real ~/.positai/ into the
 *                            sandbox user-home so Posit Assistant tests start
 *                            signed in. Default is unseeded -- tests start
 *                            signed out and must run the in-app sign-in flow.
 *                            Privacy note: real OAuth/API tokens are copied
 *                            into the sandbox. If the run is preserved (test
 *                            failure or PW_SANDBOX_SKIP_CLEANUP=1), the copied
 *                            tokens persist there until the sandbox is removed.
 *   PW_SANDBOX_SEED_COPILOT  "1"/"true" to copy the real GitHub Copilot
 *                            credentials into the sandbox user-home so
 *                            Copilot tests start authenticated. Default is
 *                            unseeded -- tests start unauthenticated. Source
 *                            path is platform-specific:
 *                              Windows: %LOCALAPPDATA%\github-copilot\
 *                              macOS/Linux: ~/.config/github-copilot/
 *                            Privacy note: real OAuth tokens are copied into
 *                            the sandbox. If the run is preserved (test
 *                            failure or PW_SANDBOX_SKIP_CLEANUP=1), the copied
 *                            tokens persist there until the sandbox is removed.
 *
 * Sets PW_SANDBOX (internal) to the absolute path of the auto-created
 * subtree. Workers inherit it via the normal child-process env, and
 * globalTeardown reads it directly from process.env.
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

  const seedPositai = ['1', 'true'].includes(
    (process.env.PW_SANDBOX_SEED_POSITAI ?? '').toLowerCase(),
  );
  if (seedPositai) {
    const realPositai = path.join(os.homedir(), '.positai');
    if (fs.existsSync(realPositai)) {
      try {
        fs.cpSync(realPositai, path.join(userHome, '.positai'), { recursive: true });
      } catch (err) {
        throw new Error(
          `PW_SANDBOX_SEED_POSITAI: failed copying ${realPositai} into sandbox: ${(err as Error).message}`,
        );
      }
      console.log(`[sandbox] seeded user-home/.positai from ${realPositai}`);
      console.warn(
        `[sandbox] WARNING: Real Posit AI credentials were copied into the sandbox from ~/.positai. Tokens persist if the run is preserved or teardown fails. Only use this on machines with a dedicated test account.`,
      );
    } else {
      console.log(
        `[sandbox] PW_SANDBOX_SEED_POSITAI set but no real ~/.positai/ found; tests will start signed out of Posit Assistant`,
      );
    }
  }

  const seedCopilot = ['1', 'true'].includes(
    (process.env.PW_SANDBOX_SEED_COPILOT ?? '').toLowerCase(),
  );
  if (seedCopilot) {
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
      } catch (err) {
        throw new Error(
          `PW_SANDBOX_SEED_COPILOT: failed copying ${realCopilot} into sandbox: ${(err as Error).message}`,
        );
      }
      console.log(`[sandbox] seeded user-home github-copilot from ${realCopilot}`);
      console.warn(
        `[sandbox] WARNING: Real GitHub Copilot credentials were copied into the sandbox from ${realCopilot}. Tokens persist if the run is preserved or teardown fails. Only use this on machines with a dedicated test account.`,
      );
    } else {
      console.log(
        `[sandbox] PW_SANDBOX_SEED_COPILOT set but no real ${realCopilot} found; Copilot tests will start unauthenticated`,
      );
    }
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
}
