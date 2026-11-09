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
import { statSync } from 'fs';
import path from 'path';

/**
 * The shell's logical working directory, as the shell helper's "$PWD/$arg"
 * sees it. process.cwd() resolves symlinks, so prefer PWD when it still names
 * the same directory.
 */
function logicalCwd(env: NodeJS.ProcessEnv): string {
  const cwd = process.cwd();
  const pwd = env.PWD;
  if (!pwd || !path.isAbsolute(pwd)) {
    return cwd;
  }

  try {
    const logical = statSync(pwd);
    const physical = statSync(cwd);
    if (logical.dev === physical.dev && logical.ino === physical.ino) {
      return pwd;
    }
  } catch {
    // a stale PWD falls back to the physical directory
  }
  return cwd;
}

/**
 * Handle the terminal's rstudio command even when a shell profile has removed
 * the postback helper from PATH. Return undefined for a normal Desktop launch,
 * otherwise an exit status. Keep the CLI consistent with session/postback/rstudio.
 *
 * Only a command naming at least one file is treated as the terminal command;
 * switch-only and bare launches (e.g. a dev build's --no-sandbox, or the
 * installed binary by absolute path) still start Desktop from a terminal, as
 * does a command naming a project file (e.g. xdg-open foo.Rproj).
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

  // Operands are arguments not starting with '-', or anything after '--'.
  const separator = args.indexOf('--');
  const operands = args.filter((arg, index) => !arg.startsWith('-') || (separator !== -1 && index > separator));
  if (operands.length === 0) {
    return undefined;
  }

  // Desktop opens project files; the session's openfile would edit them as text.
  if (operands.some((arg) => path.extname(arg).toLowerCase() === '.rproj')) {
    return undefined;
  }

  const usage = () => console.error('usage: rstudio [--] file[:line[:column]]...');
  let files = args;
  if (args[0] === '--') {
    files = args.slice(1);
  } else if (args[0] === '-h' || args[0] === '--help') {
    usage();
    return 0;
  } else if (args[0].startsWith('-')) {
    console.error(`rstudio: unknown option '${args[0]}'`);
    usage();
    return 1;
  }

  // Do not resolve the executable through PATH, which is the source of the
  // problem. An invalid postback path must fail instead of starting Desktop.
  if (!path.isAbsolute(env.RS_RPOSTBACK_PATH)) {
    console.error('rstudio: RS_RPOSTBACK_PATH must be an absolute path');
    return 1;
  }

  const cwd = logicalCwd(env);
  let status = 0;
  for (const file of files) {
    // Preserve :line[:column] and symlink/.. components for the session to
    // interpret. Its working directory can differ from the terminal's.
    const target = path.isAbsolute(file) ? file : `${cwd}/${file}`;
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
