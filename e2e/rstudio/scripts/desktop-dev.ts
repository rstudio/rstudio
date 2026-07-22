// Entry point for `npm run test:desktop-dev`.
//
// Ensures the C++ session build is current and that GWT devmode is up,
// then invokes Playwright against the desktop project with PW_RSTUDIO_DEV=1
// so the desktop.fixture launches the in-tree Electron dev build.
//
// In a checkout without a configured build/ (typically a secondary worktree
// bootstrapped with scripts/bootstrap-worktree.sh), the cmake build is
// skipped and the launcher is pointed at the worktree's dev shim instead --
// see resolveCppBuildOutput in dev-common.ts.

import { checkGwtBuildReady, resolveCppBuildOutput, runCmakeBuild, runPlaywright } from './dev-common';

const TAG = 'desktop-dev';

const buildOutputEnv = resolveCppBuildOutput(TAG);
if (buildOutputEnv === null)
  runCmakeBuild(TAG);
checkGwtBuildReady(TAG);
runPlaywright(TAG, ['--project=desktop', ...process.argv.slice(2)], {
  PW_RSTUDIO_DEV: '1',
  ...buildOutputEnv,
});
