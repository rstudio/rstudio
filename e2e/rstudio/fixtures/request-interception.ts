import type { BrowserContext } from 'playwright';

// Matches no URL. A regex rather than a predicate, so Playwright filters
// requests against it in its own process instead of sending each one to
// the test runner.
const NO_URL = /^rstudio-e2e:no-url$/;

/**
 * Keep Playwright's request interception on for the life of the context.
 *
 * Playwright turns interception on when a page gains its first route and off
 * when the last one is removed, and a request paused during that switch is
 * never continued. RStudio always has a get_events long-poll in flight, so a
 * spec that removes its route can strand it: the client then receives no
 * more events (console prompts included) and later waits time out. With
 * this route installed at launch, a spec's route is never the last one.
 *
 * Playwright also disables the HTTP cache while interception is on, so every
 * spec runs uncached, rather than only those that follow a routing spec.
 */
export async function keepRequestInterceptionOn(context: BrowserContext): Promise<void> {
  await context.route(NO_URL, (route) => route.fallback());
}
