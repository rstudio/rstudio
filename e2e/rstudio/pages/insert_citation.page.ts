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
  /** Result rows in the R Package (typeahead) source's list. */
  public readonly packageResults: Locator;
  public readonly searchButton: Locator;
  /**
   * Status line shown in the results area when a latent search fails
   * service-side (the rsession's download errored or the host was unreachable).
   * The wording comes from panmirror's errorForStatus() plus the panels'
   * "unknown error" fallback; the same node also carries "No results..." and
   * progress text, hence the text filter.
   */
  public readonly searchError: Locator;
  public readonly insertButton: Locator;
  public readonly cancelButton: Locator;
  /** Staged citations, shown as tag chips at the bottom of the dialog. */
  public readonly stagedCitations: Locator;

  constructor(page: Page) {
    super(page);
    this.dialog = page.locator('[role="dialog"][aria-label="Insert Citation"]');
    this.results = page.locator('.pm-insert-citation-source-panel-item-detailed');
    this.packageResults = page.locator('.pm-insert-citation-source-panel-item');
    this.searchButton = page.locator('button.pm-insert-citation-panel-latent-search-button');
    this.searchError = page
      .locator('.pm-insert-citation-source-panel-list-noresults-text')
      .filter({ hasText: /error occurred|Unable to search/ })
      .first();
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
   * snaps the active panel back to the dialog's initially-selected node (My
   * Sources on a fresh open) exactly once, leaving the tree highlight where we
   * put it. The reset can land on either side of our first sighting of the
   * panel, so both the initial selection and the post-sighting watch must be
   * able to re-click.
   */
  async selectSource(source: CitationSource): Promise<Locator> {
    const node = this.page.locator('.pm-navigation-tree-node', {
      has: this.page.locator(`[alt='${source.alt}']`),
    });
    const searchBox = this.page.getByPlaceholder(source.placeholder);

    // Click and verify as one retryable unit: if the reset fires before the
    // panel is ever seen (slow bibliography load on a loaded CI machine), the
    // panel snaps back to My Sources with our node still highlighted, and only
    // another click can bring it back -- a plain visibility wait just times out.
    await expect(async () => {
      await node.click();
      await expect(searchBox).toBeVisible({ timeout: 3000 });
    }).toPass({ timeout: 30000 });

    // The reset may instead land after the panel was seen; watch briefly and
    // re-select if the panel reverts (the re-selection is permanent).
    const revertDeadline = Date.now() + 6000;
    while (Date.now() < revertDeadline) {
      if (!(await searchBox.isVisible().catch(() => false))) {
        await node.click();
        await expect(searchBox).toBeVisible({ timeout: 15000 });
        break;
      }
      await this.page.waitForTimeout(200);
    }
    return searchBox;
  }

  /**
   * Select the R Package source and filter to a package by name, returning its
   * result row. R Package is a typeahead panel (filters live, no Search button)
   * and its placeholder is shared with My Sources, so the init reset can't be
   * detected via the search box the way selectSource() does. Instead we watch
   * for the package's own result row and re-select if it vanishes -- the reset
   * reverts the panel to My Sources, which has no such package row.
   */
  async selectPackageSource(packageName: string): Promise<Locator> {
    const node = this.page.locator('.pm-navigation-tree-node', {
      has: this.page.locator("[alt='R Package']"),
    });
    const box = this.page.getByPlaceholder('Search for citation');
    const match = this.packageResults.filter({ hasText: `@${packageName}` }).first();

    const selectAndFilter = async (): Promise<void> => {
      await node.click();
      // Clear leftover filter text from a prior attempt with real key events
      // (React's controlled input ignores fill()), then retype. The reset can
      // remount the panel mid-typing, wiping or orphaning what was typed.
      await box.click();
      await this.page.keyboard.press('ControlOrMeta+a');
      await this.page.keyboard.press('Backspace');
      await box.pressSequentially(packageName); // typeahead filters on each keystroke
      await expect(match).toBeVisible({ timeout: 5000 });
    };

    // Retryable for the same reason as selectSource(): the one-time init reset
    // can land before the package row is ever seen, and only re-selecting the
    // node recovers from that.
    await expect(selectAndFilter).toPass({ timeout: 30000 });

    const revertDeadline = Date.now() + 6000;
    while (Date.now() < revertDeadline) {
      if (!(await match.isVisible().catch(() => false))) {
        await selectAndFilter(); // reverted to My Sources; re-select (now permanent)
        break;
      }
      await this.page.waitForTimeout(200);
    }
    return match;
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
