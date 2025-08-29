/*
 * LoggingTests.cpp
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

#include <gtest/gtest.h>

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

#include <sstream>
#include <iomanip>

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

TEST(LoggingTest, CanFindLoggingConfFromEnvVar)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-level=info\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));

   LOG_DEBUG_MESSAGE("Debug message");
   LOG_INFO_MESSAGE("Info message");
   LOG_WARNING_MESSAGE("Warning message");
   LOG_ERROR_MESSAGE("Error message");

   // Turn on debug logging for these LOG_DEBUG_MESSAGE tests
   log::setFileLogLevel(log::LogLevel::DEBUG_LEVEL);

   // Because the LOG_DEBUG_MESSAGE is a macro with a ? operator, these tests make sure it works
   // in all important contexts
   bool dummy = false;
   if (!dummy) LOG_DEBUG_MESSAGE("Debug in solo if");

   if (dummy)
      dummy = true;
   else
      LOG_DEBUG_MESSAGE("Debug in else");

   if (!dummy)
      LOG_DEBUG_MESSAGE("Debug in if with else");
   else
      dummy = false;

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   std::string logFileContents;
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   ASSERT_EQ(std::string::npos, logFileContents.find("Debug message"));
   ASSERT_NE(std::string::npos, logFileContents.find("Info message"));
   ASSERT_NE(std::string::npos, logFileContents.find("Warning message"));
   ASSERT_NE(std::string::npos, logFileContents.find("Error message"));
   ASSERT_NE(std::string::npos, logFileContents.find("Debug in solo if"));
   ASSERT_NE(std::string::npos, logFileContents.find("Debug in else"));
   ASSERT_NE(std::string::npos, logFileContents.find("Debug in if with else"));
}

TEST(LoggingTest, CanOverrideLoggingBasedOnEnvVarsAndWriteValidJsonFormat)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-level=info\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());
   core::system::setenv("RS_LOG_LEVEL", "DEBUG");
   core::system::setenv("RS_LOG_MESSAGE_FORMAT", "JSON");
   core::system::setenv("RS_LOGGER_TYPE", "FILE");

   FilePath logDir;
   ASSERT_FALSE(FilePath::tempFilePath(logDir));
   core::system::setenv("RS_LOG_DIR", logDir.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   LOG_DEBUG_MESSAGE("Debug message");
   LOG_INFO_MESSAGE("Info message");
   LOG_WARNING_MESSAGE("Warning message");
   LOG_ERROR_MESSAGE("Error message");

   FilePath logFile = logDir.completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   std::vector<std::string> lines;
   ASSERT_FALSE(core::readStringVectorFromFile(logFile, &lines));
   ASSERT_FALSE(lines.empty());

   for (const std::string& line : lines)
   {
      json::Object obj;
      ASSERT_FALSE(obj.parse(line));

      ASSERT_TRUE(obj.hasMember("time"));
      ASSERT_TRUE(obj.hasMember("service"));
      ASSERT_TRUE(obj.hasMember("level"));
      ASSERT_TRUE(obj.hasMember("message"));

   ASSERT_EQ(std::string("logging-tests-") + id, obj["service"].getString());
      ASSERT_TRUE((obj["message"].getString() == "Debug message" ||
               obj["message"].getString() == "Info message" ||
               obj["message"].getString() == "Warning message" ||
               obj["message"].getString() == "Error message"));
   }
}
#ifndef _WIN32

TEST(LoggingTest, CanReloadLoggingConfiguration)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-level=warn\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   LOG_DEBUG_MESSAGE("Debug message");
   LOG_INFO_MESSAGE("Info message");
   LOG_WARNING_MESSAGE("Warning message");
   LOG_ERROR_MESSAGE("Error message");

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   std::string logFileContents;
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   ASSERT_EQ(std::string::npos, logFileContents.find("Debug message"));
   ASSERT_EQ(std::string::npos, logFileContents.find("Info message"));
   ASSERT_NE(std::string::npos, logFileContents.find("Warning message"));
   ASSERT_NE(std::string::npos, logFileContents.find("Error message"));

   boost::replace_all(confFileContents, "warn", "debug");
   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   // reload logging configuration by sending SIGHUP to ourselves
   // we also have to wait awhile because this is an asynchronous process handled by another thread
   core::system::sendSignalToSelf(core::system::SigHup);
   bool success = false;
   for (int i = 0; i < 5; ++i)
   {
      boost::this_thread::sleep(boost::posix_time::milliseconds(250));

      LOG_DEBUG_MESSAGE("Debug message");
      LOG_INFO_MESSAGE("Info message");

      ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

      success =
            logFileContents.find("Debug message") != std::string::npos &&
            logFileContents.find("Info message") != std::string::npos;

      if (success)
         break;
   }

   ASSERT_TRUE(success);
}
#endif

TEST(LoggingTest, CanLogToNamedLoggers)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-level=warn\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   confFileContents += "\n\n"
         "[Bob]\n"
         "log-level=error\n\n"
         "[Jill]\n"
         "log-level=info\n\n"
         "[Emma]\n"
         "log-level=trace\n\n";

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   LOG_TRACE_MESSAGE_NAMED("Bob", "This is Bob trace");
   LOG_DEBUG_MESSAGE_NAMED("Bob", "This is Bob debug");
   LOG_INFO_MESSAGE_NAMED("Bob", "This is Bob info");
   LOG_WARNING_MESSAGE_NAMED("Bob", "This is Bob warning");
   LOG_ERROR_MESSAGE_NAMED("Bob", "This is Bob error");

   LOG_TRACE_MESSAGE_NAMED("Jill", "This is Jill trace");
   LOG_DEBUG_MESSAGE_NAMED("Jill", "This is Jill debug");
   LOG_INFO_MESSAGE_NAMED("Jill", "This is Jill info");
   LOG_WARNING_MESSAGE_NAMED("Jill", "This is Jill warning");
   LOG_ERROR_MESSAGE_NAMED("Jill", "This is Jill error");

   LOG_TRACE_MESSAGE_NAMED("Sampson", "This is Sampson trace");
   LOG_DEBUG_MESSAGE_NAMED("Sampson", "This is Sampson debug");
   LOG_INFO_MESSAGE_NAMED("Sampson", "This is Sampson info");
   LOG_WARNING_MESSAGE_NAMED("Sampson", "This is Sampson warning");
   LOG_ERROR_MESSAGE_NAMED("Sampson", "This is Sampson error");

   LOG_TRACE_MESSAGE_NAMED("Emma", "This is Emma trace");
   LOG_DEBUG_MESSAGE_NAMED("Emma", "This is Emma debug");
   LOG_INFO_MESSAGE_NAMED("Emma", "This is Emma info");
   LOG_WARNING_MESSAGE_NAMED("Emma", "This is Emma warning");
   LOG_ERROR_MESSAGE_NAMED("Emma", "This is Emma error");

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   std::string logFileContents;
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   ASSERT_EQ(std::string::npos, logFileContents.find("This is Bob trace"));
   ASSERT_EQ(std::string::npos, logFileContents.find("This is Bob debug"));
   ASSERT_EQ(std::string::npos, logFileContents.find("This is Bob info"));
   ASSERT_EQ(std::string::npos, logFileContents.find("This is Bob warning"));
   ASSERT_NE(std::string::npos, logFileContents.find("This is Bob error"));

   ASSERT_EQ(std::string::npos, logFileContents.find("This is Jill trace"));
   ASSERT_EQ(std::string::npos, logFileContents.find("This is Jill debug"));
   ASSERT_NE(std::string::npos, logFileContents.find("This is Jill info"));
   ASSERT_NE(std::string::npos, logFileContents.find("This is Jill warning"));
   ASSERT_NE(std::string::npos, logFileContents.find("This is Jill error"));

   ASSERT_EQ(std::string::npos, logFileContents.find("This is Sampson trace"));
   ASSERT_EQ(std::string::npos, logFileContents.find("This is Sampson debug"));
   ASSERT_EQ(std::string::npos, logFileContents.find("This is Sampson info"));
   ASSERT_NE(std::string::npos, logFileContents.find("This is Sampson warning"));
   ASSERT_NE(std::string::npos, logFileContents.find("This is Sampson error"));

   ASSERT_NE(std::string::npos, logFileContents.find("This is Emma trace"));
   ASSERT_NE(std::string::npos, logFileContents.find("This is Emma debug"));
   ASSERT_NE(std::string::npos, logFileContents.find("This is Emma info"));
   ASSERT_NE(std::string::npos, logFileContents.find("This is Emma warning"));
   ASSERT_NE(std::string::npos, logFileContents.find("This is Emma error"));
}

TEST(LoggingTest, FileLogsCanRotate)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   // log configured to rotate after 1KB
   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-level=info\n"
         "max-size-mb=0.001\n"
         "rotate-days=14\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   std::string buf("This is a long log statement!");
   buf.reserve(1500);
   for (int i = 0; i < 1500; ++i)
      buf.append("!");

   LOG_INFO_MESSAGE(buf);
   LOG_INFO_MESSAGE(buf);

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   FilePath rotatedLogFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".1.log");
   ASSERT_TRUE(logFile.exists());
   ASSERT_TRUE(rotatedLogFile.exists());

   // check for time-based rotation
   ASSERT_FALSE(logFile.remove());
   ASSERT_FALSE(rotatedLogFile.remove());

   FilePath rotatedLogFile2 = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".2.log");
   FilePath rotatedLogFile3 = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".3.log");

   LOG_INFO_MESSAGE("Test");
   ASSERT_TRUE(logFile.exists());
   ASSERT_FALSE(rotatedLogFile.exists());
   ASSERT_FALSE(rotatedLogFile2.exists());
   ASSERT_FALSE(rotatedLogFile3.exists());

   // modify the timestamp of the first log entry to test timestamp-based rotation
   std::string logFileContents;
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   size_t pos = logFileContents.find(' ');
   ASSERT_FALSE(pos == std::string::npos);

   std::string timeStr = logFileContents.substr(0, pos);

   boost::posix_time::ptime time;
   ASSERT_TRUE(core::date_time::parseUtcTimeFromIso8601String(timeStr, &time));

   time -= boost::posix_time::hours(24) * 13;
   std::string newTimeStr = core::date_time::format(time, core::date_time::kIso8601Format);

   boost::replace_first(logFileContents, timeStr, newTimeStr);
   ASSERT_FALSE(core::writeStringToFile(logFile, logFileContents));

   ASSERT_FALSE(core::system::reinitLog());

   LOG_INFO_MESSAGE("Time-based rotation");

   ASSERT_FALSE(rotatedLogFile.exists());
   ASSERT_FALSE(rotatedLogFile2.exists());
   ASSERT_FALSE(rotatedLogFile3.exists());

   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   pos = logFileContents.find(' ');
   ASSERT_FALSE(pos == std::string::npos);

   timeStr = logFileContents.substr(0, pos);
   ASSERT_TRUE(core::date_time::parseUtcTimeFromIso8601String(timeStr, &time));

   time -= boost::posix_time::hours(24) * 15;
   newTimeStr = core::date_time::format(time, core::date_time::kIso8601Format);

   boost::replace_first(logFileContents, timeStr, newTimeStr);
   ASSERT_FALSE(core::writeStringToFile(logFile, logFileContents));

   ASSERT_FALSE(core::system::reinitLog());

   LOG_INFO_MESSAGE("Time-based rotation 2");
   ASSERT_TRUE(rotatedLogFile.exists());
   ASSERT_FALSE(rotatedLogFile2.exists());
   ASSERT_FALSE(rotatedLogFile3.exists());

   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   pos = logFileContents.find(' ');
   ASSERT_FALSE(pos == std::string::npos);

   timeStr = logFileContents.substr(0, pos);
   ASSERT_TRUE(core::date_time::parseUtcTimeFromIso8601String(timeStr, &time));

   time -= boost::posix_time::hours(24) * 15;
   newTimeStr = core::date_time::format(time, core::date_time::kIso8601Format);

   boost::replace_first(logFileContents, timeStr, newTimeStr);
   ASSERT_FALSE(core::writeStringToFile(logFile, logFileContents));

   ASSERT_FALSE(core::system::reinitLog());

   LOG_INFO_MESSAGE("Time-based rotation 3");
   ASSERT_TRUE(rotatedLogFile.exists());
   ASSERT_TRUE(rotatedLogFile2.exists());
   ASSERT_FALSE(rotatedLogFile3.exists());

   // Check to make sure we can rotate old log files (prior to standard logging)
   // which relied on a different format for the timestamp
   time = boost::posix_time::microsec_clock::universal_time();
   time -= boost::posix_time::hours(24) * 13;
   newTimeStr = core::date_time::format(time, "%d %b %Y %H:%M:%S") + "." +
         zeroPad(std::to_string(time.time_of_day().total_milliseconds() % 1000), 3);

   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   pos = logFileContents.find(' ');
   ASSERT_FALSE(pos == std::string::npos);

   timeStr = logFileContents.substr(0, pos);
   ASSERT_TRUE(core::date_time::parseUtcTimeFromIso8601String(timeStr, &time));

   boost::replace_first(logFileContents, timeStr, newTimeStr);
   ASSERT_FALSE(core::writeStringToFile(logFile, logFileContents));

   ASSERT_FALSE(core::system::reinitLog());

   LOG_INFO_MESSAGE("Time-based rotation 4");
   ASSERT_TRUE(rotatedLogFile.exists());
   ASSERT_TRUE(rotatedLogFile2.exists());
   ASSERT_FALSE(rotatedLogFile3.exists());

   time -= boost::posix_time::hours(24) * 15;
   std::string newTimeStr2 = core::date_time::format(time, "%d %b %Y %H:%M:%S") + "." +
         zeroPad(std::to_string(time.time_of_day().total_milliseconds() % 1000), 3);

   boost::replace_first(logFileContents, newTimeStr, newTimeStr2);
   ASSERT_FALSE(core::writeStringToFile(logFile, logFileContents));

   ASSERT_FALSE(core::system::reinitLog());

   LOG_INFO_MESSAGE("Time-based rotation 5");
   ASSERT_TRUE(rotatedLogFile.exists());
   ASSERT_TRUE(rotatedLogFile2.exists());
   ASSERT_TRUE(rotatedLogFile3.exists());
}

TEST(LoggingTest, EnvironmentVariablesOverrideLoggingConfConfiguration)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

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

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());
   core::system::setenv("RS_LOGGER_TYPE", "file");

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   LOG_DEBUG_MESSAGE("Debug");
   LOG_INFO_MESSAGE("Info");
   LOG_DEBUG_MESSAGE_NAMED("bob", "This is bob");

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   std::string logFileContents;
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   ASSERT_EQ(std::string::npos, logFileContents.find("Debug"));
   ASSERT_NE(std::string::npos, logFileContents.find("Info"));

   core::system::setenv("RS_LOG_LEVEL", "WARN");
   ASSERT_FALSE(core::system::initializeLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   LOG_INFO_MESSAGE("Info 2");

   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));
   ASSERT_EQ(std::string::npos, logFileContents.find("Info 2"));

   core::system::setenv("RS_LOG_MESSAGE_FORMAT", "JSON");
   ASSERT_FALSE(core::system::initializeLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   LOG_ERROR_MESSAGE("This is JSON");

   std::vector<std::string> logLines;
   ASSERT_FALSE(core::readStringVectorFromFile(logFile, &logLines));

   bool found = false;
   for (const std::string& line : logLines)
   {
      if (line.find("This is JSON") != std::string::npos)
      {
         found = true;

         json::Object obj;
         ASSERT_FALSE(obj.parse(line));
         break;
      }
   }

   ASSERT_TRUE(found);

   FilePath tmpConfPath2;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath2));
   tmpConfPath2 = tmpConfPath2.completeChildPath("subdir");

   core::system::setenv("RS_LOG_LEVEL", "ERROR");
   core::system::setenv("RS_LOG_DIR", tmpConfPath2.getAbsolutePath());
   ASSERT_FALSE(core::system::initializeLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   LOG_INFO_MESSAGE("Info 3");
   LOG_WARNING_MESSAGE_NAMED("bob", "This is bob");
   LOG_ERROR_MESSAGE_NAMED("bob", "Now this is bob!");

   FilePath newLogFile = tmpConfPath2.completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(tmpConfPath2.exists());
   ASSERT_FALSE(core::readStringFromFile(newLogFile, &logFileContents));

   ASSERT_EQ(std::string::npos, logFileContents.find("Info 3"));
   ASSERT_EQ(std::string::npos, logFileContents.find("This is bob"));
   ASSERT_NE(std::string::npos, logFileContents.find("Now this is bob!"));
}

TEST(LoggingTest, FileLogRotationsCap)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   // log configured to rotate after 10B
   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-level=info\n"
         "max-size-mb=0.00001\n"
         "max-rotations=15\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   for (int i = 0; i < 50; ++i)
   {
      std::string logStr = "Log line " + safe_convert::numberToString(i);
      LOG_INFO_MESSAGE(logStr);
   }

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   for (int i = 1; i <= 15; ++i)
   {
      FilePath rotatedLogFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + "." + safe_convert::numberToString(i) + ".log");
      ASSERT_TRUE(rotatedLogFile.exists());

      std::string logContents;
      ASSERT_FALSE(core::readStringFromFile(rotatedLogFile, &logContents));

      std::string logStr = "Log line " + safe_convert::numberToString(49 - i) + "\n";
      ASSERT_TRUE(boost::ends_with(logContents, logStr));
   }

   // No more log files should exist because we capped number of rotations at 15
   for (int i = 16; i < 50; ++i)
   {
      FilePath rotatedLogFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + "." + safe_convert::numberToString(i) + ".log");
      ASSERT_FALSE(rotatedLogFile.exists());
   }
}

TEST(LoggingTest, FileLogRotationsCapCanBeDisabled)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   // log configured to rotate after 10B
   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-level=info\n"
         "max-size-mb=0.00001\n"
         "max-rotations=0\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   for (int i = 0; i < 100; ++i)
   {
      std::string logStr = "Log line " + safe_convert::numberToString(i);
      LOG_INFO_MESSAGE(logStr);
   }

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   for (int i = 1; i < 99; ++i)
   {
      FilePath rotatedLogFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + "." + safe_convert::numberToString(i) + ".log");
      ASSERT_TRUE(rotatedLogFile.exists());

      std::string logContents;
      ASSERT_FALSE(core::readStringFromFile(rotatedLogFile, &logContents));

      std::string logStr = "Log line " + safe_convert::numberToString(99 - i) + "\n";
      ASSERT_TRUE(boost::ends_with(logContents, logStr));
   }
}

TEST(LoggingTest, CanPassthroughLog)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-level=INFO\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   LOG_INFO_MESSAGE("This is a message");

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   std::string logFileContents;
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   boost::replace_all(logFileContents, "INFO", "DEBUG");
   LOG_PASSTHROUGH_MESSAGE("mysource", logFileContents);

   (void) logFileContents.empty();
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));
   ASSERT_EQ(logFileContents.find("DEBUG"), std::string::npos);

   boost::replace_all(logFileContents, "INFO", "WARNING");
   boost::replace_all(logFileContents, "This is a message", "This is a passthrough message");
   boost::replace_all(logFileContents, "[logging-tests-" + id + "]", "[subproc]");
   LOG_PASSTHROUGH_MESSAGE("mysource", logFileContents);

   (void) logFileContents.empty();
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   ASSERT_TRUE(logFileContents.find("WARNING") != std::string::npos);
   ASSERT_TRUE(logFileContents.find("[subproc, log-source: mysource]") != std::string::npos);
   ASSERT_TRUE(logFileContents.find("This is a passthrough message") != std::string::npos);
}

TEST(LoggingTest, CanJsonLogError)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-message-format=json\n"
         "log-level=debug\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   LOG_ERROR(systemError(boost::system::errc::no_such_file_or_directory, "Couldn't read the file", ERROR_LOCATION));

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   std::string logFileContents;
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   json::Object logObj;
   ASSERT_FALSE(logObj.parse(logFileContents));

   json::Object errorObj;
   ASSERT_FALSE(json::readObject(logObj,
                                    "error", errorObj));

   int code = 0;
   std::string message, type;
   json::Object errorProperties;
   ASSERT_FALSE(json::readObject(errorObj,
                                    "code", code,
                                    "message", message,
                                    "type", type,
                                    "properties", errorProperties));

   ASSERT_EQ(boost::system::errc::no_such_file_or_directory, code);
#ifndef _WIN32
   ASSERT_EQ(std::string("No such file or directory"), message);
#else
   ASSERT_EQ(std::string("The system cannot find the file specified"), message);
#endif
   ASSERT_EQ(std::string("system"), type);

   std::string description;
   ASSERT_FALSE(json::readObject(errorProperties,
                                    "description", description));

   ASSERT_EQ(std::string("Couldn't read the file"), description);
}

#ifndef _WIN32
// Disabled in Windows, tracked here: https://github.com/rstudio/rstudio/issues/13165

TEST(LoggingTest, CanLogLogmessageproperties)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-message-format=pretty\n"
         "log-level=debug\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   json::Object obj;
   obj["first"] = 1;
   obj["second"] = 2;
   obj["third"] = 3;
   json::Array arr;
   arr.push_back(1);
   arr.push_back(2);
   arr.push_back(3);

   Error err = systemError(boost::system::errc::no_such_file_or_directory,
                           "Couldn't find file",
                           ERROR_LOCATION);

   uint_least64_t val = 1;
   core::log::LogMessageProperties props = {{"prop1", val}, {"prop2", "2"}, {"prop3", 3.14}, {"prop4", obj}, {"prop5", arr}, {"prop6", err}};
   LOG_DEBUG_MESSAGE_WITH_PROPS("Message 1", props);

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   std::string logFileContents;
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   ASSERT_NE(std::string::npos, logFileContents.find("Message 1"));
   ASSERT_NE(std::string::npos, logFileContents.find("prop1: 1"));
   ASSERT_NE(std::string::npos, logFileContents.find("prop2: 2"));
   ASSERT_NE(std::string::npos, logFileContents.find("prop3: 3.14"));
   ASSERT_NE(std::string::npos, logFileContents.find(", prop4: " + obj.write()));
   ASSERT_NE(std::string::npos, logFileContents.find(", prop5: " + arr.write()));
   ASSERT_NE(std::string::npos, logFileContents.find("Couldn't find file"));
   ASSERT_TRUE(((logFileContents.find("No such file or directory") != std::string::npos) || 
            (logFileContents.find("The system cannot find the file specified") != std::string::npos)));
   ASSERT_NE(std::string::npos, logFileContents.find("LoggingTests.cpp"));

   boost::replace_all(confFileContents, "pretty", "json");
   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   // reload logging configuration by sending SIGHUP to ourselves
   // we also have to wait awhile because this is an asynchronous process handled by another thread
   core::system::sendSignalToSelf(core::system::SigHup);
   bool success = false;
   for (int i = 0; i < 5; ++i)
   {
      boost::this_thread::sleep(boost::posix_time::milliseconds(250));

      LOG_DEBUG_MESSAGE_WITH_PROPS("Message 1", props);

      std::vector<std::string> logLines;
      ASSERT_FALSE(core::readStringVectorFromFile(logFile, &logLines));

      const std::string& lastLine = logLines.at(logLines.size() - 1);
      json::Object logObj;
      Error err = logObj.parse(lastLine);
      if (err)
         continue;

      std::string message;
      json::Object properties;
      err = json::readObject(logObj,
                              "message", message,
                              "properties", properties);
      if (err)
         continue;

      int prop1 = 0;
      std::string prop2;
      double prop3;
      json::Object prop4;
      json::Array prop5;
      json::Object prop6;

      err = json::readObject(properties,
                              "prop1", prop1,
                              "prop2", prop2,
                              "prop3", prop3,
                              "prop4", prop4,
                              "prop5", prop5,
                              "prop6", prop6);
      if (err)
         continue;

      success = prop1 == 1 &&
                  prop2 == "2" &&
                  prop3 == 3.14 &&
                  prop4 == obj &&
                  prop5 == arr;

      if (success)
         break;
   }

   ASSERT_TRUE(success);
}
#endif



TEST(LoggingTest, CanDebugActionLog)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-message-format=pretty\n"
         "log-level=debug\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   log::logDebugAction([&](boost::optional<log::LogMessageProperties>* pProps) {
      log::LogMessageProperties props = {{"1", 1}, {"2", "Two"}};
      *pProps = props;
      return "This was an action log";
   });

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   std::string logFileContents;
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   ASSERT_NE(std::string::npos, logFileContents.find("This was an action log"));
   ASSERT_NE(std::string::npos, logFileContents.find("[1: 1, 2: Two]"));
}
#ifndef _WIN32

TEST(LoggingTest, FileLogsAreCreatedWithCorrectPermissions)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   FilePath logSubDir = tmpConfPath.getParent().completeChildPath("logsubdir");
   FilePath innerLogSubDir = logSubDir.completeChildPath("logsubdir2");

   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-level=debug\n"
         "log-dir=" + innerLogSubDir.getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));
   ASSERT_FALSE(core::system::reinitLog());

   LOG_DEBUG_MESSAGE("HELLO WORLD! EXCITED TO BE HERE!");

   ASSERT_TRUE(logSubDir.exists());
   ASSERT_TRUE(innerLogSubDir.exists());

   FileMode mode;
   Error err = logSubDir.getFileMode(mode);
   ASSERT_FALSE(err);
   ASSERT_EQ(FileMode::USER_READ_WRITE_EXECUTE_GROUP_READ_WRITE_EXECUTE_ALL_READ_EXECUTE, mode);

   FileMode mode2;
   Error err2 = innerLogSubDir.getFileMode(mode2);
   ASSERT_FALSE(err2);
   ASSERT_EQ(FileMode::USER_READ_WRITE_EXECUTE_GROUP_READ_WRITE_EXECUTE_ALL_READ_EXECUTE, mode2);

   FilePath logFile = innerLogSubDir.completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   FileMode mode3;
   Error err3 = logFile.getFileMode(mode3);
   ASSERT_FALSE(err3);
   ASSERT_EQ(FileMode::USER_READ_WRITE, mode3);
}
#endif

TEST(LoggingTest, CanForceLogDirectory)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   FilePath defaultLogDir = tmpConfPath.getParent().completeChildPath("defaultlogdir");
   FilePath overrideLogDir = tmpConfPath.getParent();

   std::string confFileContents =
         "[*]\n"
         "logger-type=file\n"
         "log-level=debug\n"
         "log-dir=" + overrideLogDir.getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeLog("logging-tests-" + id, log::LogLevel::WARN, defaultLogDir, false));
   ASSERT_FALSE(core::system::reinitLog());

   LOG_DEBUG_MESSAGE("HELLO WORLD! EXCITED TO BE HERE!");

   FilePath logFile = overrideLogDir.completeChildPath("logging-tests-" + id + ".log");
   FilePath defaultLogFile = defaultLogDir.completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());
   ASSERT_FALSE(defaultLogFile.exists());
   ASSERT_FALSE(logFile.remove());
   ASSERT_FALSE(defaultLogFile.remove());

   ASSERT_FALSE(core::system::initializeLog("logging-tests-" + id, log::LogLevel::WARN, defaultLogDir, true));
   ASSERT_FALSE(core::system::reinitLog());

   LOG_DEBUG_MESSAGE("HELLO WORLD! EXCITED TO BE HERE!");

   ASSERT_FALSE(logFile.exists());
   ASSERT_TRUE(defaultLogFile.exists());
   ASSERT_FALSE(logFile.remove());
   ASSERT_FALSE(defaultLogFile.remove());
}

TEST(LoggingTest, NoNewlinesInPrettyPrintLogLine)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   std::string confFileContents =
         "[*]\n"
         "log-message-format=pretty\n"
         "logger-type=file\n"
         "log-level=debug\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));

   core::log::LogMessageProperties props = {{"prop1", "No newline"}, {"prop2", "Newlines here\nGet your newlines\n"}};
   LOG_DEBUG_MESSAGE_WITH_PROPS("Line 1\nLine 2\nLine 3\n", props);

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   std::string logFileContents;
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   // only newline should be at the end of the log file, signifying the end of the log line
   ASSERT_EQ(logFileContents.size() - 1, logFileContents.find("\n"));
}

TEST(LoggingTest, NoNewlinesInJsonLogLine)
{
   FilePath tmpConfPath;
   ASSERT_FALSE(FilePath::tempFilePath(".conf", tmpConfPath));

   std::string confFileContents =
         "[*]\n"
         "log-message-format=json\n"
         "logger-type=file\n"
         "log-level=debug\n"
         "log-dir=" + tmpConfPath.getParent().getAbsolutePath();

   ASSERT_FALSE(core::writeStringToFile(tmpConfPath, confFileContents));

   clearLogEnvVars();
   core::system::setenv("RS_LOG_CONF_FILE", tmpConfPath.getAbsolutePath());

   std::string id = core::system::generateShortenedUuid();
   ASSERT_FALSE(core::system::initializeStderrLog("logging-tests-" + id, log::LogLevel::WARN, true));

   core::log::LogMessageProperties props = {{"prop1", "No newline"}, {"prop2", "Newlines here\nGet your newlines\n"}};
   LOG_DEBUG_MESSAGE_WITH_PROPS("Line 1\nLine 2\nLine 3\n", props);

   FilePath logFile = tmpConfPath.getParent().completeChildPath("logging-tests-" + id + ".log");
   ASSERT_TRUE(logFile.exists());

   std::string logFileContents;
   ASSERT_FALSE(core::readStringFromFile(logFile, &logFileContents));

   // only newline should be at the end of the log file, signifying the end of the log line
   ASSERT_EQ(logFileContents.size() - 1, logFileContents.find("\n"));
}

} // namespace unit_tests
} // namespace core
} // namespace rstudio

