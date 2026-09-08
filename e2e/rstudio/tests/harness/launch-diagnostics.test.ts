import { test, expect } from '@playwright/test';
import { spawn, type ChildProcess } from 'child_process';
import { PassThrough } from 'stream';
import { captureOutputTail, describeLaunchState, OUTPUT_TAIL_LIMIT } from '@fixtures/launch-diagnostics';

/**
 * Harness self-test for the launch diagnostics in fixtures/desktop.fixture.ts
 * (rstudio#18522).
 *
 * When RStudio stops becoming reachable over CDP, the fixture used to report
 * only `ECONNREFUSED` after the startup timeout -- which cannot distinguish an
 * Electron that died from one that is alive but never bound the port. These
 * tests pin the three states apart, and pin that child output survives to be
 * reported, using real child processes rather than the RStudio binary.
 *
 * Deliberately imports plain `@playwright/test`: nothing here needs an IDE, so
 * the suite must not pay a launch for it.
 */

const NODE = process.execPath;
// Long enough to outlive the assertions; every test kills its own child.
const LINGER = ['-e', 'setTimeout(() => {}, 60000)'];

function waitForExit(proc: ChildProcess): Promise<void> {
  return new Promise((resolve) => proc.once('exit', () => resolve()));
}

test.describe('launch diagnostics', { tag: ['@desktop_only'] }, () => {
  test('reports a live child as still running, not as a death', async () => {
    const proc = spawn(NODE, LINGER, { stdio: 'pipe' });
    try {
      const state = describeLaunchState(proc, '', '');
      expect(state).toContain(`PID ${proc.pid}`);
      expect(state).toContain('still running');
      expect(state).not.toContain('exited with code');
      expect(state).not.toContain('killed by');
    } finally {
      proc.kill('SIGKILL');
      await waitForExit(proc);
    }
  });

  test('reports the exit code when the child exits non-zero', async () => {
    const proc = spawn(NODE, ['-e', 'process.exit(3)'], { stdio: 'pipe' });
    await waitForExit(proc);

    const state = describeLaunchState(proc, '', '');
    expect(state).toContain('process exited with code 3');
    expect(state).not.toContain('still running');
  });

  test('names the signal when the child is killed', async () => {
    test.skip(
      process.platform === 'win32',
      'Windows emulates signals: a killed child reports an exit code, not a signal name',
    );

    const proc = spawn(NODE, LINGER, { stdio: 'pipe' });
    proc.kill('SIGKILL');
    await waitForExit(proc);

    // The distinction this whole module exists for: an externally killed
    // Electron is reported as such instead of as a bare connect refusal.
    const state = describeLaunchState(proc, '', '');
    expect(state).toContain('killed by SIGKILL');
    expect(state).not.toContain('still running');
  });

  test('surfaces the CDP port owner only when the port is held', async () => {
    const proc = spawn(NODE, LINGER, { stdio: 'pipe' });
    try {
      expect(describeLaunchState(proc, '', '')).toContain('nothing was listening on the CDP port');
      expect(describeLaunchState(proc, '', '4321,8765')).toContain(
        'CDP port LISTEN owner(s): 4321,8765',
      );
    } finally {
      proc.kill('SIGKILL');
      await waitForExit(proc);
    }
  });

  test('captures the tail of child stderr and bounds it', async () => {
    // Write past the cap so the bound is actually exercised, and end with a
    // marker so we can prove it kept the *tail* rather than the head.
    const overflow = OUTPUT_TAIL_LIMIT * 2;
    const proc = spawn(
      NODE,
      ['-e', `process.stderr.write('x'.repeat(${overflow}) + 'FATAL-AT-THE-END')`],
      { stdio: 'pipe' },
    );
    const tail = captureOutputTail(proc);
    await waitForExit(proc);
    await tail.settled();

    const captured = tail.text();
    expect(captured).toHaveLength(OUTPUT_TAIL_LIMIT);
    expect(captured).toContain('FATAL-AT-THE-END');

    const state = describeLaunchState(proc, captured, '');
    expect(state).toContain('FATAL-AT-THE-END');
  });

  test('settled() waits for data that arrives after the child has exited', async () => {
    // The real race is not reproducible on demand: a spawned child that writes
    // and exits had all output delivered before 'exit' at 12B, 8KB, 200KB and
    // 2MB. Drive it deterministically instead, with streams we control and an
    // already-set exitCode standing in for a child that has gone.
    const stdout = new PassThrough();
    const stderr = new PassThrough();
    const exited = { stdout, stderr, exitCode: 1, signalCode: null };

    const tail = captureOutputTail(exited);
    let done = false;
    const settling = tail.settled(5000).then(() => {
      done = true;
    });

    // Nothing has ended yet, so settled() must still be waiting -- this is the
    // assertion that fails if it resolves eagerly off the exit code.
    await new Promise((resolve) => setImmediate(resolve));
    expect(done).toBe(false);
    expect(tail.text()).toBe('');

    stderr.write('FATAL-AFTER-EXIT');
    stderr.end();
    stdout.end();
    await settling;

    expect(done).toBe(true);
    expect(tail.text()).toContain('FATAL-AFTER-EXIT');
    expect(describeLaunchState(exited, tail.text(), '')).toContain('FATAL-AFTER-EXIT');
  });

  test('settled() does not stall the failure path for a live child', async () => {
    // A running child's streams stay open, so waiting on them would burn the
    // whole bounded timeout on every CDP-timeout failure. settled() has to
    // short-circuit instead.
    const proc = spawn(NODE, LINGER, { stdio: 'pipe' });
    const tail = captureOutputTail(proc);
    try {
      const started = Date.now();
      await tail.settled(5000);
      expect(Date.now() - started).toBeLessThan(1000);
    } finally {
      proc.kill('SIGKILL');
      await waitForExit(proc);
    }
  });

  test('distinguishes uncaptured output from a child that wrote nothing', async () => {
    const proc = spawn(NODE, LINGER, { stdio: 'pipe' });
    try {
      // Dev mode inherits the streams, so there is no tail to read -- that
      // must not read as "the child said nothing", which is a real signal.
      expect(describeLaunchState(proc, undefined, '')).toContain('output not captured');
      expect(describeLaunchState(proc, '', '')).toContain('wrote nothing');
    } finally {
      proc.kill('SIGKILL');
      await waitForExit(proc);
    }
  });
});
