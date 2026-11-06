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

// Matched as a substring, case-insensitively, so a secret added to a workflow
// later is covered without anyone remembering this list: POSIT_PASSWORD,
// COPILOT_TOTP_SECRET, CONNECT_API_KEY, ACTIONS_ID_TOKEN_REQUEST_TOKEN, ...
const CREDENTIAL_NAME = /PASSWORD|PASSWD|SECRET|TOKEN|API_?KEY|ACCESS_KEY|PRIVATE_KEY|(^|_)PAT$/i;

// Account names that pair with one of the above but don't look like secrets.
const CREDENTIAL_ACCOUNT_NAMES = new Set(['POSIT_EMAIL', 'COPILOT_USER', 'PW_RSTUDIO_SERVER_USER']);

/** Whether an environment variable name looks like it carries a credential. */
export function isCredentialEnvVar(name: string): boolean {
  return CREDENTIAL_NAME.test(name) || CREDENTIAL_ACCOUNT_NAMES.has(name.toUpperCase());
}

/**
 * Copy `env` without its credential-shaped variables, as the base environment
 * for the IDE under test. The runner needs these (sign-in flows, the Test
 * Insights reporter), but the IDE never does, and anything it can read can end
 * up in a screenshot or trace of the published report -- GitHub masks secrets
 * in logs only. If the IDE ever does need one, add it explicitly to the launch
 * environment after this copy.
 */
export function withoutCredentials(env: NodeJS.ProcessEnv): NodeJS.ProcessEnv {
  return Object.fromEntries(Object.entries(env).filter(([name]) => !isCredentialEnvVar(name)));
}
