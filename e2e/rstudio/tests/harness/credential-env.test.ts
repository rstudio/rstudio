import { test, expect } from '@playwright/test';
import { isCredentialEnvVar, withoutCredentials } from '@utils/env';

/**
 * Harness self-test for the credential scrub applied to the IDE's launch
 * environment (utils/env.ts). The names below are the ones CI actually sets;
 * a miss here means the IDE under test can read that secret.
 */

test.describe('isCredentialEnvVar', () => {
  test('matches the credentials CI hands the runner', () => {
    for (const name of [
      'POSIT_EMAIL', 'POSIT_PASSWORD',
      'COPILOT_USER', 'COPILOT_PASSWORD', 'COPILOT_TOTP_SECRET',
      'PW_RSTUDIO_SERVER_USER', 'PW_RSTUDIO_SERVER_PASSWORD',
      'CONNECT_API_KEY', 'OP_SERVICE_ACCOUNT_TOKEN',
      'ACTIONS_ID_TOKEN_REQUEST_TOKEN', 'ACTIONS_ID_TOKEN_REQUEST_URL', 'ACTIONS_RUNTIME_TOKEN',
    ]) {
      expect(isCredentialEnvVar(name), name).toBe(true);
    }
  });

  test('matches common credential shapes in any case', () => {
    for (const name of [
      'ANTHROPIC_API_KEY', 'OPENAI_APIKEY', 'AWS_ACCESS_KEY_ID', 'AWS_SECRET_ACCESS_KEY',
      'AWS_SESSION_TOKEN', 'GITHUB_PAT', 'GH_TOKEN', 'Some_Password', 'db_passwd',
    ]) {
      expect(isCredentialEnvVar(name), name).toBe(true);
    }
  });

  test('leaves the environment the IDE needs alone', () => {
    for (const name of [
      'PATH', 'HOME', 'USERPROFILE', 'LANG', 'R_LIBS_USER', 'RSTUDIO_CONFIG_HOME',
      'SSH_AUTH_SOCK', 'SECURITYSESSIONID', 'PW_RSTUDIO_SERVER_URL', 'GITHUB_ACTIONS',
      'GITHUB_WORKSPACE', 'PATHEXT', 'RETICULATE_PYTHON', 'PW_ODBC_DRIVER_PATHS',
    ]) {
      expect(isCredentialEnvVar(name), name).toBe(false);
    }
  });
});

test.describe('withoutCredentials', () => {
  test('drops credentials and keeps everything else, leaving the input untouched', () => {
    const env = { PATH: '/usr/bin', POSIT_PASSWORD: 'hunter2', HOME: '/home/pw', CONNECT_API_KEY: 'k' };

    expect(withoutCredentials(env)).toEqual({ PATH: '/usr/bin', HOME: '/home/pw' });
    expect(env.POSIT_PASSWORD).toBe('hunter2');
  });
});
