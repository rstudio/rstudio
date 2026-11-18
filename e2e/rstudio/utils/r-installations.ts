// The R installations on the machine running the tests, for specs that
// switch the session's R (desktop only). They skip when there is no second R
// to switch to.

import { execFileSync } from 'child_process';
import { readFileSync, realpathSync } from 'fs';

export interface InstalledR {
  version: string;
  binary: string;
}

// The R installations rig knows about on this machine, from both of rig's
// modes (each lists only its own installations). Empty when rig is absent.
export function installedRVersions(): InstalledR[] {
  const versions: InstalledR[] = [];
  for (const mode of ['--admin', '--user']) {
    try {
      const output = execFileSync('rig', ['list', '--json', mode], {
        encoding: 'utf-8',
        stdio: ['ignore', 'pipe', 'ignore'],
      });
      for (const entry of JSON.parse(output) as InstalledR[]) {
        if (entry.version && entry.binary)
          versions.push(entry);
      }
    } catch {
      // rig missing, or this mode has nothing to list
    }
  }
  return versions;
}

export function majorMinor(version: string): string {
  return version.split('.').slice(0, 2).join('.');
}

export function compareVersions(a: string, b: string): number {
  const pa = a.split('.').map(Number);
  const pb = b.split('.').map(Number);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const diff = (pa[i] ?? 0) - (pb[i] ?? 0);
    if (diff !== 0)
      return diff;
  }
  return 0;
}

// Whether an installation runs as itself when it isn't the default: false for
// a macOS framework version whose launcher still names the framework's shared
// home (see src/node/desktop/src/main/r-framework.ts), which RStudio offers
// to update before switching to it. The framework's current version is what
// that shared home resolves to, so it runs as itself regardless, as
// .rs.rInstallations.isOrthogonal() has it.
export function isOrthogonal(binary: string): boolean {
  const match = /^(.*\/R\.framework\/Versions)\/([^/]+)\/Resources\/bin\/R$/.exec(binary);
  if (!match)
    return true;

  try {
    if (realpathSync(`${match[1]}/Current`) === realpathSync(`${match[1]}/${match[2]}`))
      return true;
  } catch {
    // no current version to compare with
  }

  try {
    return !readFileSync(binary, 'utf-8').includes('R.framework/Resources');
  } catch {
    return true;
  }
}

// Whether an installation is built for a different architecture than the
// machine's (an x86_64 R on an Apple silicon Mac), going by rig's naming.
export function isEmulated(binary: string): boolean {
  return process.platform === 'darwin' && process.arch === 'arm64' && /-x86_64\//.test(binary);
}

// The newest installed R from a major.minor series other than the current
// one, among those that do (or don't) run as themselves.
export function otherR(current: string, orthogonal: boolean): InstalledR | undefined {
  return installedRVersions()
    .filter((entry) => majorMinor(entry.version) !== majorMinor(current))
    .filter((entry) => isOrthogonal(entry.binary) === orthogonal)
    .sort((a, b) => compareVersions(b.version, a.version))[0];
}
