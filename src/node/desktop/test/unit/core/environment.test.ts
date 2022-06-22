/*
 * environment.test.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { describe } from 'mocha';
import { assert } from 'chai';
import { saveAndClear, restore } from '../unit-utils';

import * as env from '../../../src/core/environment';

const envVarName = 'BOGUS_FAKE_ENVIRONMENT_VARIABLE_FOR_ENV_TESTS';
const envValue = 'Value used for testing environment variables';

describe('Environment', () => {
  describe('Get global environment variable', () => {
    it('Should return zero length string for non-existent variable', () => {
      const result = env.getenv(envVarName);
      assert.isEmpty(result);
    });
    it('Should return value of a variable that exists', () => {
      const result = env.getenv('PATH');
      assert.isNotEmpty(result);
    });
    it('Should apply all from an Environment object', () => {
      const testEnv = { RSTUDIO_TESTVAR_ONE: '', RSTUDIO_TESTVAR_TWO: '' };
      saveAndClear(testEnv);
      const input = { RSTUDIO_TESTVAR_ONE: 'one', RSTUDIO_TESTVAR_TWO: 'two' };
      assert.isEmpty(env.getenv('RSTUDIO_TESTVAR_ONE'));
      assert.isEmpty(env.getenv('RSTUDIO_TESTVAR_TWO'));
      env.setVars(input);
      assert.isNotEmpty(env.getenv('RSTUDIO_TESTVAR_ONE'));
      assert.isNotEmpty(env.getenv('RSTUDIO_TESTVAR_TWO'));
      restore(testEnv);
    });
  });
  describe('Set and unset global environment variable', () => {
    it('Should set a variable, fetch, then remove it', () => {
      let result = env.getenv(envVarName);
      assert.isEmpty(result);
      env.setenv(envVarName, envValue);
      result = env.getenv(envVarName);
      assert.strictEqual(result, envValue);
      env.unsetenv(envVarName);
      result = env.getenv(envVarName);
      assert.isEmpty(result);
    });
  });
  describe('Expand variables in a string using environment', () => {
    it('Should return original string if it contained no variables', () => {
      const environment = { FOO: 'bar', ZOOM: 'car' };
      const input = '~/.local/share';
      const result = env.expandEnvVars(environment, input);
      assert.strictEqual(result, input);
    });
    it('Should return original string if environment contains no matching variables', () => {
      const environment = { FOO: 'bar', ZOOM: 'car' };
      const input = '$HOME/.local/share';
      const result = env.expandEnvVars(environment, input);
      assert.strictEqual(result, input);
    });
    it('Should substitute value from environment using bare form ($FOO)', () => {
      const environment = { FOO: 'bar', ZOOM: 'car' };

      const input = 'C:\\HELLO\\$FOO';
      const result = env.expandEnvVars(environment, input);
      assert.strictEqual(result, 'C:\\HELLO\\bar');
    });
    it('Should substitute multiple values from environment using bare form ($FOO)', () => {
      const environment = { FOO: 'bar', ZOOM: 'car' };
      const input = '/usr/HELLO/$ZOOM/misc/$FOO/etc/';
      const result = env.expandEnvVars(environment, input);
      assert.strictEqual(result, '/usr/HELLO/car/misc/bar/etc/');
    });
    it('Should substitute value from environment using curly brace form (${FOO})', () => {
      const environment = { FOO: 'bar', ZOOM: 'car' };
      const input = 'C:\\HELLO\\${FOO}';
      const result = env.expandEnvVars(environment, input);
      assert.strictEqual(result, 'C:\\HELLO\\bar');
    });
    it('Should expand all instances of a variable', () => {
      const environment = {
        VAR1: 'foo',
        VAR2: 'bar',
        VAR3: 'baz',
      };
      const input = 'Metasyntactic variables include $VAR1, $VAR2, and $VAR3, ' + 'but $VAR1 is used most often.';
      const expanded = 'Metasyntactic variables include foo, bar, and baz, ' + 'but foo is used most often.';
      const result = env.expandEnvVars(environment, input);
      assert.strictEqual(result, expanded);
    });
    it('Should not expand partially matching variables', () => {
      const environment = { VAR: 'foo' };
      const input = 'I think $VAR is a nice name for a $VARIABLE.';
      const expanded = 'I think foo is a nice name for a $VARIABLE.';
      const result = env.expandEnvVars(environment, input);
      assert.strictEqual(result, expanded);
    });
  });
});
