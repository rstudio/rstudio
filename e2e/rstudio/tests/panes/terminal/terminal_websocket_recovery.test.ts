import type { Page, Request, Route } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { clearPref, executeCommand, setPref } from '@utils/commands';
import { TIMEOUTS } from '@utils/constants';
import { rStringLiteral } from '@utils/r';
import {
  captureResult,
  focusTerminal,
  killAllTerminals,
  openTerminal,
  retryTerminalInput,
} from '@utils/terminal';

// Three unanswered probes, each given another 10s interval to arrive, take
// up to 40s with the default session settings. Exercise the real timers.
const RECOVERY_TIMEOUT = 60_000;
const RPC_FALLBACK = '**/rpc/process_use_rpc';

type TerminalFault = { handle: string; stalled: boolean; droppedReplies: number };

async function installTerminalInterceptor(page: Page) {
  let pending: TerminalFault | undefined;
  await page.routeWebSocket(
    url => pending?.handle === '' && /\/terminal\/[^/]+\/$/.test(url.pathname),
    socket => {
      // Only the next terminal is affected. Playwright has no WebSocket
      // unroute API, so match only while a test has armed an empty handle;
      // other terminals/specs sharing this page pass through.
      const fault = pending!;
      fault.handle = new URL(socket.url()).pathname.split('/').at(-2)!;
      const server = socket.connectToServer();
      server.onMessage(message => {
        // Leave the real connection open and continue forwarding ordinary
        // output. A browser close/error event must not trigger the recovery.
        if (fault.stalled && message === 'b') {
          fault.droppedReplies++;
        } else {
          socket.send(message);
        }
      });
    },
  );
  // GWT creates sockets in its own iframe. Reload after installing the
  // route so Playwright's WebSocket init script runs in that frame too.
  await page.reload();
  await page.waitForFunction(() => window.rstudio?.ready === true, null, {
    timeout: TIMEOUTS.rstudioStartup,
  });
  return () => {
    const fault: TerminalFault = { handle: '', stalled: false, droppedReplies: 0 };
    pending = fault;
    return {
      fault,
      dispose: () => { pending = undefined; fault.stalled = false; },
    };
  };
}

async function renderedText(page: Page): Promise<string> {
  // Server-side terminalBuffer still grows when the client is broken. Read
  // the actual DOM renderer and normalize xterm's arbitrary hard wrapping.
  return (await page.locator('.xterm:visible .xterm-rows').innerText())
    .replace(/[\s\u00B7]+/g, '');
}

async function expectOutput(page: Page, marker: string): Promise<void> {
  await expect.poll(() => renderedText(page), { timeout: TIMEOUTS.consoleReady }).toContain(marker);
}

async function typeCommand(page: Page, command: string): Promise<void> {
  await page.keyboard.type(command);
  await page.keyboard.press('Enter');
}

async function warmUpShell(page: Page, marker = 'ready'): Promise<void> {
  // Retry only setup: ConPTY can lose input during the initial terminal resize.
  // Recovery and queued-input probes in the faulted terminal are sent once.
  await retryTerminalInput(
    page,
    () => typeCommand(page, `pw_n=0; echo "pw_""${marker}"`),
    () => expectOutput(page, `pw_${marker}`),
  );
}

async function shellPid(page: Page, handle: string): Promise<string> {
  return captureResult(page, `rstudioapi::terminalContext(${rStringLiteral(handle)})$pid`);
}

function isTerminalRpc(request: Request, method: string, handle: string): boolean {
  return request.url().endsWith(`/rpc/${method}`) && request.postDataJSON()?.params?.[0] === handle;
}

test.describe.serial('Terminal WebSocket recovery', () => {
  let interceptNextTerminal: Awaited<ReturnType<typeof installTerminalInterceptor>>;

  test.beforeAll(async ({ rstudioPage: page }) => {
    // Install once, before either test creates a terminal. A separate page
    // reload for each fault would add an unrelated session-rejoin scenario.
    interceptNextTerminal = await installTerminalInterceptor(page);
  });

  test.beforeEach(async ({ rstudioPage: page }) => {
    await killAllTerminals(page);
    await setPref(page, 'terminal_websockets', true);
    await setPref(page, 'terminal_renderer', 'dom');
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await killAllTerminals(page);
    await clearPref(page, 'terminal_websockets');
    await clearPref(page, 'terminal_renderer');
    await executeCommand(page, 'activateConsole');
  });

  test('missing keepalive replies recover without replacing the shell or disturbing another terminal', async ({
    rstudioPage: page,
  }) => {
    const { fault, dispose } = interceptNextTerminal();
    const fallbacks: string[] = [];
    const onRequest = (request: Request) => {
      if (request.url().endsWith('/rpc/process_use_rpc'))
        fallbacks.push(request.postDataJSON().params[0]);
    };
    page.on('request', onRequest);
    try {
      await openTerminal(page);
      await warmUpShell(page);
      expect(fault.handle, 'terminal must initially use a real WebSocket').not.toBe('');
      const pid = await shellPid(page, fault.handle);
      expect(Number(pid)).toBeGreaterThan(0);

      await executeCommand(page, 'newTerminal');
      await expect(page.locator('.xterm')).toHaveCount(2, { timeout: TIMEOUTS.consoleReady });
      await focusTerminal(page);
      await warmUpShell(page);
      await page.locator('#rstudio_tb_previousterminal').click();
      await focusTerminal(page);
      await warmUpShell(page, 'resumed_ready');
      await typeCommand(page, 'echo "pw_history_""retained"');
      await expectOutput(page, 'pw_history_retained');

      const fallback = page.waitForResponse(
        response => isTerminalRpc(response.request(), 'process_use_rpc', fault.handle),
        { timeout: RECOVERY_TIMEOUT },
      );
      fault.stalled = true;
      const response = await fallback;
      expect(response.ok()).toBe(true);
      expect(await response.json()).not.toHaveProperty('error');
      expect(fault.droppedReplies).toBeGreaterThanOrEqual(3);

      await typeCommand(page, 'echo "pw_rpc_""works"');
      await expectOutput(page, 'pw_rpc_works');
      await expectOutput(page, 'pw_history_retained');
      expect(await shellPid(page, fault.handle), 'fallback must preserve the shell').toBe(pid);

      await focusTerminal(page);
      await page.locator('#rstudio_tb_nextterminal').click();
      await focusTerminal(page);
      // Switching back re-fits this healthy terminal too. Settle its resize
      // with a fresh setup marker before the one-shot control probe.
      await warmUpShell(page, 'other_ready');
      await typeCommand(page, 'echo "pw_other_""works"');
      await expectOutput(page, 'pw_other_works');
      expect(fallbacks, 'only the stalled terminal should switch to RPC').toEqual([fault.handle]);
    } finally {
      dispose();
      page.off('request', onRequest);
    }
  });

  test('input typed while RPC fallback is pending arrives once and in order', async ({ rstudioPage: page }) => {
    const { fault, dispose } = interceptNextTerminal();
    let release!: () => void;
    const released = new Promise<void>(resolve => { release = resolve; });
    let fallbackHeld = false;
    let fallbackReleased = false;
    const writes: Array<{ text: string; afterRelease: boolean }> = [];
    const onRequest = (request: Request) => {
      if (isTerminalRpc(request, 'process_write_stdin', fault.handle)) {
        writes.push({ text: request.postDataJSON().params[1].text, afterRelease: fallbackReleased });
      }
    };
    const holdFallback = async (route: Route) => {
      if (!isTerminalRpc(route.request(), 'process_use_rpc', fault.handle)) {
        await route.continue();
        return;
      }
      // Hold the channel switch in flight so keyboard input must remain in
      // the client's pending queue. Continue the original browser request:
      // route.fetch() bypasses Electron's session-authentication header.
      fallbackHeld = true;
      await released;
      await route.continue();
    };
    await page.route(RPC_FALLBACK, holdFallback);
    page.on('request', onRequest);
    try {
      await openTerminal(page);
      await warmUpShell(page);
      expect(fault.handle, 'terminal must initially use a real WebSocket').not.toBe('');
      const pid = await shellPid(page, fault.handle);
      expect(Number(pid)).toBeGreaterThan(0);
      await focusTerminal(page);
      await warmUpShell(page, 'counter_ready');

      // This command is accepted over the WebSocket before it fails. If it
      // is replayed on RPC, the counter below will be 3 instead of 2.
      await typeCommand(page, 'pw_n=$((pw_n+1)); echo "pw_before_$pw_n"');
      await expectOutput(page, 'pw_before_1');
      fault.stalled = true;
      await expect.poll(() => fallbackHeld, { timeout: RECOVERY_TIMEOUT }).toBe(true);
      expect(fault.droppedReplies).toBeGreaterThanOrEqual(3);

      const queued = 'pw_n=$((pw_n+1)); echo "pw_queued_$pw_n"';
      await typeCommand(page, queued);
      const fallback = page.waitForResponse(
        response => isTerminalRpc(response.request(), 'process_use_rpc', fault.handle),
        { timeout: TIMEOUTS.consoleReady },
      );
      fallbackReleased = true;
      release();
      const response = await fallback;
      expect(response.ok()).toBe(true);
      expect(await response.json()).not.toHaveProperty('error');
      await expectOutput(page, 'pw_queued_2');

      // A later command is a shell/output barrier for the ordering and
      // duplication assertions, without sleeping or retrying lost input.
      const barrier = 'echo "pw_final_$pw_n"';
      await typeCommand(page, barrier);
      await expectOutput(page, 'pw_final_2');
      expect(writes.every(write => write.afterRelease), 'input must stay queued while fallback is held').toBe(true);
      expect(writes.map(write => write.text).join('')).toBe(`${queued}\r${barrier}\r`);
      expect((await renderedText(page)).split('pw_queued_2')).toHaveLength(2);
      expect(await shellPid(page, fault.handle)).toBe(pid);
    } finally {
      dispose();
      release();
      await page.unroute(RPC_FALLBACK, holdFallback);
      page.off('request', onRequest);
    }
  });
});
