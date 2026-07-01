import type { Page, Locator } from 'playwright';
import { expect } from '@playwright/test';
import { PageObject } from './page_object_base_classes';

/** A network-backed citation source in the Insert Citation dialog. */
export interface CitationSource {
  /** Navigation-tree image alt text, e.g. 'DataCite'. */
  alt: string;
  /** Latent-search input placeholder, e.g. 'Search DataCite for Citations'. */
  placeholder: string;
}

/**
 * The network-backed sources, in the dialog's tree order (top to bottom). All
 * four render the same latent-search panel (confirmed in the Quarto editor
 * source); only the tree label and placeholder differ.
 */
export const CITATION_SOURCES = {
  doi: { alt: 'From DOI', placeholder: 'Paste a DOI to search' },
  crossref: { alt: 'Crossref', placeholder: 'Search Crossref for Citations' },
  datacite: { alt: 'DataCite', placeholder: 'Search DataCite for Citations' },
  pubmed: { alt: 'PubMed', placeholder: 'Search PubMed for Citations' },
} satisfies Record<string, CitationSource>;

/**
 * The panmirror Insert Citation dialog (visual editor only).
 *
 * The dialog's React UI renders in a root that is a sibling of the GWT dialog
 * shell, so the tree, search box, and results are page-scoped, not dialog-scoped
 * (see visual-editor.md). Only the GWT footer buttons (Insert / Cancel) live
 * inside [role="dialog"].
 */
export class InsertCitationDialog extends PageObject {
  public readonly dialog: Locator;
  public readonly results: Locator;
  public readonly searchButton: Locator;
  public readonly insertButton: Locator;
  public readonly cancelButton: Locator;
  /** Staged citations, shown as tag chips at the bottom of the dialog. */
  public readonly stagedCitations: Locator;

  constructor(page: Page) {
    super(page);
    this.dialog = page.locator('[role="dialog"][aria-label="Insert Citation"]');
    this.results = page.locator('.pm-insert-citation-source-panel-item-detailed');
    this.searchButton = page.locator('button.pm-insert-citation-panel-latent-search-button');
    this.insertButton = this.dialog.getByRole('button', { name: 'Insert', exact: true });
    this.cancelButton = this.dialog.getByRole('button', { name: 'Cancel', exact: true });
    this.stagedCitations = page.locator('.pm-tag-input-tag');
  }

  /** Open Insert > Citation from the visual editor's Insert menu. */
  async open(): Promise<void> {
    await this.page.locator('[aria-label="Insert"]').click();
    await this.page.locator('#rstudio_label_citation_command').click();
    await this.dialog.waitFor({ state: 'visible', timeout: 15000 });
  }

  /**
   * Select a network source and return its latent-search box, surviving the
   * dialog's one-time init reset: when the bibliography load resolves, panmirror
   * snaps the active panel back to My Sources exactly once, leaving the tree
   * highlight where we put it. Re-select if that happens -- the second selection
   * lands after the reset and is permanent.
   */
  async selectSource(source: CitationSource): Promise<Locator> {
    const node = this.page.locator('.pm-navigation-tree-node', {
      has: this.page.locator(`[alt='${source.alt}']`),
    });
    const searchBox = this.page.getByPlaceholder(source.placeholder);
    await node.click();
    await expect(searchBox).toBeVisible({ timeout: 15000 });
    const revertDeadline = Date.now() + 6000;
    while (Date.now() < revertDeadline) {
      if (!(await searchBox.isVisible().catch(() => false))) {
        await node.click(); // reverted; re-select (now permanent)
        await expect(searchBox).toBeVisible({ timeout: 15000 });
        break;
      }
      await this.page.waitForTimeout(200);
    }
    return searchBox;
  }

  /**
   * Type a term with real keystrokes (so React's controlled-input onChange
   * fires -- fill() doesn't) and run the search via the Search button. Latent
   * search does not fire on typing alone.
   */
  async search(searchBox: Locator, term: string): Promise<void> {
    await searchBox.pressSequentially(term);
    await this.searchButton.click();
  }

  /** Wait for results and stage the first one via its "+" button. */
  async stageFirstResult(): Promise<void> {
    const first = this.results.first();
    await expect(first).toBeVisible({ timeout: 30000 });
    await first.locator('button').click();
  }

  /** Remove the first staged citation via its delete icon. */
  async deleteStagedCitation(): Promise<void> {
    await this.stagedCitations.first().locator('.pm-tag-input-delete-image').click();
  }

  /**
   * Visible text of the first `limit` currently-rendered result rows. The list
   * is react-window virtualized, so this reflects the rendered window rather
   * than every hit -- fine for inspecting the leading results.
   */
  async resultTexts(limit: number): Promise<string[]> {
    return this.results.evaluateAll(
      (els, n) => els.slice(0, n).map((el) => el.textContent ?? ''),
      limit,
    );
  }

  /** Click Insert to add the staged citations to the document. */
  async insert(): Promise<void> {
    await this.insertButton.click();
  }

  /** Close the dialog without inserting. */
  async cancel(): Promise<void> {
    await this.cancelButton.click();
    await expect(this.dialog).toBeHidden({ timeout: 5000 });
  }
}
