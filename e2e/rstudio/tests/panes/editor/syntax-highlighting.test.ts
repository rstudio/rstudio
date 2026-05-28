import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { SourcePaneActions } from '@actions/source_pane.actions';
import { AceEditor } from '@pages/ace_editor.page';
import { setPref } from '@utils/commands';
import { useSuiteSandbox } from '@utils/sandbox';
import { writeAndOpenFile, closeAndDeleteSandboxFiles } from '@utils/files';

test.describe('Syntax Highlighting', () => {
  const sandbox = useSuiteSandbox();
  let consoleActions: ConsolePaneActions;
  let sourceActions: SourcePaneActions;

  test.beforeAll(async ({ rstudioPage: page }) => {
    consoleActions = new ConsolePaneActions(page);
    sourceActions = new SourcePaneActions(page, consoleActions);
    await consoleActions.resetSourcePane();
  });

  test.afterEach(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, [
      'syntax_highlight.Rmd',
      'syntax_highlight.qmd',
      'syntax_highlight.R',
      'syntax_highlight.yml',
    ]);
  });

  test('tokens outside R chunks in Rmd are plain text', async ({ rstudioPage: page }) => {
    const content = `---
title: R Markdown Document
---

\`\`\`{r}
#| echo: true
\`\`\`

1 + 1
`;

    await writeAndOpenFile(page, sandbox.dir, 'syntax_highlight.Rmd', content);

    const editor = new AceEditor(page, '1 + 1');
    await expect.poll(() => editor.getValue()).toContain('1 + 1');

    await expect.poll(async () => {
      const tokens = await editor.getTokens(8);
      return tokens.length === 1 ? { type: tokens[0].type, value: tokens[0].value } : null;
    }).toEqual({ type: 'text', value: '1 + 1' });
  });

  // https://github.com/rstudio/rstudio/issues/14652
  test('Quarto callout divs are tokenized correctly', async ({ rstudioPage: page }) => {
    const content = `::: {.callout 0}
Hello, world!
:::

::: {.callout 1}
Goodbye, world!
:::
`;

    await setPref(page, 'rainbow_fenced_divs', true);
    try {
      await writeAndOpenFile(page, sandbox.dir, 'syntax_highlight.Rmd', content);

      const editor = new AceEditor(page, 'Hello, world!');
      await expect.poll(() => editor.getValue()).toContain(':::');

      // First callout block (row 0): two tokens, fenced_div_0 + fenced_div_text_0
      await expect.poll(async () => {
        const tokens = await editor.getTokens(0);
        return tokens.map((t) => ({ type: t.type, value: t.value }));
      }).toEqual([
        { type: 'fenced_div_0', value: ':::' },
        { type: 'fenced_div_text_0', value: ' {.callout 0}' },
      ]);

      // Second callout block (row 4): tokens cycle to fenced_div_1
      await expect.poll(async () => {
        const tokens = await editor.getTokens(4);
        return tokens.map((t) => ({ type: t.type, value: t.value }));
      }).toEqual([
        { type: 'fenced_div_1', value: ':::' },
        { type: 'fenced_div_text_1', value: ' {.callout 1}' },
      ]);

      expect(await editor.getState(5)).toBe('start');
    } finally {
      await setPref(page, 'rainbow_fenced_divs', false);
    }
  });

  // https://github.com/rstudio/rstudio/issues/14699
  test('Quarto chunks receive chunk begin / end markers', async ({ rstudioPage: page }) => {
    const content = `---
title: Quarto Document
---

\`\`\`{r}
# This is a code chunk.
\`\`\`
`;

    await writeAndOpenFile(page, sandbox.dir, 'syntax_highlight.qmd', content);

    const editor = new AceEditor(page, '# This is a code chunk.');
    await expect.poll(() => editor.getValue()).toContain('```{r}');

    await expect.poll(() => editor.getFoldWidget(4)).toBe('start');
    await expect.poll(() => editor.getFoldWidget(6)).toBe('end');
  });

  // https://github.com/rstudio/rstudio/issues/14592
  test('The sequence "# |" is not tokenized as a Quarto comment prefix', async ({ rstudioPage: page }) => {
    const content = `#|  yaml: true
# | yaml: false
`;

    await writeAndOpenFile(page, sandbox.dir, 'syntax_highlight.R', content);

    const editor = new AceEditor(page, '# | yaml: false');
    await expect.poll(() => editor.getValue()).toContain('# | yaml: false');

    await expect.poll(async () => {
      const tokens = await editor.getTokens(0);
      return { type: tokens[0].type, value: tokens[0].value };
    }).toEqual({ type: 'comment.doc.tag', value: '#|' });

    await expect.poll(async () => {
      const tokens = await editor.getTokens(1);
      return tokens.map((t) => ({ type: t.type, value: t.value, column: t.column ?? 0 }));
    }).toEqual([{ type: 'comment', value: '# | yaml: false', column: 0 }]);
  });

  // https://github.com/rstudio/rstudio/issues/15019
  test('tikz chunks are properly highlighted', async ({ rstudioPage: page }) => {
    const content = `---
title: tikz chunks
---

\`\`\`{tikz}
% This is a tikz chunk.
\`\`\`

\`\`\`{r}
# This is an R chunk.
"hello"
\`\`\`
`;

    await writeAndOpenFile(page, sandbox.dir, 'syntax_highlight.Rmd', content);

    const editor = new AceEditor(page, 'This is a tikz chunk');
    await expect.poll(() => editor.getValue()).toContain('```{tikz}');

    await expect.poll(async () => {
      const t = await editor.getTokenAt(5, 0);
      return t ? { type: t.type, value: t.value } : null;
    }).toEqual(expect.objectContaining({ value: '% This is a tikz chunk.' }));
    const tikzToken = await editor.getTokenAt(5, 0);
    expect(tikzToken?.type).toMatch(/comment/);

    const rCommentToken = await editor.getTokenAt(9, 0);
    expect(rCommentToken?.type).toMatch(/comment/);
    expect(rCommentToken?.value).toBe('# This is an R chunk.');

    const rStringToken = await editor.getTokenAt(10, 0);
    expect(rStringToken?.type).toMatch(/string/);
    expect(rStringToken?.value).toBe('"hello"');
  });

  // https://github.com/rstudio/rstudio/issues/12161
  test('nested GitHub chunks are highlighted appropriately', async ({ rstudioPage: page }) => {
    const content = `---
title: "Untitled"
format: html
---

## Heading 1

\`\`\`\`\` markdown
\`\`\` nested
This is a nested chunk.
\`\`\`
\`\`\`\`\`

## Heading 2
`;

    await writeAndOpenFile(page, sandbox.dir, 'syntax_highlight.qmd', content);

    const editor = new AceEditor(page, 'This is a nested chunk');
    await expect.poll(() => editor.getValue()).toContain('## Heading 2');

    await expect.poll(async () => (await editor.getTokenAt(5, 0))?.type).toBe('markup.heading.2');
    await expect.poll(async () => (await editor.getTokenAt(9, 0))?.type).toBe('support.function');
    await expect.poll(async () => (await editor.getTokenAt(13, 0))?.type).toBe('markup.heading.2');
  });

  // https://github.com/rstudio/rstudio/issues/16657
  test('R files highlight hex color strings', async ({ rstudioPage: page }) => {
    const content = `col1 <- "#ff0000"
col2 <- "#00ff00ff"
col3 <- "#abc"
`;

    await writeAndOpenFile(page, sandbox.dir, 'syntax_highlight.R', content);

    const editor = new AceEditor(page, '#ff0000');
    await expect.poll(() => editor.getValue()).toContain('"#abc"');

    for (const [row, value] of [[0, '#ff0000'], [1, '#00ff00ff'], [2, '#abc']] as const) {
      await expect.poll(async () => {
        const tokens = await editor.getTokens(row);
        const colorToken = tokens[5];
        return { type: colorToken?.type, value: colorToken?.value, bg: colorToken?.bg };
      }).toEqual({ type: 'string.color', value, bg: value });
    }
  });

  // https://github.com/rstudio/rstudio/issues/16657
  test('R files highlight named color strings', async ({ rstudioPage: page }) => {
    const content = `col1 <- "red"
col2 <- "steelblue4"
col3 <- "notacolor"
`;

    await writeAndOpenFile(page, sandbox.dir, 'syntax_highlight.R', content);

    const editor = new AceEditor(page, 'steelblue4');
    await expect.poll(() => editor.getValue()).toContain('notacolor');

    await expect.poll(async () => {
      const t = (await editor.getTokens(0))[5];
      return { type: t?.type, value: t?.value, bg: t?.bg };
    }).toEqual({ type: 'string.color', value: 'red', bg: '#ff0000' });

    await expect.poll(async () => {
      const t = (await editor.getTokens(1))[5];
      return { type: t?.type, value: t?.value, bg: t?.bg };
    }).toEqual({ type: 'string.color', value: 'steelblue4', bg: '#36648b' });

    // Non-color string stays as a single regular string token.
    await expect.poll(async () => {
      const t = (await editor.getTokens(2))[4];
      return { type: t?.type, value: t?.value };
    }).toEqual({ type: 'string', value: '"notacolor"' });
  });

  // https://github.com/rstudio/rstudio/issues/16657
  test('YAML files highlight hex color strings', async ({ rstudioPage: page }) => {
    const content = `color1: "#ff0000"
color2: "#00ff00ff"
color3: "#abc"
`;

    await writeAndOpenFile(page, sandbox.dir, 'syntax_highlight.yml', content);

    const editor = new AceEditor(page, '#ff0000');
    await expect.poll(() => editor.getValue()).toContain('color3');

    for (const [row, value] of [[0, '#ff0000'], [1, '#00ff00ff'], [2, '#abc']] as const) {
      await expect.poll(async () => {
        const tokens = await editor.getTokens(row);
        const colorToken = tokens[3];
        return { type: colorToken?.type, value: colorToken?.value, bg: colorToken?.bg };
      }).toEqual({ type: 'string.color', value, bg: value });
    }
  });

  // https://github.com/rstudio/rstudio/issues/16657
  test('YAML files highlight named color strings', async ({ rstudioPage: page }) => {
    const content = `color1: "red"
color2: "steelblue4"
color3: "notacolor"
`;

    await writeAndOpenFile(page, sandbox.dir, 'syntax_highlight.yml', content);

    const editor = new AceEditor(page, 'steelblue4');
    await expect.poll(() => editor.getValue()).toContain('notacolor');

    await expect.poll(async () => {
      const t = (await editor.getTokens(0))[3];
      return { type: t?.type, value: t?.value, bg: t?.bg };
    }).toEqual({ type: 'string.color', value: 'red', bg: '#ff0000' });

    await expect.poll(async () => {
      const t = (await editor.getTokens(1))[3];
      return { type: t?.type, value: t?.value, bg: t?.bg };
    }).toEqual({ type: 'string.color', value: 'steelblue4', bg: '#36648b' });

    // Non-color string stays as a single regular string token.
    await expect.poll(async () => {
      const t = (await editor.getTokens(2))[2];
      return { type: t?.type, value: t?.value };
    }).toEqual({ type: 'string', value: '"notacolor"' });
  });

  // https://github.com/rstudio/rstudio/issues/16657
  test('YAML files highlight unquoted named colors', async ({ rstudioPage: page }) => {
    const content = `color1: red
color2: steelblue4
color3: notacolor
`;

    await writeAndOpenFile(page, sandbox.dir, 'syntax_highlight.yml', content);

    const editor = new AceEditor(page, 'steelblue4');
    await expect.poll(() => editor.getValue()).toContain('notacolor');

    await expect.poll(async () => {
      const t = (await editor.getTokens(0))[2];
      return { type: t?.type, value: t?.value, bg: t?.bg };
    }).toEqual({ type: 'string.color', value: 'red', bg: '#ff0000' });

    await expect.poll(async () => {
      const t = (await editor.getTokens(1))[2];
      return { type: t?.type, value: t?.value, bg: t?.bg };
    }).toEqual({ type: 'string.color', value: 'steelblue4', bg: '#36648b' });

    // Non-color stays as plain text.
    await expect.poll(async () => {
      const t = (await editor.getTokens(2))[2];
      return { type: t?.type, value: t?.value };
    }).toEqual({ type: 'text', value: 'notacolor' });
  });
});
