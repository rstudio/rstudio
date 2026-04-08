import { test, expect } from '@fixtures/rstudio.fixture';
import { TIMEOUTS, sleep, CODE_SUGGESTION_PROVIDERS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { AssistantOptionsActions } from '@actions/assistant_options.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { SourcePane } from '@pages/source_pane.page';
import type { Page } from 'playwright';

// --- Content building blocks ---

const YAML_PREAMBLE = '---\\ntitle: Test Copilot\\nformat: html\\n---\\n';
const CHUNK_START = '```{r}\\n';

const LYRICS_COMMENTS = [
  '# What good is sitting',
  '# Alone in your room',
  '# Come hear the music play',
  '# Life is a cabaret, old chum',
];

const LYRICS_CODE = [
  'What good is sitting',
  'Alone in your room',
  'Come hear the music play',
  'Life is a cabaret, old chum',
];

const PARTIAL_COMMENTS = [
  '# Put down the knitting',
  '# The book and the broom',
  '# Time for a holiday',
  '# Life is a cabaret, old chum',
  '# Come to the ',
];

const PARTIAL_CODE = [
  'Put down the knitting',
  'The book and the broom',
  'Time for a holiday',
  'Life is a cabaret, old chum',
  'Come to the ',
];

// --- Test case definitions ---

interface GhostTextTestCase {
  name: string;
  slug: string;
  ext: string;
  preamble: string;
  fullLines: string[];
  partialLines: string[];
}

const testCases: GhostTextTestCase[] = [
  // RMarkdown
  { name: 'RMarkdown body', slug: 'rmd_body', ext: 'rmd', preamble: YAML_PREAMBLE, fullLines: LYRICS_COMMENTS, partialLines: PARTIAL_COMMENTS },
  { name: 'RMarkdown chunk comments', slug: 'rmd_chunk_comment', ext: 'rmd', preamble: YAML_PREAMBLE + '\\n' + CHUNK_START, fullLines: LYRICS_COMMENTS, partialLines: PARTIAL_COMMENTS },
  { name: 'RMarkdown chunk code', slug: 'rmd_chunk_code', ext: 'rmd', preamble: YAML_PREAMBLE + '\\n' + CHUNK_START, fullLines: LYRICS_CODE, partialLines: PARTIAL_CODE },
  // Quarto
  { name: 'Quarto body', slug: 'quarto_body', ext: 'qmd', preamble: YAML_PREAMBLE, fullLines: LYRICS_COMMENTS, partialLines: PARTIAL_COMMENTS },
  { name: 'Quarto chunk comments', slug: 'quarto_chunk_comment', ext: 'qmd', preamble: YAML_PREAMBLE + '\\n' + CHUNK_START, fullLines: LYRICS_COMMENTS, partialLines: PARTIAL_COMMENTS },
  { name: 'Quarto chunk code', slug: 'quarto_chunk_code', ext: 'qmd', preamble: YAML_PREAMBLE + '\\n' + CHUNK_START, fullLines: LYRICS_CODE, partialLines: PARTIAL_CODE },
  // R
  { name: 'R comments', slug: 'r_comments', ext: 'r', preamble: '', fullLines: LYRICS_COMMENTS, partialLines: PARTIAL_COMMENTS },
  { name: 'R code', slug: 'r_code', ext: 'r', preamble: '', fullLines: LYRICS_CODE, partialLines: PARTIAL_CODE },
  // Python
  { name: 'Python comments', slug: 'python_comments', ext: 'py', preamble: '', fullLines: LYRICS_COMMENTS, partialLines: PARTIAL_COMMENTS },
  { name: 'Python code', slug: 'python_code', ext: 'py', preamble: '', fullLines: LYRICS_CODE, partialLines: PARTIAL_CODE },
];

// --- Helpers ---

async function typeLines(page: Page, lines: string[], pressEnterAfterLast: boolean) {
  for (let i = 0; i < lines.length; i++) {
    await page.keyboard.type(lines[i]);
    if (i < lines.length - 1 || pressEnterAfterLast) {
      await sleep(200);
      await page.keyboard.press('Enter');
    }
  }
}

async function waitForGhostText(sourcePane: SourcePane): Promise<string> {
  await expect(sourcePane.ghostText.first()).toBeVisible({ timeout: TIMEOUTS.ghostText });
  const parts = await sourcePane.ghostText.allTextContents();
  const text = parts.join('');
  expect(text.length).toBeGreaterThan(0);
  return text;
}

async function acceptGhostText(page: Page, sourcePane: SourcePane) {
  await page.keyboard.press('ControlOrMeta+;');
  await sleep(2000);
  await expect(sourcePane.ghostText).toHaveCount(0, { timeout: 5000 });
}

// --- Tests ---

for (const [key, provider] of Object.entries(CODE_SUGGESTION_PROVIDERS)) {
  test.describe(`${provider} › Ghost text by file type`, () => {
    const prefix = provider.toLowerCase().replace(/\s+/g, '_');

    let consoleActions: ConsolePaneActions;
    let assistantActions: AssistantOptionsActions;
    let sourceActions: SourcePaneActions;
    let sourcePane: SourcePane;

    test.beforeAll(async ({ rstudioPage: page }) => {
      consoleActions = new ConsolePaneActions(page);
      assistantActions = new AssistantOptionsActions(page, consoleActions);
      sourceActions = new SourcePaneActions(page, consoleActions);
      sourcePane = sourceActions.sourcePane;

      await consoleActions.closeAllBuffersWithoutSaving();
      await sleep(1000);

      await assistantActions.setupAssistantOptions(provider);
    });

    for (const tc of testCases) {
      test(tc.name, async ({ rstudioPage: page }) => {
        if (key === 'posit-assistant') test.fixme(true, 'Posit AI does not reliably produce ghost text for non-code content');

        const fileName = `${prefix}_${tc.slug}_${Date.now()}.${tc.ext}`;

        // --- Full-line ghost text ---
        await sourceActions.createAndOpenFile(fileName, tc.preamble);
        await sleep(1000);

        await sourceActions.goToEnd();

        await typeLines(page, tc.fullLines, true);

        const ghostText1 = await waitForGhostText(sourcePane);
        console.log(`  Ghost text 1 (${tc.name}): "${ghostText1}"`);

        await acceptGhostText(page, sourcePane);
        await expect(sourcePane.contentPane).toContainText(ghostText1, { timeout: 5000 });

        await sourceActions.closeSourceAndDeleteFile(fileName);

        // --- Partial-line completion ---
        const fileName2 = `${prefix}_${tc.slug}_${Date.now()}.${tc.ext}`;

        await sourceActions.createAndOpenFile(fileName2, tc.preamble);
        await sleep(1000);

        await sourceActions.goToEnd();

        await typeLines(page, tc.partialLines, false);

        const ghostText2 = await waitForGhostText(sourcePane);
        console.log(`  Ghost text 2 (${tc.name}): "${ghostText2}"`);

        await acceptGhostText(page, sourcePane);
        await expect(sourcePane.contentPane).toContainText(ghostText2, { timeout: 5000 });

        await sourceActions.closeSourceAndDeleteFile(fileName2);
      });
    }

    // Parens test: unique pattern, both parts build on each other in the same file
    test('R code with parentheses', async ({ rstudioPage: page }) => {
      if (key === 'posit-assistant') test.fixme(true, 'Posit AI does not reliably produce ghost text for non-code content');
      const fileName = `${prefix}_r_parens_${Date.now()}.r`;

      await sourceActions.createAndOpenFile(fileName, '');
      await sleep(1000);

      await sourcePane.contentPane.click();

      await page.keyboard.type('a1 (abc)');
      await sleep(200);
      await page.keyboard.press('Enter');
      await page.keyboard.type('b2 (def)');
      await sleep(200);
      await page.keyboard.press('Enter');
      await page.keyboard.type('c3 (gh');

      const ghostText = await waitForGhostText(sourcePane);
      console.log(`  Ghost text (parens): "${ghostText}"`);
      expect(ghostText).toContain('i');

      await acceptGhostText(page, sourcePane);
      await expect(sourcePane.contentPane).toContainText('c3 (ghi)', { timeout: 5000 });

      await sourceActions.closeSourceAndDeleteFile(fileName);
    });

    test.afterAll(async () => {
      await consoleActions.closeAllBuffersWithoutSaving();
    });
  });
}
