/**
 * Helpers for encoding TypeScript values as R literals so they can be safely
 * interpolated into R commands fed through the console.
 *
 * RStudio Playwright tests routinely build R expressions via template
 * literals -- executeInConsole(`.rs.api.openProject("${path}")`) and similar.
 * Naive interpolation is fragile: any embedded `"`, `\`, or control char in
 * the value can break R parsing or, worse, silently change semantics.
 * Centralizing the encoding here keeps each test honest and lets us harden
 * the rules in one place if R-side quoting ever needs to change.
 */

/**
 * Encode an arbitrary string as an R double-quoted literal. Preserves
 * backslashes -- callers passing file contents need them intact (LaTeX,
 * regex, Windows paths, etc.). `JSON.stringify` produces a form R also
 * accepts: quotes, backslashes, and control chars are escaped consistently
 * across both languages.
 */
export function rStringLiteral(s: string): string {
  return JSON.stringify(s);
}

/**
 * The longest console line rsession hands to R (R's 4096-byte input buffer,
 * less the newline and NUL it appends). rsession truncates longer lines,
 * which usually cuts a string literal short and leaves R at a "+" prompt
 * that swallows every command submitted after it.
 */
export const R_CONSOLE_LINE_MAX_BYTES = 4094;

// Well under R_CONSOLE_LINE_MAX_BYTES, leaving room for the quotes and comma
// and for a non-UTF-8 console encoding that expands some characters.
const R_LITERAL_PIECE_BYTES = 1000;

/**
 * Like rStringLiteral, but safe for values that may exceed the console line
 * limit, such as file contents written through the console. A long value
 * becomes a `paste0()` call with one piece per line; rsession feeds the
 * lines to R one at a time, so each piece stays under the limit.
 */
export function rLongStringLiteral(s: string): string {
  const literal = rStringLiteral(s);
  if (Buffer.byteLength(literal, 'utf8') <= R_LITERAL_PIECE_BYTES)
    return literal;

  // Escape one code point at a time, so that a piece never ends inside an
  // escape sequence or a surrogate pair.
  const pieces: string[] = [];
  let piece = '';
  let pieceBytes = 0;
  for (const ch of s) {
    const escaped = rStringLiteral(ch).slice(1, -1);
    const bytes = Buffer.byteLength(escaped, 'utf8');
    if (piece !== '' && pieceBytes + bytes > R_LITERAL_PIECE_BYTES) {
      pieces.push(piece);
      piece = '';
      pieceBytes = 0;
    }
    piece += escaped;
    pieceBytes += bytes;
  }
  pieces.push(piece);

  return `paste0(\n${pieces.map(p => `"${p}"`).join(',\n')}\n)`;
}

/**
 * Encode a filesystem path as an R double-quoted literal, normalizing
 * backslashes to forward slashes for cross-platform portability. R accepts
 * forward slashes in paths on all platforms.
 */
export function rPathLiteral(p: string): string {
  return rStringLiteral(p.replace(/\\/g, '/'));
}
