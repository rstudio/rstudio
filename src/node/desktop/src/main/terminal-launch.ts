/*
 * terminal-launch.ts
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

import { spawnSync } from 'child_process';
import path from 'path';

/**
 * Handle the terminal's rstudio command even when a shell profile has removed
 * the postback helper from PATH. Return undefined for a normal Desktop launch,
 * otherwise an exit status. Keep the CLI consistent with session/postback/rstudio.
 */
export function forwardTerminalCommand(
  args: string[],
  env: NodeJS.ProcessEnv = process.env,
  platform: NodeJS.Platform = process.platform,
): number | undefined {
  // The postback command is not installed on Windows. RS_RPOSTBACK_PATH alone
  // also exists in R child processes, so require the terminal marker as well.
  if (platform === 'win32' || !env.RSTUDIO_TERM || !env.RS_RPOSTBACK_PATH) {
    return undefined;
  }

  const usage = () => console.error('usage: rstudio [--] file[:line[:column]]...');
  let files = args;
  if (args[0] === '--') {
    files = args.slice(1);
  } else if (args[0] === '-h' || args[0] === '--help') {
    usage();
    return 0;
  } else if (args[0]?.startsWith('-')) {
    console.error(`rstudio: unknown option '${args[0]}'`);
    usage();
    return 1;
  }

  if (files.length === 0) {
    usage();
    return 1;
  }

  // Do not resolve the executable through PATH, which is the source of the
  // problem. An invalid postback path must fail instead of starting Desktop.
  if (!path.isAbsolute(env.RS_RPOSTBACK_PATH)) {
    console.error('rstudio: RS_RPOSTBACK_PATH must be an absolute path');
    return 1;
  }

  let status = 0;
  for (const file of files) {
    // Preserve :line[:column] and symlink/.. components for the session to
    // interpret. Its working directory can differ from the terminal's.
    const target = path.isAbsolute(file) ? file : `${process.cwd()}/${file}`;
    const result = spawnSync(env.RS_RPOSTBACK_PATH, ['openfile', target], { env, encoding: 'utf8' });
    if (result.stderr) {
      process.stderr.write(result.stderr);
    }
    if (result.status === 0) {
      if (result.stdout) {
        process.stdout.write(result.stdout);
      }
    } else {
      status = 1;
      if (result.stdout) {
        process.stderr.write(result.stdout);
      }
      if (result.error) {
        console.error(`rstudio: ${result.error.message}`);
      }
    }
  }
  return status;
}
