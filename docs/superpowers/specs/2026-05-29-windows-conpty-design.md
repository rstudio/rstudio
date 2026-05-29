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
  failure at any point unwinds cleanly (8). The reader thread is launched
  **immediately after `CreateProcess` succeeds**, so any later failure unwinds
  through the same bounded teardown as normal shutdown (6.3) -- there is no
  window where `ClosePseudoConsole` runs with a live child but no draining
  reader.
- Reader thread: blocking `ReadFile(hOutputRead_, ...)`; append under
  `outMutex_`. Backpressure wait is `while (!stopped_ && outputBuffer_.size() >=
  kOutHighWater) outCv_.wait(...)` -- so once `stopped_` is set the reader stops
  pausing and drains freely (this is what breaks the shutdown-while-paused
  deadlock). On `ERROR_BROKEN_PIPE`/EOF set `outputClosed_`, notify, exit; on any
  other `ReadFile` error, log, set `outputClosed_`, and exit (treated as
  pipe-closed).
- `readOutput()`: swap `outputBuffer_` into `*pOut` under `outMutex_`, then
  `notify` `outCv_` so a paused reader resumes.
- Writer thread: wait on `inCv_` for queued input, `inputDone_`, or `abandon_`.
  If `abandon_` it exits immediately (queue already cleared, 6.3) -- it never
  resumes draining after a cancelled write. Otherwise blocking
  `WriteFile(hInputWrite_, ...)`; a failed/cancelled write stores the error in
  `writerError_` (surfaced via `poll()` (6.2) / logged) and exits.
- `writeInput()`: enqueue under `inMutex_` + notify, with no main-thread
  blocking. The queue is **bounded by `kInQueueMax` bytes**; input that would
  exceed the cap is rejected with an error (the caller logs it). Returns the
  stored `writerError_` if a prior async write failed.
- `setSize()`/`writeInput()` are no-ops returning an error once `stopped_`.
- **Bounding constants** (defaults; final values validated by the plan's
  sustained-high-output test): `kOutHighWater` ~1 MiB, `kOutLowWater` ~256 KiB,
  `kInQueueMax` ~1 MiB, `kShutdownTimeoutMs` ~4000.

### 6.2 `Win32ChildProcess` integration

- `ChildProcess::Impl`: replace `WinPty pty` with `ConPty pty`; the PTY path no
  longer uses `hStdOutRead`/`hStdErrRead` (ConPty owns its pipes). Non-PTY pipe
  path is unchanged.
- `ChildProcess::run` (PTY branch): `pImpl_->pty.start(exe_, args_, options_,
  &pImpl_->hProcess)`.
- `AsyncChildProcess::poll()`: replace the two `WinPty::readFromPty` calls with a
  single `pImpl_->pty.readOutput(&out)` -> `onStdout`.
- **On process exit**: run the ordered teardown (`pty.stop()`). Because `stop()`
  sets `stopped_` and wakes the reader, the reader drains ConPTY's final frame
  without re-pausing on backpressure even though `poll()` is blocked inside
  `stop()`; `stop()` joins it. Then **drain `readOutput()` one final time and
  dispatch it to `onStdout`**, and only then close the process handle and call
  `onExit`. This guarantees the last command output/prompt is not lost.
- `writeToStdin` (PTY branch): `pImpl_->pty.writeInput(input)`.
- `ptySetSize` -> `pty.setSize`; `ptyInterrupt` -> no-op (returns Success).

### 6.3 Shutdown ordering (bounded, deadlock-free)

`ConPty::stop()` is idempotent and uses one deterministic sequence (also used by
the partial-startup unwind in 8). Under `stateMutex_` to serialize with
`writeInput`/`setSize`:
1. Set `stopped_`. Wake the reader: lock `outMutex_`, `notify` `outCv_` so a
   reader paused on backpressure resumes and drains freely (no re-pause while
   `stopped_`). This is the step that prevents the shutdown-while-paused hang.
2. Stop the writer: set `abandon_`, **clear `inputQueue_`**, `notify` `inCv_`,
   and `CancelSynchronousIo(writerThread_)` to break a blocked `WriteFile`. The
   writer exits immediately after a cancelled write or on seeing `abandon_`; it
   does not resume draining. `join()` the writer (bounded by `kShutdownTimeoutMs`;
   on timeout, log and detach).
3. Run `ClosePseudoConsole(hPC_)` on a **separate closer thread** (never the
   reader). The reader is draining (step 1), so it returns promptly even
   pre-24H2.
4. Wait for the closer and the reader with a single `kShutdownTimeoutMs` budget.
   On timeout, force the reader out by closing `hOutputRead_` (breaks its
   `ReadFile`) and `CancelSynchronousIo(readerThread_)`, which also lets a
   blocked `ClosePseudoConsole` finish.
5. `join()` the reader and closer. The accumulated `outputBuffer_` holds the
   final frame for 6.2's final `readOutput`. Close `hInputWrite_`/`hOutputRead_`
   (if not already); null `hPC_`.

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
  `default: return true`; the Windows `CUSTOM` case is also reloadable (clean VT
  under ConPTY) -- resolved default, see 11. A Phase-5 gate confirms it with the
  user before shipping.
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
- `start()` failure unwinds via scope guards with two regimes:
  - **Before `CreateProcess`** (pipe/attribute-list/pseudoconsole creation):
    `DeleteProcThreadAttributeList`, close pipe ends, and `ClosePseudoConsole`
    -- with no client attached the close returns immediately, so no drain is
    needed.
  - **After `CreateProcess`**: the reader thread is already running (6.1), so
    unwind goes through the bounded `stop()` sequence (6.3), avoiding a blocking
    `ClosePseudoConsole` with a live child and no reader.
- Writer-thread transport errors are stored in `writerError_` and surfaced on
  the next `poll()` (reported via the existing `onError`/log path); `writeInput`
  also returns a stored prior error.

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
  - Combined-state cases (the likely deadlocks): **shutdown while the reader is
    paused at the high-water mark**; **writer cancellation with a non-empty
    queue** (asserts the writer abandons, does not resume); **input-queue
    overflow** (rejected with an error); **partial-startup failure after
    `CreateProcess`** (bounded teardown, no leak/hang); **reader non-EOF
    `ReadFile` error**; **`0x03` ordering** behind queued input (FIFO preserved).
  - Run via `rstudio-tests --scope core`. (Exact assertions/values pinned in the
    implementation plan.)
- **Playwright** `e2e/rstudio/tests/panes/terminal/terminal.test.ts` on Windows.
- **Manual matrix**: cmd, pwsh, ps-core, git-bash; vim/less (alt-buffer); Ctrl+C
  in a busy program; resize; browser reload (buffer replay incl. cmd/PS);
  rsession restart (replay from persistence); large paste (writer thread).

## 10. Phasing (each independently verifiable)

0. Toolchain check + standalone POC proves the 4 contract on this machine.
1. `ConPty` class with the **complete lifecycle** (reader + writer threads,
   bounded buffer with backpressure, full shutdown contract per 6.3/6.4) +
   dynamic loader + types; `Win32ConPtyTests.cpp`. The shutdown/ownership/
   timeout contract is fully specified (6.3) before coding begins. The plan
   splits this into focused commits -- (1a) dynamic loader + startup/
   `CreateProcess`, (1b) reader + bounded buffer/backpressure, (1c) writer +
   input queue, (1d) shutdown/teardown -- each with its own tests.
2. Integrate into `Win32ChildProcess` (reader -> poll with final-drain-on-exit,
   writer, setSize, shutdown). **Bump `kConsoleDir` -> `console08` in this phase**
   (ConPTY output format goes live here). Build session; smoke a terminal.
3. Session/options layer: trim `Pseudoterminal`, update `SessionConsoleProcess`,
   remove `--external-winpty-path`, regenerate options.
4. Build-system + dependency removal; delete winpty files.
5. Frontend buffer-replay re-enable (`TerminalSession.java`).
6. Full test pass (unit + Playwright) + manual matrix on cmd/pwsh/git-bash/TUI.
7. Cleanup + `NEWS.md` + final review. The cleanup removes exactly the two
   committed design artifacts (`docs/superpowers/specs/2026-05-29-windows-conpty-design.md`
   and the plan file under `docs/superpowers/plans/`) and restores the single
   `docs/superpowers` line in `.gitignore` -- nothing else.

Each phase is a commit (or small commit series) and goes through the roborev
review loop before the next phase begins.

## 11. Risks and open questions

- **32-bit session** (`RSTUDIO_SESSION_WIN32`): ConPTY works on x86; the x86
  winpty install simply goes away. Verify a 32-bit session build still links.
- **Windows `CUSTOM` shell replay**: resolved default = reloadable like the
  standard shells (clean VT under ConPTY). Open only for user veto.
- **Deadlock avoidance** rests on two invariants: steady-state output drains via
  `poll()` on the session loop, and `stop()` releases a backpressure-paused
  reader by setting `stopped_` + notifying `outCv_` (6.3). Input lives on the
  writer thread and is abandoned on stop. The `stop()` ordering must be honored
  exactly.
- **Ctrl+C ordering (not a regression)**: input flows FIFO through the writer
  queue -- identical ordering to today's synchronous writes. A large paste ahead
  of `0x03` delays the interrupt exactly as it does today; the writer thread does
  not change this. (Priority/out-of-band interrupt is the deferred signal-based
  work, not this pass.)
- **Persistence**: bumping `kConsoleDir` discards old winpty-era buffers (fresh
  start), which is acceptable; no migration.
- **Buffer-replay timing**: Phase 2 makes ConPTY live, but the frontend replay
  re-enable lands in Phase 5. In between, replay simply stays disabled for
  Windows shells (today's behavior) -- no incompatibility, just the feature is
  off until Phase 5.
- **Design vs. plan boundary**: this design fixes the lifecycle *contract*. The
  concrete tuning constants (6.1) and the full per-case test assertions are
  pinned in the implementation plan, not here.
