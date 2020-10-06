/*
 * ProcessTests.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef _WIN32

#include <atomic>

#include <boost/bind.hpp>
#include <boost/thread.hpp>

#include <shared_core/SafeConvert.hpp>
#include <core/system/PosixProcess.hpp>
#include <core/system/PosixChildProcess.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/Thread.hpp>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace tests {

void checkExitCode(int exitCode, int* outExitCode)
{
   *outExitCode = exitCode;
}

void signalExit(int exitCode, int* outExitCode, boost::mutex* mutex, boost::condition_variable* signal)
{
   *outExitCode = exitCode;

   LOCK_MUTEX(*mutex)
   {
      signal->notify_all();
   }
   END_LOCK_MUTEX
}

void appendOutput(const std::string& output, std::string* pOutput)
{
   pOutput->append(output);
}

struct IoServiceFixture
{
   boost::asio::io_service ioService;
   boost::asio::io_service::work work;
   std::vector<boost::shared_ptr<boost::thread> > threads;

   void runServiceThread()
   {
      ioService.run();
   }

   IoServiceFixture() :
      ioService(), work(ioService)
   {
      for (int i = 0; i < 4; ++i)
      {
         boost::shared_ptr<boost::thread> pThread(
                  new boost::thread(boost::bind(&IoServiceFixture::runServiceThread, this)));
         threads.push_back(pThread);
      }
   }

   ~IoServiceFixture()
   {
      ioService.stop();
      for (const boost::shared_ptr<boost::thread>& thread : threads)
         thread->join();
   }
};

test_context("ProcessTests")
{
   test_that("AsioProcessSupervisor can run program")
   {
      IoServiceFixture fixture;

      // create new supervisor
      AsioProcessSupervisor supervisor(fixture.ioService);

      // create process options and callbacks
      ProcessOptions options;
      options.threadSafe = true;

      ProcessCallbacks callbacks;
      int exitCode = -1;
      callbacks.onExit = boost::bind(&checkExitCode, _1, &exitCode);

      // construct program arguments
      std::vector<std::string> args;
      args.push_back("Hello, world! This is a string to echo!");

      // run program
      supervisor.runProgram("/bin/echo", args, options, callbacks);

      // wait for it to exit
      bool success = supervisor.wait(boost::posix_time::seconds(5));

      // verify process exited successfully
      CHECK(success);
      CHECK(exitCode == 0);
   }

   test_that("AsioProcessSupervisor returns correct output from stdout")
   {
      IoServiceFixture fixture;

      // create new supervisor
      AsioProcessSupervisor supervisor(fixture.ioService);

      // create process options and callbacks
      ProcessOptions options;
      options.threadSafe = true;

      ProcessCallbacks callbacks;

      int exitCode = -1;
      std::string output;

      callbacks.onExit = boost::bind(&checkExitCode, _1, &exitCode);
      callbacks.onStdout = boost::bind(&appendOutput, _2, &output);

      // run command
      std::string command = "bash -c \"python -c $'for i in range(10):\n   print(i)'\"";
      supervisor.runCommand(command, options, callbacks);

      // wait for it to exit
      bool success = supervisor.wait(boost::posix_time::seconds(5));

      // verify process exited successfully and we got the expected output
      std::string expectedOutput = "0\n1\n2\n3\n4\n5\n6\n7\n8\n9\n";
      CHECK(success);
      CHECK(exitCode == 0);
      CHECK(output == expectedOutput);
   }

   /* test running child process as another user
    * commented out due to users being different on every machine
   test_that("AsioProcessSupervisor can run process as another user")
   {
      IoServiceFixture fixture;

      // create new supervisor
      AsioProcessSupervisor supervisor(fixture.ioService);

      // create process options and callbacks
      ProcessOptions options;
      options.runAsUser = "jdoe1";
      ProcessCallbacks callbacks;

      int exitCode = -1;
      std::string output;

      callbacks.onExit = boost::bind(&checkExitCode, _1, &exitCode);
      callbacks.onStdout = boost::bind(&appendOutput, _2, &output);

      // run command
      std::string command = "whoami";
      supervisor.runCommand(command, options, callbacks);

      // wait for it to exit
      bool success = supervisor.wait(boost::posix_time::seconds(5));

      // verify process exited successfully and we got the expected output
      std::string expectedOutput = "jdoe1\n";
      CHECK(success);
      CHECK(exitCode == 0);
      CHECK(output == expectedOutput);
   }
   */

   test_that("AsioProcessSupervisor returns correct error code for failure exit")
   {
      IoServiceFixture fixture;

      // create new supervisor
      AsioProcessSupervisor supervisor(fixture.ioService);

      // create process options and callbacks
      ProcessOptions options;
      options.threadSafe = true;

      ProcessCallbacks callbacks;

      int exitCode = -1;
      std::string output;

      callbacks.onExit = boost::bind(&checkExitCode, _1, &exitCode);

      // run command
      std::string command = "this is not a valid command";
      supervisor.runCommand(command, options, callbacks);

      // wait for it to exit
      bool success = supervisor.wait(boost::posix_time::seconds(5));

      CHECK(success);
      CHECK(exitCode == 127);
   }

   test_that("AsioAsyncChildProcess can write to std in")
   {
      IoServiceFixture fixture;

      ProcessOptions options;
      options.threadSafe = true;

      options.threadSafe = true;

      ProcessCallbacks callbacks;

      int exitCode = -1;
      std::string output;
      boost::condition_variable signal;
      boost::mutex mutex;

      callbacks.onExit = boost::bind(&signalExit, _1, &exitCode, &mutex, &signal);
      callbacks.onStdout = boost::bind(&appendOutput, _2, &output);

      AsioAsyncChildProcess proc(fixture.ioService, "cat", options);
      proc.run(callbacks);

      proc.asyncWriteToStdin("Hello\n", false);
      proc.asyncWriteToStdin("world!\n", true);

      std::string expectedOutput = "Hello\nworld!\n";

      boost::unique_lock<boost::mutex> lock(mutex);
      bool timedOut = !signal.timed_wait<boost::posix_time::seconds>(lock, boost::posix_time::seconds(5),
                                                                     [&](){return exitCode == 0;});

      CHECK(!timedOut);
      CHECK(exitCode == 0);
      CHECK(output == expectedOutput);
   }

   test_that("Can spawn multiple sync processes and they all return correct results")
   {
      // create new supervisor
      ProcessSupervisor supervisor;

      int exitCodes[10];
      std::string outputs[10];
      for (int i = 0; i < 10; ++i)
      {
         // construct program arguments
         std::vector<std::string> args;
         args.push_back("Hello, " + safe_convert::numberToString(i));

         // create process options and callbacks
         ProcessOptions options;
         options.threadSafe = true;

         ProcessCallbacks callbacks;

         callbacks.onExit = boost::bind(&checkExitCode, _1, exitCodes + i);
         callbacks.onStdout = boost::bind(&appendOutput, _2, outputs + i);

         // run program
         supervisor.runProgram("/bin/echo", args, options, callbacks);
      }

      // wait for processes to exit
      bool success = supervisor.wait();
      CHECK(success);

      // verify correct exit statuses and outputs
      for (int i = 0; i < 10; ++i)
      {
         CHECK(exitCodes[i] == 0);
         CHECK(outputs[i] == "Hello, " + safe_convert::numberToString(i) + "\n");
      }
   }

   test_that("Can spawn multiple async processes and they all return correct results")
   {
      IoServiceFixture fixture;

      std::string asioType;
      #if defined(BOOST_ASIO_HAS_IOCP)
        asioType = "iocp";
      #elif defined(BOOST_ASIO_HAS_EPOLL)
        asioType = "epoll";
      #elif defined(BOOST_ASIO_HAS_KQUEUE)
        asioType = "kqueue";
      #elif defined(BOOST_ASIO_HAS_DEV_POLL)
        asioType = "/dev/poll";
      #else
        asioType = "select";
      #endif
      std::cout << "Using asio type: " << asioType << std::endl;

      // determine open files limit
      RLimitType soft, hard;
      Error error = core::system::getResourceLimit(core::system::FilesLimit, &soft, &hard);
      REQUIRE_FALSE(error);

      // ensure we set the hard limit
      error = core::system::setResourceLimit(core::system::FilesLimit, ::fmin(10000, hard));
      REQUIRE_FALSE(error);

      // spawn amount of processes proportional to the hard limit
      const int numProcs = ::fmin(hard / 6, 1000);
      std::cout << "Spawning " << numProcs << " child procs" << std::endl;

      // create new supervisor
      AsioProcessSupervisor supervisor(fixture.ioService);

      int exitCodes[1000];
      std::string outputs[1000];
      std::atomic<int> numExited(0);
      int numError = 0;

      Error lastError;
      for (int i = 0; i < numProcs; ++i)
      {
         // set exit code to some initial bad value to ensure it is properly set when
         // the process exits
         exitCodes[i] = -1;

         // construct program arguments
         std::vector<std::string> args;
         args.push_back("Hello, " + safe_convert::numberToString(i));

         // create process options and callbacks
         ProcessOptions options;
         options.threadSafe = true;

         ProcessCallbacks callbacks;

         callbacks.onExit = [&exitCodes, &numExited, i](int exitCode) {
            exitCodes[i] = exitCode;
            numExited++;
         };

         callbacks.onStdout = boost::bind(&appendOutput, _2, outputs + i);

         // run program
         Error error = supervisor.runProgram("/bin/echo", args, options, callbacks);
         if (error)
         {
            numError++;
            lastError = error;
         }
      }

      if (lastError)
         std::cout << lastError.getSummary() << " " << lastError.getLocation().asString() << std::endl;

      CHECK(numError == 0);

      // wait for processes to exit
      bool success = supervisor.wait(boost::posix_time::seconds(60));
      CHECK(success);

      // check to make sure all processes really exited
      CHECK(numExited == numProcs);

      // verify correct exit statuses and outputs
      for (int i = 0; i < numProcs; ++i)
      {
         CHECK(exitCodes[i] == 0);
         CHECK(outputs[i] == "Hello, " + safe_convert::numberToString(i) + "\n");
      }
   }

   test_that("Can kill child processes")
   {
      IoServiceFixture fixture;

      // create new supervisor
      AsioProcessSupervisor supervisor(fixture.ioService);

      std::atomic<int> numStarted(0);
      std::atomic<int> numExited(0);
      int numError = 0;

      Error lastError;
      for (int i = 0; i < 10; ++i)
      {
         // create process options and callbacks
         ProcessOptions options;
         options.threadSafe = true;

         ProcessCallbacks callbacks;

         callbacks.onStdout = [&numStarted](ProcessOperations&, const std::string& out) {
            ++numStarted;
         };

         callbacks.onExit = [&numExited](int exitCode) {
            ++numExited;
         };

         // run program
         Error error = supervisor.runCommand("echo hello && sleep 60", options, callbacks);
         if (error)
         {
            numError++;
            lastError = error;
         }
      }

      if (lastError)
         std::cout << lastError.getSummary() << " " << lastError.getLocation().asString() << std::endl;

      CHECK(numError == 0);

      // wait for processes to start
      int numTries = 0;
      while (numStarted < 10)
      {
         sleep(1);

         // make sure we fail out if the processes don't all start
         REQUIRE((++numTries != 10));
      }

      // kill the child processes
      Error error = core::system::terminateChildProcesses();
      REQUIRE(!error);

      // wait for processes to exit
      bool success = supervisor.wait(boost::posix_time::seconds(10));
      CHECK(success);

      // check to make sure all processes really exited
      CHECK(numExited == 10);
   }
}

} // end namespace tests
} // end namespace system
} // end namespace core
} // end namespace rstudio

#endif // !_WIN32
