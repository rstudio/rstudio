import { homedir } from 'os';
import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, CHAT_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { ChatPaneActions } from '@actions/chat_pane.actions';
import { ChatPane } from '@pages/chat_pane.page';
import type { EnvironmentVersions } from '@pages/console_pane.page';

// ---------------------------------------------------------------------------
// Project-level skill (lives in .positai/skills/ under the workspace root)
// ---------------------------------------------------------------------------

const PROJECT_SKILL_NAME = 'custom-data-summary';
const PROJECT_SKILL_DIR = `.positai/skills/${PROJECT_SKILL_NAME}`;
const PROJECT_SKILL_PATH = `${PROJECT_SKILL_DIR}/SKILL.md`;
const PROJECT_MARKER = 'PROJECT_SKILL_ACTIVE_QA7742';

// ---------------------------------------------------------------------------
// User-level skill (lives in ~/.positai/skills/ under the user home dir)
//
// On Windows, R's ~ expands to Documents, not the actual home directory.
// Databot uses Node's os.homedir() for tilde expansion, so we must use
// the same value to place the file where databot will look.
// ---------------------------------------------------------------------------

const USER_HOME = homedir().replace(/\\/g, '/');
const USER_SKILL_NAME = 'custom-code-review';
const USER_SKILL_DIR = `${USER_HOME}/.positai/skills/${USER_SKILL_NAME}`;
const USER_SKILL_PATH = `${USER_SKILL_DIR}/SKILL.md`;
const USER_MARKER = 'USER_SKILL_REVIEW_QA8853';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Create a SKILL.md file via two short R console commands.
 *
 * Long single commands through pressSequentially are fragile (autocomplete
 * interference, timing issues). Instead we use two short commands:
 * 1. dir.create() for the directory
 * 2. writeLines(c(...)) for the file content (kept minimal)
 */
async function createSkillFile(
  consoleActions: ConsolePaneActions,
  dir: string,
  filePath: string,
  name: string,
  description: string,
  marker: string,
): Promise<void> {
  await consoleActions.typeInConsole(`dir.create("${dir}", recursive = TRUE)`);
  await sleep(1000);

  // Keep content minimal to stay under ~300 chars for pressSequentially reliability
  const cmd =
    `writeLines(c("---", "name: ${name}", "description: ${description}", "---", "", "Start with: ${marker}", "Markers are MANDATORY."), "${filePath}")`;
  await consoleActions.typeInConsole(cmd);
  await sleep(1000);
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

test.describe.serial('User-Added Skills', { tag: ['@serial'] }, () => {
  let chatPane: ChatPane;
  let chatActions: ChatPaneActions;
  let consoleActions: ConsolePaneActions;
  let assistantActions: AssistantOptionsActions;
  let versions: EnvironmentVersions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    assistantActions = new AssistantOptionsActions(page, consoleActions);
    chatActions = new ChatPaneActions(page, consoleActions);
    chatPane = chatActions.chatPane;

    versions = await consoleActions.getEnvironmentVersions();
    console.log(`R: ${versions.r}, RStudio: ${versions.rstudio}`);
    await consoleActions.clearConsole();

    // -----------------------------------------------------------------------
    // Step 1: Stop any running backend FIRST.
    //
    // The default chat_provider is "posit", so the backend auto-starts when
    // RStudio launches. We must stop it before creating skill files, because
    // skills are only discovered during DatabotCore.initialize() (at process
    // start). Setting the preference to "none" (a valid enum value -- NOT "")
    // triggers onChatProviderChanged() → stopBackend() on the GWT side.
    // -----------------------------------------------------------------------
    console.log('Stopping chat backend (setting chat_provider to "none")');
    await consoleActions.typeInConsole('.rs.api.writeRStudioPreference("chat_provider", "none")');
    await sleep(5000);

    // -----------------------------------------------------------------------
    // Step 2: Create a project-level skill (.positai/skills/ in workspace)
    // -----------------------------------------------------------------------
    console.log(`Creating project skill at: ${PROJECT_SKILL_PATH}`);
    await createSkillFile(
      consoleActions,
      PROJECT_SKILL_DIR,
      PROJECT_SKILL_PATH,
      PROJECT_SKILL_NAME,
      'Summarize or describe a dataset.',
      PROJECT_MARKER,
    );

    // -----------------------------------------------------------------------
    // Step 3: Create a user-level skill (~/.positai/skills/ in home dir)
    // -----------------------------------------------------------------------
    console.log(`Creating user skill at: ${USER_SKILL_PATH}`);
    await createSkillFile(
      consoleActions,
      USER_SKILL_DIR,
      USER_SKILL_PATH,
      USER_SKILL_NAME,
      'Review or critique a code snippet.',
      USER_MARKER,
    );

    // -----------------------------------------------------------------------
    // Step 4: Verify both files exist and have correct content
    // -----------------------------------------------------------------------
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole(`cat(readLines("${PROJECT_SKILL_PATH}"), sep = "\\n")`);
    await sleep(2000);
    const projectOutput = await consoleActions.consolePane.consoleOutput.innerText();
    console.log(`Project skill content:\n${projectOutput}`);

    await consoleActions.clearConsole();
    await consoleActions.typeInConsole(`cat(readLines("${USER_SKILL_PATH}"), sep = "\\n")`);
    await sleep(2000);
    const userOutput = await consoleActions.consolePane.consoleOutput.innerText();
    console.log(`User skill content:\n${userOutput}`);

    // -----------------------------------------------------------------------
    // Step 5: Start a fresh backend by setting chat_provider back to "posit".
    //
    // This triggers onChatProviderChanged() → checkForUpdates() → startBackend()
    // The new backend process runs DatabotCore.initialize() → discoverSkills()
    // which picks up our newly created skill files.
    // -----------------------------------------------------------------------
    console.log('Starting fresh backend via Options dialog');
    await assistantActions.setChatProvider(CHAT_PROVIDERS['posit-assistant']);

    // -----------------------------------------------------------------------
    // Step 6: Open the chat pane and wait for the UI to be ready
    // -----------------------------------------------------------------------
    await chatActions.openChatPane();
    await chatActions.dismissSetupPrompts();
  });

  test.beforeEach(async () => {
    test.info().annotations.push(
      { type: 'R version', description: versions.r },
      { type: 'RStudio version', description: versions.rstudio },
    );
  });

  test.afterAll(async () => {
    // Clean up the project-level skill directory
    await consoleActions.typeInConsole('unlink(".positai", recursive = TRUE)');
    await sleep(1000);

    // Clean up only the specific user-level skill we created (leave ~/.positai/ intact)
    await consoleActions.typeInConsole(`unlink("${USER_SKILL_DIR}", recursive = TRUE)`);
    await sleep(1000);
  });

  test('both custom skills are discovered by assistant', async () => {
    await chatActions.startNewConversation();

    const initialCount = await chatPane.getMessageCount();
    await chatActions.sendChatMessage(
      'List all Agent Skills that are currently available to you. ' +
      'Include the exact name of each skill.'
    );
    const newCount = await chatActions.waitForResponse(initialCount);
    expect(newCount).toBeGreaterThan(initialCount);

    // The response should mention both custom skills by name
    const lastMessage = chatPane.messageItem.last();
    await expect(lastMessage).toContainText(PROJECT_SKILL_NAME, { timeout: 10000 });
    await expect(lastMessage).toContainText(USER_SKILL_NAME, { timeout: 10000 });
  });

  test('project-level skill markers appear in response', async () => {
    await chatActions.startNewConversation();

    const initialCount = await chatPane.getMessageCount();
    await chatActions.sendChatMessage(
      'Summarize the iris dataset. ' +
      'Use your available Agent Skills. ' +
      'Do not run any code.'
    );

    // Wait for the full response, handling any Allow dialogs that appear
    await chatActions.pollWithAllowDialogs(async () => {
      const isStillProcessing = await chatPane.isStopButtonVisible();
      const messageCount = await chatPane.getMessageCount();
      return !isStillProcessing && messageCount > initialCount;
    }, 120000);

    // Verify the response contains the markers from our project skill
    const lastMessage = chatPane.messageItem.last();
    const responseText = await lastMessage.innerText();

    console.log(`Project skill response preview: ${responseText.substring(0, 500)}`);
    expect(responseText).toContain(PROJECT_MARKER);
  });

  test('user-level skill markers appear in response', async () => {
    await chatActions.startNewConversation();

    const initialCount = await chatPane.getMessageCount();
    await chatActions.sendChatMessage(
      'Review this R code snippet and provide feedback: x <- 1:10; mean(x). ' +
      'Use your available Agent Skills. ' +
      'Do not run any code.'
    );

    // Wait for the full response, handling any Allow dialogs that appear
    await chatActions.pollWithAllowDialogs(async () => {
      const isStillProcessing = await chatPane.isStopButtonVisible();
      const messageCount = await chatPane.getMessageCount();
      return !isStillProcessing && messageCount > initialCount;
    }, 120000);

    // Verify the response contains the marker from our user-level skill
    const lastMessage = chatPane.messageItem.last();
    const responseText = await lastMessage.innerText();

    console.log(`User skill response preview: ${responseText.substring(0, 500)}`);
    expect(responseText).toContain(USER_MARKER);
  });
});
