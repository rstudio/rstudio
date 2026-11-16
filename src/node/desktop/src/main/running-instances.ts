/*
 * running-instances.ts
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

import { execFile } from 'child_process';

// Every RStudio window is its own process, and neither installer will swap
// the application while another of its processes is alive: the NSIS
// installer refuses to run beside any rstudio.exe, and Squirrel.Mac waits
// for every process of the bundle to exit. So before "Restart Now" quits
// this window, the updater asks how many other instances are running.

/**
 * The process ids in `tasklist /FO CSV /NH` output, e.g.
 *
 *     "rstudio.exe","1234","Console","1","123,456 K"
 *
 * Empty when nothing matched (tasklist then prints an INFO line instead).
 */
export function parseTasklistPids(output: string): number[] {
  const pids: number[] = [];
  for (const line of output.split(/\r?\n/)) {
    const match = /^"[^"]*","(\d+)"/.exec(line.trim());
    if (match) {
      pids.push(Number(match[1]));
    }
  }
  return pids;
}

/**
 * The pids in `ps -axo pid=,comm=` output whose executable is `execPath`, e.g.
 *
 *       812 /Applications/RStudio.app/Contents/MacOS/RStudio
 *       840 /Applications/RStudio.app/Contents/Frameworks/RStudio Helper.app/...
 *
 * The helper processes have their own executables, so only the main process
 * of each instance of this installation matches.
 */
export function parsePsPids(output: string, execPath: string): number[] {
  const pids: number[] = [];
  for (const line of output.split(/\r?\n/)) {
    const match = /^\s*(\d+)\s+(.*\S)\s*$/.exec(line);
    if (match && match[2] === execPath) {
      pids.push(Number(match[1]));
    }
  }
  return pids;
}

/**
 * Runs a command, with `env` added to its environment, and resolves to its
 * stdout, or null when it failed.
 */
export type CommandRunner = (command: string, args: string[], env?: Record<string, string>) => Promise<string | null>;

/**
 * Far longer than listing processes takes. The install preflight holds the
 * quit, and with it every later quit and project switch, until the listing
 * answers; a stalled tasklist.exe must answer "unknown" rather than never.
 */
export const kProcessListTimeoutMs = 30 * 1000;

export function execFileRunner(timeoutMs = kProcessListTimeoutMs): CommandRunner {
  return async (command, args, env) => {
    return new Promise((resolve) => {
      execFile(
        command,
        args,
        {
          windowsHide: true,
          maxBuffer: 16 * 1024 * 1024,
          timeout: timeoutMs,
          env: env ? { ...process.env, ...env } : undefined,
        },
        (error, stdout) => resolve(error ? null : stdout),
      );
    });
  };
}

/**
 * How many RStudio processes outside this application are running, or null
 * when that could not be determined. `ownPids` names this application's
 * processes (Electron's renderer, GPU and utility children run the same
 * executable on Windows and must not count). Platforms without an installer
 * to protect answer 0. The platform and the command runner are parameters
 * so the rules are testable anywhere.
 */
export async function otherInstanceCount(
  execPath: string,
  ownPids: number[],
  platform: NodeJS.Platform = process.platform,
  run: CommandRunner = execFileRunner(),
): Promise<number | null> {
  const own = new Set([process.pid, ...ownPids]);
  let pids: number[];
  if (platform === 'win32') {
    const output = await run('tasklist.exe', ['/FI', 'IMAGENAME eq rstudio.exe', '/FO', 'CSV', '/NH']);
    if (output === null) {
      return null;
    }
    pids = parseTasklistPids(output);
  } else if (platform === 'darwin') {
    const output = await run('/bin/ps', ['-axo', 'pid=,comm=']);
    if (output === null) {
      return null;
    }
    pids = parsePsPids(output, execPath);
  } else {
    return 0;
  }
  return pids.filter((pid) => !own.has(pid)).length;
}
