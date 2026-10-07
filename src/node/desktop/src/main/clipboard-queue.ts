/*
 * clipboard-queue.ts
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

import { clipboard } from 'electron';
import { logger } from '../core/logger';

// Electron 44 clipboard writes are async and callers fire them without
// waiting, so reads chain on the last write to avoid returning stale content
// (e.g. an Emacs kill followed immediately by a yank).
let pendingWrites: Promise<void> = Promise.resolve();

/**
 * Write to the clipboard after any pending write, so that reads and pastes
 * waiting on clipboardWritesSettled() see it.
 */
export function queueClipboardWrite(write: () => Promise<void>): void {
  pendingWrites = pendingWrites.then(write).catch((error: unknown) => logger().logError(error));
}

/**
 * Resolves once every write queued so far has finished; never rejects.
 */
export async function clipboardWritesSettled(): Promise<void> {
  return pendingWrites;
}

/**
 * Read the clipboard entry of the given MIME type once pending writes finish.
 *
 * A failed read is logged and treated as an empty clipboard: the renderer
 * reports a rejected IPC call without ever invoking its callback, which
 * would leave a visual-editor paste waiting forever.
 */
export async function readClipboardType(mimeType: string): Promise<Blob | undefined> {
  await pendingWrites;
  try {
    const items = await clipboard.read();
    const item = items.find((candidate) => candidate.types.includes(mimeType));
    return item ? ((await item.getType(mimeType)) as Blob) : undefined;
  } catch (error: unknown) {
    logger().logError(error);
    return undefined;
  }
}
