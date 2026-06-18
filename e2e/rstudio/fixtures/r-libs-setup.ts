import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import { spawnSync } from 'child_process';

import { heredoc } from '../utils/heredoc';

/**
 * Stable per-R-version package library used by every Playwright run, shared
 * across runs so the same set of CRAN packages doesn't get reinstalled every
 * time. We can't reuse the host user's library because the Desktop/Server
 * fixtures redirect HOME / USERPROFILE into the per-run sandbox (see
 * desktop.fixture.ts) -- under the redirected HOME R computes an empty
 * default user library, which is what surfaces the "stringr needs to be
 * updated" popup from rmarkdown's dependency check.
 *
 * The path contains R's R_LIBS_USER tokens (`%p` platform, `%v` R x.y) so a
 * single string works across architectures and R versions without templating
 * in TypeScript. R expands them at rsession startup; we expand them once here
 * via Rscript for the pre-create / pre-install step.
 *
 * Env var: PW_RSTUDIO_R_LIBS_USER overrides the default template entirely.
 */

const RSTUDIO_R_LIBS_USER_ENV = 'PW_RSTUDIO_R_LIBS_USER';
const RSTUDIO_R_LIBS_SKIP_PREP_ENV = 'PW_RSTUDIO_R_LIBS_SKIP_PREP';

// globalSetup exports the concrete (token-expanded) path of the prebuilt
// "template" library here, so workers can clone it without re-running the
// Rscript expansion. Empty/unset when prep was skipped or expansion failed --
// callers then fall back to the shared template path (single-worker behavior).
const RSTUDIO_R_LIBS_TEMPLATE_ENV = 'PW_R_LIBS_TEMPLATE';
// globalSetup exports the resolved worker count so the per-worker partitioning
// below activates only when actually running in parallel.
const TOTAL_WORKERS_ENV = 'PW_TOTAL_WORKERS';

/**
 * Packages every Playwright run should be able to find without paying the
 * lazy-install tax inside an individual test. The list intentionally mirrors
 * what the suite already references via `ensurePackages` -- pre-populating it
 * once in globalSetup means tests start with the dependencies they expect.
 *
 * Adding a new package here is cheap; trim only when a package is genuinely
 * unused by any test in the suite.
 */
export const REQUIRED_PACKAGES = [
  'DBI',
  'MASS',
  'S7',
  'bslib',
  'data.table',
  'devtools',
  'dplyr',
  'evaluate',
  'ggplot2',
  'knitr',
  'nycflights13',
  'pillar',
  'praise',
  'remotes',
  'reticulate',
  'rmarkdown',
  'rstudioapi',
  'shiny',
  'shinytest2',
  'stringr',
  'styler',
  'testthat',
  'tibble',
] as const;

/**
 * Build the default R_LIBS_USER template. Resolved *before* HOME is redirected,
 * so it points at a stable per-host cache rather than into the per-run sandbox.
 *
 *   macOS / Linux:  ~/.cache/rstudio-playwright/r-libs/%p/%v
 *   Windows:        %LOCALAPPDATA%\rstudio-playwright\r-libs\%p\%v
 *                   (or ~/AppData/Local/... when LOCALAPPDATA is unset)
 */
function defaultRLibsUserTemplate(): string {
  if (process.platform === 'win32') {
    const base = process.env.LOCALAPPDATA
      ?? path.join(os.homedir(), 'AppData', 'Local');
    return path.join(base, 'rstudio-playwright', 'r-libs', '%p', '%v');
  }
  return path.join(os.homedir(), '.cache', 'rstudio-playwright', 'r-libs', '%p', '%v');
}

/**
 * Resolve the R_LIBS_USER template -- either the explicit override from
 * PW_RSTUDIO_R_LIBS_USER or the per-platform default. Returns the *unexpanded*
 * template (still contains %p / %v / etc.) -- those tokens are interpreted by
 * R itself at startup.
 */
export function rLibsUserTemplate(): string {
  return process.env[RSTUDIO_R_LIBS_USER_ENV] ?? defaultRLibsUserTemplate();
}

interface RscriptResult {
  stdout: string;
  stderr: string;
  status: number | null;
}

/**
 * Run `Rscript -e <code>` and return the captured streams. We don't use a
 * shell to avoid quoting hazards when the script contains R syntax. Caller
 * decides what to do with a non-zero exit.
 */
function runRscript(code: string, env: NodeJS.ProcessEnv = process.env): RscriptResult {
  const result = spawnSync('Rscript', ['--no-save', '--no-restore', '-e', code], {
    env,
    encoding: 'utf8',
  });
  return {
    stdout: result.stdout ?? '',
    stderr: result.stderr ?? '',
    status: result.status,
  };
}

/**
 * Expand the R_LIBS_USER template into a concrete filesystem path by asking R
 * what `tools:::.expand_R_libs_env_var` returns for it. We must set the env
 * var explicitly on the child so R sees our template even if the caller's
 * environment defines something else.
 */
function expandRLibsUserTemplate(template: string): string | null {
  const env = { ...process.env, R_LIBS_USER: template };
  const code = heredoc`
    tryCatch({
      cat(tools:::.expand_R_libs_env_var(Sys.getenv("R_LIBS_USER")))
    }, error = function(e) {
      t <- Sys.getenv("R_LIBS_USER")
      t <- gsub("%V", paste0(R.version$major, ".", R.version$minor), t, fixed = TRUE)
      t <- gsub("%v", paste0(R.version$major, ".", strsplit(R.version$minor, ".", fixed = TRUE)[[1]][1]), t, fixed = TRUE)
      t <- gsub("%p", R.version$platform, t, fixed = TRUE)
      t <- gsub("%o", R.version$os, t, fixed = TRUE)
      t <- gsub("%a", R.version$arch, t, fixed = TRUE)
      cat(t)
    })
  `;

  const res = runRscript(code, env);
  if (res.status !== 0) return null;
  const expanded = res.stdout.trim();
  return expanded.length > 0 ? expanded : null;
}

/**
 * Pre-create the R user library and install any packages from
 * REQUIRED_PACKAGES that are missing. Idempotent: a warm cache results in a
 * fast `installed.packages()` check and no install call.
 *
 * Skipped entirely when PW_RSTUDIO_R_LIBS_SKIP_PREP is "1" / "true" -- useful
 * for runs against an external R install that already has everything, or to
 * debug the redirected-empty-lib behavior on purpose.
 */
export async function prepareRLibs(): Promise<string | null> {
  const skip = ['1', 'true'].includes(
    (process.env[RSTUDIO_R_LIBS_SKIP_PREP_ENV] ?? '').toLowerCase(),
  );
  if (skip) {
    console.log(`[r-libs] skipping prep (${RSTUDIO_R_LIBS_SKIP_PREP_ENV} set)`);
    return null;
  }

  const template = rLibsUserTemplate();
  const expanded = expandRLibsUserTemplate(template);
  if (!expanded) {
    console.warn(
      `[r-libs] could not expand R_LIBS_USER template "${template}" via Rscript; skipping pre-population. Is Rscript on PATH?`,
    );
    return null;
  }

  fs.mkdirSync(expanded, { recursive: true });
  console.log(`[r-libs] user library: ${expanded}`);

  const repos = process.platform === 'linux'
    ? 'https://packagemanager.posit.co/cran/__linux__/jammy/latest'
    : 'https://cran.r-project.org';

  // Single Rscript invocation: check installed.packages() in the resolved
  // library, then install any missing entries from the manifest in one batch.
  // Faster than the per-package check loop ensurePackages uses, and we don't
  // need its progress markers because there's no console UI here.
  //
  // installType is chosen at runtime via .Platform$pkgType so source-only R
  // builds (e.g. some macOS configurations where CRAN has no matching binary)
  // don't fall over on a forced type = "binary".
  const pkgsLiteral = REQUIRED_PACKAGES.map((p) => `"${p}"`).join(', ');
  const installCode = heredoc`
    lib <- ${JSON.stringify(expanded)}
    .libPaths(c(lib, .libPaths()))
    required <- c(${pkgsLiteral})
    have <- rownames(installed.packages(lib.loc = lib))
    missing <- setdiff(required, have)
    if (length(missing) == 0L) {
      cat("[r-libs] all packages present\n")
    } else {
      cat("[r-libs] installing:", paste(missing, collapse = ", "), "\n")
      installType <- if (identical(.Platform$pkgType, "source")) "source" else "binary"
      install.packages(missing, lib = lib, repos = ${JSON.stringify(repos)}, type = installType)
      still <- setdiff(missing, rownames(installed.packages(lib.loc = lib)))
      if (length(still) > 0L) {
        cat("[r-libs] WARNING: failed to install:", paste(still, collapse = ", "), "\n")
      }
    }
  `;

  const env = { ...process.env, R_LIBS_USER: template };
  const res = runRscript(installCode, env);
  if (res.stdout) process.stdout.write(res.stdout);
  if (res.stderr) process.stderr.write(res.stderr);
  if (res.status !== 0) {
    console.warn(
      `[r-libs] Rscript exited ${res.status}; some packages may be missing. Tests that need them will fall back to per-test ensurePackages().`,
    );
  }

  return expanded;
}

/**
 * Recursively clone a directory tree using hardlinks for regular files. Files
 * share the source inode (so the clone is near-instant and costs almost no
 * disk), but they are independent directory entries: deleting one (as
 * remove.packages does) leaves the other intact, and installing a new package
 * writes a brand-new inode -- so a worker mutating its own library clone never
 * touches the template or another worker's clone.
 *
 * Cross-platform: fs.linkSync maps to CreateHardLinkW on Windows/NTFS, which
 * needs no elevation (unlike symlinks). Hardlinks cannot span filesystems, so
 * src and dst must live on the same volume -- callers keep the clone beside the
 * template to guarantee that. Symlinks in the tree (rare in an R library) are
 * recreated as symlinks rather than hardlinked.
 */
function cloneTreeHardlinks(src: string, dst: string): void {
  fs.mkdirSync(dst, { recursive: true });
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const s = path.join(src, entry.name);
    const d = path.join(dst, entry.name);
    if (entry.isDirectory()) {
      cloneTreeHardlinks(s, d);
    } else if (entry.isSymbolicLink()) {
      fs.symlinkSync(fs.readlinkSync(s), d);
    } else {
      fs.linkSync(s, d);
    }
  }
}

/**
 * Resolve the R_LIBS_USER value for the current worker.
 *
 * Single-worker runs (the default) return the shared template token unchanged
 * -- byte-for-byte the historical behavior, with installs persisting in the
 * per-host library across runs.
 *
 * Parallel runs give every worker its own hermetic library: a full hardlink
 * clone of the prebuilt template, keyed on the (stable, bounded) parallel
 * index and persisted beside the template. Because the clone is the only entry
 * on the library path -- the shared template is NOT layered in -- a test can
 * freely install or remove packages (e.g. the praise / DBI uninstall tests)
 * without the package reappearing from a shared lib or leaking to other
 * workers. Clones are reused across runs; delete the `*-w<N>` siblings of the
 * template to force a refresh (e.g. after growing REQUIRED_PACKAGES).
 *
 * Falls back to the template token when the template path is unavailable
 * (prep skipped or expansion failed), so parallel runs still launch -- just
 * without the hermetic guarantee.
 */
export function workerRLibsUser(): string {
  const template = rLibsUserTemplate();

  const totalWorkers = Number(process.env[TOTAL_WORKERS_ENV] ?? '1');
  if (!Number.isFinite(totalWorkers) || totalWorkers <= 1) {
    return template;
  }

  const templateExpanded = process.env[RSTUDIO_R_LIBS_TEMPLATE_ENV];
  if (!templateExpanded) {
    console.warn(
      `[r-libs] ${TOTAL_WORKERS_ENV}=${totalWorkers} but ${RSTUDIO_R_LIBS_TEMPLATE_ENV} is unset ` +
        `(prep skipped or expansion failed); falling back to the shared library. ` +
        `Per-worker hermetic isolation is OFF -- uninstall/reinstall tests may race across workers.`,
    );
    return template;
  }

  const idx = Number(process.env.TEST_PARALLEL_INDEX ?? '0') || 0;
  // Sibling of the (R-version-specific) template path, so the clone is on the
  // same volume and implicitly carries the right platform/R version.
  const clone = `${templateExpanded}-w${idx}`;
  if (!fs.existsSync(clone)) {
    console.log(`[r-libs] cloning template into per-worker library: ${clone}`);
    // Clone into a temp sibling and atomically rename, so a crash mid-clone
    // can't leave a partial directory that later reads as a complete library.
    // The parallel index is exclusive to one worker process at a time, so the
    // temp name only needs to be unique against a prior aborted attempt.
    const tmp = `${clone}.partial`;
    fs.rmSync(tmp, { recursive: true, force: true });
    cloneTreeHardlinks(templateExpanded, tmp);
    fs.renameSync(tmp, clone);
  }
  return clone;
}
