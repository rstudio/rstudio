/**
 * Reachability probes for tests that depend on external services.
 *
 * Some tests exercise features whose lookups hit live third-party APIs (e.g.
 * the Insert Citation dialog's DOI/Crossref/DataCite/PubMed sources). An
 * outage or blocked egress on a CI runner is not a product bug, so such tests
 * should skip -- not fail -- when the service cannot be reached at all.
 */

import * as net from 'net';

/**
 * External hosts the rsession's citation lookups hit. The requests originate
 * in the C++ session process, not the browser (see the modules under
 * src/cpp/session/modules/panmirror/), so they cannot be intercepted or
 * mocked via Playwright routing.
 */
export const CITATION_SERVICE_HOSTS = {
  doi: 'https://doi.org',
  crossref: 'https://api.crossref.org',
  datacite: 'https://api.datacite.org',
  pubmed: 'https://eutils.ncbi.nlm.nih.gov',
} as const;

const probeCache = new Map<string, Promise<boolean>>();

/**
 * Whether an external service is reachable from this runner. Probes from Node
 * (not through the product) so that tests skip on genuine egress/outage
 * problems while a regression in RStudio's own request layer still fails
 * them. Any HTTP response -- including an error status or redirect -- counts
 * as reachable; only network-level failures (DNS, connect, TLS, timeout) do
 * not. Results are cached per URL for the lifetime of the worker.
 */
export function isServiceReachable(url: string, timeoutMs = 10000): Promise<boolean> {
  let probe = probeCache.get(url);
  if (!probe) {
    probe = fetch(url, { redirect: 'manual', signal: AbortSignal.timeout(timeoutMs) })
      .then(() => true)
      .catch(() => false);
    probeCache.set(url, probe);
  }
  return probe;
}

/**
 * Probe a service again, replacing the cached answer. A probe that passed at
 * test start only proves the host answered then -- the runner's network can
 * degrade mid-run (#18426), so a test whose service call silently times out
 * can re-probe to distinguish "service gone" (skip) from "product hung"
 * (fail). The refreshed cache entry also lets later tests' guards see the
 * degraded state.
 */
export function reprobeService(url: string, timeoutMs = 10000): Promise<boolean> {
  probeCache.delete(url);
  return isServiceReachable(url, timeoutMs);
}

/**
 * Whether a raw TCP port accepts connections, for services that don't speak
 * HTTP (e.g. a database). Deliberately uncached: the databases the
 * Connections tests talk to are started and stopped within a run, so a
 * cached answer could be stale in either direction. Note this probes from
 * the test runner; a database that must be reachable from the rsession
 * (remote Server mode) needs an in-session probe instead (see
 * utils/connections.ts).
 */
export function isTcpReachable(host: string, port: number, timeoutMs = 5000): Promise<boolean> {
  return new Promise((resolve) => {
    const socket = net.connect({ host, port });
    const done = (result: boolean) => {
      socket.destroy();
      resolve(result);
    };
    socket.setTimeout(timeoutMs, () => done(false));
    socket.once('connect', () => done(true));
    socket.once('error', () => done(false));
  });
}
