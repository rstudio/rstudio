/**
 * Tagged template that strips one leading newline, trailing whitespace after
 * the last newline, and the common leading indent across non-blank lines.
 * Lets us write embedded code (R, shell, ...) at the indent of the host
 * source file instead of building it from arrays of strings joined by '\n'.
 *
 *   const code = heredoc`
 *     lib <- ${JSON.stringify(lib)}
 *     install.packages("pkg", lib = lib)
 *   `;
 *
 * yields: `lib <- "/foo"\ninstall.packages("pkg", lib = lib)`.
 *
 * Template parts are read in raw form (`strings.raw`) so JS escape sequences
 * pass through to the target language untouched -- write `"\n"` to mean an R
 * newline escape, not a JS newline character. Real line breaks in the source
 * are still real line breaks.
 *
 * Interpolated values are coerced via String(). Multi-line interpolations are
 * inserted verbatim and can pull the computed minimum indent down to zero --
 * pre-indent them if that matters.
 */
export function heredoc(
  strings: TemplateStringsArray,
  ...values: unknown[]
): string {
  let raw = strings.raw[0];
  for (let i = 0; i < values.length; i++) {
    raw += String(values[i]) + strings.raw[i + 1];
  }

  raw = raw.replace(/\r\n?/g, '\n');
  raw = raw.replace(/^\n/, '').replace(/\n[ \t]*$/, '');

  const lines = raw.split('\n');
  const indents: number[] = [];
  for (const line of lines) {
    if (line.trim().length === 0) continue;
    indents.push(/^[ \t]*/.exec(line)![0].length);
  }
  if (indents.length === 0) return raw;
  const minIndent = Math.min(...indents);
  if (minIndent === 0) return raw;
  return lines.map((l) => l.slice(minIndent)).join('\n');
}
