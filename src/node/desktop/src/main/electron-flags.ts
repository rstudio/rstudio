/*
 * electron-flags.ts
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the GNU
 * Affero General Public License. This program is distributed WITHOUT ANY
 * EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 */

import { existsSync, readFileSync } from 'fs';
import path from 'path';

export const kOzonePlatformSwitch = 'ozone-platform';

export interface ElectronFlag {
  name: string;
  value?: string;
}

export interface ElectronFlagsConfig {
  path: string;
  flags: ElectronFlag[];
}

/**
 * Parse the line-based electron-flags.conf format.
 *
 * This intentionally preserves the existing syntax: only lines beginning
 * with '--' are recognized, and the first '=' separates a switch name from
 * its value.
 */
export function parseElectronFlags(contents: string): ElectronFlag[] {
  const flags: ElectronFlag[] = [];

  for (const line of contents.split(/\r?\n/)) {
    if (!line.startsWith('--')) {
      continue;
    }

    const equalsIndex = line.indexOf('=');
    if (equalsIndex === -1) {
      flags.push({ name: line.substring(2) });
    } else {
      flags.push({
        name: line.substring(2, equalsIndex),
        value: line.substring(equalsIndex + 1),
      });
    }
  }

  return flags;
}

/**
 * Find and parse electron-flags.conf, honoring the supplied directory
 * precedence. No application or command-line state is modified here.
 */
export function loadElectronFlags(configDirs: readonly string[]): ElectronFlagsConfig | undefined {
  for (const configDir of configDirs) {
    const configPath = path.join(configDir, 'electron-flags.conf');
    if (!existsSync(configPath)) {
      continue;
    }

    return {
      path: configPath,
      flags: parseElectronFlags(readFileSync(configPath, { encoding: 'utf-8' })),
    };
  }

  return undefined;
}

/**
 * Return the effective configured Ozone platform. As with command-line
 * switch handling, the last occurrence wins.
 */
export function getConfiguredOzonePlatform(flags: readonly ElectronFlag[]): string | undefined {
  let platform: string | undefined;

  for (const flag of flags) {
    if (flag.name === kOzonePlatformSwitch) {
      platform = flag.value;
    }
  }

  return platform;
}

/**
 * Return the effective Ozone platform in a process argv. A bare switch is
 * represented by an empty value, matching Electron's command-line API.
 */
export function getOzonePlatformFromArgs(args: readonly string[]): string | undefined {
  const prefix = `--${kOzonePlatformSwitch}`;
  let platform: string | undefined;

  for (const arg of args) {
    if (arg === prefix) {
      platform = '';
    } else if (arg.startsWith(`${prefix}=`)) {
      platform = arg.substring(prefix.length + 1);
    }
  }

  return platform;
}

export function shouldRelaunchForOzonePlatform(
  currentPlatform: string | undefined,
  configuredPlatform: string | undefined,
): boolean {
  return configuredPlatform !== undefined && currentPlatform !== configuredPlatform;
}

function isOzonePlatformArg(arg: string): boolean {
  const prefix = `--${kOzonePlatformSwitch}`;
  return arg === prefix || arg.startsWith(`${prefix}=`);
}

/**
 * Build the arguments accepted by app.relaunch({ args }). Electron expects
 * this list without the executable path. Development launches retain the
 * application path as the first argument; packaged launches do not have one.
 */
export function buildRelaunchArgs(
  processArgs: readonly string[],
  configuredPlatform: string,
  isPackaged: boolean,
): string[] {
  const ozoneArg = `--${kOzonePlatformSwitch}=${configuredPlatform}`;
  const args: string[] = [];
  let ozoneArgAdded = false;

  for (const arg of processArgs.slice(1)) {
    if (!isOzonePlatformArg(arg)) {
      args.push(arg);
      continue;
    }

    if (!ozoneArgAdded) {
      args.push(ozoneArg);
      ozoneArgAdded = true;
    }
  }

  if (!ozoneArgAdded) {
    // In development mode, processArgs.slice(1)[0] is the Electron app path.
    // Keep the switch after it so the relaunch has the same argv shape.
    const insertionIndex = isPackaged ? 0 : Math.min(1, args.length);
    args.splice(insertionIndex, 0, ozoneArg);
  }

  return args;
}
