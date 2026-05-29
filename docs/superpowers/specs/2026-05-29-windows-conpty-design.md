# Design: Replace winpty with ConPTY on Windows

- Date: 2026-05-29
- Status: Approved (design); plan to follow
- Area: Windows terminal PTY backend (`src/cpp/core/system`, `src/cpp/session`, `src/gwt` terminal)
- Tracking issue: winpty -> ConPTY migration (project notes reference #3798; confirm before PR)

## 1. Summary

The Windows terminal backend uses the vendored `winpty` library (a 2016-2017
era shim that drives a hidden console in a helper process, `winpty-agent.exe`,
and exposes named pipes). Windows now ships a native pseudoconsole API (ConPTY,
`CreatePseudoConsole` / `ResizePseudoConsole` / `ClosePseudoConsole`), available
since Windows 10 1809. RStudio's supported Windows floor (Windows 11 official,
updated Windows 10 unofficial) is well above that, so we can drop winpty
entirely and use ConPTY natively.

Goal: **functional parity** with today's terminal on Windows, plus one approved
extra (re-enable buffer replay for cmd/PowerShell). No new user-facing features
beyond that in this pass.

## 2. Background: current architecture

- `core::system::ProcessOptions::pseudoterminal` (`boost::optional<Pseudoterminal>`)
  is the single switch that selects the PTY path. On Windows the
  `Pseudoterminal` struct carries `winptyPath`, `plainText`, `conerr`.
- `Win32ChildProcess.cpp` constructs a `WinPty` (`core/system/Win32Pty.{hpp,cpp}`)
  when `pseudoterminal` is set. `WinPty::start` loads `winpty.dll` lazily via
  `LoadLibrary`/`GetProcAddress`, opens winpty's `conin`/`conout`/`conerr` named
  pipes, and spawns the child inside winpty's hidden console.
- `AsyncChildProcess::poll()` reads output each tick via
  `WinPty::readFromPty` (`PeekNamedPipe` + bounded `ReadFile`, non-blocking).
  Input is written synchronously via `WinPty::writeToPty`. `setSize` ->
  `winpty_set_size`. `interrupt()` is a no-op stub.
- Only consumer that requests a Windows PTY is `SessionConsoleProcess.cpp`
  (terminals). It always passes `plainText=false, conerr=false`, so winpty's
  separate-stderr channel is already unused and output is always VT.
- Ctrl+C is delivered today by writing the byte `0x03` through normal stdin
  (`writeToStdin`), not through `ptyInterrupt()`.

## 3. Scope

In scope:
- Replace `WinPty` with a native `ConPty` class; remove all winpty code, the
  vendored dependency, build wiring, and the `--external-winpty-path` option.
- Preserve terminal behavior: output stream to xterm.js, input, resize, Ctrl+C
  via `0x03`, subprocess/CWD polling, suspend/restore persistence.
- Re-enable cmd/PowerShell/pwsh buffer replay on reconnect (now safe under
  ConPTY's clean VT output).

Out of scope (deferred to a follow-up):
- Mouse mode, signal-based `ptyInterrupt()`, local-echo on Windows, pwsh
  startup/perf tuning, any feature parity with POSIX beyond today's Windows
  behavior.

## 4. Verified ConPTY API contract (authoritative)

Verified against Microsoft Learn (not prior assumptions):

- `CreatePseudoConsole(COORD size, HANDLE hInput, HANDLE hOutput, DWORD dwFlags,
  HPCON* phPC)`. `hInput` = read end of the input pipe; `hOutput` = write end of
  the output pipe. Returns `HRESULT`. Min OS: Windows 10 1809.
- The pipe handles passed to `CreatePseudoConsole` must be **synchronous**
  (no `FILE_FLAG_OVERLAPPED`). MS strongly recommends servicing each channel on
  its **own thread**.
- Output and input streams are **always UTF-8** with interleaved VT sequences,
  regardless of the child's code page. A single merged output stream (no
  separate stderr).
- Child launch sequence:
  1. `CreatePipe(&inputRead, &inputWrite, NULL, 0)` and
     `CreatePipe(&outputRead, &outputWrite, NULL, 0)`.
  2. `CreatePseudoConsole(size, inputRead, outputWrite, 0, &hPC)`.
  3. `STARTUPINFOEX si; si.StartupInfo.cb = sizeof(STARTUPINFOEX);`
     (note: `cb` is `sizeof(STARTUPINFOEX)`, not `sizeof(STARTUPINFO)`).
  4. `InitializeProcThreadAttributeList` (double-call to size then init),
     `UpdateProcThreadAttribute(list, 0, PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE,
     hPC, sizeof(hPC), NULL, NULL)`.
  5. `CreateProcessW(NULL, cmdline, NULL, NULL, FALSE /*bInheritHandles*/,
     EXTENDED_STARTUPINFO_PRESENT, env, cwd, &si.StartupInfo, &pi)`.
     - **`EXTENDED_STARTUPINFO_PRESENT` is required**; omitting it (or a wrong
       `cb`) silently makes the child inherit the parent's real console -- the
       most likely cause of the prior attempt's "output goes to the wrong
       console" / "input ignored" / "Ctrl+C ignored" failures (one root cause,
       three symptoms).
     - **Do not** set `STARTF_USESTDHANDLES` and do not set `hStdInput/Output/
       Error`; that overrides the pseudoconsole connection.
  6. After `CreateProcess`, **close the ConPTY-side ends** (`inputRead`,
     `outputWrite`) in our process so broken-pipe detection works. Keep
     `inputWrite` (we write input) and `outputRead` (we read output).
- `ResizePseudoConsole(HPCON, COORD)`. Min OS: 1809.
- Ctrl+C: rsession is not attached to the child's pseudoconsole, so
  `GenerateConsoleCtrlEvent` cannot reach it. The correct mechanism is writing
  the byte `0x03` into the input pipe; conhost raises `CTRL_C_EVENT` to the
  attached child (default `ENABLE_PROCESSED_INPUT`). This matches today's path.
- Shutdown: `ClosePseudoConsole(HPCON)` sends `CTRL_CLOSE_EVENT` to clients,
  terminates the attached child and its descendant tree, and may emit a final
  output frame. **Pre-Windows 11 24H2 it blocks until the output pipe is drained
  or closed.** Therefore: keep a reader thread draining output, and do not call
  `ClosePseudoConsole` on the reader thread.
- `ReleasePseudoConsole` is Windows 11 24H2+ only -> **not used**; we use the
  drain-then-close pattern instead.

References:
- https://learn.microsoft.com/windows/console/creating-a-pseudoconsole-session
- https://learn.microsoft.com/windows/console/createpseudoconsole
- https://learn.microsoft.com/windows/console/closepseudoconsole
- https://learn.microsoft.com/windows/console/resizepseudoconsole
- Microsoft sample: https://github.com/microsoft/terminal/tree/main/samples/ConPTY

## 5. Key decisions

1. **Dynamic symbol resolution (build-feasibility fix).** The build defines
   `-D_WIN32_WINNT=0x601` / `-DWINVER=0x601` (`src/cpp/CMakeLists.txt:151-152`).
   The ConPTY declarations (`HPCON`, the three functions, and the
   `PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE` macro) are gated in the SDK headers
   behind `NTDDI_WIN10_RS5`, so they are **not declared at 0x601** and a naive
   include + call will not compile. We mirror the existing winpty pattern:
   resolve `CreatePseudoConsole`/`ResizePseudoConsole`/`ClosePseudoConsole` from
   `kernel32.dll` via `GetProcAddress`, and self-declare the small surface we
   need (`typedef VOID* HPCON;`, function-pointer typedefs, and
   `#ifndef PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE
   #define PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE ProcThreadAttributeValue(22,FALSE,TRUE,FALSE)`).
   `STARTUPINFOEX` and the attribute-list APIs already exist at 0x601.
   - Rationale: zero change to the global Windows version target (large blast
     radius), matches the codebase's established dynamic-load idiom, and yields
     a clean "ConPTY unavailable" error path. Alternatives (per-TU or global
     `_WIN32_WINNT` bump) are recorded but not chosen for this pass.

2. **Dedicated output-reader thread.** Per MS guidance and to structurally
   eliminate the close/write deadlock, one thread does blocking `ReadFile` on
   the output pipe into a mutex-guarded buffer. `poll()` (main thread) drains
   the buffer and invokes `onStdout`, preserving the existing
   `callbacksRequireMainThread` contract. The reader thread never invokes
   callbacks.

3. **Single output stream.** Drop `hStdErrRead` on the PTY path (already always
   merged; no PTY consumer depends on split stderr -- confirmed: the only
   `Pseudoterminal` constructors are the two terminal sites in
   `SessionConsoleProcess.cpp` and the unit test).

4. **Rename `WinPty` -> `ConPty`** (replace, don't deprecate). Update the ~5
   call sites. Delete `Win32Pty.{hpp,cpp}`; add `Win32ConPty.{hpp,cpp}` (or keep
   filenames and rewrite -- decided in plan; prefer new names for clarity).

5. **Ctrl+C unchanged**: `ptyInterrupt()` stays a no-op; the `0x03`-as-stdin
   path continues to drive `CTRL_C_EVENT`.

6. **No `ReleasePseudoConsole`** (24H2-only). Shutdown uses drain-then-close.

## 6. Architecture

### 6.1 `ConPty` class (`core/system`)

Encapsulates the pseudoconsole, pipes, and reader thread so callers stay simple:

```cpp
class ConPty : boost::noncopyable
{
public:
   // Creates pipes + pseudoconsole + child; launches the output-reader thread.
   // Returns the child process handle (caller owns it, as today).
   Error start(const std::string& exe,
               const std::vector<std::string>& args,
               const ProcessOptions& options,
               HANDLE* pProcess);

   Error readOutput(std::string* pOut);          // non-blocking drain of buffer
   Error writeInput(const std::string& input);   // synchronous write to input pipe
   Error setSize(int cols, int rows);            // ResizePseudoConsole
   bool  running() const;
   void  stop();                                 // ordered teardown (6.3)
   ~ConPty();                                     // stop()

private:
   HPCON  hPC_ = nullptr;
   HANDLE hInputWrite_ = nullptr;                 // we write
   HANDLE hOutputRead_ = nullptr;                 // reader thread reads
   std::thread readerThread_;
   std::mutex bufferMutex_;
   std::string outputBuffer_;
   std::atomic<bool> outputClosed_{false};
};
```

- `start()` builds the command line/env exactly as the current winpty path does
  (reuse the cmdline-quoting and UTF-8 env-block logic), then runs the §4
  sequence. On any failure, it tears down partial state and returns a
  context-rich `Error`.
- Reader thread: loop on blocking `ReadFile(hOutputRead_, ...)`; append to
  `outputBuffer_` under `bufferMutex_`; on `ERROR_BROKEN_PIPE` (or 0-byte EOF)
  set `outputClosed_` and exit.
- `readOutput()`: swap/clear `outputBuffer_` under the mutex into `*pOut`.

### 6.2 `Win32ChildProcess` integration

- `ChildProcess::Impl`: replace `WinPty pty` with `ConPty pty`; the PTY path no
  longer uses `hStdOutRead`/`hStdErrRead` (ConPty owns its pipes). Non-PTY pipe
  path is unchanged.
- `ChildProcess::run` (PTY branch): `pImpl_->pty.start(exe_, args_, options_,
  &pImpl_->hProcess)`.
- `AsyncChildProcess::poll()`: replace the two `WinPty::readFromPty` calls with a
  single `pImpl_->pty.readOutput(&out)` -> `onStdout`. Process-exit detection
  (`WaitForSingleObject`) is unchanged; on exit, drain the final buffer and
  proceed to `onExit`.
- `writeToStdin` (PTY branch): `pImpl_->pty.writeInput(input)`.
- `ptySetSize` -> `pty.setSize`; `ptyInterrupt` -> no-op (returns Success).

### 6.3 Shutdown ordering

On child exit or terminal close (`ConPty::stop`, also called from `~ConPty`):
1. `ClosePseudoConsole(hPC_)` from the calling (main/poll) thread. The reader
   thread keeps draining the final frame, so this does not hang pre-24H2.
2. ConPTY closes its output write end -> the reader's `ReadFile` returns
   `ERROR_BROKEN_PIPE`; the thread exits.
3. `join()` the reader thread.
4. Close `hInputWrite_` and `hOutputRead_`; null `hPC_`.

Backstop for a wedged reader (e.g., child never disconnects): call
`CancelSynchronousIo(readerThreadHandle)` before join. Order is fixed so
`ClosePseudoConsole` is never called on the reader thread.

## 7. Touch-points (verified file:line)

- `src/cpp/core/system/Win32Pty.{hpp,cpp}` -> delete; new `Win32ConPty.{hpp,cpp}`.
- `src/cpp/core/system/Win32PtyTests.cpp` -> rewrite as `Win32ConPtyTests.cpp`.
- `src/cpp/core/system/Win32ChildProcess.cpp` -> integration (Impl, run, poll
  759-822/862-892, writeToStdin 253-284, ptySetSize/ptyInterrupt 286-302).
- `src/cpp/core/include/core/system/Process.hpp:65-90` -> drop `winptyPath`,
  `plainText`, `conerr` from `Pseudoterminal` (Windows).
- `src/cpp/session/SessionConsoleProcess.cpp:304-310` -> stop passing winpty
  path/flags.
- `src/cpp/session/session-options.json:874-879` -> remove `external-winpty-path`;
  `src/cpp/session/SessionOptions.cpp:321-345` -> remove resolution block;
  regenerate via `Rscript scripts/generate-options.R` (updates
  `SessionOptions.gen.hpp`). Remove `winptyPath()` usage.
- `src/cpp/CMakeLists.txt:374-388` -> remove `WINPTY_*` block.
- `src/cpp/session/CMakeLists.txt:860-876` -> remove winpty install rules (64-
  and 32-bit).
- `dependencies/windows/install-dependencies.cmd` -> remove winpty fetch;
  delete `dependencies/windows/winpty-0.4.3-msys2-2.7.0.zip`.
- `src/cpp/session/SessionConsoleProcessPersist.cpp:57` -> bump
  `kConsoleDir "console07"` -> `"console08"` with a dated changelog line
  (output now carries VT sequences).
- `src/gwt/.../terminal/TerminalSession.java:777-799` (`shellSupportsReload`) ->
  remove the `WIN_CMD`/`WIN_PS`/`PS_CORE` cases so they fall through to
  `default: return true`; re-evaluate the Windows `CUSTOM` case (likely also
  reloadable under ConPTY -- minor open point, see §11).
- `src/cpp/conf/rdesktop-dev.conf` -> remove any winpty path entry.
- `NEWS.md` -> entry under the appropriate section.

## 8. Error handling

- Wrap `HRESULT`/`GetLastError` with `systemError` and context (operation,
  exe, size). Recognize `0xC0000142` (child init failure from an invalid/closed
  pseudoconsole during startup) and surface a clear message.
- If `GetProcAddress` cannot resolve the ConPTY functions, fail `start()` with
  an actionable "ConPTY not available on this Windows version" error (should not
  occur above 1809).

## 9. Testing and verification

- **Phase 0 standalone POC** (`cl.exe`, no rsession build): spawn `cmd.exe`
  through the §4 sequence with a reader thread; assert `echo HELLO` round-trips
  on the output pipe (not the test console); resize; write `0x03` to a busy
  child (`ping -t` / busy loop) and assert it terminates; clean
  `ClosePseudoConsole` with no hang. Confirms the contract on this machine
  before any integration. (Diagnostic: a high-output child such as
  `cmd /c set` distinguishes pipe-routing failure from a flush race.)
- **C++ unit tests** (`Win32ConPtyTests.cpp`, google test): start/echo,
  writeInput round-trip, setSize, `0x03` interrupt, clean shutdown, broken-pipe
  on child exit. Run via `rstudio-tests --scope core`.
- **Playwright** `e2e/rstudio/tests/panes/terminal/terminal.test.ts` on Windows.
- **Manual matrix**: cmd, pwsh, ps-core, git-bash; vim/less (alt-buffer); Ctrl+C
  in a busy program; resize; browser reload (buffer replay incl. cmd/PS);
  rsession restart (replay from persistence).

## 10. Phasing (each independently verifiable)

0. Toolchain check + standalone POC proves the §4 contract on this machine.
1. `ConPty` class + dynamic loader + types; `Win32ConPtyTests.cpp`. Build + run
   unit tests.
2. Integrate into `Win32ChildProcess` (reader thread -> poll, writeInput,
   setSize, shutdown). Build session; smoke a terminal.
3. Session/options layer: trim `Pseudoterminal`, update `SessionConsoleProcess`,
   remove `--external-winpty-path`, regenerate options.
4. Build-system + dependency removal; delete winpty files; persistence bump.
5. Frontend buffer-replay re-enable (`TerminalSession.java`).
6. Full test pass (unit + Playwright) + manual matrix on cmd/pwsh/git-bash/TUI.
7. Cleanup, `NEWS.md`, final review.

Each phase is a commit (or small commit series) and goes through the roborev
review loop before the next phase begins.

## 11. Risks and open questions

- **32-bit session** (`RSTUDIO_SESSION_WIN32`): ConPTY works on x86; the x86
  winpty install simply goes away. Verify a 32-bit session build still links.
- **Windows `CUSTOM` shell replay**: under ConPTY a custom shell's output is
  clean VT, so replay is likely safe; current code is conservative. Decide
  whether to also return `true` for `CUSTOM` on Windows or keep it conservative.
- **Deadlock avoidance** rests on the reader thread always draining; the
  `stop()` ordering in §6.3 must be honored exactly.
- **Persistence**: bumping `kConsoleDir` discards old winpty-era buffers (fresh
  start), which is acceptable; no migration.
