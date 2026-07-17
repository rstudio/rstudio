/*
 * ServerSetupDb.cpp
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

#include "ServerSetupDb.hpp"

#include <cstdlib>
#include <fstream>
#include <iostream>
#include <regex>
#include <sstream>
#include <termios.h>
#include <unistd.h>
#include <vector>

#include <boost/system/error_code.hpp>

#include <core/Database.hpp>
#include <core/FileSerializer.hpp>
#include <core/Settings.hpp>
#include <core/system/Xdg.hpp>
#include <server_core/DatabaseConstants.hpp>
#include <server_core/ServerDatabaseOverlay.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/system/Crypto.hpp>

using namespace rstudio::core;
using namespace rstudio::core::database;

namespace rstudio {
namespace server {

namespace {

const char* const kMasterPasswordEnvVar = "RSERVER_SETUP_DB_MASTER_PASSWORD";
const char* const kDatabaseConfigFileName = "database.conf";
const std::string kPasswordCharset =
   "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#%^*-_=+";

// Reads a line from stdin with terminal echo disabled, for the master
// password prompt. Restores echo before returning, even on error. Only
// disables echo when `in` is actually the controlling terminal (stdin is
// both connected to a real tty and the stream in use); under test, `in` is
// a plain istringstream and this is a no-op.
std::string readMaskedLine(std::istream& in, std::ostream& out)
{
   bool isRealTty = (&in == &std::cin) && (isatty(STDIN_FILENO) != 0);
   bool settingsSaved = false;
   termios oldSettings{};

   if (isRealTty)
   {
      settingsSaved = (tcgetattr(STDIN_FILENO, &oldSettings) == 0);
      if (settingsSaved)
      {
         termios noEcho = oldSettings;
         noEcho.c_lflag &= ~ECHO;
         if (tcsetattr(STDIN_FILENO, TCSANOW, &noEcho) != 0)
         {
            out << "[WARN] Could not disable terminal echo; password may be visible."
                << std::endl;
         }
      }
   }

   std::string value;
   std::getline(in, value);

   // Only restore if we actually saved the prior settings above; if
   // tcgetattr failed, oldSettings was never populated and there is nothing
   // to restore.
   if (settingsSaved)
      tcsetattr(STDIN_FILENO, TCSANOW, &oldSettings);
   if (isRealTty)
      out << std::endl;

   return value;
}

// Reads a line from `in`, writing `prompt` to `out` first. If `pEof` is
// non-null, *pEof is set to true only when the stream hit end-of-input with
// nothing typed -- interactively pressing enter also yields an empty value,
// but without eof(), so that case leaves *pEof false and existing
// empty-value defaults still apply. Callers use *pEof to distinguish a
// deliberate blank answer from a non-interactive run that piped in less
// input than --setup-db needed.
std::string promptLine(std::istream& in, std::ostream& out, const std::string& prompt,
                        bool* pEof = nullptr)
{
   out << prompt;
   std::string value;
   std::getline(in, value);
   if (pEof != nullptr)
      *pEof = in.eof() && value.empty();
   return value;
}

// Sets *pFound to whether `sql` (expected to be a `SELECT 1 FROM ...`-style
// existence check) returned any row. A failed query propagates its Error
// rather than being folded into *pFound, so callers can distinguish "row
// absent" from "could not check" -- the latter must not be treated as a
// green light by an idempotency guard.
Error rowExists(boost::shared_ptr<IConnection> pConnection, const std::string& sql, bool* pFound)
{
   *pFound = false;
   return execAndProcessQuery(pConnection, sql,
      [pFound](const Row&) { *pFound = true; });
}

// Shared by validateIdentifier() (used to report --setup-db's own
// database-name/user prompts) and createDatabaseAndUser()'s internal guard
// (defense in depth for the Pro overlay, which also calls
// createDatabaseAndUser directly).
bool isValidIdentifier(const std::string& identifier)
{
   static const std::regex kValidIdentifier("^[A-Za-z_][A-Za-z0-9_-]*$");
   return std::regex_match(identifier, kValidIdentifier);
}

// Resolves the database.conf path the same way ServerDatabase.cpp's
// getDatabaseFilePath() does: the explicitly configured path if non-empty,
// otherwise the system default location (typically /etc/rstudio/database.conf).
FilePath resolveDatabaseConfigFile(const std::string& databaseConfigFile)
{
   return !databaseConfigFile.empty()
      ? FilePath(databaseConfigFile)
      : core::system::xdg::findSystemConfigFile("database configuration", kDatabaseConfigFileName);
}

} // anonymous namespace

// Ensures `path` exists and is 0600 before any plaintext secret is written
// into it: creates it empty if missing, and tightens its mode to 0600 whether
// or not it already existed. A truncating write preserves an existing file's
// mode, so a pre-existing database.conf left at a permissive umask (0644 is
// the common case -- this tool's job is to add credentials to config the
// admin already has) would otherwise receive the plaintext password at that
// looser mode and only be narrowed by the trailing changeFileMode each writer
// applies, leaving a read window (and, if the process is interrupted between
// the write and that chmod, a permanently world-readable secret). Chmodding
// up front closes that window and makes the trailing chmod belt-and-suspenders.
Error ensureFileExistsWithUserOnlyMode(const FilePath& path)
{
   if (!path.exists())
   {
      Error error = writeStringToFile(path, std::string());
      if (error)
         return error;
   }

   return path.changeFileMode(core::FileMode::USER_READ_WRITE);
}

// Writes the given key/value pairs as a fresh, 0600 config file at `path`,
// overwriting anything already there. Used only for the standalone
// --print-only credentials file: it has no other keys worth preserving, and
// may be stale from a previous run, so a fresh write is the right behavior
// there (unlike database.conf -- see mergeWriteDatabaseConfigFile below).
Error writeCredentialsFileFresh(const FilePath& path,
                                 const std::string& host,
                                 const std::string& port,
                                 const std::string& dbName,
                                 const std::string& dbUser,
                                 const std::string& password)
{
   std::ostringstream contents;
   contents << "provider=postgresql" << std::endl
            << "host=" << host << std::endl
            << "port=" << port << std::endl
            << "database=" << dbName << std::endl
            << "username=" << dbUser << std::endl
            << "password=" << password << std::endl;

   Error error = path.getParent().ensureDirectory();
   if (error)
      return error;

   error = ensureFileExistsWithUserOnlyMode(path);
   if (error)
      return error;

   error = writeStringToFile(path, contents.str());
   if (error)
      return error;

   return path.changeFileMode(core::FileMode::USER_READ_WRITE);
}

// Loads any existing database.conf at `path` (a missing file is not an
// error -- Settings::initialize() starts from an empty map), sets the six
// connection keys, and writes it back -- preserving any other keys the
// admin had already configured there rather than clobbering the whole file.
// Settings doesn't set file permissions on write, so the file is locked down
// to 0600 before Settings ever writes to it (in case it doesn't already
// exist), and again afterward to match writeCredentialsFileFresh's behavior.
Error mergeWriteDatabaseConfigFile(const FilePath& path,
                                    const std::string& host,
                                    const std::string& port,
                                    const std::string& dbName,
                                    const std::string& dbUser,
                                    const std::string& password)
{
   Error error = path.getParent().ensureDirectory();
   if (error)
      return error;

   error = ensureFileExistsWithUserOnlyMode(path);
   if (error)
      return error;

   Settings settings;
   error = settings.initialize(path);
   if (error)
      return error;

   const std::string provider = "postgresql";
   settings.beginUpdate();
   settings.set("provider", provider);
   settings.set("host", host);
   settings.set("port", port);
   settings.set("database", dbName);
   settings.set("username", dbUser);
   settings.set("password", password);
   settings.endUpdate();

   error = path.changeFileMode(core::FileMode::USER_READ_WRITE);
   if (error)
      return error;

   // Settings::endUpdate() writes via Settings::writeSettings(), which
   // returns void and only LOG_ERRORs on a failed write -- so on its own, a
   // truncated or failed write here would still let the caller report
   // success. Re-read the file we just wrote via a fresh Settings and
   // confirm each of the six connection keys round-tripped correctly before
   // reporting success ourselves.
   Settings written;
   error = written.initialize(path);
   if (error)
      return error;

   const std::pair<std::string, std::string> expectedValues[] = {
      { "provider", provider },
      { "host", host },
      { "port", port },
      { "database", dbName },
      { "username", dbUser },
      { "password", password }
   };
   for (const auto& expected : expectedValues)
   {
      if (written.get(expected.first) != expected.second)
      {
         return systemError(boost::system::errc::io_error,
                            "Failed to persist database configuration to " + path.getAbsolutePath(),
                            ERROR_LOCATION);
      }
   }

   return Success();
}

Error validateIdentifier(const std::string& identifier, std::ostream& out, bool* pPassed)
{
   if (!isValidIdentifier(identifier))
   {
      out << "[FAIL] \"" << identifier
          << "\" is not a valid database/user name (must match ^[A-Za-z_][A-Za-z0-9_-]*$)"
          << std::endl;
      *pPassed = false;
   }
   return Success();
}

Error generateServiceUserPassword(std::string* pPassword)
{
   const int kLength = 32;
   const int kCharsetSize = static_cast<int>(kPasswordCharset.size());

   // byte % kCharsetSize is biased toward the low end of the charset
   // whenever 256 isn't an exact multiple of kCharsetSize, so reject any
   // byte at or above the highest multiple of kCharsetSize below 256.
   // Computed as int (not unsigned char) so this stays correct even if
   // kCharsetSize is ever changed to a value that evenly divides 256.
   const int kRejectionThreshold = 256 - (256 % kCharsetSize);

   pPassword->clear();
   pPassword->reserve(kLength);

   while (static_cast<int>(pPassword->size()) < kLength)
   {
      std::vector<unsigned char> randomBytes;
      Error error = core::system::crypto::random(kLength, randomBytes);
      if (error)
         return error;

      for (unsigned char byte : randomBytes)
      {
         if (static_cast<int>(pPassword->size()) >= kLength)
            break;
         if (static_cast<int>(byte) < kRejectionThreshold)
            *pPassword += kPasswordCharset[byte % kCharsetSize];
      }
   }

   return Success();
}

const std::string& serviceUserPasswordCharset()
{
   return kPasswordCharset;
}

Error resolveMasterPassword(const std::string& masterPasswordFile,
                             std::istream& in,
                             std::ostream& out,
                             std::string* pPassword)
{
   if (!masterPasswordFile.empty())
   {
      std::ifstream file(masterPasswordFile);
      if (!file)
      {
         return systemError(boost::system::errc::no_such_file_or_directory,
                            "Could not read master password file: " + masterPasswordFile,
                            ERROR_LOCATION);
      }
      std::string line;
      std::getline(file, line);
      if (line.empty())
      {
         return systemError(boost::system::errc::invalid_argument,
                            "Master password file is empty: " + masterPasswordFile,
                            ERROR_LOCATION);
      }
      *pPassword = line;
      return Success();
   }

   const char* envPassword = std::getenv(kMasterPasswordEnvVar);
   if (envPassword != nullptr)
   {
      *pPassword = envPassword;
      return Success();
   }

   out << "Master password: ";
   *pPassword = readMaskedLine(in, out);
   return Success();
}

Error connectAsMaster(const SetupDbFlags& flags,
                       std::istream& in,
                       std::ostream& out,
                       boost::shared_ptr<IConnection>* pConnection,
                       PostgresqlConnectionOptions* pMasterOptions,
                       bool* pPassed)
{
   *pPassed = true;

   std::string host = flags.host;
   if (host.empty())
   {
      bool eof = false;
      host = promptLine(in, out, "PostgreSQL host: ", &eof);
      if (eof)
      {
         out << "[FAIL] No PostgreSQL host provided. Pass --setup-db-host to run --setup-db "
                "without interactive input." << std::endl;
         *pPassed = false;
         return Success();
      }
   }

   std::string port = flags.port;
   if (port.empty())
   {
      bool eof = false;
      port = promptLine(in, out, "PostgreSQL port [5432]: ", &eof);
      if (eof)
      {
         out << "[FAIL] No PostgreSQL port provided. Pass --setup-db-port to run --setup-db "
                "without interactive input." << std::endl;
         *pPassed = false;
         return Success();
      }
   }
   if (port.empty())
      port = "5432";

   std::string masterUser = flags.masterUsername;
   if (masterUser.empty())
   {
      bool eof = false;
      masterUser = promptLine(in, out, "Master username: ", &eof);
      if (eof)
      {
         out << "[FAIL] No PostgreSQL master username provided. Pass --setup-db-master-username "
                "to run --setup-db without interactive input." << std::endl;
         *pPassed = false;
         return Success();
      }
   }

   std::string masterPassword;
   Error error = resolveMasterPassword(flags.masterPasswordFile, in, out, &masterPassword);
   if (error)
      return error;

   pMasterOptions->host = host;
   pMasterOptions->port = port;
   pMasterOptions->username = masterUser;
   pMasterOptions->password = masterPassword;
   pMasterOptions->database = "postgres";
   pMasterOptions->connectionTimeoutSeconds = 10;
   pMasterOptions->poolSize = 1;

   error = connect(*pMasterOptions, pConnection);
   if (error)
   {
      out << "[FAIL] Could not connect to " << host << ":" << port
          << " as " << masterUser << ": " << error.getSummary() << std::endl;
      *pPassed = false;
      return Success();
   }

   return Success();
}

Error createDatabaseAndUser(boost::shared_ptr<IConnection> pMasterConnection,
                             const PostgresqlConnectionOptions& masterConnectionOptions,
                             const std::string& dbName,
                             const std::string& dbUser,
                             const std::string& password,
                             std::ostream& out,
                             bool* pPassed,
                             bool* pUserCreated,
                             bool* pDatabaseCreated)
{
   *pPassed = true;
   *pUserCreated = false;
   *pDatabaseCreated = false;

   // Defense in depth: createDatabaseAndUser is public and shared with the
   // Pro overlay, which also interpolates dbName/dbUser directly into SQL
   // below, so re-check here rather than trusting that every caller already
   // validated via validateIdentifier().
   if (!isValidIdentifier(dbName))
   {
      out << "[FAIL] Invalid identifier \"" << dbName << "\"" << std::endl;
      *pPassed = false;
      return Success();
   }
   if (!isValidIdentifier(dbUser))
   {
      out << "[FAIL] Invalid identifier \"" << dbUser << "\"" << std::endl;
      *pPassed = false;
      return Success();
   }

   bool databaseExists = false;
   Error error = rowExists(pMasterConnection,
      "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'", &databaseExists);
   if (error)
   {
      out << "[FAIL] Could not query database catalog: " << error.getSummary() << std::endl;
      *pPassed = false;
      return Success();
   }

   if (databaseExists)
   {
      out << "[INFO] Database \"" << dbName << "\" already exists, skipping" << std::endl;
   }
   else
   {
      error = pMasterConnection->executeStr("CREATE DATABASE \"" + dbName + "\"");
      if (error)
      {
         out << "[FAIL] Could not create database \"" << dbName << "\": "
             << error.getSummary() << std::endl;
         *pPassed = false;
         return Success();
      }
      out << "[PASS] Created database \"" << dbName << "\"" << std::endl;
      *pDatabaseCreated = true;
   }

   bool userExists = false;
   error = rowExists(pMasterConnection,
      "SELECT 1 FROM pg_roles WHERE rolname = '" + dbUser + "'", &userExists);
   if (error)
   {
      out << "[FAIL] Could not query database catalog: " << error.getSummary() << std::endl;
      *pPassed = false;
      return Success();
   }

   if (userExists)
   {
      out << "[INFO] User \"" << dbUser << "\" already exists, skipping" << std::endl;
   }
   else
   {
      error = pMasterConnection->executeStr(
         "CREATE USER \"" + dbUser + "\" WITH PASSWORD '" + password + "'");
      if (error)
      {
         out << "[FAIL] Could not create user \"" << dbUser << "\": "
             << error.getSummary() << std::endl;
         *pPassed = false;
         return Success();
      }
      out << "[PASS] Created user \"" << dbUser << "\"" << std::endl;
      *pUserCreated = true;
   }

   error = pMasterConnection->executeStr(
      "GRANT ALL PRIVILEGES ON DATABASE \"" + dbName + "\" TO \"" + dbUser + "\"");
   if (error)
   {
      out << "[FAIL] Could not grant database privileges: " << error.getSummary() << std::endl;
      *pPassed = false;
      return Success();
   }
   out << "[PASS] Granted database privileges" << std::endl;

   // Reconnect to the newly created database to grant PostgreSQL 15+ schema access.
   PostgresqlConnectionOptions dbOptions = masterConnectionOptions;
   dbOptions.database = dbName;
   boost::shared_ptr<IConnection> pDbConnection;
   error = connect(dbOptions, &pDbConnection);
   if (error)
   {
      out << "[FAIL] Could not reconnect to \"" << dbName
          << "\" to grant schema privileges: " << error.getSummary() << std::endl;
      *pPassed = false;
      return Success();
   }
   error = pDbConnection->executeStr("GRANT ALL ON SCHEMA public TO \"" + dbUser + "\"");
   if (error)
   {
      out << "[FAIL] Could not grant schema privileges: " << error.getSummary() << std::endl;
      *pPassed = false;
      return Success();
   }
   out << "[PASS] Granted schema privileges (PostgreSQL 15+ compatible)" << std::endl;

   return Success();
}

Error setupDb(const Options& options,
              const SetupDbFlags& flags,
              boost::shared_ptr<IConnection> pMasterConnection,
              const PostgresqlConnectionOptions& masterConnectionOptions,
              std::istream& in,
              std::ostream& out,
              bool* pPassed)
{
   *pPassed = true;

   std::string defaultName = server_core::database::overlay::getDefaultDatabaseName();

   std::string dbName = flags.databaseName;
   if (dbName.empty())
   {
      bool eof = false;
      dbName = promptLine(in, out, "Database name [" + defaultName + "]: ", &eof);
      if (eof)
      {
         out << "[FAIL] No database name provided. Pass --setup-db-database-name to run "
                "--setup-db without interactive input." << std::endl;
         *pPassed = false;
         return Success();
      }
   }
   if (dbName.empty())
      dbName = defaultName;

   std::string dbUser = flags.databaseUser;
   if (dbUser.empty())
   {
      bool eof = false;
      dbUser = promptLine(in, out, "Database user [" + defaultName + "]: ", &eof);
      if (eof)
      {
         out << "[FAIL] No database user provided. Pass --setup-db-database-user to run "
                "--setup-db without interactive input." << std::endl;
         *pPassed = false;
         return Success();
      }
   }
   if (dbUser.empty())
      dbUser = defaultName;

   Error error = validateIdentifier(dbName, out, pPassed);
   if (error) return error;
   error = validateIdentifier(dbUser, out, pPassed);
   if (error) return error;
   if (!*pPassed)
      return Success();

   // Only generate a password when dbUser doesn't already exist -- an
   // idempotent re-run against an already-provisioned database must never
   // write a freshly generated password into database.conf, since that
   // password is never actually applied to the (pre-existing) role.
   bool userAlreadyExists = false;
   error = rowExists(pMasterConnection,
      "SELECT 1 FROM pg_roles WHERE rolname = '" + dbUser + "'", &userAlreadyExists);
   if (error)
   {
      out << "[FAIL] Could not query database catalog: " << error.getSummary() << std::endl;
      *pPassed = false;
      return Success();
   }
   std::string generatedPassword;
   if (!userAlreadyExists)
   {
      error = generateServiceUserPassword(&generatedPassword);
      if (error)
      {
         out << "[FAIL] Could not generate service user password: " << error.getSummary() << std::endl;
         *pPassed = false;
         return Success();
      }
   }

   bool userCreated = false;
   bool databaseCreated = false;
   error = createDatabaseAndUser(pMasterConnection, masterConnectionOptions,
                                  dbName, dbUser, generatedPassword, out, pPassed,
                                  &userCreated, &databaseCreated);
   if (error) return error;
   if (!*pPassed)
      return Success();

   // A config file is only written when the service user was created this run,
   // since that is the only case in which we hold a password that was actually
   // applied to the role. When the user already existed we leave config alone
   // -- but distinguish "nothing changed" from "we created a database for a
   // pre-existing user whose password we don't know", since the latter leaves
   // the admin with a new database and no usable connection settings and needs
   // actionable guidance rather than a misleading "already provisioned".
   if (!userCreated)
   {
      if (databaseCreated)
      {
         FilePath configPath = resolveDatabaseConfigFile(options.databaseConfigFile());
         out << "[INFO] Created database \"" << dbName << "\", but service user \"" << dbUser
             << "\" already exists and its password is unknown to this tool, so no connection "
                "settings were written." << std::endl;
         out << "       Set a password for \"" << dbUser << "\" and record it in "
             << configPath.getAbsolutePath()
             << " (for example: ALTER USER \"" << dbUser << "\" WITH PASSWORD '<password>';),"
                " or re-run against a database where the service user does not yet exist."
             << std::endl;
      }
      else
      {
         out << "[INFO] Database \"" << dbName << "\" and service user \"" << dbUser
             << "\" already provisioned; leaving existing connection settings untouched."
             << std::endl;
      }
      return Success();
   }

   if (flags.printOnly)
   {
      FilePath credentialsPath = resolveDatabaseConfigFile(options.databaseConfigFile())
         .getParent().completeChildPath("rserver-setup-db-credentials");
      error = writeCredentialsFileFresh(credentialsPath, masterConnectionOptions.host,
                                         masterConnectionOptions.port, dbName, dbUser,
                                         generatedPassword);
      if (error)
      {
         out << "[FAIL] Could not write credentials file: " << error.getSummary() << std::endl;
         *pPassed = false;
         return Success();
      }
      out << "[INFO] --print-only set: no config file was modified. Credentials written to "
          << credentialsPath.getAbsolutePath() << "." << std::endl;
      out << "       host=" << masterConnectionOptions.host << " port=" << masterConnectionOptions.port
          << " database=" << dbName << " username=" << dbUser << std::endl;
   }
   else
   {
      FilePath configPath = resolveDatabaseConfigFile(options.databaseConfigFile());
      error = mergeWriteDatabaseConfigFile(configPath, masterConnectionOptions.host,
                                            masterConnectionOptions.port, dbName, dbUser,
                                            generatedPassword);
      if (error)
      {
         out << "[FAIL] Could not write connection settings: " << error.getSummary() << std::endl;
         *pPassed = false;
         return Success();
      }
      out << "[PASS] Wrote connection settings to " << configPath.getAbsolutePath() << std::endl;
   }

   if (flags.showPassword)
   {
      out << "[INFO] Generated password for \"" << dbUser
          << "\" (sensitive, handle with care): " << generatedPassword << std::endl;
   }

   out << "[INFO] This configuration change takes effect the next time Workbench starts."
       << std::endl;
   out << "       Restart the service to apply it." << std::endl;

   return Success();
}

} // namespace server
} // namespace rstudio
