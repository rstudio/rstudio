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
import { app, autoUpdater, dialog, net } from 'electron';
import { existsSync } from 'fs';
import http from 'http';
import i18next from 'i18next';
import { AddressInfo } from 'net';
import path from 'path';
import { getenv } from '../core/environment';
import { safeError } from '../core/err';
import { logger } from '../core/logger';
import { appState } from './app-state';
import { buildInfo } from './build-info';
import { pruneStagedInstallers, stageInstaller, verifyInstaller } from './installer-staging';
import { MainWindow } from './main-window';
import { defaultMarkerDir, forgetRelaunchProject, installationKey, rememberRelaunchProject } from './relaunch-project';
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
 *      a restart. If accepted, GWT asks `canInstallUpdate(version)` (the
 *      version must be the one downloaded, on Windows its staged installer
 *      must still verify, and no other RStudio process may be running), runs
 *      its save prompts, then calls `installUpdate(version, project)`, which
 *      checks again and records the install, and quits as usual;
 *      `applyPendingUpdate()` then runs as the main window quits: Squirrel
 *      swaps the bundle and relaunches, or the installer runs silently
 *      (`/S /update`) and relaunches RStudio when it is done. The relaunch
 *      reopens the remembered project.
 *
 * One update is in flight at a time, and every step names its version: a
 * check may complete while a download runs or a ready dialog is open, and
 * the dialogs those produce can be answered in any order.
 *
 * On macOS the download is the point of no return: Squirrel applies what it
 * downloaded the next time the application starts whether or not
 * quitAndInstall() is called, as for any Squirrel-updated application.
 * Declining the restart only defers that; a Windows installer is applied
 * only by a restart from here.
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
 * Why the update a ready dialog named cannot be applied right now; '' when
 * it can. 'missing' means it is not the downloaded update (another download
 * replaced it, or its staged installer is gone), so GWT downloads it again.
 * 'unknown' means the other-windows check itself failed, which must block
 * too: the installer would otherwise give up silently after the quit.
 */
export type InstallBlocker = '' | 'other-instances' | 'missing' | 'unknown';

export type UpdateHook = 'onUpdateDownloaded' | 'onUpdateDownloadFailed';

/**
 * Everything the updater does outside its own state: the API, the process
 * list, the two installers and the GWT client. Tests substitute it to drive
 * the state machine; `electronUpdaterHost()` is the real one.
 */
export interface UpdaterHost {
  readonly platform: NodeJS.Platform;
  readonly arch: string;
  /** the running executable; its directory identifies this installation */
  readonly execPath: string;
  fetchText(url: string): Promise<string>;
  /** other RStudio processes of this installation (each window is one); null when the listing failed */
  otherInstances(): Promise<number | null>;
  detectWindowsInstallMode(): Promise<WindowsInstallMode | null>;
  /** where Windows installers are staged */
  stagingDir(): string;
  /** where relaunch-project markers live */
  markerDir(): string;
  /** have Squirrel.Mac download and verify `entry`; resolves once it is staged */
  squirrelDownload(entry: BuildArtifact, notesUrl: string): Promise<void>;
  squirrelInstall(): void;
  /** download and verify a Windows installer; resolves to the staged file */
  stageInstaller(entry: BuildArtifact, onProgress: (fraction: number) => void): Promise<string>;
  verifyInstaller(file: string, entry: BuildArtifact): Promise<boolean>;
  launchInstaller(file: string, mode: WindowsInstallMode): void;
  /** call a desktopHooks function in the GWT client */
  notify(hook: UpdateHook, arg: string): void;
  setProgress(value: number): void;
  /** tell the user, as the window quits, that the accepted update was not installed */
  warnNotInstalled(version: string, blocker: InstallBlocker): void;
}

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
  /** the version whose restart the user accepted; the quit sequence installs it */
  private pendingInstall: string | null = null;

  constructor(private host: UpdaterHost) {
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
    const supported = !disabled && indexed && !isProVersion(this.currentVersion);
    this.keys = supported ? platformKeys(host.platform, host.arch) : null;

    if (this.keys && host.platform === 'win32') {
      void pruneStagedInstallers(host.stagingDir(), this.currentVersion, Date.now(), (file, error) =>
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
    if (this.pendingInstall !== null) {
      // the quit that installs it is under way; a download now could replace
      // the build that quit was accepted for (cancelPendingInstall() reopens
      // this once an abandoned quit is known)
      this.notifyFailed(`RStudio ${this.pendingInstall} is about to be installed.`);
      return;
    }
    if (this.activeDownload) {
      // a repeat request for the download in progress is harmless; one for
      // another build (a check completed meanwhile) is reported rather than
      // dropped, since GWT now shows progress for that build
      if (this.activeDownload.version !== entry.version) {
        this.notifyFailed(
          `RStudio ${this.activeDownload.version} is still being downloaded; wait for it to finish before downloading another update.`,
        );
      }
      return;
    }

    // taken before anything is awaited, so overlapping requests cannot both
    // start
    this.activeDownload = entry;
    try {
      if (this.downloadedVersion === entry.version) {
        // the staged Windows installer lives on disk, where it may have been
        // removed since; only report it ready while it still verifies
        if (this.host.platform !== 'win32' || (await this.stagedInstallerVerifies())) {
          this.onDownloaded(entry.version);
          return;
        }
        logger().logDebug(`the staged installer for ${entry.version} is gone; downloading it again`);
        this.downloadedVersion = null;
        this.staged = null;
      }

      this.host.setProgress(2); // > 1 shows an indeterminate dock / taskbar bar
      if (this.host.platform === 'darwin') {
        await this.host.squirrelDownload(entry, offer.notesUrl);
      } else if (this.host.platform === 'win32') {
        const file = await this.host.stageInstaller(entry, (fraction) => this.host.setProgress(fraction));
        this.staged = { file, entry };
      } else {
        throw new Error('in-app updates are not supported on this platform');
      }
      this.onDownloaded(entry.version);
    } catch (error: unknown) {
      this.onDownloadFailed(safeError(error).message);
    }
  }

  /**
   * Whether `version`, the update a ready dialog named, can be applied right
   * now, asked before "Restart Now" commits to quitting. It must be the
   * update downloaded, with no other download running (a ready dialog can
   * outlive a later download), and on Windows its staged installer must
   * still be on disk and intact. On both platforms no other RStudio window
   * may be open: each window is a separate process, neither installer will
   * swap the application while one is alive, and quitting this window alone
   * would leave the user with neither RStudio nor the update.
   */
  async canInstallUpdate(version: string): Promise<InstallBlocker> {
    return this.preflight(version);
  }

  /**
   * Record that the user accepted the restart into `version`, if it can
   * still be installed (the save prompts ran since canInstallUpdate(), and a
   * download or another window may have started meanwhile); the quit
   * sequence then applies it. `project` is the active project file (or
   * kProjectNone), which the relaunched RStudio reopens. Answers like
   * canInstallUpdate(); GWT quits only on ''.
   */
  async installUpdate(version: string, project: string): Promise<InstallBlocker> {
    const blocker = await this.preflight(version);
    if (blocker) {
      logger().logInfo(`not installing RStudio ${version}: ${blocker}`);
      return blocker;
    }

    // nothing is awaited between the preflight's last look at the state and
    // this, so what it checked is what gets installed
    this.pendingInstall = version;
    rememberRelaunchProject(project, { version, installKey: this.installKey() }, this.host.markerDir());
    return '';
  }

  hasPendingInstall(): boolean {
    return this.pendingInstall !== null;
  }

  /**
   * The quit that was to apply the update did not happen (the session
   * refused it, or the user cancelled), so a later ordinary quit must not
   * run the installer or reopen the remembered project. The download stays
   * staged: the next check offers the restart again, and on macOS Squirrel
   * applies it at the next start regardless (see the class comment).
   */
  cancelPendingInstall(): void {
    if (this.pendingInstall !== null) {
      logger().logDebug(`the quit was abandoned; RStudio ${this.pendingInstall} stays downloaded`);
      this.pendingInstall = null;
      forgetRelaunchProject(this.installKey(), this.host.markerDir());
    }
  }

  /**
   * Apply the accepted update now that the session has shut down and the
   * main window is quitting. Resolves true when the updater took over the
   * quit (Squirrel relaunches the app itself), false when the caller should
   * continue quitting as usual.
   *
   * The checks run once more: the session's shutdown took a while (save
   * prompts, .Last(), the workspace), and a window opened or an installer
   * removed in the meantime would make the installer give up silently once
   * this window is gone, leaving the user with neither RStudio nor the
   * update. Blocked here, the update is left for the next check and the
   * user is told why.
   */
  async applyPendingUpdate(): Promise<boolean> {
    const version = this.pendingInstall;
    if (version === null) {
      return false;
    }

    const blocker = await this.preflight(version);
    this.pendingInstall = null;
    const staged = this.staged;
    const mode = this.windowsInstallMode;
    if (blocker || (this.host.platform === 'win32' && (!staged || !mode))) {
      logger().logErrorMessage(`not installing RStudio ${version} on quit: ${blocker || 'no installer staged'}`);
      forgetRelaunchProject(this.installKey(), this.host.markerDir());
      this.host.warnNotInstalled(version, blocker || 'missing');
      return false;
    }

    if (this.host.platform === 'darwin') {
      logger().logInfo(`installing RStudio ${version} and relaunching`);
      this.host.squirrelInstall();
      return true;
    }

    if (this.host.platform === 'win32' && staged && mode) {
      // the installer waits (via /update) for this process to exit before it
      // touches the install directory, then relaunches RStudio
      logger().logInfo(`launching ${staged.file} to install RStudio ${version}`);
      this.host.launchInstaller(staged.file, mode);
    }
    return false;
  }

  /** Every check behind canInstallUpdate() and the install itself. */
  private async preflight(version: string): Promise<InstallBlocker> {
    if (!this.isDownloaded(version)) {
      logger().logInfo(`RStudio ${version} is not the downloaded update; it must be downloaded again`);
      return 'missing';
    }
    if (this.host.platform === 'win32' && !(await this.stagedInstallerVerifies())) {
      logger().logInfo('the staged installer is missing or altered; it must be downloaded again');
      return 'missing';
    }

    const others = await this.host.otherInstances();
    if (others === null) {
      logger().logErrorMessage('could not list running RStudio processes; not installing the update');
      return 'unknown';
    }
    if (others > 0) {
      logger().logInfo(`not installing the update yet: ${others} other RStudio process(es) running`);
      return 'other-instances';
    }

    // a download may have started or finished while the checks above waited
    if (!this.isDownloaded(version)) {
      logger().logInfo(`RStudio ${version} is no longer the downloaded update`);
      return 'missing';
    }
    return '';
  }

  private isDownloaded(version: string): boolean {
    return this.downloadedVersion === version && this.activeDownload === null;
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
      const current = parseDevelopmentBranch(await this.host.fetchText(releaseIndexUrl(this.baseUrl)));
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
      const release = parseReleaseIndex(await this.host.fetchText(indexUrl));
      notesUrl = release.notesUrl;
      buildIndex = await this.host.fetchText(release.buildsUrl);
    } else {
      buildIndex = await this.host.fetchText(indexUrl);
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

  private async canInstall(): Promise<boolean> {
    if (this.host.platform === 'darwin') {
      return true;
    }
    if (this.host.platform === 'win32') {
      this.windowsInstallMode = await this.host.detectWindowsInstallMode();
      if (!this.windowsInstallMode) {
        logger().logDebug('RStudio is not registered as an NSIS install; offering a manual download instead');
      }
      return this.windowsInstallMode !== null;
    }
    return false;
  }

  private async stagedInstallerVerifies(): Promise<boolean> {
    return this.staged !== null && (await this.host.verifyInstaller(this.staged.file, this.staged.entry));
  }

  private installKey(): string {
    return installationKey(this.host.execPath, this.host.platform);
  }

  private onDownloaded(version: string): void {
    this.activeDownload = null;
    this.downloadedVersion = version;
    this.host.setProgress(-1);
    logger().logInfo(`RStudio ${version} downloaded and ready to install`);
    this.host.notify('onUpdateDownloaded', version);
  }

  private onDownloadFailed(message: string): void {
    this.activeDownload = null;
    this.host.setProgress(-1);
    logger().logErrorMessage(`update download failed: ${message}`);
    this.notifyFailed(message);
  }

  private notifyFailed(message: string): void {
    this.host.notify('onUpdateDownloadFailed', message);
  }
}

/** The updater's surroundings in the running application. */
export function electronUpdaterHost(mainWindow: MainWindow): UpdaterHost {
  let squirrelServer: http.Server | null = null;

  const closeSquirrelServer = () => {
    squirrelServer?.close();
    squirrelServer = null;
  };

  // Squirrel.Mac wants its own feed format from an update server, and
  // downloads whatever that feed names, so serve it a one-entry document for
  // the build already chosen from a loopback listener
  const startSquirrelServer = async (document: string): Promise<string> => {
    closeSquirrelServer();
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
        squirrelServer = server;
        resolve(`http://127.0.0.1:${port}/releases.json`);
      });
    });
  };

  const blockerMessage = (blocker: InstallBlocker): string => {
    switch (blocker) {
      case 'other-instances':
        return i18next.t('updaterTs.otherWindowsWereOpen');
      case 'unknown':
        return i18next.t('updaterTs.couldNotCheckOtherWindows');
      default:
        return i18next.t('updaterTs.installerNoLongerAvailable');
    }
  };

  return {
    platform: process.platform,
    arch: process.arch,
    execPath: process.execPath,

    async fetchText(url: string): Promise<string> {
      const response = await net.fetch(url, {
        cache: 'no-store',
        signal: AbortSignal.timeout(kRequestTimeoutMs),
      });
      if (!response.ok) {
        throw new Error(`${url} answered HTTP ${response.status}`);
      }
      return response.text();
    },

    async otherInstances(): Promise<number | null> {
      // every process of this application (renderer, GPU, utility) may run
      // the same executable; only processes outside it are other windows
      const ownPids = app.getAppMetrics().map((metric) => metric.pid);
      return otherInstanceCount(process.execPath, ownPids);
    },

    async detectWindowsInstallMode(): Promise<WindowsInstallMode | null> {
      return detectWindowsInstallMode(path.dirname(process.execPath));
    },

    // keyed by the install directory so that every window of one
    // installation shares (and can reuse) a staged installer, while another
    // copy of RStudio on the same machine, e.g. an unpacked zip, never
    // prunes or reuses files meant for this one
    stagingDir(): string {
      return path.join(app.getPath('temp'), 'rstudio-updates', installationKey(process.execPath));
    },

    markerDir(): string {
      return defaultMarkerDir();
    },

    async squirrelDownload(entry: BuildArtifact, notesUrl: string): Promise<void> {
      return new Promise((resolve, reject) => {
        const settle = (outcome: () => void) => {
          autoUpdater.off('update-downloaded', onDownloaded);
          autoUpdater.off('update-not-available', onNotAvailable);
          autoUpdater.off('error', onError);
          closeSquirrelServer();
          outcome();
        };
        const onDownloaded = () => settle(resolve);
        const onNotAvailable = () => settle(() => reject(new Error('The update server did not offer an update.')));
        const onError = (error: Error) => settle(() => reject(error));

        startSquirrelServer(squirrelFeedDocument(entry, notesUrl))
          .then((feedUrl) => {
            autoUpdater.on('update-downloaded', onDownloaded);
            autoUpdater.on('update-not-available', onNotAvailable);
            autoUpdater.on('error', onError);
            autoUpdater.setFeedURL({ url: feedUrl, serverType: 'default' });
            autoUpdater.checkForUpdates();
          })
          .catch(reject);
      });
    },

    squirrelInstall(): void {
      autoUpdater.quitAndInstall();
    },

    async stageInstaller(entry: BuildArtifact, onProgress: (fraction: number) => void): Promise<string> {
      const fetch = async (url: string) => net.fetch(url, { cache: 'no-store' });
      return stageInstaller(this.stagingDir(), entry, fetch, onProgress);
    },

    async verifyInstaller(file: string, entry: BuildArtifact): Promise<boolean> {
      return verifyInstaller(file, entry);
    },

    launchInstaller(file: string, mode: WindowsInstallMode): void {
      if (!existsSync(file)) {
        logger().logErrorMessage(`${file} is gone; not installing`);
        return;
      }
      const child = spawn(file, ['/S', `/${mode}`, '/update'], {
        detached: true,
        stdio: 'ignore',
        windowsHide: true,
      });
      child.on('error', (error) => logger().logError(error));
      child.unref();
    },

    notify(hook: UpdateHook, arg: string): void {
      mainWindow
        .executeJavaScript(`window.desktopHooks && window.desktopHooks.${hook}(${JSON.stringify(arg)})`)
        .catch((error: unknown) => logger().logError(safeError(error)));
    },

    setProgress(value: number): void {
      try {
        if (!mainWindow.window.isDestroyed()) {
          mainWindow.window.setProgressBar(value);
        }
      } catch (error: unknown) {
        logger().logDebug(`could not update progress bar: ${safeError(error).message}`);
      }
    },

    warnNotInstalled(version: string, blocker: InstallBlocker): void {
      const options: Electron.MessageBoxSyncOptions = {
        type: 'warning',
        title: appState().activation().editionName(),
        message: i18next.t('updaterTs.updateNotInstalled', { version }),
        detail: blockerMessage(blocker),
      };
      appState().modalTracker.trackElectronModalSync(() =>
        mainWindow.window.isDestroyed()
          ? dialog.showMessageBoxSync(options)
          : dialog.showMessageBoxSync(mainWindow.window, options),
      );
    },
  };
}
