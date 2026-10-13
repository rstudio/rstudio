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
