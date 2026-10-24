import { test, expect, type TestInfo } from '@playwright/test';
import * as http from 'http';
import type { AddressInfo } from 'net';
import { attachRemotePositaiLog, logSince } from '@utils/remote-session-logs';

// Harness self-test for the positai.log attachment on external-server runs:
// trimming the log to a test's entries, and fetching it through /files/. A
// local HTTP server stands in for the session; no IDE.

const LOG = [
  '2026-10-23T23:28:58.100Z [rstudio:INFO] [positai] Fetched 12 models from API',
  '2026-10-23T23:28:59.900Z [rstudio:ERROR] request failed:',
  '    at fetchModels (models.js:10)',
  '2026-10-23T23:29:00.000Z [rstudio:INFO] Discovered 13 skills',
  '2026-10-23T23:29:01.500Z [rstudio:WARN] [positai] model list request failed: HTTP 503',
  '    retrying in 1000ms',
  '',
].join('\n');

const TEST_START = new Date('2026-10-23T23:29:00.000Z');

test('keeps the entries stamped since the test started', () => {
  expect(logSince(LOG, TEST_START)).toBe([
    '2026-10-23T23:29:00.000Z [rstudio:INFO] Discovered 13 skills',
    '2026-10-23T23:29:01.500Z [rstudio:WARN] [positai] model list request failed: HTTP 503',
    '    retrying in 1000ms',
    '',
  ].join('\n'));
});

test('keeps unstamped lines with the entry they continue', () => {
  const since = new Date('2026-10-23T23:28:59.000Z');
  expect(logSince(LOG, since).split('\n').slice(0, 2)).toEqual([
    '2026-10-23T23:28:59.900Z [rstudio:ERROR] request failed:',
    '    at fetchModels (models.js:10)',
  ]);
});

test('returns nothing when no entry is recent enough', () => {
  expect(logSince(LOG, new Date('2026-10-24T00:00:00.000Z'))).toBe('');
  expect(logSince('', TEST_START)).toBe('');
  expect(logSince('no timestamps here\nat all', TEST_START)).toBe('');
});

/** Serve `status` and `body` for every request; returns the server's URL and the paths it was asked for. */
async function serveFiles(status: number, body: string): Promise<{ url: string; paths: string[]; close: () => Promise<void> }> {
  const paths: string[] = [];
  const server = http.createServer((req, res) => {
    paths.push(req.url ?? '');
    res.writeHead(status, { 'Content-Type': 'text/plain' });
    res.end(body);
  });
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  const { port } = server.address() as AddressInfo;
  return {
    url: `http://127.0.0.1:${port}`,
    paths,
    close: () => new Promise<void>((resolve) => server.close(() => resolve())),
  };
}

function recordingTestInfo(): { testInfo: TestInfo; attached: Map<string, string> } {
  const attached = new Map<string, string>();
  const testInfo = {
    attach: async (name: string, options: { body?: string | Buffer }) => {
      attached.set(name, String(options.body));
    },
  } as unknown as TestInfo;
  return { testInfo, attached };
}

test('attaches the recent part of the log the session serves', async ({ page }) => {
  const server = await serveFiles(200, LOG);
  try {
    const { testInfo, attached } = recordingTestInfo();
    await attachRemotePositaiLog(page, testInfo, server.url, TEST_START);

    expect(server.paths).toEqual(['/files/.local/share/rstudio/log/positai.log']);
    expect(attached.get('positai.log')).toBe(logSince(LOG, TEST_START));
  } finally {
    await server.close();
  }
});

test('fetches the log under the path prefix the server is served from', async ({ page }) => {
  const server = await serveFiles(200, LOG);
  try {
    const { testInfo, attached } = recordingTestInfo();
    await attachRemotePositaiLog(page, testInfo, `${server.url}/rstudio/`, TEST_START);

    expect(server.paths).toEqual(['/rstudio/files/.local/share/rstudio/log/positai.log']);
    expect(attached.has('positai.log')).toBe(true);
  } finally {
    await server.close();
  }
});

test('attaches nothing when the session has no log or fails to serve it', async ({ page }) => {
  for (const status of [404, 500]) {
    const server = await serveFiles(status, 'not a log');
    try {
      const { testInfo, attached } = recordingTestInfo();
      await attachRemotePositaiLog(page, testInfo, server.url, TEST_START);
      expect(attached.size, `HTTP ${status}`).toBe(0);
    } finally {
      await server.close();
    }
  }
});

test('attaches nothing when the session cannot be reached', async ({ page }) => {
  const server = await serveFiles(200, LOG);
  const url = server.url;
  await server.close();

  const { testInfo, attached } = recordingTestInfo();
  await attachRemotePositaiLog(page, testInfo, url, TEST_START);
  expect(attached.size).toBe(0);
});
