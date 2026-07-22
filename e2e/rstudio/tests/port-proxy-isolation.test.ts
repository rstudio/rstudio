/**
 * Port-proxy cross-user isolation tests (rstudio-pro#11470).
 *
 * The `/p/` (IPv4) and `/p6/` (IPv6) portmapped-app handlers decode a raw
 * target port from a client-supplied `port-token` cookie and open a
 * localhost proxy connection to it. Historically this happened without
 * ever verifying that the destination port is owned by the requesting
 * user: any authenticated user could compute an obfuscated port value
 * with their *own* valid token that happens to decode to *another* user's
 * bound port, and reach that user's app.
 *
 * These tests exercise the fix (post-connect kernel-UID ownership check,
 * see server_core::socket_utils / LocalhostAsyncClient):
 *   1. Cross-user isolation (negative) via `/p/`.
 *   2. Same rejection via `/p6/` (IPv6 parity).
 *   3. Same-user regression: a user's own `/p/` and `/p6/` access to their
 *      own port must still succeed (the fix must not break normal
 *      portmapping).
 *
 * The obfuscation math mirrors server_core::transformPort() (UrlPorts.cpp)
 * for the non-server-routing case (an ordinary session/app port, not one
 * of rserver's own service ports): reimplemented here in TypeScript rather
 * than shelling out to the `rserver-url` CLI so the test has no runtime
 * dependency on that tool being installed/working on the SUT.
 *
 * Rejection status code: a uid mismatch (`server_core::socket_utils::
 * verifyPeerUid()` / `LocalhostAsyncClient.hpp`) is tagged with the
 * `kPortOwnershipRejectedProperty` error property (`SocketOwnership.hpp`),
 * which `handleLocalhostError()` -- in both `ServerSessionProxy.cpp`
 * (rserver-side path) and `SessionProxy.cpp` (Launcher-mode `SessionProxy`
 * self-check, Step 4's `::geteuid()` check, the path this environment
 * exercises -- see `launcherEnabled` below) -- checks for and maps to `403
 * Forbidden` with a generic body, distinct from the `500` a genuine,
 * unrelated connection failure (e.g. an unreachable listener, a transient
 * session-routing race) would still produce. Asserting on the specific `403`
 * status is therefore enough on its own to distinguish "the ownership check
 * rejected this" from any other failure mode, without needing to also
 * inspect the requesting session's rsession log (which isn't reliably at a
 * fixed, sudo-readable path across all CI environments this suite runs in).
 */

import { Browser, BrowserContext, Page } from "@playwright/test";
import { test, expect } from "../fixtures/workbenchTest";
import { LoginPage } from "../pages/LoginPage";
import { IDE } from "../pages/IDE";
import { createUser, deleteUser, TestUser } from "../utils/userCreation";
import { execCommand } from "../utils/shellExec";

// Match the convention used throughout tests/**: default to launcher-enabled
// ("1") when LAUNCHER_ENABLED isn't explicitly set, rather than assuming a
// non-launcher (direct-connect) environment.
const launcherEnabled = process.env.LAUNCHER_ENABLED ?? "1";

/**
 * Reimplementation of server_core::transformPort() (src/cpp/server_core/UrlPorts.cpp)
 * for the non-server-routing case: obfuscates `port` using `token` the same
 * way the server does when building a portmapped URL, producing the 8-hex-digit
 * value that appears after `/p/` or `/p6/` in the URL. Uses BigInt throughout to
 * exactly match the C++ 64-bit arithmetic (the intermediate product and XOR can
 * exceed 32 bits).
 */
function transformPort(token: string, port: number): string {
  const multiplier = BigInt(parseInt(token.slice(0, 4), 16));
  const key = BigInt(`0x${token.slice(4)}`);
  const preTransformed = BigInt((port * 8854) % 65535);
  const result = (preTransformed * multiplier) ^ key;
  return result.toString(16).padStart(8, "0");
}

interface UserSession {
  user: TestUser;
  context: BrowserContext;
  page: Page;
  /** Origin, e.g. http://localhost:8787 */
  origin: string;
  /** Session path, e.g. /s/abc123de45/ */
  sessionPath: string;
  /** This session's `port-token` cookie value. */
  portToken: string;
}

/**
 * Log in as `user`, launch an RStudio session in its own browser context, and
 * capture the session path + `port-token` cookie needed to build/request
 * portmapped URLs on this session's behalf.
 */
async function loginAndLaunchSession(
  browser: Browser,
  user: TestUser
): Promise<UserSession> {
  const context = await browser.newContext();
  const page = await context.newPage();
  const loginPage = new LoginPage(page);
  const ide = new IDE(page);

  await loginPage.goto();
  await loginPage.performLogin(user.username, user.password);
  await loginPage.launchSession(launcherEnabled, "RStudio");
  await ide.waitForPageToLoad();

  const url = new URL(page.url());
  const match = url.pathname.match(/^(\/s\/[^/]+\/)/);
  expect(
    match,
    `Expected an RStudio session URL of the form /s/<id>/ but got ${url.pathname}`
  ).not.toBeNull();
  const sessionPath = match![1];
  const origin = url.origin;

  // The port-token cookie is set as part of the client_init RPC that runs
  // once the session is up (SessionClientInit.cpp); waitForPageToLoad()
  // already waits for the IDE shell, but poll briefly in case the cookie
  // write lands just after the IDE becomes visible.
  let portToken: string | undefined;
  for (let attempt = 0; attempt < 10 && !portToken; attempt++) {
    const cookies = await context.cookies(`${origin}${sessionPath}`);
    portToken = cookies.find((c) => c.name === "port-token")?.value;
    if (!portToken) await page.waitForTimeout(1_000);
  }
  expect(
    portToken,
    "Expected a port-token cookie to be set for this session"
  ).toBeTruthy();

  return { user, context, page, origin, sessionPath, portToken: portToken! };
}

/**
 * Start a plain HTTP listener owned by `username` on `port`, bound to the
 * given loopback address (127.0.0.1 for IPv4, ::1 for IPv6). This does not
 * go through the RStudio session at all -- it's a bare OS-level socket
 * bound by that user's account, which is exactly the kind of thing rserver's
 * `/p/`·`/p6/` proxy connects to (arbitrary bound ports, not just ones
 * belonging to a live rsession).
 */
function startListener(
  username: string,
  port: number,
  bindAddress: string
): void {
  // nohup is required here (matching rootlessWorkbench.ts's startRootlessWorkbench
  // pattern): in ssh EXEC_MODE the remote shell session used to run this command
  // tears down as soon as execCommand() returns, and a bare backgrounded `&`
  // process receives SIGHUP and dies with it -- a plain `&` only "works" in
  // exec modes (docker/k8s/local) that keep the parent shell alive. The two
  // test users then have to log in and launch full sessions before the actual
  // attack/success request runs, which is plenty of time for an un-nohup'd
  // listener to have already been reaped.
  execCommand(
    `nohup sudo -u ${username} python3 -m http.server ${port} --bind ${bindAddress} --directory /tmp ` +
      `>/tmp/portproxy_isolation_${port}.log 2>&1 < /dev/null &`
  );
  // Poll until the listener actually accepts connections instead of a fixed
  // sleep -- keeps the test fast on a healthy box and robust on a slow one.
  const deadline = Date.now() + 15_000;
  const curlHost = bindAddress === "::1" ? "[::1]" : bindAddress;
  while (Date.now() < deadline) {
    const status = execCommand(
      `curl -s -o /dev/null -w '%{http_code}' http://${curlHost}:${port}/ || echo 000`,
      { ignoreError: true }
    ).trim();
    if (status === "200") return;
  }
  throw new Error(
    `Listener for ${username} on ${bindAddress}:${port} never came up`
  );
}

function stopListener(port: number): void {
  execCommand(`pkill -f 'http.server ${port}' || true`, { ignoreError: true });
}

/** True if something is already listening on `port` (either IPv4 or IPv6 loopback). */
function isPortInUse(port: number): boolean {
  const result = execCommand(
    `(ss -H -tln "sport = :${port}" 2>/dev/null | grep -q .) && echo BUSY || echo FREE`,
    { ignoreError: true }
  ).trim();
  return result === "BUSY";
}

/**
 * Pick a port from a range unlikely to collide across parallel test
 * workers/files, retrying against a live pre-bind check: under
 * `@parallel_safe` execution, two concurrent runs picking the same random
 * port would otherwise cause a flaky failure unrelated to the security
 * property under test.
 */
function pickTestPort(): number {
  for (let attempt = 0; attempt < 20; attempt++) {
    const port = 40000 + Math.floor(Math.random() * 10000);
    if (!isPortInUse(port)) return port;
  }
  throw new Error("pickTestPort: could not find a free port after 20 attempts");
}

/**
 * GET a portmapped (`/p/`·`/p6/`) URL that is expected to *succeed* (200),
 * retrying on `500` a few times.
 *
 * A brand-new Launcher-mode session's endpoint/SSL-cert info isn't always
 * cached yet by rserver's overlay session proxy on the very first proxied
 * request after launch (`ServerSessionProxyOverlay.cpp`
 * `getLauncherSessionJobInfo` cache miss) -- a transient race in the
 * pre-existing session-routing layer, unrelated to the rstudio-pro#11470
 * ownership check under test here. Retry rather than let it flake the
 * same-user regression assertion below. (Not used for the cross-user
 * rejection tests: there, `403` is the *expected*, deterministic outcome --
 * see the file header comment -- so no retry is needed or appropriate; a
 * `500` there would indicate an unrelated failure, not the ownership check.)
 */
async function getPortmappedUrlExpectSuccess(
  requestContext: BrowserContext["request"],
  url: string,
  attempts = 5,
  delayMs = 2_000
): Promise<import("@playwright/test").APIResponse> {
  let response = await requestContext.get(url, { failOnStatusCode: false });
  for (let attempt = 1; attempt < attempts && response.status() === 500; attempt++) {
    await new Promise((resolve) => setTimeout(resolve, delayMs));
    response = await requestContext.get(url, { failOnStatusCode: false });
  }
  return response;
}

test.describe(
  "Port proxy cross-user isolation (rstudio-pro#11470)",
  { tag: ["@security", "@rstudio", "@parallel_safe"] },
  () => {
    test.describe.configure({ timeout: 240_000 });

    test("cross-user /p/ access to another user's port is rejected", async ({
      browser,
    }) => {
      const userA = createUser();
      const userB = createUser();
      let sessA: UserSession | undefined;
      let sessB: UserSession | undefined;
      const port = pickTestPort();

      try {
        sessA = await loginAndLaunchSession(browser, userA);
        sessB = await loginAndLaunchSession(browser, userB);

        // User A owns a real, bound-and-listening localhost port.
        startListener(userA.username, port, "127.0.0.1");

        // User B computes the obfuscated port value using their OWN valid
        // port-token, but targets User A's port -- exactly the attack this
        // fix closes.
        const encoded = transformPort(sessB.portToken, port);
        const attackUrl = `${sessB.origin}${sessB.sessionPath}p/${encoded}/`;

        const response = await sessB.context.request.get(attackUrl, {
          failOnStatusCode: false,
        });

        // See the file header comment re: rejection status codes -- the
        // ownership check (Step 4's SessionProxy `::geteuid()` self-check,
        // in this Launcher-mode environment) is tagged so
        // handleLocalhostError() maps it to 403, distinct from the 500 an
        // unrelated failure (e.g. an unreachable listener) would produce --
        // so this status check alone is enough to confirm the fix actually
        // rejected the request, not just "something went wrong."
        expect(
          response.status(),
          `Expected cross-user /p/ access to be rejected with 403, got ` +
            `${response.status()} for ${attackUrl}`
        ).toBe(403);

        const body = await response.text();
        expect(
          body,
          "Response must not contain User A's proxied directory listing"
        ).not.toContain("Directory listing for");
      } finally {
        stopListener(port);
        deleteUser(userA.username);
        deleteUser(userB.username);
        await sessA?.context.close();
        await sessB?.context.close();
      }
    });

    test("cross-user /p6/ access to another user's port is rejected", async ({
      browser,
    }) => {
      const userA = createUser();
      const userB = createUser();
      let sessA: UserSession | undefined;
      let sessB: UserSession | undefined;
      const port = pickTestPort();

      try {
        sessA = await loginAndLaunchSession(browser, userA);
        sessB = await loginAndLaunchSession(browser, userB);

        // User A owns a real, bound-and-listening IPv6 loopback port.
        startListener(userA.username, port, "::1");

        // Same attack as the /p/ test, against the IPv6 handler. The
        // obfuscation math is identical for /p/ and /p6/ -- only the URL
        // prefix and the resulting server-side connect address differ.
        const encoded = transformPort(sessB.portToken, port);
        const attackUrl = `${sessB.origin}${sessB.sessionPath}p6/${encoded}/`;

        const response = await sessB.context.request.get(attackUrl, {
          failOnStatusCode: false,
        });

        // See the file header comment and the /p/ test above re: rejection
        // status codes.
        expect(
          response.status(),
          `Expected cross-user /p6/ access to be rejected with 403, got ` +
            `${response.status()} for ${attackUrl}`
        ).toBe(403);

        const body = await response.text();
        expect(
          body,
          "Response must not contain User A's proxied directory listing"
        ).not.toContain("Directory listing for");
      } finally {
        stopListener(port);
        deleteUser(userA.username);
        deleteUser(userB.username);
        await sessA?.context.close();
        await sessB?.context.close();
      }
    });

    test("same-user /p/ and /p6/ access to your own port still succeeds", async ({
      browser,
    }) => {
      const userA = createUser();
      let sessA: UserSession | undefined;
      const portV4 = pickTestPort();
      const portV6 = pickTestPort();

      try {
        sessA = await loginAndLaunchSession(browser, userA);

        startListener(userA.username, portV4, "127.0.0.1");
        startListener(userA.username, portV6, "::1");

        const encodedV4 = transformPort(sessA.portToken, portV4);
        const urlV4 = `${sessA.origin}${sessA.sessionPath}p/${encodedV4}/`;
        const responseV4 = await getPortmappedUrlExpectSuccess(sessA.context.request, urlV4);
        expect(
          responseV4.status(),
          `Expected same-user /p/ access to succeed, got ${responseV4.status()} for ${urlV4}`
        ).toBe(200);
        expect(await responseV4.text()).toContain("Directory listing for");

        const encodedV6 = transformPort(sessA.portToken, portV6);
        const urlV6 = `${sessA.origin}${sessA.sessionPath}p6/${encodedV6}/`;
        const responseV6 = await getPortmappedUrlExpectSuccess(sessA.context.request, urlV6);
        expect(
          responseV6.status(),
          `Expected same-user /p6/ access to succeed, got ${responseV6.status()} for ${urlV6}`
        ).toBe(200);
        expect(await responseV6.text()).toContain("Directory listing for");
      } finally {
        stopListener(portV4);
        stopListener(portV6);
        deleteUser(userA.username);
        await sessA?.context.close();
      }
    });
  }
);
