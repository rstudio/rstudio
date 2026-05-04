import { defineConfig } from '@playwright/test';
import os from 'os';

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

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  timeout: 300000,
  retries: 0,
  reporter: [['html'], ['list']],
  use: {
    trace: 'on-first-retry',
    actionTimeout: 10000,
  },
  projects,
});
