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
   * Check if the user has already seen What's New for this release at
   * this patch level or higher. Returns true if the stored patch >= the
   * given patch (covers same version and downgrades).
   */
  hasSeenRelease(releaseName: string, patch: number): boolean {
    const entry = this.findEntry(releaseName);
    return entry !== undefined && entry.patch >= patch;
  }

  /**
   * Record that the user has seen What's New for this release at the
   * given patch level. Updates the stored patch if the new one is higher.
   */
  markReleaseSeen(releaseName: string, patch: number): void {
    const seen = this.seenReleases();
    const existing = seen.find((r) => r.name === releaseName);
    if (existing) {
      if (patch > existing.patch) {
        existing.patch = patch;
        this.store.set('seenReleases', seen);
      }
    } else {
      seen.push({ name: releaseName, patch });
      this.store.set('seenReleases', seen);
    }
  }

  seenReleases(): SeenRelease[] {
    return this.store.get('seenReleases', []) as SeenRelease[];
  }

  private findEntry(releaseName: string): SeenRelease | undefined {
    return this.seenReleases().find((r) => r.name === releaseName);
  }
}
