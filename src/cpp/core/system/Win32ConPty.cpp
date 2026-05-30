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

   // Launch the reader BEFORE CreateProcess so a child is never live without a
   // draining reader.
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
         // ERROR_OPERATION_ABORTED is also normal here: teardownLocked() uses
         // CancelSynchronousIo on this thread as a backstop, which aborts the
         // in-flight ReadFile. It is the only caller that cancels reader IO, so
         // suppressing it cannot mask a genuine read failure.
         DWORD err = ::GetLastError();
         if (!ok && n == 0 && err != ERROR_BROKEN_PIPE &&
             err != ERROR_HANDLE_EOF && err != ERROR_OPERATION_ABORTED)
            LOG_ERROR(systemError(err, ERROR_LOCATION));
         std::lock_guard<std::mutex> lock(outMutex_);
         outCv_.notify_all();
         return;
      }
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
   }
}

Error ConPty::readOutput(std::string* pOutput)
{
   std::lock_guard<std::mutex> lock(outMutex_);
   pOutput->clear();
   pOutput->swap(outputBuffer_);
   outCv_.notify_all(); // release a paused reader (1b)
   return Success();
}

Error ConPty::writeInput(const std::string& input)
{
   if (input.empty())
      return Success();
   // Duplicate the input handle under the lock, then perform the (possibly
   // blocking) WriteFile on the duplicate OUTSIDE the lock. This keeps a stalled
   // write from holding inputMutex_, so closeInput()/teardown can close the
   // original handle without waiting. The duplicate keeps the pipe's write end
   // alive until the write completes (correct EOF-after-pending-write semantics);
   // if the child/conhost is wedged, teardown's ClosePseudoConsole breaks the
   // pipe and the write fails. Terminal input may arrive on the websocket thread
   // (ConsoleProcess::onReceivedInput), so this must be thread-safe.
   HANDLE dup = nullptr;
   {
      std::lock_guard<std::mutex> lock(inputMutex_);
      if (stopped_.load() || hInputWrite_ == nullptr)
         return systemError(boost::system::errc::not_connected,
                            "ConPty input channel is closed", ERROR_LOCATION);
      if (!::DuplicateHandle(::GetCurrentProcess(), hInputWrite_,
                             ::GetCurrentProcess(), &dup, 0, FALSE,
                             DUPLICATE_SAME_ACCESS))
         return LAST_SYSTEM_ERROR();
   }
   DWORD written = 0;
   BOOL ok = ::WriteFile(dup, input.data(),
                         static_cast<DWORD>(input.size()), &written, nullptr);
   Error err = ok ? Success() : LAST_SYSTEM_ERROR();
   ::CloseHandle(dup);
   return err;
}

Error ConPty::closeInput()
{
   std::lock_guard<std::mutex> lock(inputMutex_);
   if (hInputWrite_)
   {
      ::CloseHandle(hInputWrite_);
      hInputWrite_ = nullptr; // signals stdin EOF to the child
   }
   return Success();
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

void ConPty::teardownLocked()
{
   if (stopped_.exchange(true) && !running() && !readerThread_.joinable())
      return; // already torn down (idempotent)

   // Wake a backpressure-paused reader so it keeps draining (the closer below
   // waits for the output to drain on pre-24H2 Windows).
   { std::lock_guard<std::mutex> ol(outMutex_); outCv_.notify_all(); }

   // Close the pseudoconsole on a closer thread that captures only the HPCON
   // value (so detaching it as a last resort cannot touch freed members). This
   // terminates the child and breaks the output pipe, unblocking the reader.
   std::thread closer;
   if (hPC_)
   {
      HPCONHANDLE hpc = hPC_;
      closer = std::thread([hpc] { api().close(hpc); });
   }

   // Reap the closer (reader is still draining, so it completes even pre-24H2).
   // Detach as a last resort -- it holds only the HPCON value.
   if (closer.joinable())
   {
      if (::WaitForSingleObject(closer.native_handle(), kShutdownTimeout) == WAIT_OBJECT_0)
         closer.join();
      else
         closer.detach();
   }
   hPC_ = nullptr;

   // Join the reader: with the pseudoconsole closed its ReadFile returns EOF;
   // a bounded CancelSynchronousIo loop is the backstop. join() then cannot hang.
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

   {
      std::lock_guard<std::mutex> il(inputMutex_);
      if (hInputWrite_) { ::CloseHandle(hInputWrite_); hInputWrite_ = nullptr; }
   }
   if (hOutputRead_) { ::CloseHandle(hOutputRead_); hOutputRead_ = nullptr; }

   if (outputTruncated_)
      LOG_WARNING_MESSAGE("ConPty dropped terminal output during shutdown");
}

} // namespace system
} // namespace core
} // namespace rstudio
