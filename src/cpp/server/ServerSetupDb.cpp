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
#include <random>
#include <regex>
#include <sstream>
#include <termios.h>
#include <unistd.h>

#include <boost/system/error_code.hpp>

#include <core/Database.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Xdg.hpp>
#include <server_core/DatabaseConstants.hpp>
#include <server_core/ServerDatabaseOverlay.hpp>
#include <shared_core/FilePath.hpp>

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
// disables echo when `in` is actually the controlling terminal (stdin);
// under test, `in` is a plain istringstream and this is a no-op.
std::string readMaskedLine(std::istream& in, std::ostream& out)
{
   bool isStdin = (&in == &std::cin);
   termios oldSettings{};
   if (isStdin)
   {
      tcgetattr(STDIN_FILENO, &oldSettings);
      termios noEcho = oldSettings;
      noEcho.c_lflag &= ~ECHO;
      tcsetattr(STDIN_FILENO, TCSANOW, &noEcho);
   }

   std::string value;
   std::getline(in, value);

   if (isStdin)
   {
      tcsetattr(STDIN_FILENO, TCSANOW, &oldSettings);
      out << std::endl;
   }
   return value;
}

std::string promptLine(std::istream& in, std::ostream& out, const std::string& prompt)
{
   out << prompt;
   std::string value;
   std::getline(in, value);
   return value;
}

bool rowExists(boost::shared_ptr<IConnection> pConnection, const std::string& sql)
{
   bool found = false;
   Error error = execAndProcessQuery(pConnection, sql,
      [&found](const Row&) { found = true; });
   return !error && found;
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

// Writes the given key/value pairs as a fresh, 0600 config file at `path`,
// overwriting anything already there. Used for both database.conf (default
// mode) and the standalone credentials file (--print-only mode); the two
// differ only in destination and messaging, not format.
Error writeConnectionSettingsFile(const FilePath& path,
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

   error = writeStringToFile(path, contents.str());
   if (error)
      return error;

   return path.changeFileMode(core::FileMode::USER_READ_WRITE);
}

} // anonymous namespace

Error validateIdentifier(const std::string& identifier, std::ostream& out, bool* pPassed)
{
   static const std::regex kValidIdentifier("^[A-Za-z_][A-Za-z0-9_-]*$");
   if (!std::regex_match(identifier, kValidIdentifier))
   {
      out << "[FAIL] \"" << identifier
          << "\" is not a valid database/user name (must match ^[A-Za-z_][A-Za-z0-9_-]*$)"
          << std::endl;
      *pPassed = false;
   }
   return Success();
}

std::string generateServiceUserPassword()
{
   std::random_device rd;
   std::mt19937_64 generator(rd());
   std::uniform_int_distribution<size_t> distribution(0, kPasswordCharset.size() - 1);

   std::string password;
   const size_t kLength = 32;
   password.reserve(kLength);
   for (size_t i = 0; i < kLength; ++i)
      password += kPasswordCharset[distribution(generator)];
   return password;
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
      host = promptLine(in, out, "PostgreSQL host: ");

   std::string port = flags.port;
   if (port.empty())
      port = promptLine(in, out, "PostgreSQL port [5432]: ");
   if (port.empty())
      port = "5432";

   std::string masterUser = flags.masterUsername;
   if (masterUser.empty())
      masterUser = promptLine(in, out, "Master username: ");

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
                             bool* pUserCreated)
{
   *pPassed = true;
   *pUserCreated = false;

   if (rowExists(pMasterConnection, "SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'"))
   {
      out << "[INFO] Database \"" << dbName << "\" already exists, skipping" << std::endl;
   }
   else
   {
      Error error = pMasterConnection->executeStr("CREATE DATABASE \"" + dbName + "\"");
      if (error)
      {
         out << "[FAIL] Could not create database \"" << dbName << "\": "
             << error.getSummary() << std::endl;
         *pPassed = false;
         return Success();
      }
      out << "[PASS] Created database \"" << dbName << "\"" << std::endl;
   }

   if (rowExists(pMasterConnection, "SELECT 1 FROM pg_roles WHERE rolname = '" + dbUser + "'"))
   {
      out << "[INFO] User \"" << dbUser << "\" already exists, skipping" << std::endl;
   }
   else
   {
      Error error = pMasterConnection->executeStr(
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

   Error error = pMasterConnection->executeStr(
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
      dbName = promptLine(in, out, "Database name [" + defaultName + "]: ");
   if (dbName.empty())
      dbName = defaultName;

   std::string dbUser = flags.databaseUser;
   if (dbUser.empty())
      dbUser = promptLine(in, out, "Database user [" + defaultName + "]: ");
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
   bool userAlreadyExists = rowExists(pMasterConnection,
      "SELECT 1 FROM pg_roles WHERE rolname = '" + dbUser + "'");
   std::string generatedPassword = userAlreadyExists ? std::string() : generateServiceUserPassword();

   bool userCreated = false;
   error = createDatabaseAndUser(pMasterConnection, masterConnectionOptions,
                                  dbName, dbUser, generatedPassword, out, pPassed, &userCreated);
   if (error) return error;
   if (!*pPassed)
      return Success();

   if (!userCreated)
   {
      out << "[INFO] Service user \"" << dbUser
          << "\" already provisioned; leaving existing connection settings untouched." << std::endl;
      return Success();
   }

   if (flags.printOnly)
   {
      FilePath credentialsPath = FilePath("./rserver-setup-db-credentials");
      error = writeConnectionSettingsFile(credentialsPath, masterConnectionOptions.host,
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
      error = writeConnectionSettingsFile(configPath, masterConnectionOptions.host,
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
