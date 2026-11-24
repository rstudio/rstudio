import type { Page } from '@playwright/test';
import { spawnSync } from 'child_process';
import * as os from 'os';
import * as path from 'path';
import { test } from '../fixtures/rstudio.fixture';
import { ConsolePaneActions } from '../actions/console_pane.actions';
import { clearConsole, executeInConsole, getConsolePromptText } from '../pages/console_pane.page';
import { clearPref, setPref } from './commands';
import { rStringLiteral } from './r';

/**
 * Support for the Julia console specs (tests/julia/).
 *
 * The Julia console needs a Julia the session can find and the JuliaConnectoR
 * package. Not every machine has Julia (in CI, only some runner images come
 * with one), so specs that start it call useJulia(), which skips their tests
 * where it is missing. JuliaConnectoR is in required-packages.txt; where the
 * library lacks it all the same, it is installed once Julia has been found.
 *
 * The session runs with a sandboxed home directory, where Julia would start
 * from nothing on every run: juliaup's launcher downloads a whole Julia into
 * it, and JuliaConnectoR installs and precompiles the Julia package it needs
 * (Tables.jl) into an empty depot. So the specs find Julia from the test
 * process, whose home is the real one, and hand the session:
 *
 *   JULIA_BINDIR      the directory of the Julia found here, which the
 *                     session then runs directly, not through juliaup
 *   JULIA_DEPOT_PATH  a depot of the tests' own, kept between runs beside
 *                     the R library (see fixtures/r-libs-setup.ts), so only
 *                     the first run installs Tables.jl, and the developer's
 *                     own depot is left alone
 *
 * PW_RSTUDIO_JULIA_BINDIR and PW_RSTUDIO_JULIA_DEPOT_PATH replace either
 * (paths as the rsession host sees them); CI sets the second to the depot it
 * prepares and caches (.github/actions/os-e2e-deps). Two cases leave the
 * session to its own devices: a directory found here that the session
 * doesn't have (it runs on another host) isn't used, and neither is a depot
 * the session can't write to (it runs as another user), so Julia then uses
 * that user's default depot.
 *
 * PW_RSTUDIO_JULIA_UNAVAILABLE, set to a reason, skips the tests outright:
 * CI sets it when it couldn't prepare the depot.
 */

const JULIA_BINDIR_ENV = 'PW_RSTUDIO_JULIA_BINDIR';
const JULIA_DEPOT_PATH_ENV = 'PW_RSTUDIO_JULIA_DEPOT_PATH';
const JULIA_UNAVAILABLE_ENV = 'PW_RSTUDIO_JULIA_UNAVAILABLE';

export const JULIA_PROMPT = 'julia> ';
export const R_PROMPT = '> ';

// how long Julia's first start in a fresh depot may take
export const JULIA_START_TIMEOUT = 240_000;

// The directory of the Julia installed on this machine, or null when there
// is none. Asked of Julia itself: a juliaup launcher answers with the
// directory of the Julia it runs.
function findJuliaBinDir(): string | null {
  const launchers = ['julia'];
  if (process.platform === 'darwin')
    launchers.push('/opt/homebrew/bin/julia', '/usr/local/bin/julia');

  for (const launcher of launchers) {
    const result = spawnSync(
      launcher,
      ['--startup-file=no', '-e', 'print(Sys.BINDIR)'],
      { encoding: 'utf8', timeout: 60_000 },
    );

    const bindir = result.status === 0 ? result.stdout.trim() : '';
    if (bindir.length > 0)
      return bindir;
  }

  return null;
}

// The tests' own Julia depot, kept between runs, as a JULIA_DEPOT_PATH: the
// trailing separator keeps Julia's bundled depots on the path.
function testDepotPath(): string {
  const base = process.platform === 'win32'
    ? process.env.LOCALAPPDATA || path.join(os.homedir(), 'AppData', 'Local')
    : path.join(os.homedir(), '.cache');
  return path.join(base, 'rstudio-playwright', 'julia-depot') + path.delimiter;
}

// R code that points the session at the Julia in `bindir` and at the depot
// path `depotPath`, each only where the session can use it: see the notes at
// the top of this file.
function pointSessionAtJulia(bindir: string, bindirGiven: boolean, depotPath: string, depotGiven: boolean): string {
  return 'local({ ' + [
    `bindir <- ${rStringLiteral(bindir)}`,
    `depotPath <- ${rStringLiteral(depotPath)}`,
    `useBindir <- nzchar(bindir) && (${bindirGiven ? 'TRUE' : 'FALSE'} || dir.exists(bindir))`,
    'if (useBindir) Sys.setenv(JULIA_BINDIR = bindir)',
    `if (!useBindir && !${depotGiven ? 'TRUE' : 'FALSE'}) return(invisible())`,
    'depot <- strsplit(depotPath, .Platform$path.sep, fixed = TRUE)[[1L]][[1L]]',
    'dir.create(depot, recursive = TRUE, showWarnings = FALSE)',
    'if (file.access(depot, 2L) == 0L) Sys.setenv(JULIA_DEPOT_PATH = depotPath)',
    'invisible()',
  ].join('; ') + ' })';
}

// Evaluates to "ok", or to why the session has no Julia the console supports.
// (one line: the console takes it as a single input)
const JULIA_STATUS = 'local({ ' + [
  'exe <- .rs.julia.executable()',
  'if (is.null(exe) || !file.exists(exe)) return("Julia isn\'t installed")',
  'version <- tryCatch(suppressWarnings(system2(exe, "--version", stdout = TRUE, stderr = FALSE)), error = function(e) character())',
  'version <- regmatches(version, regexpr("[0-9]+[.][0-9]+", version))',
  'if (length(version) == 0L) return("Julia\'s version can\'t be told")',
  'if (numeric_version(version) < .rs.julia.minimumVersion) return("Julia is older than the console supports")',
  '"ok"',
].join('; ') + ' })';

/** The console's output so far, as text. */
export async function consoleOutput(page: Page): Promise<string> {
  return page.evaluate(() => document.getElementById('rstudio_console_output')?.innerText ?? '');
}

/** The text in the console's input line. */
export async function consoleInput(page: Page): Promise<string> {
  return page.evaluate(() => {
    const el = document.getElementById('rstudio_console_input') as any;
    return el?.env?.editor?.getValue() ?? '';
  });
}

/** Run code at a cleared console and return the console's output. */
export async function runInConsole(page: Page, code: string, timeout = 60_000): Promise<string> {
  await clearConsole(page);
  await executeInConsole(page, code, { timeout });
  return consoleOutput(page);
}

/** Leave the Julia console for R, if the console is in it. */
export async function leaveJulia(page: Page): Promise<void> {
  if ((await getConsolePromptText(page)).startsWith('julia>'))
    await executeInConsole(page, 'exit');
}

/**
 * Point the session at Julia, and make sure the Julia console is on (a user
 * can turn it off). Returns why there is no Julia to test with, or null.
 */
async function setUpJulia(page: Page): Promise<string | null> {
  const unavailable = process.env[JULIA_UNAVAILABLE_ENV];
  if (unavailable)
    return unavailable;

  const givenBindir = process.env[JULIA_BINDIR_ENV];
  const givenDepotPath = process.env[JULIA_DEPOT_PATH_ENV];
  await executeInConsole(page, pointSessionAtJulia(
    givenBindir || findJuliaBinDir() || '',
    !!givenBindir,
    givenDepotPath || testDepotPath(),
    !!givenDepotPath,
  ));

  await setPref(page, 'julia_console_enabled', true);

  // not evalRString(): where the session has to find Julia itself, a juliaup
  // launcher in a new home can take minutes to answer
  await clearConsole(page);
  await executeInConsole(page, `(${JULIA_STATUS})`, { timeout: JULIA_START_TIMEOUT });
  const status = (await consoleOutput(page)).match(/\[1\]\s+"(.*)"/)?.[1];
  if (status !== 'ok')
    return status ?? "Julia's status can't be told";

  const consoleActions = new ConsolePaneActions(page);

  const missing = await consoleActions.ensurePackages(['JuliaConnectoR'], 180_000);
  if (missing.length > 0)
    return 'JuliaConnectoR could not be installed';

  return null;
}

/**
 * Call inside a describe block whose tests start Julia. Its tests are skipped
 * where the session has no Julia the console supports; where it has one,
 * Julia is stopped after the suite.
 */
export function useJulia(): void {
  let skipReason: string | null = null;

  test.beforeAll(async ({ rstudioPage: page }) => {
    test.setTimeout(JULIA_START_TIMEOUT + 240_000);
    skipReason = await setUpJulia(page);
  });

  test.beforeEach(() => {
    test.skip(skipReason !== null, skipReason ?? '');
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await leaveJulia(page);

    // a Julia left running would keep a later suite's session from suspending
    await executeInConsole(page, '.rs.julia.stop()');
    await clearPref(page, 'julia_console_enabled');
  });
}
