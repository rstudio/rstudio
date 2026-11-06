/*
 * Copyright (C) 2026 by Posit Software, PBC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

function syntheticCases() {
   const cases = [];
   function add(name, base, lines, expected) {
      cases.push({ name, base, lines, expected, kind: 'synthetic' });
   }
   add('empty-file-addition', [], ['x <- 1'], [[0, 0, 0, 1]]);
   add('empty-file-deletion', ['x <- 1'], [], [[0, 1, 0, 0]]);
   add('unchanged', ['x <- 1', ''], ['x <- 1', ''], []);
   add('final-newline', ['x <- 1'], ['x <- 1', ''], [[1, 1, 1, 2]]);
   add('whitespace-change', ['x <- 1'], ['  x <- 1  '], [[0, 1, 0, 1]]);
   add('reindent-block', ['f <- function() {', '  x <- 1', '  y <- 2', '}'],
      ['f <- function() {', '    x <- 1', '    y <- 2', '}']);
   add('ambiguous-braces', ['if (x) {', '  f()', '}', '', 'g()'],
      ['if (x) {', '  if (y) {', '    f()', '  }', '}', '', 'g()']);
   add('repeated-statements', ['begin()', 'x()', 'long_meaningful_statement()', 'x()', 'end()'],
      ['start()', 'long_meaningful_statement()', 'x()', 'x()', 'finish()']);
   for (const separator of ['keep_this_meaningful_statement()', '}', '', '    ']) {
      const base = ['old_a <- 1', 'old_b <- 2', 'old_c <- 3', separator,
         'old_d <- 4', 'old_e <- 5', 'old_f <- 6'];
      const lines = base.map(line => line.replace(/^old_/, 'new_'));
      add(`separated-blocks-${JSON.stringify(separator)}`, base, lines,
         separator.length > 4 ? [[0, 3, 0, 3], [4, 7, 4, 7]] : undefined);
   }
   for (const n of [100, 300, 800]) {
      add(`replace-${n}`, Array.from({ length: n }, (_, i) => `v${i + 1} <- ${i + 1}`),
         Array.from({ length: n }, (_, i) => `w${i + 1} <- ${(i + 1) * 2}`), [[0, n, 0, n]]);
   }
   const base = Array.from({ length: 2000 }, (_, i) => `v${i} <- ${i}`);
   const lines = base.slice();
   lines[1000] = 'v1000 <- 42';
   add('one-edit-in-2000-lines', base, lines, [[1000, 1001, 1000, 1001]]);
   return cases;
}

module.exports = { syntheticCases };
