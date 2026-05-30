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
