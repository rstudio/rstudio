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
  Input is written **synchronously on the calling (session main) thread** via
  `WinPty::writeToPty`. `setSize` -> `winpty_set_size`. `interrupt()` is a no-op
  stub.
- Only consumer that requests a Windows PTY is `SessionConsoleProcess.cpp`
  (terminals). It always passes `plainText=false, conerr=false`, so winpty's
  separate-stderr channel is already unused and output is always VT.
- Ctrl+C is delivered today by writing the byte `0x03` through normal stdin
  (`writeToStdin`), not through `ptyInterrupt()`.
- Implicit backpressure today: when the consumer is slow, output accumulates in
  winpty's OS pipe buffer; when full, winpty's agent stops draining the hidden
  console and the child is throttled. The replacement must preserve an
  equivalent bound (see 6.1).

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
  (no `FILE_FLAG_OVERLAPPED`). MS strongly recommends servicing **each channel
  on its own thread** to avoid deadlocks; this design uses a reader thread and a
  writer thread (6.1).
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
  7. Clean up the attribute list (`DeleteProcThreadAttributeList` + free) once
     `CreateProcess` returns.
- `ResizePseudoConsole(HPCON, COORD)`. Min OS: 1809.
- Ctrl+C: rsession is not attached to the child's pseudoconsole, so
  `GenerateConsoleCtrlEvent` cannot reach it. The correct mechanism is writing
  the byte `0x03` into the input pipe; conhost raises `CTRL_C_EVENT` to the
  attached child (default `ENABLE_PROCESSED_INPUT`). This matches today's path.
- Shutdown: `ClosePseudoConsole(HPCON)` sends `CTRL_CLOSE_EVENT` to clients,
  terminates the attached child and its descendant tree, and may emit a final
  output frame. **Pre-Windows 11 24H2 it blocks until the output pipe is drained
  or closed.** Therefore: keep the reader thread draining output, and do not
  call `ClosePseudoConsole` on the reader thread (6.3).
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

2. **Dedicated reader and writer threads + bounded output buffer.** Per MS
   guidance and to structurally avoid the close/write deadlock:
   - A reader thread does blocking `ReadFile` on the output pipe into a
     **bounded** mutex-guarded buffer; `poll()` (main thread) drains it and
     invokes `onStdout` (preserving `callbacksRequireMainThread`).
   - A writer thread drains an input queue with blocking `WriteFile` to the
     input pipe, so a large paste or a stalled child never blocks the session
     main thread (winpty buffered input in its agent; ConPTY's ~4KB pipe does
     not, so input must move off the main thread to avoid a regression).
   - The bounded output buffer applies **backpressure**: when it reaches a
     high-water mark the reader pauses until `poll()` drains below a low-water
     mark, which lets the OS output pipe fill and ConPTY throttle the child --
     equivalent to winpty's implicit backpressure, and bounding memory when the
     consumer is slow/suspended. This is deadlock-free because `poll()` runs on
     the session loop independently of the frontend and always drains into the
     downstream capped circular buffer (6.1).

3. **Single output stream.** Drop `hStdErrRead` on the PTY path (already always
   merged; no PTY consumer depends on split stderr -- confirmed: the only
   `Pseudoterminal` constructors are the two terminal sites in
   `SessionConsoleProcess.cpp` and the unit test).

4. **Rename `WinPty` -> `ConPty`** (replace, don't deprecate). Update the ~5
   call sites. Delete `Win32Pty.{hpp,cpp}`; add `Win32ConPty.{hpp,cpp}`.

5. **Ctrl+C unchanged**: `ptyInterrupt()` stays a no-op; the `0x03`-as-stdin
   path continues to drive `CTRL_C_EVENT`.

6. **No `ReleasePseudoConsole`** (24H2-only). Shutdown uses drain-then-close.

## 6. Architecture

### 6.1 `ConPty` class (`core/system`)

Encapsulates the pseudoconsole, pipes, and the reader/writer threads so callers
stay simple:

```cpp
class ConPty : boost::noncopyable
{
public:
   // Creates pipes + pseudoconsole + child; launches reader and writer threads.
   // Returns the child process handle (caller owns it, as today).
   Error start(const std::string& exe,
               const std::vector<std::string>& args,
               const ProcessOptions& options,
               HANDLE* pProcess);

   Error readOutput(std::string* pOut);          // non-blocking drain of buffer
   Error writeInput(const std::string& input);   // enqueue; writer thread sends
   Error setSize(int cols, int rows);            // ResizePseudoConsole (guarded)
   bool  running() const;
   void  stop();                                 // ordered teardown (6.3)
   ~ConPty();                                     // stop()

private:
   HPCON  hPC_ = nullptr;
   HANDLE hInputWrite_ = nullptr;                 // writer thread writes
   HANDLE hOutputRead_ = nullptr;                 // reader thread reads
   std::thread readerThread_;
   std::thread writerThread_;

   std::mutex stateMutex_;                        // guards lifecycle/stopped_
   bool stopped_ = false;

   std::mutex outMutex_;
   std::condition_variable outCv_;                // reader<->poll backpressure
   std::string outputBuffer_;                     // bounded by kMaxBuffer
   bool outputClosed_ = false;

   std::mutex inMutex_;
   std::condition_variable inCv_;
   std::deque<std::string> inputQueue_;           // drained by writer thread
   bool inputDone_ = false;
};
```

- `start()` builds the command line/env exactly as the current winpty path does
  (reuse the cmdline-quoting and UTF-8 env-block logic), then runs the 4
  sequence. **Each step that allocates is protected by a scope guard** so a
  failure at any point closes the pipes, deletes the attribute list, and closes
  the pseudoconsole before returning a context-rich `Error` (no leaks on partial
  startup).
- Reader thread: blocking `ReadFile(hOutputRead_, ...)`; append under
  `outMutex_`. If `outputBuffer_.size()` exceeds the high-water mark, wait on
  `outCv_` until `poll()` drains below the low-water mark (backpressure). On
  `ERROR_BROKEN_PIPE`/EOF set `outputClosed_` and exit.
- `readOutput()`: swap `outputBuffer_` into `*pOut` under `outMutex_`, then
  `notify` `outCv_` so a paused reader resumes.
- Writer thread: wait on `inCv_` for queued input or `inputDone_`; blocking
  `WriteFile(hInputWrite_, ...)`. On `inputDone_` with an empty queue, exit.
- `writeInput()`: enqueue under `inMutex_` + notify (no main-thread blocking).
- `setSize()`/`writeInput()` are no-ops returning an error once `stopped_`.

### 6.2 `Win32ChildProcess` integration

- `ChildProcess::Impl`: replace `WinPty pty` with `ConPty pty`; the PTY path no
  longer uses `hStdOutRead`/`hStdErrRead` (ConPty owns its pipes). Non-PTY pipe
  path is unchanged.
- `ChildProcess::run` (PTY branch): `pImpl_->pty.start(exe_, args_, options_,
  &pImpl_->hProcess)`.
- `AsyncChildProcess::poll()`: replace the two `WinPty::readFromPty` calls with a
  single `pImpl_->pty.readOutput(&out)` -> `onStdout`.
- **On process exit**: run the ordered teardown (`pty.stop()`, which joins the
  reader after draining ConPTY's final frame), then **drain `readOutput()` one
  final time and dispatch it to `onStdout`**, and only then close the process
  handle and call `onExit`. This guarantees the last command output/prompt is
  not lost.
- `writeToStdin` (PTY branch): `pImpl_->pty.writeInput(input)`.
- `ptySetSize` -> `pty.setSize`; `ptyInterrupt` -> no-op (returns Success).

### 6.3 Shutdown ordering (bounded, deadlock-free)

`ConPty::stop()` (idempotent; also called from `~ConPty`), under `stateMutex_`
to serialize with `writeInput`/`setSize`:
1. Set `stopped_`. Signal `inputDone_` and `notify` `inCv_`; to unblock a writer
   stuck in `WriteFile`, `CancelSynchronousIo(writerThread_)` then `join()` it.
   Queued-but-unsent input is abandoned (the terminal is closing).
2. Run `ClosePseudoConsole(hPC_)` on a **separate closer thread** (never the
   reader). The reader keeps draining output, so the close returns promptly even
   pre-24H2.
3. Wait for the closer with a bounded timeout. If it returns, the output pipe is
   broken and the reader's `ReadFile` returns `ERROR_BROKEN_PIPE`. If it exceeds
   the timeout, `CancelSynchronousIo(readerThread_)` and/or close `hOutputRead_`
   to force the reader out and unblock the close.
4. `join()` the reader (and closer). Drain any final bytes (consumed by 6.2's
   final `readOutput`). Close `hInputWrite_`/`hOutputRead_`; null `hPC_`.

### 6.4 Concurrency / lifecycle guards

- `stateMutex_` serializes `start`/`stop`/`writeInput`/`setSize` so they cannot
  race during close. After `stopped_`, `writeInput`/`setSize` return an error
  and `setSize` never calls `ResizePseudoConsole` on a closed `HPCON`.
- The reader/writer threads touch only their own buffers + the raw handles; the
  handles are closed only after both threads are joined.

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
  (output now carries VT sequences). **Done in the integration phase (Phase 2),
  so the format change coincides with ConPTY going live.**
- `src/gwt/.../terminal/TerminalSession.java:777-799` (`shellSupportsReload`) ->
  remove the `WIN_CMD`/`WIN_PS`/`PS_CORE` cases so they fall through to
  `default: return true`; default the Windows `CUSTOM` case to reloadable too
  (clean VT under ConPTY), pending user confirmation (11).
- `src/cpp/conf/rdesktop-dev.conf` -> remove any winpty path entry.
- `.gitignore` -> temporarily narrowed to commit only `docs/superpowers/specs`
  and `docs/superpowers/plans`; restored to the single `docs/superpowers` line
  during final cleanup (Phase 7).
- `NEWS.md` -> entry under the appropriate section.

## 8. Error handling

- Wrap `HRESULT`/`GetLastError` with `systemError` and context (operation,
  exe, size). Recognize `0xC0000142` (child init failure from an invalid/closed
  pseudoconsole during startup) and surface a clear message.
- If `GetProcAddress` cannot resolve the ConPTY functions, fail `start()` with
  an actionable "ConPTY not available on this Windows version" error (should not
  occur above 1809).
- Every `start()` failure path unwinds via scope guards: close any created
  pipe ends, `DeleteProcThreadAttributeList`, `ClosePseudoConsole`, terminate a
  partially created child. No handle/PC leaks on partial startup.

## 9. Testing and verification

- **Phase 0 standalone POC** (`cl.exe`, no rsession build): spawn `cmd.exe`
  through the 4 sequence with a reader thread; assert `echo HELLO` round-trips
  on the output pipe (not the test console); resize; write `0x03` to a busy
  child (`ping -t` / busy loop) and assert it terminates; clean
  `ClosePseudoConsole` with no hang. Confirms the contract on this machine
  before any integration. (Diagnostic: a high-output child such as
  `cmd /c set` distinguishes pipe-routing failure from a flush race.)
- **C++ unit tests** (`Win32ConPtyTests.cpp`, google test):
  - start/echo, `writeInput` round-trip, `setSize`, `0x03` interrupt of a busy
    child, broken-pipe on child exit, clean shutdown with no hang.
  - **missing-symbol path** (simulate `GetProcAddress` failure -> actionable
    error), **partial-startup failure** (assert no handle/PC leak),
  - **sustained high-output with a slow consumer** (assert bounded memory via
    backpressure and no deadlock), **shutdown timeout/cancel** backstop.
  - Run via `rstudio-tests --scope core`.
- **Playwright** `e2e/rstudio/tests/panes/terminal/terminal.test.ts` on Windows.
- **Manual matrix**: cmd, pwsh, ps-core, git-bash; vim/less (alt-buffer); Ctrl+C
  in a busy program; resize; browser reload (buffer replay incl. cmd/PS);
  rsession restart (replay from persistence); large paste (writer thread).

## 10. Phasing (each independently verifiable)

0. Toolchain check + standalone POC proves the 4 contract on this machine.
1. `ConPty` class with the **complete lifecycle** (reader + writer threads,
   bounded buffer with backpressure, full shutdown contract per 6.3/6.4) +
   dynamic loader + types; `Win32ConPtyTests.cpp`. Build + run unit tests. The
   shutdown/ownership/timeout contract is fully specified here (6.3) before
   coding begins.
2. Integrate into `Win32ChildProcess` (reader -> poll with final-drain-on-exit,
   writer, setSize, shutdown). **Bump `kConsoleDir` -> `console08` in this phase**
   (ConPTY output format goes live here). Build session; smoke a terminal.
3. Session/options layer: trim `Pseudoterminal`, update `SessionConsoleProcess`,
   remove `--external-winpty-path`, regenerate options.
4. Build-system + dependency removal; delete winpty files.
5. Frontend buffer-replay re-enable (`TerminalSession.java`).
6. Full test pass (unit + Playwright) + manual matrix on cmd/pwsh/git-bash/TUI.
7. Cleanup, restore `.gitignore` + remove `docs/superpowers`, `NEWS.md`, final
   review.

Each phase is a commit (or small commit series) and goes through the roborev
review loop before the next phase begins.

## 11. Risks and open questions

- **32-bit session** (`RSTUDIO_SESSION_WIN32`): ConPTY works on x86; the x86
  winpty install simply goes away. Verify a 32-bit session build still links.
- **Windows `CUSTOM` shell replay**: resolved default = reloadable like the
  standard shells (clean VT under ConPTY). Open only for user veto.
- **Deadlock avoidance** rests on the reader always being able to drain (via
  `poll()` on the session loop) and input living on the writer thread; the
  `stop()` ordering in 6.3 must be honored exactly.
- **Persistence**: bumping `kConsoleDir` discards old winpty-era buffers (fresh
  start), which is acceptable; no migration.
- **Watermark sizing**: high/low-water marks for the output buffer are tuning
  values; pick conservative defaults (e.g., a few hundred KB) and confirm under
  the sustained-high-output test.
