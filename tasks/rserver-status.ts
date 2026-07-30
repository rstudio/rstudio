// Report which development RStudio Servers are running.
//
//     npm run rserver-status -- [<checkout>] [--all]
//
// Without --all this reports on a single checkout. With --all it walks every
// git worktree of the repository, which is the useful view when several
// worktrees are serving at once.

import { execFileSync } from 'node:child_process';
import * as fs from 'node:fs';
import {
  type Instance,
  fail,
  instanceFile,
  parseArgs,
  pidMatches,
  readInstance,
  rejectUnknownFlags,
  requirePosix,
  resolveCheckout,
  step,
} from './common.ts';

const TAG = 'rserver-status';

const KNOWN_FLAGS = ['path', 'all', 'help'];

const USAGE = `
Usage: npm run rserver-status -- [<checkout>] [--all]

Reports the development RStudio Servers started by rserver-dev.

Options:
  --path=<dir>   Checkout to report on (same as the positional argument)
  --all          Report on every git worktree of this repository
  --help         Show this message
`.trim();

/** Every worktree path of the repository containing `checkout`, including it. */
function gitWorktrees(checkout: string): string[] {
  try {
    const out = execFileSync('git', ['worktree', 'list', '--porcelain'], {
      cwd: checkout,
      encoding: 'utf8',
    });

    return out
      .split('\n')
      .filter((line) => line.startsWith('worktree '))
      .map((line) => line.slice('worktree '.length).trim())
      .filter((dir) => fs.existsSync(dir));
  } catch {
    // Not a git checkout, or git is unavailable: report on the one we know.
    return [checkout];
  }
}

function reportInstance(instance: Instance): void {
  const rserverAlive = pidMatches(instance.rserverPid, 'rserver');
  const gwtAlive = instance.gwtPid !== null && pidMatches(instance.gwtPid, 'devmode');

  console.log(`  ${instance.checkout}`);
  console.log(`    url:      ${instance.url}  [${rserverAlive ? 'running' : 'DEAD'}]`);
  console.log(`    rserver:  pid ${instance.rserverPid}`);

  if (instance.gwt === 'devmode') {
    console.log(
      `    devmode:  pid ${instance.gwtPid} on port ${instance.codeServerPort}  [${gwtAlive ? 'running' : 'DEAD'}]`,
    );
  } else {
    console.log(`    gwt:      ${instance.gwt} (no code server)`);
  }

  console.log(`    started:  ${instance.startedAt}`);

  if (!rserverAlive) {
    console.log(`    note:     stale record; clear it with \`npm run rserver-stop -- ${instance.checkout}\``);
  }
}

function main(): void {
  const args = parseArgs(process.argv.slice(2));
  if (args.flags.get('help')) {
    console.log(USAGE);
    return;
  }

  rejectUnknownFlags(TAG, args, KNOWN_FLAGS);
  requirePosix(TAG);

  const pathFlag = args.flags.get('path');
  const checkout = resolveCheckout(TAG, typeof pathFlag === 'string' ? pathFlag : args.positional[0]);
  const roots = args.flags.get('all') ? gitWorktrees(checkout) : [checkout];

  const instances = roots
    .filter((root) => fs.existsSync(instanceFile(root)))
    .map((root) => readInstance(root))
    .filter((instance): instance is Instance => instance !== null);

  if (instances.length === 0) {
    step(TAG, args.flags.get('all') ? 'No dev servers recorded in any worktree.' : `No dev server recorded for ${checkout}.`);
    return;
  }

  console.log('');
  for (const instance of instances) {
    reportInstance(instance);
    console.log('');
  }
}

main();
