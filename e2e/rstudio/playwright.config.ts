import { defineConfig } from '@playwright/test';
import dotenv from 'dotenv';
import os from 'os';
import path from 'path';

type ProjectOptions = { mode: 'desktop' | 'server' };

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

const allProjects = [
  {
    name: 'desktop',
    use: { mode: 'desktop' as const },
    grepInvert: new RegExp(['@server_only', ...desktopOsExclusions, ...editionExclusions].join('|')),
  },
  {
    name: 'server',
    use: { mode: 'server' as const },
    grepInvert: new RegExp(['@desktop_only', ...serverOsExclusions, ...editionExclusions].join('|')),
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
const projects = allProjects.filter(p => p.name === modeEnv);

const testIgnore = (process.env.PW_TEST_IGNORE ?? '')
  .split(/\s+/)
  .filter(Boolean);

export default defineConfig<{}, ProjectOptions>({
  testDir: './tests',
  testIgnore: testIgnore.length > 0 ? testIgnore : undefined,
  fullyParallel: false,
  workers: 1,
  timeout: 300000,
  // On CI a single transient launch flake (e.g. GWT app slow to reach
  // ready under cold disk caches on a fresh runner) can otherwise turn
  // the whole suite red. One retry absorbs that without rerunning by hand.
  retries: process.env.CI ? 1 : 0,
  reporter: [['html'], ['list'], ['./fixtures/sandbox-reporter.ts']],
  globalSetup: './fixtures/sandbox-setup.ts',
  globalTeardown: './fixtures/sandbox-teardown.ts',
  use: {
    trace: 'on-first-retry',
    actionTimeout: 10000,
  },
  projects,
});
