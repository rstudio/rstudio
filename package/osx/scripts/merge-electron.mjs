
import 'process';
import { execSync } from 'child_process';
import { existsSync, rmSync } from 'fs';
import { makeUniversalApp } from '@electron/universal';

const [node, script, x64AppPath, arm64AppPath, outPath] = process.argv;

let tmpPath = "";
let hasX64Build = existsSync(x64AppPath);
let hasArm64Build = existsSync(arm64AppPath);
let builtUniversal = false;

if (hasX64Build && hasArm64Build) {

  // build universal application in temporary directory,
  // then merge it all together when we're done
  tmpPath = `${outPath}.tmp`;
  builtUniversal = true;

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

} else if (hasX64Build) {

  console.log("# Building x86_64 Electron application");
  console.log(`- [i] x64AppPath: ${x64AppPath}`)
  tmpPath = x64AppPath;

} else if (hasArm64Build) {

  console.log("# Building arm64 Electron application");
  console.log(`- [i] arm64AppPath: ${arm64AppPath}`)
  tmpPath = arm64AppPath;

} else {

  console.error("Error: no Electron application builds found");
  console.error(`- [i] x64AppPath: ${x64AppPath}`)
  console.error(`- [i] arm64AppPath: ${arm64AppPath}`)
  process.exit(1);

}

// use rsync to move them into the final install path
console.log("- [i] Merging desktop and session packages ...")
execSync(`rsync -a "${tmpPath}/" "${outPath}/"`, { stdio: 'inherit' });
console.log("- [i] Done!")

// clean up the temporary universal build directory
if (builtUniversal) {
  rmSync(tmpPath, { recursive: true });
}

