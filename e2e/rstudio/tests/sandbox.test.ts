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

    // workers: 1 in playwright.config.ts means there's exactly one config_*
    // dir per invocation. If parallel workers are ever enabled, expose
    // session.configRoot from DesktopSession and assert against that path
    // instead of scanning the sandbox directory.
    const configDirs = fs.readdirSync(SANDBOX!).filter(e => e.startsWith('config_'));
    expect(configDirs.length).toBe(1);

    const configRoot = path.join(SANDBOX!, configDirs[0]);
    expect(
      fs.existsSync(path.join(configRoot, 'config-home', 'rstudio-prefs.json')),
    ).toBe(true);

    const electronData = path.join(configRoot, 'electron-userdata');
    expect(fs.existsSync(electronData)).toBe(true);
    // Electron writes Local State asynchronously at startup. Poll for it
    // rather than asserting readdirSync().length > 0 on a single read.
    await expect.poll(() => fs.existsSync(path.join(electronData, 'Local State'))).toBe(true);
  });
});

test.describe('sandbox layout', { tag: ['@server_only'] }, () => {
  test.skip(!SANDBOX, 'PW_SANDBOX is not set; sandbox-setup did not run');

  const sandbox = useSuiteSandbox();

  test('Server R workdir uses workdir_ prefix; no config trees written locally', async () => {
    // sandbox.dir lives on the rsession host -- could be co-located with the
    // runner or on a remote machine. The fact that createSandbox() returned
    // a path means R successfully created it; we don't re-check existence
    // from the runner because that breaks against remote rsession.
    expect(sandbox.dir).toBeTruthy();
    expect(path.basename(sandbox.dir).startsWith('workdir_')).toBe(true);

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
