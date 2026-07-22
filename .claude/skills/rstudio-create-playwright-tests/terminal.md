# Terminal pane patterns

Read this when driving the terminal pane (`tests/panes/terminal/`).

## Don't use `terminalBuffer()` for negative assertions

After an in-line edit (e.g. Shift+Backspace), readline may redraw the whole
prompt line instead of erasing in place -- shell- and prompt-width-dependent,
so it varies per CI runner. The stale pre-edit image stays in
`rstudioapi::terminalBuffer()` and `!grepl(...)` fails even though the edit
worked.

Verify keystrokes through a filesystem side effect instead (condensed from
`tests/panes/terminal/terminal.test.ts`; `captureResult` is a file-local
helper there -- copy the pattern, it isn't exported):

```typescript
await page.keyboard.type('touch term_bs_test.txtQ');
await page.keyboard.press('Shift+Backspace');
await page.keyboard.press('Enter');
await expect.poll(
  () => captureResult(page,
    '{ file.exists("term_bs_test.txt") && !file.exists("term_bs_test.txtQ") }'),
  { timeout: TIMEOUTS.consoleReady },
).toBe('TRUE');
```

Design the pair so no-effect and over-deletion both fail.

## Don't assert contiguous substrings of terminal echo

xterm hard-wraps echoed commands at an arbitrary column -- even mid-word --
depending on prompt length. If you must compare echoed text, strip all
whitespace and U+00B7 wrap markers from both sides first.
