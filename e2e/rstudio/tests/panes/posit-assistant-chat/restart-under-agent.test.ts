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
// Desktop-only: server mode shares one config/data home across workers, so
// flipping the provider prefs here would race @chat suites in other workers.
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

      // Select Posit Assistant as the chat provider through the Options
      // dialog. This settles the install/update prompt, so it also guarantees
      // the backend is actually installed in this worker's data home -- the
      // prerequisite for anything below to exercise the shutdown race.
      const { assistantActions } = createChatActions(page);
      await assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);

      // Turn on the code assistant too: the chat provider starts the chat
      // backend, this starts the NES agent. The wedged CI sessions ran both.
      await setPref(page, 'assistant', 'posit');

      // Guard against silently testing nothing: the chat backend writes
      // positai.log into the session's log directory as it starts. If it
      // never appears, the agent isn't running and the loop below would just
      // exercise plain restarts.
      const logDir = rstudioSession.logDir;
      expect(logDir, 'desktop session should expose its log directory').toBeTruthy();
      const backendLog = path.join(logDir!, 'positai.log');
      await expect
        .poll(() => fs.existsSync(backendLog), {
          message: `chat backend never started (no ${backendLog})`,
          timeout: 60000,
        })
        .toBe(true);
    });

    test.afterAll(async ({ rstudioPage: page }) => {
      await closeProjectIfOpen(page).catch(() => {});
      await setPref(page, 'assistant', 'none');
      await setPref(page, 'chat_provider', 'none');
    });

    test('project open/close cycles complete with the assistant running', async ({
      rstudioPage: page,
    }) => {
      // Each cycle is two session restarts (open + close), both landing while
      // the just-started backend/agent are seconds old. Budget generously:
      // a healthy cycle is ~30-45s on a slow CI worker.
      test.setTimeout(120000 * RESTART_CYCLES);

      for (let i = 0; i < RESTART_CYCLES; i++) {
        const projectDir = await createAndOpenProject(page, sandbox.dir, `agent_restart_${i}`);
        expect(projectDir).toContain(`agent_restart_${i}`);

        // The new project session must come up far enough for the bridge to
        // report the project active -- this is where the wedged shards died.
        await expect
          .poll(() => page.evaluate(() => window.rstudio?.project?.isActive() ?? false), {
            timeout: 60000,
          })
          .toBe(true);

        await closeProjectIfOpen(page);
      }
    });
  },
);
