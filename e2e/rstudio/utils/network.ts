/**
 * Reachability probes for tests that depend on external services.
 *
 * Some tests exercise features whose lookups hit live third-party APIs (e.g.
 * the Insert Citation dialog's DOI/Crossref/DataCite/PubMed sources). An
 * outage or blocked egress on a CI runner is not a product bug, so such tests
 * should skip -- not fail -- when the service cannot be reached at all.
 */

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
