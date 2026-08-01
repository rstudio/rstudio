import * as fs from 'fs';
import * as path from 'path';
import { test, expect } from '@fixtures/rstudio.fixture';
import { requireAiCredentials } from '@utils/ai-credentials';
import { CHAT_PROVIDERS } from '@utils/constants';
import { useSuiteSandbox } from '@utils/sandbox';
import { setPref } from '@utils/commands';
import { createAndOpenProject, closeProjectIfOpen } from '@utils/project';
import { createChatActions } from './_chat-setup';

// Session restarts with a live Posit Assistant must complete (#18394).
//
// Every project open/close tears the session down seconds after the chat
// backend and NES agent started, which is exactly the race that wedged CI
// shards: the session's shutdown wait pumped the full background-processing
// machinery mid-teardown, a blocked item wedged rsession's exit permanently,
// and the desktop relaunch (driven by the child exit event) never happened --
// the shard then died by heartbeat with 300s of silence. The @chat suites hit
// this only incidentally, when the tests/projects region happened to share
// their worker; this test makes the workload deliberate so a recurrence fails
// HERE, visibly, instead of killing whatever unrelated shard imports the
// leaked provider state.
//
// Desktop-only: the provider prefs this suite flips are per-worker on
// desktop; in server mode they can be shared (external PW_RSTUDIO_SERVER_URL
// servers), and the data home always is -- see disableLeakedAssistant in
// rstudio.fixture.ts.
test.describe.serial(
  'Session restart with active assistant (#18394)',
  { tag: ['@ai', '@chat', '@desktop_only', '@serial'] },
  () => {
    requireAiCredentials(test, 'positai');
    const sandbox = useSuiteSandbox();

    const RESTART_CYCLES = 3;

    test.beforeAll(async ({ rstudioPage: page, rstudioSession }) => {
      // The provider selection below can trigger the Posit Assistant install
      // flow on a cold worker; give the hook the headroom it needs.
      test.setTimeout(300000);

      // Baseline the backend's log BEFORE enabling the provider: the log
      // directory is the worker's data home, shared with every suite that
      // ran before this one, so a positai.log from an earlier @chat suite
      // may already exist. Only growth past this baseline proves the backend
      // started for US.
      const logDir = rstudioSession.logDir;
      expect(logDir, 'desktop session should expose its log directory').toBeTruthy();
      const backendLog = path.join(logDir!, 'positai.log');
      const baselineSize = fs.existsSync(backendLog) ? fs.statSync(backendLog).size : -1;

      // Select Posit Assistant as the chat provider through the Options
      // dialog. This settles the install/update prompt, so it also guarantees
      // the backend is actually installed in this worker's data home -- the
      // prerequisite for anything below to exercise the shutdown race.
      const { assistantActions } = createChatActions(page);
      await assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);

      // Turn on the code assistant too: the chat provider starts the chat
      // backend, this starts the NES agent. The wedged CI sessions ran both.
      // (The agent has no bridge-observable startup artifact today, so unlike
      // the backend below its startup is not independently verified.)
      await setPref(page, 'assistant', 'posit');

      // Guard against silently testing nothing: the chat backend appends to
      // positai.log in the session's log directory as it starts. If the log
      // never grows past the baseline, the backend isn't running and the
      // loop below would just exercise plain restarts.
      await expect
        .poll(
          () => {
            try {
              return fs.statSync(backendLog).size > baselineSize;
            } catch {
              return false;
            }
          },
          {
            message: `chat backend never started (${backendLog} did not grow)`,
            timeout: 60000,
          },
        )
        .toBe(true);
    });

    test.afterAll(async ({ rstudioPage: page }) => {
      // On a real wedge any of these steps can fail; keep them independent so
      // a dead session still gets the provider prefs flipped back where
      // possible, and log instead of swallowing -- a failure here IS the
      // signal this suite exists to surface.
      await closeProjectIfOpen(page).catch((err) => {
        console.warn(`[restart-under-agent] closeProjectIfOpen failed in afterAll: ${err}`);
      });

      for (const name of ['assistant', 'chat_provider'] as const) {
        await setPref(page, name, 'none').catch((err) => {
          console.warn(`[restart-under-agent] resetting ${name} failed in afterAll: ${err}`);
        });
      }
    });

    test('project open/close cycles complete with the assistant running', async ({
      rstudioPage: page,
    }) => {
      // Each cycle is two session restarts (open + close), both landing while
      // the just-started backend/agent are seconds old. Budget generously:
      // a healthy cycle is ~30-45s on a slow CI worker.
      test.setTimeout(120000 * RESTART_CYCLES);

      for (let i = 0; i < RESTART_CYCLES; i++) {
        // Both helpers block through the restart they drive: openProject only
        // returns once the bridge reports the new project active and the
        // console idle, throwing a purpose-built diagnostic otherwise. A
        // wedged restart therefore dies INSIDE one of these calls -- no extra
        // assertions are needed (or reachable) between them.
        await createAndOpenProject(page, sandbox.dir, `agent_restart_${i}`);
        await closeProjectIfOpen(page);
      }
    });
  },
);
