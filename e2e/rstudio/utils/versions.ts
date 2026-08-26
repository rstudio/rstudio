import * as fs from 'fs';
import * as path from 'path';
import type { Page } from '@playwright/test';
import { executeInConsole, clearConsole, CONSOLE_OUTPUT } from '../pages/console_pane.page';
import { getVersion } from './commands';

/**
 * What build this run exercised. Gathered once per worker from the live
 * session, then reported at the top of the Playwright HTML report and the
 * GitHub Actions job summary, so a report answers "what was this actually
 * run against?" without digging through logs.
 */
export interface RunVersions {
  /** RStudio long version, e.g. "2026.09.0-daily+109". */
  rstudio: string;
  /** RStudio release name, e.g. "Autumn Hawkbit"; empty when unreadable. */
  releaseName: string;
  /** R version, e.g. "4.6.1". */
  r: string;
  /** "Desktop" or "Server" -- which product this worker drove. */
  edition: string;
  /** Human OS label, e.g. "Ubuntu 24 ARM". */
  os: string;
}

/** File the workers write and the reporter reads, inside $PW_SANDBOX. */
const VERSIONS_FILE = 'run-versions.json';

/**
 * Read the release name ("Autumn Hawkbit") out of the live session. It isn't on
 * the automation bridge -- `window.rstudio.version` carries only the two version
 * strings -- and SessionInfo doesn't forward it either, so the R side is the only
 * place it's exposed (`.rs.api.versionInfo()`, see src/cpp/r/R/Api.R).
 *
 * The marker is split across cat() arguments so the literal string never appears
 * in the submitted command: the command echo travels the same output stream this
 * reads back, so an unsplit marker would match the echo whether or not R ran it.
 */
async function readReleaseName(page: Page): Promise<string> {
  try {
    await clearConsole(page);
    await executeInConsole(
      page,
      `cat("[pw", "release:", .rs.api.versionInfo()$release_name, "]\n", sep = "")`,
    );
    const output = await page.locator(CONSOLE_OUTPUT).innerText();
    return output.match(/\[pwrelease:([^\]]*)\]/)?.[1]?.trim() ?? '';
  } catch (err) {
    console.warn(`WARNING: could not read the RStudio release name: ${err}`);
    return '';
  } finally {
    await clearConsole(page).catch(() => {});
  }
}

/** Fall back to the host platform when CI hasn't supplied a nicer label. */
function osLabel(): string {
  if (process.env.PW_OS_LABEL) return process.env.PW_OS_LABEL;
  switch (process.platform) {
    case 'darwin': return 'macOS';
    case 'win32': return 'Windows';
    case 'linux': return 'Linux';
    default: return process.platform;
  }
}

/** Gather everything this run should report about the build under test. */
export async function collectRunVersions(
  page: Page,
  mode: 'desktop' | 'server',
): Promise<RunVersions> {
  const { rstudio, r } = await getVersion(page);
  return {
    rstudio,
    releaseName: await readReleaseName(page),
    r,
    edition: mode === 'server' ? 'Server' : 'Desktop',
    os: osLabel(),
  };
}

/** One-line rendering, used by both the report header and the job summary. */
export function formatRunVersions(v: RunVersions): string {
  const release = v.releaseName ? ` "${v.releaseName}"` : '';
  return [`RStudio ${v.rstudio}${release}`, `R ${v.r}`, v.edition].join(' · ');
}

/**
 * Publish this worker's findings for the reporter to pick up. Workers run in
 * their own processes, so mutating the report metadata directly from here would
 * never reach the main process -- the file in $PW_SANDBOX is the handoff. Every
 * worker in a run reports the same build, so a later write simply overwrites an
 * identical value.
 */
export function publishRunVersions(versions: RunVersions): void {
  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) return;
  try {
    fs.writeFileSync(path.join(sandbox, VERSIONS_FILE), JSON.stringify(versions));
  } catch (err) {
    console.warn(`WARNING: could not record run versions: ${err}`);
  }
}

/**
 * Put the same line at the top of the GitHub Actions run summary, so the build
 * under test is readable from the run page without opening the report.
 *
 * Written from the first worker to get here rather than from a workflow step:
 * only the harness knows these values, and only once a session is up. The
 * summary is append-only, so "top" means being the first thing written in the
 * job -- which holds as long as nothing upstream writes to it (the R-version
 * block in .github/actions/os-e2e-deps was removed for exactly this reason).
 *
 * The lock is an exclusive create, so of several workers racing here exactly one
 * wins and the block appears once. A worker that loses does nothing.
 */
export function writeJobSummary(versions: RunVersions): void {
  const summaryPath = process.env.GITHUB_STEP_SUMMARY;
  const sandbox = process.env.PW_SANDBOX;
  if (!summaryPath || !sandbox) return;
  try {
    fs.writeFileSync(path.join(sandbox, '.summary-written'), '', { flag: 'wx' });
  } catch {
    return; // Another worker already wrote it.
  }
  const rows: [string, string][] = [
    ['RStudio', `\`${versions.rstudio}\`${versions.releaseName ? ` "${versions.releaseName}"` : ''}`],
    ['R', `\`${versions.r}\``],
    ['Edition', versions.edition],
    ['Operating system', versions.os],
  ];
  const block = [
    '### Run under test',
    '',
    '| | |',
    '| --- | --- |',
    ...rows.map(([label, value]) => `| **${label}** | ${value} |`),
    '',
    '',
  ].join('\n');
  try {
    fs.appendFileSync(summaryPath, block);
  } catch (err) {
    console.warn(`WARNING: could not write the run summary: ${err}`);
  }
}

/** Main-process counterpart of publishRunVersions; undefined if nothing yet. */
export function loadRunVersions(): RunVersions | undefined {
  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) return undefined;
  try {
    const raw = fs.readFileSync(path.join(sandbox, VERSIONS_FILE), 'utf-8');
    return JSON.parse(raw) as RunVersions;
  } catch {
    return undefined;
  }
}
