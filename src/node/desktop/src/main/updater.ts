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
import { app, autoUpdater, net } from 'electron';
import { existsSync } from 'fs';
import http from 'http';
import { AddressInfo } from 'net';
import path from 'path';
import { getenv } from '../core/environment';
import { safeError } from '../core/err';
import { logger } from '../core/logger';
import { buildInfo } from './build-info';
import { pruneStagedInstallers, stageInstaller, verifyInstaller } from './installer-staging';
import { MainWindow } from './main-window';
import { forgetRelaunchProject, installationKey, RelaunchIdentity, rememberRelaunchProject } from './relaunch-project';
import {
  branchSlug,
  BuildArtifact,
  channelForVersion,
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
import { otherInstanceCount } from './running-instances';
import { detectWindowsInstallMode, WindowsInstallMode } from './windows-install-mode';

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
 *      a restart. If accepted, GWT asks `canInstallUpdate()` (on Windows, the
 *      staged installer must still verify and no other RStudio process may
 *      be running), calls `installUpdate()` with the active project and runs
 *      its normal quit sequence; `applyPendingUpdate()` then runs as the
 *      main window quits: Squirrel swaps the bundle and relaunches, or the
 *      installer runs silently (`/S /update`) and relaunches RStudio when it
 *      is done. The relaunch reopens the remembered project.
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

/**
 * Why a downloaded update cannot be applied right now; '' when it can.
 * 'unknown' means the other-windows check itself failed, which must block
 * too: the installer would otherwise give up silently after the quit.
 */
export type InstallBlocker = '' | 'other-instances' | 'missing' | 'unknown';

/** A verified Windows installer on disk and the API entry it was checked against. */
interface StagedInstaller {
  file: string;
  entry: BuildArtifact;
}

export class Updater {
  private keys: PlatformKeys | null;
  private channel: UpdateChannel;
  private currentVersion: string;
  private baseUrl: string;
  private branch: string;
  private branchPinned: boolean;
  /** every update offered to the user this session, by version; a dialog may outlive later checks */
  private offers = new Map<string, AvailableUpdate>();
  private activeDownload: BuildArtifact | null = null;
  private downloadedVersion: string | null = null;
  private staged: StagedInstaller | null = null;
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
      void pruneStagedInstallers(this.stagingDir(), this.currentVersion, Date.now(), (file, error) =>
        logger().logDebug(`could not prune ${file}: ${safeError(error).message}`),
      );
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

    this.offers.set(update.download.version, update);
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
   * Start downloading the update the user accepted, named by the version its
   * dialog showed: checks may have found a newer build since, and that one
   * may have been declined. Completion is reported to GWT through
   * desktopHooks rather than a return value, since the Squirrel download runs
   * outside our control.
   */
  async downloadUpdate(version: string): Promise<void> {
    const offer = this.offers.get(version);
    const entry = offer?.install;
    if (!offer || !entry) {
      this.notifyFailed(`RStudio ${version} is not available to download.`);
      return;
    }
    if (this.activeDownload) {
      return;
    }

    // taken before anything is awaited, so overlapping requests cannot both
    // start; later checks may also replace `available` while this download
    // runs, so completion reports the artifact that was actually fetched
    this.activeDownload = entry;
    try {
      if (this.downloadedVersion === entry.version) {
        // the staged Windows installer lives on disk, where it may have been
        // removed since; only report it ready while it still verifies
        if (process.platform !== 'win32' || (await this.stagedInstallerVerifies())) {
          this.onDownloaded(entry.version);
          return;
        }
        logger().logDebug(`the staged installer for ${entry.version} is gone; downloading it again`);
        this.downloadedVersion = null;
        this.staged = null;
      }

      this.setProgress(2); // > 1 shows an indeterminate dock / taskbar bar
      if (process.platform === 'darwin') {
        await this.downloadWithSquirrel(entry, offer.notesUrl);
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
   * Whether the downloaded update can be applied right now, asked before
   * "Restart Now" commits to quitting. On Windows the staged installer must
   * still be on disk and intact ('missing' asks GWT to download it again).
   * On both platforms no other RStudio window may be open: each window is a
   * separate process, neither installer will swap the application while one
   * is alive, and quitting this window alone would leave the user with
   * neither RStudio nor the update.
   */
  async canInstallUpdate(): Promise<InstallBlocker> {
    if (process.platform === 'win32' && !(await this.stagedInstallerVerifies())) {
      logger().logInfo('the staged installer is missing or altered; it must be downloaded again');
      return 'missing';
    }

    // every process of this application (renderer, GPU, utility) may run the
    // same executable; only processes outside it are other windows
    const ownPids = app.getAppMetrics().map((metric) => metric.pid);
    const others = await otherInstanceCount(process.execPath, ownPids);
    if (others === null) {
      logger().logErrorMessage('could not list running RStudio processes; not installing the update');
      return 'unknown';
    }
    if (others > 0) {
      logger().logInfo(`not installing the update yet: ${others} other RStudio process(es) running`);
      return 'other-instances';
    }
    return '';
  }

  /**
   * Record that the user accepted the restart; the quit sequence applies it.
   * `project` is the active project file (or kProjectNone), which the
   * relaunched RStudio reopens.
   */
  installUpdate(project: string): void {
    if (this.downloadedVersion === null) {
      logger().logErrorMessage('installUpdate() called before an update was downloaded');
      return;
    }
    this.pendingInstall = true;
    rememberRelaunchProject(project, this.identity());
  }

  /**
   * The quit that was to apply the update did not happen (the session
   * refused it, or the user cancelled), so a later ordinary quit must not
   * install. The download itself stays staged.
   */
  cancelPendingInstall(): void {
    if (this.pendingInstall) {
      logger().logDebug('the quit was abandoned; the downloaded update stays staged');
      this.pendingInstall = false;
      forgetRelaunchProject(this.identity().installKey);
    }
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

    if (process.platform === 'win32' && this.staged && this.windowsInstallMode) {
      // canInstallUpdate() verified the installer moments ago; this is the
      // last cheap check before handing it the quit
      if (!existsSync(this.staged.file)) {
        logger().logErrorMessage(`${this.staged.file} is gone; not installing RStudio ${this.downloadedVersion}`);
        forgetRelaunchProject(this.identity().installKey);
        return false;
      }

      // the installer waits (via /update) for this process to exit before it
      // touches the install directory, then relaunches RStudio
      logger().logInfo(`launching ${this.staged.file} to install RStudio ${this.downloadedVersion}`);
      const child = spawn(this.staged.file, ['/S', `/${this.windowsInstallMode}`, '/update'], {
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

  /**
   * Where Windows installers are staged. Keyed by the install directory so
   * that every window of one installation shares (and can reuse) a staged
   * installer, while another copy of RStudio on the same machine, e.g. an
   * unpacked zip, never prunes or reuses files meant for this one.
   */
  private stagingDir(): string {
    return path.join(app.getPath('temp'), 'rstudio-updates', installationKey(process.execPath));
  }

  private async downloadWindowsInstaller(entry: BuildArtifact): Promise<void> {
    const fetch = async (url: string) => net.fetch(url, { cache: 'no-store' });
    const file = await stageInstaller(this.stagingDir(), entry, fetch, (fraction) => this.setProgress(fraction));
    this.staged = { file, entry };
  }

  private async stagedInstallerVerifies(): Promise<boolean> {
    return this.staged !== null && (await verifyInstaller(this.staged.file, this.staged.entry));
  }

  private identity(): RelaunchIdentity {
    return { version: this.downloadedVersion ?? this.currentVersion, installKey: installationKey(process.execPath) };
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
