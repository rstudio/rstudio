// Start a development RStudio Server for a checkout or git worktree.
//
//     npm run rserver-dev -- [<checkout>] [options]
//
// Brings up two long-lived background processes -- the in-tree `rserver` and
// (by default) the GWT Super Dev Mode code server -- then prints the localhost
// URL to open. Both keep running after this task exits; `npm run rserver-stop`
// tears them down.
//
// Ports are chosen per instance, so several worktrees can serve at once.

import { spawn, spawnSync } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import * as fs from 'node:fs';
import { isIPv4, isIPv6 } from 'node:net';
import * as os from 'node:os';
import * as path from 'node:path';
import {
  DATA_DIR_PREFIX,
  type GwtMode,
  type Instance,
  cppBuildDir,
  currentUser,
  fail,
  findFreePort,
  flagNumber,
  logFile,
  parseArgs,
  pidMatches,
  readInstance,
  rejectUnknownFlags,
  requirePortFree,
  requirePosix,
  resolveCheckout,
  runCmakeBuild,
  stateDir,
  step,
  stopInstance,
  tailLog,
  waitForHttp,
  which,
  writeInstance,
} from './common.ts';

const TAG = 'rserver-dev';

const KNOWN_FLAGS = [
  'path',
  'port',
  'code-server-port',
  'www-address',
  'gwt',
  'build',
  'restart',
  'wait',
  'gwt-timeout',
  'help',
];

const USAGE = `
Usage: npm run rserver-dev -- [<checkout>] [options]

Starts a development RStudio Server for an RStudio checkout or git worktree
(default: the checkout these tasks live in).

Options:
  --path=<dir>              Checkout to serve (same as the positional argument)
  --port=<n>                rserver port (default: first free port from 8787)
  --code-server-port=<n>    GWT code server port (default: first free from 9876)
  --www-address=<addr>      Address rserver binds (default: 127.0.0.1)
  --gwt=<devmode|draft|none>
                            devmode (default) runs \`ant devmode\` for live
                            recompiles; draft runs a one-shot \`ant draft\`;
                            none reuses whatever is already in src/gwt/www
  --no-build                Skip the incremental cmake build
  --no-wait                 Return as soon as the processes are spawned
  --gwt-timeout=<seconds>   How long to wait for the code server (default: 900)
  --restart                 Stop an already-running instance and start fresh
  --help                    Show this message
`.trim();

/**
 * Validate --www-address and return a canonical copy. Only 'localhost' and
 * literal IP addresses make sense for a bind address, and rebuilding the value
 * from parsed numeric groups keeps the command-line string itself out of the
 * rserver spawn below, which the Snyk Code scan otherwise reports as command
 * injection (see rebuildFromDirectoryListings in common.ts).
 */
function canonicalizeAddress(raw: string): string {
  if (raw === 'localhost') {
    return 'localhost';
  }

  const rebuildIPv4 = (address: string): string => address.split('.').map(Number).join('.');

  if (isIPv4(raw)) {
    return rebuildIPv4(raw);
  }

  if (isIPv6(raw)) {
    // A group is empty (the '::' shorthand), an IPv4 dotted quad (mapped
    // addresses like ::ffff:127.0.0.1), or plain hex.
    return raw
      .split(':')
      .map((group) => {
        if (group === '') {
          return '';
        }
        if (isIPv4(group)) {
          return rebuildIPv4(group);
        }
        return parseInt(group, 16).toString(16);
      })
      .join(':');
  }

  fail(TAG, `--www-address must be 'localhost' or a literal IP address (got ${raw})`);
}

function parseGwtMode(raw: string | boolean | undefined): GwtMode {
  if (raw === undefined) {
    return 'devmode';
  }

  if (raw === 'devmode' || raw === 'draft' || raw === 'none') {
    return raw;
  }

  fail(TAG, `--gwt must be one of devmode, draft, none (got ${String(raw)})`);
}

/**
 * A private server-data-dir for this instance. rserver-dev.conf points every
 * server at /tmp/rstudio-server, which two instances would fight over; rserver
 * also creates Unix-domain sockets in here, and macOS caps sun_path at ~104
 * characters, so the name is kept short and anchored at the system temp dir.
 */
function makeDataDir(): string {
  return fs.mkdtempSync(path.join(os.tmpdir(), DATA_DIR_PREFIX));
}

/**
 * rserver refuses to start without a readable secure-cookie-key, and the
 * packaged default lives under /var/lib/rstudio-server where a developer has
 * no permission to create one. Must be at least 256 bits -- see
 * ensureKeyStrength() in server_core/http/SecureCookie.cpp.
 */
function writeSecureCookieKey(checkout: string): string {
  const file = path.join(stateDir(checkout), 'secure-cookie-key');
  fs.writeFileSync(file, randomBytes(32).toString('hex'), { mode: 0o600 });
  return file;
}

/**
 * Fail fast if a non-default code server port was chosen but the checkout's
 * build.xml predates the codeserver.port property. Such a build.xml ignores
 * the -Dcodeserver.port override, DevMode starts on 9876 regardless, and the
 * readiness wait below would poll a dead port for its full 15-minute timeout.
 */
function requireCodeServerPortPlumbing(checkout: string, port: number): void {
  if (port === 9876) {
    return;
  }

  const buildXml = path.join(checkout, 'src', 'gwt', 'build.xml');
  if (!fs.readFileSync(buildXml, 'utf8').includes('codeserver.port')) {
    fail(
      TAG,
      `code server port ${port} was requested, but ${buildXml} predates the
       codeserver.port property and DevMode would start on 9876 regardless.
       Update that checkout, or free port 9876 so the default can be used.`,
    );
  }
}

/**
 * True if src/gwt/www holds a Super Dev Mode bootstrap rather than a real
 * compile. Such a stub only redirects to a code server, so serving it without
 * one running leaves the IDE dead at startup ("code server not available").
 */
function wwwIsSuperDevStub(checkout: string): boolean {
  const nocache = path.join(checkout, 'src', 'gwt', 'www', 'rstudio', 'rstudio.nocache.js');
  try {
    return fs.readFileSync(nocache, 'utf8').includes('__gwt_sdm');
  } catch {
    return false;
  }
}

/**
 * Report a readiness failure. The spawned processes are deliberately left
 * running -- instance.json was written before the waits began, so the logs are
 * still there to inspect and rserver-stop can clean up.
 */
function failAfterSpawn(message: string, log: string, checkout: string): never {
  const target = checkout === OWN_CHECKOUT ? '' : ` -- ${checkout}`;
  fail(
    TAG,
    `${message}\n${tailLog(log)}\n\n` +
      `       The processes were left running; stop them with \`npm run rserver-stop${target}\`.`,
  );
}

function runAntDraft(tag: string, checkout: string): void {
  step(tag, 'Compiling the GWT front end (ant draft)...');
  const result = spawnSync('ant', ['draft'], {
    cwd: path.join(checkout, 'src', 'gwt'),
    stdio: 'inherit',
  });

  if (result.status !== 0) {
    fail(tag, `ant draft failed with exit code ${result.status ?? 'null'}`);
  }
}

/** The checkout these tasks were invoked from, used to keep hints short. */
const OWN_CHECKOUT = path.resolve(import.meta.dirname, '..');

function printSummary(instance: Instance): void {
  const target = instance.checkout === OWN_CHECKOUT ? '' : ` -- ${instance.checkout}`;
  const lines = [
    '',
    'RStudio Server (dev) is running.',
    '',
    `  URL:        ${instance.url}`,
    `  checkout:   ${instance.checkout}`,
    `  rserver:    pid ${instance.rserverPid}  (log: ${logFile(instance.checkout, 'rserver.log')})`,
  ];

  if (instance.gwt === 'devmode') {
    lines.push(
      `  GWT devmode: pid ${instance.gwtPid}  code server http://localhost:${instance.codeServerPort}  (log: ${logFile(instance.checkout, 'gwt.log')})`,
      '',
      '  The first page load compiles the app on demand and can take a couple of minutes.',
      '  Java edits are picked up by reloading the browser.',
    );
  } else {
    lines.push(`  GWT:        ${instance.gwt} (no code server)`);
  }

  lines.push('', `  Stop with:  npm run rserver-stop${target}`, '');
  console.log(lines.join('\n'));
}

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
  const gwtMode = parseGwtMode(args.flags.get('gwt'));
  const wwwAddressFlag = args.flags.get('www-address');
  const wwwAddress = typeof wwwAddressFlag === 'string' ? canonicalizeAddress(wwwAddressFlag) : '127.0.0.1';
  const shouldBuild = args.flags.get('build') !== false;
  const shouldWait = args.flags.get('wait') !== false;
  const gwtTimeoutMs = (flagNumber(TAG, args, 'gwt-timeout') ?? 900) * 1000;

  step(TAG, `Checkout: ${checkout}`);

  // The dev server runs with --auth-none, so binding anything but loopback puts
  // an unauthenticated R session (i.e. arbitrary code execution as this user)
  // on the network. Allowed, since testing from a VM sometimes needs it, but
  // never silently.
  if (wwwAddress !== '127.0.0.1' && wwwAddress !== 'localhost' && wwwAddress !== '::1') {
    step(TAG, `warning: binding ${wwwAddress} exposes this passwordless dev server to the network.`);
  }

  // An instance is keyed by checkout, since a single src/gwt/www can only host
  // one devmode at a time. Recover from a previous run's leftovers either way.
  const existing = readInstance(checkout);
  if (existing) {
    const alive = pidMatches(existing.rserverPid, 'rserver');
    if (alive && args.flags.get('restart') !== true) {
      step(TAG, 'A dev server is already running for this checkout.');
      printSummary(existing);
      step(TAG, 'Pass --restart to rebuild and restart it.');
      return;
    }

    step(TAG, alive ? 'Stopping the running dev server first (--restart)...' : 'Clearing state from a dev server that is no longer running...');
    await stopInstance(TAG, existing);
  }

  if (shouldBuild) {
    runCmakeBuild(TAG, checkout);
  }

  const cppDir = cppBuildDir(checkout);
  const rserverBin = path.join(cppDir, 'server', 'rserver');
  const rserverConf = path.join(cppDir, 'conf', 'rserver-dev.conf');
  for (const required of [rserverBin, rserverConf]) {
    if (!fs.existsSync(required)) {
      fail(
        TAG,
        `${required} does not exist.\n` +
          '       The build directory did not produce RStudio Server. If it was configured for a\n' +
          '       desktop-only target, reconfigure it with the default:\n' +
          `           cmake -S ${checkout} -B ${path.join(checkout, 'build')} -DRSTUDIO_TARGET=Development`,
      );
    }
  }

  if (gwtMode !== 'none' && !which('ant')) {
    fail(TAG, 'ant was not found on PATH; install Apache Ant (or use --gwt=none) and retry.');
  }

  if (gwtMode === 'draft') {
    runAntDraft(TAG, checkout);
  } else if (gwtMode === 'none' && wwwIsSuperDevStub(checkout)) {
    step(
      TAG,
      'warning: src/gwt/www holds a Super Dev Mode bootstrap, which needs a code server. ' +
        'Without one the IDE will fail to start; use --gwt=devmode or --gwt=draft.',
    );
  }

  fs.mkdirSync(stateDir(checkout), { recursive: true });

  const portFlag = flagNumber(TAG, args, 'port');
  const port =
    portFlag !== undefined
      ? await requirePortFree(TAG, portFlag, wwwAddress, 'rserver')
      : await findFreePort(TAG, 8787, wwwAddress);

  const codeServerFlag = flagNumber(TAG, args, 'code-server-port');
  let codeServerPort: number | null = null;
  if (gwtMode === 'devmode') {
    codeServerPort =
      codeServerFlag !== undefined
        ? await requirePortFree(TAG, codeServerFlag, '127.0.0.1', 'code server')
        : await findFreePort(TAG, 9876, '127.0.0.1');
    requireCodeServerPortPlumbing(checkout, codeServerPort);
  }

  const dataDir = makeDataDir();
  const secureCookieKey = writeSecureCookieKey(checkout);
  const rserverLog = logFile(checkout, 'rserver.log');
  const gwtLog = logFile(checkout, 'gwt.log');

  // Spawned directly rather than through build/src/cpp/rserver-dev: that
  // wrapper announces a hardcoded port, deletes the shared /tmp/rstudio-server,
  // and on exit sends SIGUSR2 to every rsession on the machine -- all of which
  // would break a second instance (and any desktop RStudio) running alongside
  // this one. The flags below reproduce what the wrapper passes.
  const rserverArgs = [
    `--server-user=${currentUser()}`,
    '--auth-none=1',
    '--server-daemonize=0',
    `--www-address=${wwwAddress}`,
    `--www-port=${port}`,
    `--server-data-dir=${dataDir}`,
    `--secure-cookie-key-file=${secureCookieKey}`,
    `--config-file=${rserverConf}`,
  ];

  step(TAG, `Starting rserver on ${wwwAddress}:${port}...`);
  const rserverOut = fs.openSync(rserverLog, 'w');
  const rserver = spawn(rserverBin, rserverArgs, {
    // rserver-dev.conf resolves rsession, the pam helper and r-ldpath relative
    // to the build tree, so rserver has to run from build/src/cpp.
    cwd: cppDir,
    env: {
      ...process.env,
      RS_DB_MIGRATIONS_PATH: path.join(checkout, 'src', 'cpp', 'server', 'db'),
      RSTUDIO_PROJECT_ROOT: checkout,
    },
    stdio: ['ignore', rserverOut, rserverOut],
    detached: true,
  });
  fs.closeSync(rserverOut);

  if (rserver.pid === undefined) {
    fail(TAG, `could not spawn ${rserverBin}`);
  }

  let rserverExit: string | null = null;
  rserver.on('exit', (code, signal) => {
    rserverExit = `rserver exited before becoming ready (code=${code} signal=${signal}).\n${tailLog(rserverLog)}`;
  });

  let gwtPid: number | null = null;
  if (gwtMode === 'devmode') {
    step(TAG, `Starting GWT devmode (code server port ${codeServerPort})...`);
    const gwtOut = fs.openSync(gwtLog, 'w');
    const gwt = spawn(
      'ant',
      ['devmode', `-Dcodeserver.port=${codeServerPort}`, `-Dserver.port=${port}`],
      {
        cwd: path.join(checkout, 'src', 'gwt'),
        stdio: ['ignore', gwtOut, gwtOut],
        detached: true,
      },
    );
    fs.closeSync(gwtOut);

    if (gwt.pid === undefined) {
      fail(TAG, 'could not spawn ant devmode');
    }

    gwtPid = gwt.pid;
    gwt.unref();
  }

  const instance: Instance = {
    version: 1,
    checkout,
    url: `http://localhost:${port}`,
    wwwAddress,
    port,
    rserverPid: rserver.pid,
    gwt: gwtMode,
    codeServerPort,
    gwtPid,
    dataDir,
    secureCookieKey,
    startedAt: new Date().toISOString(),
  };

  // Recorded before the readiness waits below so that a start which times out
  // still leaves something for rserver-stop to clean up.
  writeInstance(instance);

  if (!shouldWait) {
    step(TAG, 'Spawned (--no-wait); not waiting for readiness.');
    printSummary(instance);
    rserver.unref();
    return;
  }

  try {
    await waitForHttp(`http://127.0.0.1:${port}/auth-sign-in`, {
      timeoutMs: 60_000,
      checkAbort: () => rserverExit,
    });
  } catch (e) {
    failAfterSpawn((e as Error).message, rserverLog, checkout);
  }
  step(TAG, 'rserver is accepting connections.');

  if (gwtMode === 'devmode' && gwtPid !== null) {
    step(TAG, 'Waiting for the GWT code server (first run compiles the Java sources)...');
    try {
      await waitForHttp(`http://127.0.0.1:${codeServerPort}/`, {
        timeoutMs: gwtTimeoutMs,
        onWaiting: (elapsed) => step(TAG, `  still compiling (${Math.round(elapsed / 1000)}s)...`),
        checkAbort: () =>
          pidMatches(gwtPid, 'devmode')
            ? null
            : `ant devmode exited before the code server came up.\n${tailLog(gwtLog)}`,
      });
    } catch (e) {
      failAfterSpawn((e as Error).message, gwtLog, checkout);
    }
    step(TAG, 'GWT code server is up.');
  }

  printSummary(instance);
  rserver.unref();
}

main().catch((e: unknown) => fail(TAG, (e as Error).message));
