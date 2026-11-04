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
import { rm } from 'fs/promises';
import http from 'http';
import i18next from 'i18next';
import { AddressInfo } from 'net';
import path from 'path';
import { getenv } from '../core/environment';
import { safeError } from '../core/err';
import { logger } from '../core/logger';
import { appState } from './app-state';
import { kRStudioInitialProject, kRStudioInitialWorkingDir } from '../core/r-user-data';
import { buildInfo } from './build-info';
import { FileStamp, fileStamp, pruneStagedInstallers, stageInstaller, verifyInstaller } from './installer-staging';
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
  parseReleasedBranch,
  PlatformKeys,
  platformKeys,
  releaseIndexUrl,
  squirrelFeedDocument,
  UpdateChannel,
  updateIndexUrl,
} from './update-feed';
import { execFileRunner, otherInstanceCount } from './running-instances';
import { isAdministrator } from './windows-administrator';
import {
  installerSignature,
  isTrustedInstallerSignature,
  kInstallerSigner,
  kSignatureCheckTimeoutMs,
} from './windows-authenticode';
import { detectWindowsInstallMode, WindowsInstallMode } from './windows-install-mode';

/**
 * The environment RStudio was launched with. Detecting R rewrites
 * process.env for the sessions (R_HOME, PATH, RSTUDIO_WHICH_R, ...), and the
 * Windows installer, and the RStudio it relaunches, must not inherit that.
 */
const kLaunchEnvironment: NodeJS.ProcessEnv = { ...process.env };

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
 *
 * For release builds the answer is marked `confirm-with-session`: the
 * builds site says what is published, but rstudio.org's check_for_update
 * (the session's check) is where Posit withdraws a release and marks one
 * urgent, so GWT runs that check too and lets it have the final say. The
 * site knows nothing about daily and preview builds, so those are offered
 * from the builds site alone, never as urgent.
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
  /** GWT should run the session's check as well and defer to its answer (see the class comment) */
  'confirm-with-session'?: boolean;
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

/** Why an accepted update was not installed at the quit: a blocker, or the installer would not start. */
export type InstallFailure = Exclude<InstallBlocker, ''> | 'launch-failed';

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
  /** whether this Windows user can consent to the UAC prompt an all-users install raises; null when unknown */
  isAdministrator(): Promise<boolean | null>;
  /** where Windows installers are staged */
  stagingDir(): string;
  /** where relaunch-project markers live */
  markerDir(): string;
  /** have Squirrel.Mac download and verify `entry`; resolves once it is staged */
  squirrelDownload(entry: BuildArtifact, notesUrl: string): Promise<void>;
  squirrelInstall(version: string): void;
  /** download and verify a Windows installer; resolves to the staged file */
  stageInstaller(entry: BuildArtifact, onProgress: (fraction: number) => void): Promise<string>;
  verifyInstaller(file: string, entry: BuildArtifact): Promise<boolean>;
  /** the size and modification time of a staged installer; null when it is gone */
  fileStamp(file: string): Promise<FileStamp | null>;
  /** start the Windows installer; false when it would not start */
  launchInstaller(file: string, mode: WindowsInstallMode): Promise<boolean>;
  /** call a desktopHooks function in the GWT client */
  notify(hook: UpdateHook, arg: string): void;
  setProgress(value: number): void;
  /** tell the user, as the window quits, that the accepted update was not installed */
  warnNotInstalled(version: string, reason: InstallFailure): void;
}

/** A verified Windows installer on disk and the API entry it was checked against. */
interface StagedInstaller {
  file: string;
  entry: BuildArtifact;
  /** the file as it was when it last hashed correctly; null when it must be hashed again */
  stamp: FileStamp | null;
}

/** Where a check reads the newest build from: the release API, or a branch index. */
type UpdateIndex = { kind: 'release'; buildsUrl: string; notesUrl: string } | { kind: 'branch'; url: string };

function describeIndex(index: UpdateIndex): string {
  return index.kind === 'release' ? index.buildsUrl : index.url;
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
  /** the project the relaunch after that install reopens */
  private pendingProject = '';
  /** applyPendingUpdate() is deciding; the window must stay open until it has */
  private handingOff = false;

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
      const index = await this.resolveIndex();
      const found = await this.readIndex(index, this.keys);
      if (!found) {
        // the branch is not on the site (yet), or its newest build is not on
        // this channel, so there is nothing to compare with; the session's
        // own check still runs
        logger().logDebug(
          `${describeIndex(index)} offers no ${this.keys.download} build; deferring to the session check`,
        );
        return { status: 'unsupported' };
      }
      update = found;
    } catch (error: unknown) {
      const message = safeError(error).message;
      logger().logDebug(`update check (${manual ? 'manual' : 'automatic'}) failed: ${message}`);
      return { status: 'error', error: message };
    }

    const confirm = this.channel === 'release';
    if (!isUpdateAvailable(this.currentVersion, update.download.version)) {
      return { status: 'up-to-date', 'confirm-with-session': confirm };
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
      'confirm-with-session': confirm,
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
        this.staged = { file, entry, stamp: null };
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
   * kProjectNone), which the relaunched RStudio reopens; it is written down
   * for that relaunch only at the handoff to the installer, so the marker's
   * lifetime does not run out during a slow session shutdown. Answers like
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
    this.pendingProject = project;
    return '';
  }

  hasPendingInstall(): boolean {
    return this.pendingInstall !== null;
  }

  /**
   * Whether applyPendingUpdate() is between the session's exit and its
   * decision. The window must not close meanwhile: the session is gone, so
   * nothing else keeps the process alive, and ending it now would skip the
   * accepted install without a word.
   */
  isHandingOff(): boolean {
    return this.handingOff;
  }

  /**
   * The quit that was to apply the update is not happening (GWT could not
   * start it, the session refused it, or the user cancelled), so a later
   * ordinary quit must not run the installer. The download stays staged:
   * the next check offers the restart again, and on macOS Squirrel applies
   * it at the next start regardless (see the class comment). With
   * `version`, only that request's record is dropped, never one made since.
   */
  cancelPendingInstall(version?: string): void {
    if (this.pendingInstall === null || (version !== undefined && this.pendingInstall !== version)) {
      return;
    }
    logger().logDebug(`the quit was abandoned; RStudio ${this.pendingInstall} stays downloaded`);
    this.pendingInstall = null;
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

    this.handingOff = true;
    try {
      const blocker = await this.preflight(version);
      if (this.pendingInstall !== version) {
        // cancelled while the checks ran; the marker went with it
        logger().logInfo(`not installing RStudio ${version} on quit: the install was cancelled`);
        return false;
      }
      this.pendingInstall = null;
      const staged = this.staged;
      const mode = this.windowsInstallMode;
      if (blocker || (this.host.platform === 'win32' && (!staged || !mode))) {
        logger().logErrorMessage(`not installing RStudio ${version} on quit: ${blocker || 'no installer staged'}`);
        this.notInstalled(version, blocker || 'missing');
        return false;
      }

      // the session is down and the installer is about to run, so the
      // marker's lifetime starts now; written any earlier, a long shutdown
      // (.Last(), the workspace) could outlast it and lose the project
      rememberRelaunchProject(this.pendingProject, { version, installKey: this.installKey() }, this.host.markerDir());

      if (this.host.platform === 'darwin') {
        // Squirrel closes the windows itself, which the guard must not refuse
        this.handingOff = false;
        logger().logInfo(`installing RStudio ${version} and relaunching`);
        this.host.squirrelInstall(version);
        return true;
      }

      if (this.host.platform === 'win32' && staged && mode) {
        // the installer waits (via /update) for this process to exit before
        // it touches the install directory, then relaunches RStudio; until
        // it has started, the window stays open for the same reason as above
        logger().logInfo(`launching ${staged.file} to install RStudio ${version}`);
        if (!(await this.host.launchInstaller(staged.file, mode))) {
          logger().logErrorMessage(`the installer for RStudio ${version} did not start`);
          this.notInstalled(version, 'launch-failed');
        }
      }
      return false;
    } finally {
      this.handingOff = false;
    }
  }

  /** the accepted update is not going to be installed by this quit; the download stays for the next check */
  private notInstalled(version: string, reason: InstallFailure): void {
    forgetRelaunchProject(this.installKey(), this.host.markerDir());
    this.host.warnNotInstalled(version, reason);
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
   * Where this check reads the newest build from. Release builds read the
   * release API. Daily and hourly builds move to a new branch at every
   * release cut (version/new-version.sh), so unless a branch was pinned they
   * follow whichever branch the release index says is receiving dailies.
   * Otherwise (preview builds, and dailies with no such branch) the build's
   * own branch is read -- through the release API once that branch has been
   * released, since a released branch's own index lists unpublished patch
   * candidates, which must never be offered.
   */
  private async resolveIndex(): Promise<UpdateIndex> {
    const releaseIndex = await this.host.fetchText(releaseIndexUrl(this.baseUrl));
    if (this.channel === 'release') {
      return { kind: 'release', ...parseReleaseIndex(releaseIndex) };
    }

    if (!this.branchPinned && (this.channel === 'daily' || this.channel === 'hourly')) {
      const current = parseDevelopmentBranch(releaseIndex);
      if (current) {
        return { kind: 'branch', url: this.branchIndexUrl(current) };
      }
      logger().logDebug(`the release index names no branch in development; staying on ${this.branch}`);
    }

    const released = parseReleasedBranch(releaseIndex, this.branch);
    if (released) {
      logger().logDebug(`${this.branch} has been released; reading its published builds`);
      return { kind: 'release', ...released };
    }
    return { kind: 'branch', url: this.branchIndexUrl(this.branch) };
  }

  private branchIndexUrl(branch: string): string {
    // the constructor checked that this channel has an index
    return updateIndexUrl(this.baseUrl, this.channel, branch) as string;
  }

  /**
   * Resolve the newest build in `index`. The release API needs a second
   * request for the build's artifacts; branch indexes carry them inline. A
   * branch index may hold builds of another channel (release candidates
   * once a branch moves towards release), which are not offered: those are
   * published through the release API, or not at all.
   */
  private async readIndex(index: UpdateIndex, keys: PlatformKeys): Promise<AvailableUpdate | null> {
    const notesUrl = index.kind === 'release' ? index.notesUrl : '';
    const buildIndex = await this.host.fetchText(index.kind === 'release' ? index.buildsUrl : index.url);

    const download = parseBuildIndex(buildIndex, keys.download);
    if (!download) {
      return null;
    }
    if (index.kind === 'branch' && channelForVersion(download.version) !== this.channel) {
      logger().logDebug(
        `${index.url} is at ${download.version}, which is not a ${this.channel} build; not offering it`,
      );
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
    if (this.host.platform !== 'win32') {
      return false;
    }

    // the mode is kept from the last check that could read it: the registry
    // read can fail transiently, and a null here after a download would send
    // the user through the quit only to find no installer to run
    const mode = await this.host.detectWindowsInstallMode();
    if (mode) {
      this.windowsInstallMode = mode;
    } else if (!this.windowsInstallMode) {
      logger().logDebug('RStudio is not registered as an NSIS install; offering a manual download instead');
      return false;
    }

    // an all-users install elevates, and a user who is not an administrator
    // gets a credentials prompt they most likely cannot answer, after which
    // the installer gives up; they are sent to the download page instead
    if (this.windowsInstallMode === 'allusers') {
      const admin = await this.host.isAdministrator();
      if (!admin) {
        logger().logDebug(
          admin === null
            ? 'could not tell whether this user is an administrator; offering a manual download instead'
            : 'this user is not an administrator, so an all-users install cannot be updated in-app',
        );
        return false;
      }
    }
    return true;
  }

  /**
   * Whether the staged installer still has the bytes that were verified.
   * Hashing it reads a few hundred megabytes, and one restart asks three
   * times (before the save prompts, after them, and at the quit handoff),
   * so an unchanged file (same size and modification time as when it last
   * hashed correctly) is trusted without reading it again.
   */
  private async stagedInstallerVerifies(): Promise<boolean> {
    const staged = this.staged;
    if (!staged) {
      return false;
    }

    // stamped before hashing: a change after the stat shows up as a new
    // stamp next time, so the file is hashed again rather than trusted
    const stamp = await this.host.fileStamp(staged.file);
    if (!stamp) {
      staged.stamp = null;
      return false;
    }
    if (staged.stamp && staged.stamp.size === stamp.size && staged.stamp.mtimeMs === stamp.mtimeMs) {
      return true;
    }

    const verified = await this.host.verifyInstaller(staged.file, staged.entry);
    staged.stamp = verified ? stamp : null;
    return verified;
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

/**
 * The environment the Windows installer runs in, from `env`, the one RStudio
 * was launched with (not process.env, which detecting R has rewritten for
 * the sessions). The installer's relaunch of RStudio inherits it, so it must
 * not carry what this instance was launched for: the project a new window
 * was opened with, which would make the relaunch skip the relaunch marker
 * (as application-launch.ts does for the sessions it starts), or a version
 * impersonated for testing, which would have the new build offer the same
 * update again.
 */
export function installerEnvironment(env: NodeJS.ProcessEnv): NodeJS.ProcessEnv {
  const clean = { ...env };
  delete clean[kRStudioInitialProject];
  delete clean[kRStudioInitialWorkingDir];
  delete clean.RSTUDIO_UPDATE_VERSION;
  return clean;
}

/** The updater's surroundings in the running application. */
export function electronUpdaterHost(mainWindow: MainWindow): UpdaterHost {
  let squirrelServer: http.Server | null = null;
  /** the version quitAndInstall() was asked to install, until it has */
  let squirrelInstalling: string | null = null;

  const closeSquirrelServer = () => {
    squirrelServer?.close();
    squirrelServer = null;
  };

  const failureMessage = (reason: InstallFailure): string => {
    switch (reason) {
      case 'other-instances':
        return i18next.t('updaterTs.otherWindowsWereOpen');
      case 'unknown':
        return i18next.t('updaterTs.couldNotCheckOtherWindows');
      case 'launch-failed':
        return i18next.t('updaterTs.installerDidNotStart');
      default:
        return i18next.t('updaterTs.installerNoLongerAvailable');
    }
  };

  const warnNotInstalled = (version: string, reason: InstallFailure): void => {
    const options: Electron.MessageBoxSyncOptions = {
      type: 'warning',
      title: appState().activation().editionName(),
      message: i18next.t('updaterTs.updateNotInstalled', { version }),
      detail: failureMessage(reason),
    };
    appState().modalTracker.trackElectronModalSync(() =>
      mainWindow.window.isDestroyed()
        ? dialog.showMessageBoxSync(options)
        : dialog.showMessageBoxSync(mainWindow.window, options),
    );
  };

  // Squirrel reports every failure through 'error', including ones from
  // quitAndInstall() after the download's own listener is gone (the windows
  // are closed by then); an 'error' nobody listens for is thrown instead
  if (process.platform === 'darwin') {
    autoUpdater.on('error', (error: Error) => {
      if (squirrelInstalling !== null) {
        const version = squirrelInstalling;
        squirrelInstalling = null;
        logger().logErrorMessage(`Squirrel did not install RStudio ${version}: ${error.message}`);
        warnNotInstalled(version, 'launch-failed');
      } else {
        logger().logDebug(`Squirrel: ${error.message}`);
      }
    });
  }

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
            try {
              autoUpdater.setFeedURL({ url: feedUrl, serverType: 'default' });
              autoUpdater.checkForUpdates();
            } catch (error: unknown) {
              // thrown rather than reported, e.g. an unsigned build's "Could
              // not get code signature"; the listeners and server go too
              settle(() => reject(safeError(error)));
            }
          })
          .catch(reject);
      });
    },

    squirrelInstall(version: string): void {
      squirrelInstalling = version;
      autoUpdater.quitAndInstall();
    },

    async stageInstaller(entry: BuildArtifact, onProgress: (fraction: number) => void): Promise<string> {
      const fetch = async (url: string, signal?: AbortSignal) => net.fetch(url, { cache: 'no-store', signal });
      const file = await stageInstaller(this.stagingDir(), entry, fetch, onProgress);

      // the checksum only proves the file is what the API named; the
      // installer runs silently (no UAC prompt for a per-user install), so
      // it must also be what Posit signed. Checked once per download: later
      // checks compare the file against this very checksum.
      const signature = await installerSignature(file, execFileRunner(kSignatureCheckTimeoutMs));
      if (!isTrustedInstallerSignature(signature, kInstallerSigner)) {
        await rm(file, { force: true });
        const found = signature ? `${signature.status}, signed by "${signature.subject}"` : 'unknown';
        throw new Error(`the downloaded installer is not signed by ${kInstallerSigner} (${found})`);
      }
      return file;
    },

    async verifyInstaller(file: string, entry: BuildArtifact): Promise<boolean> {
      return verifyInstaller(file, entry);
    },

    async fileStamp(file: string): Promise<FileStamp | null> {
      return fileStamp(file);
    },

    async isAdministrator(): Promise<boolean | null> {
      return isAdministrator(execFileRunner());
    },

    async launchInstaller(file: string, mode: WindowsInstallMode): Promise<boolean> {
      if (!existsSync(file)) {
        logger().logErrorMessage(`${file} is gone; not installing`);
        return false;
      }
      // spawn() reports a refusal (a policy blocking the executable, say)
      // through 'error' rather than by throwing
      return new Promise((resolve) => {
        const child = spawn(file, ['/S', `/${mode}`, '/update'], {
          detached: true,
          stdio: 'ignore',
          windowsHide: true,
          env: installerEnvironment(kLaunchEnvironment),
        });
        child.once('spawn', () => {
          child.unref();
          resolve(true);
        });
        child.once('error', (error) => {
          logger().logError(error);
          resolve(false);
        });
      });
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

    warnNotInstalled,
  };
}
