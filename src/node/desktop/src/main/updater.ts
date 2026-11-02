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
import { createWriteStream } from 'fs';
import { mkdir, rm, stat } from 'fs/promises';
import path from 'path';
import { Readable } from 'stream';
import { pipeline } from 'stream/promises';
import { getenv } from '../core/environment';
import { safeError } from '../core/err';
import { logger } from '../core/logger';
import { buildInfo } from './build-info';
import { MainWindow } from './main-window';
import {
  channelForVersion,
  isUpdateAvailable,
  kDefaultUpdateFeedBaseUrl,
  parseReleaseFeed,
  ReleaseFeedEntry,
  UpdateChannel,
  updateFeedUrl,
} from './update-feed';
import { detectWindowsInstallMode, WindowsInstallMode } from './windows-install-mode';

/**
 * In-app updates for RStudio Desktop, modelled on Positron's updater.
 *
 * The flow is split between this class and the GWT client, which owns every
 * dialog so the experience matches the rest of the IDE:
 *
 *   1. GWT asks `checkForUpdates()`; we fetch the channel's releases.json and
 *      compare versions. The answer uses the same keys as the session's
 *      check_for_updates RPC so GWT can show its existing dialog, plus
 *      `can-install` when this platform can apply the update itself.
 *   2. The user accepts; GWT calls `downloadUpdate()`. macOS hands the feed to
 *      Electron's autoUpdater (Squirrel.Mac), which verifies the download and
 *      the new bundle's code signature. Windows downloads the NSIS installer
 *      and checks its SHA-256 against the feed.
 *   3. We call back into GWT (`desktopHooks.onUpdateDownloaded`), which offers
 *      a restart. If accepted, GWT runs its normal quit sequence after calling
 *      `installUpdate()`; `applyPendingUpdate()` then runs as the main window
 *      quits: Squirrel swaps the bundle and relaunches, or the installer runs
 *      silently (`/S /update`) and relaunches RStudio when it is done.
 *
 * A feed that is missing or unreachable answers `unsupported`, so GWT falls
 * back to the session's check and users see today's "Quit and Download"
 * dialog. Linux always answers `unsupported` (#4186 tracks package repos).
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

/** How long the feed fetch may take before it counts as unreachable. */
const kFeedTimeoutMs = 15000;

export class Updater {
  private feedUrl: string | null = null;
  private channel: UpdateChannel;
  private currentVersion: string;
  private available: ReleaseFeedEntry | null = null;
  private downloading = false;
  private downloadedVersion: string | null = null;
  private windowsInstaller: string | null = null;
  private windowsInstallMode: WindowsInstallMode | null = null;
  private pendingInstall = false;
  private macListenersInstalled = false;

  constructor(private mainWindow: MainWindow) {
    // RSTUDIO_UPDATE_VERSION impersonates another version, as it does for the
    // session's check; RSTUDIO_UPDATE_CHANNEL and RSTUDIO_UPDATE_FEED_URL
    // point a build at another channel or a test feed
    this.currentVersion = getenv('RSTUDIO_UPDATE_VERSION') || buildInfo().RSTUDIO_VERSION;
    const channelOverride = getenv('RSTUDIO_UPDATE_CHANNEL');
    this.channel = channelOverride ? (channelOverride as UpdateChannel) : channelForVersion(this.currentVersion);
    const baseUrl = getenv('RSTUDIO_UPDATE_FEED_URL') || kDefaultUpdateFeedBaseUrl;

    // developer builds have nothing to update to unless a feed is forced
    if (this.channel !== 'dev' || getenv('RSTUDIO_UPDATE_FEED_URL')) {
      this.feedUrl = updateFeedUrl(baseUrl, this.channel, process.platform, process.arch);
    }
  }

  /** Whether in-app update checks are possible on this platform and build. */
  supported(): boolean {
    return this.feedUrl !== null;
  }

  async checkForUpdates(manual: boolean): Promise<UpdateCheckResult> {
    if (!this.feedUrl) {
      return { status: 'unsupported' };
    }

    let entry: ReleaseFeedEntry;
    try {
      const response = await net.fetch(this.feedUrl, {
        cache: 'no-store',
        signal: AbortSignal.timeout(kFeedTimeoutMs),
      });
      if (!response.ok) {
        // a feed that is not there (yet) is not an error worth surfacing:
        // the session's own check still runs
        logger().logDebug(`update feed ${this.feedUrl} answered ${response.status}; deferring to the session check`);
        return { status: 'unsupported' };
      }
      entry = parseReleaseFeed(await response.text());
    } catch (error: unknown) {
      const message = safeError(error).message;
      logger().logDebug(`update check (${manual ? 'manual' : 'automatic'}) failed: ${message}`);
      return { status: 'error', error: message };
    }

    if (!isUpdateAvailable(this.currentVersion, entry)) {
      return { status: 'up-to-date' };
    }

    this.available = entry;
    const canInstall = await this.canInstall();
    return {
      status: 'available',
      'update-version': entry.version,
      'update-url': entry.url,
      'update-message': `RStudio ${entry.version} is now available (you're using ${this.currentVersion})`,
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
    const entry = this.available;
    if (!entry) {
      this.notifyFailed('No update is available to download.');
      return;
    }
    if (this.downloading) {
      return;
    }
    if (this.downloadedVersion === entry.version) {
      this.notifyDownloaded(entry.version);
      return;
    }

    this.downloading = true;
    this.setProgress(2); // > 1 shows an indeterminate dock / taskbar bar
    try {
      if (process.platform === 'darwin') {
        this.downloadWithSquirrel();
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

  private downloadWithSquirrel(): void {
    if (!this.feedUrl) {
      throw new Error('no update feed configured');
    }

    if (!this.macListenersInstalled) {
      this.macListenersInstalled = true;
      autoUpdater.on('update-downloaded', (_event, _notes, releaseName) => {
        this.onDownloaded(this.available?.version ?? releaseName);
      });
      autoUpdater.on('update-not-available', () => {
        this.onDownloadFailed('The update server did not offer an update.');
      });
      autoUpdater.on('error', (error) => {
        this.onDownloadFailed(error.message);
      });
    }

    // the feed is the Squirrel.Mac "default" server format (url, name,
    // notes, pub_date); checkForUpdates() downloads whatever it names, which
    // is why it only runs after our own version comparison
    autoUpdater.setFeedURL({ url: this.feedUrl, serverType: 'default' });
    autoUpdater.checkForUpdates();
  }

  private async downloadWindowsInstaller(entry: ReleaseFeedEntry): Promise<void> {
    const dir = path.join(app.getPath('temp'), 'rstudio-updates');
    await mkdir(dir, { recursive: true });
    const target = path.join(dir, path.basename(new URL(entry.url).pathname) || `RStudio-${entry.version}.exe`);
    await rm(target, { force: true });

    const response = await net.fetch(entry.url, { cache: 'no-store' });
    if (!response.ok || !response.body) {
      throw new Error(`download failed with HTTP ${response.status}`);
    }

    const total = Number(response.headers.get('content-length')) || entry.size;
    let received = 0;
    const hash = createHash('sha256');
    const source = Readable.fromWeb(response.body as import('stream/web').ReadableStream);
    source.on('data', (chunk: Buffer) => {
      hash.update(chunk);
      received += chunk.length;
      if (total > 0) {
        this.setProgress(received / total);
      }
    });
    await pipeline(source, createWriteStream(target));

    const digest = hash.digest('hex');
    if (digest !== entry.sha256) {
      await rm(target, { force: true });
      throw new Error('the downloaded installer did not match the checksum published for this release');
    }
    if (entry.size > 0 && (await stat(target)).size !== entry.size) {
      await rm(target, { force: true });
      throw new Error('the downloaded installer is not the size published for this release');
    }

    this.windowsInstaller = target;
  }

  private onDownloaded(version: string): void {
    this.downloading = false;
    this.downloadedVersion = version;
    this.setProgress(-1);
    logger().logInfo(`RStudio ${version} downloaded and ready to install`);
    this.notifyDownloaded(version);
  }

  private onDownloadFailed(message: string): void {
    this.downloading = false;
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
