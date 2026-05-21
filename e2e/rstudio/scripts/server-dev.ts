// Entry point for `npm run test:server-dev`.
//
// Ensures the C++ build (including rserver) is current and that GWT
// devmode is up so Java edits are reflected, then invokes Playwright
// against the server project. The server.fixture handles spawning the
// in-tree rserver-dev binary and config.

import { checkGwtDevmode, runCmakeBuild, runPlaywright } from './dev-common';

const TAG = 'server-dev';

runCmakeBuild(TAG);
checkGwtDevmode(TAG);
runPlaywright(TAG, ['--project=server', ...process.argv.slice(2)]);
