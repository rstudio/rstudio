/*
 * LoggingTests.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include <tests/TestThat.hpp>

#include <core/Log.hpp>
#include <core/LogOptions.hpp>

#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>
#include <core/system/System.hpp>

#include <shared_core/DateTime.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>
#include <shared_core/SafeConvert.hpp>

#include <boost/algorithm/string.hpp>

namespace rstudio {
namespace core {
namespace unit_tests {

void clearLogEnvVars()
{
   core::system::setenv("RS_LOG_LEVEL", std::string());
   core::system::setenv("RS_LOGGER_TYPE", std::string());
   core::system::setenv("RS_LOG_MESSAGE_FORMAT", std::string());
   core::system::setenv("RS_LOG_DIR", std::string());
   core::system::setenv("RS_LOG_CONF_FILE", std::string());
}

std::string zeroPad(const std::string& in, const unsigned int width)
{
   if (in.length() >= width)
      return in;

   std::ostringstream oss;
   oss << std::setw(width) << std::setfill('0') << in;

   return oss.str();
}

test_context("Logging")
{
   test_that("Can find logging.conf from env var")
   {
      FilePath tmpConfPath;
      REQUIRE_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

      std::string confFileContents =
            "[*]\n"
            "logger-type=file\n"
            "log-level=info\n"
            "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

      REQUIRE_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

      clearLogEnvVars();
      core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

      std::string id = core::system::generateShortenedUuid();
      REQUIRE_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));

      LOG_DEBUG_MESSAGE("Debug message");
      LOG_INFO_MESSAGE("Info message");
      LOG_WARNING_MESSAGE("Warning message");
      LOG_ERROR_MESSAGE("Error message");

      FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
      REQUIRE(logFile.exists());

      std::string logFileContents;
      REQUIRE_FALSE(core::readStringFromFile(logFile, &logFileContents));

      REQUIRE(logFileContents.find("Debug message") == std::string::npos);
      REQUIRE(logFileContents.find("Info message") != std::string::npos);
      REQUIRE(logFileContents.find("Warning message") != std::string::npos);
      REQUIRE(logFileContents.find("Error message") != std::string::npos);
   }

   test_that("Can override logging based on env vars and write valid json format")
   {
      FilePath tmpConfPath;
      REQUIRE_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

      std::string confFileContents =
            "[*]\n"
            "logger-type=file\n"
            "log-level=info\n"
            "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

      REQUIRE_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

      clearLogEnvVars();
      core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());
      core::system::setenv("RS_LOG_LEVEL", "DEBUG");
      core::system::setenv("RS_LOG_MESSAGE_FORMAT", "JSON");
      core::system::setenv("RS_LOGGER_TYPE", "FILE");

      FilePath logDir;
      REQUIRE_FALSE(FilePath::tempFilePath(logDir));
      core::system::setenv("RS_LOG_DIR", logDir.getAbsolutePath());

      std::string id = core::system::generateShortenedUuid();
      REQUIRE_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
      REQUIRE_FALSE(core::system::reinitLog());

      LOG_DEBUG_MESSAGE("Debug message");
      LOG_INFO_MESSAGE("Info message");
      LOG_WARNING_MESSAGE("Warning message");
      LOG_ERROR_MESSAGE("Error message");

      FilePath logFile = logDir.completeChildPath("logging-tests-" + id + ".log");
      REQUIRE(logFile.exists());

      std::vector<std::string> lines;
      REQUIRE_FALSE(core::readStringVectorFromFile(logFile, &lines));
      REQUIRE_FALSE(lines.empty());

      for (const std::string& line : lines)
      {
         json::Object obj;
         REQUIRE_FALSE(obj.parse(line));

         REQUIRE(obj.hasMember("time"));
         REQUIRE(obj.hasMember("service"));
         REQUIRE(obj.hasMember("level"));
         REQUIRE(obj.hasMember("message"));

         REQUIRE(obj["service"].getString() == "logging-tests-" + id);
         REQUIRE((obj["message"].getString() == "Debug message" ||
                  obj["message"].getString() == "Info message" ||
                  obj["message"].getString() == "Warning message" ||
                  obj["message"].getString() == "Error message"));
      }

#ifndef _WIN32
      test_that("Can reload logging configuration")
      {
         FilePath tmpConfPath;
         REQUIRE_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

         std::string confFileContents =
               "[*]\n"
               "logger-type=file\n"
               "log-level=warn\n"
               "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

         REQUIRE_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

         clearLogEnvVars();
         core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

         std::string id = core::system::generateShortenedUuid();
         REQUIRE_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
         REQUIRE_FALSE(core::system::reinitLog());

         LOG_DEBUG_MESSAGE("Debug message");
         LOG_INFO_MESSAGE("Info message");
         LOG_WARNING_MESSAGE("Warning message");
         LOG_ERROR_MESSAGE("Error message");

         FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
         REQUIRE(logFile.exists());

         std::string logFileContents;
         REQUIRE_FALSE(core::readStringFromFile(logFile, &logFileContents));

         REQUIRE(logFileContents.find("Debug message") == std::string::npos);
         REQUIRE(logFileContents.find("Info message") == std::string::npos);
         REQUIRE(logFileContents.find("Warning message") != std::string::npos);
         REQUIRE(logFileContents.find("Error message") != std::string::npos);

         boost::replace_all(confFileContents, "warn", "debug");
         REQUIRE_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

         // reload logging configuration by sending SIGHUP to ourselves
         // we also have to wait awhile because this is an asynchronous process handled by another thread
         core::system::sendSignalToSelf(core::system::SigHup);
         bool success = false;
         for (int i = 0; i < 5; ++i)
         {
            boost::this_thread::sleep(boost::posix_time::milliseconds(250));

            LOG_DEBUG_MESSAGE("Debug message");
            LOG_INFO_MESSAGE("Info message");

            REQUIRE_FALSE(core::readStringFromFile(logFile, &logFileContents));

            success =
                  logFileContents.find("Debug message") != std::string::npos &&
                  logFileContents.find("Info message") != std::string::npos;

            if (success)
               break;
         }

         REQUIRE(success);
      }
#endif

      test_that("Can log to named loggers")
      {
         FilePath tmpConfPath;
         REQUIRE_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

         std::string confFileContents =
               "[*]\n"
               "logger-type=file\n"
               "log-level=warn\n"
               "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

         confFileContents += "\n\n"
               "[Bob]\n"
               "log-level=error\n\n"
               "[Jill]\n"
               "log-level=info";

         REQUIRE_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

         clearLogEnvVars();
         core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

         std::string id = core::system::generateShortenedUuid();
         REQUIRE_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
         REQUIRE_FALSE(core::system::reinitLog());

         LOG_DEBUG_MESSAGE_NAMED("Bob", "This is Bob debug");
         LOG_INFO_MESSAGE_NAMED("Bob", "This is Bob info");
         LOG_WARNING_MESSAGE_NAMED("Bob", "This is Bob warning");
         LOG_ERROR_MESSAGE_NAMED("Bob", "This is Bob error");

         LOG_DEBUG_MESSAGE_NAMED("Jill", "This is Jill debug");
         LOG_INFO_MESSAGE_NAMED("Jill", "This is Jill info");
         LOG_WARNING_MESSAGE_NAMED("Jill", "This is Jill warning");
         LOG_ERROR_MESSAGE_NAMED("Jill", "This is Jill error");

         LOG_DEBUG_MESSAGE_NAMED("Sampson", "This is Sampson debug");
         LOG_INFO_MESSAGE_NAMED("Sampson", "This is Sampson info");
         LOG_WARNING_MESSAGE_NAMED("Sampson", "This is Sampson warning");
         LOG_ERROR_MESSAGE_NAMED("Sampson", "This is Sampson error");

         FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
         REQUIRE(logFile.exists());

         std::string logFileContents;
         REQUIRE_FALSE(core::readStringFromFile(logFile, &logFileContents));

         REQUIRE(logFileContents.find("This is Bob debug") == std::string::npos);
         REQUIRE(logFileContents.find("This is Bob info") == std::string::npos);
         REQUIRE(logFileContents.find("This is Bob warning") == std::string::npos);
         REQUIRE(logFileContents.find("This is Bob error") != std::string::npos);

         REQUIRE(logFileContents.find("This is Jill debug") == std::string::npos);
         REQUIRE(logFileContents.find("This is Jill info") != std::string::npos);
         REQUIRE(logFileContents.find("This is Jill warning") != std::string::npos);
         REQUIRE(logFileContents.find("This is Jill error") != std::string::npos);

         REQUIRE(logFileContents.find("This is Sampson debug") == std::string::npos);
         REQUIRE(logFileContents.find("This is Sampson info") == std::string::npos);
         REQUIRE(logFileContents.find("This is Sampson warning") != std::string::npos);
         REQUIRE(logFileContents.find("This is Sampson error") != std::string::npos);
      }

      test_that("File logs can rotate")
      {
         FilePath tmpConfPath;
         REQUIRE_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

         // log configured to rotate after 1KB
         std::string confFileContents =
               "[*]\n"
               "logger-type=file\n"
               "log-level=info\n"
               "max-size-mb=0.001\n"
               "rotate-days=14\n"
               "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

         REQUIRE_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

         clearLogEnvVars();
         core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

         std::string id = core::system::generateShortenedUuid();
         REQUIRE_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
         REQUIRE_FALSE(core::system::reinitLog());

         std::string buf("This is a long log statement!");
         buf.reserve(1500);
         for (int i = 0; i < 1500; ++i)
            buf.append("!");

         LOG_INFO_MESSAGE(buf);
         LOG_INFO_MESSAGE(buf);

         FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
         FilePath rotatedLogFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".1.log");
         REQUIRE(logFile.exists());
         REQUIRE(rotatedLogFile.exists());

         // check for time-based rotation
         REQUIRE_FALSE(logFile.remove());
         REQUIRE_FALSE(rotatedLogFile.remove());

         FilePath rotatedLogFile2 = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".2.log");
         FilePath rotatedLogFile3 = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".3.log");

         LOG_INFO_MESSAGE("Test");
         REQUIRE(logFile.exists());
         REQUIRE_FALSE(rotatedLogFile.exists());
         REQUIRE_FALSE(rotatedLogFile2.exists());
         REQUIRE_FALSE(rotatedLogFile3.exists());

         // modify the timestamp of the first log entry to test timestamp-based rotation
         std::string logFileContents;
         REQUIRE_FALSE(core::readStringFromFile(logFile, &logFileContents));

         size_t pos = logFileContents.find(' ');
         REQUIRE_FALSE(pos == std::string::npos);

         std::string timeStr = logFileContents.substr(0, pos);

         boost::posix_time::ptime time;
         REQUIRE(core::date_time::parseUtcTimeFromIso8601String(timeStr, &time));

         time -= boost::posix_time::hours(24) * 13;
         std::string newTimeStr = core::date_time::format(time, core::date_time::kIso8601Format);

         boost::replace_first(logFileContents, timeStr, newTimeStr);
         REQUIRE_FALSE(core::writeStringToFile(logFile, logFileContents));

         REQUIRE_FALSE(core::system::reinitLog());

         LOG_INFO_MESSAGE("Time-based rotation");

         REQUIRE_FALSE(rotatedLogFile.exists());
         REQUIRE_FALSE(rotatedLogFile2.exists());
         REQUIRE_FALSE(rotatedLogFile3.exists());

         REQUIRE_FALSE(core::readStringFromFile(logFile, &logFileContents));

         pos = logFileContents.find(' ');
         REQUIRE_FALSE(pos == std::string::npos);

         timeStr = logFileContents.substr(0, pos);
         REQUIRE(core::date_time::parseUtcTimeFromIso8601String(timeStr, &time));

         time -= boost::posix_time::hours(24) * 15;
         newTimeStr = core::date_time::format(time, core::date_time::kIso8601Format);

         boost::replace_first(logFileContents, timeStr, newTimeStr);
         REQUIRE_FALSE(core::writeStringToFile(logFile, logFileContents));

         REQUIRE_FALSE(core::system::reinitLog());

         LOG_INFO_MESSAGE("Time-based rotation 2");
         REQUIRE(rotatedLogFile.exists());
         REQUIRE_FALSE(rotatedLogFile2.exists());
         REQUIRE_FALSE(rotatedLogFile3.exists());

         REQUIRE_FALSE(core::readStringFromFile(logFile, &logFileContents));

         pos = logFileContents.find(' ');
         REQUIRE_FALSE(pos == std::string::npos);

         timeStr = logFileContents.substr(0, pos);
         REQUIRE(core::date_time::parseUtcTimeFromIso8601String(timeStr, &time));

         time -= boost::posix_time::hours(24) * 15;
         newTimeStr = core::date_time::format(time, core::date_time::kIso8601Format);

         boost::replace_first(logFileContents, timeStr, newTimeStr);
         REQUIRE_FALSE(core::writeStringToFile(logFile, logFileContents));

         REQUIRE_FALSE(core::system::reinitLog());

         LOG_INFO_MESSAGE("Time-based rotation 3");
         REQUIRE(rotatedLogFile.exists());
         REQUIRE(rotatedLogFile2.exists());
         REQUIRE_FALSE(rotatedLogFile3.exists());

         // Check to make sure we can rotate old log files (prior to standard logging)
         // which relied on a different format for the timestamp
         time = boost::posix_time::microsec_clock::universal_time();
         time -= boost::posix_time::hours(24) * 13;
         newTimeStr = core::date_time::format(time, "%d %b %Y %H:%M:%S") + "." +
               zeroPad(std::to_string(time.time_of_day().total_milliseconds() % 1000), 3);

         REQUIRE_FALSE(core::readStringFromFile(logFile, &logFileContents));

         pos = logFileContents.find(' ');
         REQUIRE_FALSE(pos == std::string::npos);

         timeStr = logFileContents.substr(0, pos);
         REQUIRE(core::date_time::parseUtcTimeFromIso8601String(timeStr, &time));

         boost::replace_first(logFileContents, timeStr, newTimeStr);
         REQUIRE_FALSE(core::writeStringToFile(logFile, logFileContents));

         REQUIRE_FALSE(core::system::reinitLog());

         LOG_INFO_MESSAGE("Time-based rotation 4");
         REQUIRE(rotatedLogFile.exists());
         REQUIRE(rotatedLogFile2.exists());
         REQUIRE_FALSE(rotatedLogFile3.exists());

         time -= boost::posix_time::hours(24) * 15;
         std::string newTimeStr2 = core::date_time::format(time, "%d %b %Y %H:%M:%S") + "." +
              zeroPad(std::to_string(time.time_of_day().total_milliseconds() % 1000), 3);

         boost::replace_first(logFileContents, newTimeStr, newTimeStr2);
         REQUIRE_FALSE(core::writeStringToFile(logFile, logFileContents));

         REQUIRE_FALSE(core::system::reinitLog());

         LOG_INFO_MESSAGE("Time-based rotation 5");
         REQUIRE(rotatedLogFile.exists());
         REQUIRE(rotatedLogFile2.exists());
         REQUIRE(rotatedLogFile3.exists());
      }

      test_that("Environment variables override logging.conf configuration")
      {
         FilePath tmpConfPath;
         REQUIRE_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

         // log configured to rotate after 1KB
         std::string confFileContents =
               "[*]\n"
               "logger-type=stderr\n"
               "log-level=info\n"
               "max-size-mb=0.001\n"
               "log-dir=" + tmpConfPath.getParent().getAbsolutePath() +
               "\n\n" +
               "[bob]\n" +
               "log-level=warn";

         REQUIRE_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

         clearLogEnvVars();
         core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());
         core::system::setenv("RS_LOGGER_TYPE", "file");

         std::string id = core::system::generateShortenedUuid();
         REQUIRE_FALSE(core::system::initializeLog("logging-tests-" + id, log::LogLevel::WARN, true));
         REQUIRE_FALSE(core::system::reinitLog());

         LOG_DEBUG_MESSAGE("Debug");
         LOG_INFO_MESSAGE("Info");
         LOG_DEBUG_MESSAGE_NAMED("bob", "This is bob");

         FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
         REQUIRE(logFile.exists());

         std::string logFileContents;
         REQUIRE_FALSE(core::readStringFromFile(logFile, &logFileContents));

         REQUIRE(logFileContents.find("Debug") == std::string::npos);
         REQUIRE(logFileContents.find("Info") != std::string::npos);

         core::system::setenv("RS_LOG_LEVEL", "WARN");
         REQUIRE_FALSE(core::system::initializeLog("logging-tests-" + id, log::LogLevel::WARN, true));
         REQUIRE_FALSE(core::system::reinitLog());

         LOG_INFO_MESSAGE("Info 2");

         REQUIRE_FALSE(core::readStringFromFile(logFile, &logFileContents));
         REQUIRE(logFileContents.find("Info 2") == std::string::npos);

         core::system::setenv("RS_LOG_MESSAGE_FORMAT", "JSON");
         REQUIRE_FALSE(core::system::initializeLog("logging-tests-" + id, log::LogLevel::WARN, true));
         REQUIRE_FALSE(core::system::reinitLog());

         LOG_ERROR_MESSAGE("This is JSON");

         std::vector<std::string> logLines;
         REQUIRE_FALSE(core::readStringVectorFromFile(logFile, &logLines));

         bool found = false;
         for (const std::string& line : logLines)
         {
            if (line.find("This is JSON") != std::string::npos)
            {
               found = true;

               json::Object obj;
               REQUIRE_FALSE(obj.parse(line));
               break;
            }
         }

         REQUIRE(found);

         FilePath tmpConfPath2;
         REQUIRE_FALSE(FilePath::tempFilePath(".conf", tmpConfPath2));
         tmpConfPath2 = tmpConfPath2.completeChildPath("subdir");

         core::system::setenv("RS_LOG_LEVEL", "ERROR");
         core::system::setenv("RS_LOG_DIR", tmpConfPath2.getAbsolutePath());
         REQUIRE_FALSE(core::system::initializeLog("logging-tests-" + id, log::LogLevel::WARN, true));
         REQUIRE_FALSE(core::system::reinitLog());

         LOG_INFO_MESSAGE("Info 3");
         LOG_WARNING_MESSAGE_NAMED("bob", "This is bob");
         LOG_ERROR_MESSAGE_NAMED("bob", "Now this is bob!");

         FilePath newLogFile = tmpConfPath2.completeChildPath("logging-tests-" + id + ".log");
         REQUIRE(tmpConfPath2.exists());
         REQUIRE_FALSE(core::readStringFromFile(newLogFile, &logFileContents));

         REQUIRE(logFileContents.find("Info 3") == std::string::npos);
         REQUIRE(logFileContents.find("This is bob") == std::string::npos);
         REQUIRE(logFileContents.find("Now this is bob!") != std::string::npos);
      }

      test_that("File log rotations cap")
      {
         FilePath tmpConfPath;
         REQUIRE_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

         // log configured to rotate after 10B
         std::string confFileContents =
               "[*]\n"
               "logger-type=file\n"
               "log-level=info\n"
               "max-size-mb=0.00001\n"
               "max-rotations=15\n"
               "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

         REQUIRE_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

         clearLogEnvVars();
         core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

         std::string id = core::system::generateShortenedUuid();
         REQUIRE_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
         REQUIRE_FALSE(core::system::reinitLog());

         for (int i = 0; i < 50; ++i)
         {
            std::string logStr = "Log line " + safe_convert::numberToString(i);
            LOG_INFO_MESSAGE(logStr);
         }

         FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
         REQUIRE(logFile.exists());

         for (int i = 1; i <= 15; ++i)
         {
            FilePath rotatedLogFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + "." + safe_convert::numberToString(i) + ".log");
            REQUIRE(rotatedLogFile.exists());

            std::string logContents;
            REQUIRE_FALSE(core::readStringFromFile(rotatedLogFile, &logContents));

            std::string logStr = "Log line " + safe_convert::numberToString(49 - i) + "\n";
            REQUIRE(boost::ends_with(logContents, logStr));
         }

         // No more log files should exist because we capped number of rotations at 15
         for (int i = 16; i < 50; ++i)
         {
            FilePath rotatedLogFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + "." + safe_convert::numberToString(i) + ".log");
            REQUIRE_FALSE(rotatedLogFile.exists());
         }
      }

      test_that("File log rotations cap can be disabled")
      {
         FilePath tmpConfPath;
         REQUIRE_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

         // log configured to rotate after 10B
         std::string confFileContents =
               "[*]\n"
               "logger-type=file\n"
               "log-level=info\n"
               "max-size-mb=0.00001\n"
               "max-rotations=0\n"
               "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

         REQUIRE_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

         clearLogEnvVars();
         core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

         std::string id = core::system::generateShortenedUuid();
         REQUIRE_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
         REQUIRE_FALSE(core::system::reinitLog());

         for (int i = 0; i < 100; ++i)
         {
            std::string logStr = "Log line " + safe_convert::numberToString(i);
            LOG_INFO_MESSAGE(logStr);
         }

         FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
         REQUIRE(logFile.exists());

         for (int i = 1; i < 99; ++i)
         {
            FilePath rotatedLogFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + "." + safe_convert::numberToString(i) + ".log");
            REQUIRE(rotatedLogFile.exists());

            std::string logContents;
            REQUIRE_FALSE(core::readStringFromFile(rotatedLogFile, &logContents));

            std::string logStr = "Log line " + safe_convert::numberToString(99 - i) + "\n";
            REQUIRE(boost::ends_with(logContents, logStr));
         }
      }
   }
}

} // namespace unit_tests
} // namespace core
} // namespace rstudio
