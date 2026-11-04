/**
 * Read an on/off switch from the environment, case-insensitively. Unset,
 * empty, 0 and false are off; 1 and true are on; anything else throws.
 * Plain truthiness is not enough: a workflow that interpolates a false
 * boolean input hands us the string "false".
 */
export function envFlag(name: string): boolean {
  const raw = process.env[name];
  const value = (raw ?? '').toLowerCase();
  if (value === '' || value === '0' || value === 'false')
    return false;
  if (value === '1' || value === 'true')
    return true;
  throw new Error(`${name}="${raw}" -- expected one of: 1, true, 0, false (or unset)`);
}

/**
 * Resolve an env override against its allowed values, falling back to the
 * default when unset or empty. An unrecognized value throws rather than being
 * passed through, where a typo like PW_TRACE=retain-on-falure would otherwise
 * turn a debugging aid into a confusing failure further along.
 */
export function envChoice<T extends string>(name: string, allowed: readonly T[], fallback: T): T {
  const value = process.env[name];
  if (value === undefined || value === '')
    return fallback;
  if (!allowed.includes(value as T))
    throw new Error(`${name}="${value}" -- expected one of: ${allowed.join(', ')}`);
  return value as T;
}
