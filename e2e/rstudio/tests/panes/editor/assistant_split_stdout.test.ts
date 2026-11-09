// Posit Assistant messages whose header and body arrive in separate stdout
// reads (#19520). The language server writes the two separately, so a read can
// land between them; rsession used to parse each read on its own and drop such
// messages. That race is rare, so this suite swaps in a proxy agent
// (resources/split-stdout-agent.cjs) that relays the real language server with
// a pause between every header and its body, so every message is split.

import path from 'path';
import type { Page } from 'playwright';
import { test, expect } from '@fixtures/rstudio.fixture';
import { CODE_SUGGESTION_PROVIDERS, sleep } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { useSuiteSandbox } from '@utils/sandbox';
import { resetSourcePaneState, setPref } from '@utils/commands';
import { rPathLiteral } from '@utils/r';
import { isExternalServerRun } from '@utils/auth';
import {
  failUnlessAiServiceGone,
  hasAiCredentials,
  requireAiCredentials,
} from '@utils/ai-credentials';

const PROXY_AGENT = path.resolve(__dirname, 'resources', 'split-stdout-agent.cjs');
const FILE_NAME = 'split_stdout_ghost_text.R';
const COUNT_FILE = 'split-agent-count';

// The running agent's language server script: the argument of the node child
// of this rsession that runs dist/nes/language-server.
const FIND_AGENT_R =
  'local({ rows <- trimws(system2("ps", c("-A", "-o", "ppid=", "-o", "args="), stdout = TRUE)); ' +
  'mine <- rows[startsWith(rows, paste0(Sys.getpid(), " "))]; ' +
  'hits <- regmatches(mine, regexpr("/\\\\S+/dist/nes/language-server\\\\.c?js", mine)); ' +
  'if (length(hits)) hits[[1]] else "" })';

test.describe('Posit Assistant output split across reads', { tag: ['@ai'] }, () => {
  requireAiCredentials(test, 'positai');
  // The proxy is a file in this checkout, which an external server's
  // rsession can't read.
  test.skip(isExternalServerRun(), 'needs the proxy agent on the rsession host');
  test.skip(process.platform === 'win32', 'locates the agent with ps');

  useSuiteSandbox();

  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  const typeTriggerOnNewLine = async (page: Page, trigger: string) => {
    await sourceActions.goToEnd();
    await page.keyboard.press('Enter');
    await page.keyboard.type(trigger);
    const popup = page.locator('#rstudio_popup_completions');
    await popup.waitFor({ state: 'visible', timeout: 1500 }).catch(() => {});
    if (await popup.isVisible().catch(() => false))
      await page.keyboard.press('Escape');
  };

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    if (!(await hasAiCredentials('positai')))
      return;

    await resetSourcePaneState(page);
    await new AssistantOptionsActions(page, consoleActions)
      .setupAssistantOptions(CODE_SUGGESTION_PROVIDERS['posit-assistant']);
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await consoleActions.executeInConsole(
      'Sys.unsetenv(c("RSTUDIO_AGENT_PATH", "SPLIT_AGENT_TARGET", "SPLIT_AGENT_COUNT_FILE")); ' +
      'invisible(.Call("rs_assistantStopAgent", PACKAGE = "(embedding)"))',
      { wait: true },
    );
    // The pref defaults to "posit", so it must be set to "none" explicitly
    // to keep later suites from racing real completions.
    await setPref(page, 'assistant', 'none');
    await resetSourcePaneState(page);
  });

  const FILE_CONTENT =
    'calculate_total <- function(price, tax_rate) {\n' +
    '  total <- price + (price * tax_rate)\n' +
    '  return(total)\n' +
    '}\n' +
    '\n' +
    'result <- calculate_total(100, 0.08)\n';

  const expectGhostTextInNewFile = async (page: Page) => {
    await sourceActions.createAndOpenFile(FILE_NAME, FILE_CONTENT);
    await sleep(1000);
    await typeTriggerOnNewLine(page, 'x <- func');
    try {
      await sourceActions.waitForGhostText();
    } catch (err) {
      await failUnlessAiServiceGone('positai', err);
    }
    await sourceActions.closeSourceAndDeleteFile(FILE_NAME);
  };

  test('ghost text arrives when every message is split', async ({ rstudioPage: page }) => {
    // A suggestion from the real agent first: it starts the agent, and shows
    // the assistant works here before the proxy is involved.
    await test.step('real agent', () => expectGhostTextInNewFile(page));

    expect(
      await consoleActions.evalRLogical(`nzchar(.pw_agent <- ${FIND_AGENT_R})`),
      'the Posit Assistant language server is a child of this rsession',
    ).toBe(true);

    await consoleActions.executeInConsole(
      `Sys.setenv(RSTUDIO_AGENT_PATH = ${rPathLiteral(PROXY_AGENT)}, ` +
      'SPLIT_AGENT_TARGET = .pw_agent, ' +
      `SPLIT_AGENT_COUNT_FILE = file.path(getwd(), "${COUNT_FILE}")); ` +
      `unlink("${COUNT_FILE}"); ` +
      'stopifnot(.Call("rs_assistantStopAgent", PACKAGE = "(embedding)"))',
      { wait: true },
    );

    // The editor stops asking for suggestions once the agent has stopped, so
    // start it again, now as the proxy, the way a change of assistant does.
    await setPref(page, 'assistant', 'none');
    await setPref(page, 'assistant', 'posit');

    await test.step('split agent', () => expectGhostTextInNewFile(page));

    // The suggestion came through the proxy, not a stray real agent.
    expect(await consoleActions.evalRLogical(
      `isTRUE(as.integer(readLines("${COUNT_FILE}", warn = FALSE)) > 0L)`,
    )).toBe(true);
  });
});
