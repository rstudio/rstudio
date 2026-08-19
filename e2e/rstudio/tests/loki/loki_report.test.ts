/**
 * Checks on the parts of Agent Loki that need no running RStudio: the step
 * lint, crash signatures, the policy, and the renderer.
 *
 * These guard the promise the whole project rests on, so they are worth reading
 * as a statement of it: a reported crash comes with numbered steps a person can
 * follow, and when that is impossible the tool says so and prints nothing.
 *
 * Imports `test` from Playwright directly rather than from the RStudio fixture.
 * The fixture's automatic per-test reset depends on a live session, so using it
 * here would launch and drive RStudio to check a regular expression.
 */

import { test, expect } from '@playwright/test';
import {
  auditPolicy,
  blockedElementIds,
  classifyCommand,
  COMMAND_FACTS,
  shortcutIsBlocked,
} from '@utils/loki/policy';
import {
  lintStep,
  normalizeFrames,
  renderFindings,
  signatureFor,
  StepLintError,
  FULL_LOG_NOTE,
  NOT_REPRODUCED_NOTE,
  LEAD_NOTE,
  REPORT_VERSION,
  type Finding,
  type LokiReport,
} from '@utils/loki/report';
import { idSafeString, paletteEntryId, toolbarButtonId } from '@utils/loki/element-ids';
import { InterfaceWatch, parseSwallowedFailure, type Fingerprint } from '@utils/loki/settle';
import { Prng } from '@utils/loki/prng';

function reportWith(findings: Finding[], leads: LokiReport['leads'] = []): LokiReport {
  return {
    version: REPORT_VERSION,
    run: {
      seed: 1, mode: 'desktop', platform: 'darwin',
      rstudioVersion: '2026.02.0', rVersion: '4.5.1',
      stepsExecuted: 100, budget: { steps: 100, minutes: 15 },
      endReason: 'steps', factsCommandsXmlMd5: 'abc123',
    },
    policy: { allowed: 535, blockedByHazard: 48, blockedByRule: 87, unclassified: [] },
    coverage: { commandsInvoked: 12, notFuzzed: [] },
    findings,
    leads,
    artifacts: { actionLog: 'loki-actions.jsonl' },
  };
}

function findingWith(overrides: Partial<Finding>): Finding {
  return {
    signature: 'ab12cd34ef56',
    status: 'not-reproduced',
    kind: 'client-exception',
    message: "TypeError: Cannot read properties of null (reading 'substr')",
    stack: 'at Unknown.pKi(rstudio-0.js)',
    count: 1,
    firstStep: 41,
    precondition: {
      dialogTitle: null,
      activeDoc: 'Untitled1',
      activeTabs: ['Console'],
      prefs: { native_file_dialogs: false },
    },
    steps: [],
    ...overrides,
  };
}

test.describe('Agent Loki: what counts as a reproduction @loki', () => {
  // One row per way of failing to be a reproduction. Each of these has been
  // offered, by a person or by a tool, as though it told a reader what to do.
  const NOT_A_REPRODUCTION: { why: string; sentence: string }[] = [
    { why: 'a test file name', sentence: 'Run tests/loki/loki.test.ts to see the crash' },
    { why: 'a command line', sentence: 'Run npx playwright test tests/loki --grep @loki' },
    { why: 'a seed', sentence: 'Re-run with PW_LOKI_SEED=382495775' },
    { why: 'an artifact reference', sentence: 'Replay loki-actions.jsonl from the report' },
    { why: 'an internal command id', sentence: 'Dispatch presentation2PresentFromBeginning' },
    { why: 'a selector', sentence: 'Click #rstudio_tb_newrpresentationdoc' },
    { why: 'a Playwright call', sentence: "Call page.keyboard.press('Escape')" },
    { why: 'a fixture reference', sentence: 'Start from the rstudio fixture state' },
  ];

  for (const { why, sentence } of NOT_A_REPRODUCTION) {
    test(`rejects ${why}`, () => {
      expect(() => lintStep(sentence)).toThrow(StepLintError);
    });
  }

  test('rejects narration that a reader cannot act on', () => {
    // The failure that caused this project: a story about what the tool did,
    // written after the fact, in place of instructions.
    expect(() => lintStep('The tool then invoked the presentation command'))
      .toThrow(/opens with "The"/);
    expect(() => lintStep('A dialog was showing at this point')).toThrow(StepLintError);
  });

  test('accepts steps a person can follow, and requires an actionable verb', () => {
    const good = [
      "Press Cmd+Shift+P to open the Command Palette, type 'Create a new R presentation', "
        + "and click 'Create a new R presentation'",
      'Press Escape to close the Save File dialog',
      "Click 'OK' in the 'New R Markdown' dialog",
      'Type plot(1:10) into the Console and press Enter',
      'Choose Yes in the confirmation dialog',
      'Open the Files pane',
      'Close the Untitled1 tab',
      'Wait for the Plots pane to finish drawing',
      'Select the first row of the data viewer',
    ];
    for (const sentence of good)
      expect(() => lintStep(sentence), sentence).not.toThrow();
  });

  test('leaves alone command ids that are also ordinary words', () => {
    // reindent and redo are real command ids. Flagging them would make honest
    // sentences unprintable and push the tool toward silence over truth, so only
    // ids no sentence could contain by accident are rejected.
    expect(COMMAND_FACTS.reindent).toBeDefined();
    expect(() => lintStep('Press Cmd+I to reindent the selection')).not.toThrow();
    expect(() => lintStep('Press Cmd+Shift+Z to redo the change')).not.toThrow();
  });

  test('rejects an empty step', () => {
    expect(() => lintStep('   ')).toThrow(StepLintError);
  });
});

test.describe('Agent Loki: crash signatures @loki', () => {
  const OBFUSCATED_A = [
    'TypeError: Cannot read properties of null',
    '    at Unknown.pKi(rstudio-0.js)',
    '    at Unknown.qLm(rstudio-0.js:1:2)',
  ].join('\n');

  const OBFUSCATED_B = [
    'TypeError: Cannot read properties of null',
    '    at Unknown.zZz(rstudio-0.js)',
    '    at Unknown.wWw(rstudio-0.js:9:9)',
  ].join('\n');

  const DRAFT = [
    'TypeError: Cannot read properties of null',
    '    at org.rstudio.studio.client.workbench.Presentation.present(Presentation.java:41)',
    '    at org.rstudio.core.client.command.AppCommand.execute(AppCommand.java:120)',
  ].join('\n');

  test('discards per-build placeholder frames entirely', () => {
    // "Unknown" reads as a readable identifier, which is why an earlier version
    // kept these frames and produced a new signature on every rebuild.
    expect(normalizeFrames(OBFUSCATED_A)).toEqual([]);
  });

  test('keeps real Java frames from a draft build', () => {
    const frames = normalizeFrames(DRAFT);
    expect(frames.length).toBeGreaterThan(0);
    expect(frames[0]).toContain('Presentation');
    expect(frames[0]).toContain('present');
  });

  test('two obfuscated builds of the same crash share a signature', () => {
    const a = signatureFor('client-exception', 'TypeError: Cannot read properties of null', OBFUSCATED_A);
    const b = signatureFor('client-exception', 'TypeError: Cannot read properties of null', OBFUSCATED_B);
    expect(a).toBe(b);
  });

  test('a draft build keeps more detail than an obfuscated one', () => {
    const obfuscated = signatureFor('client-exception', 'TypeError: x', OBFUSCATED_A);
    const draft = signatureFor('client-exception', 'TypeError: x', DRAFT);
    expect(draft).not.toBe(obfuscated);
  });

  test('run-specific values in a message do not split a signature', () => {
    const a = signatureFor('client-exception', 'Element gwt-uid-417 not found', '');
    const b = signatureFor('client-exception', 'Element gwt-uid-902 not found', '');
    expect(a).toBe(b);
  });
});

test.describe('Agent Loki: the policy @loki', () => {
  // Blocked for a behavioural reason read out of the handler or the command's
  // own description, not because of the way its name reads.
  const MUST_BLOCK: [string, string][] = [
    ['presentation2PresentFromBeginning', 'opens an external browser'],
    ['presentation2Present', 'opens an external browser'],
    ['browseCheatSheets', 'opens an external browser'],
    ['openRMarkdownCheatSheet', 'opens an external browser, and says nothing about it'],
    ['openShinyCheatSheet', 'opens an external browser, and says nothing about it'],
    ['helpUsingRStudio', 'opens an external browser'],
    ['rstudioSupport', 'opens an external browser'],
    ['showLogFiles', 'opens a folder in the file manager'],
    ['showFolder', 'opens a folder in the file manager'],
    ['loadServerHome', 'navigates the page away'],
    ['signOut', 'navigates the page away'],
    ['printSourceDoc', 'can raise the operating-system print sheet'],
    ['printHelp', 'can raise the operating-system print sheet'],
    ['raiseException', 'exists in order to throw'],
    ['crashDesktopApplication', 'crashes the application'],
    ['quitSession', 'ends the session'],
    ['restartR', 'restarts the session'],
    ['openProject', 'restarts the session'],
    ['projectMru0', 'restarts the session'],
    ['installPackage', 'reaches a package repository'],
    ['rsconnectDeploy', 'publishes off the machine'],
    ['vcsPush', 'writes to a remote branch'],
    ['deleteFiles', 'destroys files'],
    ['clearUserPrefs', 'wipes the preferences this suite sets'],
    ['showCommandPalette', 'is the instrument the run drives'],
    ['sendToTerminal', 'runs editor text as a shell command'],
    ['checkPackage', 'starts a toolchain that takes minutes'],
  ];

  for (const [commandId, why] of MUST_BLOCK) {
    test(`blocks ${commandId} because it ${why}`, () => {
      expect(COMMAND_FACTS[commandId], `${commandId} is a real command`).toBeDefined();
      expect(classifyCommand(commandId).allowed).toBe(false);
    });
  }

  // The other half, and the more important one. Over-blocking is invisible: the
  // run simply never finds anything in the commands it wrongly avoided.
  const MUST_ALLOW: [string, string][] = [
    // fixtures/base-prefs.jsonc sets native_file_dialogs false, so every file
    // dialog in this suite is a web dialog in the page. These are a target, not
    // a trap, and an earlier version blocked the whole family by name.
    ['newRPresentationDoc', 'its Save dialog is a web dialog here'],
    ['openSourceDoc', 'its Open dialog is a web dialog here'],
    ['saveSourceDocAs', 'its Save dialog is a web dialog here'],
    ['importDatasetFromCsv', 'its wizard is a web dialog here'],
    ['exportFiles', 'its dialog is a web dialog here'],
    ['savePlotAsImage', 'its dialog is a web dialog here'],
    // openMinimalWindow is an Electron window on Desktop and a popup in the same
    // browser context on Server, never the operating system's browser.
    ['viewerZoom', 'it opens an RStudio window, not a browser'],
    ['showGpuDiagnostics', 'it opens an RStudio window, not a browser'],
    ['showA11yDiagnostics', 'it opens an RStudio window, not a browser'],
    ['openDeveloperConsole', 'it opens an RStudio window, not a browser'],
    // Ordinary work.
    ['newSourceDoc', 'it is ordinary editing'],
    ['activateConsole', 'it is ordinary navigation'],
    ['showOptions', 'its dialog is a rich target'],
    ['interruptR', 'interrupting R is recoverable'],
    ['vcsDiff', 'it only reads version-control state'],
  ];

  for (const [commandId, why] of MUST_ALLOW) {
    test(`allows ${commandId} because ${why}`, () => {
      const result = classifyCommand(commandId);
      expect(COMMAND_FACTS[commandId], `${commandId} is a real command`).toBeDefined();
      expect(result.allowed, result.allowed ? '' : JSON.stringify(result)).toBe(true);
    });
  }

  test('blocks a command this checkout has never heard of', () => {
    const result = classifyCommand('someCommandFromANewerBuild');
    expect(result.allowed).toBe(false);
    if (!result.allowed)
      expect(result.reason.kind).toBe('unclassified');
  });

  test('the whole command list classifies, and most of it is fuzzable', () => {
    const audit = auditPolicy(Object.keys(COMMAND_FACTS));
    expect(audit.unclassified).toEqual([]);
    expect(audit.allowed).toBeGreaterThan(400);
    expect(audit.blockedByHazard).toBeGreaterThan(30);
    expect(audit.blockedByRule).toBeGreaterThan(50);
    expect(audit.allowed + audit.blockedByHazard + audit.blockedByRule)
      .toBe(Object.keys(COMMAND_FACTS).length);
  });

  test('a blocked command is blocked through its toolbar button too', () => {
    const blocked = blockedElementIds();
    expect(blocked).toContain(toolbarButtonId('printSourceDoc'));
    expect(blocked).not.toContain(toolbarButtonId('newSourceDoc'));
  });

  test('shortcuts that belong to the browser or a blocked command are off limits', () => {
    expect(shortcutIsBlocked('Meta+Q')).toBe(true);
    expect(shortcutIsBlocked('F5')).toBe(true);
    // printSourceDoc is blocked, so whatever it is bound to is blocked with it.
    const printShortcut = COMMAND_FACTS.printSourceDoc.shortcuts[0]?.value;
    if (printShortcut !== undefined)
      expect(shortcutIsBlocked(printShortcut)).toBe(true);
    expect(shortcutIsBlocked('Ctrl+Alt+I')).toBe(false);
  });
});

test.describe('Agent Loki: the generated command facts @loki', () => {
  test('covers every command in Commands.cmd.xml', () => {
    // 670 definitions, of which 40 are invisible. A naive search for '<cmd id="'
    // reports 668, because goToHelp and goToDefinition are written 'id ="..."'.
    expect(Object.keys(COMMAND_FACTS).length).toBe(670);
    expect(COMMAND_FACTS.goToHelp).toBeDefined();
    expect(COMMAND_FACTS.goToDefinition).toBeDefined();
    expect(Object.values(COMMAND_FACTS).filter(f => !f.visible).length).toBe(40);
  });

  test('resolves palette labels the way the palette does', () => {
    // AppCommandPaletteItem takes label, then buttonLabel, then desc, then
    // menuLabel. newRPresentationDoc genuinely reads as its description rather
    // than as "R Presentation", and that is what a person sees.
    expect(COMMAND_FACTS.newRPresentationDoc.labels.palette)
      .toBe('Create a new R presentation');
    // raiseException has only a menuLabel, with a mnemonic marker in it.
    expect(COMMAND_FACTS.raiseException.labels.palette).toBe('Raise Exception');
    // buttonLabel wins when there is no label.
    expect(COMMAND_FACTS.presentation2Present.labels.palette).toBe('Present');
  });

  test('strips mnemonic markers from menu labels only', () => {
    expect(COMMAND_FACTS.raiseException.labels.menuLabel).toBe('Raise Exception');
    // devtoolsLoadAll carries a real underscore. Treating it as a mnemonic would
    // turn its palette text into "loadall" and the run would never find it.
    expect(COMMAND_FACTS.devtoolsLoadAll.labels.palette).toContain('load_all');
  });

  test('records menu paths and shortcuts as context', () => {
    expect(COMMAND_FACTS.newSourceDoc.menuPath).toContain('File');
    expect(COMMAND_FACTS.showCommandPalette.shortcuts.map(s => s.value))
      .toContain('Cmd+Shift+P');
  });
});

test.describe('Agent Loki: element ids @loki', () => {
  test('builds palette and toolbar ids the way ElementIds.java does', () => {
    expect(paletteEntryId('newSourceDoc')).toBe('rstudio_command_entry_command_newsourcedoc');
    expect(toolbarButtonId('newSourceDoc')).toBe('rstudio_tb_newsourcedoc');
    // C++ collapses to C without the substitution, colliding with plain "C".
    expect(idSafeString('New C++ File')).toBe('new_cpp_file');
    expect(idSafeString('  Save As...  ')).toBe('save_as');
  });
});

test.describe('Agent Loki: the rendered report @loki', () => {
  test('an unreproduced crash is labelled, not dressed up', () => {
    const markdown = renderFindings(reportWith([findingWith({ status: 'not-reproduced' })]));
    expect(markdown).toContain(NOT_REPRODUCED_NOTE);
    expect(markdown).toContain('## Leads, not independently verified');
    // No numbered steps anywhere: the absence is the honest output.
    expect(markdown).not.toMatch(/^\d+\.\s/m);
  });

  test('a crash that needs the whole run carries its caveat and no steps', () => {
    const markdown = renderFindings(reportWith([findingWith({ status: 'reproduced-full-log' })]));
    expect(markdown).toContain(FULL_LOG_NOTE);
    expect(markdown).not.toMatch(/^\d+\.\s/m);
  });

  test('a verified crash prints numbered steps and the settings it depended on', () => {
    const finding = findingWith({
      status: 'verified',
      steps: [
        {
          n: 1,
          do: "Press Cmd+Shift+P to open the Command Palette, type 'Create a new R "
            + "presentation', and click 'Create a new R presentation'",
          machine: { route: 'palette', commandId: 'newRPresentationDoc' },
        },
        {
          n: 2,
          do: 'Press Escape to close the Save File dialog',
          machine: { route: 'shortcut', keys: 'Escape' },
        },
      ],
      alsoReachableVia: { menuPath: 'File > New File > R Presentation' },
    });
    const markdown = renderFindings(reportWith([finding]));

    expect(markdown).toContain('## Reproducible crashes');
    expect(markdown).toMatch(/^1\. Press Cmd\+Shift\+P/m);
    expect(markdown).toMatch(/^2\. Press Escape/m);
    // A reader with a default install has to be told what this run changed.
    expect(markdown).toContain("uncheck 'Use native file and message dialog boxes'");
    expect(markdown).toContain('File > New File > R Presentation');
  });

  test('rendering refuses a step that is not a reproduction', () => {
    const finding = findingWith({
      status: 'verified',
      steps: [{
        n: 1,
        do: 'Run npx playwright test tests/loki to see it happen',
        machine: { route: 'palette', commandId: 'newSourceDoc' },
      }],
    });
    // A lint failure here is a bug in the tool, which is why it throws rather
    // than printing a warning next to the bad step.
    expect(() => renderFindings(reportWith([finding]))).toThrow(StepLintError);
  });

  test('an exception with no action behind it renders as a lead with no steps', () => {
    const markdown = renderFindings(reportWith([], [{
      signature: 'ff00ff00',
      kind: 'client-exception',
      message: 'TypeError: something happened between steps',
      stack: '',
      count: 2,
      duringRecovery: true,
      note: LEAD_NOTE,
    }]));
    expect(markdown).toContain('## Exceptions with no attributable action');
    expect(markdown).toContain(LEAD_NOTE);
    expect(markdown).toContain('during recovery');
    expect(markdown).not.toMatch(/^\d+\.\s/m);
  });

  test('a clean run says so', () => {
    expect(renderFindings(reportWith([]))).toContain('No crashes were found.');
  });
});

test.describe('Agent Loki: crashes the Command Palette swallows @loki', () => {
  // Verbatim from a real 2026.08 build. The trailing accessibility warning is the
  // whole reason this parser exists: an earlier version used the dialog's entire
  // text as the crash message, that line was not always present, and so the same
  // crash hashed to two different signatures. Every finding then came out
  // unreproducible, because the replay's signature never matched the live one.
  const REAL_DIALOG = [
    'Command Execution Failed',
    "The command 'Raise Exception' could not be executed.",
    '',
    'Exception caught: foo',
    'OK',
    'Warning: screen reader mode not enabled. Turn on using shortcut Ctrl+Shift+U.',
  ].join('\n');

  test('reads the command and the exception out of the dialog', () => {
    const failure = parseSwallowedFailure('Command Execution Failed', REAL_DIALOG);
    expect(failure.commandLabel).toBe('Raise Exception');
    expect(failure.detail).toBe('Exception caught: foo');
    expect(failure.message)
      .toBe("The command 'Raise Exception' could not be executed: Exception caught: foo");
  });

  test('the same crash keeps one signature when incidental text differs', () => {
    const withWarning = parseSwallowedFailure('Command Execution Failed', REAL_DIALOG);
    const withoutWarning = parseSwallowedFailure(
      'Command Execution Failed',
      REAL_DIALOG.split('\n').filter(line => !line.startsWith('Warning:')).join('\n'),
    );
    expect(withoutWarning.message).toBe(withWarning.message);
    expect(signatureFor('command-execution-failed', withoutWarning.message, ''))
      .toBe(signatureFor('command-execution-failed', withWarning.message, ''));
  });

  test('two different commands failing are two different crashes', () => {
    const a = parseSwallowedFailure('Command Execution Failed',
      "Command Execution Failed\nThe command 'Knit Document' could not be executed.\n\nboom\nOK");
    const b = parseSwallowedFailure('Command Execution Failed',
      "Command Execution Failed\nThe command 'Run Chunk' could not be executed.\n\nboom\nOK");
    expect(signatureFor('command-execution-failed', a.message, ''))
      .not.toBe(signatureFor('command-execution-failed', b.message, ''));
  });

  test('a wording change is reported rather than dropped', () => {
    // If PaletteConstants is reworded, the caption may still match while the body
    // does not. Losing a real crash to that would be worse than an ugly message.
    const failure = parseSwallowedFailure(
      'Command Execution Failed',
      'Command Execution Failed\nSomething else entirely happened.\nOK',
    );
    expect(failure.commandLabel).toBe('');
    expect(failure.message).toContain('Something else entirely happened');
  });

  test('reads the French wording too', () => {
    const failure = parseSwallowedFailure(
      "Échec de l'exécution de la commande",
      "Échec de l'exécution de la commande\n"
        + "La commande 'Lever une exception' n'a pas pu être exécutée.\n\nboum\nOK",
    );
    expect(failure.commandLabel).toBe('Lever une exception');
    expect(failure.detail).toBe('boum');
  });
});

test.describe('Agent Loki: noticing the session has gone @loki', () => {
  const SESSION = 'http://localhost:8787';

  function fingerprint(overrides: Partial<Fingerprint> = {}): Fingerprint {
    return {
      url: `${SESSION}/`,
      bridgePresent: true,
      ready: true,
      numDialogs: 0,
      topDialogLabel: null,
      activeDocId: 'doc1',
      consoleBusy: false,
      ...overrides,
    };
  }

  test('a healthy session reads as fine', () => {
    const watch = new InterfaceWatch(SESSION);
    expect(watch.check(fingerprint())).toBeUndefined();
  });

  test('a page that left the session origin ends the run at once', () => {
    // The failure this exists for: an earlier version dispatched a command that
    // replaced the page, then logged "no candidates" thirty-four times against a
    // dead session and reported the run as having finished normally.
    const watch = new InterfaceWatch(SESSION);
    const reason = watch.check(fingerprint({ url: 'about:blank', bridgePresent: false }));
    expect(reason).toBeDefined();
    expect(reason).toContain('about:blank');
  });

  test('a briefly absent bridge is tolerated, because a restart clears it', () => {
    const watch = new InterfaceWatch(SESSION);
    // First observation only starts the clock.
    expect(watch.check(fingerprint({ bridgePresent: false }))).toBeUndefined();
    expect(watch.check(fingerprint({ bridgePresent: false }))).toBeUndefined();
    // And a bridge that comes back resets it.
    expect(watch.check(fingerprint())).toBeUndefined();
    expect(watch.check(fingerprint({ bridgePresent: false }))).toBeUndefined();
  });
});

test.describe('Agent Loki: the random source @loki', () => {
  test('the same seed makes the same choices', () => {
    const items = ['a', 'b', 'c', 'd', 'e'];
    const first = Array.from({ length: 20 }, () => new Prng(7)).map(p => p.pick(items));
    expect(new Set(first).size).toBe(1);

    const a = new Prng(99);
    const b = new Prng(99);
    expect(Array.from({ length: 50 }, () => a.int(1000)))
      .toEqual(Array.from({ length: 50 }, () => b.int(1000)));
  });

  test('picking from nothing returns nothing rather than throwing', () => {
    expect(new Prng(1).pick([])).toBeUndefined();
    expect(new Prng(1).pickWeighted([])).toBeUndefined();
  });

  test('a zero weight is never chosen', () => {
    const prng = new Prng(3);
    const picks = Array.from({ length: 200 }, () => prng.pickWeighted([[0, 'never'], [1, 'always']]));
    expect(new Set(picks)).toEqual(new Set(['always']));
  });
});
