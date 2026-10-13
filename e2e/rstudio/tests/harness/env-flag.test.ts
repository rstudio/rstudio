import { test, expect } from '@playwright/test';
import { envFlag } from '@utils/env';

/**
 * Harness self-test for envFlag (utils/env.ts). Workflows interpolate boolean
 * inputs as strings, so "false" must read as off and anything unrecognized
 * must fail the run rather than silently pick a mode.
 */

const NAME = 'PW_TEST_ENV_FLAG';

function setEnv(value: string | undefined): void {
  if (value === undefined)
    delete process.env[NAME];
  else
    process.env[NAME] = value;
}

test.describe('envFlag', () => {
  test.afterEach(() => setEnv(undefined));

  test('reads unset, empty, 0 and false (any case) as off', () => {
    for (const value of [undefined, '', '0', 'false', 'FALSE', 'False']) {
      setEnv(value);
      expect(envFlag(NAME), `value ${JSON.stringify(value)}`).toBe(false);
    }
  });

  test('reads 1 and true (any case) as on', () => {
    for (const value of ['1', 'true', 'TRUE', 'True']) {
      setEnv(value);
      expect(envFlag(NAME), `value ${JSON.stringify(value)}`).toBe(true);
    }
  });

  test('rejects anything else, naming the variable and the accepted values', () => {
    for (const value of ['maybe', 'yes', 'on', '2', ' 1']) {
      setEnv(value);
      expect(() => envFlag(NAME)).toThrow(`${NAME}="${value}"`);
      expect(() => envFlag(NAME)).toThrow('1, true, 0, false');
    }
  });
});
