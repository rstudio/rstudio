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

/** Default wait for a dead child's streams to deliver their last reads. */
export const TAIL_SETTLE_TIMEOUT_MS = 500;

export interface OutputTail {
  /** The captured tail as of now. */
  text(): string;
  /**
   * Resolve once both streams have ended, or after `timeoutMs`.
   *
   * Node emits `'exit'` before the stdio streams necessarily deliver their
   * final reads, so reading the tail straight off an exit can miss the
   * trailing output -- which is exactly the part that says why the child
   * died. Bounded because a stream that errors may never end, and resolves
   * at once for a child that is still running (its streams stay open) or one
   * whose streams already ended.
   */
  settled(timeoutMs?: number): Promise<void>;
}

/**
 * Drain a spawned child's stdout/stderr into a bounded tail.
 *
 * Draining matters on its own: nothing reads these pipes otherwise, and a
 * child blocks once a pipe buffer fills (~64KB on Linux), so a chatty
 * startup could hang rather than fail. Only the tail is kept, since a
 * launch that dies says so at the end.
 */
export function captureOutputTail(
  proc: ChildProcess,
  limit: number = OUTPUT_TAIL_LIMIT,
): OutputTail {
  let tail = '';
  const append = (chunk: Buffer | string): void => {
    tail = (tail + chunk.toString()).slice(-limit);
  };

  const streams = [proc.stdout, proc.stderr].filter((s): s is NonNullable<typeof s> => !!s);
  let pending = streams.length;
  let onDrained: (() => void) | undefined;
  for (const stream of streams) {
    stream.on('data', append);
    stream.once('end', () => {
      if (--pending === 0) onDrained?.();
    });
  }

  return {
    text: () => tail,
    settled: (timeoutMs: number = TAIL_SETTLE_TIMEOUT_MS) =>
      new Promise<void>((resolve) => {
        const running = proc.exitCode === null && proc.signalCode === null;
        if (pending === 0 || running) {
          resolve();
          return;
        }
        const timer = setTimeout(resolve, timeoutMs);
        onDrained = () => {
          clearTimeout(timer);
          resolve();
        };
      }),
  };
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
