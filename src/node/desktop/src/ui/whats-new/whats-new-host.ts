/*
 * whats-new-host.ts
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

export {};

const SLUG_PATTERN = /^[a-z0-9-]+$/;

declare global {
  interface Window {
    whatsNew: {
      close: () => void;
      openExternal: (url: string) => void;
    };
  }
}

function isProductionMode(): boolean {
  return window.location.protocol === 'file:';
}

function init(): void {
  const params = new URLSearchParams(window.location.search);
  const release = params.get('release') ?? '';
  const releaseName = params.get('releaseName') ?? '';
  const version = params.get('version') ?? '';

  // Validate slug before constructing any path
  if (!release || !SLUG_PATTERN.test(release)) {
    return;
  }

  const iframe = document.getElementById('release-content') as HTMLIFrameElement | null;

  // In dev mode, webpack dev server injects a hot-reload script into served
  // HTML. Add allow-scripts so the iframe content can load without errors.
  // In production the iframe stays restricted (allow-same-origin only).
  if (!isProductionMode() && iframe) {
    iframe.sandbox.add('allow-scripts');
  }

  // Set header subtitle
  const releaseInfo = document.getElementById('release-info');
  if (releaseInfo) {
    const parts = [releaseName, version].filter((s) => s.length > 0);
    releaseInfo.textContent = parts.join(' \u2014 ');
  }

  // Load release content in iframe via relative path
  if (iframe) {
    iframe.src = `../assets/whats-new/${release}/index.html`;

    // Intercept external link clicks inside the iframe. The sandbox
    // prevents normal navigation handling, so we catch clicks on the
    // iframe's document and route http(s) links through IPC.
    iframe.addEventListener('load', () => {
      const doc = iframe.contentDocument;
      if (!doc) {
        return;
      }
      doc.addEventListener('click', (e: MouseEvent) => {
        const anchor = (e.target as Element).closest('a[href]') as HTMLAnchorElement | null;
        if (!anchor) {
          return;
        }
        // Skip fragment-only and empty links (same-document navigation)
        const raw = anchor.getAttribute('href') ?? '';
        if (!raw || raw.startsWith('#')) {
          return;
        }
        // Use the resolved href property so relative URLs, <base>,
        // and case variations are handled correctly
        const resolved = anchor.href;
        const protocol = new URL(resolved).protocol;
        if (protocol === 'https:' || protocol === 'http:') {
          e.preventDefault();
          window.whatsNew.openExternal(resolved);
        }
      });
    });
  }

  // Wire close button
  const btn = document.getElementById('btn-get-started');
  if (btn) {
    btn.addEventListener('click', () => {
      window.whatsNew.close();
    });
  }

}

document.addEventListener('DOMContentLoaded', init);
