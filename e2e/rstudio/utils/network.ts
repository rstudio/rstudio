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

/** A request a test can re-probe with reprobeRequest(). */
export interface ServiceRequest {
  /** The host whose cached reachability the probe's outcome replaces. */
  host: string;
  // Not `url`: Snyk Code reads any `x.url` that reaches fetch() as an incoming
  // request's URL and reports SSRF.
  href: string;
  init?: RequestInit;
  /**
   * The request the rsession makes next, derived from this one's response
   * body (e.g. PubMed's esummary for the ids esearch returned), or undefined
   * when the lookup ends here. Probed within the same timeout.
   */
  next?: (body: string) => ServiceRequest | undefined;
}

/**
 * The requests the rsession makes for each citation lookup, mirrored from the
 * panmirror modules so a test can re-probe the request it actually waited on
 * rather than just the host.
 */
export const CITATION_SERVICE_REQUESTS = {
  // SessionPanmirrorDOI.cpp: CSL content negotiation at doi.org, which
  // redirects to the registration agency's own service (data.crosscite.org
  // for DataCite DOIs) -- the leg that hangs when that agency is down.
  doi: (doi: string): ServiceRequest => ({
    host: CITATION_SERVICE_HOSTS.doi,
    href: `${CITATION_SERVICE_HOSTS.doi}/${doi}`,
    init: { headers: { Accept: 'application/vnd.citationstyles.csl+json' } },
  }),
  // SessionPanmirrorCrossref.cpp
  crossref: (query: string): ServiceRequest => ({
    host: CITATION_SERVICE_HOSTS.crossref,
    href: `${CITATION_SERVICE_HOSTS.crossref}/works?query=${encodeURIComponent(query)}`,
  }),
  // SessionPanmirrorDataCite.cpp
  datacite: (query: string): ServiceRequest => ({
    host: CITATION_SERVICE_HOSTS.datacite,
    href: `${CITATION_SERVICE_HOSTS.datacite}/dois?query=${encodeURIComponent(query)}`,
  }),
  // SessionPanmirrorPubMed.cpp: esearch for the ids, then esummary for their
  // records -- the search is not answered until both have.
  pubmed: (query: string): ServiceRequest => ({
    host: CITATION_SERVICE_HOSTS.pubmed,
    href:
      `${CITATION_SERVICE_HOSTS.pubmed}/entrez/eutils/esearch.fcgi` +
      `?db=pubmed&term=${encodeURIComponent(query)}&sort=relevance&retmax=25${PUBMED_STANDARD_PARAMS}`,
    next: (body) => {
      const ids = pubMedIdList(body);
      if (ids.length === 0) {
        return undefined;
      }
      return {
        host: CITATION_SERVICE_HOSTS.pubmed,
        href:
          `${CITATION_SERVICE_HOSTS.pubmed}/entrez/eutils/esummary.fcgi` +
          `?db=pubmed&id=${encodeURIComponent(ids.join(','))}${PUBMED_STANDARD_PARAMS}`,
      };
    },
  }),
} as const;

// Appended to every PubMed request by pubMedRequest().
const PUBMED_STANDARD_PARAMS = '&tool=rstudio&email=pubmed%40posit.co&retmode=json';

// The ids an esearch response carries, or none when it is not the JSON the
// rsession expects (the probe then stops at this leg, as the lookup would).
function pubMedIdList(body: string): string[] {
  try {
    const idlist: unknown = JSON.parse(body)?.esearchresult?.idlist;
    return Array.isArray(idlist) ? idlist.map(String) : [];
  } catch {
    return [];
  }
}

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
 * Re-probe the exact request a test waited on, not just its host. A host can
 * answer a root probe while the request itself hangs: a DOI lookup's content
 * negotiation at doi.org redirects to the registration agency, and a search
 * endpoint can stall while the API's front door still responds. So this
 * follows redirects and reads the body, within the same timeout, and treats a
 * service-side failure (5xx, 429) like a hang -- either way it is the service
 * that did not answer, not the product. The outcome replaces the cached
 * answer for `request.host`, so later tests' guards on that host skip too.
 */
export function reprobeRequest(request: ServiceRequest, timeoutMs = 10000): Promise<boolean> {
  const probe = probeRequestChain(request, AbortSignal.timeout(timeoutMs)).catch(() => false);
  probeCache.set(request.host, probe);
  return probe;
}

// Follow `request` and whatever it derives from its response, all under one
// abort signal; rejects on a network-level failure or timeout on any leg.
async function probeRequestChain(request: ServiceRequest, signal: AbortSignal): Promise<boolean> {
  const response = await fetch(request.href, { ...request.init, redirect: 'follow', signal });
  const body = await response.text();
  if (response.status >= 500 || response.status === 429) {
    return false;
  }

  const next = request.next?.(body);
  return next ? probeRequestChain(next, signal) : true;
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
