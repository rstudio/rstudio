/*
 * updater.ts
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

import { spawn } from 'child_process';
import { createHash } from 'crypto';
import { app, autoUpdater, net } from 'electron';
import { createReadStream, createWriteStream, existsSync } from 'fs';
import { mkdir, readdir, rename, rm, stat } from 'fs/promises';
import http from 'http';
import { AddressInfo } from 'net';
import path from 'path';
import { Readable } from 'stream';
import { pipeline } from 'stream/promises';
import { getenv } from '../core/environment';
import { safeError } from '../core/err';
import { logger } from '../core/logger';
import { buildInfo } from './build-info';
import { MainWindow } from './main-window';
import {
  branchSlug,
  BuildArtifact,
  channelForVersion,
  installerVersion,
  isProVersion,
  isUpdateAvailable,
  kDefaultUpdateApiBaseUrl,
  parseBuildIndex,
  parseDevelopmentBranch,
  parseReleaseIndex,
  PlatformKeys,
  platformKeys,
  releaseIndexUrl,
  squirrelFeedDocument,
  UpdateChannel,
  updateIndexUrl,
} from './update-feed';
import { detectWindowsInstallMode, otherRStudioProcessCount, WindowsInstallMode } from './windows-install-mode';

/**
 * In-app updates for RStudio Desktop, modelled on Positron's updater.
 *
 * The flow is split between this class and the GWT client, which owns every
 * dialog so the experience matches the rest of the IDE:
 *
 *   1. GWT asks `checkForUpdates()`; we read the dailies.rstudio.com JSON API
 *      for this build's channel and compare versions. The answer uses the
 *      same keys as the session's check_for_updates RPC so GWT can show its
 *      existing dialog, plus `can-install` when this platform can apply the
 *      update itself.
 *   2. The user accepts; GWT calls `downloadUpdate()`. macOS hands Electron's
 *      autoUpdater (Squirrel.Mac) a feed served from a loopback listener,
 *      and Squirrel verifies the download and the new bundle's code
 *      signature. Windows downloads the NSIS installer and checks its SHA-256
 *      against the API.
 *   3. We call back into GWT (`desktopHooks.onUpdateDownloaded`), which offers
 *      a restart. If accepted, GWT asks `canInstallUpdate()` (on Windows, no
 *      other RStudio process may be running), calls `installUpdate()` and
 *      runs its normal quit sequence; `applyPendingUpdate()` then runs as
 *      the main window quits: Squirrel swaps the bundle and relaunches, or
 *      the installer runs silently (`/S /update`) and relaunches RStudio
 *      when it is done.
 *
 * Any failure to read the API answers `unsupported` or `error`, and GWT
 * falls back to the session's check and today's "Quit and Download" dialog.
 * Linux always answers `unsupported` (#4186 tracks package repos), as does
 * any build with RSTUDIO_DISABLE_IN_APP_UPDATES set.
 */

export type UpdateCheckStatus = 'available' | 'up-to-date' | 'unsupported' | 'error';

/** Wire format returned to GWT; keys mirror the check_for_updates RPC. */
export interface UpdateCheckResult {
  status: UpdateCheckStatus;
  'update-version'?: string;
  'update-url'?: string;
  'update-message'?: string;
  'update-urgent'?: string;
  'can-install'?: boolean;
  error?: string;
}

interface AvailableUpdate {
  /** the user-facing installer, linked from the dialog's fallback path */
  download: BuildArtifact;
  /** what the updater installs from; null when it is not published for this build */
  install: BuildArtifact | null;
  notesUrl: string;
}

/** How long an API request may take before it counts as unreachable. */
const kRequestTimeoutMs = 15000;

/** A partial download older than this was abandoned by a crashed instance. */
const kPartialDownloadTtlMs = 24 * 60 * 60 * 1000;

export class Updater {
  private keys: PlatformKeys | null;
  private channel: UpdateChannel;
  private currentVersion: string;
  private baseUrl: string;
  private branch: string;
  private branchPinned: boolean;
  private available: AvailableUpdate | null = null;
  private activeDownload: BuildArtifact | null = null;
  private downloadedVersion: string | null = null;
  private windowsInstaller: string | null = null;
  private windowsInstallMode: WindowsInstallMode | null = null;
  private pendingInstall = false;
  private macListenersInstalled = false;
  private squirrelServer: http.Server | null = null;

  constructor(private mainWindow: MainWindow) {
    // RSTUDIO_UPDATE_VERSION impersonates another version, as it does for the
    // session's check; RSTUDIO_UPDATE_CHANNEL, RSTUDIO_UPDATE_BRANCH and
    // RSTUDIO_UPDATE_API_URL point a build (a developer build included) at
    // another channel, branch or copy of the API
    this.currentVersion = getenv('RSTUDIO_UPDATE_VERSION') || buildInfo().RSTUDIO_VERSION;
    const channelOverride = getenv('RSTUDIO_UPDATE_CHANNEL');
    this.channel = channelOverride ? (channelOverride as UpdateChannel) : channelForVersion(this.currentVersion);
    const branchOverride = getenv('RSTUDIO_UPDATE_BRANCH');
    this.branchPinned = branchOverride.length > 0;
    this.branch = branchOverride || branchSlug(buildInfo().RSTUDIO_RELEASE_NAME);
    this.baseUrl = getenv('RSTUDIO_UPDATE_API_URL') || kDefaultUpdateApiBaseUrl;

    // open-source RStudio Desktop only: Workbench has its own distribution
    // and will need its own update infrastructure. Administrators turn the
    // whole feature off with RSTUDIO_DISABLE_IN_APP_UPDATES; the session's
    // own check then runs as before.
    const disabled = getenv('RSTUDIO_DISABLE_IN_APP_UPDATES').length > 0;
    const indexed = updateIndexUrl(this.baseUrl, this.channel, this.branch) !== null;
    this.keys =
      disabled || !indexed || isProVersion(this.currentVersion) ? null : platformKeys(process.platform, process.arch);

    if (this.keys && process.platform === 'win32') {
      void this.pruneStagedInstallers();
    }
  }

  async checkForUpdates(manual: boolean): Promise<UpdateCheckResult> {
    if (!this.keys) {
      return { status: 'unsupported' };
    }

    let update: AvailableUpdate;
    try {
      const indexUrl = await this.resolveIndexUrl();
      const found = await this.readIndex(indexUrl, this.keys);
      if (!found) {
        // the branch is not on the site (yet), so there is nothing to compare
        // with; the session's own check still runs
        logger().logDebug(`${indexUrl} lists no ${this.keys.download} build; deferring to the session check`);
        return { status: 'unsupported' };
      }
      update = found;
    } catch (error: unknown) {
      const message = safeError(error).message;
      logger().logDebug(`update check (${manual ? 'manual' : 'automatic'}) failed: ${message}`);
      return { status: 'error', error: message };
    }

    if (!isUpdateAvailable(this.currentVersion, update.download.version)) {
      return { status: 'up-to-date' };
    }

    this.available = update;
    const canInstall = update.install !== null && (await this.canInstall());
    return {
      status: 'available',
      'update-version': update.download.version,
      'update-url': update.download.url,
      'update-message': `RStudio ${update.download.version} is now available (you're using ${this.currentVersion})`,
      'update-urgent': '0',
      'can-install': canInstall,
    };
  }

  /**
   * Start downloading the update found by the last check. Completion is
   * reported to GWT through desktopHooks rather than a return value, since
   * the Squirrel download runs outside our control.
   */
  async downloadUpdate(): Promise<void> {
    const entry = this.available?.install;
    if (!this.available || !entry) {
      this.notifyFailed('No update is available to download.');
      return;
    }
    if (this.activeDownload) {
      return;
    }
    if (this.downloadedVersion === entry.version) {
      this.notifyDownloaded(entry.version);
      return;
    }

    // later checks may replace `available` while this download runs, so
    // completion reports the artifact that was actually fetched
    this.activeDownload = entry;
    this.setProgress(2); // > 1 shows an indeterminate dock / taskbar bar
    try {
      if (process.platform === 'darwin') {
        await this.downloadWithSquirrel(entry, this.available.notesUrl);
      } else if (process.platform === 'win32') {
        await this.downloadWindowsInstaller(entry);
        this.onDownloaded(entry.version);
      } else {
        throw new Error('in-app updates are not supported on this platform');
      }
    } catch (error: unknown) {
      this.onDownloadFailed(safeError(error).message);
    }
  }

  /**
   * Whether the downloaded update can be applied right now. "Restart Now"
   * quits only this window, and on Windows each window is a separate
   * rstudio.exe that the installer refuses to run beside; GWT asks the user
   * to close the others first.
   */
  async canInstallUpdate(): Promise<boolean> {
    if (process.platform !== 'win32') {
      return true;
    }

    const others = await otherRStudioProcessCount();
    if (others > 0) {
      logger().logInfo(`not installing the update yet: ${others} other RStudio process(es) running`);
    }
    return others === 0;
  }

  /** Record that the user accepted the restart; the quit sequence applies it. */
  installUpdate(): void {
    if (this.downloadedVersion === null) {
      logger().logErrorMessage('installUpdate() called before an update was downloaded');
      return;
    }
    this.pendingInstall = true;
  }

  /**
   * Apply a downloaded update now that the session has shut down and the
   * main window is quitting. Returns true when the updater took over the
   * quit (Squirrel relaunches the app itself), false when the caller should
   * continue quitting as usual.
   */
  applyPendingUpdate(): boolean {
    if (!this.pendingInstall) {
      return false;
    }
    this.pendingInstall = false;

    if (process.platform === 'darwin') {
      logger().logInfo(`installing RStudio ${this.downloadedVersion} and relaunching`);
      autoUpdater.quitAndInstall();
      return true;
    }

    if (process.platform === 'win32' && this.windowsInstaller && this.windowsInstallMode) {
      // the staging directory is shared with other instances, so make sure
      // the verified installer is still there before handing it the quit
      if (!existsSync(this.windowsInstaller)) {
        logger().logErrorMessage(`${this.windowsInstaller} is gone; not installing RStudio ${this.downloadedVersion}`);
        return false;
      }

      // the installer waits (via /update) for this process to exit before it
      // touches the install directory, then relaunches RStudio
      logger().logInfo(`launching ${this.windowsInstaller} to install RStudio ${this.downloadedVersion}`);
      const child = spawn(this.windowsInstaller, ['/S', `/${this.windowsInstallMode}`, '/update'], {
        detached: true,
        stdio: 'ignore',
        windowsHide: true,
      });
      child.on('error', (error) => logger().logError(error));
      child.unref();
    }
    return false;
  }

  /**
   * The index to poll on this check. Daily and hourly builds move to a new
   * branch at every release cut (version/new-version.sh), so unless a branch
   * was pinned, follow whichever branch the site says is in development
   * rather than the one this build came from.
   */
  private async resolveIndexUrl(): Promise<string> {
    let branch = this.branch;
    if (!this.branchPinned && (this.channel === 'daily' || this.channel === 'hourly')) {
      const current = parseDevelopmentBranch(await this.fetchText(releaseIndexUrl(this.baseUrl)));
      if (current) {
        branch = current;
      } else {
        logger().logDebug(`the release index names no branch in development; staying on ${branch}`);
      }
    }

    // the constructor checked that this channel has an index
    return updateIndexUrl(this.baseUrl, this.channel, branch) as string;
  }

  /**
   * Resolve the newest build on this channel. The release API needs a second
   * request for the build's artifacts; branch indexes carry them inline.
   */
  private async readIndex(indexUrl: string, keys: PlatformKeys): Promise<AvailableUpdate | null> {
    let buildIndex: string;
    let notesUrl = '';
    if (this.channel === 'release') {
      const release = parseReleaseIndex(await this.fetchText(indexUrl));
      notesUrl = release.notesUrl;
      buildIndex = await this.fetchText(release.buildsUrl);
    } else {
      buildIndex = await this.fetchText(indexUrl);
    }

    const download = parseBuildIndex(buildIndex, keys.download);
    if (!download) {
      return null;
    }

    // the site is rebuilt a few minutes after each artifact lands, so a
    // branch index can briefly mix two builds; only install from an artifact
    // of the same build as the one being offered
    let install = keys.install === keys.download ? download : parseBuildIndex(buildIndex, keys.install);
    if (install && install.version !== download.version) {
      logger().logDebug(
        `${keys.install} is at ${install.version} while ${keys.download} is at ${download.version}; not installable yet`,
      );
      install = null;
    }

    return { download, install, notesUrl };
  }

  private async fetchText(url: string): Promise<string> {
    const response = await net.fetch(url, {
      cache: 'no-store',
      signal: AbortSignal.timeout(kRequestTimeoutMs),
    });
    if (!response.ok) {
      throw new Error(`${url} answered HTTP ${response.status}`);
    }
    return response.text();
  }

  private async canInstall(): Promise<boolean> {
    if (process.platform === 'darwin') {
      return true;
    }
    if (process.platform === 'win32') {
      this.windowsInstallMode = await detectWindowsInstallMode(path.dirname(process.execPath));
      if (!this.windowsInstallMode) {
        logger().logDebug('RStudio is not registered as an NSIS install; offering a manual download instead');
      }
      return this.windowsInstallMode !== null;
    }
    return false;
  }

  private async downloadWithSquirrel(entry: BuildArtifact, notesUrl: string): Promise<void> {
    if (!this.macListenersInstalled) {
      this.macListenersInstalled = true;
      autoUpdater.on('update-downloaded', (_event, _notes, releaseName) => {
        this.closeSquirrelServer();
        this.onDownloaded(this.activeDownload?.version ?? releaseName);
      });
      autoUpdater.on('update-not-available', () => {
        this.closeSquirrelServer();
        this.onDownloadFailed('The update server did not offer an update.');
      });
      autoUpdater.on('error', (error) => {
        this.closeSquirrelServer();
        this.onDownloadFailed(error.message);
      });
    }

    // Squirrel.Mac wants its own feed format from an update server, and
    // downloads whatever that feed names, so serve it a one-entry document
    // for the build already chosen above from a loopback listener
    const feedUrl = await this.startSquirrelServer(squirrelFeedDocument(entry, notesUrl));
    autoUpdater.setFeedURL({ url: feedUrl, serverType: 'default' });
    autoUpdater.checkForUpdates();
  }

  private async startSquirrelServer(document: string): Promise<string> {
    this.closeSquirrelServer();
    return new Promise((resolve, reject) => {
      // plain http is fine here: the listener is loopback-only and the
      // document it serves names an https download url
      const server = new http.Server();
      server.on('request', (_request, response) => {
        response.writeHead(200, {
          'Content-Type': 'application/json',
          'Cache-Control': 'no-store',
        });
        response.end(document);
      });
      server.on('error', reject);
      server.listen(0, '127.0.0.1', () => {
        const { port } = server.address() as AddressInfo;
        this.squirrelServer = server;
        resolve(`http://127.0.0.1:${port}/releases.json`);
      });
    });
  }

  private closeSquirrelServer(): void {
    this.squirrelServer?.close();
    this.squirrelServer = null;
  }

  /** Where Windows installers are staged; shared by every RStudio instance of this user. */
  private stagingDir(): string {
    return path.join(app.getPath('temp'), 'rstudio-updates');
  }

  /**
   * Stage the Windows installer. The staging directory is shared, so a
   * verified installer already there (left by another instance, or by an
   * earlier run whose user chose "Later") is reused rather than replaced,
   * and a fresh download lands under a private name until it is verified.
   */
  private async downloadWindowsInstaller(entry: BuildArtifact): Promise<void> {
    const dir = this.stagingDir();
    await mkdir(dir, { recursive: true });
    const target = path.join(dir, path.basename(entry.filename));

    if (await this.verifyInstaller(target, entry)) {
      logger().logDebug(`reusing the already downloaded ${target}`);
      this.windowsInstaller = target;
      return;
    }

    const partial = `${target}.${process.pid}.part`;
    try {
      await this.fetchToFile(entry, partial);
      if (!(await this.verifyInstaller(partial, entry))) {
        throw new Error('the downloaded installer did not match the checksum and size published for this release');
      }
      await rename(partial, target);
    } finally {
      await rm(partial, { force: true });
    }

    this.windowsInstaller = target;
  }

  private async fetchToFile(entry: BuildArtifact, file: string): Promise<void> {
    const response = await net.fetch(entry.url, { cache: 'no-store' });
    if (!response.ok || !response.body) {
      throw new Error(`download failed with HTTP ${response.status}`);
    }

    const total = Number(response.headers.get('content-length')) || entry.size;
    let received = 0;
    const source = Readable.fromWeb(response.body as import('stream/web').ReadableStream);
    source.on('data', (chunk: Buffer) => {
      received += chunk.length;
      if (total > 0) {
        this.setProgress(received / total);
      }
    });
    await pipeline(source, createWriteStream(file));
  }

  /** True when `file` exists with the size and SHA-256 the API published. */
  private async verifyInstaller(file: string, entry: BuildArtifact): Promise<boolean> {
    try {
      if (entry.size > 0 && (await stat(file)).size !== entry.size) {
        return false;
      }
      const hash = createHash('sha256');
      for await (const chunk of createReadStream(file)) {
        hash.update(chunk as Buffer);
      }
      return hash.digest('hex') === entry.sha256;
    } catch {
      return false;
    }
  }

  /**
   * Drop staged installers that can no longer be used: builds no newer than
   * the running one (the update was applied, or superseded) and partial
   * downloads abandoned by an instance that died. Newer installers stay, so
   * "Later" followed by a relaunch does not download again.
   */
  private async pruneStagedInstallers(): Promise<void> {
    let names: string[];
    try {
      names = await readdir(this.stagingDir());
    } catch {
      return;
    }

    for (const name of names) {
      const file = path.join(this.stagingDir(), name);
      try {
        const version = installerVersion(name);
        let stale: boolean;
        if (version) {
          stale = !isUpdateAvailable(this.currentVersion, version);
        } else if (/\.part$/i.test(name)) {
          stale = Date.now() - (await stat(file)).mtimeMs > kPartialDownloadTtlMs;
        } else {
          stale = false;
        }
        if (stale) {
          logger().logDebug(`removing stale staged installer ${file}`);
          await rm(file, { force: true });
        }
      } catch (error: unknown) {
        logger().logDebug(`could not prune ${file}: ${safeError(error).message}`);
      }
    }
  }

  private onDownloaded(version: string): void {
    this.activeDownload = null;
    this.downloadedVersion = version;
    this.setProgress(-1);
    logger().logInfo(`RStudio ${version} downloaded and ready to install`);
    this.notifyDownloaded(version);
  }

  private onDownloadFailed(message: string): void {
    this.activeDownload = null;
    this.setProgress(-1);
    logger().logErrorMessage(`update download failed: ${message}`);
    this.notifyFailed(message);
  }

  private notifyDownloaded(version: string): void {
    this.callHook('onUpdateDownloaded', version);
  }

  private notifyFailed(message: string): void {
    this.callHook('onUpdateDownloadFailed', message);
  }

  private callHook(name: string, arg: string): void {
    this.mainWindow
      .executeJavaScript(`window.desktopHooks && window.desktopHooks.${name}(${JSON.stringify(arg)})`)
      .catch((error: unknown) => logger().logError(safeError(error)));
  }

  private setProgress(value: number): void {
    try {
      if (!this.mainWindow.window.isDestroyed()) {
        this.mainWindow.window.setProgressBar(value);
      }
    } catch (error: unknown) {
      logger().logDebug(`could not update progress bar: ${safeError(error).message}`);
    }
  }
}
