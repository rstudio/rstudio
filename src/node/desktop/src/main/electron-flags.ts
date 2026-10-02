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
import { safeError } from '../core/err';

export const kOzonePlatformSwitch = 'ozone-platform';

// Chromium aborts during Ozone initialization, before main.ts runs and so
// without any log or error dialog, if it is asked for a backend it lacks.
const kSupportedOzonePlatforms = ['x11', 'wayland'];

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
 * its value. Trailing whitespace is dropped.
 */
export function parseElectronFlags(contents: string): ElectronFlag[] {
  const flags: ElectronFlag[] = [];

  for (const rawLine of contents.split(/\r?\n/)) {
    const line = rawLine.trimEnd();
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

    let contents: string;
    try {
      contents = readFileSync(configPath, { encoding: 'utf-8' });
    } catch (error: unknown) {
      // tsconfig's es2021 lib has no Error `cause` option; the message carries it instead
      // eslint-disable-next-line preserve-caught-error
      throw new Error(
        `Unable to read Electron flags from ${configPath}: ${safeError(error).message}. ` +
          'Make sure it is a readable file, or remove it.',
      );
    }

    return { path: configPath, flags: parseElectronFlags(contents) };
  }

  return undefined;
}

export function isOzonePlatformArg(arg: string): boolean {
  const prefix = `--${kOzonePlatformSwitch}`;
  return arg === prefix || arg.startsWith(`${prefix}=`);
}

interface RequestedOzonePlatform {
  platform: string;
  source: string;
}

function requestedOzonePlatform(
  chromiumArguments: string,
  config: ElectronFlagsConfig | undefined,
): RequestedOzonePlatform | undefined {
  // last occurrence wins, as it does for Chromium's own switch parsing; a
  // bare switch has an empty value, which is then reported as unsupported
  const fromEnv = chromiumArguments.split(' ').filter(isOzonePlatformArg).pop();
  if (fromEnv !== undefined) {
    const platform = fromEnv.substring(`--${kOzonePlatformSwitch}=`.length);
    return { platform, source: 'RSTUDIO_CHROMIUM_ARGUMENTS' };
  }

  const fromConfig = config?.flags.filter((flag) => flag.name === kOzonePlatformSwitch).pop();
  if (config && fromConfig) {
    return { platform: fromConfig.value ?? '', source: config.path };
  }

  return undefined;
}

export interface OzonePlatformInputs {
  /** process.argv of the current launch */
  argv: readonly string[];
  /** the --ozone-platform value Chromium started with, if any */
  currentPlatform: string | undefined;
  /** the value of RSTUDIO_CHROMIUM_ARGUMENTS */
  chromiumArguments: string;
  config: ElectronFlagsConfig | undefined;
  isPackaged: boolean;
}

export interface OzoneRelaunchPlan {
  /** arguments for app.relaunch(), when a relaunch is needed */
  relaunchArgs?: string[];
  /** explains the decision, when a requested value is used or ignored */
  message?: string;
}

/**
 * Decide whether to relaunch so that Chromium starts with the requested Ozone
 * platform. A value on the command line takes precedence over
 * RSTUDIO_CHROMIUM_ARGUMENTS, which takes precedence over electron-flags.conf.
 */
export function planOzoneRelaunch(inputs: OzonePlatformInputs): OzoneRelaunchPlan {
  const requested = requestedOzonePlatform(inputs.chromiumArguments, inputs.config);
  if (!requested || requested.platform === inputs.currentPlatform) {
    return {};
  }

  const { platform, source } = requested;
  const switchText = `--${kOzonePlatformSwitch}=${platform}`;

  // this also covers a process that was already relaunched by this code
  if (inputs.argv.some(isOzonePlatformArg)) {
    return { message: `Ignoring ${switchText} from ${source}: the command line sets --${kOzonePlatformSwitch}` };
  }

  if (!kSupportedOzonePlatforms.includes(platform)) {
    return {
      message: `Ignoring ${switchText} from ${source}: expected one of ${kSupportedOzonePlatforms.join(', ')}`,
    };
  }

  // electron-forge stops its dev server when the first process exits, which
  // would leave the relaunched process with nothing to load
  if (!inputs.isPackaged) {
    return {
      message: `Ignoring ${switchText} from ${source} in a development build; pass it on the command line instead`,
    };
  }

  const relaunchArgs = [...inputs.argv.slice(1), switchText];
  return {
    relaunchArgs,
    message:
      `Relaunching with ${switchText} from ${source} ` +
      `(started with ${inputs.currentPlatform ?? 'none'}): ${relaunchArgs.join(' ')}`,
  };
}
