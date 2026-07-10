import { test, expect } from '@fixtures/rstudio.fixture';
import { dismissAllModals } from '@utils/commands';

/**
 * Harness self-test for uncaught-client-exception capture (#17952 follow-up).
 *
 * The automation agent wraps GWT's uncaught-exception handler and records
 * {message, stack, time} into window.rstudio.errors; the per-test fixture
 * drains the record after every test and fails the test that raised one.
 * `errors.simulate()` throws from a scheduled ($entry-wrapped) context, so
 * the exception takes the real uncaught-handler path -- the same one a
 * product bug (e.g. the Plots-pane ImageFrame TypeError) takes.
 */
test.describe('client exception capture', () => {
  test('simulated uncaught exceptions are recorded with a stack', async ({ rstudioPage: page }) => {
    await page.evaluate(() => window.rstudio!.errors.simulate('automation capture probe'));

    // The simulated throw is scheduled; poll until the recorder sees it.
    await expect.poll(
      () => page.evaluate(
        () => window.rstudio!.errors.list().map((e) => e.message).join('|'),
      ),
    ).toContain('automation capture probe');

    const errors = await page.evaluate(() => window.rstudio!.errors.list());
    expect(errors.length).toBeGreaterThanOrEqual(1);
    // Stack content varies by build (Java names in draft, obfuscated when
    // optimized); assert presence, not shape.
    expect(typeof errors[0].stack).toBe('string');

    // Clean up: clear the record so the per-test drain doesn't fail this
    // test, and dismiss the Error dialog the (delegated-to) default handler
    // showed for the simulated exception.
    await page.evaluate(() => window.rstudio!.errors.clear());
    await dismissAllModals(page);
  });

  test('unhandled promise rejections are recorded with a stack', async ({ rstudioPage: page }) => {
    // GWT does not $entry-wrap native promise continuations, so an exception
    // thrown in one bypasses the uncaught-exception handler and previously
    // vanished entirely (#18134's silent visual-editor stall). The agent
    // listens for unhandledrejection and records it into the same
    // window.rstudio.errors buffer the fixture drains.
    await page.evaluate(() => {
      void Promise.reject(new Error('automation rejection probe'));
    });

    // The unhandledrejection event fires after the current task; poll for it.
    await expect.poll(
      () => page.evaluate(
        () => window.rstudio!.errors.list().map((e) => e.message).join('|'),
      ),
    ).toContain('automation rejection probe');

    const errors = await page.evaluate(() => window.rstudio!.errors.list());
    const rejection = errors.find((e) => e.message.includes('automation rejection probe'));
    expect(rejection!.message).toContain('Unhandled promise rejection');
    expect(typeof rejection!.stack).toBe('string');

    // Clean up so the per-test drain doesn't fail this test. Unlike the
    // simulate() path, no Error dialog is shown for a rejection.
    await page.evaluate(() => window.rstudio!.errors.clear());
  });

  // NOTE: there is deliberately no self-test that leaves an exception in the
  // record to prove the fixture fails the test. The fail-on-exception throw
  // happens in the auto fixture's teardown (after use()), and Playwright's
  // test.fail() does NOT invert fixture-teardown failures -- such a test
  // reports as a hard failure, not an inverted pass. The fail-on-exception
  // path is instead exercised for real whenever a product bug raises an
  // uncaught exception during a test (it's how the AceEditorPreview detached
  // -frame TypeError was caught), so a dedicated negative self-test would add
  // a fragile, perpetually-red spec for no extra coverage.
});
