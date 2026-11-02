/**
 * A local stand-in for the services the Insert Citation dialog's lookups hit
 * (doi.org, Crossref, DataCite, PubMed).
 *
 * Those requests come from the rsession, not the browser, so Playwright
 * routing cannot intercept them. Instead the session's rstudio.<service>_url
 * options point them here (see serviceBaseUrl() under
 * src/cpp/session/modules/panmirror/). The answers come from
 * fixtures/citation-services.json, trimmed from the live services' own
 * responses, so the tests depend on neither those services' availability
 * nor their latency -- DataCite's search alone ranges from seconds to past
 * the rsession's timeout.
 */

import * as fs from 'fs';
import * as http from 'http';
import * as path from 'path';
import type { AddressInfo } from 'net';
import { rStringLiteral } from './r';

export type CitationService = 'doi' | 'crossref' | 'datacite' | 'pubmed';

const SERVICES: readonly CitationService[] = ['doi', 'crossref', 'datacite', 'pubmed'];

const CSL_JSON = 'application/vnd.citationstyles.csl+json';

interface Fixtures {
  /** CSL JSON by lowercased DOI. */
  doi: Record<string, unknown>;
  /** /works responses by query. */
  crossref: Record<string, unknown>;
  /** /dois responses by query. */
  datacite: Record<string, unknown>;
  pubmed: {
    /** esearch responses by term. */
    esearch: Record<string, unknown>;
    /** esummary records by uid. */
    esummary: Record<string, unknown>;
  };
}

const FIXTURES: Fixtures = JSON.parse(
  fs.readFileSync(path.join(__dirname, '..', 'fixtures', 'citation-services.json'), 'utf8'),
);

interface Answer {
  status: number;
  body?: unknown;
  contentType?: string;
}

/** R code that restores the live services after optionsCall(). */
export const RESET_CITATION_SERVICE_OPTIONS =
  `options(${SERVICES.map((s) => `rstudio.${s}_url = NULL`).join(', ')})`;

export class CitationServiceMock {
  private readonly failures = new Map<CitationService, { status: number; resource?: string }>();

  private constructor(
    private readonly server: http.Server,
    private readonly baseUrl: string,
  ) {}

  /** Start the mock on a free loopback port. */
  static async start(): Promise<CitationServiceMock> {
    // Not http.createServer, which Snyk Code reports as cleartext transmission
    // even for a loopback-only test server.
    const server = new http.Server();
    await new Promise<void>((resolve, reject) => {
      server.once('error', reject);
      server.listen(0, '127.0.0.1', resolve);
    });
    const { port } = server.address() as AddressInfo;
    const mock = new CitationServiceMock(server, `http://127.0.0.1:${port}`);
    server.on('request', (req, res) => mock.handle(req, res));
    return mock;
  }

  /** R code that points the session's citation lookups at this mock. */
  optionsCall(): string {
    const args = SERVICES.map((s) => `rstudio.${s}_url = ${rStringLiteral(`${this.baseUrl}/${s}`)}`);
    return `options(${args.join(', ')})`;
  }

  /**
   * Answer requests to `service` with `status` until reset(): every request,
   * or only those for `resource` (e.g. '/entrez/eutils/esummary.fcgi').
   */
  fail(service: CitationService, status: number, resource?: string): void {
    this.failures.set(service, { status, resource });
  }

  reset(): void {
    this.failures.clear();
  }

  async close(): Promise<void> {
    this.server.closeAllConnections();
    await new Promise<void>((resolve) => this.server.close(() => resolve()));
  }

  private handle(req: http.IncomingMessage, res: http.ServerResponse): void {
    const url = new URL(req.url ?? '/', this.baseUrl);
    const [, service, ...rest] = url.pathname.split('/');
    const answer = SERVICES.includes(service as CitationService)
      ? this.answer(service as CitationService, `/${rest.join('/')}`, url.searchParams, req.headers)
      : { status: 404 };

    res.writeHead(answer.status, { 'Content-Type': answer.contentType ?? 'application/json' });
    res.end(answer.body === undefined ? '' : JSON.stringify(answer.body));
  }

  private answer(
    service: CitationService,
    resource: string,
    params: URLSearchParams,
    headers: http.IncomingHttpHeaders,
  ): Answer {
    const failure = this.failures.get(service);
    if (failure !== undefined && (failure.resource === undefined || failure.resource === resource)) {
      return { status: failure.status };
    }

    switch (service) {
      case 'doi': {
        // doi.org only returns CSL to a request that negotiates for it; any
        // other Accept gets the work's landing page, which the lookup can't
        // parse. So a lookup that drops the header must not get CSL here.
        if (!headers.accept?.includes(CSL_JSON)) {
          return { status: 406 };
        }
        const csl = FIXTURES.doi[safeDecode(resource.slice(1)).toLowerCase()];
        return csl === undefined ? { status: 404 } : { status: 200, body: csl, contentType: CSL_JSON };
      }

      case 'crossref':
        if (resource === '/works') {
          const query = params.get('query') ?? '';
          return {
            status: 200,
            body: FIXTURES.crossref[query.toLowerCase()] ?? {
              status: 'ok',
              'message-type': 'work-list',
              message: { 'total-results': 0, items: [], 'items-per-page': 0, query: { 'search-terms': query } },
            },
          };
        }
        break;

      case 'datacite':
        if (resource === '/dois') {
          // Unsorted, DataCite lists matches newest-first, which can push every
          // close match off the dialog's first rows (#19319). So only a search
          // that asks for relevance gets results here.
          if (params.get('sort') !== 'relevance') {
            return { status: 200, body: { data: [] } };
          }
          const query = (params.get('query') ?? '').toLowerCase();
          return { status: 200, body: FIXTURES.datacite[query] ?? { data: [] } };
        }
        break;

      case 'pubmed':
        if (resource === '/entrez/eutils/esearch.fcgi') {
          const term = (params.get('term') ?? '').toLowerCase();
          return {
            status: 200,
            body: FIXTURES.pubmed.esearch[term] ?? { esearchresult: { count: '0', idlist: [] } },
          };
        }
        if (resource === '/entrez/eutils/esummary.fcgi') {
          const uids = (params.get('id') ?? '').split(',').filter((id) => id in FIXTURES.pubmed.esummary);
          const records = Object.fromEntries(uids.map((id) => [id, FIXTURES.pubmed.esummary[id]]));
          return { status: 200, body: { result: { uids, ...records } } };
        }
        break;
    }

    return { status: 404 };
  }
}

// A malformed escape is a request the fixtures can't answer, not a crash.
function safeDecode(s: string): string {
  try {
    return decodeURIComponent(s);
  } catch {
    return s;
  }
}
