import { test, expect } from '@fixtures/rstudio.fixture';
import { ConsolePaneActions } from '@actions/console_pane.actions';
import { executeCommand } from '@utils/commands';
import { closeAndDeleteSandboxFiles, writeAndOpenFile } from '@utils/files';
import { heredoc } from '@utils/heredoc';
import { useSuiteSandbox } from '@utils/sandbox';

// Editing a Julia script: the editor's Julia mode (acesupport/acemode/julia*.js)
// indents and outdents blocks as they are typed, and comments lines. This
// needs no Julia. The mode's highlighting, indentation and folding rules are
// covered one by one in src/gwt/test/julia_mode_test.html.

const ELEMENT_OF = '∈';

test.describe.serial('Julia editor', () => {
  const sandbox = useSuiteSandbox();

  test.beforeAll(async ({ rstudioPage: page }) => {
    await new ConsolePaneActions(page).resetSourcePane();
  });

  test.afterAll(async ({ rstudioPage: page }) => {
    await closeAndDeleteSandboxFiles(page, sandbox.dir, ['editing.jl']);
  });

  test('typing and commenting Julia code', async ({ rstudioPage: page }) => {
    const script = heredoc`
      """
          Shape

      An abstract shape, with an area of $(round(area; digits = 2)).
      """
      abstract type Shape end

      #= block comments #= nest =#
         over several lines =#
      mutable struct Circle <: Shape
          r::Float64
      end

      area(c::Circle) = pi * c.r^2      # the adjoint: A'
      const K = 1_000 + 0x1F + 2im
      s = "circle: $(area(Circle(1.0))) and $K"
      m = r"^\d+$"i
      c = 'x'
      syms = [:a, :b]
      @time for i in 1:3
          println(i ${ELEMENT_OF} 2:3 ? "yes" : "no")
      end
    `;
    await writeAndOpenFile(page, sandbox.dir, 'editing.jl', script + '\n');

    // type a block at the end of the file, relying on auto-indent and outdent
    await page.evaluate(() => {
      const editor = window.rstudio!.documents.activeEditor()!;
      editor.navigateFileEnd();
      editor.focus();
    });
    for (const line of ['function h(x)', 'if x', 'x', 'else', '0', 'end', 'end']) {
      await page.keyboard.type(line);
      await page.keyboard.press('Enter');
    }
    const typed = await page.evaluate(() => {
      const editor = window.rstudio!.documents.activeEditor()!;
      const lines = editor.getValue().split('\n');
      return lines.slice(-8).join('\n');
    });
    expect(typed).toBe('function h(x)\n  if x\n    x\n  else\n    0\n  end\nend\n');

    // comment / uncomment
    await page.evaluate(() => {
      const editor = window.rstudio!.documents.activeEditor()!;
      editor.gotoLine(6, 0, false);
      editor.focus();
    });
    await executeCommand(page, 'commentUncomment');
    const commented = await page.evaluate(() => window.rstudio!.documents.activeEditor()!.session.getLine(5));
    expect(commented).toMatch(/^#\s*abstract type Shape end/);
  });
});
