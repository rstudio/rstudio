/**
 * Read RStudio's command handlers and report which ones leave the application.
 *
 * This exists because a command's name and description do not tell you what it
 * does. "Browse Cheat Sheets" says "web browser" in its description and is easy
 * to catch; the ten other cheat-sheet commands say nothing of the kind and open
 * exactly the same external link. Blocking by name misses them, and a run that
 * opens a browser mid-step loses the window it was driving.
 *
 * The evidence is the handler body. Every command handler is a method named
 * on<CommandId> marked @Handler, so this walks the GWT sources, matches each
 * handler to its command, and looks for the calls that hand a URL or a file to
 * something outside RStudio. Handlers usually delegate to a private helper in
 * the same file (Help.onOpenRMarkdownCheatSheet calls openCheatSheet, which
 * calls openRStudioLink), so calls are followed a bounded number of levels
 * within the file that declares them.
 *
 * Used by generate-command-facts.ts. Run directly to see what it finds:
 *   npx tsx utils/loki/scan-command-handlers.ts
 */

import { readdirSync, readFileSync, statSync } from 'fs';
import path from 'path';

const GWT_SOURCE_ROOT = path.resolve(__dirname, '../../../../src/gwt/src/org/rstudio');

/** How many levels of same-file helper calls to follow out of a handler. */
const MAX_CALL_DEPTH = 3;

export type ExternalKind = 'external-browser' | 'external-application';

/**
 * Calls that hand a URL or a path to something outside the application, with
 * the kind of departure each one causes.
 *
 * `openWindow` earns its place: DesktopWindowOpener.openWindow sends any URL
 * with a protocol that is not an app URL to Desktop.getFrame().browseUrl, which
 * is the operating system's browser.
 *
 * Deliberately absent, both verified rather than assumed:
 *
 *   - openMinimalWindow / openWebMinimalWindow. DesktopWindowOpener routes
 *     these to DesktopFrame.openMinimalWindow, an Electron window belonging to
 *     RStudio; on Server WebWindowOpener uses window.open, a popup in the same
 *     browser context. Neither is the operating system's browser, and the run
 *     already notices new pages and closes them. Treating them as external
 *     would block openDeveloperConsole, showA11yDiagnostics, showGpuDiagnostics
 *     and viewerZoom for no reason.
 *   - openSatelliteWindow, for the same reason: a satellite is RStudio's own
 *     window in both modes.
 */
const EXTERNAL_CALLS: { call: string; kind: ExternalKind; note: string }[] = [
  { call: 'openWindow', kind: 'external-browser', note: 'GlobalDisplay.openWindow' },
  { call: 'openProgressWindow', kind: 'external-browser', note: 'GlobalDisplay.openProgressWindow' },
  { call: 'openRStudioLink', kind: 'external-browser', note: 'GlobalDisplay.openRStudioLink' },
  { call: 'browseUrl', kind: 'external-browser', note: 'DesktopFrame.browseUrl' },
  { call: 'showHtmlFile', kind: 'external-application', note: 'GlobalDisplay.showHtmlFile' },
  { call: 'showWordDoc', kind: 'external-application', note: 'opens the document in Word' },
  { call: 'showPptPresentation', kind: 'external-application', note: 'opens the deck in PowerPoint' },
  { call: 'showPDF', kind: 'external-application', note: 'DesktopFrame.showPDF' },
  { call: 'showFile', kind: 'external-application', note: 'DesktopFrame.showFile' },
  { call: 'showFolder', kind: 'external-application', note: 'DesktopFrame.showFolder' },
  { call: 'openSessionInNewWindow', kind: 'external-application', note: 'launches a second RStudio session' },
];

export type ExternalFinding = {
  commandId: string;
  kind: ExternalKind;
  /** Where the evidence is, for a reviewer to check. */
  evidence: string;
};

// ---------------------------------------------------------------------------
// Java reading
// ---------------------------------------------------------------------------

/**
 * Blank out comments and string literals, keeping the text length so any
 * offsets stay aligned. Brace matching and call detection both need this: a
 * brace inside a string would unbalance the scan, and a method name inside a
 * comment is not a call.
 */
function blankNonCode(java: string): string {
  let out = '';
  let i = 0;
  while (i < java.length) {
    const two = java.slice(i, i + 2);
    if (two === '//') {
      const end = java.indexOf('\n', i);
      const stop = end === -1 ? java.length : end;
      out += ' '.repeat(stop - i);
      i = stop;
      continue;
    }
    if (two === '/*') {
      const end = java.indexOf('*/', i + 2);
      const stop = end === -1 ? java.length : end + 2;
      // Keep newlines so line numbers survive.
      out += java.slice(i, stop).replace(/[^\n]/g, ' ');
      i = stop;
      continue;
    }
    if (java[i] === '"' || java[i] === "'") {
      const quote = java[i];
      let j = i + 1;
      while (j < java.length && java[j] !== quote) {
        if (java[j] === '\\') j++;
        j++;
      }
      const stop = Math.min(j + 1, java.length);
      out += java.slice(i, stop).replace(/[^\n]/g, ' ');
      i = stop;
      continue;
    }
    out += java[i];
    i++;
  }
  return out;
}

/** Body text of every method in one file, keyed by method name. */
type MethodIndex = Map<string, string[]>;

const METHOD_RE = /(?:^|[\s;}])([A-Za-z_$][\w$]*)\s*\(([^;{}()]*)\)\s*(?:throws\s+[\w.,\s]+)?\{/g;

function indexMethods(code: string): MethodIndex {
  const index: MethodIndex = new Map();
  for (const match of code.matchAll(METHOD_RE)) {
    const name = match[1];
    // Control-flow keywords look like calls followed by a block.
    if (['if', 'for', 'while', 'switch', 'catch', 'synchronized', 'try'].includes(name))
      continue;
    const open = match.index + match[0].length - 1;
    const body = extractBlock(code, open);
    if (body === undefined)
      continue;
    const bodies = index.get(name) ?? [];
    bodies.push(body);
    index.set(name, bodies);
  }
  return index;
}

/** Text between the brace at `open` and its match, or undefined if unbalanced. */
function extractBlock(code: string, open: number): string | undefined {
  let depth = 0;
  for (let i = open; i < code.length; i++) {
    if (code[i] === '{') depth++;
    else if (code[i] === '}') {
      depth--;
      if (depth === 0)
        return code.slice(open + 1, i);
    }
  }
  return undefined;
}

/** Names called in a fragment of code, ignoring declarations and keywords. */
function calledNames(body: string): string[] {
  const names = new Set<string>();
  for (const match of body.matchAll(/([A-Za-z_$][\w$]*)\s*\(/g)) {
    const name = match[1];
    if (['if', 'for', 'while', 'switch', 'catch', 'return', 'new', 'super', 'this'].includes(name))
      continue;
    names.add(name);
  }
  return Array.from(names);
}

/**
 * Interface and callback methods that many unrelated classes in one file all
 * implement. Following them by name jumps into whichever anonymous class
 * happened to be indexed under the same name, which reads as evidence and is
 * not: it reported Files.onDeleteFiles as a browser opener because some other
 * ProgressOperation in Files.java calls showFileInBrowser.
 *
 * Nothing is lost by refusing to follow them. A callback written inline sits
 * lexically inside the handler body, so the direct scan already sees it.
 */
const CALLBACK_NAMES = new Set(['execute', 'run', 'call', 'apply', 'accept', 'get']);

/**
 * Whether a called name is worth following out of a handler.
 *
 * Three exclusions, each for the same reason: the name is shared by many
 * unrelated implementations in one file, so resolving it by name lands
 * somewhere arbitrary.
 *
 *   - Java methods are lowerCamelCase, so an uppercase initial is really a
 *     constructor call for an anonymous class.
 *   - on<Something> is an event or callback interface method. Following
 *     onReadyToQuit out of Projects.onCloseProject reported it as a browser
 *     opener, when the handler in fact calls performQuit. A command handler is
 *     never another handler's helper, so nothing real is lost.
 *   - The short generic callback names above.
 */
function followable(name: string): boolean {
  return /^[a-z]/.test(name) && !/^on[A-Z]/.test(name) && !CALLBACK_NAMES.has(name);
}

// ---------------------------------------------------------------------------
// Handler matching
// ---------------------------------------------------------------------------

/**
 * Resolve on<Name> to a command id. The usual case only needs the leading
 * capital lowered (onLoadServerHome -> loadServerHome); a handler written with
 * a leading acronym (onVCSPush) needs the case-insensitive fallback.
 */
function commandIdForHandler(handler: string, knownIds: Set<string>): string | undefined {
  const stem = handler.slice(2);
  if (stem === '')
    return undefined;
  const naive = stem[0].toLowerCase() + stem.slice(1);
  if (knownIds.has(naive))
    return naive;
  const lowered = stem.toLowerCase();
  for (const id of knownIds) {
    if (id.toLowerCase() === lowered)
      return id;
  }
  return undefined;
}

/**
 * Find the external-departure call reachable from a handler body, following
 * same-file helpers up to MAX_CALL_DEPTH.
 */
function findExternalCall(
  body: string,
  methods: MethodIndex,
  depth: number,
  seen: Set<string>,
): { kind: ExternalKind; note: string } | undefined {
  const called = calledNames(body);

  for (const { call, kind, note } of EXTERNAL_CALLS) {
    if (called.includes(call))
      return { kind, note };
  }

  if (depth >= MAX_CALL_DEPTH)
    return undefined;

  for (const name of called) {
    if (seen.has(name) || !followable(name))
      continue;
    seen.add(name);
    for (const helperBody of methods.get(name) ?? []) {
      const hit = findExternalCall(helperBody, methods, depth + 1, seen);
      if (hit)
        return { kind: hit.kind, note: `${hit.note} (via ${name})` };
    }
  }
  return undefined;
}

function javaFiles(dir: string): string[] {
  const out: string[] = [];
  for (const entry of readdirSync(dir)) {
    const full = path.join(dir, entry);
    if (statSync(full).isDirectory())
      out.push(...javaFiles(full));
    else if (entry.endsWith('.java'))
      out.push(full);
  }
  return out;
}

/**
 * Scan the GWT sources for commands whose handlers leave the application.
 *
 * `knownIds` is the command-id set from Commands.cmd.xml: it both filters out
 * on<X> methods that are not command handlers and guarantees every reported id
 * is a real command.
 */
export function scanExternalCommands(knownIds: Set<string>): ExternalFinding[] {
  const findings = new Map<string, ExternalFinding>();

  for (const file of javaFiles(GWT_SOURCE_ROOT)) {
    const raw = readFileSync(file, 'utf8');
    if (!raw.includes('@Handler'))
      continue;

    const code = blankNonCode(raw);
    const methods = indexMethods(code);
    const relative = path.relative(path.resolve(GWT_SOURCE_ROOT, '../../../..'), file);

    for (const [name, bodies] of methods) {
      if (!/^on[A-Z]/.test(name))
        continue;
      const commandId = commandIdForHandler(name, knownIds);
      if (commandId === undefined)
        continue;

      for (const body of bodies) {
        const hit = findExternalCall(body, methods, 0, new Set([name]));
        if (hit === undefined)
          continue;
        // First file to show the behavior wins; an override elsewhere would
        // report the same departure.
        if (!findings.has(commandId)) {
          findings.set(commandId, {
            commandId,
            kind: hit.kind,
            evidence: `${relative} ${name}: ${hit.note}`,
          });
        }
      }
    }
  }

  return Array.from(findings.values()).sort((a, b) => a.commandId.localeCompare(b.commandId));
}

if (require.main === module) {
  // Standalone: read the ids straight from the generated facts so the scan can
  // be inspected without regenerating them.
  const { COMMAND_FACTS } = require('./command-facts') as {
    COMMAND_FACTS: Record<string, unknown>;
  };
  const findings = scanExternalCommands(new Set(Object.keys(COMMAND_FACTS)));
  for (const finding of findings)
    console.log(`${finding.kind.padEnd(22)} ${finding.commandId.padEnd(34)} ${finding.evidence}`);
  console.log(`\n${findings.length} commands leave the application.`);
}
