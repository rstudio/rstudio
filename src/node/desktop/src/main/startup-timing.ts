/*
 * startup-timing.ts
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

import { WebContents } from 'electron';
import { appendFileSync } from 'fs';
import { performance } from 'perf_hooks';
import { getenv } from '../core/environment';
import { logger } from '../core/logger';

// Startup checkpoints are recorded only when RSTUDIO_STARTUP_TIMING names a
// file. Each checkpoint is one JSON line:
//
//    {"tier":"desktop","name":"<name>","t":<epoch ms>,"pid":<pid>}
//
// with an optional "dur" (ms) for completed spans. The rsession process
// inherits the variable and appends its own "session" checkpoints, and the
// renderer's performance timeline is harvested into "client" checkpoints
// once the workbench is up, so the file holds a single cross-process
// timeline. See tasks/startup-timing.ts for the report.

interface Checkpoint {
  tier: 'desktop' | 'client';
  name: string;
  t: number;
  pid?: number;
  dur?: number;
}

// how long to wait after the workbench initializes before harvesting the
// renderer timeline; long enough to include the deferred-init tail
// the harvest waits for the client's deferred-init-completed mark (the last
// startup milestone), polling at this interval and giving up at the timeout
// so a stalled session cannot postpone the harvest forever
const kHarvestPollIntervalMs = 500;
const kHarvestTimeoutMs = 30000;

// once the final mark is seen, a short settle lets trailing entries (late
// resource timings) be recorded before the timeline is copied
const kHarvestSettleMs = 1000;

// looked up on every checkpoint rather than cached: there are only a handful
// of checkpoints, and this keeps the module trivially testable
function resolveTimingFile(): string | null {
  const value = getenv('RSTUDIO_STARTUP_TIMING');
  return value.length > 0 ? value : null;
}

function nowMs(): number {
  return performance.timeOrigin + performance.now();
}

function write(checkpoint: Checkpoint): void {
  const file = resolveTimingFile();
  if (!file) {
    return;
  }

  try {
    appendFileSync(file, JSON.stringify(checkpoint) + '\n');
  } catch (error: unknown) {
    logger().logError(error);
  }
}

export function startupTimingEnabled(): boolean {
  return resolveTimingFile() !== null;
}

/**
 * Records a named desktop startup checkpoint at the current time.
 */
export function startupCheckpoint(name: string): void {
  if (!startupTimingEnabled()) {
    return;
  }
  write({ tier: 'desktop', name: name, t: nowMs(), pid: process.pid });
}

/**
 * Records the Electron process creation time, which precedes any JavaScript
 * we run; the gap to the first checkpoint is the runtime's own boot cost.
 */
export function recordProcessStart(): void {
  if (!startupTimingEnabled()) {
    return;
  }

  const created = process.getCreationTime();
  if (created !== null) {
    write({ tier: 'desktop', name: 'process-start', t: created, pid: process.pid });
  }
}

interface RendererEntry {
  name: string;
  t: number;
  dur?: number;
}

/**
 * Copies the renderer's navigation timing, resource timing and
 * `performance.mark()` entries into the timing file as "client" checkpoints.
 * The GWT client records marks prefixed "rstudio:" at its own milestones; the
 * harvest runs once the last of them (deferred-init-completed) has been
 * recorded, so slow launches are captured in full rather than truncated at a
 * fixed delay.
 */
export function harvestRendererTiming(webContents: WebContents): void {
  if (!startupTimingEnabled()) {
    return;
  }

  const kProbeScript = String.raw`
    performance.getEntriesByName('rstudio:deferred-init-completed', 'mark').length > 0`;

  const deadline = Date.now() + kHarvestTimeoutMs;
  const poll = () => {
    webContents
      .executeJavaScript(kProbeScript)
      .then((completed: boolean) => {
        if (completed) {
          setTimeout(() => harvestNow(webContents, true), kHarvestSettleMs);
        } else if (Date.now() >= deadline) {
          // harvest what exists, but mark the timeline as incomplete so
          // consumers do not mistake a stalled startup for a finished one
          setTimeout(() => harvestNow(webContents, false), kHarvestSettleMs);
        } else {
          setTimeout(poll, kHarvestPollIntervalMs);
        }
      })
      .catch((error: unknown) => logger().logError(error));
  };
  setTimeout(poll, kHarvestPollIntervalMs);
}

function harvestNow(webContents: WebContents, completed: boolean): void {
  // performance.timeOrigin is epoch-based, so everything can be converted to
  // the same wall-clock scale used by the other tiers
  const script = String.raw`
    (function() {
      var origin = performance.timeOrigin;
      var entries = [];
      var nav = performance.getEntriesByType('navigation')[0];
      if (nav) {
        entries.push({ name: 'navigation-start', t: origin + nav.startTime });
        entries.push({ name: 'response-start', t: origin + nav.responseStart });
        entries.push({ name: 'dom-interactive', t: origin + nav.domInteractive });
        entries.push({ name: 'dom-content-loaded', t: origin + nav.domContentLoadedEventEnd });
        entries.push({ name: 'load', t: origin + nav.loadEventEnd });
      }
      performance.getEntriesByType('resource').forEach(function(e) {
        var name = e.name.replace(/^https?:\/\/[^\/]+/, '');
        entries.push({ name: 'resource:' + name, t: origin + e.startTime, dur: e.duration });
      });
      performance.getEntriesByType('mark').forEach(function(e) {
        if (e.name.indexOf('rstudio:') === 0) {
          entries.push({ name: e.name.substring('rstudio:'.length), t: origin + e.startTime });
        }
      });
      return entries;
    })()`;

  webContents
    .executeJavaScript(script)
    .then((entries: RendererEntry[]) => {
      for (const entry of entries) {
        write({ tier: 'client', name: entry.name, t: entry.t, dur: entry.dur });
      }
      const name = completed ? 'timing-harvested' : 'timing-harvest-timeout';
      write({ tier: 'desktop', name, t: nowMs(), pid: process.pid });
    })
    .catch((error: unknown) => logger().logError(error));
}
