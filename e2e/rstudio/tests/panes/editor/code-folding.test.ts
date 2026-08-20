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
    expect(scopes.map((scope) => scope.depth)).toEqual([
      undefined,
      undefined,
      undefined,
      undefined,
    ]);
  });

  // https://github.com/rstudio/rstudio/issues/18602
  test('a banner header does not fold away a wider banner below it', async ({ rstudioPage: page }) => {
    await setPref(page, 'hierarchical_section_folding', true);

    // Same root cause as a bar of hashes, but with the label inline: the
    // leading '#' run is decoration, not a heading level. A run deeper than
    // h6 carries no heading level, so it can't nest under a shallower one.
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

    // '#####' is an h5; '##########' is past h6, so it is a plain section.
    const scopes = await editor.getSectionScopes();
    expect(scopes).toEqual([
      { label: 'Section A', row: 0, depth: 5 },
      { label: 'Section B', row: 2, depth: undefined },
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
