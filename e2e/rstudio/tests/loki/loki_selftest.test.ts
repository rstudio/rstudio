/**
 * Agent Loki checking itself against a live RStudio.
 *
 * The first test here is the one that matters. RStudio ships a command whose
 * handler throws on purpose (`raiseException`, shown in the palette as "Raise
 * Exception"), which gives a crash that is guaranteed to happen and guaranteed
 * to be reachable through the interface. Agent Loki is pointed at it and then
 * held to its whole promise: one finding, marked verified, with numbered steps
 * that pass the lint, and -- separately, from a clean start -- those printed
 * steps and nothing else have to raise the same crash again.
 *
 * That is precisely the check the earlier version of this tool would have
 * failed. It found a real crash and then produced steps it had invented, and
 * nothing in the suite could tell the difference. This can.
 *
 * `raiseException` is blocked by the policy, so the fuzzing loop can never
 * reach it and no ordinary run will ever "find" it. These tests invoke it by
 * name instead.
 */

import { test, expect } from '@fixtures/rstudio.fixture';
import { drainClientExceptions } from '@utils/commands';
import { Executor, PALETTE_SEARCH } from '@utils/loki/executor';
import { auditPolicy, classifyCommand, COMMAND_FACTS } from '@utils/loki/policy';
import { cleanState, minimizeFinding, replayActions } from '@utils/loki/replay';
import { Collector, lintStep, type Machine } from '@utils/loki/report';
import { readScreen, readSwallowedFailure, settle, settleSettings } from '@utils/loki/settle';
import { dismissBlockingModals } from '@pages/modals.page';

const RAISE_EXCEPTION = 'raiseException';

test.describe('Agent Loki: checking itself @loki', () => {
  let restoreIgnoreExceptions: string | undefined;

  test.beforeAll(() => {
    // This suite raises exceptions on purpose, and the shared fixture fails any
    // test that raised one. Restored in afterAll so it never leaks.
    restoreIgnoreExceptions = process.env.PW_IGNORE_CLIENT_EXCEPTIONS;
    process.env.PW_IGNORE_CLIENT_EXCEPTIONS = '1';
  });

  test.afterAll(() => {
    if (restoreIgnoreExceptions === undefined)
      delete process.env.PW_IGNORE_CLIENT_EXCEPTIONS;
    else
      process.env.PW_IGNORE_CLIENT_EXCEPTIONS = restoreIgnoreExceptions;
  });

  test('a crash it finds comes with steps that raise the crash again on their own',
    async ({ rstudioPage: page }) => {
      test.setTimeout(300_000);

      const executor = new Executor(page, process.platform);
      const collector = new Collector();
      const paletteLabel = COMMAND_FACTS[RAISE_EXCEPTION].labels.palette;
      expect(paletteLabel, 'the palette shows this command by a readable name')
        .toBe('Raise Exception');

      await cleanState(page);

      // Act once, through the interface, exactly as the loop would.
      const { screen } = await readScreen(page);
      const performed = await executor.runCommandViaPalette(RAISE_EXCEPTION, paletteLabel);
      expect(performed.outcome, performed.detail ?? '').toBe('performed');
      await settle(page);

      const precondition = {
        dialogTitle: screen.dialogTitle,
        activeDoc: screen.activeDoc,
        activeTabs: screen.activeTabs,
        prefs: { native_file_dialogs: false },
      };

      // Both detectors, because which one fires depends on how the crash
      // escapes. raiseException throws synchronously from its handler, and
      // AppCommandPaletteItem.invoke catches that and shows a dialog, so the
      // crash never reaches the recorded exceptions. A crash that surfaces from a
      // later callback escapes the catch and does reach them. Agent Loki has to
      // see both, and this test would have passed on a tool that saw only one.
      const raised = await drainClientExceptions(page);
      for (const exception of raised) {
        collector.addFinding({
          kind: 'client-exception',
          message: exception.message,
          stack: exception.stack,
          step: 1,
          precondition,
        });
      }

      const swallowed = await readSwallowedFailure(page);
      if (swallowed !== null) {
        collector.addFinding({
          kind: 'command-execution-failed',
          message: swallowed.message,
          stack: '',
          step: 1,
          precondition,
        });
        await dismissBlockingModals(page).catch(() => []);
      }

      expect(
        raised.length + (swallowed === null ? 0 : 1),
        'the deliberate crash was detected, by one detector or the other',
      ).toBeGreaterThan(0);

      const findings = collector.allFindings();
      expect(findings.length, 'one crash, not one per occurrence').toBe(1);
      const finding = findings[0];

      // Shrink and verify, exactly as a real run does.
      const machines: Machine[] = [performed.machine];
      const result = await minimizeFinding({
        page,
        executor,
        machines,
        signature: finding.signature,
        deadline: Date.now() + 120_000,
        log: message => console.log(`[loki-selftest] ${message}`),
      });
      console.log(`[loki-selftest] hunting signature ${finding.signature} `
        + `(${finding.kind}) for message: ${finding.message}`);

      expect(result.status, 'the crash was reproduced from its own steps').toBe('verified');
      if (result.status !== 'verified')
        return;

      finding.status = result.status;
      finding.steps = result.steps;

      // The steps have to be something a person can follow, and they have to
      // name the route and the label that were actually used.
      expect(finding.steps.length).toBeGreaterThan(0);
      for (const step of finding.steps)
        expect(() => lintStep(step.do), step.do).not.toThrow();

      const joined = finding.steps.map(s => s.do).join(' ');
      expect(joined).toContain('Command Palette');
      expect(joined).toContain('Raise Exception');

      // The decisive part: from a clean start, perform ONLY what the report
      // prints, and require the same crash. If the printed steps were not
      // sufficient on their own, this is where that shows up.
      await cleanState(page);
      const fromPrintedSteps = await replayActions(
        page,
        executor,
        finding.steps.map(step => step.machine),
      );
      expect(
        fromPrintedSteps.signatures,
        'the printed steps are sufficient by themselves',
      ).toContain(finding.signature);

      await cleanState(page);
    });

  test('an exception with no action behind it becomes a lead and never gets steps',
    async ({ rstudioPage: page }) => {
      const collector = new Collector();
      await cleanState(page);

      // window.rstudio.errors.simulate raises a real uncaught exception from a
      // scheduled context. Nothing was performed, so nothing can be written down.
      const nonce = `loki-selftest-${Date.now()}`;
      await page.evaluate(msg => window.rstudio?.errors?.simulate(msg), nonce);
      await page.evaluate(msg => window.rstudio?.errors?.simulate(msg), nonce);
      await settle(page);

      const raised = await drainClientExceptions(page);
      const ours = raised.filter(e => e.message.includes(nonce));
      expect(ours.length, 'both simulated exceptions were recorded').toBe(2);

      for (const exception of ours) {
        collector.addLead({
          kind: 'client-exception',
          message: exception.message,
          stack: exception.stack,
          duringRecovery: false,
        });
      }

      const leads = collector.allLeads();
      expect(leads.length, 'the same message twice is one lead').toBe(1);
      expect(leads[0].count).toBe(2);
      expect(leads[0].note).toContain('no reproduction steps exist');

      // Nothing reached the findings list, so nothing can acquire steps later.
      expect(collector.allFindings()).toEqual([]);

      await cleanState(page);
    });

  test('it paces itself, and records what was on screen before each action',
    async ({ rstudioPage: page }) => {
      test.setTimeout(120_000);
      const executor = new Executor(page, process.platform);
      const { paceMs } = settleSettings();

      await cleanState(page);

      const timestamps: number[] = [];
      for (const commandId of ['activateConsole', 'activateFiles', 'activateEnvironment']) {
        expect(classifyCommand(commandId).allowed, `${commandId} is fuzzable`).toBe(true);
        const { screen } = await readScreen(page);
        // Steps are meaningless without the state they acted on.
        expect(screen.url).toBeTruthy();
        expect(Array.isArray(screen.activeTabs)).toBe(true);

        await executor.closePalette();
        timestamps.push(Date.now());
        await executor.runCommandViaPalette(commandId, COMMAND_FACTS[commandId].labels.palette);
        await settle(page);
      }

      for (let i = 1; i < timestamps.length; i++) {
        expect(
          timestamps[i] - timestamps[i - 1],
          'consecutive actions are at least a pace apart',
        ).toBeGreaterThanOrEqual(paceMs);
      }

      await cleanState(page);
    });

  test('the palette route clicks the command it searched for, never a neighbour',
    async ({ rstudioPage: page }) => {
      // The highlighted row in the palette can be a preference toggle or an R
      // addin rather than the command that was typed, so the executor addresses
      // the row by id and never presses Enter. This checks the row it clicked.
      const executor = new Executor(page, process.platform);
      await cleanState(page);

      const performed = await executor.runCommandViaPalette(
        'activateFiles',
        COMMAND_FACTS.activateFiles.labels.palette,
      );
      expect(performed.outcome).toBe('performed');
      expect(performed.machine.commandId).toBe('activateFiles');
      // The recorded route is a palette command, so a replay repeats a palette
      // command, not a keystroke into whatever happened to be focused.
      expect(performed.machine.route).toBe('palette');

      await settle(page);
      // The palette closes behind an invoked command; a lingering one would
      // intercept the next action's clicks.
      await expect(page.locator(PALETTE_SEARCH)).toBeHidden();

      await cleanState(page);
    });

  test('every command the running build offers is classified',
    async ({ rstudioPage: page }) => {
      // Meaningful only when the build under test matches this checkout. When it
      // does not, the extra commands are unknown here and get blocked, which is
      // safe; the report records the count, the build version, and the checksum
      // of the Commands.cmd.xml the facts came from.
      const liveIds = await page.evaluate(() => window.rstudio?.commands?.list ?? []);
      expect(liveIds.length, 'the bridge reported its command list').toBeGreaterThan(0);

      const audit = auditPolicy(liveIds);
      expect(audit.allowed + audit.blockedByHazard + audit.blockedByRule
        + audit.unclassified.length).toBe(liveIds.length);

      if (audit.unclassified.length > 0) {
        console.warn(`[loki] ${audit.unclassified.length} command(s) in this build are unknown `
          + 'to the generated facts and were blocked. Regenerate with `npm run loki:facts` from a '
          + `checkout matching the build. First few: ${audit.unclassified.slice(0, 5).join(', ')}`);
      }
      // Every live command has a verdict either way, which is the property that
      // keeps an unknown command from being fuzzed by accident.
      for (const id of audit.unclassified)
        expect(classifyCommand(id).allowed).toBe(false);
    });

  test('a deliberate crash generator is never offered to the loop',
    async ({ rstudioPage: page }) => {
      // raiseException is visible and enabled, so it really is in the live
      // candidate pool the loop reads. The policy is the only thing keeping it
      // out, and without that every run would "find" the tool's own bait.
      const live = await page.evaluate((id) => {
        const command = window.rstudio?.commands?.[id];
        if (typeof command !== 'function')
          return null;
        return { enabled: command.isEnabled(), visible: command.isVisible() };
      }, RAISE_EXCEPTION);

      expect(live, 'raiseException exists in this build').not.toBeNull();
      expect(live?.visible, 'raiseException is a real palette entry').toBe(true);
      expect(classifyCommand(RAISE_EXCEPTION).allowed).toBe(false);
    });
});
