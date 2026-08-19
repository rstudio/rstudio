/**
 * Agent Loki: drive RStudio through random user actions and report what breaks.
 *
 * Excluded from ordinary runs. Set PW_RUN_LOKI=1 to include it, or use
 * `npm run test:loki` / `npm run test:loki-server`.
 *
 * What it reports and what it refuses to report is documented in docs/loki.md.
 * The short version: a crash is only written up with numbered steps once those
 * steps have been replayed from a clean start and seen to raise the same crash
 * again. Anything less says so and carries no steps.
 */

import { writeFile } from 'fs/promises';
import type { Page } from '@playwright/test';
import { test, expect } from '@fixtures/rstudio.fixture';
import { drainClientExceptions, getVersion } from '@utils/commands';
import { useSuiteSandbox } from '@utils/sandbox';
import { Executor, humanPaletteShortcut } from '@utils/loki/executor';
import { Prng, resolveSeed } from '@utils/loki/prng';
import { auditPolicy, classifyCommand, COMMAND_FACTS, COMMANDS_XML_MD5 } from '@utils/loki/policy';
import {
  Collector,
  lintStep,
  REPORT_VERSION,
  renderFindings,
  type ActionRecord,
  type EndReason,
  type LokiReport,
  type Machine,
  type Precondition,
} from '@utils/loki/report';
import { Recovery } from '@utils/loki/recovery';
import { minimizeFinding, minimizeSettings } from '@utils/loki/replay';
import { InterfaceWatch, readScreen, readSwallowedFailure, settle } from '@utils/loki/settle';
import { dismissBlockingModals } from '@pages/modals.page';

/**
 * Preferences this suite sets that a default install does not have, so every
 * finding can state them and a reader can put their own RStudio in the same
 * state. Values mirror fixtures/base-prefs.jsonc; only the ones that could
 * plausibly change whether a crash happens are listed.
 */
const DIVERGENT_PREFS: Record<string, boolean | number | string> = {
  native_file_dialogs: false,
  reduced_motion: true,
};

function budget() {
  const steps = Number(process.env.PW_LOKI_STEPS ?? 150);
  const minutes = Number(process.env.PW_LOKI_MINUTES ?? 15);
  if (!Number.isFinite(steps) || steps < 1)
    throw new Error(`PW_LOKI_STEPS="${process.env.PW_LOKI_STEPS}" -- expected a positive number`);
  if (!Number.isFinite(minutes) || minutes < 1)
    throw new Error(`PW_LOKI_MINUTES="${process.env.PW_LOKI_MINUTES}" -- expected a positive number`);
  return { steps, minutes };
}

test.describe('Agent Loki @loki', () => {
  useSuiteSandbox();

  let restoreIgnoreExceptions: string | undefined;

  test.beforeAll(() => {
    // The shared fixture fails any test that raised an uncaught client
    // exception, which is exactly what this suite goes looking for. The loop
    // drains and attributes exceptions itself, so by teardown there is normally
    // nothing left; this covers anything raised after the final drain. Restored
    // in afterAll so the setting never leaks into another spec.
    restoreIgnoreExceptions = process.env.PW_IGNORE_CLIENT_EXCEPTIONS;
    process.env.PW_IGNORE_CLIENT_EXCEPTIONS = '1';
  });

  test.afterAll(() => {
    if (restoreIgnoreExceptions === undefined)
      delete process.env.PW_IGNORE_CLIENT_EXCEPTIONS;
    else
      process.env.PW_IGNORE_CLIENT_EXCEPTIONS = restoreIgnoreExceptions;
  });

  test('drives RStudio through random user actions and reports what crashes',
    async ({ rstudioPage: page }, testInfo) => {
      const { steps: stepBudget, minutes } = budget();
      // The run owns its own clock, and needs room to write the report after it.
      test.setTimeout(minutes * 60_000 + 120_000);

      const seed = resolveSeed();
      const prng = new Prng(seed);
      const executor = new Executor(page, process.platform);
      const collector = new Collector();
      const watch = InterfaceWatch.from(page);
      const recovery = new Recovery(page, executor);

      const actions: ActionRecord[] = [];
      const invoked = new Set<string>();
      const everOffered = new Set<string>();
      const satelliteUrls: string[] = [];
      // Actions that actually happened, in order, so a crash can be replayed
      // from the run that produced it. Skipped actions are not in here: they
      // changed nothing, so replaying them would only pad the recipe.
      const performedMachines: { step: number; machine: Machine }[] = [];

      // Satellite and popup windows are out of scope to fuzz, but they steal
      // focus from the page being driven, so note them and close them.
      page.context().on('page', async (opened) => {
        satelliteUrls.push(opened.url());
        await opened.close().catch(() => {});
      });

      const version = await getVersion(page);
      const startedAt = Date.now();
      const deadline = startedAt + minutes * 60_000;

      let endReason: EndReason = 'steps';
      let endDetail: string | undefined;
      // A lint failure while rendering is a bug in Agent Loki and must fail the
      // test -- but throwing it from the finally below would replace whatever
      // actually went wrong with it, and skip the rest of the diagnostics. Held
      // here and raised after the report is safely attached.
      let renderError: unknown;
      let stepsExecuted = 0;
      let emptyCandidateRuns = 0;

      console.log(`[loki] seed ${seed}, budget ${stepBudget} actions / ${minutes} minutes, `
        + `RStudio ${version.rstudio}, R ${version.r}`);

      try {
        for (let step = 1; step <= stepBudget; step++) {
          if (Date.now() >= deadline) {
            endReason = 'time';
            endDetail = `after ${step - 1} actions`;
            break;
          }

          // 1. Observe. The screen state is read before acting, because a step
          //    without the state it acted on cannot be followed.
          const { fingerprint, screen } = await readScreen(page);

          const dead = watch.check(fingerprint);
          if (dead !== undefined) {
            endReason = 'lost-ui';
            endDetail = `${dead}; the last action was step ${step - 1}`;
            break;
          }

          // 2. Choose. The bridge reads which commands are live; it does not run
          //    them. Filtering is the one thing it is allowed to decide.
          const offered = await liveCommandIds(page);
          for (const id of offered)
            everOffered.add(id);
          const candidates = offered.filter(id => classifyCommand(id).allowed);

          if (recovery.isWedged()) {
            endReason = 'wedged';
            endDetail = `${recovery.failures} actions in a row did nothing, and recovery `
              + 'did not bring the interface back';
            break;
          }

          if (candidates.length === 0) {
            emptyCandidateRuns++;
            if (emptyCandidateRuns >= 3) {
              endReason = 'wedged';
              endDetail = 'three consecutive observations offered nothing that could be done';
              break;
            }
            continue;
          }
          emptyCandidateRuns = 0;

          const commandId = prng.pick(candidates)!;
          const fact = COMMAND_FACTS[commandId];

          // 3. Act, through the interface, never the bridge. Leave no palette
          //    open from a previous step: recovery, so it is not a step itself.
          await executor.closePalette();

          const at = Date.now() - startedAt;
          // Every executor path returns an outcome rather than throwing, but a
          // thrown action must not be able to end the run: that is how an earlier
          // version came to report a dead session as a completed budget.
          let performed;
          try {
            performed = await executor.runCommandViaPalette(commandId, fact.labels.palette);
          } catch (err) {
            performed = {
              do: `Press ${humanPaletteShortcut(process.platform)} to open the Command Palette `
                + `and type '${fact.labels.palette}'`,
              machine: { route: 'palette' as const, commandId, typed: fact.labels.palette },
              outcome: 'timed-out' as const,
              detail: `the action did not complete: `
                + `${err instanceof Error ? err.message.split('\n')[0] : String(err)}`,
            };
            await executor.closePalette().catch(() => {});
            await dismissBlockingModals(page).catch(() => []);
          }
          stepsExecuted = step;

          // 4. Settle, so the exceptions drained next belong to this action and
          //    not to a handler still running from the last one.
          const settled = await settle(page);

          actions.push({
            step,
            at,
            do: performed.do,
            machine: performed.machine,
            before: screen,
            outcome: performed.outcome,
            detail: performed.detail,
            settled: settled.settled,
          });

          if (performed.outcome === 'performed') {
            invoked.add(commandId);
            performedMachines.push({ step, machine: performed.machine });
            recovery.succeeded();
          } else {
            // An action that did not happen is usually a dialog holding the
            // interface. Escalate rather than skipping the rest of the budget.
            const attempt = await recovery.failed();
            if (attempt !== null) {
              console.log(`[loki] after ${recovery.failures} actions that did nothing, `
                + `${attempt.rung}`);
              for (const exception of attempt.raised) {
                collector.addLead({
                  kind: 'client-exception',
                  message: exception.message,
                  stack: exception.stack,
                  duringRecovery: true,
                });
              }
            }
          }

          // 5. Attribute. Anything recorded now is this action's.
          const precondition: Precondition = {
            dialogTitle: screen.dialogTitle,
            activeDoc: screen.activeDoc,
            activeTabs: screen.activeTabs,
            prefs: DIVERGENT_PREFS,
          };

          const raised = await drainClientExceptions(page);
          for (const exception of raised) {
            if (performed.outcome === 'performed') {
              collector.addFinding({
                kind: 'client-exception',
                message: exception.message,
                stack: exception.stack,
                step,
                precondition,
              });
            } else {
              // The action was not performed, so there is no step to write down.
              collector.addLead({
                kind: 'client-exception',
                message: exception.message,
                stack: exception.stack,
                duringRecovery: false,
              });
            }
          }

          // Second detector, and not a redundant one. The Command Palette wraps
          // command_.execute() in a try/catch and shows a dialog, so a handler
          // that throws synchronously never reaches the recorded exceptions
          // above. Without this the palette route would silently miss that whole
          // class of crash.
          const swallowed = await readSwallowedFailure(page);
          if (swallowed !== null) {
            if (performed.outcome === 'performed') {
              collector.addFinding({
                kind: 'command-execution-failed',
                message: swallowed.message,
                stack: '',
                step,
                precondition,
              });
            } else {
              collector.addLead({
                kind: 'command-execution-failed',
                message: swallowed.message,
                stack: '',
                duringRecovery: false,
              });
            }
            // Recovery, so it is not a step: an error dialog left up throws a
            // glass panel over everything the next action would click.
            await dismissBlockingModals(page).catch(() => []);
          }
        }
      } catch (err) {
        // Whatever went wrong, the report must not claim the run finished its
        // budget. Naming the failure is the whole point.
        endReason = 'tool-error';
        endDetail = `${err instanceof Error ? err.message.split('\n')[0] : String(err)} `
          + `(after ${stepsExecuted} actions)`;
        console.warn(`[loki] the run stopped early: ${endDetail}`);
      }

      try {
        // Turn crashes into recipes. Worst first, budgeted, and skipped
        // entirely when the session is gone: a replay against a dead page would
        // report every crash as unreproducible for the wrong reason.
        if (endReason !== 'lost-ui' && endReason !== 'tool-error') {
          const { findings: maxFindings, minutes: perFinding } = minimizeSettings();
          const worstFirst = collector.allFindings().slice(0, maxFindings);

          for (const finding of worstFirst) {
            const prefix = performedMachines
              .filter(entry => entry.step <= finding.firstStep)
              .map(entry => entry.machine);

            console.log(`[loki] minimising ${finding.signature} `
              + `from ${prefix.length} action(s)`);
            try {
              const result = await minimizeFinding({
                page,
                executor,
                machines: prefix,
                signature: finding.signature,
                deadline: Date.now() + perFinding * 60_000,
                log: message => console.log(`[loki]   ${message}`),
              });
              finding.status = result.status;
              // Only a verified finding carries steps. The renderer prints
              // steps for nothing else, and keeping them here for a finding
              // that was not proven is how a reader ends up following a recipe
              // nobody checked.
              finding.steps = result.status === 'verified' ? result.steps : [];

              if (result.status === 'verified') {
                const lastCommand = [...result.steps]
                  .reverse()
                  .find(step => step.machine.commandId !== undefined)
                  ?.machine.commandId;
                const fact = lastCommand ? COMMAND_FACTS[lastCommand] : undefined;
                if (fact !== undefined) {
                  finding.alsoReachableVia = {
                    menuPath: fact.menuPath,
                    shortcut: fact.shortcuts[0]?.value,
                  };
                }
              }
            } catch (err) {
              console.warn(`[loki] minimising ${finding.signature} failed: `
                + `${err instanceof Error ? err.message : String(err)}`);
              finding.status = 'not-reproduced';
              finding.steps = [];
            }
          }
        }
      } finally {
        // The report is the deliverable, so it is written whatever happened,
        // including a thrown action or an exhausted timeout.
        // Audit the build's whole command list, not the subset enabled at this
        // instant. liveCommandIds filters to enabled and visible, which is right
        // for choosing what to do next and wrong here: it would report "372
        // allowed" for a build with 670 commands and read as though the policy
        // had blocked the rest.
        const allIds = await allCommandIds(page).catch(() => [] as string[]);
        const policy = auditPolicy(allIds.length > 0 ? allIds : Object.keys(COMMAND_FACTS));

        const notFuzzed = Object.keys(COMMAND_FACTS)
          .filter(id => classifyCommand(id).allowed && !everOffered.has(id))
          .sort();

        const report: LokiReport = {
          version: REPORT_VERSION,
          run: {
            seed,
            mode: testInfo.project.name,
            platform: process.platform,
            rstudioVersion: version.rstudio,
            rVersion: version.r,
            stepsExecuted,
            budget: { steps: stepBudget, minutes },
            endReason,
            endDetail,
            factsCommandsXmlMd5: COMMANDS_XML_MD5,
          },
          policy,
          coverage: { commandsInvoked: invoked.size, notFuzzed },
          findings: collector.allFindings(),
          leads: collector.allLeads(),
          artifacts: { actionLog: 'loki-actions.jsonl' },
        };

        // Written to the test's output directory and attached from there, rather
        // than attached as inline bodies. Attaching a body puts the file only
        // inside the HTML report bundle; triage wants to open loki-findings.md
        // and paste from it, so it has to exist as a file.
        const writeArtifact = async (name: string, body: string, contentType: string) => {
          const filePath = testInfo.outputPath(name);
          await writeFile(filePath, body, 'utf8');
          await testInfo.attach(name, { path: filePath, contentType });
        };

        await writeArtifact(
          'loki-report.json', JSON.stringify(report, null, 2), 'application/json');
        await writeArtifact(
          'loki-actions.jsonl',
          actions.map(a => JSON.stringify(a)).join('\n'),
          'application/x-ndjson',
        );
        try {
          await writeArtifact('loki-findings.md', renderFindings(report), 'text/markdown');
        } catch (err) {
          renderError = err;
          // Attach the failure in place of the document, so the reason is in the
          // report rather than only in the test output.
          await writeArtifact(
            'loki-findings-render-error.txt',
            err instanceof Error ? `${err.message}\n\n${err.stack ?? ''}` : String(err),
            'text/plain',
          );
        }

        console.log(`[loki] report written to ${testInfo.outputPath('loki-findings.md')}`);
        console.log(`[loki] ${stepsExecuted} actions, ended: ${endReason}`
          + `${endDetail ? ` (${endDetail})` : ''}`);
        console.log(`[loki] ${report.findings.length} crash(es), ${report.leads.length} lead(s), `
          + `${invoked.size} distinct commands invoked`);
        if (satelliteUrls.length > 0)
          console.log(`[loki] closed ${satelliteUrls.length} popup window(s)`);
      }

      // The run itself is the deliverable, so findings do not fail it. What does
      // fail it is the tool failing to describe its own behaviour: every action
      // has to carry a sentence a person could follow, and the report is
      // worthless without that. renderFindings has already linted the document.
      expect(actions.length, 'the run performed at least one action').toBeGreaterThan(0);
      for (const action of actions) {
        expect(action.do, `step ${action.step} has a sentence a reader can follow`)
          .not.toBe('');
      }

      // Rendering the report is where the promise is enforced, so a failure there
      // is a failure of the tool. Raised now that every artifact has landed.
      if (renderError !== undefined)
        throw renderError;

      // Every action has to carry a sentence that survives the lint, not merely
      // a non-empty one. The renderer only lints the steps it prints, so a
      // sentence that never made it into a verified finding would otherwise go
      // unchecked -- and this is the run's exit condition, so it checks all of
      // them.
      for (const action of actions) {
        expect(() => lintStep(action.do), `step ${action.step}: ${action.do}`).not.toThrow();
      }

      // Two ways of ending are faults in the run rather than findings, and both
      // used to be reported as a completed budget. A lost session means every
      // later step acted on nothing; a tool error means Agent Loki broke.
      expect(endReason, 'the session survived the run').not.toBe('lost-ui');
      expect(endReason, `Agent Loki failed: ${endDetail ?? ''}`).not.toBe('tool-error');
    });
});

/**
 * Every command id the running build has, enabled or not. The report's policy
 * section audits this, so its counts describe the build rather than one moment.
 */
async function allCommandIds(page: Page): Promise<string[]> {
  return page.evaluate(() => window.rstudio?.commands?.list ?? []).catch(() => []);
}

/**
 * Command ids the live session currently offers a person: present, visible, and
 * enabled. One page evaluation rather than 670 bridge calls, so the answer
 * describes a single moment.
 *
 * AppCommand.isEnabled() is `enabled_ && isVisible()`, so an invisible command
 * always reads disabled. That suits this filter exactly: a command a person
 * cannot see is not one the Command Palette will offer.
 */
async function liveCommandIds(page: Page): Promise<string[]> {
  return page.evaluate(() => {
    const bridge = window.rstudio;
    if (!bridge?.commands)
      return [];
    const out: string[] = [];
    for (const id of bridge.commands.list) {
      const command = bridge.commands[id];
      if (typeof command !== 'function')
        continue;
      try {
        if (command.isEnabled() && command.isVisible())
          out.push(id);
      } catch {
        // A command whose state cannot be read is not a candidate.
      }
    }
    return out;
  }).catch(() => []);
}
