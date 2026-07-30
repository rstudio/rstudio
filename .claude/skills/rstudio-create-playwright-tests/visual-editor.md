# Visual editor (panmirror) patterns

Read this when driving the visual (WYSIWYG) markdown editor -- the Insert
Citation dialog, format/insert menus, etc. panmirror is a React app; its source
lives in the Quarto repo (`packages/editor/src/behaviors/...`), not in this repo.

## Entering visual mode

Citations and the Insert menu exist only in visual mode. Use
`SourcePaneActions.ensureVisualMode()`; it handles the first-switch
pandoc-conversion confirmation dialog. Plain `newMarkdownDoc` is the cheapest
host (no `rmarkdown` package, no Quarto new-doc wizard).

## panmirror renders outside the GWT dialog

panmirror UI (e.g. the Insert Citation panel) renders in a React root that is a
sibling of the GWT `[role="dialog"]` shell, not inside it. Scope selectors to
`page` -- a dialog-scoped `locator('button')` or `outerHTML` dump misses the
real controls.

## Insert Citation dialog

Open it in visual mode:

```typescript
await page.locator('[aria-label="Insert"]').click();
await page.locator('#rstudio_label_citation_command').click();
await page.locator('[role="dialog"][aria-label="Insert Citation"]')
  .waitFor({ state: 'visible', timeout: 15000 });
```

**Select a source by clicking the node row, not the icon:**
`.pm-navigation-tree-node` containing the source's `img[alt='<Source>']`.
Clicking the img or its wrapper only moves the highlight. The tree is
react-window virtualized (it scrolls on selection), so don't rapid-fire clicks
at it.

**Highlight != selected.** `pm-selected-navigation-tree-item` moving to a source
does not mean its panel is active. Verify true selection by the source's own
search box (placeholder e.g. `Search DataCite for Citations`) or its Search
button becoming visible -- never by the highlight class.

**Survive the one-time init reset.** When the dialog's bibliography load
resolves, the post-load configuration poll fires once and calls
`setSelectedPanelProvider`, snapping the active panel back to the dialog's
initially-selected node (My Sources on a fresh open) while leaving the tree
highlight where you put it. It fires at most once. So select, and re-select if
the panel reverts -- the second selection lands after the reset and is
permanent:

```typescript
const node = page.locator('.pm-navigation-tree-node', { has: page.locator("[alt='DataCite']") });
const box = page.getByPlaceholder('Search DataCite for Citations');
await node.click();
await expect(box).toBeVisible({ timeout: 15000 });
const deadline = Date.now() + 6000;
while (Date.now() < deadline) {
  if (!(await box.isVisible().catch(() => false))) {
    await node.click();               // reverted; re-select (now permanent)
    await expect(box).toBeVisible({ timeout: 15000 });
    break;
  }
  await page.waitForTimeout(200);
}
```

## Latent search (external sources: DataCite, Crossref, DOI, PubMed)

Typing does not search -- it only updates the term. The search fires on Enter,
paste, or the Search button (`button.pm-insert-citation-panel-latent-search-button`),
which is present whenever the source panel is active and disabled only while a
search is running. Type, then click the button:

```typescript
await box.pressSequentially('bobolink');   // real keystrokes, not fill()
await page.locator('button.pm-insert-citation-panel-latent-search-button').click();
const results = page.locator('.pm-insert-citation-source-panel-item-detailed');
await expect.poll(() => results.count(), { timeout: 30000 }).toBeGreaterThan(0);
```

Use `pressSequentially`, not `fill()`: `fill()` sets the DOM value but does not
reliably fire React's controlled-input `onChange`, so the term can stay empty
and Enter/the button do nothing even though the box visibly shows the text.

## Search RPCs are async

`datacite_search`, `crossref_works`, `pubmed_search`, and `doi_fetch_csl` are
async RPCs: the POST returns only `{asyncHandle}`, and the results arrive later
via `/events/get_events` as an `async_completion` event keyed by that handle.
The service HTTP request is made server-side (rsession), so it is not reachable
at the API boundary from Playwright -- only the browser<->rsession RPC seam is.

## Citation search failures: skip on service error, still fail on nothing

The search query runs in the *rsession*, so a Node-side reachability probe can
pass while the query still fails (e.g. NCBI rate-limits shared CI egress IPs).
That's a service problem, not a product bug -- skip. But a timeout with
*neither* results nor an error must still fail, so a regression that silently
renders nothing isn't masked. Condensed from `citations.test.ts`:

```typescript
const searchOutcome = async (): Promise<'pending' | 'matched' | 'error'> => {
  if ((await citation.resultTexts(5)).some((t) => matcher.test(t))) return 'matched';
  if (await citation.searchError.isVisible()) return 'error';
  return 'pending';
};
await expect.poll(searchOutcome, { timeout: 30000 }).not.toBe('pending');
if ((await searchOutcome()) === 'error') {
  await citation.cancel();
  test.skip(true, 'search failed service-side from this runner');
}
```

`searchError` (in `insert_citation.page.ts`) filters
`.pm-insert-citation-source-panel-list-noresults-text` by
`/error occurred|Unable to search/` -- required because the same node also
carries "No results..." and progress text.
