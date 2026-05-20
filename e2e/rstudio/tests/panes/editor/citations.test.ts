import { test, expect } from '@fixtures/rstudio.fixture';
import { sleep, TIMEOUTS } from '@utils/constants';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { InsertCitationModal } from '@pages/citations.page';
import { useSuiteSandbox } from '@utils/sandbox';

const FILE_TYPES = [
  { type: 'markdown', label: 'Markdown' },
  { type: 'quarto', label: 'Quarto' },
] as const;

type CitationSource = {
  name: string;
  modalKey: 'fromDoiLabel' | 'crossrefLabel' | 'dataCiteLabel' | 'pubMedLabel' | 'rPackageLabel';
  doi: string;
};

const CITATION_SOURCES: CitationSource[] = [
  { name: 'DataCite', modalKey: 'dataCiteLabel', doi: '10.5281/ZENODO.4266706' },
  { name: 'DOI',      modalKey: 'fromDoiLabel',  doi: '10.3133/93888' },
  { name: 'Crossref', modalKey: 'crossrefLabel', doi: '10.3133/93888' },
  { name: 'PubMed',   modalKey: 'pubMedLabel',   doi: '10.3133/93888' },
  { name: 'R Package', modalKey: 'rPackageLabel', doi: '' },
];

test.describe('Citations', () => {
  useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let modal: InsertCitationModal;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    modal = new InsertCitationModal(page);
    await consoleActions.closeAllBuffersWithoutSaving();
    await consoleActions.ensurePackages(['rmarkdown']);
    await consoleActions.clearConsole();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    // Dismiss any stuck Insert Citation dialog. Try Cancel first (more
    // reliable than Escape against the GWT glass), then fall back to Escape.
    try {
      await modal.cancelBtn.first().click({ timeout: 2000, force: true });
    } catch {
      await page.keyboard.press('Escape').catch(() => {});
    }
    await consoleActions.closeAllBuffersWithoutSaving();
    await consoleActions.typeInConsole('unlink(c("references.bib", "test.md", "test.qmd"))');
    await sleep(500);
  });

  /** Open the visual-editor Insert > Citation dialog. */
  async function openCitationDialog(): Promise<void> {
    await sourceActions.sourcePane.visualInsertMenu.click();
    await sourceActions.sourcePane.visualInsertCitation.click();
  }

  /**
   * Type a query into the citation search input and wait for a result. The
   * modal uses latent (auto-triggered) search -- no submit button. Pressing
   * Enter explicitly to force submission if the latent debounce doesn't fire.
   * Returns true if a citation result appears within the search timeout;
   * false on API failure.
   */
  async function searchForCitation(query: string): Promise<boolean> {
    await modal.searchInput.click({ force: true });
    // Per-char delay -- the latent-search input drops the trailing character
    // when typed too fast (observed default-speed pressSequentially leaving
    // off the last char).
    await modal.searchInput.pressSequentially(query, { delay: 50 });
    await sleep(3000);
    await consoleActions.page.screenshot({ path: `debug-search-${Date.now()}.png`, fullPage: true });
    try {
      await expect(modal.citationItem.first()).toBeVisible({ timeout: TIMEOUTS.citationSearch });
      await sleep(1000);
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Selects a citation in the modal via the given source. For DOI-based
   * sources, performs a search; for R Package, picks the first package item.
   * Returns true on success; false if the external lookup failed.
   */
  async function stageCitation(source: CitationSource): Promise<boolean> {
    await modal[source.modalKey].click();
    if (source.doi.length > 0) {
      if (!(await searchForCitation(source.doi))) return false;
      await modal.citationItem.first().click();
    } else {
      // R Package source shows the installed-package list; pick the first.
      const firstPackage = modal.rPackageCitationItem.first();
      await expect(firstPackage).toBeVisible({ timeout: 30000 });
      await firstPackage.click();
      await expect(modal.stagedCitationItem.first()).toBeVisible();
    }
    return true;
  }

  /** Create a visual-editor doc of the given type with Quarto YAML navigated past. */
  async function createDocWithVisualEditor(fileType: 'markdown' | 'quarto'): Promise<void> {
    if (fileType === 'markdown') {
      await sourceActions.createMarkdownWithVisualEditor();
    } else {
      await sourceActions.createQuartoWithVisualEditor();
      await sourceActions.navigateOutOfQuartoYaml();
    }
  }

  test('no duplicate citations across DOI and DataCite sources', async () => {
    // Original: test_desktop_Citations.py::test_no_duplicate_citations
    // Addresses https://github.com/rstudio/rstudio/issues/8335
    await sourceActions.createMarkdownWithVisualEditor();

    // First insert via DOI
    await openCitationDialog();
    await modal.fromDoiLabel.click();
    test.skip(!(await searchForCitation('10.5281/ZENODO.4266706')), 'Citation search API unavailable');
    await modal.citationItem.first().click();
    await modal.insertBtn.first().click();
    await expect(sourceActions.sourcePane.visualEditorContent).toContainText('@bermúdez2020', { timeout: 10000 });

    // Second insert via DataCite (same DOI) — should dedupe in bib but render twice in editor
    await openCitationDialog();
    await modal.dataCiteLabel.click();
    test.skip(!(await searchForCitation('10.5281/ZENODO.4266706')), 'Citation search API unavailable');
    await modal.citationItem.first().click();
    await modal.insertBtn.first().click();

    await expect(sourceActions.sourcePane.visualEditorContent).toContainText('[@bermúdez2020][@bermúdez2020]', { timeout: 10000 });

    // Verify references.bib has a single entry with the expected fields
    await consoleActions.clearConsole();
    await consoleActions.typeInConsole('writeLines(readLines("references.bib"))');
    await sleep(1000);
    const bibOutput = consoleActions.consolePane.consoleOutput;
    await expect(bibOutput).toContainText('bermúdez2020', { timeout: 10000 });
    await expect(bibOutput).toContainText('10.5281/ZENODO.4266706');
    await expect(bibOutput).toContainText('2020');
  });

  for (const fileType of FILE_TYPES) {
    test(`add DOI citation writes bib (${fileType.type})`, async () => {
      // Original: test_desktop_Citations.py::test_add_citation_no_author_${fileType.type}
      await createDocWithVisualEditor(fileType.type);

      await openCitationDialog();
      await modal.fromDoiLabel.click();
      test.skip(!(await searchForCitation('10.3133/93888')), 'Citation search API unavailable');
      await modal.citationItem.first().click();
      await modal.insertBtn.first().click();
      await expect(sourceActions.sourcePane.visualEditorContent).toContainText('@effects1999', { timeout: 10000 });

      // Verify bib content
      await consoleActions.clearConsole();
      await consoleActions.typeInConsole('writeLines(readLines("references.bib"))');
      await sleep(1000);
      const bibOutput = consoleActions.consolePane.consoleOutput;
      await expect(bibOutput).toContainText('effects1999', { timeout: 10000 });
      await expect(bibOutput).toContainText('10.3133/93888');
    });
  }

  for (const fileType of FILE_TYPES) {
    for (const source of CITATION_SOURCES) {
      test(`delete ${source.name} citation (${fileType.type})`, async () => {
        // Original: test_desktop_Citations.py::test_delete_${source.name.toLowerCase()}_citation_${fileType.type}
        // Addresses https://github.com/rstudio/rstudio/issues/9124
        await createDocWithVisualEditor(fileType.type);
        await openCitationDialog();

        const staged = await stageCitation(source);
        test.skip(!staged, 'Citation search API unavailable');

        await modal.deleteCitationBtn.first().click();
        await expect(modal.stagedCitationItem).not.toBeVisible();
        await modal.cancelBtn.first().click();
      });
    }
  }

  for (const fileType of FILE_TYPES) {
    test(`citation is not escaped in source mode (${fileType.type})`, async ({ rstudioPage: page }) => {
      // Original: test_desktop_Citations.py::test_dont_escape_citation_${fileType.type}
      // Addresses https://github.com/rstudio/rstudio/issues/10075
      const fileName = fileType.type === 'markdown' ? 'test.md' : 'test.qmd';
      const content = 'This is some text [@abelsen1993, 93].';

      await consoleActions.typeInConsole(`writeLines("${content}", con = "${fileName}")`);
      await sleep(500);
      await consoleActions.typeInConsole(`.rs.api.documentOpen("${fileName}")`);
      await expect(sourceActions.sourcePane.selectedTab).toContainText(fileName, { timeout: TIMEOUTS.fileOpen });

      await sourceActions.ensureVisualMode();
      await expect(sourceActions.sourcePane.visualEditorContent).toContainText(content, { timeout: 10000 });

      // Toggle back to source mode and verify the citation is not escaped
      await sourceActions.sourcePane.visualMdToggle.click();
      await sleep(1000);
      const sourceText = await sourceActions.sourcePane.contentPane.innerText();
      expect(sourceText).toContain(content);
      expect(sourceText).not.toContain('\\[@abelsen1993');
    });
  }

  for (const fileType of FILE_TYPES) {
    test(`blank citation dialog dismisses on insert (${fileType.type})`, async () => {
      // Original: test_desktop_Citations.py::test_blank_citation_${fileType.type}
      // Addresses https://github.com/rstudio/rstudio/issues/12833
      await createDocWithVisualEditor(fileType.type);

      await openCitationDialog();
      await expect(modal.insertBtn).toBeVisible({ timeout: 10000 });
      await modal.insertBtn.click();
      await expect(modal.insertBtn).not.toBeVisible({ timeout: 5000 });
    });
  }
});
