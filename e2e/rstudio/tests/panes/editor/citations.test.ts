import { test, expect } from '@fixtures/rstudio.fixture';
import { externalServerUrl } from '@fixtures/server.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { InsertCitationDialog, CITATION_SOURCES } from '@pages/insert_citation.page';
import { installDepIfPrompted } from '@pages/modals.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { executeCommand } from '@utils/commands';
import { executeInConsole, CONSOLE_OUTPUT } from '@pages/console_pane.page';
import { rStringLiteral } from '@utils/r';
import { CitationServiceMock, RESET_CITATION_SERVICE_OPTIONS, type CitationService } from '@utils/citation-services';
import { sessionsRunLocally } from '@utils/session-liveness';
import { envFlag } from '@utils/env';
import type { Page } from 'playwright';

// PW_CITATION_LIVE=1 leaves the session on the live services, to check that
// fixtures/citation-services.json still describes what they answer.
const LIVE_SERVICES = envFlag('PW_CITATION_LIVE');

// Read the value of an R expression via marker-wrapped console output. Runs on
// the rsession host, so it reads files the IDE wrote (e.g. references.bib in the
// working directory) regardless of where the sandbox lives.
async function captureResult(page: Page, rExpression: string): Promise<string> {
  const marker = `__CIT_${Date.now()}__`;
  await executeInConsole(
    page,
    `cat(${rStringLiteral(marker)}, ${rExpression}, ${rStringLiteral(marker)})`,
    { wait: true },
  );
  const pattern = new RegExp(`${marker}\\s+(.*?)\\s+${marker}`, 's');
  let match: RegExpMatchArray | null = null;
  await expect
    .poll(
      async () => {
        const output = await page.locator(CONSOLE_OUTPUT).innerText();
        match = output.match(pattern);
        return match !== null;
      },
      {
        timeout: 15000,
        message: `captureResult: no marker-wrapped output for \`${rExpression}\` within 15s -- the R expression likely errored (e.g. references.bib missing)`,
      },
    )
    .toBe(true);
  return match![1].trim();
}

// Migrated from Selenium test_desktop_Citations.py. Citations live in the
// panmirror visual editor's Insert menu; see the InsertCitationDialog page
// object and visual-editor.md for the dialog's selectors and the one-time init
// reset these tests work around. The network-backed sources (DOI, Crossref,
// DataCite, PubMed) answer from a local mock of those services (see
// utils/citation-services.ts), or from the live services under
// PW_CITATION_LIVE.
test.describe('Citations', () => {
  // Inserting a citation writes references.bib to the working directory;
  // useSuiteSandbox() redirects cwd into the per-run sandbox so nothing lands
  // in the repo tree or home dir. The globalTeardown removes it.
  useSuiteSandbox();

  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;
  let services: CitationServiceMock;
  let servicesReachable: boolean;
  let usingMock: boolean;

  test.beforeAll(async ({ rstudioPage: page, mode }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    services = await CitationServiceMock.start();
    // The mock listens on this machine's loopback, which only an rsession on
    // this machine can reach.
    servicesReachable = LIVE_SERVICES || sessionsRunLocally(mode, externalServerUrl());
    usingMock = servicesReachable && !LIVE_SERVICES;
  });

  test.afterAll(async () => {
    try {
      if (usingMock)
        await consoleActions.executeInConsole(RESET_CITATION_SERVICE_OPTIONS, { wait: true });
    } finally {
      // Unset if beforeAll failed before the mock started.
      await services?.close();
    }
  });

  // Each test starts from a single clean Untitled tab. resetSourcePane()
  // dispatches resetToUntitled under the hood, reverting the previous test's
  // dirty doc without a save prompt.
  test.beforeEach(async () => {
    await consoleActions.resetSourcePane();
    services.reset();
    // Citation inserts append to references.bib in the shared workdir; remove
    // any leftover so each test's bib assertions start from a clean file. The
    // same command (re)points the session's citation lookups at the mock --
    // only when it can reach the mock, so a remote session that never runs
    // afterAll isn't left with Insert Citation pointed at a dead port.
    const pointAtMock = usingMock ? `; ${services.optionsCall()}` : '';
    await consoleActions.executeInConsole(`unlink("references.bib")${pointAtMock}`, { wait: true });
  });

  function skipUnlessServicesReachable(): void {
    test.skip(!servicesReachable, 'the citation service mock is unreachable from a remote rsession');
  }

  function skipUnlessUsingMock(): void {
    skipUnlessServicesReachable();
    test.skip(LIVE_SERVICES, 'PW_CITATION_LIVE: the live services cannot be made to fail');
  }

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
    skipUnlessServicesReachable();
    await newMarkdownVisualDoc(page);

    const citation = new InsertCitationDialog(page);
    await citation.open();
    const searchBox = await citation.selectSource(CITATION_SOURCES.doi);

    // A DOI resolves to exactly one work.
    await citation.search(searchBox, '10.3133/93888');
    await citation.stageFirstResult();
    await citation.insert();

    // The citation lands in the document. With no author, panmirror derives the
    // key from the title ("Effects of management practices on grassland birds:
    // Bobolink", 1999) -> effects1999.
    await expect(page.locator('.ProseMirror')).toContainText('[@effects1999]', { timeout: 15000 });

    // The citation metadata is persisted to references.bib (in the session's
    // working directory), not just the key placed in the document.
    expect(await captureResult(page, 'file.exists("references.bib")')).toBe('TRUE');
    const bib = await captureResult(page, 'readLines("references.bib")');
    expect(bib).toContain('effects1999');
    expect(bib).toContain('@techreport'); // entry type, not just fields
    expect(bib).toContain('Bobolink');
    expect(bib).toContain('10.3133/93888');
    // No author: the key is title-derived. Guards the fixture too -- a CSL
    // record with an author would change the key and this test's premise.
    expect(bib).not.toMatch(/author\s*=/i);
  });

  // The same DOI insert flow works in a Quarto (.qmd) document -- the format
  // where the previous migration attempt stalled ("never reaches visual mode").
  // Opening a file-based .qmd and using ensureVisualMode() gets there reliably.
  test('inserts a DOI citation into a Quarto visual editor', async ({ rstudioPage: page }) => {
    skipUnlessServicesReachable();
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

  // The R Package source is a typeahead list of installed packages, not a latent
  // search. Insert one and verify it lands in the document -- the only insert
  // test that exercises a non-network source.
  test('inserts an R Package citation into the document', async ({ rstudioPage: page }) => {
    await newMarkdownVisualDoc(page);

    const citation = new InsertCitationDialog(page);
    await citation.open();

    // knitr is pre-installed (REQUIRED_PACKAGES); its cite key is the package name.
    const match = await citation.selectPackageSource('knitr');
    await match.locator('button').click(); // stage via "+"
    await citation.insert();

    await expect(page.locator('.ProseMirror')).toContainText('[@knitr]', { timeout: 15000 });
  });

  test('deletes a staged citation', async ({ rstudioPage: page }) => {
    skipUnlessServicesReachable();
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
    // The same work is inserted first via From DOI, then via DataCite.
    skipUnlessServicesReachable();
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

    // references.bib holds a single entry despite two citations in the document
    // -- the direct #8335 check (one "@" entry header, not two).
    expect(await captureResult(page, 'sum(grepl("^@", readLines("references.bib")))')).toBe('1');
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
  // without its brackets being backslash-escaped (#10075). Checked in both
  // markdown and Quarto: pandoc's markdown and quarto writers escape
  // differently, so the .qmd round-trip is distinct coverage.
  for (const ext of ['md', 'qmd']) {
    test(`does not escape citation brackets across a visual round-trip (.${ext})`, async ({ rstudioPage: page }) => {
      const fileName = `citation_escape_${Date.now()}.${ext}`;
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
      await expect(sourceActions.sourcePane.contentPane).not.toContainText('93\\]');
    });
  }

  // Selecting each network-backed source and searching returns matching results.
  // "bobolink" has hits in every service's fixture (DataCite, Crossref, PubMed).
  // Listed in the dialog's tree order (top to bottom): Crossref, DataCite, PubMed.
  const SEARCH_SOURCES = [CITATION_SOURCES.crossref, CITATION_SOURCES.datacite, CITATION_SOURCES.pubmed];
  const searchTerm = 'bobolink';
  for (const source of SEARCH_SOURCES) {
    test(`searches ${source.alt} and returns matching results`, async ({ rstudioPage: page }) => {
      skipUnlessServicesReachable();
      await newMarkdownVisualDoc(page);

      const citation = new InsertCitationDialog(page);
      await citation.open();
      const searchBox = await citation.selectSource(source);

      // At least one of the first five results should mention the term, not just
      // that some rows rendered -- otherwise arbitrary or stale hits would pass.
      // We don't require it of result #1: relevance ranking can put a non-title
      // match first, and the PubMed fixture keeps such a hit (a "Bobo-link"
      // correspondence) ahead of the title matches.
      await citation.search(searchBox, searchTerm);
      const matcher = new RegExp(searchTerm, 'i');
      await expect
        .poll(async () => (await citation.resultTexts(5)).some((t) => matcher.test(t)), {
          timeout: 30000,
          message: `no ${source.alt} result mentioning "${searchTerm}" within 30s`,
        })
        .toBe(true);

      await citation.cancel();
    });
  }

  // A service-side failure surfaces as the dialog's error status line, not an
  // endless "Searching..." or an empty result list. Each case fails through
  // its own rsession path: doiFetchCSL, crossrefRequest, dataCiteRequest, and
  // both legs of pubMedSearch (esummary runs in a nested continuation). This
  // also proves each lookup goes through the mock: one that fell back to the
  // live service would get results instead. Crossref's panel has no
  // service-specific wording, only the catch-all.
  const SERVICE_FAILURES: { service: CitationService; resource?: string; query: string; error: string }[] = [
    { service: 'doi', query: '10.3133/93888', error: 'An error occurred while searching for this DOI.' },
    { service: 'crossref', query: searchTerm, error: 'An unknown error occurred. Please try again.' },
    { service: 'datacite', query: searchTerm, error: 'An error occurred while searching DataCite.' },
    { service: 'pubmed', query: searchTerm, error: 'An error occurred while searching PubMed.' },
    {
      service: 'pubmed',
      resource: '/entrez/eutils/esummary.fcgi',
      query: searchTerm,
      error: 'An error occurred while searching PubMed.',
    },
  ];
  for (const { service, resource, query, error } of SERVICE_FAILURES) {
    const source = CITATION_SOURCES[service];
    const what = resource ? `${source.alt} ${resource.split('/').pop()}` : source.alt;
    test(`reports a ${what} service failure in the dialog`, async ({ rstudioPage: page }) => {
      skipUnlessUsingMock();
      services.fail(service, 503, resource);
      await newMarkdownVisualDoc(page);

      const citation = new InsertCitationDialog(page);
      await citation.open();
      const searchBox = await citation.selectSource(source);
      await citation.search(searchBox, query);
      await expect(citation.searchError).toHaveText(error, { timeout: 30000 });

      await citation.cancel();
    });
  }

  // The DOI reaches the rsession's download.file() call inside an R string
  // literal; a quote in it must not end the literal. Neither the mock nor
  // doi.org has such a DOI, so a correctly quoted lookup gets "No data", not
  // an R parse error.
  test('looks up a DOI containing a quote', async ({ rstudioPage: page }) => {
    skipUnlessServicesReachable();
    await newMarkdownVisualDoc(page);

    const citation = new InsertCitationDialog(page);
    await citation.open();
    const searchBox = await citation.selectSource(CITATION_SOURCES.doi);
    await citation.search(searchBox, "10.1002/it's");
    await expect(citation.searchNoResults).toHaveText('No data for this DOI could be found.', { timeout: 30000 });

    await citation.cancel();
  });
});
