/**
 * What Agent Loki is allowed to do.
 *
 * Two layers, in this order:
 *
 *   1. **Hazards**, from utils/loki/command-facts.ts. Behaviour read out of the
 *      command's handler or its own description: it opens a browser, replaces
 *      the page, raises an operating-system dialog, ends the session, or exists
 *      to crash. This layer is regenerated from source, not maintained here.
 *   2. **Rules**, below. Intent, expressed as named sets of command ids. A rule
 *      says why a family of commands is off limits even when nothing in its
 *      handler is technically hazardous: it costs money, it touches a machine
 *      that is not this one, it destroys something the run needs, or it takes
 *      four minutes.
 *
 * Anything the facts file has never heard of is blocked as unclassified, which
 * is how a run against a newer build stays safe: the commands it does not know
 * about are exactly the ones it must not touch.
 *
 * Both layers exist because either alone has failed. Blocking by name missed
 * every cheat-sheet command; blocking only by handler behaviour would happily
 * fuzz `installPackage`.
 */

import { COMMAND_FACTS, COMMANDS_XML_MD5, type CommandFact, type Hazard } from './command-facts';
import { toolbarButtonId } from './element-ids';

export { COMMAND_FACTS, COMMANDS_XML_MD5 };
export type { CommandFact, Hazard };

// ---------------------------------------------------------------------------
// Rules
// ---------------------------------------------------------------------------

export type Rule = {
  /** Short name, quoted in the report. */
  name: string;
  /** Why the family is off limits, in one sentence. */
  why: string;
  ids: string[];
  /** Families too large to list, such as the recent-project entries. */
  pattern?: RegExp;
};

/**
 * Checked in order. The first match wins, so the name in a report is the most
 * specific reason rather than whichever rule happened to be listed first.
 */
export const RULES: Rule[] = [
  {
    name: 'deliberate-crash',
    why: 'exists in order to crash, so a run that reached it would report its own bait as a finding',
    ids: ['raiseException', 'raiseException2', 'crashDesktopApplication'],
  },
  {
    name: 'harness-integrity',
    why: 'breaks the instrument the run drives, or the state the run depends on',
    ids: [
      // The run owns the palette; a step that reopened it would fight the executor.
      'showCommandPalette',
      // Wipes every preference, including the web-dialog setting this suite sets.
      'clearUserPrefs',
      // Reloads the frontend, taking the automation bridge with it.
      'reloadUi',
      'refreshSuperDevMode',
      // Developer instrumentation that floods the page it is meant to observe.
      'showRequestLog',
      'logFocusedElement',
      'enableProsemirrorDevTools',
    ],
  },
  {
    name: 'session-lifecycle',
    why: 'ends, restarts, suspends, or switches the session the run is driving',
    ids: [
      'quitSession', 'forceQuitSession', 'suspendSession',
      'restartR', 'restartRClearOutput', 'restartRRunAllChunks', 'terminateR',
      'signOut', 'assistantSignOut',
      // A project change restarts R and resets the bridge, which also makes the
      // clean state that minimisation assumes untrue.
      'newProject', 'openProject', 'openProjectInNewWindow', 'openSharedProject',
      'closeProject', 'shareProject',
    ],
    pattern: /^projectMru\d+$/,
  },
  {
    name: 'off-machine',
    why: 'reaches an account, a paid service, a package repository, or a remote branch',
    ids: [
      'rsconnectDeploy', 'rsconnectConfigure', 'rsconnectManageAccounts',
      'publishHTML', 'showPublishingOptions',
      'checkForUpdates', 'checkForPositAssistantUpdates', 'updateCredentials',
      'rstudioLicense', 'showLicenseDialog',
      'assistantSignIn', 'uninstallPositAssistant',
      'installPackage', 'updatePackages',
      'packratBootstrap', 'packratBundle', 'renvRestore', 'renvSnapshot',
      'vcsPush', 'vcsPull', 'vcsPullRebase',
    ],
  },
  {
    name: 'substrate-destruction',
    why: 'destroys files, version-control state, or session history that the run needs to stay reproducible',
    ids: [
      'deleteFiles',
      'vcsRevert', 'vcsFileRevert', 'vcsRemoveFiles', 'vcsCleanup',
      'vcsCommit', 'vcsAddFiles', 'vcsIgnore', 'vcsResolve',
      'cleanAll', 'packratClean',
      'clearWorkspace', 'clearHistory', 'historyRemoveEntries',
      'clearRecentFiles', 'clearRecentProjects',
      'removeConnection',
    ],
  },
  {
    name: 'shell-execution',
    why: 'runs editor text as a shell command on the host, outside the sandbox',
    ids: ['sendToTerminal', 'sendFilenameToTerminal'],
  },
  {
    name: 'long-toolchain',
    why: 'starts a build, check, or test run that takes minutes and drowns the step budget',
    ids: [
      'buildAll', 'buildFull', 'buildIncremental',
      'buildSourcePackage', 'buildBinaryPackage',
      'checkPackage', 'testPackage', 'roxygenizePackage', 'devtoolsLoadAll',
      'testTestthatFile', 'testShinytestFile', 'shinyRunAllTests',
      'serveQuartoSite',
    ],
  },
];

/**
 * Shortcuts the run never presses, whatever they are bound to.
 *
 * The command-bound half is derived: shortcutIsBlocked also rejects anything
 * bound to a blocked command. These are the ones that belong to the browser or
 * the operating system rather than to a command at all.
 */
export const BLOCKED_SHORTCUTS: string[] = [
  // Quits the application on macOS and Windows.
  'Meta+Q', 'Ctrl+Q',
  // Closes the window, taking the session with it.
  'Meta+W', 'Ctrl+W',
  // Reloads the page, destroying the automation bridge.
  'F5', 'Meta+R', 'Ctrl+R', 'Meta+Shift+R', 'Ctrl+Shift+R',
  // Opens the browser's own developer tools over the application.
  'F12', 'Meta+Alt+I', 'Ctrl+Shift+I',
  // Prints through the browser, which can raise the operating-system sheet.
  'Meta+P', 'Ctrl+P',
];

// ---------------------------------------------------------------------------
// Classification
// ---------------------------------------------------------------------------

export type BlockReason =
  | { kind: 'hazard'; hazards: Hazard[] }
  | { kind: 'rule'; rule: string; why: string }
  | { kind: 'unclassified' };

export type Classification =
  | { allowed: true; fact: CommandFact }
  | { allowed: false; reason: BlockReason };

function matchingRule(commandId: string): Rule | undefined {
  return RULES.find(rule => (
    rule.ids.includes(commandId) || (rule.pattern?.test(commandId) ?? false)
  ));
}

/**
 * Decide whether the run may invoke a command.
 *
 * Hazards first, so a report names the behaviour rather than the family; then
 * rules; then presence in the facts. A command absent from the facts is blocked:
 * the build under test may be newer than this checkout, and an unknown command
 * is the one case where guessing is worst.
 */
export function classifyCommand(commandId: string): Classification {
  const fact = COMMAND_FACTS[commandId];

  if (fact !== undefined && fact.hazards.length > 0)
    return { allowed: false, reason: { kind: 'hazard', hazards: fact.hazards } };

  const rule = matchingRule(commandId);
  if (rule !== undefined)
    return { allowed: false, reason: { kind: 'rule', rule: rule.name, why: rule.why } };

  if (fact === undefined)
    return { allowed: false, reason: { kind: 'unclassified' } };

  return { allowed: true, fact };
}

/** Convenience for filtering candidate lists. */
export function isAllowed(commandId: string): boolean {
  return classifyCommand(commandId).allowed;
}

/**
 * Element ids the DOM-click route must never click: the toolbar buttons of
 * blocked commands. Clicking "Publish" is blocked whether the run reached it
 * through the palette or through the button face.
 */
export function blockedElementIds(): string[] {
  return Object.keys(COMMAND_FACTS)
    .filter(id => !isAllowed(id))
    .map(toolbarButtonId);
}

/**
 * Whether a keyboard shortcut is off limits, either in its own right or because
 * of the command it runs.
 */
export function shortcutIsBlocked(shortcut: string): boolean {
  if (BLOCKED_SHORTCUTS.includes(shortcut))
    return true;
  for (const [id, fact] of Object.entries(COMMAND_FACTS)) {
    if (fact.shortcuts.some(s => s.value === shortcut) && !isAllowed(id))
      return true;
  }
  return false;
}

// ---------------------------------------------------------------------------
// Audit
// ---------------------------------------------------------------------------

export type PolicyAudit = {
  allowed: number;
  blockedByHazard: number;
  blockedByRule: number;
  /**
   * Commands the running build has that this checkout does not. All blocked.
   * A non-empty list is information, not a fault: it means the build under test
   * is not the one these facts were generated from.
   */
  unclassified: string[];
};

/**
 * Classify every command the live session reports, for the report's policy
 * section. Pass `window.rstudio.commands.list`.
 */
export function auditPolicy(liveCommandIds: readonly string[]): PolicyAudit {
  const audit: PolicyAudit = {
    allowed: 0,
    blockedByHazard: 0,
    blockedByRule: 0,
    unclassified: [],
  };

  for (const id of liveCommandIds) {
    const result = classifyCommand(id);
    if (result.allowed) {
      audit.allowed++;
      continue;
    }
    switch (result.reason.kind) {
      case 'hazard': audit.blockedByHazard++; break;
      case 'rule': audit.blockedByRule++; break;
      case 'unclassified': audit.unclassified.push(id); break;
    }
  }

  audit.unclassified.sort();
  return audit;
}
