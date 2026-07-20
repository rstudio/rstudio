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

TEST(ServerSetupDbTests, ConnectAsMasterFailsOnEofForMissingHost)
{
   // The host prompt is the first one connectAsMaster issues, so hitting EOF
   // there is reachable without ever attempting a connection -- no live
   // PostgreSQL server needed for this case.
   std::istringstream in(""); // at EOF from the start; host flag left empty
   std::ostringstream out;

   SetupDbFlags flags;
   boost::shared_ptr<database::IConnection> pConnection;
   database::PostgresqlConnectionOptions masterOptions;
   bool passed = true;
   Error error = connectAsMaster(flags, in, out, &pConnection, &masterOptions, &passed);

   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
   EXPECT_NE(std::string::npos, out.str().find("[FAIL]"));
   EXPECT_NE(std::string::npos, out.str().find("--setup-db-host"));
}

TEST(ServerSetupDbTests, ConnectAsMasterFailsOnEofForMissingPort)
{
   // Supply host via flag so the port prompt is the one reached at EOF.
   std::istringstream in("");
   std::ostringstream out;

   SetupDbFlags flags;
   flags.host = "127.0.0.1";
   boost::shared_ptr<database::IConnection> pConnection;
   database::PostgresqlConnectionOptions masterOptions;
   bool passed = true;
   Error error = connectAsMaster(flags, in, out, &pConnection, &masterOptions, &passed);

   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
   EXPECT_NE(std::string::npos, out.str().find("--setup-db-port"));
}

TEST(ServerSetupDbTests, ConnectAsMasterFailsOnEofForMissingMasterUsername)
{
   // Supply host and port via flags so the master username prompt is the one
   // reached at EOF.
   std::istringstream in("");
   std::ostringstream out;

   SetupDbFlags flags;
   flags.host = "127.0.0.1";
   flags.port = "5432";
   boost::shared_ptr<database::IConnection> pConnection;
   database::PostgresqlConnectionOptions masterOptions;
   bool passed = true;
   Error error = connectAsMaster(flags, in, out, &pConnection, &masterOptions, &passed);

   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
   EXPECT_NE(std::string::npos, out.str().find("--setup-db-master-username"));
}

TEST(ServerSetupDbTests, ConnectAsMasterFailsOnEofForMissingMasterPassword)
{
   // Supply host/port/master-username via flags and provide neither a
   // password file nor RSERVER_SETUP_DB_MASTER_PASSWORD, so the masked
   // password prompt is the one reached at EOF. Before this hardening the
   // prompt returned "" on exhausted stdin and setup proceeded with an empty
   // password; it must now fail loudly like the sibling prompts.
   unsetenv("RSERVER_SETUP_DB_MASTER_PASSWORD");
   std::istringstream in("");
   std::ostringstream out;

   SetupDbFlags flags;
   flags.host = "127.0.0.1";
   flags.port = "5432";
   flags.masterUsername = "postgres";
   boost::shared_ptr<database::IConnection> pConnection;
   database::PostgresqlConnectionOptions masterOptions;
   bool passed = true;
   Error error = connectAsMaster(flags, in, out, &pConnection, &masterOptions, &passed);

   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
   EXPECT_NE(std::string::npos, out.str().find("[FAIL]"));
   EXPECT_NE(std::string::npos, out.str().find("--setup-db-master-password-file"));
   EXPECT_NE(std::string::npos, out.str().find("RSERVER_SETUP_DB_MASTER_PASSWORD"));
   // Must not have reached the connect attempt with an empty password.
   EXPECT_EQ(std::string::npos, out.str().find("[FAIL] Could not connect"));
}

TEST(ServerSetupDbTests, ConnectAsMasterHonorsHostPortAndMasterUsernameFlags)
{
   // host/port/masterUsername are all supplied via flags against a fully
   // empty (already-at-EOF) input stream. If any of the three flags were not
   // honored, the corresponding prompt would consume from the empty stream,
   // hit EOF, and fail with a "no ... provided" message before ever
   // attempting to connect. Getting past all three to a real (failing, since
   // nothing is listening) connect attempt proves the flags were honored and
   // their prompts skipped.
   std::istringstream in("");
   std::ostringstream out;
   setenv("RSERVER_SETUP_DB_MASTER_PASSWORD", "unused", 1 /* overwrite */);

   SetupDbFlags flags;
   flags.host = "127.0.0.1";
   flags.port = "1";
   flags.masterUsername = "no-such-user";
   boost::shared_ptr<database::IConnection> pConnection;
   database::PostgresqlConnectionOptions masterOptions;
   bool passed = true;
   Error error = connectAsMaster(flags, in, out, &pConnection, &masterOptions, &passed);

   unsetenv("RSERVER_SETUP_DB_MASTER_PASSWORD");

   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
   EXPECT_EQ(std::string::npos, out.str().find("No PostgreSQL host provided"));
   EXPECT_EQ(std::string::npos, out.str().find("No PostgreSQL port provided"));
   EXPECT_EQ(std::string::npos, out.str().find("No PostgreSQL master username provided"));
   EXPECT_NE(std::string::npos, out.str().find("[FAIL] Could not connect"));

   // Prove the flag values flowed into the master connection options (which
   // are populated before connect() and survive its failure), not merely that
   // the prompts were skipped -- a bug that skipped the prompt but assigned a
   // wrong/fixed value would still get past the EOF guards above.
   EXPECT_EQ("127.0.0.1", masterOptions.host);
   EXPECT_EQ("1", masterOptions.port);
   EXPECT_EQ("no-such-user", masterOptions.username);
}

TEST(ServerSetupDbTests, ConnectAsMasterBlankPortLineKeepsDefaultWhenNotEof)
{
   // The port prompt receives a blank line, but more input follows on the
   // stream, so this is an interactive "press enter to accept the default"
   // rather than an exhausted-stdin EOF. The blank must fall through to the
   // documented 5432 default instead of tripping the EOF guard. This is the
   // only test that exercises the not-eof side of promptLine's
   // (!in.good() && value.empty()) predicate: with every other guarded-prompt
   // test feeding either "" (immediate EOF) or a flag value, weakening the
   // predicate to just value.empty() would otherwise leave the suite green.
   std::istringstream in("\nno-such-user\n"); // blank port, then master username
   std::ostringstream out;
   setenv("RSERVER_SETUP_DB_MASTER_PASSWORD", "unused", 1 /* overwrite */);

   SetupDbFlags flags;
   flags.host = "127.0.0.1"; // supplied so the port prompt is the blank one
   boost::shared_ptr<database::IConnection> pConnection;
   database::PostgresqlConnectionOptions masterOptions;
   bool passed = true;
   Error error = connectAsMaster(flags, in, out, &pConnection, &masterOptions, &passed);

   unsetenv("RSERVER_SETUP_DB_MASTER_PASSWORD");

   EXPECT_FALSE(error);
   // The blank port must not be mistaken for exhausted stdin...
   EXPECT_EQ(std::string::npos, out.str().find("No PostgreSQL port provided"));
   // ...and must default to 5432. masterOptions.port is assigned immediately
   // before connect(), so this holds whether or not anything is actually
   // listening on 5432 -- keeping the test independent of the local
   // environment while still proving both the port and username prompts were
   // reached and cleared without tripping the EOF guard.
   EXPECT_EQ("5432", masterOptions.port);
}

TEST(ServerSetupDbTests, SetupDbFailsOnEofForMissingDatabaseName)
{
   // The database-name prompt is the first setupDb() issues, and this EOF
   // path returns before pMasterConnection is ever dereferenced, so a null
   // connection is safe here -- no live PostgreSQL server needed.
   Options& options = server::options();
   SetupDbFlags flags;
   std::istringstream in("");
   std::ostringstream out;
   boost::shared_ptr<database::IConnection> pMasterConnection;
   database::PostgresqlConnectionOptions masterOptions;
   bool passed = true;
   Error error = setupDb(options, flags, pMasterConnection, masterOptions, in, out, &passed);

   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
   EXPECT_NE(std::string::npos, out.str().find("[FAIL]"));
   EXPECT_NE(std::string::npos, out.str().find("--setup-db-database-name"));
}

TEST(ServerSetupDbTests, SetupDbFailsOnEofForMissingDatabaseUser)
{
   // Supply the database name via flag so the database-user prompt is the
   // one reached at EOF; still returns before pMasterConnection is
   // dereferenced.
   Options& options = server::options();
   SetupDbFlags flags;
   flags.databaseName = "rstudio-os";
   std::istringstream in("");
   std::ostringstream out;
   boost::shared_ptr<database::IConnection> pMasterConnection;
   database::PostgresqlConnectionOptions masterOptions;
   bool passed = true;
   Error error = setupDb(options, flags, pMasterConnection, masterOptions, in, out, &passed);

   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
   EXPECT_NE(std::string::npos, out.str().find("--setup-db-database-user"));
}

TEST(ServerSetupDbTests, SetupDbHonorsDatabaseNameAndUserFlagsAndSkipsPrompts)
{
   // Supplying invalid identifiers (rather than valid ones) via flags keeps
   // this test DB-free: an invalid identifier fails validateIdentifier() and
   // returns before setupDb() ever touches pMasterConnection, while still
   // proving the flags were honored -- if they were NOT honored, the empty
   // (already-at-EOF) `in` stream below would instead trip the "no ...
   // provided" EOF guard first.
   Options& options = server::options();
   SetupDbFlags flags;
   flags.databaseName = "not valid!";
   flags.databaseUser = "not valid!";
   std::istringstream in("");
   std::ostringstream out;
   boost::shared_ptr<database::IConnection> pMasterConnection;
   database::PostgresqlConnectionOptions masterOptions;
   bool passed = true;
   Error error = setupDb(options, flags, pMasterConnection, masterOptions, in, out, &passed);

   EXPECT_FALSE(error);
   EXPECT_FALSE(passed);
   EXPECT_EQ(std::string::npos, out.str().find("No database name provided"));
   EXPECT_EQ(std::string::npos, out.str().find("No database user provided"));
   EXPECT_NE(std::string::npos, out.str().find("not a valid database/user name"));
   // The invalid-identifier message quotes the offending name, so asserting the
   // supplied flag value appears proves the flag value itself reached
   // validateIdentifier() -- not just that the prompt was skipped.
   EXPECT_NE(std::string::npos, out.str().find("\"not valid!\""));
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
