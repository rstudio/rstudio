// Buffered request limits and streamed Files pane uploads.
// The tests skip unless PW_REQUEST_BODY_LIMIT_MB is set: rserver's default
// leaves bodies unlimited. An in-tree spawned server is started with that
// limit; an external server must have www-max-request-size-mb set to match.

import { once } from 'node:events';
import * as fs from 'node:fs';
import * as http from 'node:http';
import * as https from 'node:https';

import { test, expect } from '@fixtures/rstudio.fixture';
import { externalServerUrl, requestBodyLimitMb } from '@fixtures/server.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { CONFIRM_BTN } from '@pages/modals.page';
import { executeCommand } from '@utils/commands';
import { rPathLiteral } from '@utils/r';
import { useSuiteSandbox } from '@utils/sandbox';

const MB = 1024 * 1024;

function maxRequestSizeBytes(): number {
  return requestBodyLimitMb() * MB;
}

// Stream the body from a small reusable buffer and stop on an early response.
async function postBody(url: URL, size: number): Promise<number> {
  const client = url.protocol === 'https:' ? https : http;
  const req = client.request(url, {
    method: 'POST',
    headers: { 'Content-Type': 'text/plain', 'Content-Length': size },
  });
  req.setTimeout(30_000, () => req.destroy(new Error('Request timed out')));

  let responded = false;
  const response = new Promise<number>((resolve, reject) => {
    req.on('response', (res) => {
      responded = true;
      res.resume();
      resolve(res.statusCode ?? 0);
    });
    req.on('error', (error) => {
      if (!responded)
        reject(error);
    });
  });

  try {
    const chunk = Buffer.alloc(MB, 'x');
    let remaining = size;
    while (remaining > 0 && !responded) {
      const n = Math.min(remaining, chunk.length);
      const flushed = req.write(n === chunk.length ? chunk : chunk.subarray(0, n));
      remaining -= n;
      if (!flushed)
        await Promise.race([once(req, 'drain'), response]);
    }
    if (!responded)
      req.end();
    return await response;
  } finally {
    req.destroy();
  }
}

test.describe('Request body limit', { tag: ['@server_only'] }, () => {
  test.skip(requestBodyLimitMb() === 0, 'set PW_REQUEST_BODY_LIMIT_MB to run with a limit');

  const sandbox = useSuiteSandbox();

  test('buffered body over the limit receives 413', async ({ rstudioPage: page }) => {
    test.setTimeout(180_000);
    const maxBytes = maxRequestSizeBytes();

    // Keep any reverse-proxy prefix configured for an external server.
    const baseUrl = externalServerUrl() ?? new URL(page.url()).origin;
    const url = new URL(`${baseUrl.replace(/\/$/, '')}/rpc/client_init`);
    const smallStatus = await postBody(url, 1024);
    expect(smallStatus).toBeGreaterThan(0);
    expect(smallStatus).not.toBe(413);
    expect(await postBody(url, maxBytes + 1)).toBe(413);
  });

  test('Files pane upload larger than the buffered limit succeeds', async (
    { rstudioPage: page }, testInfo,
  ) => {
    test.setTimeout(300_000);
    const maxBytes = maxRequestSizeBytes();

    const fileName = 'large_upload.bin';
    const fileSize = maxBytes + MB;
    const filePath = testInfo.outputPath(fileName);
    fs.writeFileSync(filePath, '');
    fs.truncateSync(filePath, fileSize);

    const consoleActions = new ConsolePaneActions(page);
    const uploadedPath = rPathLiteral(`${sandbox.dir}/${fileName}`);
    try {
      await consoleActions.executeInConsole(
        `writeLines('ready', file.path(${rPathLiteral(sandbox.dir)}, 'upload_ready.txt')); ` +
        `.rs.api.filesPaneNavigate(${rPathLiteral(sandbox.dir)})`,
        { wait: true },
      );
      await executeCommand(page, 'activateFiles');
      const filesPanel = page.locator('#rstudio_workbench_panel_files');
      await expect(filesPanel.locator('div[title="upload_ready.txt"]')).toBeVisible();
      await executeCommand(page, 'uploadFile');
      const dialog = page.getByRole('dialog', { name: 'Upload Files', exact: true });
      await expect(dialog).toBeVisible();
      await dialog.locator('input[type="file"]').setInputFiles(filePath);
      await dialog.locator(CONFIRM_BTN).click();

      await expect(filesPanel.locator(`div[title="${fileName}"]`)).toBeVisible({ timeout: 180_000 });
      const marker = `__UPLOADED_SIZE_${Date.now()}__`;
      await consoleActions.executeInConsole(
        `cat('${marker}', sprintf('%.0f', file.size(${uploadedPath})), '${marker}\\n')`,
        { wait: true },
      );
      await expect(consoleActions.consolePane.consoleOutput).toContainText(
        `${marker} ${fileSize} ${marker}`,
      );
      await consoleActions.executeInConsole(`unlink(${uploadedPath})`, { wait: true });
    } finally {
      fs.unlinkSync(filePath);
    }
  });
});
