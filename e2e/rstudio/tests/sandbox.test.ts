import { test, expect } from '@fixtures/rstudio.fixture';
import { useSuiteSandbox } from '@utils/sandbox';
import * as fs from 'fs';
import * as path from 'path';

/**
 * Sandbox layout assertions.
 *
 * Catches silent regressions if Electron's --user-data-dir handling changes,
 * if RStudio relocates state writes off RSTUDIO_DATA_HOME, or if the Server
 * mode workdir relocation regresses.
 */

const SANDBOX = process.env.PW_SANDBOX;

test.describe('sandbox layout', { tag: ['@desktop_only'] }, () => {
  test.skip(!SANDBOX, 'PW_SANDBOX is not set; sandbox-setup did not run');

  test('Desktop launch populates electron-userdata and creates data-home/user-home', async ({ rstudioPage }) => {
    void rstudioPage; // Triggers the worker-scoped launch.

    expect(fs.existsSync(path.join(SANDBOX!, 'data-home'))).toBe(true);
    expect(fs.existsSync(path.join(SANDBOX!, 'user-home'))).toBe(true);

    const configDirs = fs.readdirSync(SANDBOX!).filter(e => e.startsWith('config_'));
    expect(configDirs.length).toBeGreaterThan(0);

    const electronData = path.join(SANDBOX!, configDirs[0], 'electron-userdata');
    expect(fs.existsSync(electronData)).toBe(true);
    // Electron writes Local State, Preferences, GPU caches, etc. on startup.
    expect(fs.readdirSync(electronData).length).toBeGreaterThan(0);
  });
});

test.describe('sandbox layout', { tag: ['@server_only'] }, () => {
  test.skip(!SANDBOX, 'PW_SANDBOX is not set; sandbox-setup did not run');

  const sandbox = useSuiteSandbox();

  test('Server R workdir lives under the sandbox; no config trees written locally', async () => {
    expect(sandbox.dir).toBeTruthy();
    expect(path.dirname(sandbox.dir)).toBe(SANDBOX!);
    expect(path.basename(sandbox.dir).startsWith('workdir_')).toBe(true);
    expect(fs.existsSync(sandbox.dir)).toBe(true);

    // Server doesn't call launchRStudio(), so no per-spec config tree exists
    // on the test runner's filesystem.
    const configDirs = fs.readdirSync(SANDBOX!).filter(e => e.startsWith('config_'));
    expect(configDirs.length).toBe(0);

    // Server uses its own data home; nothing should be written to ours by
    // the server itself. The directory exists (globalSetup created it) but
    // Server-mode tests shouldn't see writes here.
    const dataHome = path.join(SANDBOX!, 'data-home');
    expect(fs.existsSync(dataHome)).toBe(true);
    const dataHomeEntries = fs.readdirSync(dataHome);
    expect(dataHomeEntries.length).toBe(0);
  });
});
