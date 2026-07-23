import { defineConfig, ReporterDescription } from '@playwright/test';
import dotenv from 'dotenv';
import os from 'os';
import path from 'path';

type ProjectOptions = { mode: 'desktop' | 'server' };

// Supported values for the PW_TRACE / PW_SCREENSHOT env escalations (a subset
// of Playwright's string options, which is all we expose).
const TRACE_MODES = ['off', 'on', 'retain-on-failure', 'on-first-retry', 'on-all-retries'] as const;
const SCREENSHOT_MODES = ['off', 'on', 'only-on-failure'] as const;

// Resolve an env override against its allowed values, falling back to the
// default when unset. An unrecognized value is a hard error (matching the
// PW_RSTUDIO_* env handling below) rather than being passed through to
// Playwright, where a typo like PW_TRACE=retain-on-falure would otherwise turn
// a debugging aid into a config error at startup.
function resolveEnvMode<T extends string>(name: string, allowed: readonly T[], fallback: T): T {
  const value = process.env[name];
  if (value === undefined || value === '')
    return fallback;
  if (!allowed.includes(value as T))
    throw new Error(`${name}="${value}" -- expected one of: ${allowed.join(', ')}`);
  return value as T;
}

// Load env vars from a dotenv file before any process.env reads below.
// Path is anchored to this config's directory so it works regardless of cwd.
// PW_ENV_FILE overrides the default path; existing process.env values win
// (dotenv does not overwrite vars already set in the shell). When PW_ENV_FILE
// is set explicitly, a missing/unreadable file is a hard error to catch typos.
const envFile = process.env.PW_ENV_FILE;
const envFilePath = path.resolve(__dirname, envFile ?? '.env.local');
const dotenvResult = dotenv.config({ path: envFilePath });
if (envFile && dotenvResult.error) {
  throw new Error(`PW_ENV_FILE="${envFile}" set but could not load ${envFilePath}: ${dotenvResult.error.message}`);
}

const platform = os.platform();
const desktopOsExclusions: string[] = [];
if (platform !== 'win32')  desktopOsExclusions.push('@windows_only');
if (platform !== 'darwin') desktopOsExclusions.push('@macos_only');
if (platform !== 'linux')  desktopOsExclusions.push('@linux_only');

// Server always targets Linux regardless of host OS, so its OS filter is hardcoded.
const serverOsExclusions = ['@windows_only', '@macos_only'];

const edition = (process.env.PW_RSTUDIO_EDITION ?? 'os').toLowerCase();
if (edition !== 'os' && edition !== 'pro') {
  throw new Error(`PW_RSTUDIO_EDITION="${edition}" -- expected "os" or "pro"`);
}
const editionExclusions = edition === 'os' ? ['@pro_only'] : ['@os_only'];

const setupProject = {
  name: 'setup',
  testMatch: /tests[\\/]auth\.setup\.ts$/,
  // Never retry the setup: only its deterministic (fail-loud) path throws --
  // wrong credentials, an OAuth config fault -- and retrying that means a
  // second live bad-credential sign-in against login.posit.cloud, plus the
  // chance a flaky retry passes and launders the fail-loud verdict into
  // green-with-skips. The transient path returns normally (tests skip via
  // the status file), so it was never retried anyway. This overrides the
  // config-level retries (including the CI branch below); a manual
  // --retries=N on the CLI still outranks it (the CI workflows don't pass
  // one today, and shouldn't).
  retries: 0,
  // Artifacts stay off for this project: the sign-in flows type real
  // credentials (POSIT_PASSWORD, COPILOT_PASSWORD) into pages, and a
  // trace, video, or screenshot would capture them into the report.
  // Diagnostic context comes from the flows' own error messages instead
  // (step name, page URL); PW_DEBUG_AUTH_CAPTURE=1 opts into flow-owned page dumps
  // for debugging login-page changes (see utils/auth-debug.ts).
  use: { trace: 'off' as const, video: 'off' as const, screenshot: 'off' as const },
};

// The @smoke startup test is a long, low-information liveness check that just
// idles for 30s. It is redundant alongside the full suite, and with no timeout
// headroom (and full runs often capping the global timeout low) it is the first
// casualty under parallel load. Exclude it by default; opt in with PW_RUN_SMOKE.
const smokeExclusions = process.env.PW_RUN_SMOKE ? [] : ['@smoke'];

const allProjects = [
  {
    name: 'desktop',
    use: { mode: 'desktop' as const },
    grepInvert: new RegExp(['@server_only', ...desktopOsExclusions, ...editionExclusions, ...smokeExclusions].join('|')),
    dependencies: ['setup'],
  },
  {
    name: 'server',
    use: { mode: 'server' as const },
    grepInvert: new RegExp(['@desktop_only', ...serverOsExclusions, ...editionExclusions, ...smokeExclusions].join('|')),
    dependencies: ['setup'],
  },
];

if (process.env.PW_PROJECT) {
  console.warn('PW_PROJECT is no longer used; switch to --project=desktop|server or PW_RSTUDIO_MODE=desktop|server');
}

// Worker processes re-load this config without --project in their argv, so we can't
// rely on argv to pick the project there. Parse --project once in the main process and
// forward it via PW_RSTUDIO_MODE; workers inherit process.env at fork time and end up
// filtering to the same project the main process picked.
function parseProjectFromArgv(): string | undefined {
  const argv = process.argv;
  for (let i = 0; i < argv.length; i++) {
    if (argv[i] === '--project' && i + 1 < argv.length) return argv[i + 1];
    const prefix = '--project=';
    if (argv[i].startsWith(prefix)) return argv[i].slice(prefix.length);
  }
  return undefined;
}

const argvProject = parseProjectFromArgv()?.toLowerCase();
if (argvProject) {
  // --project wins over a pre-existing PW_RSTUDIO_MODE (README documents this).
  process.env.PW_RSTUDIO_MODE = argvProject;
}

const modeEnv = process.env.PW_RSTUDIO_MODE?.toLowerCase() ?? 'desktop';
if (modeEnv !== 'desktop' && modeEnv !== 'server') {
  throw new Error(`PW_RSTUDIO_MODE="${modeEnv}" -- expected "desktop" or "server"`);
}

// Display label for the active project, surfaced as the project "name" in
// reports. CI sets a distinct label per platform (e.g. desktop-macos,
// desktop-windows, server-linux) so a cross-platform merged report can tell
// results apart -- without it every desktop platform collapses under a single
// indistinguishable "desktop" group. The label is env-only: CI does not pass
// --project (the config already narrows to one project), so mode selection
// never sees the label. Defaults to the mode for local runs, so
// `--project desktop|server` keeps working unchanged. NOTE: the E2E Test
// Insights reporter also records this as its dashboard "project" field, so
// per-platform labels show up there as distinct series.
const projectLabel = process.env.PW_PROJECT_LABEL || modeEnv;
const projects = [
  // The auth setup project runs auth.setup.ts (raw chromium, no RStudio launch),
  // so mode is inert for it -- it's set only to satisfy ProjectOptions. modeEnv
  // is validated to be 'desktop'|'server' above, so the cast is sound.
  { ...setupProject, use: { ...setupProject.use, mode: modeEnv as ProjectOptions['mode'] } },
  ...allProjects
    .filter(p => p.name === modeEnv)
    .map(p => ({ ...p, name: projectLabel })),
];

const testIgnore = (process.env.PW_TEST_IGNORE ?? '')
  .split(/\s+/)
  .filter(Boolean);

// Reporters common to every environment. The HTML report is also uploaded as
// a CI artifact (view locally with `npx playwright show-report`).
const reporters: ReporterDescription[] = [
  ['html'],
  ['list'],
  ['./fixtures/sandbox-reporter.ts'],
];

// On CI, add the 'github' reporter so failures are annotated inline on the
// pull request at the failing file/line.
if (process.env.CI)
  reporters.unshift(['github']);

// On GitHub Actions, also render a results table on the workflow run's Summary
// page. This reporter writes to $GITHUB_STEP_SUMMARY, so test results are
// viewable online directly from the run page without downloading and unzipping
// the HTML report artifact. useDetails collapses the (potentially large) table
// behind a disclosure so the summary stays scannable; showError inlines the
// failure message for each failing test.
if (process.env.GITHUB_ACTIONS)
  reporters.unshift(['@estruyf/github-actions-reporter', {
    title: 'RStudio Playwright Results',
    useDetails: true,
    showError: true,
  }]);

// Sharded CI runs use blob reporter so the merge job can reassemble a single
// HTML report and accurate counts from all shards. The blob file name embeds
// the platform label and shard: the default report-<shard>.zip would collide
// across platforms (mac shard 1 vs windows shard 1) when the PR run flattens
// every platform's blobs into one directory to merge. Other human-facing
// reporters are suppressed per-shard. 'list' is kept so the CI log shows
// per-test progress (blob is silent during the run); this also gives the
// run-with-heartbeat watchdog a reliable heartbeat -- a line per test -- so a
// hung run is detected by its silence. sandbox-reporter is kept so teardown can
// still detect failures and preserve sandbox state for artifact upload. The E2E
// Test Insights reporter rides alongside blob so per-shard results also reach
// the dashboard. Without CONNECT_API_KEY (e.g. fork PRs) it still runs but its
// uploads fail with warnings (never fails the run).
if (process.env.GITHUB_ACTIONS && process.env.PW_SHARD)
  reporters.splice(0, reporters.length,
    ['blob', { fileName: `report-${projectLabel}-${process.env.PW_SHARD}.zip` }],
    ['list'],
    ['./fixtures/sandbox-reporter.ts'],
    ['@midleman/playwright-reporter', { mode: 'prod' }]);

export default defineConfig<{}, ProjectOptions>({
  testDir: './tests',
  testIgnore: testIgnore.length > 0 ? testIgnore : undefined,
  fullyParallel: false,
  workers: 1,
  // Global per-test budget. Kept low so a hung test fails fast rather than
  // parking a worker for minutes; individual slow tests opt up with
  // test.setTimeout() (e.g. package installs in a beforeAll), and slow
  // individual actions pass their own { timeout } (see TIMEOUTS).
  timeout: 120000,
  // On CI a single transient launch flake (e.g. GWT app slow to reach
  // ready under cold disk caches on a fresh runner) can otherwise turn
  // the whole suite red. One retry absorbs that without rerunning by hand.
  retries: process.env.CI ? 1 : 0,
  reporter: reporters,
  globalSetup: './fixtures/sandbox-setup.ts',
  globalTeardown: './fixtures/sandbox-teardown.ts',
  use: {
    // Capture a screenshot at the moment of failure and retain a full trace
    // (DOM snapshots, console, network) for any failing test -- not just on a
    // retry, which never happens locally (retries: 0). View with
    // `npx playwright show-trace <trace.zip>`.
    //
    // Both can be escalated via env for a run that passes but behaves oddly
    // (e.g. leaves a modal that only breaks a *later* test), where the
    // on-failure default captures nothing: PW_TRACE=on / PW_SCREENSHOT=on.
    screenshot: resolveEnvMode('PW_SCREENSHOT', SCREENSHOT_MODES, 'only-on-failure'),
    trace: resolveEnvMode('PW_TRACE', TRACE_MODES, 'retain-on-failure'),
    actionTimeout: 10000,
  },
  projects,
});
