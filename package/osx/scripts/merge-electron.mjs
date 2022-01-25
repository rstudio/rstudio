
import 'process';
import { execSync } from 'child_process';
import { makeUniversalApp } from '@electron/universal';
import { rm } from 'fs/promises';

const [ node, script, x64AppPath, arm64AppPath, outPath ] = process.argv;

// build universal application in temporary directory,
// then merge it all together when we're done
const tmpPath = `${outPath}.tmp`;

// merge the two builds together
await makeUniversalApp({
  x64AppPath: x64AppPath,
  arm64AppPath: arm64AppPath,
  outAppPath: tmpPath,
  force: true,
});

// use rsync to move them into the final install path
execSync(`rsync -azvhP "${tmpPath}/" "${outPath}/"`, { stdio: 'inherit' });

// clean up
await rm(tmpPath, { recursive: true });

