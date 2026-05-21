// Entry point for `npm run test:desktop-dev`.
//
// Ensures the C++ session build is current and that GWT devmode is up,
// then invokes Playwright against the desktop project with PW_RSTUDIO_DEV=1
// so the desktop.fixture launches the in-tree Electron dev build.

import { checkGwtDevmode, runCmakeBuild, runPlaywright } from './dev-common';

const TAG = 'desktop-dev';

runCmakeBuild(TAG);
checkGwtDevmode(TAG);
runPlaywright(TAG, ['--project=desktop', ...process.argv.slice(2)], { PW_RSTUDIO_DEV: '1' });
