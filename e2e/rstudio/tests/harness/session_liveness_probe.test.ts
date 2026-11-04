import { test, expect } from '@playwright/test';
import { DeadlineError } from '@utils/deadline';
import { DeafSessionError, probeSessionLiveness } from '@utils/session-liveness';

// Exercise the liveness probe against a controlled bridge. What matters is
// the classification, since it decides whether the fixture takes a stack
// dump: a ping that never settles is a deaf session; a ping answered with an
// error is a transport or transition hiccup, retried and then surfaced with
// its detail; a page that cannot even answer the bridge check is a wedge,
// not a verdict on the session. Short deadlines keep the whole file quick.

type Bridge = typeof window.rstudio;

test.beforeEach(async ({ page }) => {
  await page.setContent('<div id="root"></div>');
});

test('a bridge without ping is unsupported, not a failure', async ({ page }) => {
  await page.evaluate(() => {
    window.rstudio = { ready: true } as Bridge;
  });
  expect(await probeSessionLiveness(page, 500)).toBe('unsupported');
});

test('an answered ping is alive', async ({ page }) => {
  await page.evaluate(() => {
    window.rstudio = { session: { ping: () => Promise.resolve() } } as Bridge;
  });
  expect(await probeSessionLiveness(page, 500)).toBe('alive');
});

test('a ping that never settles is a deaf session', async ({ page }) => {
  await page.evaluate(() => {
    window.rstudio = { session: { ping: () => new Promise<void>(() => {}) } } as Bridge;
  });
  const err = await probeSessionLiveness(page, 300).catch((e: unknown) => e);
  expect(err).toBeInstanceOf(DeafSessionError);
  expect(err).toBeInstanceOf(DeadlineError);
});

test('a ping answered with an error is retried, and the answer that follows wins', async ({ page }) => {
  await page.evaluate(() => {
    let calls = 0;
    (window as unknown as { pingCalls: () => number }).pingCalls = () => calls;
    window.rstudio = {
      session: {
        ping: () => {
          calls++;
          return calls < 3
            ? Promise.reject(new Error('ping RPC failed -- connection_refused'))
            : Promise.resolve();
        },
      },
    } as Bridge;
  });
  expect(await probeSessionLiveness(page, 2000)).toBe('alive');
  expect(await page.evaluate(() => (window as unknown as { pingCalls: () => number }).pingCalls())).toBe(3);
});

test('a persistent error surfaces with the RPC detail once the retries are spent', async ({ page }) => {
  await page.evaluate(() => {
    let calls = 0;
    (window as unknown as { pingCalls: () => number }).pingCalls = () => calls;
    window.rstudio = {
      session: {
        ping: () => {
          calls++;
          return Promise.reject(new Error('ping RPC failed -- connection_refused'));
        },
      },
    } as Bridge;
  });
  const err = await probeSessionLiveness(page, 2000).catch((e: unknown) => e);
  expect(err).toBeInstanceOf(Error);
  expect(err).not.toBeInstanceOf(DeadlineError);
  expect((err as Error).message).toContain('connection_refused');
  // One attempt plus the three retries.
  expect(await page.evaluate(() => (window as unknown as { pingCalls: () => number }).pingCalls())).toBe(4);
});

test('a page that cannot answer the bridge check is a deadline, not a deaf session', async ({ page }) => {
  // The first read of window.rstudio parks the page's main thread past the
  // probe's deadline, the way a frame with no JS context parks an evaluate.
  await page.evaluate(() => {
    let blocked = false;
    Object.defineProperty(window, 'rstudio', {
      configurable: true,
      get() {
        if (!blocked) {
          blocked = true;
          const end = Date.now() + 1500;
          while (Date.now() < end) {
            // spin
          }
        }
        return undefined;
      },
    });
  });
  const err = await probeSessionLiveness(page, 300).catch((e: unknown) => e);
  expect(err).toBeInstanceOf(DeadlineError);
  expect(err).not.toBeInstanceOf(DeafSessionError);
});
