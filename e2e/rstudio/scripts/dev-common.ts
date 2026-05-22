// Shared helpers for the dev-mode Playwright runners (test:desktop-dev,
// test:server-dev). These keep the build and codeserver state current so
// that a `npm run test:*-dev` invocation actually exercises the user's
// uncommitted changes.

import { spawnSync } from 'node:child_process';
import * as fs from 'node:fs';
import * as path from 'node:path';

export const REPO_ROOT = path.resolve(__dirname, '../../..');
export const BUILD_DIR = path.join(REPO_ROOT, 'build');

export function step(tag: string, msg: string): void {
  console.log(`\n[${tag}] ${msg}`);
}

export function fail(tag: string, msg: string): never {
  console.error(`\n[${tag}] error: ${msg}`);
  process.exit(1);
}

// Configure the build directory if it has never been initialized. Presence
// of CMakeCache.txt is the authoritative signal -- an empty build/ dir can
// exist for various reasons but only a configured tree has the cache file.
function ensureBuildDirConfigured(tag: string): void {
  if (fs.existsSync(path.join(BUILD_DIR, 'CMakeCache.txt'))) {
    return;
  }

  step(tag, 'Configuring build directory (first run)...');
  fs.mkdirSync(BUILD_DIR, { recursive: true });

  const result = spawnSync(
    'cmake',
    ['-S', REPO_ROOT, '-B', BUILD_DIR, '-DCMAKE_EXPORT_COMPILE_COMMANDS=1'],
    { stdio: 'inherit' },
  );

  if (result.status !== 0) {
    fail(tag, `cmake configure failed with exit code ${result.status ?? 'null'}`);
  }
}

// Run an incremental C++ build. When nothing has changed this is a no-op
// of a few seconds; when something has changed it picks up just those
// translation units. Either way it's cheaper than letting tests run
// against a stale session binary.
export function runCmakeBuild(tag: string): void {
  ensureBuildDirConfigured(tag);

  step(tag, 'Running cmake --build (incremental)...');
  const result = spawnSync('cmake', ['--build', BUILD_DIR], { stdio: 'inherit' });

  if (result.status !== 0) {
    fail(tag, `cmake --build failed with exit code ${result.status ?? 'null'}`);
  }
}

// GWT devmode runs as a Java process whose command line ends with
// `org.rstudio.studio.RStudioSuperDevMode`. That token is unique to this
// project, so matching it avoids colliding with other Java processes.
export function isGwtDevmodeRunning(): boolean {
  if (process.platform === 'win32') {
    // wmic is deprecated on recent Windows but still ships; PowerShell's
    // Get-CimInstance is the modern replacement. Try wmic first since it
    // is cheaper; fall back to PowerShell if wmic is unavailable.
    const wmic = spawnSync(
      'wmic',
      ['process', 'where', "name='java.exe'", 'get', 'commandline'],
      { encoding: 'utf8' },
    );

    if (wmic.status === 0) {
      return /RStudioSuperDevMode/.test(wmic.stdout);
    }

    const ps = spawnSync(
      'powershell',
      [
        '-NoProfile',
        '-Command',
        "Get-CimInstance Win32_Process -Filter \"name = 'java.exe'\" | Select-Object -ExpandProperty CommandLine",
      ],
      { encoding: 'utf8' },
    );

    return ps.status === 0 && /RStudioSuperDevMode/.test(ps.stdout);
  }

  const result = spawnSync('pgrep', ['-f', 'RStudioSuperDevMode'], { stdio: 'pipe' });
  return result.status === 0;
}

// True if a precompiled GWT bootstrap exists on disk. `ant draft` writes
// this; presence is enough to know the dev build can serve a working IDE
// without devmode. We don't bother comparing source mtimes -- if the user
// edits Java without re-running `ant draft`, that's on them.
function hasPrecompiledGwt(): boolean {
  return fs.existsSync(path.join(REPO_ROOT, 'src/gwt/www/rstudio/rstudio.nocache.js'));
}

export function checkGwtBuildReady(tag: string): void {
  step(tag, 'Checking GWT build state...');

  if (isGwtDevmodeRunning()) {
    console.log(`[${tag}] GWT devmode is running.`);
    return;
  }

  if (hasPrecompiledGwt()) {
    console.log(`[${tag}] Precompiled GWT bootstrap present (devmode not running).`);
    return;
  }

  console.log(
    `[${tag}] WARNING: no GWT build available. Tests will likely fail until you run one of:\n` +
      '    (cd src/gwt && ant devmode)   # active development\n' +
      '    (cd src/gwt && ant draft)     # one-shot precompile',
  );
}

// Spawn `npx playwright test ...` with the supplied args appended and the
// supplied env merged on top of process.env. Inherits stdio so the user
// sees the live test output, and propagates the playwright exit code.
export function runPlaywright(
  tag: string,
  extraArgs: string[],
  env: Record<string, string> = {},
): never {
  step(tag, 'Running Playwright tests...');

  const npx = process.platform === 'win32' ? 'npx.cmd' : 'npx';
  const args = ['playwright', 'test', ...extraArgs];

  const result = spawnSync(npx, args, {
    stdio: 'inherit',
    env: { ...process.env, ...env },
  });

  process.exit(result.status ?? 1);
}
