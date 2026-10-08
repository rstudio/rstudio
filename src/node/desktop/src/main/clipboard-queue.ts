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
export function queueClipboardWrite(write: () => void | Promise<void>): void {
  pendingWrites = pendingWrites.then(write).catch((error: unknown) => logger().logError(error));
}

/**
 * Resolves once every write queued so far has finished; never rejects.
 */
export async function clipboardWritesSettled(): Promise<void> {
  return pendingWrites;
}

// The reads below log a failure and treat it as an empty clipboard: the
// renderer reports a rejected IPC call without ever invoking its callback,
// which would leave a paste (or the dialog waiting on it) stuck.

/**
 * Read the text on the given clipboard once pending writes finish.
 */
export async function readClipboardText(source: Electron.Clipboard = clipboard): Promise<string> {
  await pendingWrites;
  try {
    return await source.readText();
  } catch (error: unknown) {
    logger().logError(error);
    return '';
  }
}

/**
 * Read and decode the clipboard entry of the given MIME type once pending
 * writes finish; undefined when there is no such entry or it is empty.
 */
export async function readClipboardType<T>(
  mimeType: string,
  decode: (blob: Blob) => Promise<T>,
): Promise<T | undefined> {
  await pendingWrites;
  try {
    const items = await clipboard.read();
    const item = items.find((candidate) => candidate.types.includes(mimeType));
    if (!item) {
      return undefined;
    }
    // Electron resolves an empty Blob, rather than rejecting, when it can't
    // convert the platform data (e.g. a clipboard bitmap that fails to encode
    // as PNG)
    const blob = (await item.getType(mimeType)) as Blob;
    return blob.size > 0 ? await decode(blob) : undefined;
  } catch (error: unknown) {
    logger().logError(error);
    return undefined;
  }
}
