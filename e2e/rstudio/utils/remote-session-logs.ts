import type { Page, TestInfo } from '@playwright/test';

/**
 * Posit Assistant's backend log on an external server, relative to the
 * account's home as the /files/ endpoint takes it. Assumes the default data
 * home (~/.local/share/rstudio), as remote-provision.ts assumes the default
 * credential store locations.
 */
const REMOTE_POSITAI_LOG = '.local/share/rstudio/log/positai.log';

const FETCH_TIMEOUT_MS = 10000;

// Each entry in the backend's log begins with an ISO-8601 UTC timestamp.
const LOG_TIMESTAMP = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z)\s/;

/**
 * The part of a timestamped log written at or after `since`: everything from
 * the first line stamped no earlier than it. Unstamped lines continue the
 * entry above them, so they go with it. Returns '' when no entry qualifies.
 */
export function logSince(log: string, since: Date): string {
  const lines = log.split('\n');
  const start = lines.findIndex((line) => {
    const match = LOG_TIMESTAMP.exec(line);
    return match !== null && Date.parse(match[1]) >= since.getTime();
  });
  return start === -1 ? '' : lines.slice(start).join('\n');
}

/**
 * Attach what an external server's positai.log gained since `since`, read
 * through the session's /files/ endpoint: session logs are attached only where
 * the harness can read the log directory itself (Desktop). The account's
 * sessions share the log, so other workers' entries can be interleaved. A
 * failure here is reported and dropped, so it can't mask the test's own.
 */
export async function attachRemotePositaiLog(
  page: Page,
  testInfo: TestInfo,
  serverUrl: string,
  since: Date,
): Promise<void> {
  // /files/ sits at the server's root, under any path prefix it is served from.
  const url = `${serverUrl.replace(/\/+$/, '')}/files/${REMOTE_POSITAI_LOG}`;
  let log: string;
  try {
    const response = await page.request.get(url, {
      headers: { 'Sec-Fetch-Site': 'same-origin' },
      failOnStatusCode: false,
      timeout: FETCH_TIMEOUT_MS,
    });
    // No log: the backend hasn't run for this account (or it uses another data home).
    if (response.status() === 404)
      return;
    if (!response.ok()) {
      console.warn(`[positai.log] not attached: GET ${url} returned HTTP ${response.status()}`);
      return;
    }
    log = await response.text();
  } catch (err) {
    console.warn(`[positai.log] not attached: GET ${url} failed: ${err}`);
    return;
  }

  const slice = logSince(log, since);
  if (slice.trim() !== '')
    await testInfo.attach('positai.log', { body: slice, contentType: 'text/plain' });
}
