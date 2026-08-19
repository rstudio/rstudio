/**
 * The report, and the rule it exists to enforce.
 *
 * Every crash Agent Loki reports comes with a numbered list of things to do in
 * RStudio, in English, that someone with no checkout of this repository can
 * follow to see the crash for themselves. Not a test file name. Not a command
 * line. Not "re-run with this seed". Not a log to replay. If a list of that kind
 * cannot be produced for a crash, no steps are printed at all and the crash is
 * filed as a lead.
 *
 * That rule is enforced here rather than trusted, by `lintStep` below. Anything
 * that reads as a file name, a command line, an environment setting, a
 * Playwright call, an internal command id, or a selector fails the check, and a
 * failure is a bug in the tool rather than a note for the reader. A step also
 * has to begin with something a person can act on, because "the tool then
 * invoked..." is narration, not an instruction.
 *
 * The reason for the strictness: an earlier version of this tool found a real
 * crash, was asked for reproduction steps, had nothing but nine command ids,
 * and produced a nine-step story that contradicted itself. Absence of steps is
 * an acceptable answer. Something that merely looks like steps is not.
 */

import { createHash } from 'crypto';
import { COMMAND_FACTS } from './command-facts';
import type { ScreenState } from './settle';

export const REPORT_VERSION = 1;

// ---------------------------------------------------------------------------
// Shapes
// ---------------------------------------------------------------------------

export type Route = 'palette' | 'click' | 'type' | 'shortcut' | 'dialog';

/** The executable half of an action: what a replay re-performs. */
export type Machine = {
  route: Route;
  commandId?: string;
  elementId?: string;
  typed?: string;
  keys?: string;
};

/**
 * One performed action, in both registers. `do` is what a person reads;
 * `machine` is what a replay executes. They are two fields of a single
 * performed action, written at the moment it happened, so they cannot describe
 * different things.
 */
export type Step = {
  n: number;
  do: string;
  machine: Machine;
};

export type Outcome = 'performed' | 'skipped' | 'timed-out';

/** A line in the action log: a step plus everything observed around it. */
export type ActionRecord = {
  step: number;
  /** Milliseconds since the run began. */
  at: number;
  do: string;
  machine: Machine;
  /** What was on screen before the action. Steps are meaningless without it. */
  before: ScreenState;
  outcome: Outcome;
  detail?: string;
  settled: boolean;
};

export type FindingKind =
  /** An uncaught exception recorded by the automation agent. */
  | 'client-exception'
  /**
   * A command handler that threw synchronously, which the Command Palette
   * caught and turned into its "Command Execution Failed" dialog. Watching only
   * the recorded exceptions misses these entirely: see readSwallowedFailure in
   * utils/loki/settle.ts for why.
   */
  | 'command-execution-failed'
  /** R stopped answering. */
  | 'session-death';

export type FindingStatus = 'verified' | 'reproduced-full-log' | 'not-reproduced';

export type Precondition = {
  dialogTitle: string | null;
  activeDoc: string | null;
  activeTabs: string[];
  /** Preferences this run differs from a default install on. */
  prefs: Record<string, boolean | number | string>;
};

export type Finding = {
  signature: string;
  status: FindingStatus;
  kind: FindingKind;
  message: string;
  stack: string;
  count: number;
  firstStep: number;
  precondition: Precondition;
  /** Empty for anything other than `verified`. Absence is the honest output. */
  steps: Step[];
  /** Context for a reader, never a performed step. */
  alsoReachableVia?: { menuPath?: string; shortcut?: string };
  artifacts?: { screenshot?: string };
};

/**
 * An exception with no action to attribute it to. Leads never carry steps: an
 * action that was not performed cannot be written down as one.
 */
export type Lead = {
  signature: string;
  kind: FindingKind;
  message: string;
  stack: string;
  count: number;
  duringRecovery: boolean;
  note: string;
};

export const LEAD_NOTE =
  'No user action is associated with this exception, so no reproduction steps exist.';

export const NOT_REPRODUCED_NOTE =
  'The tool observed this crash once but could not reproduce it from the recorded '
  + 'actions. Treat as a lead, not a bug report.';

export const FULL_LOG_NOTE =
  'This crash reproduces only by replaying the whole run, so the actions below are '
  + 'context rather than a verified recipe. They have not been shown to be sufficient '
  + 'on their own.';

export type EndReason =
  | 'steps' | 'time' | 'wedged' | 'lost-ui' | 'session-death' | 'replay-complete'
  /**
   * Agent Loki itself failed. Named rather than folded into 'steps', because an
   * earlier version reported a run as having finished its budget when in fact it
   * had died on step 17 and spent the rest of the run acting on nothing.
   */
  | 'tool-error';

export type LokiReport = {
  version: number;
  run: {
    seed: number;
    mode: string;
    platform: string;
    rstudioVersion: string;
    rVersion: string;
    stepsExecuted: number;
    budget: { steps: number; minutes: number };
    endReason: EndReason;
    endDetail?: string;
    factsCommandsXmlMd5: string;
  };
  policy: {
    allowed: number;
    blockedByHazard: number;
    blockedByRule: number;
    unclassified: string[];
  };
  coverage: {
    commandsInvoked: number;
    /** Allowed commands that never had a live user path during the run. */
    notFuzzed: string[];
  };
  findings: Finding[];
  leads: Lead[];
  artifacts: { actionLog: string };
};

// ---------------------------------------------------------------------------
// Signatures
// ---------------------------------------------------------------------------

/**
 * Frames worth putting in a signature.
 *
 * Optimized GWT builds render frames as `at Unknown.pKi(rstudio-0.js)`. Both
 * halves are per-build noise: the symbol changes with every compile, and
 * "Unknown" is a literal placeholder. An earlier version tested each frame for
 * a readable identifier and kept these, because the word "Unknown" passes that
 * test, so signatures changed on every rebuild and nothing ever deduplicated.
 *
 * So the placeholders and the script file name are removed first, and only then
 * is the frame tested for a surviving identifier of four characters or more.
 * Obfuscated frames come out empty and the signature falls back to the kind and
 * the message, which is stable across builds. Draft builds, which carry real
 * Java names, keep their full detail.
 */
export function normalizeFrames(stack: string): string[] {
  const frames: string[] = [];

  for (const rawLine of stack.split('\n')) {
    const line = rawLine.trim();
    if (!line.startsWith('at '))
      continue;

    const stripped = line
      .slice(3)
      // Placeholders the compiler emits where a name is unavailable.
      .replace(/\b(Unknown|anonymous|<anonymous>)\b/g, ' ')
      // Script file names: rstudio-0.js, ABC123.cache.js, bundle.js.
      .replace(/[\w-]+\.cache\.js/g, ' ')
      .replace(/[\w-]+\.js/g, ' ')
      // Line and column positions, which move with any edit.
      .replace(/:\d+(:\d+)?/g, ' ')
      .replace(/[()]/g, ' ')
      .trim();

    const identifiers = stripped
      .split(/[^A-Za-z0-9_$]+/)
      .filter(token => token.length >= 4 && /[A-Za-z]/.test(token));

    if (identifiers.length > 0)
      frames.push(identifiers.join('.'));
  }

  return frames;
}

/**
 * Normalize a message so two runs of the same crash agree.
 * Quoted values, numbers, and generated ids differ per run and say nothing
 * about which defect this is.
 */
export function normalizeMessage(message: string): string {
  return message
    .replace(/gwt-uid-\d+/g, 'gwt-uid')
    .replace(/\b\d+\b/g, 'N')
    .replace(/\s+/g, ' ')
    .trim();
}

/**
 * Stable identity for a crash: kind, normalized message, and up to the top
 * three normalized frames. Degrades to kind and message when the build gives
 * no usable frames.
 */
export function signatureFor(kind: FindingKind, message: string, stack: string): string {
  const frames = normalizeFrames(stack).slice(0, 3);
  const basis = [kind, normalizeMessage(message), ...frames].join('|');
  return createHash('sha256').update(basis).digest('hex').slice(0, 12);
}

// ---------------------------------------------------------------------------
// The step lint
// ---------------------------------------------------------------------------

export class StepLintError extends Error {
  constructor(readonly sentence: string, readonly why: string) {
    super(`A reproduction step is not something a person can follow (${why}): ${sentence}`);
    this.name = 'StepLintError';
  }
}

/**
 * Verbs a step may open with. A reader has to be able to do the first word.
 */
const ACTIONABLE_VERBS = [
  'Press', 'Type', 'Click', 'Choose', 'Open', 'Close', 'Wait', 'Select',
];

/**
 * Command ids are rejected when they appear in a step, but only the ones an
 * ordinary sentence could not contain by accident: those with an internal
 * capital. `newSourceDoc` and `presentation2PresentFromBeginning` are
 * unmistakably internal. Ids that are also plain English words -- `reindent`,
 * `redo` -- are left alone, because "Press Cmd+I to reindent the selection" is
 * a perfectly good step and flagging it would push the tool toward printing
 * nothing rather than printing the truth.
 */
const CAMEL_CASE_COMMAND_IDS = Object.keys(COMMAND_FACTS).filter(id => /[a-z][A-Z]/.test(id));

const REJECTIONS: { pattern: RegExp; why: string }[] = [
  // Test-framework vocabulary.
  { pattern: /\bpage\s*\./, why: 'a Playwright call' },
  { pattern: /\blocator\b/i, why: 'a Playwright call' },
  { pattern: /\bpressSequentially\b|\bgetByRole\b|\btoBeVisible\b/, why: 'a Playwright call' },
  { pattern: /\bexpect\s*\(/, why: 'a test assertion' },
  { pattern: /\bawait\s/, why: 'test code' },
  { pattern: /\bplaywright\b/i, why: 'the test framework' },
  { pattern: /\bnpx\b|\bnpm run\b/, why: 'a command line' },

  // File and artifact names.
  { pattern: /[\w-]+\.test\.ts\b/, why: 'a test file name' },
  { pattern: /[\w-]+\.(jsonl|json|zip)\b/, why: 'an artifact file name' },
  { pattern: /\bspec\b/i, why: 'a test-suite word the reader has no use for' },
  { pattern: /\bfixture\b/i, why: 'a test-suite word the reader has no use for' },

  // Run settings.
  { pattern: /\bPW_[A-Z_]+\b/, why: 'an environment setting' },
  { pattern: /\bseed\b/i, why: 'a seed tells the reader how to gamble, not what to do' },

  // Internal identifiers and selectors.
  { pattern: /#rstudio_/, why: 'an element selector' },
  { pattern: /\.gwt-/, why: 'an element selector' },
  { pattern: /\bgwt-uid-?\d*/, why: 'a generated element id' },
  { pattern: /\[(id|class|role|aria-label)\s*[\^*~|$]?=/, why: 'an element selector' },
];

/**
 * Check one step sentence. Throws on anything a reader could not act on.
 *
 * Called by the renderer for every step of every finding, and exercised
 * directly by the self-test against one example per rule.
 */
export function lintStep(sentence: string): void {
  const trimmed = sentence.trim();

  if (trimmed === '')
    throw new StepLintError(sentence, 'it is empty');

  for (const { pattern, why } of REJECTIONS) {
    if (pattern.test(trimmed))
      throw new StepLintError(sentence, why);
  }

  for (const id of CAMEL_CASE_COMMAND_IDS) {
    // Word-boundary match, case sensitive: the id as written in the source.
    if (new RegExp(`\\b${id}\\b`).test(trimmed))
      throw new StepLintError(sentence, `the internal command id "${id}"`);
  }

  const firstWord = trimmed.split(/\s+/)[0].replace(/[^A-Za-z]/g, '');
  if (!ACTIONABLE_VERBS.includes(firstWord)) {
    throw new StepLintError(
      sentence,
      `it opens with "${firstWord}" rather than one of ${ACTIONABLE_VERBS.join(', ')}`,
    );
  }
}

/**
 * Scan a whole rendered document for the same patterns.
 *
 * The guarantee is about the finished page, not only the sentences that went
 * into it, so this runs over `renderFindings` output as well. Headings and
 * caveats are exempt from the opening-verb rule; the rejection patterns are not.
 */
export function lintRenderedDocument(markdown: string): void {
  for (const { pattern, why } of REJECTIONS) {
    // The document legitimately names the report's own artifacts in its footer,
    // so scan only the numbered-step lines: those are the ones a reader follows.
    for (const line of markdown.split('\n')) {
      if (!/^\d+\.\s/.test(line.trim()))
        continue;
      if (pattern.test(line))
        throw new StepLintError(line.trim(), why);
    }
  }
}

// ---------------------------------------------------------------------------
// Rendering
// ---------------------------------------------------------------------------

/**
 * Preference names spelled out as a person would change them, so a reader can
 * put their own RStudio into the state the run was in. The wording matches the
 * checkbox labels in Global Options.
 */
const PREF_SENTENCES: Record<string, (value: unknown) => string> = {
  native_file_dialogs: value => (
    value === false
      ? "In Tools > Global Options > General > Advanced, uncheck 'Use native file and "
        + "message dialog boxes'. It is checked by default, and with it checked the file "
        + 'dialogs are the operating system\'s, so this may not reproduce.'
      : "In Tools > Global Options > General > Advanced, check 'Use native file and "
        + "message dialog boxes'."
  ),
  reduced_motion: value => (
    value === true
      ? "In Tools > Global Options > Accessibility, check 'Reduce user interface "
        + "animations'."
      : "In Tools > Global Options > Accessibility, uncheck 'Reduce user interface "
        + "animations'."
  ),
  save_workspace: value => `Set the "Save workspace to .RData on exit" option to "${String(value)}".`,
  restore_source_documents: value => (
    value === false
      ? "In Tools > Global Options > General, uncheck 'Restore most recently opened "
        + "project at startup' related source-document restore."
      : 'Leave source-document restore at its default.'
  ),
};

function prefSentence(name: string, value: unknown): string {
  const sentence = PREF_SENTENCES[name];
  if (sentence !== undefined)
    return sentence(value);
  return `Set the preference "${name}" to ${JSON.stringify(value)}.`;
}

function collapseStack(stack: string): string {
  const trimmed = stack.trim();
  if (trimmed === '')
    return '';
  return [
    '<details><summary>Stack</summary>',
    '',
    '```',
    trimmed,
    '```',
    '',
    '</details>',
  ].join('\n');
}

function renderPrecondition(precondition: Precondition): string[] {
  const lines: string[] = ['**Before the crash**', ''];

  const prefNames = Object.keys(precondition.prefs);
  if (prefNames.length > 0) {
    lines.push('This run differed from a default install in these settings, and the '
      + 'crash may depend on them:');
    lines.push('');
    for (const name of prefNames)
      lines.push(`- ${prefSentence(name, precondition.prefs[name])}`);
    lines.push('');
  }

  const onScreen: string[] = [];
  onScreen.push(precondition.dialogTitle === null
    ? 'No dialog was open.'
    : `The "${precondition.dialogTitle}" dialog was open.`);
  if (precondition.activeDoc !== null)
    onScreen.push(`The editor was showing ${precondition.activeDoc}.`);
  if (precondition.activeTabs.length > 0)
    onScreen.push(`Selected tabs: ${precondition.activeTabs.join(', ')}.`);
  lines.push(onScreen.join(' '));
  lines.push('');

  return lines;
}

function renderVerifiedFinding(finding: Finding, index: number, run: LokiReport['run']): string[] {
  const lines: string[] = [];
  lines.push(`### ${index + 1}. ${finding.message}`);
  lines.push('');
  lines.push(`RStudio ${run.rstudioVersion}, R ${run.rVersion}, ${run.platform}, `
    + `${run.mode} mode. Seen ${finding.count} time${finding.count === 1 ? '' : 's'}.`);
  lines.push('');
  lines.push(...renderPrecondition(finding.precondition));

  lines.push('**Steps to reproduce**');
  lines.push('');
  for (const step of finding.steps) {
    lintStep(step.do);
    lines.push(`${step.n}. ${step.do}`);
  }
  lines.push('');

  lines.push('**What happens**');
  lines.push('');
  lines.push(`RStudio raises an uncaught error: \`${finding.message}\``);
  lines.push('');
  const stack = collapseStack(finding.stack);
  if (stack !== '') {
    lines.push(stack);
    lines.push('');
  }

  const also = finding.alsoReachableVia;
  if (also && (also.menuPath || also.shortcut)) {
    const parts: string[] = [];
    if (also.menuPath)
      parts.push(`the menu at ${also.menuPath}`);
    if (also.shortcut)
      parts.push(`the keyboard shortcut ${also.shortcut}`);
    lines.push(`The same command is also reachable through ${parts.join(', and ')}.`);
    lines.push('');
  }

  return lines;
}

function renderUnverifiedFinding(finding: Finding, index: number): string[] {
  const lines: string[] = [];
  lines.push(`### ${index + 1}. ${finding.message}`);
  lines.push('');
  lines.push(finding.status === 'not-reproduced' ? NOT_REPRODUCED_NOTE : FULL_LOG_NOTE);
  lines.push('');
  lines.push(`Seen ${finding.count} time${finding.count === 1 ? '' : 's'}, `
    + `first at step ${finding.firstStep}.`);
  lines.push('');
  const stack = collapseStack(finding.stack);
  if (stack !== '') {
    lines.push(stack);
    lines.push('');
  }
  return lines;
}

/**
 * Render the human-facing document.
 *
 * Verified findings get numbered steps. Everything else goes under a separate
 * heading with its caveat and no steps at all, which is the point: a reader must
 * never have to work out whether a list has been checked.
 */
export function renderFindings(report: LokiReport): string {
  const lines: string[] = [];

  lines.push('# Agent Loki');
  lines.push('');
  lines.push(`Agent Loki drove RStudio ${report.run.rstudioVersion} through `
    + `${report.run.stepsExecuted} actions in ${report.run.mode} mode on `
    + `${report.run.platform}, and stopped because: ${describeEndReason(report.run)}.`);
  lines.push('');

  const verified = report.findings.filter(f => f.status === 'verified');
  const unverified = report.findings.filter(f => f.status !== 'verified');

  if (verified.length === 0 && unverified.length === 0 && report.leads.length === 0) {
    lines.push('No crashes were found.');
    lines.push('');
  }

  if (verified.length > 0) {
    lines.push('## Reproducible crashes');
    lines.push('');
    lines.push('Each of these was reproduced from the steps below, on its own, from a '
      + 'clean start. The steps are what the tool actually did.');
    lines.push('');
    verified.forEach((finding, index) => {
      lines.push(...renderVerifiedFinding(finding, index, report.run));
    });
  }

  if (unverified.length > 0) {
    lines.push('## Leads, not independently verified');
    lines.push('');
    unverified.forEach((finding, index) => {
      lines.push(...renderUnverifiedFinding(finding, index));
    });
  }

  if (report.leads.length > 0) {
    lines.push('## Exceptions with no attributable action');
    lines.push('');
    for (const lead of report.leads) {
      lines.push(`- \`${lead.message}\` (seen ${lead.count} `
        + `time${lead.count === 1 ? '' : 's'}${lead.duringRecovery ? ', during recovery' : ''}). `
        + lead.note);
    }
    lines.push('');
  }

  const markdown = lines.join('\n');
  // The guarantee is about the finished document, not only the sentences that
  // were fed into it.
  lintRenderedDocument(markdown);
  return markdown;
}

function describeEndReason(run: LokiReport['run']): string {
  const detail = run.endDetail ? ` (${run.endDetail})` : '';
  switch (run.endReason) {
    case 'steps': return `it finished its budget of ${run.budget.steps} actions${detail}`;
    case 'time': return `it ran out of its ${run.budget.minutes} minutes${detail}`;
    case 'wedged': return `the interface stopped offering anything to do${detail}`;
    case 'lost-ui': return `the session went away${detail}`;
    case 'session-death': return `R stopped responding${detail}`;
    case 'replay-complete': return `it finished replaying a recorded run${detail}`;
    case 'tool-error': return `Agent Loki itself failed${detail}`;
  }
}

// ---------------------------------------------------------------------------
// Collecting
// ---------------------------------------------------------------------------

/**
 * Accumulates crashes during a run, deduplicating by signature.
 *
 * Findings and leads are kept apart from the moment of collection. An exception
 * that arrived while an action was being attributed can carry steps; one that
 * arrived outside a step, or during recovery, cannot, and never will, so it
 * never enters the findings list where a later stage might try to give it some.
 */
export class Collector {
  private readonly findings = new Map<string, Finding>();
  private readonly leads = new Map<string, Lead>();

  /** Record an exception attributed to a performed action. */
  addFinding(args: {
    kind: FindingKind;
    message: string;
    stack: string;
    step: number;
    precondition: Precondition;
  }): Finding {
    const signature = signatureFor(args.kind, args.message, args.stack);
    const existing = this.findings.get(signature);
    if (existing !== undefined) {
      existing.count++;
      return existing;
    }
    const finding: Finding = {
      signature,
      // Nothing is verified until a replay says so.
      status: 'not-reproduced',
      kind: args.kind,
      message: args.message,
      stack: args.stack,
      count: 1,
      firstStep: args.step,
      precondition: args.precondition,
      steps: [],
    };
    this.findings.set(signature, finding);
    return finding;
  }

  /** Record an exception with no action behind it. */
  addLead(args: {
    kind: FindingKind;
    message: string;
    stack: string;
    duringRecovery: boolean;
  }): Lead {
    const signature = signatureFor(args.kind, args.message, args.stack);
    const existing = this.leads.get(signature);
    if (existing !== undefined) {
      existing.count++;
      // Once attributable to a real gap, stay that way.
      existing.duringRecovery = existing.duringRecovery || args.duringRecovery;
      return existing;
    }
    const lead: Lead = {
      signature,
      kind: args.kind,
      message: args.message,
      stack: args.stack,
      count: 1,
      duringRecovery: args.duringRecovery,
      note: LEAD_NOTE,
    };
    this.leads.set(signature, lead);
    return lead;
  }

  /** Worst first, so minimisation spends its budget on the most-seen crash. */
  allFindings(): Finding[] {
    return Array.from(this.findings.values()).sort((a, b) => (
      b.count - a.count || a.firstStep - b.firstStep
    ));
  }

  allLeads(): Lead[] {
    return Array.from(this.leads.values()).sort((a, b) => b.count - a.count);
  }
}
