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

#include <atomic>
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
   // Keep the child alive ~1s after echoing: a sub-millisecond child hits the
   // documented ConPTY fast-exit attach race (it can exit before it finishes
   // binding to the pseudoconsole, leaking output to the host console). A child
   // that lives >~1s binds deterministically. See the plan's Phase 0 results.
   std::vector<std::string> args = {"/c", "echo HELLO_CONPTY & ping -n 2 127.0.0.1 >nul"};
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

TEST(Win32ConPtyTest, InputRejectedAfterCloseInput)
{
   ConPty pty;
   std::vector<std::string> args; // interactive cmd
   HANDLE hProc = nullptr;
   ASSERT_FALSE(pty.start(cmdExe(), args, ptyOptions(), &hProc));
   drainUntil(pty, ">", 4000);

   ASSERT_FALSE(pty.closeInput());     // signal stdin EOF by closing the write end
   Error e = pty.writeInput("x");      // must be rejected (hInputWrite_ is now null)
   EXPECT_TRUE(e);                     // not silently written to a closed channel

   pty.stop();
   ::CloseHandle(hProc);
}

TEST(Win32ConPtyTest, ConcurrentWriteAndStopIsSafe)
{
   ConPty pty;
   std::vector<std::string> args; // interactive cmd
   HANDLE hProc = nullptr;
   ASSERT_FALSE(pty.start(cmdExe(), args, ptyOptions(), &hProc));
   drainUntil(pty, ">", 4000);

   // Hammer writeInput from another thread while the main thread tears down.
   std::atomic<bool> done{false};
   std::thread writer([&]{
      while (!done.load())
         pty.writeInput("echo X\r\n"); // returns an error once stopped; never crashes
   });
   std::this_thread::sleep_for(std::chrono::milliseconds(50));
   pty.stop();          // closes hInputWrite_ concurrently with the writer
   done.store(true);
   writer.join();       // must complete (no hang)
   EXPECT_FALSE(pty.running());
   ::CloseHandle(hProc);
}

TEST(Win32ConPtyTest, ConcurrentWriteAndCloseInputIsSafe)
{
   ConPty pty;
   std::vector<std::string> args; // interactive cmd
   HANDLE hProc = nullptr;
   ASSERT_FALSE(pty.start(cmdExe(), args, ptyOptions(), &hProc));
   drainUntil(pty, ">", 4000);

   std::atomic<bool> done{false};
   std::thread writer([&]{
      while (!done.load())
         pty.writeInput("echo X\r\n"); // returns an error after closeInput; never blocks the closer
   });
   std::this_thread::sleep_for(std::chrono::milliseconds(50));

   // closeInput() must return promptly even with writes in flight (watchdog).
   std::atomic<bool> closed{false};
   std::thread closeThread([&]{ pty.closeInput(); closed.store(true); });
   auto deadline = std::chrono::steady_clock::now() + std::chrono::seconds(5);
   while (!closed.load() && std::chrono::steady_clock::now() < deadline)
      std::this_thread::sleep_for(std::chrono::milliseconds(10));
   EXPECT_TRUE(closed.load()) << "closeInput() did not return promptly";

   done.store(true);
   writer.join();
   closeThread.join();
   pty.stop();
   ::CloseHandle(hProc);
}

TEST(Win32ConPtyTest, RoutesOutputWhenHostStdHandlesRedirected)
{
   // Simulate the desktop/GUI launch context: the host process's std handles are
   // redirected to a (non-console) file at spawn time. The child must still
   // attach to the pseudoconsole rather than inherit the host's file handle.
   wchar_t tmpDir[MAX_PATH];
   wchar_t tmpFile[MAX_PATH];
   ::GetTempPathW(MAX_PATH, tmpDir);
   ::GetTempFileNameW(tmpDir, L"cpt", 0, tmpFile);
   HANDLE hFile = ::CreateFileW(tmpFile, GENERIC_WRITE, FILE_SHARE_READ | FILE_SHARE_WRITE,
                                nullptr, CREATE_ALWAYS, FILE_ATTRIBUTE_TEMPORARY, nullptr);
   ASSERT_NE(hFile, INVALID_HANDLE_VALUE);

   HANDLE savedIn  = ::GetStdHandle(STD_INPUT_HANDLE);
   HANDLE savedOut = ::GetStdHandle(STD_OUTPUT_HANDLE);
   HANDLE savedErr = ::GetStdHandle(STD_ERROR_HANDLE);

   ConPty pty;
   std::vector<std::string> args = {"/c", "echo HELLO_REDIR & ping -n 2 127.0.0.1 >nul"};
   HANDLE hProc = nullptr;

   ::SetStdHandle(STD_INPUT_HANDLE, hFile);
   ::SetStdHandle(STD_OUTPUT_HANDLE, hFile);
   ::SetStdHandle(STD_ERROR_HANDLE, hFile);
   Error err = pty.start(cmdExe(), args, ptyOptions(), &hProc);
   // restore IMMEDIATELY so gtest output/assertions are not redirected
   ::SetStdHandle(STD_INPUT_HANDLE, savedIn);
   ::SetStdHandle(STD_OUTPUT_HANDLE, savedOut);
   ::SetStdHandle(STD_ERROR_HANDLE, savedErr);

   ASSERT_FALSE(err) << err.asString();
   std::string out = drainUntil(pty, "HELLO_REDIR", 5000);
   EXPECT_NE(out.find("HELLO_REDIR"), std::string::npos)
       << "child output did not route through the pseudoconsole; got: " << out;

   pty.stop();
   ::CloseHandle(hProc);
   ::CloseHandle(hFile);
   ::DeleteFileW(tmpFile);
}

} // namespace tests
} // namespace system
} // namespace core
} // namespace rstudio

#endif // _WIN32
