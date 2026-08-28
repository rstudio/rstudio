import { test, expect } from '@fixtures/rstudio.fixture';
import { workerRLibsUser } from '@fixtures/r-libs-setup';
import { useSuiteSandbox } from '@utils/sandbox';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { rStringLiteral } from '@utils/r';
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

  test('Desktop launch populates electron-userdata and creates data-home/user-home', async ({ rstudioSession }) => {
    // Sandbox-level dirs: user-home is shared; data-home is only the
    // seeded-pai source (Desktop sessions get per-spec data homes below).
    expect(fs.existsSync(path.join(SANDBOX!, 'data-home'))).toBe(true);
    expect(fs.existsSync(path.join(SANDBOX!, 'user-home'))).toBe(true);

    // The worker-scoped fixture launches RStudio once per worker, but tests
    // that drive their own Desktop instances (e.g., the multi-Desktop tests)
    // or relaunch from scratch can add more config dirs over a full-suite
    // run. Validate the layout against *every* config dir so a partial-init
    // sibling (e.g., from a Desktop spawn that died mid-launch) can't pass
    // silently behind a healthy one chosen by readdir order. Only markers
    // written synchronously before/at launch are asserted here: Chromium
    // writes electron-userdata/Local State through a delayed committer
    // (~10s after launch), so a healthy sibling instance that exited young
    // may never have flushed it, and its stale dir would fail this test on
    // every retry for the rest of the run (#18475).
    const configDirs = fs.readdirSync(SANDBOX!).filter(e => e.startsWith('config_'));
    expect(configDirs.length).toBeGreaterThanOrEqual(1);

    for (const dir of configDirs) {
      const configRoot = path.join(SANDBOX!, dir);
      expect(
        fs.existsSync(path.join(configRoot, 'config-home', 'rstudio-prefs.json')),
        `${dir}: expected config-home/rstudio-prefs.json to exist`,
      ).toBe(true);

      expect(
        fs.existsSync(path.join(configRoot, 'electron-userdata')),
        `${dir}: expected electron-userdata to exist`,
      ).toBe(true);

      // Each config root carries its own isolated data home; a shared one
      // let leaked client state (e.g. a maximized pane) poison every later
      // launch in the run.
      expect(
        fs.existsSync(path.join(configRoot, 'data-home')),
        `${dir}: expected per-spec data-home to exist`,
      ).toBe(true);
    }

    // Check Local State -- proof that Electron actually adopted our
    // --user-data-dir -- only in this worker's own config root, whose
    // instance is still alive and therefore guaranteed to reach Chromium's
    // first commit. The poll budget must cover the committer's ~10s delay,
    // measured from process start; the default 5s expect timeout left too
    // little headroom when launch readiness came up fast.
    const ownConfigRoot = rstudioSession.configRoot;
    expect(ownConfigRoot, 'expected the desktop fixture to expose its config root').toBeTruthy();
    await expect.poll(
      () => fs.existsSync(path.join(ownConfigRoot!, 'electron-userdata', 'Local State')),
      {
        message: 'expected electron-userdata/Local State to exist in the current spec config root',
        timeout: 15000,
      },
    ).toBe(true);

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

  // A seeded assistant has to be laid out the way an install leaves it
  // (rstudio/rstudio#18658): a version slot carrying its own manifest, named
  // by the selector. Get this wrong and nothing fails loudly -- the IDE just
  // resolves nothing and downloads the official package, so the @ai suite
  // silently exercises a build nobody chose. See fixtures/pai-seed.ts.
  test('a seeded Posit Assistant is laid out as a selected version slot', async ({ rstudioSession }) => {
    test.skip(!process.env.PW_SEED_PAI, 'PW_SEED_PAI is not set; nothing was seeded');

    const storage = path.join(SANDBOX!, 'data-home', 'pai');
    const selected = JSON.parse(fs.readFileSync(path.join(storage, 'selected.json'), 'utf-8')).selected;
    const protocols = Object.keys(selected);
    expect(protocols, 'expected selected.json to name a slot for exactly one protocol').toHaveLength(1);

    const slot = path.join(storage, 'versions', selected[protocols[0]]);
    for (const file of ['package.json', 'protocol.json', '.slot-manifest.json']) {
      expect(fs.existsSync(path.join(slot, file)), `expected ${file} in the seeded slot`).toBe(true);
    }

    // The selector entry has to agree with what the slot declares, or
    // resolution rejects the slot and falls back.
    const { protocol } = JSON.parse(fs.readFileSync(path.join(slot, 'protocol.json'), 'utf-8'));
    expect(protocol).toBe(protocols[0]);

    // The legacy unversioned install is never read, so it is never seeded.
    expect(fs.existsSync(path.join(storage, 'bin'))).toBe(false);

    // And the session under test reaches all of it through its own data home,
    // which links pai to the sandbox seed (desktop.fixture's
    // seedPaiIntoDataHome).
    const ownDataHome = path.join(rstudioSession.configRoot!, 'data-home');
    expect(fs.existsSync(path.join(ownDataHome, 'pai', 'selected.json'))).toBe(true);
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

  test('spawned server rsession receives the wrapper environment', async ({ rstudioPage: page }) => {
    // Only the spawned in-tree server redirects the rsession HOME (via the
    // --rsession-path wrapper in fixtures/server.fixture.ts, #18348); an
    // external server's rsession uses the logged-in account's real home.
    test.skip(
      !!process.env.PW_RSTUDIO_SERVER_URL,
      'External server: the harness does not control the rsession HOME',
    );

    // Guards the whole delivery chain -- wrapper generation, rserver exec'ing
    // it, and the exports surviving into the session. If this fails, sandbox
    // AI credentials (and dotfile isolation) are silently broken in server
    // mode even though the credential gate reads them as present.
    const consoleActions = new ConsolePaneActions(page);
    const inSandbox = await consoleActions.evalRLogical(
      `startsWith(Sys.getenv("HOME"), ${rStringLiteral(SANDBOX!)})`,
    );
    expect(
      inSandbox,
      'expected the rsession HOME to be under PW_SANDBOX (rsession-wrapper delivery chain broken?)',
    ).toBe(true);

    // The wrapper unsets XDG_CONFIG_HOME so the copilot-language-server falls
    // back to $HOME/.config/github-copilot, inside the sandbox. It resolves
    // that directory from XDG_CONFIG_HOME before HOME, and rserver's xdg
    // filter forwards a developer-shell value straight through -- so a HOME
    // that arrives correctly is not on its own enough.
    const xdgUnset = await consoleActions.evalRLogical('Sys.getenv("XDG_CONFIG_HOME") == ""');
    expect(
      xdgUnset,
      'expected XDG_CONFIG_HOME to be empty in the rsession (Copilot would resolve its config dir outside the sandbox)',
    ).toBe(true);

    // Under the redirected HOME, R computes an empty default user library
    // unless the wrapper carries one, and the packages globalSetup installed
    // become invisible. Compare against the exact value this worker resolved
    // rather than merely checking non-empty, so a wrapper exporting the wrong
    // library is caught too. Note the path is a per-host cache outside the
    // sandbox by design, so "under PW_SANDBOX" is deliberately not asserted.
    //
    // workerRLibsUser() is only a lookup *here* because the launch fixture
    // already called it when it generated the wrapper -- depending on
    // rstudioPage above is what orders the two. On a parallel run a first call
    // clones the template library (see r-libs-setup.ts), so don't lift this
    // assertion into a test that doesn't take the page fixture.
    //
    // The expected value goes through .expand_R_libs_env_var because base R's
    // Rprofile rewrites R_LIBS_USER at startup, expanding the %p / %v tokens the
    // wrapper exports -- Sys.getenv returns a concrete path, never the template
    // it was handed. Expanding inside the same session is what makes the two
    // comparable on any platform and R version; comparing against the raw
    // template fails anywhere the template still carries tokens, which is the
    // default case.
    const rLibsMatches = await consoleActions.evalRLogical(
      'identical(Sys.getenv("R_LIBS_USER"), '
        + `base:::.expand_R_libs_env_var(${rStringLiteral(workerRLibsUser())}))`,
    );
    expect(
      rLibsMatches,
      'expected the rsession R_LIBS_USER to match workerRLibsUser() (pre-populated packages would be invisible)',
    ).toBe(true);
  });
});
