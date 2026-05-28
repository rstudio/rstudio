import * as path from 'path';

/**
 * True when `p` is a non-empty absolute path in either POSIX or Windows form.
 *
 * Paths computed R-side use forward slashes even on Windows (e.g.
 * "C:/Users/..."), so we accept either flavor regardless of the runner
 * platform rather than relying on the platform-specific `path.isAbsolute`.
 */
export function isAbsolutePath(p: string): boolean {
  return !!p && (path.posix.isAbsolute(p) || path.win32.isAbsolute(p));
}

/**
 * Throw unless `value` is a non-empty absolute path.
 *
 * Guards the sandbox / project path builders. The input actually screened is
 * an empty `sandbox.dir` (read before its beforeAll populated it): joining ''
 * with a project name yields a root-relative path like
 * "/reformat-styler-project/...", which RStudio later fails to open with a
 * confusing "Error Opening Project" modal far from the real cause. Failing
 * here instead names the offending caller and value at the source.
 *
 * `label` should identify the caller and the argument being checked, e.g.
 * `createAndOpenProject(name="foo"): parentDir`.
 */
export function assertAbsolutePath(value: string, label: string): void {
  if (!isAbsolutePath(value)) {
    throw new Error(
      `${label}: expected a non-empty absolute path, got ${JSON.stringify(value)}. ` +
      'A useSuiteSandbox() sandbox.dir was likely read before its beforeAll ' +
      'populated it, or createSandbox returned an unusable path.',
    );
  }
}
