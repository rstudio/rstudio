
import 'process';
import { execSync } from 'child_process';
import { makeUniversalApp } from '@electron/universal';
import { rm } from 'fs/promises';

const [ node, script, x64AppPath, arm64AppPath, outPath ] = process.argv;

// build universal application in temporary directory,
// then merge it all together when we're done
const tmpPath = `${outPath}.tmp`;

// merge the two builds together
console.log("# Building universal Desktop application.")
console.log(`- [i] x64AppPath: ${x64AppPath}`)
console.log(`- [i] arm64AppPath: ${arm64AppPath}`)
await makeUniversalApp({
  x64AppPath: x64AppPath,
  arm64AppPath: arm64AppPath,
  outAppPath: tmpPath,
  force: true,
});

// use rsync to move them into the final install path
console.log("- [i] Merging desktop and session packages ...")
execSync(`rsync -a "${tmpPath}/" "${outPath}/"`, { stdio: 'inherit' });
console.log("- [i] Done!")

// clean up
await rm(tmpPath, { recursive: true });

