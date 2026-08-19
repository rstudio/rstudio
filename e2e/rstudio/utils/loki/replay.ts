/**
 * Shrinking a run down to a recipe, and then proving the recipe works.
 *
 * A crash found at step 96 of a 150-step run is not a bug report. Nobody will
 * follow 96 instructions, and most of them had nothing to do with the crash. So
 * every crash worth reporting goes through two stages here:
 *
 *   1. **Minimise.** Find the shortest tail of the run that still raises the
 *      same crash, then try dropping individual actions from it.
 *   2. **Verify.** From a clean start, perform exactly the actions that are
 *      about to be printed, and check the same crash happens again.
 *
 * Only after the second stage does a finding become `verified` and get numbered
 * steps. This ordering is the point: the steps a reader follows are the steps
 * that were replayed, so they are proven rather than asserted. Anything that
 * fails to reproduce says so and prints no steps at all.
 *
 * Every replayed action goes through the same executor the live loop uses, so a
 * replay is not an approximation of the run: it is the run's actions, performed
 * the same way, through the same interface.
 */

import type { Page } from '@playwright/test';
import { documentCloseAllNoSave, drainClientExceptions, resetLayoutZoom } from '../commands';
import { resetForNextTest } from '../test-reset';
import { Executor } from './executor';
import { COMMAND_FACTS } from './policy';
import { signatureFor, type Machine, type Step } from './report';
import { readSwallowedFailure, settle } from './settle';
import { dismissBlockingModals } from '../../pages/modals.page';

/** Budget, so one stubborn crash cannot consume the whole run's time. */
export function minimizeSettings() {
  const findings = Number(process.env.PW_LOKI_MAX_MINIMIZE ?? 3);
  const minutes = Number(process.env.PW_LOKI_MINIMIZE_MINUTES ?? 10);
  if (!Number.isFinite(findings) || findings < 0)
    throw new Error(`PW_LOKI_MAX_MINIMIZE="${process.env.PW_LOKI_MAX_MINIMIZE}" -- expected a number`);
  if (!Number.isFinite(minutes) || minutes <= 0)
    throw new Error(`PW_LOKI_MINIMIZE_MINUTES="${process.env.PW_LOKI_MINIMIZE_MINUTES}" -- expected a positive number`);
  return { findings, minutes };
}

/** Tail lengths tried, shortest first. */
const TAIL_LENGTHS = [1, 2, 4, 8, 16];

export type ReplayOutcome = {
  /** The performed sentences, in order, as this replay produced them. */
  steps: Step[];
  /** Signatures of every crash the replay raised. */
  signatures: string[];
};

/**
 * Return RStudio to the state a test starts in.
 *
 * This is the suite's own definition of a clean start (`resetForNextTest`) plus
 * closing every buffer and ending any pane zoom. It is not a fresh process: a
 * relaunch would cost 30 to 60 seconds for each candidate and minimisation tries
 * a dozen of them. The weaker isolation is a real limitation, and it is recorded
 * in the report rather than glossed over: a crash that depends on something a
 * reset does not clear will fail to reproduce here and be reported as a lead.
 */
export async function cleanState(page: Page): Promise<void> {
  await documentCloseAllNoSave(page).catch(() => {});
  await resetLayoutZoom(page).catch(() => {});
  await resetForNextTest(page);
  // Discard anything the reset itself stirred up, so the next replay's
  // exceptions are the replay's own.
  await drainClientExceptions(page).catch(() => []);
}

/**
 * Perform a list of recorded actions and collect what they raise.
 *
 * Each action is settled and drained exactly as in the live loop, so a crash
 * seen here is attributed to the same action it was attributed to originally.
 */
export async function replayActions(
  page: Page,
  executor: Executor,
  machines: readonly Machine[],
): Promise<ReplayOutcome> {
  const steps: Step[] = [];
  const signatures: string[] = [];

  for (const machine of machines) {
    await executor.closePalette();

    let sentence: string;
    switch (machine.route) {
      case 'palette': {
        const commandId = machine.commandId;
        if (commandId === undefined)
          continue;
        const searchText = machine.typed ?? COMMAND_FACTS[commandId]?.labels.palette ?? '';
        const performed = await executor.runCommandViaPalette(commandId, searchText);
        sentence = performed.do;
        // An action that could not be performed on replay cannot be part of a
        // recipe, so the replay stops describing itself as reproducing anything.
        if (performed.outcome !== 'performed')
          return { steps, signatures };
        break;
      }
      default:
        // Routes beyond the palette are not yet replayable; a recipe that needs
        // one cannot be verified, and saying so is the correct outcome.
        return { steps, signatures };
    }

    await settle(page);

    const raised = await drainClientExceptions(page);
    for (const exception of raised)
      signatures.push(signatureFor('client-exception', exception.message, exception.stack));

    // The Command Palette catches a handler that throws and shows a dialog
    // instead, so this class of crash has to be read off the screen. Checked on
    // replay as well as live, or a crash found one way could never be verified.
    const swallowed = await readSwallowedFailure(page);
    if (swallowed !== null) {
      signatures.push(signatureFor('command-execution-failed', swallowed.message, ''));
      // Clear it so the next action is not blocked behind a glass panel. This is
      // recovery, so it is not recorded as a step.
      await dismissBlockingModals(page).catch(() => []);
    }

    steps.push({ n: steps.length + 1, do: sentence, machine });
  }

  return { steps, signatures };
}

export type MinimizeResult =
  | { status: 'verified'; steps: Step[] }
  | { status: 'reproduced-full-log'; steps: Step[] }
  | { status: 'not-reproduced' };

/**
 * Reduce a run to the shortest action list that still raises `signature`, then
 * confirm that list on its own.
 *
 * `machines` is every action performed before and including the crash. The
 * search runs from the end backwards, because a crash is almost always caused
 * by something recent, and a short tail is both faster to test and better to
 * read.
 */
export async function minimizeFinding(args: {
  page: Page;
  executor: Executor;
  machines: readonly Machine[];
  signature: string;
  /** Wall-clock budget for this one finding. */
  deadline: number;
  log?: (message: string) => void;
}): Promise<MinimizeResult> {
  const { page, executor, machines, signature, deadline } = args;
  const log = args.log ?? (() => {});

  if (machines.length === 0)
    return { status: 'not-reproduced' };

  const reproduces = async (candidate: readonly Machine[]): Promise<boolean> => {
    await cleanState(page);
    const outcome = await replayActions(page, executor, candidate);
    const hit = outcome.signatures.includes(signature);
    // Say what the replay actually raised. Without this a candidate that missed
    // is indistinguishable from one that raised a different crash, and the whole
    // finding lands as unreproducible with no way to tell why.
    if (!hit) {
      log(`  ${candidate.length} action(s) replayed ${outcome.steps.length}, raised `
        + `${outcome.signatures.length === 0 ? 'nothing' : outcome.signatures.join(', ')}`);
    }
    return hit;
  };

  const outOfTime = () => Date.now() >= deadline;

  // 1. Tail search: the shortest recent window that still crashes.
  let window: Machine[] | undefined;
  for (const length of TAIL_LENGTHS) {
    if (outOfTime()) {
      log(`ran out of time during the tail search for ${signature}`);
      break;
    }
    if (length > machines.length)
      break;
    const candidate = machines.slice(-length);
    log(`trying the last ${length} action(s) for ${signature}`);
    if (await reproduces(candidate)) {
      window = candidate;
      break;
    }
  }

  // 2. Linear trim inside the window: drop one action at a time, keeping a drop
  //    when the crash survives it.
  if (window !== undefined) {
    let trimmed = window;
    for (let index = 0; index < trimmed.length && !outOfTime(); index++) {
      if (trimmed.length === 1)
        break;
      const attempt = trimmed.slice(0, index).concat(trimmed.slice(index + 1));
      if (await reproduces(attempt)) {
        trimmed = attempt;
        index--;
      }
    }

    // 3. Verification: perform exactly the list that will be printed, from a
    //    clean start, and require the same crash. This is what earns the word
    //    "verified", and it recaptures the sentences that get printed.
    await cleanState(page);
      const proof = await replayActions(page, executor, trimmed);
    if (proof.signatures.includes(signature)) {
      log(`verified ${signature} in ${proof.steps.length} step(s)`);
      return { status: 'verified', steps: proof.steps };
    }
    log(`the trimmed list for ${signature} did not hold up on its own`);
  }

  // The fallback: does the whole run reproduce it at all? If so the actions are
  // context, clearly labelled as unverified. If not, it is a lead.
  if (outOfTime())
    return { status: 'not-reproduced' };

  log(`replaying the whole run for ${signature}`);
  await cleanState(page);
  const full = await replayActions(page, executor, machines);
  if (full.signatures.includes(signature))
    return { status: 'reproduced-full-log', steps: full.steps };

  return { status: 'not-reproduced' };
}
