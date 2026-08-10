import { test as teardown } from '@playwright/test';
import { externalServerUrl } from '../fixtures/server.fixture';
import { isExternalServerRun } from '../utils/auth';
import {
  closeExternalSession,
  connectExternalSession,
  readManifest,
  removeManifest,
  scrubRemote,
} from '../utils/remote-provision';

/**
 * Best-effort scrub of the credentials the external-server provisioning step
 * pushed into the remote account's home (#18348, part 2). Runs as the
 * auth-teardown project (see playwright.config.ts), after every project that
 * depends on setup has finished. Only paths listed in the provisioning
 * manifest are removed -- stores that pre-existed on the account were never
 * added to it, so they are never touched.
 *
 * Honest failure mode (also documented in README.md): if the run crashes
 * before this executes, or the server is unreachable here, the pushed tokens
 * remain in the remote home. Re-run the teardown (the manifest survives in a
 * preserved sandbox) or remove the paths by hand. CI engines destroy the test
 * user with the runner, which cleans up implicitly.
 */
teardown('scrub external server credentials', async () => {
  teardown.skip(!isExternalServerRun(), 'remote scrubbing applies only to external-server runs (PW_RSTUDIO_SERVER_URL)');
  teardown.setTimeout(120_000);

  const sandbox = process.env.PW_SANDBOX;
  if (!sandbox) throw new Error('PW_SANDBOX is not set; sandbox-setup must run first');

  const manifest = readManifest(sandbox);
  if (manifest === null || manifest.createdPaths.length === 0) {
    console.log('[auth-teardown] no remote provisioning manifest (or nothing was created); nothing to scrub');
    if (manifest !== null) removeManifest(sandbox);
    return;
  }

  // The manifest names the host its paths were pushed to. If the run's server
  // URL has changed since provisioning, connecting now would log into a
  // different account entirely, where the scrub either finds nothing or deletes
  // files this run never wrote -- and either way the real tokens stay behind on
  // the original host. Fail instead, naming the host that still holds them.
  const serverUrl = externalServerUrl();
  if (manifest.serverUrl !== serverUrl) {
    throw new Error(
      `[auth-teardown] the manifest records credentials pushed to ${manifest.serverUrl}, but this run points at `
        + `${serverUrl ?? '(no server URL)'} -- refusing to scrub a host the credentials never reached; `
        + `remove them from ${manifest.serverUrl} by hand (they may hold real tokens)`,
    );
  }

  const session = await connectExternalSession();
  let survivors: string[];
  try {
    survivors = await scrubRemote(session.page, manifest.createdPaths);
  } finally {
    await closeExternalSession(session);
  }

  if (survivors.length > 0) {
    // Keep the manifest so a preserved sandbox still names what's left, and
    // fail this teardown test: credentials surviving on a remote host must
    // turn the run's report red, not scroll by as a warning.
    throw new Error(
      `[auth-teardown] could not remove from ${manifest.serverUrl}: ${survivors.join(', ')} -- `
        + 'remove these by hand (they may hold real tokens)',
    );
  }
  removeManifest(sandbox);
  console.log(`[auth-teardown] scrubbed ${manifest.createdPaths.length} provisioned path(s) from ${manifest.serverUrl}`);
});
