# ANSI / Control Code Support in the RStudio Console

Last Updated: 2026-05-29

This summarizes how the RStudio Console's `VirtualConsole` and `AnsiCode`
classes handle terminal control sequences.

- Frontend parser: `src/gwt/src/org/rstudio/core/client/VirtualConsole.java`
- SGR / color logic: `src/gwt/src/org/rstudio/core/client/AnsiCode.java`

The branch `bugfix/17768-multi-progress-bars` (PR #17786) adds line-aware
cursor movement: **CSI A/B (up/down)**, **CSI E/F (next/previous line)**, and
**CSI G (cursor horizontal absolute)**. It also changes the horizontal moves
**CSI C/D and backspace** so they no longer cross line boundaries (see
[Behavior change](#behavior-change-horizontal-moves-stay-within-the-line)).
Items not marked "New on this branch" predate it.

ANSI handling is gated by the `ansi_console_mode` user preference
(`on` / `off` / `strip`). When `off`, only the plain control characters
`\r \b \f \n` are interpreted and escape sequences are passed through; when
`strip`, SGR styling is removed rather than applied.

---

## Supported control characters

| Char | Name | Behavior |
|------|------|----------|
| `\r` (CR) | Carriage return | Moves cursor to start of current line |
| `\b` (BS) | Backspace | Moves cursor back one column, clamped to the start of the current line (no longer crosses the preceding newline) |
| `\n` (LF) | Line feed / newline | Moves to / inserts a new line |
| `\f` (FF) | Form feed | Clears the entire console |

## Supported CSI sequences (`ESC [ ... <letter>`)

| Sequence | Name | Status | Behavior |
|----------|------|--------|----------|
| `CSI n m` | SGR (Select Graphic Rendition) | Supported | Colors / font styling (see SGR table) |
| `CSI n A` | CUU (Cursor Up) | **New on this branch** | Move cursor up n rows, column preserved/clamped |
| `CSI n B` | CUD (Cursor Down) | **New on this branch** | Move cursor down n rows, column preserved/clamped |
| `CSI n C` | CUF (Cursor Forward) | Supported | Move cursor right n columns, clamped to the end of the current line |
| `CSI n D` | CUB (Cursor Back) | Supported | Move cursor left n columns, clamped to the start of the current line |
| `CSI n E` | CNL (Cursor Next Line) | **New on this branch** | Move to column 0 of the line n rows down (clamped to last row) |
| `CSI n F` | CPL (Cursor Previous Line) | **New on this branch** | Move to column 0 of the line n rows up (clamped to first row) |
| `CSI n G` | CHA (Cursor Horizontal Absolute) | **New on this branch** | Move to column n (1-based) on the current line, clamped to line length |
| `CSI n K` | EL (Erase in Line) | Supported | Modes 0 (to EOL), 1 (to BOL), 2 (whole line) |

## Supported SGR parameters (`CSI n m`)

| Code(s) | Meaning | Notes |
|---------|---------|-------|
| `0` | Reset all attributes | |
| `1` | Bold | |
| `2` | Faint / dim | |
| `22` | Bold/faint off | |
| `3` | Italic | |
| `23` | Italic off | |
| `4` | Underline | |
| `24` | Underline off | |
| `5` | Blink (slow) | Rendered as a single blink style |
| `6` | Blink (fast) | Rendered as a single blink style |
| `25` | Blink off | |
| `7` | Inverse / reverse video | Swaps fg/bg |
| `27` | Inverse off | |
| `8` | Hidden / conceal | |
| `28` | Hidden off | |
| `9` | Strikethrough | |
| `29` | Strikethrough off | |
| `30`-`37` | Foreground color (8 standard) | |
| `40`-`47` | Background color (8 standard) | |
| `90`-`97` | Bright foreground color | |
| `100`-`107` | Bright background color | |
| `38;5;n` | 256-color foreground | Index colors supported |
| `48;5;n` | 256-color background | Index colors supported |
| `38;2;r;g;b` | 24-bit RGB foreground | **Parsed but ignored** (not rendered) |
| `48;2;r;g;b` | 24-bit RGB background | **Parsed but ignored** (not rendered) |
| `39` | Default foreground | |
| `49` | Default background | |
| `10` | Default (primary) font | |
| `11`-`18` | Alternate fonts | Treated only as "turn off font 19" |
| `19` | Font 19 | RStudio-specific: reduces line spacing |

## Supported non-CSI / RStudio-custom escapes

| Sequence | Meaning | Behavior |
|----------|---------|----------|
| `ESC ] 8 ; params ; url BEL` (OSC 8) | Hyperlink start/end | Renders clickable links |
| `ESC \` (ST, String Terminator) | Terminator | Consumed / skipped |
| `ESC G n ;` ... `ESC g` | RStudio group span (error/warning/message/agent) | Custom output grouping |
| `ESC H n ;` ... `ESC h` | RStudio highlight span (error/warning/message) | Custom condition highlighting |

---

## Behavior change: horizontal moves stay within the line

Before this branch, `CSI C` (right), `CSI D` (left), and backspace operated as
plain arithmetic on the flat buffer offset, so moving past the edge of a line
would slide across the embedded `\n` onto an adjacent line -- a horizontal move
effectively travelling vertically. That never matches a real terminal.

They are now clamped to the current line: left/backspace stop at the start of
the line, right stops at the end. Code that used "cursor left N" or backspace
to climb to a previous line will no longer do so and should use `CSI A` (up) or
`CSI F` (previous line). This also keeps the cursor from landing on a foreign
line's `\n`, which the line-sensitive operations (`\r`, `CSI K`) would otherwise
misattribute to the following line.

---

## Commonly used ANSI codes that are NOT supported

These are recognized by the generic ANSI regex and **silently discarded**
(the bytes are consumed so they don't appear as garbage, but they have no
effect). Tab and BEL are not matched at all and pass through as literal
characters.

| Sequence | Name | Notes |
|----------|------|-------|
| `\t` (TAB) | Horizontal tab | Not in the control-char set; emitted literally |
| `\a` / `BEL` (`\007`) | Bell | Not interpreted; passes through |
| `CSI r ; c H` | CUP (Cursor Position) | Discarded - no absolute positioning |
| `CSI r ; c f` | HVP (Horizontal/Vertical Position) | Discarded |
| `CSI n J` | ED (Erase in Display) | Discarded - cannot clear screen regions |
| `CSI n S` | SU (Scroll Up) | Discarded |
| `CSI n T` | SD (Scroll Down) | Discarded |
| `CSI s` / `CSI u` | Save / Restore cursor position | Discarded |
| `CSI n @` | ICH (Insert Character) | Discarded |
| `CSI n P` | DCH (Delete Character) | Discarded |
| `CSI n X` | ECH (Erase Character) | Discarded |
| `CSI n L` | IL (Insert Line) | Discarded |
| `CSI n M` | DL (Delete Line) | Discarded |
| `CSI ? 25 h` / `CSI ? 25 l` | Show / Hide cursor | Discarded |
| `CSI ? 1049 h/l` | Alternate screen buffer | Discarded |
| `CSI n h` / `CSI n l` | SM / RM (Set / Reset mode) | Discarded |
| `CSI n n` | DSR (Device Status Report) | Discarded |
| `ESC c` | RIS (Reset to Initial State) | Discarded |
| `ESC 7` / `ESC 8` | DECSC / DECRC (save/restore cursor) | Discarded |
| `ESC ( B` etc. | Designate character set | Discarded |
| SGR `53` / `55` | Overline on/off | Discarded |

### Notable gaps

Everything below describes the **Console pane** (`VirtualConsole`). The
separate **Terminal pane** is built on xterm.js (`TerminalSession extends
XTermWidget`) and is a full terminal emulator, so it supports absolute cursor
positioning, erase-in-display, alternate screen buffer, etc.

- **No 2-D absolute cursor positioning** (`CUP`/`HVP`): the console can move
  within a line (`CHA`/`CSI G` is now supported) and between lines (`CSI A/B/E/F`),
  but cannot set an absolute (row, column). Full-screen TUIs relying on
  `ESC[r;cH` will not render correctly.
- **No Erase in Display** (`CSI J`): only single-line erase (`CSI K`) is
  available; `\f` is the only way to clear everything.
- **No 24-bit truecolor**: RGB SGR sequences are parsed to keep the stream in
  sync but produce no color; only the 256-index palette is rendered.
- **No alternate screen buffer / scroll-region** support, so programs that
  expect a real terminal (e.g. `htop`, full-screen pagers) are not supported.
