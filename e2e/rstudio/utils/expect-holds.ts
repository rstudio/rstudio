import { sleep } from '@utils/constants';

/**
 * Asserts continuously for `durationMs`, failing on the first violation.
 *
 * expect.poll cannot express "must not happen": it returns on its first passing
 * sample, which passes at once when the good state is also the starting state.
 * The window starts after the first sample, which can be a slow locator read.
 */
export async function expectHoldsFor(
  durationMs: number,
  assertions: () => Promise<void>,
): Promise<void> {
  await assertions();
  const deadline = Date.now() + durationMs;
  while (Date.now() < deadline) {
    await sleep(50);
    await assertions();
  }
}
