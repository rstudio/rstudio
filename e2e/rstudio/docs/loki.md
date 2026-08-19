# Agent Loki

Agent Loki drives RStudio through random actions looking for crashes, and writes
up what it finds as a bug report someone can act on.

It is named for the trickster, and the name is the job description: it makes
mischief on purpose. The useful part is not the mischief. It is that Agent Loki
can tell you exactly what it did.

## The promise

**Every crash Agent Loki reports comes with a numbered list of things to do in
RStudio, in English, that a person with no checkout of this repository can follow
to see the crash for themselves.**

Not a test file name. Not a command line. Not "re-run with this seed". Not a log
to replay. If a list of that kind cannot be produced for a crash, Agent Loki
prints no steps at all and files the crash as a lead.

This is a reproduction:

> 1. Press Cmd+Shift+P to open the Command Palette, type "Create a new R
>    presentation", and click "Create a new R presentation"
> 2. Press Escape to close the Save File dialog
> 3. Press Cmd+Shift+P, type "Present from Beginning", and click "Present from
>    Beginning"

None of these are, and none of them may ever appear in place of the steps:

| Not a reproduction | Why it fails |
|---|---|
| "Run `tests/loki/loki.test.ts`" | A file name. The reader has no checkout |
| "`npx playwright test tests/loki`" | A command line, not an action |
| "Re-run with `PW_LOKI_SEED=382495775`" | Tells them how to gamble, not what to do |
| "Replay `loki-actions.jsonl`" | An artifact reference |
| "Dispatch `presentation2PresentFromBeginning`" | An internal command id |
| "Click `#rstudio_tb_newrpresentationdoc`" | A CSS selector |
| "`page.keyboard.press('Escape')`" | Test-framework vocabulary |

### Why the promise is this emphatic

An earlier attempt at this tool worked, in the sense that it found a real crash
in fifty actions. Asked for reproduction steps, it had nothing to offer but nine
command ids and no record of what had been on screen. Instead of saying so, it
invented a nine-step story, and the story contradicted itself: it had the user
editing a document after dismissing the dialog that would have created it.

A crash nobody can reproduce is not worth reporting. A set of steps that merely
looks plausible is worse than none, because someone will spend an afternoon on
it. So the promise is enforced in code, at three points, rather than left to
anyone's judgement.

## How the promise is kept

### 1. Every action goes through the real interface

`window.rstudio`, the automation bridge, is an **instrument, never an actor**. It
reads which commands are enabled, counts dialogs, and collects exceptions. It
never performs an action that could produce a finding.

Every action is a real key press, a real piece of typed text, or a real click on
a visible element. That costs a few seconds per action instead of a few
milliseconds, and it means commands with no route through the interface never get
fuzzed at all. Both are accepted, because the alternative is that every step
becomes a translation: "the tool dispatched `presentation2PresentFromBeginning`,
and we claim a person would have chosen Present from Beginning". Translating
after the fact is exactly where the invented story came from.

On Desktop the main menu bar is native, so nothing in it is in the page:
`DesktopMenuCallback` hands the menu structure to Electron, which builds an
operating-system menu. The **Command Palette** is the way in. It lists every
visible command, is identical on Desktop and Server, and is itself a route a
person uses.

### 2. Each step is written down as it is performed

Every action is recorded twice, at the moment it happens:

- **`do`**, the sentence a person reads, built only from what was on the live
  page at the moment of acting. For a palette command that is the shortcut
  pressed, the text typed, and the row's label read from the page immediately
  before clicking it.
- **`machine`**, the executable record: the route, the command id, the typed
  text, the keys.

They are two fields of one performed action, so they cannot drift apart. Each
action also records what was on screen before it: the frontmost dialog, the
active document, the selected tabs, the page address. Steps are meaningless
without the state they acted on.

Nothing is ever reconstructed later from a log.

### 3. It will not click a button it cannot name

If an element has no readable accessible name, Agent Loki does not click it. The
action is logged as skipped, with `unnameable element` as the reason.

An action the tool cannot describe is an action it does not take. That is the
code-level form of "nothing left to invent": the gap between what was done and
what can be said never opens in the first place.

### 4. Steps are shrunk, then replayed to prove they work

A crash found at action 96 of 150 is not a bug report. So each crash worth
reporting goes through:

1. **Tail search.** Replay the last 1, then 2, 4, 8, 16 actions, stopping at the
   first length that raises the same crash.
2. **Trim.** Drop actions from that window one at a time, keeping each drop that
   still reproduces.
3. **Verify.** From a clean start, perform *exactly the steps that are about to
   be printed*, and require the same crash again.

Only after the third stage does a finding become `verified` and get numbered
steps. Because the printed sentences and the replayed records are two views of
the same actions, a verified finding's steps are proven rather than asserted.

A crash that survives none of this is reported honestly:

| Status | What it means | Steps printed |
|---|---|---|
| `verified` | The printed steps raised it again, on their own, from a clean start | Yes |
| `reproduced-full-log` | Only the whole run raises it | No |
| `not-reproduced` | Nothing raised it again | No |

A run can also end because Agent Loki itself broke, which is reported as
`tool-error` and fails the test. It is named separately on purpose: an earlier
version died on one action and reported the run as having completed its whole
budget.

The clean start between candidates is the suite's own reset, not a fresh
process: a relaunch costs 30 to 60 seconds and minimisation tries a dozen
candidates. That is weaker isolation, and it is a real limitation. A crash that
depends on something a reset does not clear will be reported as a lead rather
than silently attributed to a short recipe.

## What it looks for, and what it does not

Agent Loki detects **crashes**: uncaught client exceptions, and a session that
stops responding.

It has no idea whether anything is *correct*. A plot drawn from the wrong data, a
preference that fails to stick, a wrong number in a table: all invisible to it,
on purpose. One kind of finding, findable by construction, keeps the reproduction
promise absolute.

## The two ways a crash shows up

Agent Loki watches for crashes in two places, and it needs both.

**Recorded exceptions.** The automation agent hooks GWT's uncaught-exception
handler, so anything that escapes to it lands in `window.rstudio.errors`. Crashes
that surface from a later callback, an RPC response, or a deferred command arrive
this way.

**The "Command Execution Failed" dialog.** This one is easy to miss and was found
only by pointing the tool at a crash that was certain to happen. The relevant code
is `AppCommandPaletteItem.invoke`:

```java
try { command_.execute(); }
catch (Exception e) { display.showErrorMessage("Command Execution Failed", ...); }
```

A command handler that throws **synchronously** is therefore caught by the
Command Palette itself. It never reaches GWT's uncaught handler and never reaches
the recorded exceptions, so a tool watching only those would report a clean run
while RStudio put an error dialog on screen. The dialog is the product's own
report of that crash, carrying the command's label and the exception's message, so
Agent Loki reads it and reports it like any other crash.

Two consequences worth knowing:

- The swallowing is specific to the palette. A click on a toolbar button goes
  through GWT's ordinary event dispatch, where an exception does reach the
  uncaught handler. When the click route lands, the same crash may arrive by the
  other path.
- The dialog's message is parsed against the wording in `PaletteConstants`, not
  scraped whole. The dialog's text also carries the button label and, on some
  builds, a trailing "screen reader mode not enabled" warning; using all of it as
  the crash message made the signature depend on that incidental text, so the same
  crash hashed differently on replay and every finding came out unreproducible.

## What it is not allowed to touch

535 of RStudio's 670 commands are fuzzable. The other 135 are blocked, in two
layers.

**Hazards** are read out of the source by `npm run loki:facts`, which walks
`Commands.cmd.xml` and then reads the Java handler for every command:

| Hazard | Meaning |
|---|---|
| `external-browser` | Hands a URL to the desktop browser |
| `external-application` | Opens a file or folder in Finder, Word, a PDF viewer |
| `navigates-away` | Replaces the page, destroying the bridge with it |
| `os-print-dialog` | Can raise the operating-system print sheet, which cannot be closed |
| `session-lifecycle` | Ends or restarts the session |
| `deliberate-crash` | Exists in order to crash |

Reading the handlers is what makes this work. Ten cheat-sheet commands open an
external browser and say nothing about it in their description; the eleventh
mentions a browser and is the only one a name-based rule catches. Conversely
`shinyRunInBrowser` says so plainly but reaches the browser by a route the
handler scan does not follow. Both signals are kept, and their union is used.

**Rules** cover intent, where nothing in the handler is technically hazardous:
`off-machine` (a package repository, an account, a remote branch),
`substrate-destruction` (files, version-control state, history),
`shell-execution`, `long-toolchain`, `harness-integrity`, `deliberate-crash`.

Anything the generated facts have never heard of is **blocked as unclassified**.
That is how a run against a newer build stays safe: the commands it does not
recognise are exactly the ones it must not touch. The report says how many, which
is information rather than a fault.

### File dialogs are a target, not a trap

`fixtures/base-prefs.jsonc` sets `native_file_dialogs: false`, so every Desktop
worker in this suite gets web dialogs in the page rather than the operating
system's. `newRPresentationDoc`, `openSourceDoc`, `saveSourceDocAs`, the
`importDatasetFrom*` family and `exportFiles` are therefore all fuzzable, and
their dialogs are among the richest things to fuzz. An earlier version blocked the
whole family on the strength of their names.

Printing is **not** covered by that preference. `AceEditor.print()` takes the
non-desktop-frame branch on Electron and prints through the browser, which can
raise the operating system's print sheet, and Playwright cannot close it. The
print commands stay blocked.

Because the run diverges from a default install here, every finding records the
preferences it depended on and the rendered steps spell them out in the words on
the checkboxes.

## Getting unstuck

Agent Loki puts RStudio into states nobody designed for, so it regularly ends up
somewhere it cannot act. The commonest case: an action opens a modal dialog, and
from then on the Command Palette shortcut goes nowhere, because a GWT modal throws
a glass panel over the page and swallows the keystroke.

That failure is silent, which is what makes it dangerous. In an early 25-action
run, action 2 opened the Import Dataset dialog and the remaining 23 all reported
"the Command Palette did not open". The run ended as a clean, complete budget
having invoked two commands. Nothing failed. The tool had simply stopped doing
anything.

So consecutive actions that cannot be performed escalate through a ladder, from
cheapest to most destructive: press Escape and dismiss blocking dialogs, then
dismiss every dialog and end any pane zoom, then close the open project, then
reset the session. A run that comes out the far end ends as `wedged` rather than
counting empty actions toward its budget. The first rung fires after a single
failure, because a dialog is the usual cause and Escape costs nothing.

Recovery may use the automation bridge freely. The rule that the bridge is never
an actor is about actions that could produce a finding, and recovery cannot:
nothing it does is recorded as a step, and any exception it stirs up becomes a
lead with no steps attached.

### How much of a run actually does something

Roughly two thirds, measured on a 30-action Desktop run: 19 actions performed, 6
skipped because the palette showed the command as unavailable, 5 skipped because a
dialog was holding the interface.

Both kinds of skip are worth understanding rather than tuning away.

The "unavailable" ones come from a gap the bridge cannot close.
`AppCommandPaletteEntry.enabled()` is `isEnabled() && hasCommandHandlers()`, and
the second half is invisible from JavaScript, so a command can report itself
enabled through `window.rstudio` while its palette row renders
`aria-disabled="true"`. `popoutChat` does this when the chat pane has not been
built. Agent Loki reads the row's own state and declines, which costs an action;
clicking anyway would just earn a "Command Disabled" dialog.

The "dialog was holding the interface" ones cost one action each, after which
recovery clears the dialog. That is the price of not having a dialog route yet.

## Running it

```bash
npm run test:loki
```

```bash
npm run test:loki-server
```

Both set `PW_RUN_LOKI=1`, which is what includes the `@loki` tag. Without it the
suite is excluded from every run.

Useful settings, all documented in the environment table in `README.md`:
`PW_LOKI_STEPS`, `PW_LOKI_MINUTES`, `PW_LOKI_PACE_MS`, `PW_LOKI_MAX_MINIMIZE`,
`PW_LOKI_MINIMIZE_MINUTES`, and `PW_LOKI_SEED`.

`PW_LOKI_SEED` makes a run repeat its choices, which is useful while working on
the tool itself. It is **not** a reproduction and never appears in a report as
one.

### Reading the output

Three files are attached to the test result:

| File | What it holds |
|---|---|
| `loki-findings.md` | The human report. Verified crashes with numbered steps; everything else under a separate heading with its caveat |
| `loki-report.json` | The same run as data: policy counts, coverage, every finding and lead |
| `loki-actions.jsonl` | One line per action, with the sentence, the machine record, and the screen state before it |

Findings do not fail the test. What fails it is Agent Loki failing to describe
its own behaviour, or ending a run against a session that had already gone away.

### Regenerating the command facts

```bash
npm run loki:facts
```

Run this from a checkout that **matches the RStudio build under test**. When they
disagree, the build's extra commands are unknown to the facts and get blocked,
and the report records the count along with the build version and the checksum of
the `Commands.cmd.xml` the facts came from.

The generator checks its own output against floors for how many commands, menu
paths and shortcuts it expects to find, so a parser that quietly stopped matching
the file fails loudly instead of producing a thin, permissive result.

## Triage

1. Open `loki-findings.md` and read the **Reproducible crashes** section. Those
   steps have been replayed and hold up on their own. They are ready to paste
   into an issue, along with the preconditions the report states.
2. Check the preconditions before filing. A crash reached through a web file
   dialog may not reproduce on a default install, and the report says so.
3. Treat everything under **Leads** as a starting point, not a bug report. It
   carries no steps because none were proven.
4. **Exceptions with no attributable action** happened between actions or during
   recovery. There is no action to write down, so there never will be steps.

## Files

| File | Contents |
|---|---|
| `tests/loki/loki.test.ts` | The main loop: observe, act, settle, attribute; then minimise and verify |
| `tests/loki/loki_report.test.ts` | Checks needing no running RStudio: the step lint, signatures, policy, renderer |
| `tests/loki/loki_selftest.test.ts` | Checks against a live session, including the fabrication tripwire |
| `utils/loki/executor.ts` | Performing actions through the interface, returning the sentence and the record together |
| `utils/loki/settle.ts` | Waiting for the interface to stop moving; noticing when the session has gone |
| `utils/loki/recovery.ts` | The ladder that gets the run unstuck when actions stop working |
| `utils/loki/replay.ts` | Replaying recorded actions, shrinking a run to a recipe, verifying it |
| `utils/loki/report.ts` | The report shape, crash signatures, the renderer, and the step lint |
| `utils/loki/policy.ts` | What may be touched: hazards from the facts, plus intent rules |
| `utils/loki/command-facts.ts` | Generated. Labels, menu paths, shortcuts, visibility, hazards |
| `utils/loki/generate-command-facts.ts` | The generator |
| `utils/loki/scan-command-handlers.ts` | Reads the Java handlers to find commands that leave the application |
| `utils/loki/element-ids.ts` | Port of `ElementIds.java`, so the tool builds the ids GWT assigns |
| `utils/loki/prng.ts` | The seeded random source |

## Known limitations

- **Only the Command Palette route is implemented.** Clicking arbitrary buttons,
  typing into the editor and console, pressing curated shortcuts, and driving
  dialog buttons are all designed for and not yet built. Until they are, coverage
  is limited to commands the palette offers, and the report's `notFuzzed` list
  shows what was never reachable.
- **Dialogs are dismissed, not explored.** With no dialog route yet, a modal is
  something recovery clears rather than something to fuzz. That is a real coverage
  gap: dialogs are among the likeliest places for a crash to live, and the plan's
  dialog route exists to close it.
- **A replay can only repeat palette actions.** When the other routes land, a
  recipe that needs one of them will report as unverified until `replay.ts`
  learns to perform it.
- **The end-to-end check for a lost session is not run.** `InterfaceWatch` is
  tested directly, but the version that navigates the live page away and asserts
  the run ends as `lost-ui` would destroy the worker's session and break every
  spec after it. The session is worker-scoped and shared.
- **Isolation between minimisation candidates is a reset, not a relaunch.**
  See above.
