// Stop the development RStudio Server started by rserver-dev.
//
//     npm run rserver-stop -- [<checkout>]
//
// Reads <checkout>/.rstudio-dev/instance.json and shuts down whatever it
// describes. Safe to run when nothing (or only part of it) is still running.

import {
  fail,
  parseArgs,
  readInstance,
  rejectUnknownFlags,
  requirePosix,
  resolveCheckout,
  step,
  stopInstance,
} from './common.ts';

const TAG = 'rserver-stop';

const KNOWN_FLAGS = ['path', 'help'];

const USAGE = `
Usage: npm run rserver-stop -- [<checkout>]

Stops the development RStudio Server running for an RStudio checkout or git
worktree (default: the checkout these tasks live in).

Options:
  --path=<dir>   Checkout to stop (same as the positional argument)
  --help         Show this message
`.trim();

async function main(): Promise<void> {
  const args = parseArgs(process.argv.slice(2));
  if (args.flags.get('help')) {
    console.log(USAGE);
    return;
  }

  rejectUnknownFlags(TAG, args, KNOWN_FLAGS);
  requirePosix(TAG);

  const pathFlag = args.flags.get('path');
  const checkout = resolveCheckout(TAG, typeof pathFlag === 'string' ? pathFlag : args.positional[0]);

  const instance = readInstance(checkout);
  if (!instance) {
    step(TAG, `No dev server recorded for ${checkout}; nothing to stop.`);
    return;
  }

  step(TAG, `Stopping the dev server at ${instance.url} (${checkout})...`);
  await stopInstance(TAG, instance);
  step(TAG, 'Done.');
}

main().catch((e: unknown) => fail(TAG, (e as Error).message));
