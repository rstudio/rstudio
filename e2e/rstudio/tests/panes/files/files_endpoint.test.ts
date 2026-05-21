// /files/ HTTP endpoint cross-site protections (rstudio-pro#10980).
//
// The /files/ endpoint serves user-controlled files with native MIME types
// and must reject cross-site requests so attacker pages can't redirect
// victims into loading attacker HTML in the RStudio session origin.
//
// Server-only: /files/ is registered only in kSessionProgramModeServer (see
// handleFilesRequest in SessionFiles.cpp). The server fixture spawns
// rserver-dev with --auth-none=1 and no --www-enable-origin-check=1, so
// every 400 below is produced by the /files/ handler itself -- the same
// situation as a default open-source deployment.

import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { sleep, TIMEOUTS } from '@utils/constants';

const TEST_FILE = '.rstudio-test-files-endpoint';

test.describe('/files/ HTTP endpoint cross-site protections', { tag: ['@server_only'] }, () => {
  let consoleActions: ConsolePaneActions;
  let serverOrigin: string;
  let filesUrl: string;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    // After login the page URL has a session prefix (e.g. /s/<id>/...); the
    // /files/ endpoint is at the server root.
    serverOrigin = new URL(page.url()).origin;
    filesUrl = `${serverOrigin}/files/${TEST_FILE}`;

    // Drop a small file in the user home directory so the handler reaches
    // the Sec-Fetch-Site check before any not-found logic. The leading '.'
    // keeps it out of the visible Files pane.
    await consoleActions.typeInConsole(
      `writeLines('test content', file.path('~', '${TEST_FILE}'))`,
    );
    await sleep(TIMEOUTS.settleDelay);
  });

  test.afterAll(async () => {
    await consoleActions.typeInConsole(
      `unlink(file.path('~', '${TEST_FILE}'))`,
    );
  });

  test('Sec-Fetch-Site and Referer are enforced', async ({ rstudioPage: page }) => {
    const get = (headers: Record<string, string> = {}) =>
      page.request.get(filesUrl, { headers, failOnStatusCode: false });

    // Warm up: the very first request against a fresh session can go through
    // session-establishment scaffolding before the /files/ handler runs, so
    // its status doesn't reflect the cross-site policy. Make a throwaway
    // request first so subsequent assertions exercise the handler.
    await get({ 'Sec-Fetch-Site': 'same-origin' });

    // Primary check: Sec-Fetch-Site == "cross-site" is the explicit attacker
    // signal and must be rejected.
    expect((await get({ 'Sec-Fetch-Site': 'cross-site' })).status()).toBe(400);

    // Trusted Sec-Fetch-Site values are allowed. "none" covers user-typed
    // URLs and bookmarks; "same-origin" covers in-RStudio navigation;
    // "same-site" is allowed deliberately so Posit Workbench front-ends
    // iframe-embedding RStudio across sibling subdomains keep functioning.
    const sameOriginRes = await get({ 'Sec-Fetch-Site': 'same-origin' });
    expect(sameOriginRes.status()).toBe(200);
    // Tie 200 to actually serving the file, so a regression that returns 200
    // with the wrong contents (e.g. an empty body) doesn't go unnoticed.
    expect((await sameOriginRes.text()).trim()).toBe('test content');
    expect((await get({ 'Sec-Fetch-Site': 'same-site' })).status()).toBe(200);
    expect((await get({ 'Sec-Fetch-Site': 'none' })).status()).toBe(200);

    // Sec-Fetch-Site is case-sensitive (browsers send lowercase tokens). A
    // capitalized value isn't on the allow-list, so it must be treated as
    // cross-site. Pin this so a future move to case-insensitive matching
    // is a deliberate decision.
    expect((await get({ 'Sec-Fetch-Site': 'Cross-Site' })).status()).toBe(400);

    // Unknown / future Sec-Fetch-Site values default to deny.
    expect((await get({ 'Sec-Fetch-Site': 'garbage' })).status()).toBe(400);

    // Older browsers don't send Sec-Fetch-Site. The handler's fallback then
    // enforces a same-origin Referer when one is present.
    const sameOriginReferer = `${serverOrigin}/`;
    const crossOriginReferer = 'https://attacker.example/';

    // Sec-Fetch-Site absent + same-origin Referer -> allowed.
    expect((await get({ Referer: sameOriginReferer })).status()).toBe(200);

    // Sec-Fetch-Site absent + cross-origin Referer -> rejected.
    expect((await get({ Referer: crossOriginReferer })).status()).toBe(400);

    // Sec-Fetch-Site absent + Referer absent -> allowed. Preserves direct
    // URL navigation in older browsers; the residual attack window
    // (older browser + attacker-strips-referer) is the documented trade-off.
    expect((await get()).status()).toBe(200);

    // Edge case: same host:port but different scheme is NOT same-origin.
    // The handler compares scheme + host:port to canonicalize away from
    // bare-host-equality bugs.
    const crossSchemeReferer = serverOrigin.replace(/^http:/, 'https:') + '/';
    expect((await get({ Referer: crossSchemeReferer })).status()).toBe(400);

    // Edge case: same scheme + same host but different port -> rejected.
    const url = new URL(serverOrigin);
    const otherPort = url.port === '9999' ? '9998' : '9999';
    const crossPortReferer = `${url.protocol}//${url.hostname}:${otherPort}/`;
    expect((await get({ Referer: crossPortReferer })).status()).toBe(400);

    // Malformed Referer values fail closed: the URL parser rejects anything
    // outside http/https/file/ftp(s), so these produce empty protocol/host.
    // The handler treats that as a parse failure and rejects rather than
    // collapsing both sides to empty and matching.
    expect((await get({ Referer: 'javascript:alert(1)' })).status()).toBe(400);
    expect((await get({ Referer: 'about:blank' })).status()).toBe(400);
    expect((await get({ Referer: 'not a url' })).status()).toBe(400);
  });
});
