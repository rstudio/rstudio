/*
 * ProgramOptionsTests.cpp
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

#include <cstdlib>
#include <string>
#include <vector>

#include <gtest/gtest.h>

#include <boost/program_options/options_description.hpp>
#include <boost/program_options/value_semantic.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/FileSerializer.hpp>
#include <core/ProgramOptions.hpp>
#include <core/ProgramStatus.hpp>

namespace rstudio {
namespace core {
namespace program_options {

namespace {

// Build an OptionsDescription whose config file recognizes a couple of keys so
// that any other key in a config file is reported as unrecognized.
OptionsDescription testOptions()
{
   OptionsDescription odesc("test");
   // default values keep these optional: validateOptionsProvided treats any
   // registered option absent from the variables map as a required-but-missing
   // error, so a default ensures they are always present.
   odesc.configFile.add_options()
      ("valid-one", boost::program_options::value<std::string>()->default_value(""))
      ("valid-two", boost::program_options::value<std::string>()->default_value(""));
   return odesc;
}

// Write contents to a temp config file and return its path.
FilePath writeConfig(const std::string& contents)
{
   FilePath path;
   Error error = FilePath::tempFilePath(path);
   EXPECT_FALSE(error);
   error = writeStringToFile(path, contents);
   EXPECT_FALSE(error);
   return path;
}

// Run read() with the given flag (e.g. "--check-config") against a config file.
ProgramStatus runCheck(const std::string& flag, const FilePath& configFile)
{
   OptionsDescription odesc = testOptions();
   std::string pathArg = configFile.getAbsolutePath();
   const char* argv[] = {"test", flag.c_str(), "--config-file", pathArg.c_str()};
   bool help = false;
   return read(odesc, 4, argv, &help);
}

} // anonymous namespace

TEST(ProgramOptionsCheckConfigTest, CleanConfigPasses)
{
   FilePath cfg = writeConfig("valid-one=hello\nvalid-two=world\n");
   ProgramStatus status = runCheck("--check-config", cfg);
   EXPECT_TRUE(status.exit());
   EXPECT_EQ(EXIT_SUCCESS, status.exitCode());
   cfg.removeIfExists();
}

TEST(ProgramOptionsCheckConfigTest, ReportsAllUnrecognizedKeysInOnePass)
{
   FilePath cfg = writeConfig("valid-one=hi\nbogus-one=1\nvalid-two=ok\nbogus-two=2\n");

   testing::internal::CaptureStdout();
   ProgramStatus status = runCheck("--check-config", cfg);
   std::string out = testing::internal::GetCapturedStdout();

   EXPECT_TRUE(status.exit());
   EXPECT_EQ(EXIT_FAILURE, status.exitCode());
   // both unrecognized keys must be reported in the single pass
   EXPECT_NE(std::string::npos, out.find("bogus-one"));
   EXPECT_NE(std::string::npos, out.find("bogus-two"));
   cfg.removeIfExists();
}

TEST(ProgramOptionsCheckConfigTest, MissingConfigFileFails)
{
   OptionsDescription odesc = testOptions();
   // "none" is the sentinel that suppresses the default config file, leaving
   // no config to validate.
   const char* argv[] = {"test", "--check-config", "--config-file", "none"};
   bool help = false;

   testing::internal::CaptureStdout();
   ProgramStatus status = read(odesc, 4, argv, &help);
   std::string out = testing::internal::GetCapturedStdout();

   EXPECT_TRUE(status.exit());
   EXPECT_EQ(EXIT_FAILURE, status.exitCode());
   EXPECT_NE(std::string::npos, out.find("No configuration file"));
}

TEST(ProgramOptionsCheckConfigTest, TestConfigIsDeprecatedAliasOfCheckConfig)
{
   FilePath cfg = writeConfig("valid-one=hi\nbogus-one=1\nbogus-two=2\n");

   testing::internal::CaptureStderr();
   testing::internal::CaptureStdout();
   ProgramStatus status = runCheck("--test-config", cfg);
   std::string out = testing::internal::GetCapturedStdout();
   std::string err = testing::internal::GetCapturedStderr();

   // same whole-config behavior as --check-config (all keys, one pass)
   EXPECT_TRUE(status.exit());
   EXPECT_EQ(EXIT_FAILURE, status.exitCode());
   EXPECT_NE(std::string::npos, out.find("bogus-one"));
   EXPECT_NE(std::string::npos, out.find("bogus-two"));
   // and it emits a deprecation warning
   EXPECT_NE(std::string::npos, err.find("deprecated"));
   cfg.removeIfExists();
}

TEST(ProgramOptionsCheckConfigTest, DoesNotStartServiceOnCleanConfig)
{
   FilePath cfg = writeConfig("valid-one=hello\n");
   ProgramStatus status = runCheck("--check-config", cfg);
   // a check must always exit, never fall through to running the service
   EXPECT_TRUE(status.exit());
   cfg.removeIfExists();
}

TEST(ProgramOptionsCheckConfigTest, DeferredCleanConfigReturnsRun)
{
   // When deferCheckConfig=true a clean check must return run() so the caller
   // can perform extended checks before exiting -- it must NOT return exitSuccess.
   FilePath cfg = writeConfig("valid-one=hello\n");
   OptionsDescription odesc = testOptions();
   std::string pathArg = cfg.getAbsolutePath();
   const char* argv[] = {"test", "--check-config", "--config-file", pathArg.c_str()};
   bool help = false;

   testing::internal::CaptureStdout();
   ProgramStatus status = read(odesc, 4, argv, &help,
                               false /* allowUnregisteredConfigOptions */,
                               false /* configFileHasPrecedence */,
                               true  /* deferCheckConfig */);
   testing::internal::GetCapturedStdout();

   EXPECT_FALSE(status.exit()); // run(), not exitSuccess()
   cfg.removeIfExists();
}

TEST(ProgramOptionsReadTest, NoCheckFlagRunsNormally)
{
   FilePath cfg = writeConfig("valid-one=hello\n");
   OptionsDescription odesc = testOptions();
   std::string pathArg = cfg.getAbsolutePath();
   const char* argv[] = {"test", "--config-file", pathArg.c_str()};
   bool help = false;
   ProgramStatus status = read(odesc, 3, argv, &help);
   // without a check flag, a valid config yields a run (not an exit)
   EXPECT_FALSE(status.exit());
   cfg.removeIfExists();
}

} // namespace program_options
} // namespace core
} // namespace rstudio
