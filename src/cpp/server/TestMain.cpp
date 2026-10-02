/*
 * TestMain.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <core/http/AsyncServer.hpp>

#include <server/auth/ServerAuthHandler.hpp>
#include <server/auth/ServerSecureUriHandler.hpp>

#include <core/Log.hpp>
#include <core/system/System.hpp>

#include <gtest/gtest.h>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace uri_handlers {

/* The following method stubs provide definitions for global
 * functions that may be called from server code that is normally
 * defined in ServerMain.cpp, which is replaced with TestMain.cpp
 * when built as the test binary. These functions must not be called
 * by test code, as they will become do-nothing impls. Code under test
 * should not assume the ability to directly interact with the HTTP server.
 */

void add(const std::string& prefix,
         const http::AsyncUriHandlerFunction& handler)
{
}

void addUploadHandler(const std::string& prefix,
         const http::AsyncUriUploadHandlerFunction& handler)
{
}

void addProxyHandler(const std::string& prefix,
                     const http::AsyncUriHandlerFunction& handler)
{
}

void addBlocking(const std::string& prefix,
                 const http::UriHandlerFunction& handler)
{
}

void setDefault(const http::AsyncUriHandlerFunction& handler)
{
}

void setBlockingDefault(const http::UriHandlerFunction& handler)
{
}

void setRequestFilter(const core::http::RequestFilter& filter)
{
}

void setResponseFilter(const core::http::ResponseFilter& filter)
{
}

} // namespace uri_handlers

namespace scheduler {

void addCommand(boost::shared_ptr<ScheduledCommand> pCmd)
{
}

} // namespace scheduler

boost::shared_ptr<http::AsyncServer> server()
{
   return boost::shared_ptr<http::AsyncServer>();
}

} // namespace server
} // namespace rstudio

namespace {

// Parse a log level string (DEBUG, INFO, WARN, ERR/ERROR) into a LogLevel.
// Returns WARN as default if the string is empty or unrecognized.
rstudio::core::log::LogLevel parseLogLevel(const std::string& level)
{
   if (level == "DEBUG")
      return rstudio::core::log::LogLevel::DEBUG;
   else if (level == "INFO")
      return rstudio::core::log::LogLevel::INFO;
   else if (level == "ERR" || level == "ERROR")
      return rstudio::core::log::LogLevel::ERR;
   else if (level == "TRACE")
      return rstudio::core::log::LogLevel::TRACE;
   else if (level == "OFF")
      return rstudio::core::log::LogLevel::OFF;

   std::cerr << "WARNING: Unrecognized log level '" << level
             << "', defaulting to WARN. "
             << "Valid: OFF, ERR, WARN, INFO, DEBUG, TRACE"
             << std::endl;

   return rstudio::core::log::LogLevel::WARN;
}

} // anonymous namespace

int main(int argc, char* argv[])
{
   // Enable stderr logging with --log[=LEVEL] (e.g. --log, --log=DEBUG, --log=INFO).
   // Without --log, no logging is initialized so automation output stays clean.
   // The RSTUDIO_TEST_LOG_LEVEL env var also works (--log flag takes priority).
   // Recognized levels: DEBUG, INFO, WARN (default), ERR/ERROR.
   //
   // --only-database (or RSTUDIO_ONLY_DATABASE=1) restricts the run to the
   // database-backed fixture tests (the *IntegrationTest suites), skipping
   // everything else -- used to rerun just those against a real PostgreSQL
   // instance without repeating the rest of the suite.
   using namespace rstudio::core;
   bool enableLog = false;
   log::LogLevel logLevel = log::LogLevel::WARN;
   bool onlyDatabase = false;

   // Scan argv for our custom flags, removing them so gtest doesn't see them.
   for (int i = 1; i < argc; )
   {
      std::string arg(argv[i]);
      if (arg == "--log")
      {
         enableLog = true;
         // Shift remaining args down
         for (int j = i; j < argc - 1; ++j)
            argv[j] = argv[j + 1];
         --argc;
      }
      else if (arg.substr(0, 6) == "--log=")
      {
         enableLog = true;
         logLevel = parseLogLevel(arg.substr(6));
         for (int j = i; j < argc - 1; ++j)
            argv[j] = argv[j + 1];
         --argc;
      }
      else if (arg == "--only-database")
      {
         onlyDatabase = true;
         for (int j = i; j < argc - 1; ++j)
            argv[j] = argv[j + 1];
         --argc;
      }
      else
      {
         ++i;
      }
   }
   argv[argc] = nullptr;

   // Fall back to env vars if the flags were not specified
   if (!enableLog)
   {
      const char* logLevelEnv = std::getenv("RSTUDIO_TEST_LOG_LEVEL");
      if (logLevelEnv)
      {
         enableLog = true;
         logLevel = parseLogLevel(std::string(logLevelEnv));
      }
   }

   if (!onlyDatabase)
   {
      const char* onlyDatabaseEnv = std::getenv("RSTUDIO_ONLY_DATABASE");
      if (onlyDatabaseEnv &&
          (std::string(onlyDatabaseEnv) == "1" || std::string(onlyDatabaseEnv) == "true"))
      {
         onlyDatabase = true;
      }
   }

   if (enableLog)
   {
      std::string programId = "rserver-tests";
      log::setProgramId(programId);
      Error error = system::initializeStderrLog(programId, logLevel, true);
      if (error)
         std::cerr << "WARNING: Failed to initialize logging: "
                   << error.getMessage() << std::endl;
   }

   testing::InitGoogleTest(&argc, argv);

   if (onlyDatabase)
      GTEST_FLAG_SET(filter, "*IntegrationTest*");

   return RUN_ALL_TESTS();
}
