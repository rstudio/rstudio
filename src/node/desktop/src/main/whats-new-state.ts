/*
 * whats-new-state.ts
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

import ElectronStore from 'electron-store';

/**
 * A release whose What's New the user has seen. The patch level is written but
 * never read: the file is shared with builds that show What's New once per
 * patch, and those builds treat an entry without a patch as unseen (and cannot
 * repair it), so omitting it would make them show What's New on every launch
 * after a downgrade.
 */
export interface SeenRelease {
  name: string;
  patch: number;
}

interface WhatsNewSchema {
  seenReleases: SeenRelease[];
}

// Workaround for electron-store CommonJS/ESM type mismatch
interface StoreInterface {
  get(key: string, defaultValue?: unknown): unknown;
  set(key: string, value: unknown): void;
}

export class WhatsNewState {
  private store: StoreInterface;

  constructor(cwd?: string) {
    const options: Record<string, unknown> = { name: 'whats-new-state' };
    if (cwd) {
      options.cwd = cwd;
    }
    this.store = new ElectronStore<WhatsNewSchema>(options) as unknown as StoreInterface;
  }

  /**
   * Check if the user has already seen What's New for this release. The
   * content is keyed by release, not by patch level, so a patch of a release
   * the user has already seen does not show it again.
   */
  hasSeenRelease(releaseName: string): boolean {
    return this.findEntry(releaseName) !== undefined;
  }

  /**
   * Record that the user has seen What's New for this release, at the patch
   * level it was seen from (see SeenRelease). An already-seen release is left
   * as it stands.
   */
  markReleaseSeen(releaseName: string, patch: number): void {
    if (this.hasSeenRelease(releaseName)) {
      return;
    }
    const seen = this.seenReleases();
    seen.push({ name: releaseName, patch });
    this.store.set('seenReleases', seen);
  }

  seenReleases(): SeenRelease[] {
    return this.store.get('seenReleases', []) as SeenRelease[];
  }

  private findEntry(releaseName: string): SeenRelease | undefined {
    return this.seenReleases().find((r) => r.name === releaseName);
  }
}
