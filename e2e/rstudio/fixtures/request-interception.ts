import type { BrowserContext } from 'playwright';

// Matches no URL. A regex rather than a predicate, so Playwright's server
// side matches it and continues each request itself, without dispatching
// it to this handler.
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
 * It is installed at launch rather than by the specs that route, since the
 * session outlives each spec: turned on by the first routing spec, the costs
 * below would apply to whichever specs happen to follow it. While
 * interception is on, for every spec:
 * - the HTTP cache is disabled;
 * - each request is paused until Playwright continues it. In @playwright/test
 *   Playwright's server runs in the worker process, so the IDE's requests wait
 *   while that process's event loop is blocked (e.g. by spawnSync);
 * - Playwright answers CORS preflights itself, with a permissive 204, so they
 *   never reach the server.
 */
export async function keepRequestInterceptionOn(context: BrowserContext): Promise<void> {
  await context.route(NO_URL, (route) => route.fallback());
}
