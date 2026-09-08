import type { ChildProcess } from 'child_process';

/**
 * Diagnostics for an RStudio Desktop launch that never became reachable over
 * CDP.
 *
 * A bare `ECONNREFUSED` after the startup timeout cannot tell an Electron
 * that died from one that is alive but never bound the port. That ambiguity
 * is why the repeated launch failures in rstudio#18522 stayed unexplained:
 * the fixture spawned a process, waited out the timeout, and reported
 * nothing about what the process actually did.
 */

/** Cap on retained child output -- enough for a stack or a fatal error. */
export const OUTPUT_TAIL_LIMIT = 4000;

/**
 * Drain a spawned child's stdout/stderr into a bounded tail and return a
 * getter for it.
 *
 * Draining matters on its own: nothing reads these pipes otherwise, and a
 * child blocks once a pipe buffer fills (~64KB on Linux), so a chatty
 * startup could hang rather than fail. Only the tail is kept, since a
 * launch that dies says so at the end.
 */
export function captureOutputTail(
  proc: ChildProcess,
  limit: number = OUTPUT_TAIL_LIMIT,
): () => string {
  let tail = '';
  const append = (chunk: Buffer | string): void => {
    tail = (tail + chunk.toString()).slice(-limit);
  };
  proc.stdout?.on('data', append);
  proc.stderr?.on('data', append);
  return () => tail;
}

/**
 * Describe how the child and the CDP port looked when the launch gave up.
 *
 * `outputTail` is `undefined` when output was never captured (dev mode
 * inherits the streams). That reads differently from a child that ran and
 * wrote nothing, which is itself a signal worth seeing.
 */
export function describeLaunchState(
  proc: Pick<ChildProcess, 'pid' | 'exitCode' | 'signalCode'>,
  outputTail: string | undefined,
  portHolders: string,
): string {
  const parts: string[] = [];

  if (proc.exitCode !== null) {
    parts.push(`process exited with code ${proc.exitCode}`);
  } else if (proc.signalCode !== null) {
    parts.push(`process was killed by ${proc.signalCode}`);
  } else {
    parts.push(`process (PID ${proc.pid ?? 'unknown'}) was still running`);
  }

  parts.push(
    portHolders
      ? `CDP port LISTEN owner(s): ${portHolders}`
      : 'nothing was listening on the CDP port',
  );

  const state = `[launch-state] ${parts.join('; ')}`;
  if (outputTail === undefined) {
    return `${state}; child output not captured (streams inherited)`;
  }

  const trimmed = outputTail.trim();
  return trimmed
    ? `${state}\n[launch-output] last ${trimmed.length} char(s) of stdout/stderr:\n${trimmed}`
    : `${state}; the child wrote nothing to stdout/stderr`;
}
