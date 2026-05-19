import { defineConfig } from '@playwright/test';
import dotenv from 'dotenv';
import os from 'os';
import path from 'path';

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
  use: { trace: 'off' as const, video: 'off' as const, screenshot: 'off' as const },
};

const allProjects = [
  {
    name: 'desktop',
    use: { mode: 'desktop' as const },
    grepInvert: new RegExp(['@server_only', ...desktopOsExclusions, ...editionExclusions].join('|')),
    dependencies: ['setup'],
  },
  {
    name: 'server',
    use: { mode: 'server' as const },
    grepInvert: new RegExp(['@desktop_only', ...serverOsExclusions, ...editionExclusions].join('|')),
    dependencies: ['setup'],
  },
];

if (process.env.PW_PROJECT) {
  console.warn('PW_PROJECT is no longer used; switch to --project=desktop|server or PW_RSTUDIO_MODE=desktop|server');
}

const projectFlagPresent = process.argv.some(a => a === '--project' || a.startsWith('--project='));
const modeEnv = process.env.PW_RSTUDIO_MODE?.toLowerCase();

let projects;
if (projectFlagPresent) {
  // Expose both projects; Playwright's CLI narrows down to the requested name post-load.
  projects = allProjects;
} else if (modeEnv === 'server') {
  projects = allProjects.filter(p => p.name === 'server');
} else if (modeEnv === 'desktop' || modeEnv === undefined) {
  projects = allProjects.filter(p => p.name === 'desktop');
} else {
  throw new Error(`PW_RSTUDIO_MODE="${modeEnv}" -- expected "desktop" or "server"`);
}

projects = [setupProject, ...projects];

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  timeout: 300000,
  retries: 0,
  reporter: [['html'], ['list'], ['./fixtures/sandbox-reporter.ts']],
  globalSetup: './fixtures/sandbox-setup.ts',
  globalTeardown: './fixtures/sandbox-teardown.ts',
  use: {
    trace: 'on-first-retry',
    actionTimeout: 10000,
  },
  projects,
});
