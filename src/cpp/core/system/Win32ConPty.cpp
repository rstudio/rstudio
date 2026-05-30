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

   // Launch reader and writer BEFORE CreateProcess so a child is never live
   // without a draining reader.
   readerThread_ = std::thread([this] { readerLoop(); });
   writerThread_ = std::thread([this] { writerLoop(); });

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

} // namespace system
} // namespace core
} // namespace rstudio
