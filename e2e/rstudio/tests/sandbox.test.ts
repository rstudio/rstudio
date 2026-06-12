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

    // Sandbox-level dirs: user-home is shared; data-home is only the
    // seeded-pai source (Desktop sessions get per-spec data homes below).
    expect(fs.existsSync(path.join(SANDBOX!, 'data-home'))).toBe(true);
    expect(fs.existsSync(path.join(SANDBOX!, 'user-home'))).toBe(true);

    // The worker-scoped fixture launches RStudio once per worker, but tests
    // that drive their own Desktop instances (e.g., the multi-Desktop tests)
    // or relaunch from scratch can add more config dirs over a full-suite
    // run. Validate the layout against *every* config dir so a partial-init
    // sibling (e.g., from a Desktop spawn that died mid-launch) can't pass
    // silently behind a healthy one chosen by readdir order.
    const configDirs = fs.readdirSync(SANDBOX!).filter(e => e.startsWith('config_'));
    expect(configDirs.length).toBeGreaterThanOrEqual(1);

    for (const dir of configDirs) {
      const configRoot = path.join(SANDBOX!, dir);
      expect(
        fs.existsSync(path.join(configRoot, 'config-home', 'rstudio-prefs.json')),
        `${dir}: expected config-home/rstudio-prefs.json to exist`,
      ).toBe(true);

      const electronData = path.join(configRoot, 'electron-userdata');
      expect(
        fs.existsSync(electronData),
        `${dir}: expected electron-userdata to exist`,
      ).toBe(true);
      // Electron writes Local State asynchronously at startup. Poll for it
      // rather than asserting readdirSync().length > 0 on a single read.
      await expect.poll(
        () => fs.existsSync(path.join(electronData, 'Local State')),
        { message: `${dir}: expected electron-userdata/Local State to exist` },
      ).toBe(true);

      // Each config root carries its own isolated data home; a shared one
      // let leaked client state (e.g. a maximized pane) poison every later
      // launch in the run.
      expect(
        fs.existsSync(path.join(configRoot, 'data-home')),
        `${dir}: expected per-spec data-home to exist`,
      ).toBe(true);
    }

    // The session persists its state under RSTUDIO_DATA_HOME; at least the
    // current worker's launch must have written there. Catches the session
    // relocating state writes off RSTUDIO_DATA_HOME (the rest of this suite
    // would then silently lose its cross-restart persistence coverage).
    await expect.poll(
      () => configDirs.some(dir =>
        fs.existsSync(path.join(SANDBOX!, dir, 'data-home', 'rstudio-desktop.json'))),
      { message: 'expected at least one per-spec data-home to contain rstudio-desktop.json' },
    ).toBe(true);
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
