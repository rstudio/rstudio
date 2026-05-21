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
 * Encode a filesystem path as an R double-quoted literal, normalizing
 * backslashes to forward slashes for cross-platform portability. R accepts
 * forward slashes in paths on all platforms.
 */
export function rPathLiteral(p: string): string {
  return rStringLiteral(p.replace(/\\/g, '/'));
}
