/**
 * Generate utils/loki/command-facts.ts from Commands.cmd.xml.
 *
 * Run with `npm run loki:facts`. The output is checked in and reviewed like any
 * other source file: the hazard list decides what Agent Loki is allowed to
 * touch, so it has to be readable in a diff rather than computed at run time.
 *
 * Commands.cmd.xml spreads a single <cmd> over several lines and writes at
 * least one id as `id ="goToHelp"`, so this walks tags instead of matching
 * whole elements with one pattern.
 */

import { createHash } from 'crypto';
import { readFileSync, writeFileSync } from 'fs';
import path from 'path';
import { scanExternalCommands } from './scan-command-handlers';

const COMMANDS_XML = path.resolve(
  __dirname,
  '../../../../src/gwt/src/org/rstudio/studio/client/workbench/commands/Commands.cmd.xml',
);
const OUTPUT = path.resolve(__dirname, 'command-facts.ts');

// The output is worthless if it silently comes up thin: a parser that stopped
// understanding the file would otherwise produce a small, permissive facts set
// and the run would still look fine. These floors sit just under the real
// counts at the time of writing, so a parsing regression trips at once.
const MINIMUMS = { commands: 650, invisible: 35, menuPaths: 350, shortcuts: 170 };

// ---------------------------------------------------------------------------
// Tag walking
// ---------------------------------------------------------------------------

type Tag = {
  name: string;
  attrs: Record<string, string>;
  closing: boolean;
};

// One tag. The attribute run allows quoted values containing '>' so that a
// desc such as "a > b" cannot end the tag early.
const TAG_RE = /<(\/?)([A-Za-z][\w-]*)((?:[^>"']|"[^"]*"|'[^']*')*?)\/?>/g;
const ATTR_RE = /([\w-]+)\s*=\s*"([^"]*)"/g;

function walkTags(xml: string): Tag[] {
  // Comments hold example markup ("<cmd> (in menu context)"), which would
  // otherwise parse as real tags.
  const withoutComments = xml.replace(/<!--[\s\S]*?-->/g, '');
  const tags: Tag[] = [];
  for (const match of withoutComments.matchAll(TAG_RE)) {
    const [, close, name, attrChunk] = match;
    const attrs: Record<string, string> = {};
    for (const attr of attrChunk.matchAll(ATTR_RE))
      attrs[attr[1]] = attr[2];
    tags.push({ name, attrs, closing: close === '/' });
  }
  return tags;
}

/**
 * Resolve GWT mnemonic markers the way AppMenuItem.replaceMnemonics does with
 * an empty replacement: "__" is a literal underscore, a lone "_" marks the next
 * character and disappears. "Raise E_xception" becomes "Raise Exception".
 *
 * Applies to menuLabel and to <menu label>, and to nothing else. AppCommand's
 * getLabel/getButtonLabel/getDesc return their raw values, and devtoolsLoadAll
 * carries a real underscore ("Execute devtools::load_all()") that stripping
 * would destroy.
 */
function stripMnemonics(label: string): string {
  return label.replace(/__|_/g, match => (match === '__' ? '_' : ''));
}

function orUndefined(value: string | undefined): string | undefined {
  return value === undefined || value === '' ? undefined : value;
}

// ---------------------------------------------------------------------------
// Hazards
// ---------------------------------------------------------------------------

export type Hazard =
  /** Hands a URL to the desktop browser, taking focus away from the app. */
  | 'external-browser'
  /** Hands a file or folder to another application (Finder, Word, a PDF viewer). */
  | 'external-application'
  /** Replaces the page, destroying the automation bridge with it. */
  | 'navigates-away'
  /** Can raise the operating-system print sheet, which Playwright cannot close. */
  | 'os-print-dialog'
  /** Ends or restarts the R session the run depends on. */
  | 'session-lifecycle'
  /** Exists in order to crash or throw. */
  | 'deliberate-crash';

/**
 * Hazards the XML text cannot reveal, each traced to the handler that causes
 * it. Read the handler before adding a line here: a name is not evidence.
 */
const EXTRA_HAZARDS: Record<string, Hazard[]> = {
  // Application.java onLoadServerHome calls loadUserHomePage(), replacing the page.
  loadServerHome: ['navigates-away'],
  // Application.java onSignOut fires LogoutRequestedEvent.
  signOut: ['navigates-away', 'session-lifecycle'],
  // Application.java onCrashDesktopApplication crashes the Electron process.
  crashDesktopApplication: ['deliberate-crash'],
  // Application.java onRaiseException throws a RuntimeException; onRaiseException2
  // calls a missing JS function. The self-test invokes these two by name; the
  // fuzzing loop must never reach them, or every run would "find" them.
  raiseException: ['deliberate-crash'],
  raiseException2: ['deliberate-crash'],
  // AceEditor.print() (AceEditor.java) takes the non-desktop-frame branch on
  // Electron, rendering into a PrintIFrame and printing through the browser,
  // which can raise the operating-system print sheet. Playwright cannot close
  // that sheet, so the run would wedge. Not covered by native_file_dialogs.
  printSourceDoc: ['os-print-dialog'],
  printHelp: ['os-print-dialog'],
  printCppCompletions: ['os-print-dialog'],
  presentation2Print: ['os-print-dialog'],
};

/**
 * Derive `external-browser` from the command's own wording.
 *
 * Kept alongside the handler scan because neither one is sufficient. The
 * wording catches shinyRunInBrowser, plumberRunInBrowser, showPdfExternal and
 * sparkUI, whose handlers reach a browser by a route the scan does not follow;
 * the scan catches the ten cheat-sheet commands, every help link, popoutDoc and
 * showLogFiles, none of which say anything about a browser. The union is the
 * answer, and both halves are visible in the generated file.
 */
function describedHazards(text: string): Hazard[] {
  return /\b(external|browser)\b/i.test(text) ? ['external-browser'] : [];
}

// ---------------------------------------------------------------------------
// Parse
// ---------------------------------------------------------------------------

export type CommandFact = {
  labels: {
    label?: string;
    buttonLabel?: string;
    menuLabel?: string;
    desc?: string;
    /** What the Command Palette renders: label, buttonLabel, desc, menuLabel. */
    palette: string;
  };
  /** Main-menu location. Reports quote it as context; it is never a performed step. */
  menuPath?: string;
  shortcuts: { value: string; if?: string }[];
  visible: boolean;
  hazards: Hazard[];
};

type Counts = { commands: number; invisible: number; menuPaths: number; shortcuts: number };
type Parsed = { facts: Record<string, CommandFact>; counts: Counts };

function parse(xml: string, scanned: Map<string, Hazard>): Parsed {
  const tags = walkTags(xml);

  const defs = new Map<string, Record<string, string>>();
  const menuPaths = new Map<string, string>();
  const shortcuts = new Map<string, { value: string; if?: string }[]>();

  // Labels of the enclosing menus, maintained only inside <menu id="mainMenu">.
  const menuStack: string[] = [];
  let inMainMenu = false;
  let inShortcuts = false;

  for (const tag of tags) {
    if (tag.name === 'menu') {
      if (tag.closing) {
        menuStack.pop();
        if (menuStack.length === 0)
          inMainMenu = false;
        continue;
      }
      if (tag.attrs.id === 'mainMenu') {
        inMainMenu = true;
        menuStack.length = 0;
        // The bar itself contributes no label.
        menuStack.push('');
        continue;
      }
      if (inMainMenu)
        menuStack.push(tag.attrs.label ? stripMnemonics(tag.attrs.label) : '');
      continue;
    }

    if (tag.name === 'shortcuts') {
      inShortcuts = !tag.closing;
      continue;
    }

    if (tag.name === 'shortcut' && inShortcuts) {
      const { refid, value } = tag.attrs;
      if (refid && value) {
        const list = shortcuts.get(refid) ?? [];
        list.push(tag.attrs.if ? { value, if: tag.attrs.if } : { value });
        shortcuts.set(refid, list);
      }
      continue;
    }

    if (tag.name !== 'cmd' || tag.closing)
      continue;

    // A <cmd> in menu context carries refid; in command context, id.
    if (tag.attrs.refid !== undefined) {
      // First placement wins: a command listed in two menus reads better in a
      // report as its primary home than as whichever entry came last.
      if (inMainMenu && !menuPaths.has(tag.attrs.refid)) {
        const trail = menuStack.filter(Boolean);
        if (trail.length > 0)
          menuPaths.set(tag.attrs.refid, trail.join(' > '));
      }
      continue;
    }
    if (tag.attrs.id !== undefined)
      defs.set(tag.attrs.id, tag.attrs);
  }

  const facts: Record<string, CommandFact> = {};
  const counts: Counts = { commands: defs.size, invisible: 0, menuPaths: 0, shortcuts: 0 };

  for (const [id, attrs] of defs) {
    const label = orUndefined(attrs.label);
    const buttonLabel = orUndefined(attrs.buttonLabel);
    const desc = orUndefined(attrs.desc);
    const rawMenuLabel = orUndefined(attrs.menuLabel);
    const menuLabel = rawMenuLabel === undefined ? undefined : stripMnemonics(rawMenuLabel);

    // AppCommandPaletteItem walks label, buttonLabel, desc, menuLabel and takes
    // the first non-empty one. getButtonLabel() falls back to getLabel() and
    // getMenuLabel(false) does the same, so an explicit buttonLabel="" does not
    // halt the walk -- treating empty as absent reproduces that.
    const palette = label ?? buttonLabel ?? desc ?? menuLabel ?? '';

    const menuPath = menuPaths.get(id);
    const cmdShortcuts = shortcuts.get(id) ?? [];
    const visible = attrs.visible !== 'false';

    const hazardText = [label, buttonLabel, menuLabel, desc].filter(Boolean).join(' ');
    const scannedHazard = scanned.get(id);
    const hazards = Array.from(new Set([
      ...describedHazards(hazardText),
      ...(scannedHazard ? [scannedHazard] : []),
      ...(EXTRA_HAZARDS[id] ?? []),
    ])).sort();

    if (!visible) counts.invisible++;
    if (menuPath) counts.menuPaths++;
    if (cmdShortcuts.length > 0) counts.shortcuts++;

    facts[id] = {
      labels: { label, buttonLabel, menuLabel, desc, palette },
      menuPath,
      shortcuts: cmdShortcuts,
      visible,
      hazards,
    };
  }

  return { facts, counts };
}

// ---------------------------------------------------------------------------
// Emit
// ---------------------------------------------------------------------------

function emit(parsed: Parsed, md5: string): string {
  const lines: string[] = [
    '/**',
    ' * GENERATED FILE. Do not edit by hand.',
    ' *',
    " * Produced by utils/loki/generate-command-facts.ts ('npm run loki:facts')",
    ' * from src/gwt/.../workbench/commands/Commands.cmd.xml.',
    ' *',
    ' * Regenerate from a checkout that matches the RStudio build under test.',
    ' * When the two disagree, the build\'s extra commands are unknown here and',
    ' * Agent Loki blocks them; the report says how many, rather than warning.',
    ' */',
    '',
    "import type { CommandFact, Hazard } from './generate-command-facts';",
    '',
    'export type { CommandFact, Hazard };',
    '',
    '/** MD5 of the Commands.cmd.xml these facts were read from. */',
    `export const COMMANDS_XML_MD5 = '${md5}';`,
    '',
    'export const COMMAND_FACTS: Record<string, CommandFact> = {',
  ];

  for (const id of Object.keys(parsed.facts).sort()) {
    const fact = parsed.facts[id];
    lines.push(`  ${JSON.stringify(id)}: {`);

    const labelParts: string[] = [];
    for (const key of ['label', 'buttonLabel', 'menuLabel', 'desc'] as const) {
      const value = fact.labels[key];
      if (value !== undefined)
        labelParts.push(`${key}: ${JSON.stringify(value)}`);
    }
    labelParts.push(`palette: ${JSON.stringify(fact.labels.palette)}`);
    lines.push(`    labels: { ${labelParts.join(', ')} },`);

    if (fact.menuPath !== undefined)
      lines.push(`    menuPath: ${JSON.stringify(fact.menuPath)},`);

    const rendered = fact.shortcuts.map(s => (
      s.if === undefined
        ? `{ value: ${JSON.stringify(s.value)} }`
        : `{ value: ${JSON.stringify(s.value)}, if: ${JSON.stringify(s.if)} }`
    ));
    lines.push(`    shortcuts: [${rendered.join(', ')}],`);
    lines.push(`    visible: ${fact.visible},`);
    lines.push(`    hazards: [${fact.hazards.map(h => `'${h}'`).join(', ')}],`);
    lines.push('  },');
  }

  lines.push('};');
  lines.push('');
  return lines.join('\n');
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------

function main(): void {
  const xml = readFileSync(COMMANDS_XML, 'utf8');
  const md5 = createHash('md5').update(xml).digest('hex');

  // Two passes: the handler scan needs the command-id set to tell command
  // handlers apart from any other method named on<Something>, and the ids come
  // from the XML. The first pass is cheap next to reading the Java sources.
  const ids = new Set(Object.keys(parse(xml, new Map()).facts));
  const findings = scanExternalCommands(ids);
  const scanned = new Map(findings.map(f => [f.commandId, f.kind as Hazard]));

  const parsed = parse(xml, scanned);
  const { counts } = parsed;

  const shortfalls = (Object.keys(MINIMUMS) as (keyof Counts)[])
    .filter(key => counts[key] < MINIMUMS[key])
    .map(key => `${key}: got ${counts[key]}, expected at least ${MINIMUMS[key]}`);
  if (shortfalls.length > 0) {
    throw new Error(
      'Command facts came up thin, so the parser has probably stopped matching '
      + `Commands.cmd.xml:\n  ${shortfalls.join('\n  ')}`,
    );
  }

  const hazardCounts = new Map<Hazard, number>();
  for (const fact of Object.values(parsed.facts))
    for (const hazard of fact.hazards)
      hazardCounts.set(hazard, (hazardCounts.get(hazard) ?? 0) + 1);

  writeFileSync(OUTPUT, emit(parsed, md5), 'utf8');

  console.log(`Wrote ${path.relative(process.cwd(), OUTPUT)}`);
  console.log(`  commands:   ${counts.commands} (${counts.invisible} invisible)`);
  console.log(`  menu paths: ${counts.menuPaths}`);
  console.log(`  shortcuts:  ${counts.shortcuts}`);
  console.log(`  xml md5:    ${md5}`);
  console.log(`  handler scan: ${findings.length} commands leave the application`);
  console.log('  hazards:');
  for (const [hazard, count] of Array.from(hazardCounts).sort())
    console.log(`    ${hazard}: ${count}`);
}

if (require.main === module)
  main();
