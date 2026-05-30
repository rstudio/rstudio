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
