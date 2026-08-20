import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { clearPref, setPref } from '@utils/commands';

test.describe('Code folding', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await clearPref(page, 'hierarchical_section_folding');
    await closeAndDeleteSandboxFiles(page, sandbox.dir, ['code_folding.R']);
  });

  // https://github.com/rstudio/rstudio/issues/16541
  test('hierarchical section folding respects heading depth', async ({ rstudioPage: page }) => {
    await setPref(page, 'hierarchical_section_folding', true);

    const content = `# Section 1 ----
code_1 <- 1
## Section 1.1 ----
code_1_1 <- 2
## Section 1.2 ----
code_1_2 <- 3
# Section 2 ----
code_2 <- 4
`;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_1_2');
    await expect.poll(() => editor.getValue()).toContain('code_1_2');

    // All section headers should be fold starts.
    expect(await editor.getFoldWidget(0)).toBe('start'); // # Section 1
    expect(await editor.getFoldWidget(2)).toBe('start'); // ## Section 1.1
    expect(await editor.getFoldWidget(4)).toBe('start'); // ## Section 1.2
    expect(await editor.getFoldWidget(6)).toBe('start'); // # Section 2

    // '# Section 1' folds through both ## subsections to the line before '# Section 2' (row 5).
    let range = await editor.getFoldWidgetRange(0);
    expect(range?.end.row).toBe(5);

    // '## Section 1.1' folds to the line before '## Section 1.2' (row 3).
    range = await editor.getFoldWidgetRange(2);
    expect(range?.end.row).toBe(3);

    // '## Section 1.2' folds to the line before '# Section 2' (row 5).
    range = await editor.getFoldWidgetRange(4);
    expect(range?.end.row).toBe(5);

    // '# Section 2' is the last section; folds to end of document (row 8).
    range = await editor.getFoldWidgetRange(6);
    expect(range?.end.row).toBe(8);
  });

  // https://github.com/rstudio/rstudio/issues/17734
  test('em dashes and box-drawing chars are recognized as section delimiters', async ({ rstudioPage: page }) => {
    // U+2014 em dash, U+2013 en dash,
    // U+2500 box drawings light horizontal, U+2501 box drawings heavy horizontal.
    const EM = '\u2014';
    const EN = '\u2013';
    const BOX_L = '\u2500';
    const BOX_H = '\u2501';

    const content = heredoc`
      # Em dash section ${EM.repeat(4)}
      code_em <- 1
      # En dash section ${EN.repeat(4)}
      code_en <- 2
      # Box light section ${BOX_L.repeat(4)}
      code_box_l <- 3
      # Box heavy section ${BOX_H.repeat(4)}
      code_box_h <- 4
      # ${BOX_L.repeat(2)} 1. Load Data ${BOX_L.repeat(20)}
      code_issue_example <- 5
    `;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_issue_example');
    await expect.poll(() => editor.getValue()).toContain('code_issue_example');

    // Each section header row should be tokenized as comment.sectionhead
    // (drives the outline), have a fold-widget start, and yield a valid
    // fold range (the range computation has its own delimiter regex that
    // must stay in sync with the tokenizer).
    for (const row of [0, 2, 4, 6, 8]) {
      const tokens = await editor.getTokens(row);
      expect(tokens[0]?.type, `row ${row} token`).toBe('comment.sectionhead');
      expect(await editor.getFoldWidget(row), `row ${row} fold widget`).toBe('start');
      const range = await editor.getFoldWidgetRange(row);
      expect(range?.end.row, `row ${row} fold range end`).toBe(row + 1);
    }
  });

  // https://github.com/rstudio/rstudio/issues/18602
  test('a bar of hashes folds flat regardless of its width', async ({ rstudioPage: page }) => {
    await setPref(page, 'hierarchical_section_folding', true);

    // A bar of hashes is nothing but leading '#' characters, so its width must
    // not be read as a heading depth -- otherwise a wider bar later in the file
    // looks like a subsection and gets folded away with its contents.
    const content = heredoc`
      ##############
      # Chunk 1
      ##############
      code_1 <- 1
      ##########################
      # Chunk 2
      ##########################
      code_2 <- 2
      ###########
      # Chunk 3
      ###########
      code_3 <- 3
      ####################
      # Chunk 4
      ####################
      code_4 <- 4
    `;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_4');
    await expect.poll(() => editor.getValue()).toContain('code_4');

    // Chunk 1's closing bar (14 hashes) folds over its own code only, and stops
    // at chunk 2's wider bar (26 hashes) on row 4.
    let range = await editor.getFoldWidgetRange(2);
    expect(range?.end.row).toBe(3);

    // Chunk 3's closing bar (11 hashes) stops at chunk 4's wider bar (20
    // hashes) on row 12, rather than running to the end of the document.
    range = await editor.getFoldWidgetRange(10);
    expect(range?.end.row).toBe(11);
  });

  // https://github.com/rstudio/rstudio/issues/18602
  test('a bar of mixed delimiters folds flat regardless of its width', async ({ rstudioPage: page }) => {
    await setPref(page, 'hierarchical_section_folding', true);

    const content = heredoc`
      ####====
      # Narrow bar
      ####====
      code_narrow <- 1
      ##########========================
      # Wide bar
      ##########========================
      code_wide <- 2
    `;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_wide');
    await expect.poll(() => editor.getValue()).toContain('code_wide');

    // The narrow bar stops at the wide bar on row 4.
    const range = await editor.getFoldWidgetRange(2);
    expect(range?.end.row).toBe(3);

    // The outline reads the same headers through the same rule, so a bar has
    // no heading level there either -- '####====' is not an h4.
    const scopes = await editor.getSectionScopes();
    expect(scopes.map((scope) => scope.depth)).toEqual([0, 0, 0, 0]);
  });

  // https://github.com/rstudio/rstudio/issues/18602
  test('a banner header does not fold away a wider banner below it', async ({ rstudioPage: page }) => {
    await setPref(page, 'hierarchical_section_folding', true);

    // Same root cause as a bar of hashes, but with the label inline: the
    // leading '#' run mirrors the trailing one to draw a box around the label,
    // so it is decoration rather than a heading level.
    const content = heredoc`
      ##### Section A #####
      code_a <- 1
      ########## Section B ##########
      code_b <- 2
    `;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_b');
    await expect.poll(() => editor.getValue()).toContain('code_b');

    // Section A stops at section B on row 2, rather than swallowing it.
    const range = await editor.getFoldWidgetRange(0);
    expect(range?.end.row).toBe(1);

    // Neither banner carries a heading level, so neither nests in the other.
    const scopes = await editor.getSectionScopes();
    expect(scopes).toEqual([
      { label: 'Section A', row: 0, depth: 0, parent: null },
      { label: 'Section B', row: 2, depth: 0, parent: null },
    ]);
  });

  // https://github.com/rstudio/rstudio/issues/18602
  test('a banner header does not fold away a banner one hash wider', async ({
    rstudioPage: page,
  }) => {
    await setPref(page, 'hierarchical_section_folding', true);

    // Both leading runs are within h1..h6, so a rule that only discounts runs
    // deeper than h6 still reads these as an h5 containing an h6.
    const content = heredoc`
      ##### Section A #####
      code_a <- 1
      ###### Section B ######
      code_b <- 2
    `;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_b');
    await expect.poll(() => editor.getValue()).toContain('code_b');

    const range = await editor.getFoldWidgetRange(0);
    expect(range?.end.row).toBe(1);

    const scopes = await editor.getSectionScopes();
    expect(scopes).toEqual([
      { label: 'Section A', row: 0, depth: 0, parent: null },
      { label: 'Section B', row: 2, depth: 0, parent: null },
    ]);
  });

  // https://github.com/rstudio/rstudio/issues/18602
  test('a bar with interior whitespace folds flat', async ({ rstudioPage: page }) => {
    await setPref(page, 'hierarchical_section_folding', true);

    // A bar is decoration whether or not its delimiters run unbroken, so the
    // space between the two runs must not turn its width back into a depth.
    const content = heredoc`
      ##### #####
      code_a <- 1
      ###### ######
      code_b <- 2
    `;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_b');
    await expect.poll(() => editor.getValue()).toContain('code_b');

    // The narrow bar stops at the wider one on row 2, rather than running to
    // the end of the document.
    const range = await editor.getFoldWidgetRange(0);
    expect(range?.end.row).toBe(1);

    const scopes = await editor.getSectionScopes();
    expect(scopes.map((scope) => scope.depth)).toEqual([0, 0]);
  });

  // https://github.com/rstudio/rstudio/issues/18602
  test('a bar between two subsections does not adopt the one below it', async ({
    rstudioPage: page,
  }) => {
    await setPref(page, 'hierarchical_section_folding', true);

    // A header with no heading level is flat in both directions: it ends the
    // sections above it, and the next header of any kind ends it. Folding it
    // as an h1 instead would swallow every '##' section that follows.
    const content = heredoc`
      ## Section A ----
      code_a <- 1
      ##########
      code_bar <- 2
      ## Section B ----
      code_b <- 3
    `;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_b');
    await expect.poll(() => editor.getValue()).toContain('code_b');

    // The bar folds over its own code only, stopping at Section B on row 4.
    const range = await editor.getFoldWidgetRange(2);
    expect(range?.end.row).toBe(3);

    const scopes = await editor.getSectionScopes();
    expect(scopes).toEqual([
      { label: 'Section A', row: 0, depth: 2, parent: null },
      { label: '(Untitled)', row: 2, depth: 0, parent: null },
      { label: 'Section B', row: 4, depth: 2, parent: null },
    ]);
  });

  // https://github.com/rstudio/rstudio/issues/18602
  test('sections after a banner file header stay top-level', async ({ rstudioPage: page }) => {
    await setPref(page, 'hierarchical_section_folding', true);

    // The banner block at the top of a script leaves a header open that no
    // heading level can close, so the rest of the file used to indent under it.
    const content = heredoc`
      ##############################
      # Analysis script
      ##############################
      code_setup <- 1
      # Load data ----
      code_load <- 2
      # Fit model ----
      code_fit <- 3
    `;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_fit');
    await expect.poll(() => editor.getValue()).toContain('code_fit');

    const scopes = await editor.getSectionScopes();
    expect(scopes).toEqual([
      { label: '(Untitled)', row: 0, depth: 0, parent: null },
      { label: '(Untitled)', row: 2, depth: 0, parent: null },
      { label: 'Load data', row: 4, depth: 1, parent: null },
      { label: 'Fit model', row: 6, depth: 1, parent: null },
    ]);

    // The closing bar folds over its own code only, stopping at 'Load data'.
    const range = await editor.getFoldWidgetRange(2);
    expect(range?.end.row).toBe(3);
  });

  // https://github.com/rstudio/rstudio/issues/18602
  test('a header with no heading level is top-level in the outline too', async ({
    rstudioPage: page,
  }) => {
    await setPref(page, 'hierarchical_section_folding', true);

    const content = heredoc`
      # A ----
      code_a <- 1
      ## B ----
      code_b <- 2
      ######## C ########
      code_c <- 3
    `;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_c');
    await expect.poll(() => editor.getValue()).toContain('code_c');

    // C carries no heading level, so it ends A's fold rather than nesting
    // inside it: A folds through B only, to row 3.
    const range = await editor.getFoldWidgetRange(0);
    expect(range?.end.row).toBe(3);

    // The outline agrees: B nests under A, while C is a sibling of A. A
    // depthless header closes every open section, not just the innermost.
    const scopes = await editor.getSectionScopes();
    expect(scopes).toEqual([
      { label: 'A', row: 0, depth: 1, parent: null },
      { label: 'B', row: 2, depth: 2, parent: 'A' },
      { label: 'C', row: 4, depth: 0, parent: null },
    ]);
  });

  // https://github.com/rstudio/rstudio/issues/18602
  test('a header preceded by code folds at its own heading level', async ({
    rstudioPage: page,
  }) => {
    await setPref(page, 'hierarchical_section_folding', true);

    // The header is a trailing comment, so the line does not start with '#'.
    // Folding reads the heading level out of the header token, as the outline
    // does, rather than off the start of the line.
    const content = heredoc`
      # Top ----
      code_top <- 1
      x <- 1 ## Trailing ----
      code_trailing <- 2
      # Next ----
      code_next <- 3
    `;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_next');
    await expect.poll(() => editor.getValue()).toContain('code_next');

    // '## Trailing' is an h2, so it nests inside '# Top' and '# Top' folds
    // through it, down to the row before '# Next' (row 3).
    const range = await editor.getFoldWidgetRange(0);
    expect(range?.end.row).toBe(3);

    const scopes = await editor.getSectionScopes();
    expect(scopes).toEqual([
      { label: 'Top', row: 0, depth: 1, parent: null },
      { label: 'Trailing', row: 2, depth: 2, parent: 'Top' },
      { label: 'Next', row: 4, depth: 1, parent: null },
    ]);
  });

  // https://github.com/rstudio/rstudio/issues/16541
  test('flat section folding stops at any section header', async ({ rstudioPage: page }) => {
    await setPref(page, 'hierarchical_section_folding', false);

    const content = `# Section 1 ----
code_1 <- 1
## Section 1.1 ----
code_1_1 <- 2
# Section 2 ----
code_2 <- 3
`;

    await writeAndOpenFile(page, sandbox.dir, 'code_folding.R', content);

    const editor = new AceEditor(page, 'code_1_1');
    await expect.poll(() => editor.getValue()).toContain('code_1_1');

    // Flat folding: '# Section 1' stops at the next section header (row 1, before ## Section 1.1).
    let range = await editor.getFoldWidgetRange(0);
    expect(range?.end.row).toBe(1);

    // '## Section 1.1' folds to row 3 (before # Section 2).
    range = await editor.getFoldWidgetRange(2);
    expect(range?.end.row).toBe(3);
  });
});
