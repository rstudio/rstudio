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
#include <cstddef>
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

// Native Windows pseudoconsole (ConPTY) wrapper. Replaces the winpty-based
// WinPty class. Owns the pseudoconsole, its input/output pipes, and a reader
// thread. Output is buffered (bounded, with backpressure) and drained by the
// caller via readOutput(); input is written synchronously on the caller thread.
class ConPty : boost::noncopyable
{
public:
   ConPty() = default;
   ~ConPty();

   // True if ConPTY is available on this OS (functions resolved from kernel32).
   static bool isAvailable();

   // Create the pseudoconsole + pipes, launch the reader thread, and spawn the
   // child. On success *pProcess receives the child process handle (caller owns
   // it). On failure all internal state is torn down and *pProcess is null.
   Error start(const std::string& exe,
               const std::vector<std::string>& args,
               const ProcessOptions& options,
               HANDLE* pProcess);

   // True between a successful start() and teardown. Observes the lifecycle
   // state under stateMutex_, so it is safe to call from any thread.
   bool running() const;

   // Non-blocking: move buffered output into *pOutput (may be empty).
   Error readOutput(std::string* pOutput);

   // Synchronously write input to the child's stdin on the caller thread
   // (blocking WriteFile, matching the prior winpty behavior). Returns an error
   // if the input channel is closed or the write fails.
   Error writeInput(const std::string& input);

   // Request EOF on the child's stdin by closing our input write end (parity
   // with the old winpty conin-close on eof).
   Error closeInput();

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
   void teardownLocked();   // single ordered teardown path; caller holds stateMutex_

   HPCON hPC_ = nullptr;
   HANDLE hInputWrite_ = nullptr;    // caller thread writes here (synchronous)
   HANDLE hOutputRead_ = nullptr;    // reader thread reads here

   std::thread readerThread_;

   mutable std::mutex stateMutex_;   // serializes start/stop/setSize; guards hPC_
   std::mutex inputMutex_;           // guards hInputWrite_ (input may arrive on the WS thread)
   std::atomic<bool> stopped_{false};

   std::mutex outMutex_;
   std::condition_variable outCv_;
   std::string outputBuffer_;
   std::size_t outputDroppedBytes_ = 0;  // bytes dropped if forced shutdown truncated output
};

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_WIN32_CONPTY_HPP
