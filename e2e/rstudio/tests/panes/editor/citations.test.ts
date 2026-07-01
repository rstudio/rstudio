import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { InsertCitationDialog, CITATION_SOURCES } from '@pages/insert_citation.page';
import { installDepIfPrompted } from '@pages/modals.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand } from '@utils/commands';
import type { Page } from 'playwright';

// Migrated from Selenium test_desktop_Citations.py. Citations live in the
// panmirror visual editor's Insert menu; see the InsertCitationDialog page
// object and visual-editor.md for the dialog's selectors and the one-time init
// reset these tests work around.
test.describe('Citations', () => {
  // Inserting a citation writes references.bib to the working directory;
  // useSuiteSandbox() redirects cwd into the per-run sandbox so nothing lands
  // in the repo tree or home dir. The globalTeardown removes it.
  useSuiteSandbox();

  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
  });

  // Each test starts from a single clean Untitled tab; resetToUntitled reverts
  // the previous test's dirty doc without a save prompt.
  test.beforeEach(async () => {
    await consoleActions.resetSourcePane();
  });

  // Open a fresh markdown document in visual mode (the only place citations are
  // reachable). Plain markdown is the cheapest host -- no rmarkdown package, no
  // Quarto new-doc wizard.
  async function newMarkdownVisualDoc(page: Page): Promise<void> {
    await executeCommand(page, 'newMarkdownDoc');
    await installDepIfPrompted(page);
    await expect(sourceActions.sourcePane.selectedTab).toContainText('Untitled', { timeout: 20000 });
    await sourceActions.ensureVisualMode();
  }

  test('inserts an authorless DOI citation into a markdown visual editor', async ({ rstudioPage: page }) => {
    await newMarkdownVisualDoc(page);

    const citation = new InsertCitationDialog(page);
    await citation.open();
    const searchBox = await citation.selectSource(CITATION_SOURCES.doi);

    // A DOI resolves to exactly one work via a real service call (no intercept).
    await citation.search(searchBox, '10.3133/93888');
    await citation.stageFirstResult();
    await citation.insert();

    // The citation lands in the document. With no author, panmirror derives the
    // key from the title ("Effects of management practices on grassland birds:
    // Bobolink", 1999) -> effects1999.
    await expect(page.locator('.ProseMirror')).toContainText('[@effects1999]', { timeout: 15000 });
  });

  // The same DOI insert flow works in a Quarto (.qmd) document -- the format
  // where the previous migration attempt stalled ("never reaches visual mode").
  // Opening a file-based .qmd and using ensureVisualMode() gets there reliably.
  test('inserts a DOI citation into a Quarto visual editor', async ({ rstudioPage: page }) => {
    const fileName = `citations_${Date.now()}.qmd`;
    await sourceActions.createAndOpenFile(fileName, '---\ntitle: Citations\n---\n\nGrassland bird research.\n');
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });
    await sourceActions.ensureVisualMode();

    // Citations are disabled while the cursor sits in the YAML; click into the
    // body paragraph first (the analogue of the Selenium navigate_out_of_quarto_yaml).
    await page.locator('.ProseMirror').getByText('Grassland bird research.').click();

    const citation = new InsertCitationDialog(page);
    await citation.open();
    const searchBox = await citation.selectSource(CITATION_SOURCES.doi);
    await citation.search(searchBox, '10.3133/93888');
    await citation.stageFirstResult();
    await citation.insert();

    await expect(page.locator('.ProseMirror')).toContainText('[@effects1999]', { timeout: 15000 });
  });

  test('deletes a staged citation', async ({ rstudioPage: page }) => {
    await newMarkdownVisualDoc(page);

    const citation = new InsertCitationDialog(page);
    await citation.open();
    const searchBox = await citation.selectSource(CITATION_SOURCES.doi);

    // Stage the single DOI result, then remove it from the staging area (#9124).
    await citation.search(searchBox, '10.3133/93888');
    await citation.stageFirstResult();
    await expect(citation.stagedCitations).toHaveCount(1);

    await citation.deleteStagedCitation();
    await expect(citation.stagedCitations).toHaveCount(0);

    await citation.cancel();
  });

  // Inserting the same work via two different sources reuses its citation key
  // rather than creating a duplicate (#8335). The document ends up showing the
  // key twice; a regression would produce a de-duplicated variant like
  // "bermúdez2020a" (and a second bib entry) instead.
  test('does not duplicate a citation inserted from two sources', async ({ rstudioPage: page }) => {
    await newMarkdownVisualDoc(page);
    const citation = new InsertCitationDialog(page);
    const doi = '10.5281/ZENODO.4266706';

    // First insert via From DOI -> [@bermúdez2020].
    await citation.open();
    let searchBox = await citation.selectSource(CITATION_SOURCES.doi);
    await citation.search(searchBox, doi);
    await citation.stageFirstResult();
    await citation.insert();
    await expect(page.locator('.ProseMirror')).toContainText('[@bermúdez2020]', { timeout: 15000 });

    // Insert the same work again via DataCite. It is already in the bibliography,
    // so panmirror reuses the same key -> the document shows it twice.
    await citation.open();
    searchBox = await citation.selectSource(CITATION_SOURCES.datacite);
    await citation.search(searchBox, doi);
    await citation.stageFirstResult();
    await citation.insert();
    await expect(page.locator('.ProseMirror')).toContainText('[@bermúdez2020][@bermúdez2020]', { timeout: 15000 });
  });

  // Clicking Insert with nothing staged should dismiss the dialog rather than
  // leaving it stuck open (#12833).
  test('dismisses the dialog when inserting with nothing staged', async ({ rstudioPage: page }) => {
    await newMarkdownVisualDoc(page);

    const citation = new InsertCitationDialog(page);
    await citation.open();
    await citation.insert();
    await expect(citation.dialog).toBeHidden({ timeout: 5000 });
  });

  // A citation already in the text must survive a visual<->source round-trip
  // without its brackets being backslash-escaped (#10075).
  test('does not escape citation brackets across a visual round-trip', async ({ rstudioPage: page }) => {
    const fileName = `citation_escape_${Date.now()}.md`;
    const line = 'This is some text [@abelsen1993, 93].';
    await sourceActions.createAndOpenFile(fileName, line);
    await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: 20000 });

    // Visual mode renders the citation as-is.
    await sourceActions.ensureVisualMode();
    await expect(page.locator('.ProseMirror')).toContainText(line, { timeout: 15000 });

    // Back in source mode the brackets must remain unescaped (no backslashes).
    await sourceActions.ensureSourceMode();
    await expect(sourceActions.sourcePane.contentPane).toContainText(line, { timeout: 10000 });
    await expect(sourceActions.sourcePane.contentPane).not.toContainText('\\[@abelsen1993');
  });

  // Selecting each network-backed source and searching returns matching results.
  // "bobolink" returns hits on all three services (DataCite, Crossref, PubMed).
  // Listed in the dialog's tree order (top to bottom): Crossref, DataCite, PubMed.
  const SEARCH_SOURCES = [CITATION_SOURCES.crossref, CITATION_SOURCES.datacite, CITATION_SOURCES.pubmed];
  const searchTerm = 'bobolink';
  for (const source of SEARCH_SOURCES) {
    test(`searches ${source.alt} and returns matching results`, async ({ rstudioPage: page }) => {
      await newMarkdownVisualDoc(page);

      const citation = new InsertCitationDialog(page);
      await citation.open();
      const searchBox = await citation.selectSource(source);

      // At least one of the first five results should mention the term, not just
      // that some rows rendered -- otherwise arbitrary or stale hits would pass.
      // We don't require it of result #1: relevance ranking can put a non-title
      // match first (e.g. PubMed ranks a "Bobo-link" correspondence hit above the
      // title matches). Poll to ride out the list still populating.
      await citation.search(searchBox, searchTerm);
      const matcher = new RegExp(searchTerm, 'i');
      await expect
        .poll(async () => (await citation.resultTexts(5)).some((t) => matcher.test(t)), { timeout: 30000 })
        .toBe(true);

      await citation.cancel();
    });
  }
});
