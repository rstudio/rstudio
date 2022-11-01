/*
 * environment.ts
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import process from 'process';

export type Environment = Record<string, string>;

/**
 * Get value of process environment variable; returns empty string it not found.
 */
export function getenv(name: string): string {
  return process.env[name] ?? '';
}

/**
 * Add given name=value to process environment.
 */
export function setenv(name: string, value: string): void {
  process.env[name] = value;
}

/**
 * Unset given environment variable in process environment.
 */
export function unsetenv(name: string): void {
  delete process.env[name];
}

/**
 * Expand environment variables in a string; for example /$USER/foo to
 * /bob/foo when USER=bob
 */
export function expandEnvVars(environment: Environment, str: string): string {
  let result = str;
  for (const name in environment) {
    // replace bare forms (/home/$USER)
    const reVar = new RegExp('\\$\\b' + name + '\\b', 'g');
    result = result.replace(reVar, environment[name]);

    // replace curly brace forms (/home/${USER})
    const reBraceVar = new RegExp('\\${' + name + '}', 'g');
    result = result.replace(reBraceVar, environment[name]);
  }
  return result;
}

/**
 * Apply Environment to current process environment
 *
 * @param vars Environment to set
 */
export function setVars(vars: Environment): void {
  for (const name in vars) {
    setenv(name, vars[name]);
  }
}
