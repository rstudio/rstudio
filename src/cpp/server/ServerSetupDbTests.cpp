/*
 * ServerSetupDbTests.cpp
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

#include <cstdio>
#include <cstdlib>
#include <fstream>
#include <sstream>
#include <unistd.h>

#include <gtest/gtest.h>

#include <core/FileSerializer.hpp>
#include <shared_core/FilePath.hpp>

#include "ServerSetupDb.hpp"

using namespace rstudio::core;

namespace {

// Creates a fresh temp directory for a test's config/credentials file and
// returns it; the caller is responsible for removing it.
FilePath makeTempDir()
{
   FilePath dir;
   Error error = FilePath::tempFilePath(dir);
   EXPECT_FALSE(error);
   error = dir.ensureDirectory();
   EXPECT_FALSE(error);
   return dir;
}

} // anonymous namespace

namespace rstudio {
namespace server {

TEST(ServerSetupDbTests, RejectsInvalidIdentifier)
{
   std::ostringstream out;
   bool passed = true;
   Error error = validateIdentifier("not-a-valid-name; DROP TABLE x", out, &passed);
   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
   EXPECT_NE(std::string::npos, out.str().find("[FAIL]"));
}

TEST(ServerSetupDbTests, AcceptsValidIdentifier)
{
   std::ostringstream out;
   bool passed = true;
   Error error = validateIdentifier("rstudio_os", out, &passed);
   EXPECT_FALSE(error);
   EXPECT_TRUE(passed);
   EXPECT_TRUE(out.str().empty());
}

TEST(ServerSetupDbTests, AcceptsIdentifierWithHyphenAfterLeadingChar)
{
   // The tool's own default database name (kDefaultOpenSourceDatabaseName,
   // "rstudio-os") must pass its own validation.
   std::ostringstream out;
   bool passed = true;
   Error error = validateIdentifier("rstudio-os", out, &passed);
   EXPECT_FALSE(error);
   EXPECT_TRUE(passed);
   EXPECT_TRUE(out.str().empty());
}

TEST(ServerSetupDbTests, RejectsIdentifierWithLoneSingleQuote)
{
   std::ostringstream out;
   bool passed = true;
   Error error = validateIdentifier("x'y", out, &passed);
   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
}

TEST(ServerSetupDbTests, RejectsEmptyIdentifier)
{
   std::ostringstream out;
   bool passed = true;
   Error error = validateIdentifier("", out, &passed);
   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
}

TEST(ServerSetupDbTests, RejectsIdentifierWithLeadingDigit)
{
   std::ostringstream out;
   bool passed = true;
   Error error = validateIdentifier("1rstudio", out, &passed);
   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
}

TEST(ServerSetupDbTests, RejectsIdentifierWithLeadingHyphen)
{
   std::ostringstream out;
   bool passed = true;
   Error error = validateIdentifier("-rstudio", out, &passed);
   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
}

TEST(ServerSetupDbTests, RejectsIdentifierThatIsOnlyAHyphen)
{
   std::ostringstream out;
   bool passed = true;
   Error error = validateIdentifier("-", out, &passed);
   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
}

TEST(ServerSetupDbTests, RejectsIdentifierWithBackslash)
{
   std::ostringstream out;
   bool passed = true;
   Error error = validateIdentifier("x\\y", out, &passed);
   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
}

TEST(ServerSetupDbTests, RejectsIdentifierWithDoubleQuote)
{
   std::ostringstream out;
   bool passed = true;
   Error error = validateIdentifier("x\"y", out, &passed);
   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
}

TEST(ServerSetupDbTests, GeneratesPasswordWithSafeCharsetAndLength)
{
   std::string password;
   Error error = generateServiceUserPassword(&password);
   EXPECT_FALSE(error);
   EXPECT_GE(password.size(), 24u);
   EXPECT_EQ(std::string::npos, password.find('\''));
   EXPECT_EQ(std::string::npos, password.find('"'));
   EXPECT_EQ(std::string::npos, password.find('\\'));
   EXPECT_EQ(std::string::npos, password.find(';'));

   // Every generated character must be a member of the real charset, not
   // merely absent from the four forbidden characters checked above.
   const std::string& charset = serviceUserPasswordCharset();
   for (char c : password)
      EXPECT_NE(std::string::npos, charset.find(c));
}

TEST(ServerSetupDbTests, GeneratesDifferentPasswordsEachCall)
{
   // Not a rigorous randomness test -- just a sanity check that the OpenSSL
   // CSPRNG isn't accidentally producing the same output every call.
   std::string password1;
   std::string password2;
   Error error1 = generateServiceUserPassword(&password1);
   Error error2 = generateServiceUserPassword(&password2);
   EXPECT_FALSE(error1);
   EXPECT_FALSE(error2);
   EXPECT_NE(password1, password2);
}

TEST(ServerSetupDbTests, ResolveMasterPasswordPrefersFileOverEnvAndPrompt)
{
   char path[] = "/tmp/setup-db-master-password-XXXXXX";
   int fd = mkstemp(path);
   ASSERT_GE(fd, 0);
   close(fd);
   {
      std::ofstream file(path);
      file << "from-file-secret\nignored-second-line\n";
   }

   setenv("RSERVER_SETUP_DB_MASTER_PASSWORD", "from-env-secret", 1 /* overwrite */);

   std::istringstream in("");
   std::ostringstream out;
   std::string password;
   Error error = resolveMasterPassword(path, in, out, &password);

   unsetenv("RSERVER_SETUP_DB_MASTER_PASSWORD");
   std::remove(path);

   EXPECT_FALSE(error);
   EXPECT_EQ("from-file-secret", password);
}

TEST(ServerSetupDbTests, ResolveMasterPasswordFallsBackToEnvVar)
{
   setenv("RSERVER_SETUP_DB_MASTER_PASSWORD", "from-env-secret", 1 /* overwrite */);

   std::istringstream in("");
   std::ostringstream out;
   std::string password;
   Error error = resolveMasterPassword(std::string(), in, out, &password);

   unsetenv("RSERVER_SETUP_DB_MASTER_PASSWORD");

   EXPECT_FALSE(error);
   EXPECT_EQ("from-env-secret", password);
}

TEST(ServerSetupDbTests, ResolveMasterPasswordFallsBackToPromptWhenUnset)
{
   unsetenv("RSERVER_SETUP_DB_MASTER_PASSWORD");

   std::istringstream in("typed-secret\n");
   std::ostringstream out;
   std::string password;
   Error error = resolveMasterPassword(std::string(), in, out, &password);

   EXPECT_FALSE(error);
   EXPECT_EQ("typed-secret", password);
   EXPECT_NE(std::string::npos, out.str().find("Master password:"));
}

TEST(ServerSetupDbTests, ResolveMasterPasswordFailsOnUnreadableFile)
{
   std::istringstream in("");
   std::ostringstream out;
   std::string password;
   Error error = resolveMasterPassword("/nonexistent/path/that/does/not/exist", in, out, &password);
   EXPECT_TRUE(error);
}

TEST(ServerSetupDbTests, ConnectAsMasterReportsConnectFailure)
{
   // No PostgreSQL server listening on this port, so connect() should fail
   // and be reported as [FAIL] rather than propagated as an Error.
   std::istringstream in("127.0.0.1\n1\nno-such-user\n");
   std::ostringstream out;
   setenv("RSERVER_SETUP_DB_MASTER_PASSWORD", "unused", 1 /* overwrite */);

   SetupDbFlags flags;
   boost::shared_ptr<database::IConnection> pConnection;
   database::PostgresqlConnectionOptions masterOptions;
   bool passed = true;
   Error error = connectAsMaster(flags, in, out, &pConnection, &masterOptions, &passed);

   unsetenv("RSERVER_SETUP_DB_MASTER_PASSWORD");

   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
   EXPECT_NE(std::string::npos, out.str().find("[FAIL]"));
}

TEST(ServerSetupDbTests, MergeWriteDatabaseConfigFilePreservesUnrelatedKeys)
{
   FilePath dir = makeTempDir();
   FilePath configFile = dir.completeChildPath("database.conf");
   Error error = writeStringToFile(configFile, "foo=bar\n");
   ASSERT_FALSE(error);

   error = mergeWriteDatabaseConfigFile(configFile, "db.internal", "5432",
                                         "rstudio-os", "rstudio-os", "s3cr3t");
   EXPECT_FALSE(error);

   std::string contents;
   error = readStringFromFile(configFile, &contents);
   ASSERT_FALSE(error);

   // Settings writes each key as key="value" (see FileSerializer's
   // stringifyStringPair), not the bare key=value that database.conf
   // itself accepts as input.
   EXPECT_NE(std::string::npos, contents.find("foo=\"bar\""));
   EXPECT_NE(std::string::npos, contents.find("provider=\"postgresql\""));
   EXPECT_NE(std::string::npos, contents.find("host=\"db.internal\""));
   EXPECT_NE(std::string::npos, contents.find("port=\"5432\""));
   EXPECT_NE(std::string::npos, contents.find("database=\"rstudio-os\""));
   EXPECT_NE(std::string::npos, contents.find("username=\"rstudio-os\""));
   EXPECT_NE(std::string::npos, contents.find("password=\"s3cr3t\""));

   dir.remove();
}

TEST(ServerSetupDbTests, MergeWriteDatabaseConfigFileOverwritesStaleValue)
{
   FilePath dir = makeTempDir();
   FilePath configFile = dir.completeChildPath("database.conf");
   Error error = writeStringToFile(configFile, "host=old\n");
   ASSERT_FALSE(error);

   error = mergeWriteDatabaseConfigFile(configFile, "db.internal", "5432",
                                         "rstudio-os", "rstudio-os", "s3cr3t");
   EXPECT_FALSE(error);

   std::string contents;
   error = readStringFromFile(configFile, &contents);
   ASSERT_FALSE(error);

   EXPECT_NE(std::string::npos, contents.find("host=\"db.internal\""));
   EXPECT_EQ(std::string::npos, contents.find("old"));

   dir.remove();
}

TEST(ServerSetupDbTests, EnsureFileExistsWithUserOnlyModeTightensPreexistingLooseMode)
{
   FilePath dir = makeTempDir();
   FilePath configFile = dir.completeChildPath("database.conf");
   Error error = writeStringToFile(configFile, "foo=bar\n");
   ASSERT_FALSE(error);

   // Simulate a pre-existing config file created under a permissive umask
   // (0644) -- the common case, since this tool adds credentials to config an
   // admin already has. It must be tightened to 0600 up front, before any
   // writer puts the plaintext password into it. (The writers' trailing chmod
   // would mask this at the final state, so assert against the helper the
   // writers call before writing rather than the writers themselves.)
   error = configFile.changeFileMode(FileMode::USER_READ_WRITE_ALL_READ);
   ASSERT_FALSE(error);

   error = ensureFileExistsWithUserOnlyMode(configFile);
   EXPECT_FALSE(error);

   FileMode mode;
   error = configFile.getFileMode(mode);
   ASSERT_FALSE(error);
   EXPECT_EQ(FileMode::USER_READ_WRITE, mode);

   // The pre-existing content is left intact -- the helper only touches the mode.
   std::string contents;
   error = readStringFromFile(configFile, &contents);
   ASSERT_FALSE(error);
   EXPECT_EQ("foo=bar\n", contents);

   dir.remove();
}

TEST(ServerSetupDbTests, EnsureFileExistsWithUserOnlyModeCreatesMissingFileAt0600)
{
   FilePath dir = makeTempDir();
   FilePath configFile = dir.completeChildPath("database.conf");
   ASSERT_FALSE(configFile.exists());

   Error error = ensureFileExistsWithUserOnlyMode(configFile);
   EXPECT_FALSE(error);
   ASSERT_TRUE(configFile.exists());

   FileMode mode;
   error = configFile.getFileMode(mode);
   ASSERT_FALSE(error);
   EXPECT_EQ(FileMode::USER_READ_WRITE, mode);

   dir.remove();
}

TEST(ServerSetupDbTests, WriteCredentialsFileFreshWritesExpectedContentAt0600)
{
   FilePath dir = makeTempDir();
   FilePath credentialsFile = dir.completeChildPath("rserver-setup-db-credentials");

   Error error = writeCredentialsFileFresh(credentialsFile, "db.internal", "5432",
                                            "rstudio-os", "rstudio-os", "s3cr3t");
   EXPECT_FALSE(error);

   std::string contents;
   error = readStringFromFile(credentialsFile, &contents);
   ASSERT_FALSE(error);
   EXPECT_EQ("provider=postgresql\n"
             "host=db.internal\n"
             "port=5432\n"
             "database=rstudio-os\n"
             "username=rstudio-os\n"
             "password=s3cr3t\n",
             contents);

   FileMode mode;
   error = credentialsFile.getFileMode(mode);
   ASSERT_FALSE(error);
   EXPECT_EQ(FileMode::USER_READ_WRITE, mode);

   dir.remove();
}

} // namespace server
} // namespace rstudio
