# Windows ConPTY Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the vendored winpty Windows terminal backend with the native ConPTY pseudoconsole API, at functional parity plus re-enabled cmd/PowerShell buffer replay.

**Architecture:** A new `ConPty` class (`core/system/Win32ConPty.{hpp,cpp}`) owns a pseudoconsole, its input/output pipes, and a reader thread + writer thread. The ConPTY functions are resolved dynamically from `kernel32.dll` (the build targets `_WIN32_WINNT=0x601`, which hides the declarations). `Win32ChildProcess` drives it: `poll()` drains the reader's bounded buffer on the session main thread (preserving the callbacks-on-main-thread contract); input is queued to the writer thread. Shutdown uses a drain-then-close sequence with bounded timeouts. winpty (code, dependency, build wiring, the `--external-winpty-path` option) is removed entirely.

**Tech Stack:** C++17, Win32 (ConPTY: `CreatePseudoConsole`/`ResizePseudoConsole`/`ClosePseudoConsole`, `STARTUPINFOEX`, `PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE`), Google Test, CMake/Ninja, GWT (Java) frontend.

**Spec:** `docs/superpowers/specs/2026-05-29-windows-conpty-design.md` (read it before starting; section references below like "(spec 6.3)" point into it).

---

## Conventions used in this plan

- **Build:** from the configured build dir, run `ninja` (build everything; do not scope to a subset target). If the build dir does not exist: `mkdir build && cd build && cmake -DCMAKE_EXPORT_COMPILE_COMMANDS=1 -DRSTUDIO_UNIT_TESTS_ENABLED=1 ..`
- **Run C++ unit tests:** from the build dir, `./rstudio-tests --scope core --filter "<gtest-pattern>"` (e.g. `--filter "*ConPty*"`).
- **Commits:** one logical change per commit; imperative subject <=72 chars; end body with nothing extra (no co-author/footer). Each commit auto-triggers a roborev review — after committing, fetch the review, address findings, and close it before the next task (see CLAUDE.md and the roborev-workflow note).
- **Branch:** `feature/windows-conpty-rewrite` (already current).
- **Platform:** all C++ work here is Windows-only; build and run on this Windows machine.

---

## File Structure

**New files:**
- `src/cpp/core/system/Win32ConPty.hpp` — `ConPty` class declaration (replaces `Win32Pty.hpp`).
- `src/cpp/core/system/Win32ConPty.cpp` — implementation: dynamic loader, start sequence, reader/writer loops, shutdown (replaces `Win32Pty.cpp`).
- `src/cpp/core/system/Win32ConPtyTests.cpp` — gtest suite (replaces `Win32PtyTests.cpp`; auto-globbed by `core/CMakeLists.txt`).
- `build-conpty-poc/conpty_poc.cpp` — **throwaway** Phase-0 proof-of-concept (never committed; lives outside `src/`).

**Deleted files:**
- `src/cpp/core/system/Win32Pty.hpp`, `src/cpp/core/system/Win32Pty.cpp`, `src/cpp/core/system/Win32PtyTests.cpp`
- `dependencies/windows/winpty-0.4.3-msys2-2.7.0.zip`

**Modified files:**
- `src/cpp/core/system/Win32ChildProcess.cpp` — include, `Impl`, `run()`, `writeToStdin()`, `ptySetSize()`/`ptyInterrupt()`, `poll()`.
- `src/cpp/core/include/core/system/Process.hpp` — trim `Pseudoterminal` (Windows fields).
- `src/cpp/core/CMakeLists.txt` — source list (`Win32Pty.cpp` -> `Win32ConPty.cpp`).
- `src/cpp/session/SessionConsoleProcess.cpp` — `Pseudoterminal` construction.
- `src/cpp/session/SessionConsoleProcessPersist.cpp` — `kConsoleDir` bump.
- `src/cpp/session/session-options.json` + `SessionOptions.cpp` (+ regenerated `SessionOptions.gen.hpp`) — remove `external-winpty-path`.
- `src/cpp/CMakeLists.txt`, `src/cpp/session/CMakeLists.txt` — remove winpty wiring.
- `dependencies/windows/install-dependencies.cmd` — remove winpty fetch.
- `src/cpp/conf/rdesktop-dev.conf` — remove `external-winpty-path`.
- `src/gwt/src/org/rstudio/studio/client/workbench/views/terminal/TerminalSession.java` — `shellSupportsReload()`.
- `NEWS.md`, `.gitignore` (final cleanup).

---

## Phase 0: Standalone ConPTY proof-of-concept

**Why:** Before touching rsession, prove the exact ConPTY contract works on this machine and that the self-declared `PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE` value and dynamic resolution compile/run under the project's `_WIN32_WINNT=0x601` gate. This directly tests the prior attempt's "child output went to the wrong console" theory.

### Task 0.1: Write and run the POC

**Files:**
- Create: `build-conpty-poc/conpty_poc.cpp` (NOT committed)

- [ ] **Step 1: Write the POC**

```cpp
// conpty_poc.cpp - throwaway ConPTY contract proof. Build with MSVC.
// Reproduces the project's version gate so the dynamic-load path and the
// self-declared pseudoconsole attribute value are validated exactly.
#define WINVER 0x0601
#define _WIN32_WINNT 0x0601
#include <windows.h>
#include <process.h>
#include <atomic>
#include <string>
#include <thread>
#include <cstdio>

// --- self-declared ConPTY surface (hidden at 0x601) ---
typedef VOID* HPCON;
typedef HRESULT (WINAPI *CreatePseudoConsoleFn)(COORD, HANDLE, HANDLE, DWORD, HPCON*);
typedef HRESULT (WINAPI *ResizePseudoConsoleFn)(HPCON, COORD);
typedef void    (WINAPI *ClosePseudoConsoleFn)(HPCON);
#ifndef PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE
#define PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE ProcThreadAttributeValue(22, FALSE, TRUE, FALSE)
#endif

static CreatePseudoConsoleFn pCreate;
static ResizePseudoConsoleFn pResize;
static ClosePseudoConsoleFn  pClose;

static bool resolveApi()
{
   HMODULE k = GetModuleHandleW(L"kernel32.dll");
   pCreate = (CreatePseudoConsoleFn)GetProcAddress(k, "CreatePseudoConsole");
   pResize = (ResizePseudoConsoleFn)GetProcAddress(k, "ResizePseudoConsole");
   pClose  = (ClosePseudoConsoleFn) GetProcAddress(k, "ClosePseudoConsole");
   printf("resolve: create=%p resize=%p close=%p (attr=0x%08X)\n",
          (void*)pCreate, (void*)pResize, (void*)pClose,
          (unsigned)PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE);
   return pCreate && pResize && pClose;
}

struct Reader {
   HANDLE h; std::atomic<bool>* done; std::string* out;
   void operator()() {
      char buf[4096]; DWORD n;
      for (;;) {
         if (!ReadFile(h, buf, sizeof(buf), &n, nullptr) || n == 0) break;
         out->append(buf, n);
      }
      done->store(true);
   }
};

// Run one child through a pseudoconsole; return its captured output.
// killWith0x03=true writes Ctrl+C and asserts the (busy) child dies fast.
static bool runChild(const wchar_t* cmdline, std::string* captured,
                     bool interactiveEcho, bool killWith0x03)
{
   HANDLE inR=nullptr, inW=nullptr, outR=nullptr, outW=nullptr;
   if (!CreatePipe(&inR, &inW, nullptr, 0)) return false;
   if (!CreatePipe(&outR, &outW, nullptr, 0)) return false;

   HPCON hpc=nullptr;
   COORD size{80, 25};
   if (FAILED(pCreate(size, inR, outW, 0, &hpc))) { printf("CreatePseudoConsole failed\n"); return false; }

   // reader thread BEFORE CreateProcess
   std::atomic<bool> readerDone{false};
   std::string out;
   std::thread reader(Reader{outR, &readerDone, &out});

   STARTUPINFOEXW si; ZeroMemory(&si, sizeof(si));
   si.StartupInfo.cb = sizeof(STARTUPINFOEXW);
   SIZE_T bytes = 0;
   InitializeProcThreadAttributeList(nullptr, 1, 0, &bytes);
   si.lpAttributeList = (LPPROC_THREAD_ATTRIBUTE_LIST)HeapAlloc(GetProcessHeap(), 0, bytes);
   InitializeProcThreadAttributeList(si.lpAttributeList, 1, 0, &bytes);
   if (!UpdateProcThreadAttribute(si.lpAttributeList, 0,
            PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE, hpc, sizeof(hpc), nullptr, nullptr)) {
      printf("UpdateProcThreadAttribute failed: %lu\n", GetLastError()); return false;
   }

   std::wstring mutableCmd(cmdline);
   PROCESS_INFORMATION pi; ZeroMemory(&pi, sizeof(pi));
   BOOL ok = CreateProcessW(nullptr, &mutableCmd[0], nullptr, nullptr, FALSE,
                            EXTENDED_STARTUPINFO_PRESENT, nullptr, nullptr,
                            &si.StartupInfo, &pi);
   if (!ok) { printf("CreateProcess failed: %lu\n", GetLastError()); return false; }

   // close ConPTY-side ends in our process
   CloseHandle(inR); CloseHandle(outW); inR = outW = nullptr;

   if (interactiveEcho) {
      const char* cmd = "echo MARK_CONPTY\r\n";
      DWORD w; WriteFile(inW, cmd, (DWORD)strlen(cmd), &w, nullptr);
   }
   if (killWith0x03) {
      Sleep(300);
      char ctrlC = 0x03; DWORD w; WriteFile(inW, &ctrlC, 1, &w, nullptr);
   }

   DWORD waitMs = killWith0x03 ? 5000 : 4000;
   DWORD wr = WaitForSingleObject(pi.hProcess, waitMs);
   bool exited = (wr == WAIT_OBJECT_0);

   // ordered shutdown: ClosePseudoConsole off the reader thread
   std::thread closer([&]{ pClose(hpc); });
   if (reader.joinable()) reader.join();
   if (closer.joinable()) closer.join();

   DeleteProcThreadAttributeList(si.lpAttributeList);
   HeapFree(GetProcessHeap(), 0, si.lpAttributeList);
   CloseHandle(inW); CloseHandle(outR);
   CloseHandle(pi.hThread); CloseHandle(pi.hProcess);

   *captured = out;
   if (killWith0x03) return exited; // success == child died promptly
   return true;
}

int main()
{
   if (!resolveApi()) { printf("FAIL: ConPTY symbols not available\n"); return 2; }

   std::string out1;
   runChild(L"C:\\Windows\\System32\\cmd.exe /c echo HELLO_CONPTY", &out1, false, false);
   bool t1 = out1.find("HELLO_CONPTY") != std::string::npos;
   printf("[%s] echo: captured %zu bytes\n", t1?"PASS":"FAIL", out1.size());

   std::string out2;
   runChild(L"C:\\Windows\\System32\\cmd.exe", &out2, true, false);
   bool t2 = out2.find("MARK_CONPTY") != std::string::npos;
   printf("[%s] interactive input echo\n", t2?"PASS":"FAIL");

   std::string out3;
   bool t3 = runChild(L"C:\\Windows\\System32\\cmd.exe /c \"ping -n 30 127.0.0.1 >nul\"",
                      &out3, false, true);
   printf("[%s] Ctrl+C (0x03) terminates busy child\n", t3?"PASS":"FAIL");

   printf("RESULT: %s\n", (t1 && t2 && t3) ? "ALL PASS" : "FAILURE");
   return (t1 && t2 && t3) ? 0 : 1;
}
```

- [ ] **Step 2: Build the POC**

From a "x64 Native Tools Command Prompt for VS" (or after running `vcvars64.bat`):
```
cl /EHsc /std:c++17 /Fe:conpty_poc.exe build-conpty-poc\conpty_poc.cpp
```
Expected: compiles cleanly. If `ProcThreadAttributeValue` is undefined, the SDK is unexpectedly old — stop and report.

- [ ] **Step 3: Run the POC**

```
conpty_poc.exe
```
Expected output ends with `RESULT: ALL PASS`, and the `resolve:` line shows three non-null pointers and `attr=0x00020016`.

**This is the gate for the whole project.** If `echo` captures the banner but not `HELLO_CONPTY`, or output appears in the console instead of `captured`, the attach is wrong — debug the `STARTUPINFOEX`/`UpdateProcThreadAttribute` wiring here before writing any in-tree code. Do NOT proceed until ALL PASS.

- [ ] **Step 4: Record the validated constant**

Note the printed `attr` value (must be `0x00020016`). This confirms the macro used in Phase 1.

(No commit — the POC is throwaway and lives outside `src/`. Leave the file on disk for reference until Phase 7.)

### Phase 0 results (recorded 2026-05-30)

Completed on this machine (MSVC VS2022 BuildTools, cl 19.44). Findings that shape Phase 1:

- **Build approach validated:** compiles under `_WIN32_WINNT=0x601` with dynamic
  kernel32 resolution; `PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE` == `0x00020016`; all
  three functions resolve.
- **Routing contract validated, and the prior failure explained:** with
  `EXTENDED_STARTUPINFO_PRESENT`, `bInheritHandles=FALSE`, a zeroed `STARTUPINFOEX`
  (no `STARTF_USESTDHANDLES`), and the PTY-side pipe ends closed before
  `CreateProcess`, the child attaches to the pseudoconsole and `HELLO_CONPTY` /
  interactive input route through the pipes **even when the host owns a console**.
  A controlled confound experiment isolated the real breaker as **redirecting the
  child's std handles** (`STARTF_USESTDHANDLES`), not console ownership —
  `FreeConsole` is NOT needed. `DETACHED_PROCESS`/`CREATE_NO_WINDOW` break PC
  attach and must not be used. `launchChild` above already matches the validated
  configuration.
- **Teardown validated:** `ClosePseudoConsole` on its own thread with a draining
  reader terminated even a non-cooperative, actively-writing child in single-digit
  milliseconds, both threads joined, deterministic across runs. The Task 1d
  cancel-loop is a backstop; the closer breaking the pipes is the fast hot path.
- **Caveat (fast-exit attach race):** a sub-millisecond child (e.g. `cmd /c echo`)
  attaches ~95% of the time; a child living >~1s is deterministic. Real interactive
  shells are unaffected. Not worth special handling; note it if a flaky
  fast-command test appears.

---

## Phase 1: The `ConPty` class

Built in four commits (1a-1d), each adding a testable capability. The reader/writer threads are launched **after `CreatePseudoConsole` but before `CreateProcess`** (spec 6.1) so there is never a live child without a draining reader.

### Task 1a: Dynamic loader, start sequence, reader thread, basic stop

**Files:**
- Create: `src/cpp/core/system/Win32ConPty.hpp`
- Create: `src/cpp/core/system/Win32ConPty.cpp`
- Create: `src/cpp/core/system/Win32ConPtyTests.cpp`
- Modify: `src/cpp/core/CMakeLists.txt:285`

- [ ] **Step 1: Write `Win32ConPty.hpp`**

```cpp
/*
 * Win32ConPty.hpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_SYSTEM_WIN32_CONPTY_HPP
#define CORE_SYSTEM_WIN32_CONPTY_HPP

#include <atomic>
#include <condition_variable>
#include <deque>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include <windows.h>

#include <boost/noncopyable.hpp>

#include <core/system/Process.hpp>
#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace system {

// HPCON is only declared in SDK headers gated behind NTDDI_WIN10_RS5; the build
// targets _WIN32_WINNT=0x601, so we use our own handle type and resolve the
// pseudoconsole functions dynamically from kernel32 (see Win32ConPty.cpp).
typedef VOID* HPCONHANDLE;

// Native Windows pseudoconsole (ConPTY) wrapper. Replaces the winpty-based
// WinPty class. Owns the pseudoconsole, its input/output pipes, and a reader
// thread + writer thread. Output is buffered (bounded, with backpressure) and
// drained by the caller via readOutput(); input is queued to the writer thread.
class ConPty : boost::noncopyable
{
public:
   ConPty() = default;
   ~ConPty();

   // True if ConPTY is available on this OS (functions resolved from kernel32).
   static bool isAvailable();

   // Create the pseudoconsole + pipes, launch reader/writer threads, and spawn
   // the child. On success *pProcess receives the child process handle (caller
   // owns it). On failure all internal state is torn down and *pProcess is null.
   Error start(const std::string& exe,
               const std::vector<std::string>& args,
               const ProcessOptions& options,
               HANDLE* pProcess);

   bool running() const;

   // Non-blocking: move buffered output into *pOutput (may be empty).
   Error readOutput(std::string* pOutput);

   // Enqueue input for the writer thread (non-blocking). Rejects with an error
   // if the queue would exceed its byte cap, or if a prior write failed.
   Error writeInput(const std::string& input);

   // Request EOF on the child's stdin: after the queued input drains, the writer
   // closes the input pipe (parity with the old winpty conin-close on eof).
   Error closeInput();

   // Returns and clears any error a prior asynchronous write recorded, so the
   // caller (poll()) can surface it promptly. Success() if none.
   Error takeWriterError();

   // ResizePseudoConsole. No-op error after stop().
   Error setSize(int cols, int rows);

   // Parity no-op: Ctrl+C is delivered as the 0x03 byte via writeInput().
   Error interrupt();

   // Ordered, bounded teardown (idempotent). Also called by the destructor.
   void stop();

private:
   Error createPipesAndConsole(int cols, int rows);
   Error launchChild(const std::string& exe,
                     const std::vector<std::string>& args,
                     const ProcessOptions& options,
                     HANDLE* pProcess);
   void readerLoop();
   void writerLoop();       // wraps writerLoopBody(), then sets writerExited_
   void writerLoopBody();   // drains the input queue (defined in phase 1c)
   void teardownLocked();   // single ordered teardown path; caller holds stateMutex_

   HPCONHANDLE hPC_ = nullptr;
   HANDLE hInputWrite_ = nullptr;    // writer thread writes here
   HANDLE hOutputRead_ = nullptr;    // reader thread reads here

   std::thread readerThread_;
   std::thread writerThread_;

   std::mutex stateMutex_;           // serializes start/stop/setSize
   std::atomic<bool> stopped_{false};
   std::atomic<bool> abandon_{false};
   std::atomic<bool> writerExited_{false};  // writer no longer consuming input

   std::mutex outMutex_;
   std::condition_variable outCv_;
   std::string outputBuffer_;
   bool outputTruncated_ = false;   // set if forced-shutdown dropped output

   std::mutex inMutex_;
   std::condition_variable inCv_;
   std::deque<std::string> inputQueue_;
   size_t inputQueuedBytes_ = 0;
   bool closeInputRequested_ = false;   // writer closes input pipe after draining
   Error writerError_;
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_WIN32_CONPTY_HPP
```

- [ ] **Step 2: Write `Win32ConPty.cpp` (loader + start + reader + basic stop)**

This is the 1a slice. The writer loop body, backpressure, and the hardened teardown are added in 1b-1d; for now `writerLoop()` is a stub and `stop()` is the simple ordered close.

```cpp
/*
 * Win32ConPty.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * (license header as in Win32ConPty.hpp)
 *
 */

#include "Win32ConPty.hpp"

#include <core/Log.hpp>
#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

// Bounding constants (defaults; validated by the sustained-high-output test).
// readOutput() swaps the whole buffer out, so a paused reader resumes from an
// empty buffer on the next drain -- no low-water hysteresis is needed.
const size_t kOutHighWater    = 1u * 1024 * 1024;  // 1 MiB
const size_t kInQueueMax      = 1u * 1024 * 1024;  // 1 MiB
const DWORD  kShutdownTimeout = 4000;              // ms
const DWORD  kReadChunk       = 4096;

#ifndef PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE
#define PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE ProcThreadAttributeValue(22, FALSE, TRUE, FALSE)
#endif

typedef HRESULT (WINAPI *CreatePseudoConsoleFn)(COORD, HANDLE, HANDLE, DWORD, HPCONHANDLE*);
typedef HRESULT (WINAPI *ResizePseudoConsoleFn)(HPCONHANDLE, COORD);
typedef void    (WINAPI *ClosePseudoConsoleFn)(HPCONHANDLE);

struct ConPtyApi
{
   CreatePseudoConsoleFn create = nullptr;
   ResizePseudoConsoleFn resize = nullptr;
   ClosePseudoConsoleFn  close  = nullptr;
   bool available = false;
};

const ConPtyApi& api()
{
   static ConPtyApi s_api = []
   {
      ConPtyApi a;
      HMODULE hKernel = ::GetModuleHandleW(L"kernel32.dll");
      if (hKernel)
      {
         a.create = reinterpret_cast<CreatePseudoConsoleFn>(
               ::GetProcAddress(hKernel, "CreatePseudoConsole"));
         a.resize = reinterpret_cast<ResizePseudoConsoleFn>(
               ::GetProcAddress(hKernel, "ResizePseudoConsole"));
         a.close = reinterpret_cast<ClosePseudoConsoleFn>(
               ::GetProcAddress(hKernel, "ClosePseudoConsole"));
      }
      a.available = a.create && a.resize && a.close;
      return a;
   }();
   return s_api;
}

// Build a mutable command-line string (ported from WinPty::runProcess).
std::wstring buildCommandLine(const std::string& exe,
                              const std::vector<std::string>& args)
{
   std::string cmdLine = "\"" + exe + "\"";
   for (const std::string& arg : args)
   {
      cmdLine.push_back(' ');
      bool quot = std::string::npos != arg.find(' ') &&
                  std::string::npos == arg.find('"');
      if (quot) cmdLine.push_back('"');
      cmdLine += arg;
      if (quot) cmdLine.push_back('"');
   }
   return string_utils::utf8ToWide(cmdLine, "ConPty::buildCommandLine");
}

// Build a UTF-16 environment block (ported from WinPtySpawnConfig).
std::vector<wchar_t> buildEnvBlock(const ProcessOptions& options)
{
   std::vector<wchar_t> env;
   if (options.environment)
   {
      for (const Option& var : options.environment.get())
      {
         std::wstring key = string_utils::utf8ToWide(var.first);
         std::wstring value = string_utils::utf8ToWide(var.second);
         env.insert(env.end(), key.begin(), key.end());
         env.push_back(L'=');
         env.insert(env.end(), value.begin(), value.end());
         env.push_back(L'\0');
      }
   }
   // A Unicode environment block is terminated by a final L'\0'. An EMPTY block
   // must still be two nulls (CreateProcessW reads a double-null terminator), so
   // emit an extra null when there are no entries.
   if (env.empty())
      env.push_back(L'\0');
   env.push_back(L'\0');
   return env;
}

} // anonymous namespace

bool ConPty::isAvailable()
{
   return api().available;
}

ConPty::~ConPty()
{
   stop();
}

bool ConPty::running() const
{
   return hPC_ != nullptr;
}

Error ConPty::createPipesAndConsole(int cols, int rows)
{
   if (cols < 1) cols = 80;
   if (rows < 1) rows = 25;

   // Each failure path leaves the object clean (no half-opened handles), so a
   // failed start() can be safely destructed or retried.
   HANDLE inputRead = nullptr, outputWrite = nullptr;
   if (!::CreatePipe(&inputRead, &hInputWrite_, nullptr, 0))
   {
      hInputWrite_ = nullptr;
      return LAST_SYSTEM_ERROR();
   }
   if (!::CreatePipe(&hOutputRead_, &outputWrite, nullptr, 0))
   {
      Error e = LAST_SYSTEM_ERROR();
      ::CloseHandle(inputRead);
      ::CloseHandle(hInputWrite_); hInputWrite_ = nullptr;
      hOutputRead_ = nullptr;
      return e;
   }

   COORD size;
   size.X = static_cast<SHORT>(cols);
   size.Y = static_cast<SHORT>(rows);
   HRESULT hr = api().create(size, inputRead, outputWrite, 0, &hPC_);

   // The ConPTY duplicates these; close our copies so broken-pipe works.
   ::CloseHandle(inputRead);
   ::CloseHandle(outputWrite);

   if (FAILED(hr))
   {
      hPC_ = nullptr;
      ::CloseHandle(hInputWrite_); hInputWrite_ = nullptr;
      ::CloseHandle(hOutputRead_); hOutputRead_ = nullptr;
      return systemError(boost::system::errc::io_error,
                         "CreatePseudoConsole failed (hr=" + std::to_string(hr) + ")",
                         ERROR_LOCATION);
   }
   return Success();
}

Error ConPty::launchChild(const std::string& exe,
                          const std::vector<std::string>& args,
                          const ProcessOptions& options,
                          HANDLE* pProcess)
{
   STARTUPINFOEXW si;
   ZeroMemory(&si, sizeof(si));
   si.StartupInfo.cb = sizeof(STARTUPINFOEXW);
   // EMPIRICALLY VALIDATED (Phase 0): leave dwFlags=0 and the hStdInput/Output/
   // Error fields null -- do NOT set STARTF_USESTDHANDLES. Redirecting the
   // child's std handles is what actually breaks ConPTY routing (the child
   // writes to the redirected handle instead of the pseudoconsole); it is the
   // real cause of the prior migration's "output went to the wrong place"
   // failure, not console ownership. With clean std handles + bInheritHandles
   // FALSE + closing the PTY-side ends before CreateProcess, routing works even
   // when the host process owns a console -- no FreeConsole needed.

   SIZE_T bytes = 0;
   ::InitializeProcThreadAttributeList(nullptr, 1, 0, &bytes);
   si.lpAttributeList = reinterpret_cast<LPPROC_THREAD_ATTRIBUTE_LIST>(
         ::HeapAlloc(::GetProcessHeap(), 0, bytes));
   if (!si.lpAttributeList)
      return systemError(boost::system::errc::not_enough_memory, ERROR_LOCATION);

   if (!::InitializeProcThreadAttributeList(si.lpAttributeList, 1, 0, &bytes))
   {
      Error e = LAST_SYSTEM_ERROR();
      ::HeapFree(::GetProcessHeap(), 0, si.lpAttributeList);
      return e;
   }

   if (!::UpdateProcThreadAttribute(si.lpAttributeList, 0,
            PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE, hPC_, sizeof(hPC_), nullptr, nullptr))
   {
      Error e = LAST_SYSTEM_ERROR();
      ::DeleteProcThreadAttributeList(si.lpAttributeList);
      ::HeapFree(::GetProcessHeap(), 0, si.lpAttributeList);
      return e;
   }

   std::wstring cmdLine = buildCommandLine(exe, args);

   // Pass an explicit (UTF-16) environment block only when the caller supplied
   // one; otherwise pass nullptr so the child inherits our environment (matching
   // the old winpty behavior). An empty non-null block would give the child an
   // empty environment.
   std::vector<wchar_t> env;
   LPVOID lpEnv = nullptr;
   // EMPIRICALLY VALIDATED (Phase 0): do NOT add DETACHED_PROCESS or
   // CREATE_NO_WINDOW -- both prevent the child from attaching to the
   // pseudoconsole (routing breaks, 0/4 in testing). CREATE_NEW_PROCESS_GROUP is
   // safe if ever needed for signal-based interrupt (deferred). Plain
   // EXTENDED_STARTUPINFO_PRESENT is correct.
   DWORD creationFlags = EXTENDED_STARTUPINFO_PRESENT;
   if (options.environment)
   {
      env = buildEnvBlock(options);
      lpEnv = env.data();
      creationFlags |= CREATE_UNICODE_ENVIRONMENT;
   }

   std::wstring workingDir = options.workingDir.isEmpty()
         ? std::wstring()
         : options.workingDir.getAbsolutePathW();

   PROCESS_INFORMATION pi;
   ZeroMemory(&pi, sizeof(pi));
   BOOL ok = ::CreateProcessW(
         nullptr,
         &cmdLine[0],
         nullptr, nullptr,
         FALSE, // bInheritHandles
         creationFlags,
         lpEnv,
         workingDir.empty() ? nullptr : workingDir.c_str(),
         &si.StartupInfo,
         &pi);
   Error createErr = ok ? Success() : LAST_SYSTEM_ERROR();

   ::DeleteProcThreadAttributeList(si.lpAttributeList);
   ::HeapFree(::GetProcessHeap(), 0, si.lpAttributeList);

   if (!ok)
      return createErr;

   ::CloseHandle(pi.hThread);
   *pProcess = pi.hProcess;
   return Success();
}

Error ConPty::start(const std::string& exe,
                    const std::vector<std::string>& args,
                    const ProcessOptions& options,
                    HANDLE* pProcess)
{
   std::lock_guard<std::mutex> lock(stateMutex_);
   *pProcess = nullptr;

   if (!isAvailable())
      return systemError(boost::system::errc::not_supported,
                         "ConPTY is not available on this version of Windows",
                         ERROR_LOCATION);
   if (running())
      return systemError(boost::system::errc::already_connected,
                         "ConPty already running", ERROR_LOCATION);
   // A ConPty is single-use: once stopped (or a start failed), per-run state is
   // not reset. Reject reuse rather than support a restart path we never need
   // (one ConPty per terminal). stopped_ is set by teardownLocked()/stop().
   if (stopped_.load())
      return systemError(boost::system::errc::not_supported,
                         "ConPty cannot be reused after stop()", ERROR_LOCATION);

   int cols = options.pseudoterminal ? options.pseudoterminal.get().cols : options.cols;
   int rows = options.pseudoterminal ? options.pseudoterminal.get().rows : options.rows;

   Error error = createPipesAndConsole(cols, rows);
   if (error)
      return error;

   // Launch reader (and, from 1c, writer) BEFORE CreateProcess so a child is
   // never live without a draining reader.
   readerThread_ = std::thread([this] { readerLoop(); });

   error = launchChild(exe, args, options, pProcess);
   if (error)
   {
      teardownLocked(); // reader already running; single teardown path (below)
      return error;
   }
   return Success();
}

void ConPty::readerLoop()
{
   std::vector<char> buf(kReadChunk);
   for (;;)
   {
      DWORD n = 0;
      BOOL ok = ::ReadFile(hOutputRead_, buf.data(), kReadChunk, &n, nullptr);
      if (!ok || n == 0)
      {
         // EOF/broken-pipe is the normal end; log only unexpected errors.
         DWORD err = ::GetLastError();
         if (!ok && n == 0 && err != ERROR_BROKEN_PIPE && err != ERROR_HANDLE_EOF)
            LOG_ERROR(systemError(err, ERROR_LOCATION));
         std::lock_guard<std::mutex> lock(outMutex_);
         outCv_.notify_all();
         return;
      }
      std::lock_guard<std::mutex> lock(outMutex_);
      outputBuffer_.append(buf.data(), n);
      // (backpressure added in 1b)
   }
}

void ConPty::writerLoop()
{
   // (implemented in 1c)
}

Error ConPty::readOutput(std::string* pOutput)
{
   std::lock_guard<std::mutex> lock(outMutex_);
   pOutput->clear();
   pOutput->swap(outputBuffer_);
   outCv_.notify_all(); // release a paused reader (1b)
   return Success();
}

Error ConPty::writeInput(const std::string& /*input*/)
{
   return Success(); // (implemented in 1c)
}

Error ConPty::setSize(int cols, int rows)
{
   // Hold stateMutex_ so we cannot race teardownLocked() nulling/closing hPC_
   // (the resize call and the running/stopped check must be atomic together).
   std::lock_guard<std::mutex> lock(stateMutex_);
   if (stopped_.load() || !running())
      return systemError(boost::system::errc::not_connected, ERROR_LOCATION);
   if (cols < 1 || rows < 1)
      return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
   COORD size;
   size.X = static_cast<SHORT>(cols);
   size.Y = static_cast<SHORT>(rows);
   HRESULT hr = api().resize(hPC_, size);
   if (FAILED(hr))
      return systemError(boost::system::errc::io_error,
                         "ResizePseudoConsole failed", ERROR_LOCATION);
   return Success();
}

Error ConPty::interrupt()
{
   return Success(); // parity no-op; Ctrl+C is 0x03 via writeInput()
}

void ConPty::stop()
{
   std::lock_guard<std::mutex> lock(stateMutex_);
   teardownLocked();
}

// 1a version: reader only. Extended for the writer in 1c and hardened
// (closer-first ordering, cancel loops, bounded waits) in 1d.
void ConPty::teardownLocked()
{
   if (stopped_.exchange(true) && !running() && !readerThread_.joinable())
      return; // already torn down (idempotent)

   { std::lock_guard<std::mutex> ol(outMutex_); outCv_.notify_all(); } // wake reader

   // Close the pseudoconsole on a closer thread; it breaks the output pipe so
   // the reader's ReadFile returns. The closer captures only the HPCON value.
   std::thread closer;
   if (hPC_)
   {
      HPCONHANDLE hpc = hPC_;
      closer = std::thread([hpc] { api().close(hpc); });
   }
   if (readerThread_.joinable())
      readerThread_.join();
   if (closer.joinable())
      closer.join();
   hPC_ = nullptr;

   if (hInputWrite_) { ::CloseHandle(hInputWrite_); hInputWrite_ = nullptr; }
   if (hOutputRead_) { ::CloseHandle(hOutputRead_); hOutputRead_ = nullptr; }
}
```

- [ ] **Step 3: Add `Win32ConPty.cpp` to `core/CMakeLists.txt` (coexist with winpty)**

In `src/cpp/core/CMakeLists.txt`, add a line directly after `system/Win32Pty.cpp` (line 285) so both compile during Phase 1:
```
      system/Win32Pty.cpp
      system/Win32ConPty.cpp
```
(Phase 2 removes the `Win32Pty.cpp` line. winpty stays fully functional through Phase 1; `ConPty` is new code exercised only by its unit tests until the Phase 2 swap.)

- [ ] **Step 4: Write the 1a test in `Win32ConPtyTests.cpp`**

```cpp
/*
 * Win32ConPtyTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * (license header)
 *
 */

#ifdef _WIN32

#include "Win32ConPty.hpp"

#include <gtest/gtest.h>

#include <chrono>
#include <thread>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <core/StringUtils.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

namespace {

// Note: the ConPty unit tests call pty.start() directly and only need
// cols/rows, which ConPty::start reads from options.cols/rows when
// pseudoterminal is unset. Deliberately do NOT construct a Pseudoterminal here
// so these tests are independent of the struct's signature (which is trimmed in
// Phase 3) and of the winpty coexistence during Phase 1.
ProcessOptions ptyOptions(int cols = 80, int rows = 25)
{
   ProcessOptions options;
   options.cols = cols;
   options.rows = rows;
   return options;
}

std::string cmdExe()
{
   return expandComSpec().getAbsolutePathNative();
}

// Drain output until `needle` appears or timeout. Returns accumulated output.
std::string drainUntil(ConPty& pty, const std::string& needle, int timeoutMs)
{
   std::string acc;
   auto deadline = std::chrono::steady_clock::now() +
                   std::chrono::milliseconds(timeoutMs);
   while (std::chrono::steady_clock::now() < deadline)
   {
      std::string chunk;
      pty.readOutput(&chunk);
      acc += chunk;
      if (!needle.empty() && acc.find(needle) != std::string::npos)
         break;
      std::this_thread::sleep_for(std::chrono::milliseconds(25));
   }
   return acc;
}

} // anonymous namespace

TEST(Win32ConPtyTest, ApiIsAvailable)
{
   ASSERT_TRUE(ConPty::isAvailable());
}

TEST(Win32ConPtyTest, StartSpawnsChildAndEchoes)
{
   ConPty pty;
   std::vector<std::string> args = {"/c", "echo HELLO_CONPTY"};
   HANDLE hProc = nullptr;

   Error err = pty.start(cmdExe(), args, ptyOptions(), &hProc);
   ASSERT_FALSE(err) << err.asString();
   ASSERT_TRUE(hProc);
   ASSERT_TRUE(pty.running());

   std::string out = drainUntil(pty, "HELLO_CONPTY", 5000);
   EXPECT_NE(out.find("HELLO_CONPTY"), std::string::npos) << out;

   pty.stop();
   EXPECT_FALSE(pty.running());
   ::CloseHandle(hProc);
}

TEST(Win32ConPtyTest, StopBeforeOutputDoesNotHang)
{
   ConPty pty;
   std::vector<std::string> args; // interactive cmd
   HANDLE hProc = nullptr;
   ASSERT_FALSE(pty.start(cmdExe(), args, ptyOptions(), &hProc));
   pty.stop(); // must return promptly
   EXPECT_FALSE(pty.running());
   ::CloseHandle(hProc);
}

} // namespace tests
} // namespace system
} // namespace core
} // namespace rstudio

#endif // _WIN32
```

- [ ] **Step 5: Build (winpty and ConPty coexist)**

Run: `ninja` (from build dir)
Expected: builds `rstudio-core-tests`. `Win32ChildProcess.cpp` still uses `WinPty` (untouched); `Win32ConPty.cpp` is a new TU exercised only by the new test. Do NOT delete `Win32Pty.*` yet (that is the Phase 2 swap).

- [ ] **Step 6: Run the 1a tests**

Run: `./rstudio-tests --scope core --filter "Win32ConPtyTest.*"`
Expected: `ApiIsAvailable`, `StartSpawnsChildAndEchoes`, `StopBeforeOutputDoesNotHang` all PASS. (The old `Win32PtyTest.*` still pass too.)

- [ ] **Step 7: Commit**

```bash
git add src/cpp/core/system/Win32ConPty.hpp src/cpp/core/system/Win32ConPty.cpp src/cpp/core/system/Win32ConPtyTests.cpp src/cpp/core/CMakeLists.txt
git commit -m "Add ConPty class: dynamic loader, start, reader, basic stop"
```
Then handle the roborev review for this commit before continuing.

### Task 1b: Bounded output buffer + backpressure + shutdown-while-paused fix

**Files:**
- Modify: `src/cpp/core/system/Win32ConPty.cpp` (`readerLoop`, `stop`)
- Modify: `src/cpp/core/system/Win32ConPtyTests.cpp` (add tests)

- [ ] **Step 1: Write failing tests**

Add to `Win32ConPtyTests.cpp`:
```cpp
TEST(Win32ConPtyTest, HighOutputStaysBounded)
{
   ConPty pty;
   // emit ~5 MiB quickly without draining
   std::vector<std::string> args = {"/c", "for /L %i in (1,1,80000) do @echo AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"};
   HANDLE hProc = nullptr;
   ASSERT_FALSE(pty.start(cmdExe(), args, ptyOptions(), &hProc));

   // do NOT drain for a while; the internal buffer must stay bounded
   std::this_thread::sleep_for(std::chrono::seconds(2));
   // First drain returns at most ~kOutHighWater; the class must not have grown
   // unboundedly (process did not OOM and stop() must not hang).
   std::string chunk;
   pty.readOutput(&chunk);
   EXPECT_LE(chunk.size(), 2u * 1024 * 1024);

   pty.stop(); // must not hang even though the reader was paused
   ::CloseHandle(hProc);
}
```

- [ ] **Step 2: Run it (expect FAIL or hang risk)**

Run: `./rstudio-tests --scope core --filter "Win32ConPtyTest.HighOutputStaysBounded"`
Expected: without backpressure the buffer grows unbounded (no cap assertion) — FAIL on the `EXPECT_LE` or excessive memory. This documents the gap.

- [ ] **Step 3: Add backpressure to `readerLoop()`**

Replace the append section of `readerLoop()`:
```cpp
      std::lock_guard<std::mutex> lock(outMutex_);
      outputBuffer_.append(buf.data(), n);
      // (backpressure added in 1b)
```
with:
```cpp
      std::unique_lock<std::mutex> lock(outMutex_);
      // Backpressure: while not stopping and at/over the high-water mark, wait
      // for poll() to drain the buffer (readOutput swaps it out entirely).
      // Once stopped_, never pause (this prevents the shutdown-while-paused
      // hang); instead drop the chunk and flag truncation to keep memory bounded.
      while (!stopped_.load() && outputBuffer_.size() >= kOutHighWater)
         outCv_.wait(lock);
      if (outputBuffer_.size() >= kOutHighWater)
         outputTruncated_ = true; // stopped_ and at cap: drop to stay bounded
      else
         outputBuffer_.append(buf.data(), n);
```
(`readOutput()` already calls `outCv_.notify_all()` after swapping the buffer out, which wakes a paused reader.)

- [ ] **Step 4: Confirm `stop()` wakes a paused reader**

In `stop()`, immediately after `stopped_.store(true);`, add:
```cpp
   { std::lock_guard<std::mutex> ol(outMutex_); outCv_.notify_all(); }
```
(So a reader paused on `outCv_` resumes, sees `stopped_`, and drains without re-pausing.)

- [ ] **Step 5: Run tests**

Run: `./rstudio-tests --scope core --filter "Win32ConPtyTest.*"`
Expected: all PASS, including `HighOutputStaysBounded`, with no hang.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "Bound ConPty output buffer with backpressure; wake reader on stop"
```
Handle the roborev review before continuing.

### Task 1c: Writer thread, input queue, setSize, interrupt no-op

**Files:**
- Modify: `src/cpp/core/system/Win32ConPty.cpp` (`start` to launch writer, `writerLoop`, `writeInput`)
- Modify: `src/cpp/core/system/Win32ConPtyTests.cpp`

- [ ] **Step 1: Write failing tests**

```cpp
TEST(Win32ConPtyTest, WriteInputIsEchoed)
{
   ConPty pty;
   std::vector<std::string> args; // interactive
   HANDLE hProc = nullptr;
   ASSERT_FALSE(pty.start(cmdExe(), args, ptyOptions(), &hProc));

   drainUntil(pty, ">", 4000); // wait for prompt
   ASSERT_FALSE(pty.writeInput("echo MARK_WRITE\r\n"));
   std::string out = drainUntil(pty, "MARK_WRITE", 4000);
   EXPECT_NE(out.find("MARK_WRITE"), std::string::npos) << out;

   pty.stop();
   ::CloseHandle(hProc);
}

TEST(Win32ConPtyTest, CtrlCByteTerminatesBusyChild)
{
   ConPty pty;
   std::vector<std::string> args = {"/c", "ping -n 30 127.0.0.1 >nul"};
   HANDLE hProc = nullptr;
   ASSERT_FALSE(pty.start(cmdExe(), args, ptyOptions(), &hProc));

   std::this_thread::sleep_for(std::chrono::milliseconds(300));
   ASSERT_FALSE(pty.writeInput(std::string(1, '\x03')));

   DWORD wr = ::WaitForSingleObject(hProc, 5000);
   EXPECT_EQ(wr, WAIT_OBJECT_0); // child died promptly

   pty.stop();
   ::CloseHandle(hProc);
}

TEST(Win32ConPtyTest, InputQueueOverflowRejected)
{
   ConPty pty;
   std::vector<std::string> args; // interactive, won't drain 1MiB fast
   HANDLE hProc = nullptr;
   ASSERT_FALSE(pty.start(cmdExe(), args, ptyOptions(), &hProc));

   Error err;
   for (int i = 0; i < 4096 && !err; ++i)
      err = pty.writeInput(std::string(1024, 'x')); // 4 MiB attempted
   EXPECT_TRUE(err); // some write rejected by the queue cap

   pty.stop();
   ::CloseHandle(hProc);
}
```

- [ ] **Step 2: Run (expect FAIL — writeInput is a stub)**

Run: `./rstudio-tests --scope core --filter "Win32ConPtyTest.WriteInputIsEchoed:Win32ConPtyTest.CtrlCByteTerminatesBusyChild:Win32ConPtyTest.InputQueueOverflowRejected"`
Expected: FAIL (no echo; child not killed; no overflow error).

- [ ] **Step 3: Launch the writer thread in `start()`**

In `start()`, next to the reader launch, add:
```cpp
   readerThread_ = std::thread([this] { readerLoop(); });
   writerThread_ = std::thread([this] { writerLoop(); });
```

- [ ] **Step 4: Implement `writerLoop()` and `writeInput()`**

Replace the stub `writerLoop()`. The inner loop runs until it returns for any
reason; the outer wrapper sets `writerExited_` exactly once so `writeInput()`
rejects input after the writer is gone (whatever the exit cause):
```cpp
void ConPty::writerLoop()
{
   writerLoopBody();
   writerExited_.store(true); // any exit: writer is no longer consuming input
}

void ConPty::writerLoopBody()
{
   for (;;)
   {
      std::string chunk;
      bool closeNow = false;
      {
         std::unique_lock<std::mutex> lock(inMutex_);
         inCv_.wait(lock, [this] {
            return abandon_.load() || !inputQueue_.empty() || closeInputRequested_;
         });
         if (abandon_.load())
            return; // shutdown abandons queued input; teardown closes the pipe
         if (!inputQueue_.empty())
         {
            chunk = std::move(inputQueue_.front());
            inputQueue_.pop_front();
            inputQueuedBytes_ -= chunk.size();
         }
         else if (closeInputRequested_)
         {
            closeNow = true; // queue fully drained and EOF was requested
         }
      }

      if (closeNow)
      {
         // Deliver stdin EOF by closing our input write end, then exit. Safe to
         // close here: teardown joins this thread before touching hInputWrite_.
         if (hInputWrite_) { ::CloseHandle(hInputWrite_); hInputWrite_ = nullptr; }
         return;
      }

      DWORD written = 0;
      if (!::WriteFile(hInputWrite_, chunk.data(),
                       static_cast<DWORD>(chunk.size()), &written, nullptr))
      {
         std::lock_guard<std::mutex> lock(inMutex_);
         writerError_ = LAST_SYSTEM_ERROR();
         writerExited_.store(true); // reject later input atomically with the error
         return; // exit after a failed/cancelled write (surfaced via takeWriterError)
      }
   }
}
```
Declare the helper in the header's private section: `void writerLoopBody();` (already
added to the 1a header). Setting `writerExited_` under `inMutex_` here closes the
window where `takeWriterError()` has cleared `writerError_` but the writer is gone:
`writeInput()` (below) rejects on `writerExited_`, which is never cleared.
Replace the stub `writeInput()` (the final `stopped_`-under-lock form is set in 1d):
```cpp
Error ConPty::writeInput(const std::string& input)
{
   if (input.empty())
      return Success();

   std::lock_guard<std::mutex> lock(inMutex_);
   // Reject once the input channel is gone: stopping, EOF requested, or the
   // writer thread has exited (writerExited_ is persistent and never cleared, so
   // input is never queued to a dead writer even after takeWriterError() clears
   // the reportable error).
   if (stopped_.load() || closeInputRequested_ || writerExited_.load())
      return systemError(boost::system::errc::not_connected,
                         "ConPty input channel is closed", ERROR_LOCATION);
   if (inputQueuedBytes_ + input.size() > kInQueueMax)
      return systemError(boost::system::errc::no_buffer_space,
                         "ConPty input queue full", ERROR_LOCATION);
   inputQueuedBytes_ += input.size();
   inputQueue_.push_back(input);
   inCv_.notify_one();
   return Success();
}
```
Add `closeInput()` and `takeWriterError()`:
```cpp
Error ConPty::closeInput()
{
   std::lock_guard<std::mutex> lock(inMutex_);
   if (stopped_.load())
      return systemError(boost::system::errc::not_connected, ERROR_LOCATION);
   closeInputRequested_ = true; // writer closes the pipe after draining the queue
   inCv_.notify_one();
   return Success();
}

Error ConPty::takeWriterError()
{
   std::lock_guard<std::mutex> lock(inMutex_);
   Error e = writerError_;
   writerError_ = Success();
   return e;
}
```

- [ ] **Step 5: Extend `teardownLocked()` to stop the writer**

`start()` now launches the writer thread, so `teardownLocked()` must also stop it (or destroying a joinable thread calls `std::terminate`). Add writer teardown to `teardownLocked()` — abandon queued input, then cancel and join the writer — between the reader wake and the closer:
```cpp
   { std::lock_guard<std::mutex> ol(outMutex_); outCv_.notify_all(); } // wake reader

   // stop the writer: abandon queued input and cancel a blocked WriteFile
   abandon_.store(true);
   {
      std::lock_guard<std::mutex> il(inMutex_);
      inputQueue_.clear();
      inputQueuedBytes_ = 0;
      inCv_.notify_all();
   }
   if (writerThread_.joinable())
   {
      ::CancelSynchronousIo(writerThread_.native_handle());
      writerThread_.join();
   }
```
Also extend the idempotency guard at the top of `teardownLocked()` to include the writer:
```cpp
   if (stopped_.exchange(true) && !running() &&
       !readerThread_.joinable() && !writerThread_.joinable())
      return;
```
(The robust, closer-first/bounded version is finalized in 1d. `start()`'s failure path and `stop()` both already route through `teardownLocked()`, so they get writer teardown for free.)

- [ ] **Step 6: Run tests**

Run: `./rstudio-tests --scope core --filter "Win32ConPtyTest.*"`
Expected: all PASS (echo seen, busy child killed by 0x03, overflow rejected).

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Add ConPty writer thread, bounded input queue, setSize"
```
Handle the roborev review before continuing.

### Task 1d: Hardened teardown, failure paths, race guards

**Files:**
- Modify: `src/cpp/core/system/Win32ConPty.hpp` (declare `teardownLocked`)
- Modify: `src/cpp/core/system/Win32ConPty.cpp` (unify teardown, partial-startup, bounded timeouts)
- Modify: `src/cpp/core/system/Win32ConPtyTests.cpp`

- [ ] **Step 1: Write failing/at-risk tests**

```cpp
TEST(Win32ConPtyTest, ShutdownWhilePausedAtWatermark)
{
   ConPty pty;
   std::vector<std::string> args = {"/c", "for /L %i in (1,1,200000) do @echo PADPADPADPADPADPADPADPADPADPADPADPAD"};
   HANDLE hProc = nullptr;
   ASSERT_FALSE(pty.start(cmdExe(), args, ptyOptions(), &hProc));
   std::this_thread::sleep_for(std::chrono::seconds(1)); // reader pauses at HWM

   auto t0 = std::chrono::steady_clock::now();
   pty.stop(); // must return within the shutdown budget, no hang
   auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(
         std::chrono::steady_clock::now() - t0).count();
   EXPECT_LT(ms, 8000);
   ::CloseHandle(hProc);
}

TEST(Win32ConPtyTest, StartFailsForMissingExeNoLeakNoHang)
{
   ConPty pty;
   std::vector<std::string> args;
   HANDLE hProc = nullptr;
   Error err = pty.start("C:\\nope\\does-not-exist.exe", args, ptyOptions(), &hProc);
   EXPECT_TRUE(err);
   EXPECT_EQ(hProc, nullptr);
   EXPECT_FALSE(pty.running());
   // destructor must not hang
}
```

- [ ] **Step 2: Run (the missing-exe test may already pass; shutdown test guards regressions)**

Run: `./rstudio-tests --scope core --filter "Win32ConPtyTest.ShutdownWhilePausedAtWatermark:Win32ConPtyTest.StartFailsForMissingExeNoLeakNoHang"`
Expected: PASS (or hang/fail if teardown is not yet bounded — proceed to harden).

- [ ] **Step 3: Replace `teardownLocked()` with the hardened body**

`teardownLocked()` already exists (declared in 1a, extended for the writer in 1c) and both `stop()` and `start()`'s failure path already route through it, so only its body changes here. Replace the whole `teardownLocked()` body with the hardened, closer-first, bounded version:
```cpp
void ConPty::teardownLocked()
{
   if (stopped_.exchange(true) && !running() &&
       !readerThread_.joinable() && !writerThread_.joinable())
      return; // already fully torn down (idempotent)

   // 1) Wake a backpressure-paused reader so it keeps draining output (the
   //    closer below waits for the output to drain on pre-24H2 Windows).
   { std::lock_guard<std::mutex> ol(outMutex_); outCv_.notify_all(); }

   // 2) Tell the writer to abandon any queued input.
   abandon_.store(true);
   {
      std::lock_guard<std::mutex> il(inMutex_);
      inputQueue_.clear();
      inputQueuedBytes_ = 0;
      inCv_.notify_all();
   }

   // 3) Close the pseudoconsole on a closer thread FIRST. ClosePseudoConsole
   //    terminates the child and closes ConPTY's pipe ends -- the RELIABLE
   //    unblock for both our writer (broken input pipe) and reader (broken
   //    output pipe). CancelSynchronousIo below is only a backstop. The closer
   //    captures only the HPCON value, so detaching it as a last resort cannot
   //    touch freed members.
   std::thread closer;
   if (hPC_)
   {
      HPCONHANDLE hpc = hPC_;
      closer = std::thread([hpc] { api().close(hpc); });
   }

   // 4) Join the writer. Bounded WITHOUT closing any handle under in-flight IO
   //    (that would be the handle-reuse race the design must avoid; ConPTY
   //    requires synchronous handles, so cancelable overlapped IO is not an
   //    option). The guarantee instead rests on CancelSynchronousIo: a thread
   //    blocked in cancellable synchronous IO is always unblocked by it. We
   //    repeat it every 25ms so the dequeue->WriteFile TOCTOU cannot make it
   //    miss permanently; once the WriteFile is cancelled (or the closer breaks
   //    the input pipe), the writer sees the error or abandon_ and returns. So
   //    the loop is bounded and the following join() cannot hang -- independent
   //    of whether the closer has finished. We close hInputWrite_ only AFTER the
   //    join, when no thread can be using it.
   if (writerThread_.joinable())
   {
      HANDLE wh = writerThread_.native_handle();
      while (::WaitForSingleObject(wh, 25) != WAIT_OBJECT_0)
         ::CancelSynchronousIo(wh);
      writerThread_.join();
   }

   // 5) Reap the closer. The reader is still running and draining, so the closer
   //    can complete even pre-24H2. Detach as a last resort (it holds only the
   //    HPCON value, so this cannot race freed members).
   if (closer.joinable())
   {
      if (::WaitForSingleObject(closer.native_handle(), kShutdownTimeout) == WAIT_OBJECT_0)
         closer.join();
      else
         closer.detach();
   }
   hPC_ = nullptr;

   // 6) Join the reader AFTER the closer, in two bounded phases (also without
   //    closing its handle under IO). Phase 1: wait kShutdownTimeout for a clean
   //    exit -- the closed pseudoconsole breaks the output pipe so ReadFile
   //    returns EOF after the reader has drained the final frame; we do NOT
   //    cancel here, which would abort that drain. Phase 2 (closer slow/stuck):
   //    fall back to the writer's bounded cancel loop -- a cancelled ReadFile
   //    returns !ok, so the reader exits. Either way the following join() is
   //    bounded and cannot hang.
   if (readerThread_.joinable())
   {
      HANDLE rh = readerThread_.native_handle();
      if (::WaitForSingleObject(rh, kShutdownTimeout) != WAIT_OBJECT_0)
      {
         while (::WaitForSingleObject(rh, 25) != WAIT_OBJECT_0)
            ::CancelSynchronousIo(rh);
      }
      readerThread_.join();
   }

   // Threads are joined, so closing the handles now cannot race them. (The
   // writer may already have closed hInputWrite_ on EOF, hence the null guard.)
   if (hInputWrite_) { ::CloseHandle(hInputWrite_); hInputWrite_ = nullptr; }
   if (hOutputRead_) { ::CloseHandle(hOutputRead_); hOutputRead_ = nullptr; }

   if (outputTruncated_)
      LOG_WARNING_MESSAGE("ConPty dropped terminal output during shutdown");
}
```
Note: `std::thread::native_handle()` returns the Win32 `HANDLE` for `WaitForSingleObject`/`CancelSynchronousIo`; do not close it (the `std::thread` owns it). `start()`'s failure path and `stop()` already call `teardownLocked()` (from 1a), so no call-site changes are needed here.

- [ ] **Step 4: Confirm the enqueue race guard**

The race between `writeInput()` and `teardownLocked()` clearing the queue is already closed: the final `writeInput()` (Task 1c Step 4) takes `inMutex_` and checks `stopped_`/`closeInputRequested_` under that lock. No change needed here — just verify the 1c form is in place (the `stopped_.load()` check is inside `inMutex_`, not before it).

- [ ] **Step 5: Add input-rejection-after-EOF and writer-error contract tests**

On testing the writer-error report-once path: exercising `writerExited_`
*distinctly from* `closeInputRequested_` requires the writer to exit via a real
`WriteFile` failure while the session is live. That is not deterministically
reproducible at the unit level -- ConPTY keeps the input read end open until
`ClosePseudoConsole`, so `WriteFile` does not fail merely because the child
exited, and forcing a failure would require a white-box test seam in the
production class (which the project's "test behavior, not implementation"
guidance discourages). So this path is covered by code review plus the Phase 6
manual matrix (paste into a wedged child, then force-close). What IS
deterministic -- and is the user-visible half of finding #3's fix -- is that
input is rejected once the channel is closing, never silently queued to a writer
that is gone:
```cpp
TEST(Win32ConPtyTest, InputRejectedAfterCloseInput)
{
   ConPty pty;
   std::vector<std::string> args; // interactive cmd
   HANDLE hProc = nullptr;
   ASSERT_FALSE(pty.start(cmdExe(), args, ptyOptions(), &hProc));
   drainUntil(pty, ">", 4000);

   ASSERT_FALSE(pty.closeInput());     // request stdin EOF; writer drains then exits
   Error e = pty.writeInput("x");      // must be rejected (closeInputRequested_/writerExited_)
   EXPECT_TRUE(e);                     // not silently queued to a closing writer

   EXPECT_FALSE(pty.takeWriterError()); // healthy session: no spurious writer error

   pty.stop();
   ::CloseHandle(hProc);
}
```
(`takeWriterError()` is what `poll()` calls each tick in Phase 2 to surface a real
write failure; it returns `Success()` when healthy, as asserted here.)

- [ ] **Step 6: Run all class tests**

Run: `./rstudio-tests --scope core --filter "Win32ConPtyTest.*"`
Expected: all PASS, no hangs.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Harden ConPty teardown: closer-first unblock, bounded waits, EOF"
```
Handle the roborev review before continuing.

---

## Phase 2: Swap the backend in `Win32ChildProcess`

This is the atomic backend swap. Because `Win32ChildProcess.cpp` uses the `WinPty` symbol in several places (and the static `WinPty::readFromPty` utility is used even by the non-PTY path), and the whole `core` library builds as one unit, the swap and the deletion of `Win32Pty.*` must land in one building commit. Do all sub-steps, then build once.

### Task 2.1: Replace WinPty with ConPty (atomic)

**Files:**
- Modify: `src/cpp/core/system/Win32ChildProcess.cpp` (include 17; `Impl` 204; `run()` 392-404; `writeToStdin()` 253-284; `poll()` 798-822 and 862-919)
- Modify: `src/cpp/core/CMakeLists.txt` (drop the `Win32Pty.cpp` line added in 1a)
- Delete: `src/cpp/core/system/Win32Pty.hpp`, `src/cpp/core/system/Win32Pty.cpp`, `src/cpp/core/system/Win32PtyTests.cpp`

- [ ] **Step 1: Relocate the non-blocking pipe-read helper**

The non-PTY poll path uses `WinPty::readFromPty`, which is just a non-blocking read utility. Before deleting `Win32Pty.*`, add it as a file-static helper in the anonymous namespace of `Win32ChildProcess.cpp` (near the existing `readPipeUntilDone`):
```cpp
// Non-blocking read of whatever bytes are currently available on a pipe.
// (Relocated from the former WinPty::readFromPty; used by the non-PTY path.)
Error readPipeAvailable(HANDLE hPipe, std::string* pOutput)
{
   DWORD dwAvail = 0;
   if (!::PeekNamedPipe(hPipe, nullptr, 0, nullptr, &dwAvail, nullptr))
   {
      DWORD lastErr = ::GetLastError();
      if (lastErr == ERROR_BROKEN_PIPE)
         return Success();
      return systemError(lastErr, ERROR_LOCATION);
   }
   if (dwAvail == 0)
      return Success();

   DWORD nBytesRead = 0;
   std::vector<char> buffer(dwAvail, 0);
   if (!::ReadFile(hPipe, buffer.data(), dwAvail, &nBytesRead, nullptr))
      return LAST_SYSTEM_ERROR();
   pOutput->append(buffer.data(), nBytesRead);
   return Success();
}
```
Note: the former `readFromPty` used `OVERLAPPED`; the anonymous pipes used here are synchronous, so a plain `ReadFile` after a non-zero `PeekNamedPipe` does not block. (`readPipeUntilDone` remains for the blocking exit drain.)

- [ ] **Step 2: Swap the include (line 17)**

`#include "Win32Pty.hpp"` -> `#include "Win32ConPty.hpp"`

- [ ] **Step 3: Swap the Impl member (line 204)**

`WinPty pty;` -> `ConPty pty;`

- [ ] **Step 4: Update `run()` PTY branch (lines 392-404)**

Replace:
```cpp
      error = pImpl_->pty.start(exe_, args_, options_,
                               &pImpl_->hStdInWrite,
                               &pImpl_->hStdOutRead,
                               &pImpl_->hStdErrRead,
                               &pImpl_->hProcess);
      if (!error)
      {
         pImpl_->pid = ::GetProcessId(pImpl_->hProcess);
      }
      return error;
```
with:
```cpp
      error = pImpl_->pty.start(exe_, args_, options_, &pImpl_->hProcess);
      if (!error)
         pImpl_->pid = ::GetProcessId(pImpl_->hProcess);
      return error;
```

- [ ] **Step 5: Restructure `writeToStdin()` (lines 253-284)**

The PTY path must route input through `ConPty` and route EOF through `ConPty::closeInput()` (closing `hStdInWrite` for the PTY case is wrong now -- ConPTY owns a different input handle, so the child would never see EOF). Replace the whole method body:
```cpp
Error ChildProcess::writeToStdin(const std::string& input, bool eof)
{
   if (options().pseudoterminal)
   {
      if (!input.empty())
      {
         Error error = pImpl_->pty.writeInput(input);
         if (error)
            return error;
      }
      if (eof)
         return pImpl_->pty.closeInput();
      return Success();
   }

   // non-pseudoterminal (anonymous pipe) path -- unchanged behavior
   if (!input.empty())
   {
      DWORD dwWritten;
      BOOL bSuccess = ::WriteFile(pImpl_->hStdInWrite,
                                  input.data(),
                                  static_cast<DWORD>(input.length()),
                                  &dwWritten,
                                  nullptr);
      if (!bSuccess)
         return LAST_SYSTEM_ERROR();
   }
   if (eof)
      return closeHandle(&pImpl_->hStdInWrite, ERROR_LOCATION);
   return Success();
}
```

- [ ] **Step 6: `ptySetSize()`/`ptyInterrupt()` (lines 286-302)**

`pImpl_->pty.setSize(cols, rows)` and `pImpl_->pty.interrupt()` both resolve to the new `ConPty` methods (interrupt is a parity no-op). No edit required beyond the type swap in Step 3; verify they compile.

- [ ] **Step 7: Replace the steady-state read in `poll()` (lines 797-822)**

Replace the two `WinPty::readFromPty` blocks:
```cpp
   // check stdout
   if (pImpl_->hStdOutRead)
   {
      std::string stdOut;
      Error error = WinPty::readFromPty(pImpl_->hStdOutRead, &stdOut);
      if (error)
         reportError(error);

      if (!stdOut.empty() && callbacks_.onStdout)
         callbacks_.onStdout(*this, stdOut);
   }

   // check stderr
   if (pImpl_->hStdErrRead)
   {
      std::string stdErr;
      Error error = WinPty::readFromPty(pImpl_->hStdErrRead, &stdErr);
      if (error)
         reportError(error);

      if (!stdErr.empty() && callbacks_.onStderr)
      {
         hasRecentOutput = true;
         callbacks_.onStderr(*this, stdErr);
      }
   }
```
with:
```cpp
   if (options().pseudoterminal)
   {
      // surface any asynchronous writer (stdin) error promptly
      Error writerErr = pImpl_->pty.takeWriterError();
      if (writerErr)
         reportError(writerErr);

      // ConPTY: single merged VT output stream
      std::string out;
      Error error = pImpl_->pty.readOutput(&out);
      if (error)
         reportError(error);
      if (!out.empty() && callbacks_.onStdout)
      {
         hasRecentOutput = true;
         callbacks_.onStdout(*this, out);
      }
   }
   else
   {
      if (pImpl_->hStdOutRead)
      {
         std::string stdOut;
         Error error = readPipeAvailable(pImpl_->hStdOutRead, &stdOut);
         if (error)
            reportError(error);
         if (!stdOut.empty() && callbacks_.onStdout)
            callbacks_.onStdout(*this, stdOut);
      }
      if (pImpl_->hStdErrRead)
      {
         std::string stdErr;
         Error error = readPipeAvailable(pImpl_->hStdErrRead, &stdErr);
         if (error)
            reportError(error);
         if (!stdErr.empty() && callbacks_.onStderr)
         {
            hasRecentOutput = true;
            callbacks_.onStderr(*this, stdErr);
         }
      }
   }
```

- [ ] **Step 8: Final-drain-on-exit in `poll()` (lines 862-905)**

In the process-exit block, branch the drain by PTY mode. For the PTY case use the ordered teardown + final read; leave the existing non-PTY threaded drain (`readStdOutThread`/`readStdErrThread` using `readPipeUntilDone`) unchanged. Wrap the existing threaded drain in an `else`, and add before it:
```cpp
   if (options().pseudoterminal)
   {
      // This branch runs only after WaitForSingleObject(hProcess) showed the
      // child already exited, so the only output left is what is already in the
      // OS output pipe buffer (tens of KB) -- far below kOutHighWater. The
      // top-of-tick steady-state read above already drained the bulk; stop()
      // then wakes the reader to drain ConPTY's final frame (no re-pause) and
      // joins it. So normal-exit output is not truncated. (Truncation can only
      // occur on terminal *close* with a live, noisy child via ~ConPty -> stop,
      // where the buffer is being discarded anyway.)
      pImpl_->pty.stop();
      std::string out;
      pImpl_->pty.readOutput(&out);
      if (!out.empty() && callbacks_.onStdout)
      {
         hasRecentOutput = true;
         callbacks_.onStdout(*this, out);
      }
   }
   else
   {
      // UNCHANGED: the existing non-PTY exit drain stays exactly as-is here --
      // the std::string stdOut/stdErr locals, the readStdOutThread/
      // readStdErrThread that call readPipeUntilDone, their timed joins, and the
      // onStdout/onStderr dispatch. Do not modify that block; only wrap it in
      // this else so it runs only for non-pseudoterminal children.
   }
```
`Impl::hStdInWrite`/`hStdOutRead`/`hStdErrRead` remain (used by the non-PTY path); the PTY path simply never sets them now. No `Impl` struct change.

- [ ] **Step 9: Delete the winpty class files and drop from CMake**

```bash
git rm src/cpp/core/system/Win32Pty.hpp src/cpp/core/system/Win32Pty.cpp src/cpp/core/system/Win32PtyTests.cpp
```
In `src/cpp/core/CMakeLists.txt`, remove the `system/Win32Pty.cpp` line (leaving `system/Win32ConPty.cpp`).

- [ ] **Step 9b: Bump the persistence version in the SAME commit**

ConPTY's output format (clean VT) differs from winpty's screen-scraped buffers, and ConPTY goes live in this commit -- so the persistence dir bump must land here, not in a later commit, or an intermediate build could load incompatible winpty-era buffers. In `src/cpp/session/SessionConsoleProcessPersist.cpp`, replace:
```cpp
// 2019/07/30 - console06 -> console07
//                Changed shell type from int to string to align with user
//                preferences
#define kConsoleDir "console07"
```
with:
```cpp
// 2019/07/30 - console06 -> console07
//                Changed shell type from int to string to align with user
//                preferences
// 2026/05/30 - console07 -> console08
//                Windows terminal output now comes from ConPTY as clean VT
//                sequences (was winpty screen-scraping); old buffers cannot be
//                replayed under the new renderer, so start fresh.
#define kConsoleDir "console08"
```

- [ ] **Step 10: Build**

Run: `ninja`
Expected: compiles; no remaining `WinPty` references (`grep -n WinPty src/cpp/core/system/Win32ChildProcess.cpp` returns nothing).

- [ ] **Step 11: Smoke test a terminal manually**

Launch the dev build, open a terminal, run `dir`, type a command, resize the pane, press Ctrl+C on a running command; confirm output, echo, resize, and interrupt all work.

- [ ] **Step 12: Run core tests**

Run: `./rstudio-tests --scope core --filter "*ConPty*:*ChildProcess*"`
Expected: PASS (the old `Win32PtyTest.*` are gone).

- [ ] **Step 13: Commit (swap + persistence bump together)**

```bash
git add -A
git commit -m "Swap Windows PTY backend from winpty to ConPty"
```
The persistence bump (Step 9b) is part of this same commit so no intermediate build mixes ConPTY output with the `console07` format. Handle the roborev review.

---

## Phase 3: Session and options layer

### Task 3.1: Trim the `Pseudoterminal` struct (Windows)

**Files:**
- Modify: `src/cpp/core/include/core/system/Process.hpp:65-90`

- [ ] **Step 1: Replace the struct**

Replace lines 65-90 (the `#ifdef _WIN32` form with `winptyPath`, `plainText`, `conerr`):
```cpp
struct Pseudoterminal
{
   Pseudoterminal(
#ifdef _WIN32
         const FilePath& winptyPath,
         bool plainText,
         bool conerr,
#endif
         int cols, int rows)
      :
#ifdef _WIN32
        winptyPath(winptyPath),
        plainText(plainText),
        conerr(conerr),
#endif
        cols(cols), rows(rows)
   {
   }
#ifdef _WIN32
   FilePath winptyPath;
   bool plainText;
   bool conerr;
#endif
   int cols;
   int rows;
};
```
with:
```cpp
struct Pseudoterminal
{
   Pseudoterminal(int cols, int rows)
      : cols(cols), rows(rows)
   {
   }
   int cols;
   int rows;
};
```
(The `#ifdef _WIN32` include of `FilePath` for this struct may now be unused; leave other uses intact.)

- [ ] **Step 2: Update the Win32 terminal construction in `SessionConsoleProcess.cpp:304-310`**

Replace:
```cpp
         // request a pseudoterminal if this is an interactive console process
         options_.pseudoterminal = core::system::Pseudoterminal(
                  session::options().winptyPath(),
                  false /*plainText*/,
                  false /*conerr*/,
                  options_.cols,
                  options_.rows);
```
with:
```cpp
         // request a pseudoterminal if this is an interactive console process
         options_.pseudoterminal = core::system::Pseudoterminal(
                  options_.cols,
                  options_.rows);
```
This makes the Windows and POSIX branches identical; if the surrounding `#ifdef`/`#else` now has two identical bodies, collapse them (verify the `TERM` unset above stays Windows-only as written).

- [ ] **Step 3: Build**

Run: `ninja`
Expected: compiles. (`Win32ConPtyTests.cpp` already constructs `Pseudoterminal(cols, rows)` — now matches.)

- [ ] **Step 4: Run core tests**

Run: `./rstudio-tests --scope core --filter "*ConPty*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/cpp/core/include/core/system/Process.hpp src/cpp/session/SessionConsoleProcess.cpp
git commit -m "Drop winpty fields from Pseudoterminal struct"
```
Handle the roborev review.

### Task 3.2: Remove the `--external-winpty-path` option

**Files:**
- Modify: `src/cpp/session/session-options.json:873-879`
- Modify: `src/cpp/session/SessionOptions.cpp:323,327-345`
- Regenerated: `src/cpp/session/include/session/SessionOptions.gen.hpp`

- [ ] **Step 1: Remove the JSON option (session-options.json:873-879)**

Delete the object:
```json
         {
            "name": "external-winpty-path",
            "type": "core::FilePath",
            "memberName": "winptyPath_",
            "defaultValue": "bin",
            "description": "Specifies the path to winpty binaries."
         }
```
(Remove the leading comma on the now-last element of that array if needed for valid JSON.)

- [ ] **Step 2: Remove the resolution block (SessionOptions.cpp:323-345)**

Delete:
```cpp
   resolvePath(resourcePath_, &winptyPath_);

   // winpty.dll lives next to rsession.exe on a full install; otherwise
   // it lives in a directory named 32 or 64
   core::FilePath pty(winptyPath_);
   std::string completion;
   if (pty.isWithin(resourcePath_))
   {
#ifdef _WIN64
      completion = "winpty.dll";
#else
      completion = "x86/winpty.dll";
#endif
   }
   else
   {
#ifdef _WIN64
      completion = "64/bin/winpty.dll";
#else
      completion = "32/bin/winpty.dll";
#endif
   }
   winptyPath_ = pty.completePath(completion).getAbsolutePath();
```
(Leave the surrounding `#ifdef _WIN32 ... #endif` block and the other resolvePath calls intact.)

- [ ] **Step 3: Regenerate the options header**

Run: `Rscript scripts/generate-options.R`
Expected: `SessionOptions.gen.hpp` no longer contains `winptyPath_`, `winptyPath()`, or `external-winpty-path` (verify with a search).

- [ ] **Step 4: Build**

Run: `ninja`
Expected: compiles. If any stray reference to `options().winptyPath()` remains, remove it (there should be none after Task 3.1).

- [ ] **Step 5: Commit**

```bash
git add src/cpp/session/session-options.json src/cpp/session/SessionOptions.cpp src/cpp/session/include/session/SessionOptions.gen.hpp
git commit -m "Remove --external-winpty-path session option"
```
Handle the roborev review.

---

## Phase 4: Build system and dependency removal

### Task 4.1: Remove winpty from CMake and the dev conf

**Files:**
- Modify: `src/cpp/CMakeLists.txt:373-388`
- Modify: `src/cpp/session/CMakeLists.txt:858-876`
- Modify: `src/cpp/conf/rdesktop-dev.conf:47`

- [ ] **Step 1: Remove the `WINPTY_*` block (src/cpp/CMakeLists.txt:374-388)**

Delete the winpty include-dir block:
```cmake
     # winpty - pseudoterminal support on Windows
     if(RSTUDIO_SESSION_WIN32)
        set(WINPTY_ARCH "32")
     else()
        set(WINPTY_ARCH "64")
     endif()
     set(WINPTY_ROOT "${RSTUDIO_WINDOWS_DEPENDENCIES_DIR}/winpty-0.4.3-msys2-2.7.0")
     set(WINPTY_INCLUDEDIR "${WINPTY_ROOT}/${WINPTY_ARCH}/include")
     set(WINPTY_BINDIR_32 "${WINPTY_ROOT}/32/bin")
     set(WINPTY_BINDIR_64 "${WINPTY_ROOT}/64/bin")

     # add winpty as system include directory
     include_directories(SYSTEM ${WINPTY_INCLUDEDIR})
```
(Keep the surrounding `if(WIN32) ... endif()` and the openssl block that follows.)

- [ ] **Step 2: Remove the install rules (src/cpp/session/CMakeLists.txt:860-876)**

Delete the `# install winpty on windows` block (both the 64-bit `winpty.dll`/`winpty-agent.exe` installs and the 32-bit `x86/` installs). Verify no other rule references `WINPTY_BINDIR_*`.

- [ ] **Step 3: Remove the dev conf line (rdesktop-dev.conf:47)**

Delete:
```
external-winpty-path=${WINPTY_ROOT}
```

- [ ] **Step 4: Reconfigure + build clean**

Run: from build dir, `cmake .` then `ninja` (a fresh configure picks up the removed `WINPTY_ROOT`).
Expected: configures and builds with no winpty references; no missing-variable warnings.

- [ ] **Step 5: Commit**

```bash
git add src/cpp/CMakeLists.txt src/cpp/session/CMakeLists.txt src/cpp/conf/rdesktop-dev.conf
git commit -m "Remove winpty from build system and dev conf"
```
Handle the roborev review.

### Task 4.2: Remove the winpty dependency download

**Files:**
- Modify: `dependencies/windows/install-dependencies.cmd`
- Delete: `dependencies/windows/winpty-0.4.3-msys2-2.7.0.zip`

- [ ] **Step 1: Find and remove the winpty fetch**

Search `dependencies/windows/install-dependencies.cmd` for `winpty` and remove the download/extract lines (the version pin `winpty-0.4.3-msys2-2.7.0` and any `wget`/`unzip` for it).

Run (to locate): `grep -ni winpty dependencies/windows/install-dependencies.cmd`

- [ ] **Step 2: Delete the vendored archive**

```bash
git rm dependencies/windows/winpty-0.4.3-msys2-2.7.0.zip
```

- [ ] **Step 3: Verify no remaining winpty references in the tree**

Run: `grep -rni winpty src/cpp dependencies/windows --include=*.cpp --include=*.hpp --include=*.json --include=*.cmd --include=*.txt --include=*.conf`
Expected: no matches (other than possibly comments you intend to keep — there should be none).

- [ ] **Step 4: Commit**

```bash
git add dependencies/windows/install-dependencies.cmd
git commit -m "Remove vendored winpty dependency"
```
Handle the roborev review.

---

## Phase 5: Frontend buffer-replay re-enable

### Task 5.1: Allow buffer replay for Windows shells

**Files:**
- Modify: `src/gwt/src/org/rstudio/studio/client/workbench/views/terminal/TerminalSession.java:772-800`

- [ ] **Step 1: Confirm the user decision (Phase-5 gate)**

The spec resolves all Windows shells (including `CUSTOM`) to reloadable. Confirm with the user before shipping (per spec 11). If confirmed, proceed; if `CUSTOM` should stay conservative, keep its case as-is and only remove `WIN_CMD`/`WIN_PS`/`PS_CORE`.

- [ ] **Step 2: Edit `shellSupportsReload()`**

Replace:
```java
      switch (consoleProcess_.getProcessInfo().getShellType())
      {
      // Windows command-prompt and PowerShell don't support buffer reloading
      // due to limitations of how they work with WinPty.
      case UserPrefs.WINDOWS_TERMINAL_SHELL_WIN_CMD:
      case UserPrefs.WINDOWS_TERMINAL_SHELL_WIN_PS:
      case UserPrefs.WINDOWS_TERMINAL_SHELL_PS_CORE:
         // Do load the buffer if terminal was just created via API, as
         // the initial message and prompt may have been sent before the
         // client/server channel was opened.
         return createdByApi_;

      case UserPrefs.WINDOWS_TERMINAL_SHELL_CUSTOM:
         // on Windows we don't know if custom shell supports reload so
         // assume it does not
         if (BrowseCap.isWindowsDesktop())
            return createdByApi_;
         else
            return true;

      default:
         return true;
      }
```
with:
```java
      // ConPTY emits clean VT sequences for all Windows shells, so recorded
      // buffers replay faithfully on reconnect (unlike the old WinPty
      // screen-scraped output).
      return true;
```
(If `consoleProcess_ == null` guard above is still present, keep it.)

- [ ] **Step 3: Compile-check the Java**

Run: `cd src/gwt && ant javac`
Expected: BUILD SUCCESSFUL (no unused-import errors for `UserPrefs`/`BrowseCap` — they are used elsewhere in the file; verify).

- [ ] **Step 4: Produce a runnable build and test reconnect**

Run: `cd src/gwt && ant draft`
Then in the dev build: open a cmd/PowerShell terminal, produce output, reload the browser, and confirm scrollback replays.

- [ ] **Step 5: Commit**

```bash
git add src/gwt/src/org/rstudio/studio/client/workbench/views/terminal/TerminalSession.java
git commit -m "Re-enable terminal buffer replay for Windows shells"
```
Handle the roborev review.

---

## Phase 6: Full verification

### Task 6.1: Full C++ + Playwright pass

- [ ] **Step 1: Full build**

Run: `ninja` (from build dir)
Expected: clean build, zero warnings introduced by this work.

- [ ] **Step 2: Run the full core test scope**

Run: `./rstudio-tests --scope core`
Expected: all PASS (including all `Win32ConPtyTest.*`).

- [ ] **Step 3: Run the rsession scope (terminal-adjacent tests)**

Run: `./rstudio-tests --scope rsession --filter "*ConsoleProcess*"`
Expected: PASS (serialization/persistence/socket tests unaffected by the backend swap).

- [ ] **Step 4: Run the Playwright terminal test on Windows**

Per `.claude/skills/rstudio-run-playwright-tests/SKILL.md`, run `e2e/rstudio/tests/panes/terminal/terminal.test.ts` against the Desktop dev build.
Expected: PASS.

- [ ] **Step 5: Manual matrix**

Verify by hand: cmd, pwsh, ps-core, git-bash; a TUI (vim or less) entering/leaving alt-buffer; Ctrl+C on a busy command; pane resize reflows; browser reload replays scrollback (cmd + PowerShell); rsession restart replays from persistence; a large clipboard paste (exercises the writer thread) does not stall the UI.

- [ ] **Step 6: 32-bit session check (if 32-bit is built)**

If `RSTUDIO_SESSION_WIN32` builds are produced, confirm the 32-bit session links and a terminal works. (ConPTY is available on x86; the x86 winpty install is simply gone.)

- [ ] **Step 7: Commit any fixes found**

Commit fixes individually with descriptive messages; handle each roborev review.

---

## Phase 7: Cleanup and NEWS

### Task 7.1: Add NEWS entry

**Files:**
- Modify: `NEWS.md`

- [ ] **Step 1: Add an entry under the appropriate section**

Add under `### Fixed` (or the current dev section), matching existing format:
```
- Replaced the Windows terminal backend (winpty) with the native Windows pseudoconsole (ConPTY), fixing Ctrl+C handling and enabling terminal buffer reload for Command Prompt and PowerShell.
```
(If a tracking issue is confirmed, use the `- ([#NNNN](...)):` form.)

- [ ] **Step 2: Commit**

```bash
git add NEWS.md
git commit -m "Add NEWS entry for ConPTY terminal backend"
```

### Task 7.2: Remove the temporary design-doc artifacts and restore `.gitignore`

**Files:**
- Modify: `.gitignore`
- Delete: `docs/superpowers/specs/2026-05-29-windows-conpty-design.md`, `docs/superpowers/plans/2026-05-29-windows-conpty.md`
- Delete (on disk, never committed): `build-conpty-poc/`

- [ ] **Step 1: Confirm with the user**

The spec and plan were committed only to enable review; per the cleanup obligation they are removed before completion. Confirm the user is ready (they may want to keep them until the PR merges).

- [ ] **Step 2: Restore the single `.gitignore` line**

In `.gitignore`, replace:
```
# TEMP (restore the single "docs/superpowers" line before final cleanup):
# narrowed to allow committing only the ConPTY spec/plan docs.
docs/superpowers/*
!docs/superpowers/specs/
!docs/superpowers/plans/
```
with:
```
docs/superpowers
```

- [ ] **Step 3: Remove the committed docs**

```bash
git rm docs/superpowers/specs/2026-05-29-windows-conpty-design.md docs/superpowers/plans/2026-05-29-windows-conpty.md
```

- [ ] **Step 4: Remove the throwaway POC from disk**

```bash
rm -r build-conpty-poc
```
(It was never committed; this just cleans the working tree.)

- [ ] **Step 5: Commit**

```bash
git add .gitignore
git commit -m "Restore docs/superpowers gitignore; remove ConPTY design docs"
```
Handle the roborev review.

- [ ] **Step 6: Final verification before PR**

Run: `git status` (clean), `grep -rni winpty src/cpp` (no matches), `ninja` (clean), `./rstudio-tests --scope core` (PASS).

---

## Self-review notes (coverage map)

- Spec 4 (API contract) -> Phase 0 POC + Task 1a `launchChild`.
- Spec 5.1 (dynamic loader / version gate) -> Phase 0 (validates 0x601 + macro), Task 1a `api()`.
- Spec 5.2 / 6.1 (reader+writer threads, bounded buffer, backpressure) -> Tasks 1a/1b/1c.
- Spec 5.3 (single output stream) -> Task 2.1.
- Spec 5.4 (rename) -> Tasks 1a + 2.1.
- Spec 5.5 (Ctrl+C via 0x03) -> Task 1c test `CtrlCByteTerminatesBusyChild`; `interrupt()` no-op.
- Spec 6.2 (poll integration + final drain) -> Task 2.1 (Steps 7-8).
- Spec 6.3 / 6.4 (bounded teardown, race guards) -> Task 1d.
- Spec 7 (touch-points) -> Phases 2-5, 7.
- Spec 8 (error handling, partial-startup) -> Tasks 1a/1d.
- Spec 9 (tests) -> Tasks 1a-1d + Phase 6.
- Spec 10 (phasing, persistence in the swap commit) -> Task 2.1 (Step 9b).
- Stdin EOF parity (writeToStdin eof) -> `ConPty::closeInput()` (Task 1c) + Task 2.1 (Step 5).
- Async writer-error surfacing -> `ConPty::takeWriterError()` (Task 1c) + Task 2.1 (Step 7).
- Single-use lifecycle (no reuse after stop) -> `start()` reuse rejection (Task 1a/1d).
- Spec 11 (32-bit, CUSTOM replay, timing) -> Task 5.1 gate + Task 6.1 Step 6.
