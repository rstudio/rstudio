/*
 * updater.test.ts
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { describe } from 'mocha';
import { assert } from 'chai';
import { mkdtempSync, rmSync } from 'fs';
import os from 'os';
import path from 'path';
import { installationKey, takeRelaunchProject } from '../../../src/main/relaunch-project';
import { BuildArtifact } from '../../../src/main/update-feed';
import { InstallFailure, installerEnvironment, UpdateHook, Updater, UpdaterHost } from '../../../src/main/updater';
import { WindowsInstallMode } from '../../../src/main/windows-install-mode';
import { restore, saveAndClear } from '../unit-utils';

// The updater's state machine, driven through a host that stands in for the
// API, the process list, Squirrel, the NSIS installer and the GWT client.
// Downloads complete only when a test says so, so requests can overlap the
// way dialogs answered in any order make them overlap.

interface Deferred<T> {
  promise: Promise<T>;
  resolve: (value: T) => void;
}

function deferred<T>(): Deferred<T> {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((res) => {
    resolve = res;
  });
  return { promise, resolve };
}

/** let every settled promise run its continuations */
const flush = async () => new Promise<void>((resolve) => setImmediate(resolve));

const current = '2026.12.0-daily+10';
const A = '2026.12.0-daily+11';
const B = '2026.12.0-daily+12';
const project = '/home/me/proj/proj.Rproj';

function artifact(version: string, ext: string): Record<string, unknown> {
  const filename = `RStudio-${version.replace('+', '-')}.${ext}`;
  return {
    version,
    link: `https://dl.test/${filename}`,
    filename,
    sha256: 'ab'.repeat(32),
    size: 100,
    date: '2026-11-02T00:00:00Z',
  };
}

/** a branch index whose newest build is `version` */
function indexFor(version: string): string {
  const platforms = {
    macos: artifact(version, 'dmg'),
    'macos-zip': artifact(version, 'zip'),
    windows: artifact(version, 'exe'),
  };
  return JSON.stringify({ products: { electron: { platforms } } });
}

class FakeHost implements UpdaterHost {
  readonly arch = 'x64';
  readonly execPath: string;
  index = indexFor(A);
  /** answers for particular urls; anything else gets `index` */
  responses = new Map<string, string | Error>();
  requests: string[] = [];
  others: number | null = 0;
  /** when set, otherInstances() waits for it, so a test can act mid-preflight */
  othersGate: Deferred<boolean> | null = null;
  installMode: WindowsInstallMode | null = 'allusers';
  verifies = true;
  downloads: { entry: BuildArtifact; done: Deferred<boolean> }[] = [];
  installs = 0;
  onInstall: () => void = () => undefined;
  launched: { file: string; mode: WindowsInstallMode }[] = [];
  /** what launchInstaller() answers, once `launchGate` (if set) resolves */
  launches = true;
  launchGate: Deferred<boolean> | null = null;
  notifications: [UpdateHook, string][] = [];
  warnings: [string, InstallFailure][] = [];

  constructor(
    readonly platform: NodeJS.Platform,
    readonly dir: string,
  ) {
    this.execPath =
      platform === 'win32' ? 'C:\\RStudio\\rstudio.exe' : '/Applications/RStudio.app/Contents/MacOS/RStudio';
  }

  async fetchText(url: string): Promise<string> {
    this.requests.push(url);
    const response = this.responses.get(url);
    if (response instanceof Error) {
      throw response;
    }
    return response ?? this.index;
  }

  async otherInstances(): Promise<number | null> {
    if (this.othersGate) {
      await this.othersGate.promise;
    }
    return this.others;
  }

  async detectWindowsInstallMode(): Promise<WindowsInstallMode | null> {
    return this.installMode;
  }

  stagingDir(): string {
    return path.join(this.dir, 'staging');
  }

  markerDir(): string {
    return path.join(this.dir, 'markers');
  }

  async squirrelDownload(entry: BuildArtifact): Promise<void> {
    await this.download(entry).promise;
  }

  squirrelInstall(): void {
    this.installs++;
    this.onInstall();
  }

  async stageInstaller(entry: BuildArtifact): Promise<string> {
    await this.download(entry).promise;
    return path.join(this.stagingDir(), entry.filename);
  }

  async verifyInstaller(): Promise<boolean> {
    return this.verifies;
  }

  async launchInstaller(file: string, mode: WindowsInstallMode): Promise<boolean> {
    this.launched.push({ file, mode });
    if (this.launchGate) {
      await this.launchGate.promise;
    }
    return this.launches;
  }

  notify(hook: UpdateHook, arg: string): void {
    this.notifications.push([hook, arg]);
  }

  setProgress(): void {
    // the fake has no window
  }

  warnNotInstalled(version: string, reason: InstallFailure): void {
    this.warnings.push([version, reason]);
  }

  /** complete a download (the latest by default) and let the updater react */
  async finishDownload(index = this.downloads.length - 1): Promise<void> {
    this.downloads[index].done.resolve(true);
    await flush();
  }

  lastNotification(): [UpdateHook, string] | undefined {
    return this.notifications[this.notifications.length - 1];
  }

  private download(entry: BuildArtifact): Deferred<boolean> {
    const done = deferred<boolean>();
    this.downloads.push({ entry, done });
    return done;
  }
}

describe('updater', () => {
  const env = {
    RSTUDIO_UPDATE_VERSION: '',
    RSTUDIO_UPDATE_CHANNEL: '',
    RSTUDIO_UPDATE_BRANCH: '',
    RSTUDIO_UPDATE_API_URL: '',
    RSTUDIO_DISABLE_IN_APP_UPDATES: '',
  };
  let dir: string;

  before(() => {
    saveAndClear(env);
    process.env.RSTUDIO_UPDATE_VERSION = current;
    process.env.RSTUDIO_UPDATE_CHANNEL = 'daily';
    process.env.RSTUDIO_UPDATE_BRANCH = 'parrot-balsam';
    process.env.RSTUDIO_UPDATE_API_URL = 'https://api.test';
  });

  after(() => {
    restore(env);
  });

  beforeEach(() => {
    dir = mkdtempSync(path.join(os.tmpdir(), 'updater-test-'));
  });

  afterEach(() => {
    rmSync(dir, { recursive: true, force: true });
  });

  function setup(platform: NodeJS.Platform = 'darwin') {
    const host = new FakeHost(platform, dir);
    return { host, updater: new Updater(host) };
  }

  /** the next check finds `version`, and the user is shown its dialog */
  async function offer(updater: Updater, host: FakeHost, version: string): Promise<void> {
    host.index = indexFor(version);
    const result = await updater.checkForUpdates(true);
    assert.equal(result.status, 'available');
    assert.equal(result['update-version'], version);
    assert.isTrue(result['can-install']);
  }

  /** download `version` to completion, as the user accepting its dialog does */
  async function download(updater: Updater, host: FakeHost, version: string): Promise<void> {
    const downloading = updater.downloadUpdate(version);
    await host.finishDownload();
    await downloading;
    assert.deepEqual(host.lastNotification(), ['onUpdateDownloaded', version]);
  }

  function rememberedProject(host: FakeHost, version: string): string | null {
    const identity = { version, installKey: installationKey(host.execPath, host.platform) };
    return takeRelaunchProject(identity, Date.now(), host.markerDir());
  }

  it('downloads the accepted update and has Squirrel install it on quit', async () => {
    const { host, updater } = setup('darwin');
    await offer(updater, host, A);
    await download(updater, host, A);
    assert.equal(host.downloads[0].entry.version, A);

    assert.equal(await updater.canInstallUpdate(A), '');
    assert.equal(await updater.installUpdate(A, project), '');
    assert.isTrue(updater.hasPendingInstall());

    assert.isTrue(await updater.applyPendingUpdate());
    assert.equal(host.installs, 1);
    assert.isFalse(updater.hasPendingInstall());
    assert.equal(rememberedProject(host, A), project);
  });

  it('runs the staged Windows installer on quit', async () => {
    const { host, updater } = setup('win32');
    await offer(updater, host, A);
    await download(updater, host, A);
    assert.equal(await updater.installUpdate(A, project), '');

    assert.isFalse(await updater.applyPendingUpdate()); // the window still closes itself
    assert.equal(host.launched.length, 1);
    assert.equal(host.launched[0].mode, 'allusers');
    assert.match(host.launched[0].file, /RStudio-2026\.12\.0-daily-11\.exe$/);
    assert.equal(rememberedProject(host, A), project);
  });

  it('warns, and drops the relaunch marker, when the Windows installer will not start', async () => {
    const { host, updater } = setup('win32');
    await offer(updater, host, A);
    await download(updater, host, A);
    assert.equal(await updater.installUpdate(A, project), '');

    host.launches = false;
    host.launchGate = deferred();
    const applying = updater.applyPendingUpdate();
    await flush();
    assert.equal(host.launched.length, 1);
    assert.isTrue(updater.isHandingOff()); // the window waits for the launch too

    host.launchGate.resolve(true);
    assert.isFalse(await applying);
    assert.isFalse(updater.isHandingOff());
    assert.deepEqual(host.warnings, [[A, 'launch-failed']]);
    assert.isNull(rememberedProject(host, A));
  });

  it('offers a downloaded update again without downloading it twice', async () => {
    const { host, updater } = setup('darwin');
    await offer(updater, host, A);
    await download(updater, host, A);

    await updater.downloadUpdate(A);
    assert.equal(host.downloads.length, 1);
    assert.deepEqual(host.lastNotification(), ['onUpdateDownloaded', A]);
  });

  it('reports a download asked for another build while one runs, and ignores a repeat', async () => {
    const { host, updater } = setup('darwin');
    await offer(updater, host, A);
    await offer(updater, host, B);

    const downloadingA = updater.downloadUpdate(A);
    await updater.downloadUpdate(B);
    const [hook, message] = host.lastNotification() ?? [];
    assert.equal(hook, 'onUpdateDownloadFailed');
    assert.include(message, `${A} is still being downloaded`);

    const before = host.notifications.length;
    await updater.downloadUpdate(A);
    assert.equal(host.notifications.length, before);

    await host.finishDownload();
    await downloadingA;
    assert.equal(host.downloads.length, 1);
    assert.deepEqual(host.lastNotification(), ['onUpdateDownloaded', A]);
  });

  it('treats a ready dialog for a build that is no longer the downloaded one as missing', async () => {
    const { host, updater } = setup('darwin');
    await offer(updater, host, A);
    await offer(updater, host, B);
    await download(updater, host, A);
    await download(updater, host, B);

    assert.equal(await updater.canInstallUpdate(A), 'missing');
    assert.equal(await updater.canInstallUpdate(B), '');

    // GWT answers 'missing' by downloading the version the dialog named
    await download(updater, host, A);
    assert.equal(host.downloads.length, 3);
    assert.equal(await updater.canInstallUpdate(A), '');
    assert.equal(await updater.canInstallUpdate(B), 'missing');
  });

  it('refuses the install when a download started during its checks', async () => {
    const { host, updater } = setup('darwin');
    await offer(updater, host, A);
    await offer(updater, host, B);
    await download(updater, host, A);

    host.othersGate = deferred();
    const installing = updater.installUpdate(A, project);
    await flush();
    void updater.downloadUpdate(B); // accepted from a dialog that stayed open
    host.othersGate.resolve(true);

    assert.equal(await installing, 'missing');
    assert.isFalse(updater.hasPendingInstall());
    assert.isNull(rememberedProject(host, A));
  });

  it('refuses downloads while an install is pending, until the quit is abandoned', async () => {
    const { host, updater } = setup('darwin');
    await offer(updater, host, A);
    await offer(updater, host, B);
    await download(updater, host, A);
    assert.equal(await updater.installUpdate(A, project), '');

    await updater.downloadUpdate(B);
    const [hook, message] = host.lastNotification() ?? [];
    assert.equal(hook, 'onUpdateDownloadFailed');
    assert.include(message, `${A} is about to be installed`);
    assert.equal(host.downloads.length, 1);

    updater.cancelPendingInstall();
    assert.isFalse(updater.hasPendingInstall());
    assert.isNull(rememberedProject(host, A));
    void updater.downloadUpdate(B);
    assert.equal(host.downloads.length, 2);
  });

  it('keeps the window from closing while the handoff decides, not once it installs', async () => {
    const { host, updater } = setup('darwin');
    await offer(updater, host, A);
    await download(updater, host, A);
    assert.equal(await updater.installUpdate(A, project), '');
    assert.isFalse(updater.isHandingOff());

    // Squirrel closes the windows itself from inside squirrelInstall()
    let handingOffAtInstall: boolean | null = null;
    host.onInstall = () => (handingOffAtInstall = updater.isHandingOff());
    host.othersGate = deferred();
    const applying = updater.applyPendingUpdate();
    await flush();
    assert.isTrue(updater.isHandingOff());

    host.othersGate.resolve(true);
    assert.isTrue(await applying);
    assert.isFalse(updater.isHandingOff());
    assert.isFalse(handingOffAtInstall);
  });

  it('installs nothing when the install is cancelled while the handoff decides', async () => {
    const { host, updater } = setup('darwin');
    await offer(updater, host, A);
    await download(updater, host, A);
    assert.equal(await updater.installUpdate(A, project), '');

    host.othersGate = deferred();
    const applying = updater.applyPendingUpdate();
    await flush();
    updater.cancelPendingInstall(A);
    host.othersGate.resolve(true);

    assert.isFalse(await applying);
    assert.equal(host.installs, 0);
    assert.deepEqual(host.warnings, []); // the quit was given up, not blocked
    assert.isNull(rememberedProject(host, A));
  });

  it('drops only the install record it was asked about', async () => {
    const { host, updater } = setup('darwin');
    await offer(updater, host, A);
    await download(updater, host, A);
    assert.equal(await updater.installUpdate(A, project), '');

    updater.cancelPendingInstall(B); // a request for another build, not this one
    assert.isTrue(updater.hasPendingInstall());
    assert.isTrue(await updater.applyPendingUpdate());
    assert.equal(host.installs, 1);
  });

  it('gives up at the quit handoff when another window opened meanwhile, and says so', async () => {
    const { host, updater } = setup('darwin');
    await offer(updater, host, A);
    await download(updater, host, A);
    assert.equal(await updater.installUpdate(A, project), '');

    host.others = 1;
    assert.isFalse(await updater.applyPendingUpdate());
    assert.equal(host.installs, 0);
    assert.deepEqual(host.warnings, [[A, 'other-instances']]);
    assert.isFalse(updater.hasPendingInstall());
    assert.isNull(rememberedProject(host, A));

    // the download is still there for the next check
    host.others = 0;
    assert.equal(await updater.canInstallUpdate(A), '');
  });

  it('blocks on other windows, or on not knowing, before the save prompts', async () => {
    const { host, updater } = setup('darwin');
    await offer(updater, host, A);
    await download(updater, host, A);

    host.others = 2;
    assert.equal(await updater.canInstallUpdate(A), 'other-instances');
    host.others = null;
    assert.equal(await updater.canInstallUpdate(A), 'unknown');
    assert.equal(await updater.installUpdate(A, project), 'unknown');
    assert.isFalse(updater.hasPendingInstall());
  });

  it('asks for the Windows installer again when the staged one no longer verifies', async () => {
    const { host, updater } = setup('win32');
    await offer(updater, host, A);
    await download(updater, host, A);

    host.verifies = false;
    assert.equal(await updater.canInstallUpdate(A), 'missing');

    // the repeat request downloads again rather than reporting the stale file
    const downloading = updater.downloadUpdate(A);
    await flush();
    assert.equal(host.downloads.length, 2);
    host.verifies = true;
    await host.finishDownload();
    await downloading;
    assert.equal(await updater.canInstallUpdate(A), '');
  });

  describe('on the release channel', () => {
    const releaseIndexUrl = 'https://api.test/api/v1/release/index.json';
    const buildsUrl = `https://api.test/api/v1/build/${A}/index.json`;
    const releaseIndex = JSON.stringify({
      released: [{ published: [{ build: A, builds_url: buildsUrl, notes_url: 'https://notes.test/' }] }],
    });

    function releaseSetup(builds: string | Error) {
      process.env.RSTUDIO_UPDATE_CHANNEL = 'release';
      try {
        const { host, updater } = setup('darwin');
        host.responses.set(releaseIndexUrl, releaseIndex);
        host.responses.set(buildsUrl, builds);
        host.index = 'not to be read';
        return { host, updater };
      } finally {
        process.env.RSTUDIO_UPDATE_CHANNEL = 'daily';
      }
    }

    it('reads the release index, then the build it names', async () => {
      const { host, updater } = releaseSetup(indexFor(A));
      const result = await updater.checkForUpdates(false);
      assert.deepEqual(host.requests, [releaseIndexUrl, buildsUrl]);
      assert.equal(result.status, 'available');
      assert.equal(result['update-version'], A);
      assert.equal(result['update-url'], 'https://dl.test/RStudio-2026.12.0-daily-11.dmg');
      assert.isTrue(result['can-install']);
    });

    it('answers error, and so the session check, when the build index cannot be read', async () => {
      const { updater } = releaseSetup(new Error('HTTP 503'));
      const result = await updater.checkForUpdates(false);
      assert.equal(result.status, 'error');
      assert.include(result.error, 'HTTP 503');
    });

    it('offers only a manual download when the macOS zip is missing or from another build', async () => {
      const dmgOnly = JSON.parse(indexFor(A)) as { products: { electron: { platforms: Record<string, unknown> } } };
      delete dmgOnly.products.electron.platforms['macos-zip'];
      const dmgOnlyResult = await releaseSetup(JSON.stringify(dmgOnly)).updater.checkForUpdates(false);
      assert.equal(dmgOnlyResult.status, 'available');
      assert.isFalse(dmgOnlyResult['can-install']);

      const mixed = JSON.parse(indexFor(A)) as { products: { electron: { platforms: Record<string, unknown> } } };
      mixed.products.electron.platforms['macos-zip'] = artifact(B, 'zip');
      const { host, updater } = releaseSetup(JSON.stringify(mixed));
      const result = await updater.checkForUpdates(false);
      assert.equal(result.status, 'available');
      assert.equal(result['update-version'], A);
      assert.isFalse(result['can-install']);

      // and the dialog's fallback is all there is: nothing to download in-app
      await updater.downloadUpdate(A);
      assert.deepEqual(host.lastNotification(), [
        'onUpdateDownloadFailed',
        `RStudio ${A} is not available to download.`,
      ]);
    });
  });

  it('keeps the project this instance was opened with out of the installer', () => {
    const env = installerEnvironment({
      PATH: '/usr/bin',
      RS_INITIAL_PROJECT: '/home/me/proj/proj.Rproj',
      RS_INITIAL_WD: '/home/me/proj',
    });
    assert.deepEqual(env, { PATH: '/usr/bin' });
  });

  it('is unsupported when disabled, on Linux, and for a version never offered', async () => {
    process.env.RSTUDIO_DISABLE_IN_APP_UPDATES = '1';
    try {
      assert.equal((await setup('darwin').updater.checkForUpdates(true)).status, 'unsupported');
    } finally {
      delete process.env.RSTUDIO_DISABLE_IN_APP_UPDATES;
    }
    assert.equal((await setup('linux').updater.checkForUpdates(true)).status, 'unsupported');

    const { host, updater } = setup('darwin');
    await updater.downloadUpdate(B);
    assert.deepEqual(host.lastNotification(), ['onUpdateDownloadFailed', `RStudio ${B} is not available to download.`]);
  });
});
