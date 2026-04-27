import { defineConfig } from '@playwright/test';

const allProjects = [
  // Desktop open-source
  {
    name: 'desktop-os-windows',
    grepInvert: /@server_only|@pro_only|@macos_only|@linux_only/,
  },
  {
    name: 'desktop-os-macos',
    grepInvert: /@server_only|@pro_only|@windows_only|@linux_only/,
  },
  {
    name: 'desktop-os-linux',
    grepInvert: /@server_only|@pro_only|@windows_only|@macos_only/,
  },
  // Desktop Pro
  {
    name: 'desktop-pro-windows',
    grepInvert: /@server_only|@os_only|@macos_only|@linux_only/,
  },
  {
    name: 'desktop-pro-macos',
    grepInvert: /@server_only|@os_only|@windows_only|@linux_only/,
  },
  {
    name: 'desktop-pro-linux',
    grepInvert: /@server_only|@os_only|@windows_only|@macos_only/,
  },
  // Server (Linux only)
  {
    name: 'server-os-linux',
    grepInvert: /@desktop_only|@pro_only|@windows_only|@macos_only/,
  },
  {
    name: 'server-pro-linux',
    grepInvert: /@desktop_only|@os_only|@windows_only|@macos_only/,
  },
];

// PW_PROJECT selects a single project for local runs (e.g. "desktop-pro-windows").
// When unset, all projects are available — CI runs all, or use --project to pick one.
// PW_PROJECT and --project conflict -- don't use both at the same time.
// PW_PROJECT pre-filters the project list, so --project can't select anything outside it.
const selectedProject = process.env.PW_PROJECT;
const projects = selectedProject
  ? allProjects.filter(p => p.name === selectedProject)
  : allProjects;

if (selectedProject) {
  if (projects.length === 0) {
    throw new Error(
      `PW_PROJECT="${selectedProject}" does not match any project. ` +
      `Available: ${allProjects.map(p => p.name).join(', ')}`
    );
  }
  console.log(`Project: ${selectedProject} (via PW_PROJECT)`);
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
