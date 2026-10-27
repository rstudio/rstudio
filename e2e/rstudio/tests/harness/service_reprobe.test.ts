import { test, expect } from '@playwright/test';
import * as http from 'http';
import type { AddressInfo } from 'net';
import { isServiceReachable, reprobeRequest, reprobeService } from '@utils/network';

// Exercise the request-level re-probe against a controlled local service.
// What matters is the classification, since it decides whether a citation
// test that saw neither results nor an error skips (service gone) or fails
// (product hung): the request must be followed through its redirects and
// read to the end, so a hang anywhere on that path -- not just at the host's
// front door -- counts as the service not answering, and the verdict must
// reach the host's cached entry so later guards on that host skip too.

type Behaviour = 'ok' | 'error' | 'hang' | 'redirect-then-hang' | 'redirect-then-ok' | 'ids';

let server: http.Server;
let origin: string;
let hanging: http.ServerResponse[] = [];

test.beforeAll(async () => {
  // Not http.createServer, which Snyk Code reports as cleartext transmission
  // even for a loopback-only test server.
  server = new http.Server((req, res) => {
    const behaviour = new URL(req.url ?? '/', origin).pathname.slice(1) as Behaviour;
    switch (behaviour) {
      case 'ok':
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end('{"data":[]}');
        break;
      case 'error':
        res.writeHead(503);
        res.end('unavailable');
        break;
      case 'redirect-then-hang':
        res.writeHead(302, { Location: `${origin}/hang` });
        res.end();
        break;
      case 'redirect-then-ok':
        res.writeHead(302, { Location: `${origin}/ok` });
        res.end();
        break;
      case 'ids':
        // A first leg whose answer names the second (PubMed esearch -> esummary).
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end('{"esearchresult":{"idlist":["1","2"]}}');
        break;
      default:
        // Headers only, then stall: the client has a connection and a status
        // line but never gets the body.
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.flushHeaders();
        hanging.push(res);
    }
  });
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
  origin = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
});

test.afterAll(async () => {
  for (const res of hanging) {
    res.destroy();
  }
  hanging = [];
  await new Promise<void>((resolve) => server.close(() => resolve()));
});

// Distinct hosts per test keep the probe cache (per worker, keyed by URL)
// from leaking one test's verdict into the next.
function host(name: string): string {
  return `${origin}/${name}`;
}

test('a request that answers still counts as reachable', async () => {
  expect(await reprobeRequest({ host: host('a'), href: `${origin}/ok` }, 2000)).toBe(true);
  expect(await isServiceReachable(host('a'))).toBe(true);
});

test('a request that hangs is not answering, and marks its host down', async () => {
  expect(await reprobeRequest({ host: host('b'), href: `${origin}/hang` }, 500)).toBe(false);
  // The cached host entry now reflects the request's outcome, so a later
  // test's guard on the same host skips without re-probing.
  expect(await isServiceReachable(host('b'))).toBe(false);
  // Until something re-probes the host itself.
  expect(await reprobeService(host('b'), 2000)).toBe(true);
});

test('a hang behind a redirect is still a hang', async () => {
  // The host-root probe does not follow redirects, so it would pass here; the
  // request probe follows them and times out where the lookup actually stalls.
  expect(await isServiceReachable(host('c'))).toBe(true);
  expect(await reprobeRequest({ host: host('c'), href: `${origin}/redirect-then-hang` }, 500)).toBe(false);
  expect(await isServiceReachable(host('c'))).toBe(false);
});

test('a redirect that lands on an answer is reachable', async () => {
  expect(await reprobeRequest({ host: host('d'), href: `${origin}/redirect-then-ok` }, 2000)).toBe(true);
});

test('a lookup that continues with a second request is probed through it', async () => {
  const chained = (second: string) => ({
    host: host(second),
    href: `${origin}/ids`,
    next: (body: string) => {
      const ids: string[] = JSON.parse(body).esearchresult.idlist;
      return { host: host(second), href: `${origin}/${second}?id=${ids.join(',')}` };
    },
  });
  expect(await reprobeRequest(chained('ok'), 2000)).toBe(true);
  // The first leg answers; the second hangs -- the lookup has not answered.
  expect(await reprobeRequest(chained('hang'), 500)).toBe(false);
  expect(await isServiceReachable(host('hang'))).toBe(false);
});

test('a server-side failure counts as the service not answering', async () => {
  expect(await reprobeRequest({ host: host('e'), href: `${origin}/error` }, 2000)).toBe(false);
  expect(await isServiceReachable(host('e'))).toBe(false);
});

test('request headers are passed through', async () => {
  const seen: string[] = [];
  server.prependListener('request', (req) => {
    seen.push(req.headers.accept ?? '');
  });
  await reprobeRequest(
    { host: host('f'), href: `${origin}/ok`, init: { headers: { Accept: 'application/vnd.citationstyles.csl+json' } } },
    2000,
  );
  expect(seen).toContain('application/vnd.citationstyles.csl+json');
});
