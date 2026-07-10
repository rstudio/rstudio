// Entry point for the non-dev Playwright test tasks (npm run test:desktop,
// test:server, test:autocomplete). Unlike the *-dev runners this does NOT
// trigger an incremental build -- it runs Playwright against an
// already-installed/built RStudio.
//
// Routing these through a script (rather than long inline commands in
// package.json) keeps the server env setup readable and ensures every run
// prints the HTML report location on exit via runPlaywright.
//
// Any extra arguments are forwarded to `playwright test`, e.g.
//   npm run test:desktop -- autocomplete --headed

import { runPlaywright } from './dev-common';

const TAG = 'test';

const extraArgs = process.argv.slice(2);

// Detect server mode from either an explicit --project=server (in any of its
// spellings) or a pre-set PW_RSTUDIO_MODE, matching how playwright.config.ts
// resolves the project.
function isServerMode(args: string[]): boolean {
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--project' && args[i + 1]?.toLowerCase() === 'server')
      return true;
    if (args[i].toLowerCase() === '--project=server')
      return true;
  }
  return (process.env.PW_RSTUDIO_MODE ?? '').toLowerCase() === 'server';
}

// For server runs, point at an installed RStudio Server build by default. Both
// are overridable from the shell, so a pre-set value always wins.
const env: Record<string, string> = {};
if (isServerMode(extraArgs)) {
  if (!process.env.PW_RSERVER_BIN)
    env.PW_RSERVER_BIN = '/usr/lib/rstudio-server/bin/rserver';
  if (!process.env.PW_RSERVER_CONF)
    env.PW_RSERVER_CONF = '/etc/rstudio/rserver.conf';
}

runPlaywright(TAG, extraArgs, env);
