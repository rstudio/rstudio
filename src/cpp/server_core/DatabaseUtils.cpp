/*
 * DatabaseUtils.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include <server_core/DatabaseUtils.hpp>
#include <server_core/DatabaseUtilsOverlay.hpp>

#include <server_core/ServerLicense.hpp>
#include <server_core/ServerKeyObfuscation.hpp>
#include <server_core/http/SecureCookie.hpp>

#include <shared_core/SafeConvert.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/thread.hpp>
#include <boost/regex.hpp>

#include <core/Database.hpp>
#include <core/FileSerializer.hpp>
#include <core/Log.hpp>
#include <core/RegexUtils.hpp>
#include <core/Settings.hpp>
#include <core/system/Environment.hpp>
#include <core/system/PosixSystem.hpp>
#include <core/system/Process.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>

namespace rstudio {
namespace server_core {
namespace database {
namespace utils {

using namespace core;
using namespace core::database;

namespace {

struct ConfiguredDriverVisitor : boost::static_visitor<Driver>
{
   Driver operator()(const SqliteConnectionOptions& options)
   {
      return Driver::Sqlite;
   }

   Driver operator()(const PostgresqlConnectionOptions& options)
   {
      return Driver::Postgresql;
   }

   Driver operator()(const ProviderNotSpecifiedConnectionOptions& options)
   {
      return Driver::Unknown;
   }
};

struct ConnectionPoolSizeVisitor : boost::static_visitor<int>
{
   int operator()(const SqliteConnectionOptions& options)
   {
      return options.poolSize;
   }

   int operator()(const PostgresqlConnectionOptions& options)
   {
      return options.poolSize;
   }   
   
   int operator()(const ProviderNotSpecifiedConnectionOptions& options)
   {
      return 0; // No pool size specified
   }
};

} // anonymous namespace

// Constructor for SqliteConnectionOptions
ConnectionOptionsVisitor::ConnectionOptionsVisitor(const boost::optional<core::system::User>& databaseFileUser)
   : databaseFileUser_(databaseFileUser),
      databaseConfigFile_(core::FilePath()),
      defaultDatabaseName_("")
{}

// Constructor for PostgresqlConnectionOptions
ConnectionOptionsVisitor::ConnectionOptionsVisitor(const core::FilePath& databaseConfigFile,
                                                   const std::string& defaultDatabaseName)
   : databaseFileUser_(boost::none),
      databaseConfigFile_(databaseConfigFile),
      defaultDatabaseName_(defaultDatabaseName)
{}
   
// Generic constructor
ConnectionOptionsVisitor::ConnectionOptionsVisitor(const boost::optional<core::system::User>& databaseFileUser,
                           const core::FilePath& databaseConfigFile,
                           const std::string& defaultDatabaseName)
   : databaseFileUser_(databaseFileUser),
      databaseConfigFile_(databaseConfigFile),
      defaultDatabaseName_(defaultDatabaseName)
{}

core::Error ConnectionOptionsVisitor::operator()(const core::database::SqliteConnectionOptions& options) const
{
   return processSqliteOptions(options, databaseFileUser_);
}

core::Error ConnectionOptionsVisitor::operator()(const core::database::PostgresqlConnectionOptions& options) const
{
   return processPostgresqlOptions(options, databaseConfigFile_, defaultDatabaseName_);
}

core::Error ConnectionOptionsVisitor::operator()(const core::database::ProviderNotSpecifiedConnectionOptions& options) const
{
   // Handle default case
   return core::Error(boost::system::errc::not_supported,
                     "No database provider specified",
                     ERROR_LOCATION);
}

void determineConnectionPoolSize(const ConnectionOptions& options, size_t& rOutPoolSize, std::string& rOutSource)
{
   ConnectionPoolSizeVisitor visitor;
   rOutPoolSize = boost::apply_visitor(visitor, options);

   if (rOutPoolSize == 0)
   {
      // If no size specified in config file, start with a connection pool with one connection per
      // logical CPU.
      rOutPoolSize = boost::thread::hardware_concurrency();
      rOutSource = "logical CPU count";

      if (rOutPoolSize == 0)
      {
         // Not able to determine number of CPUs; use the default pool minimum size.
         rOutPoolSize = kDefaultMinPoolSize;
         rOutSource = "default minimum";
      }

      if (rOutPoolSize > kDefaultMaxPoolSize)
      {
         // Some machines have a very large number of logical CPUs (128 or more). A pool that large can
         // exhaust the connection limit on the database, so cap the pool size to be gentler on the
         // database.
         rOutPoolSize = kDefaultMaxPoolSize;
         rOutSource = "default maximum with " + safe_convert::numberToString(rOutPoolSize) + " CPUs";
      }
   }
}

/**
 * @brief Validates that the PostgreSQL version is at least the minimum supported version.
 * Prints a warning if the version is not supported.
*/
void validateMinimumPostgreSqlVersion(boost::shared_ptr<IConnection> pConnection)
{
   if(!pConnection)
   {
      LOG_WARNING_MESSAGE("Failed to get connection from connection pool to determine PostgreSQL version.");
      return;
   }
   const std::string queryStatement = "SHOW server_version;";
   std::string versionStr;
   core::database::Query versionQuery = pConnection->query(queryStatement).withOutput(versionStr);
   const Error error = pConnection->execute(versionQuery);
   if (error)
   {
      LOG_WARNING_MESSAGE("Failed to run query \"" + queryStatement +
                          "\" to determine PostgreSQL version. error: " +
                          error.getMessage());
      return;
   }

   boost::smatch matches;
   /* PostgreSQL's versioning policy changed between 9.6 and 10, versions below
    * 10 will be in the format of X.X.X, whereas versions >= 10 will be in the
    * format of X.X, so make the last version number match optional */
   boost::regex versionRegex("([\\d]+)\\.([\\d]+)\\.?([\\d]+)?");
   if (regex_utils::search(
           versionStr, matches, versionRegex))
   {
      /* First match is the whole matching string, so the second match should be the major version number */
      const int versionMajor = safe_convert::stringTo(matches[1], 0);

      if (versionMajor < kMinimumSupportedPostgreSqlMajorVersion)
      {
         LOG_WARNING_MESSAGE("PostgreSQL version " + versionStr +
                             " is not supported. "
                             "Please upgrade to version " +
                             safe_convert::numberToString(
                                 kMinimumSupportedPostgreSqlMajorVersion) +
                             " or later.");
      } else {
         LOG_INFO_MESSAGE("Using PostgreSQL version " + versionStr);
      }
   }
   else {
      LOG_WARNING_MESSAGE("Failed to parse PostgreSQL version: " + versionStr);
   }
}

Error processSqliteOptions(const SqliteConnectionOptions& options,
                           const boost::optional<system::User>& databaseFileUser)
{
   Error error = options.databaseDirectory.ensureDirectory();
   if (error)
      return error;

   FilePath databaseFile = FilePath(options.file);
   if (!databaseFile.exists())
   {      // If the database file does not exist, we will create it if autoCreate is true
      if (options.autoCreate)
      {
         error = databaseFile.ensureFile();
         if (error)
            return error;
      }
      else
      {
         return Error(boost::system::errc::no_such_file_or_directory,
                      "SQLite database file does not exist: " + databaseFile.getAbsolutePath() + " and " + kAutoCreateDatabase + " is set to false.",
                      ERROR_LOCATION);
      }
   }

   // want to ensure users other than the server can't read the database
   error = databaseFile.changeFileMode(FileMode::USER_READ_WRITE);
   if (error)
      return error;

   if (databaseFileUser.has_value())
   {
      // always ensure the database file user is correct, if specified
      error = databaseFile.changeOwnership(databaseFileUser.get());
      if (error)
      {
         error.addProperty("server-user", databaseFileUser.get().getUsername());
         error.addProperty("description", "Unable to change ownership of database file");
         return error;
      }

#ifndef _WIN32
      error = options.databaseDirectory.changeOwnership(databaseFileUser.get());
      if (error)
      {
         bool writable = false;
         Error writableError = options.databaseDirectory.isWriteable(writable);
         if (writableError || !writable)
         {
               LOG_ERROR_MESSAGE("SQLite database directory: " + options.databaseDirectory.getAbsolutePath() + " must be writable by: " + databaseFileUser.get().getUsername());
         }
      }
#endif
   }

   return Success();
}

Error processPostgresqlOptions(const PostgresqlConnectionOptions& options,
                               const FilePath& databaseConfigFile,
                               const std::string& defaultDatabaseName)
{
   if (!options.connectionUri.empty() &&
      (options.database != defaultDatabaseName ||
      options.host != kDefaultDatabaseHost ||
      options.username != kDefaultPostgresqlDatabaseUsername ||
      options.port != kDefaultPostgresqlDatabasePort ||
      options.connectionTimeoutSeconds != kDefaultPostgresqlDatabaseConnectionTimeoutSeconds))
   {
      LOG_WARNING_MESSAGE("A " + std::string(kPostgresqlDatabaseConnectionUri) +
                              " was specified for Postgres database connection"
                              " in addition to other connection parameters. Only the " +
                              std::string(kPostgresqlDatabaseConnectionUri) +
                              " and password settings will be used.");
   }

   if (options.connectionUri.empty())
      LOG_INFO_MESSAGE("Connecting to Postgres database " + options.username + "@" + options.host + ":" + options.port + "/" + options.database);
   else
   {
      // matches up to the password, the password itself, and rest of it
      // replaces the password with a mask (***) leaving the rest untouched
      // no replacements if not a match (no password in the URI)
      boost::regex matchPassword(R"((.*:\/\/[^:]*:)([^@]*)(@.*$))");
      LOG_INFO_MESSAGE("Connecting to Postgres database: " + boost::regex_replace(options.connectionUri, matchPassword, "$1***$3"));
   }
   
   if (databaseConfigFile.exists())
   {
      // the database configuration file can potentially contain sensitive information
      // attempt to update the permissions and log a warning if permissions are too lax
      FileMode fileMode;
      Error error = databaseConfigFile.getFileMode(fileMode);
      if (error)
      {
         LOG_ERROR_MESSAGE("Could not determine file permissions for database configuration file: " +
                           databaseConfigFile.getAbsolutePath() + " - please ensure that the file has " +
                           "only user read/write permissions (600) if it contains sensitive information");
         LOG_ERROR(error);
      }
      else
      {
         if (fileMode != FileMode::USER_READ_WRITE_EXECUTE &&
             fileMode != FileMode::USER_READ_WRITE)
         {
            LOG_WARNING_MESSAGE("The database configuration file " + databaseConfigFile.getAbsolutePath() +
                                " has unrestrictive permissions. Posit Workbench will attempt to"
                                " change the file permissions to 600 to protect sensitive information");
            // ensure the config file has root ownership and correct file permissions
            error = databaseConfigFile.changeFileMode(FileMode::USER_READ_WRITE);
            if (error)
               LOG_WARNING_MESSAGE("Unable to change the file permissions for " +
                                   databaseConfigFile.getAbsolutePath() + " - please ensure that the file has " +
                                   "only user read/write permissions (600) if it contains sensitive information");
         }

         if (core::system::realUserIsRoot())
         {
            system::User rootUser;
            error = system::User::getUserFromIdentifier(UidType(0), rootUser);
            bool logOwnershipWarning = false;
            if (error)
               logOwnershipWarning = true;
   
            error = databaseConfigFile.changeOwnership(rootUser);
            if (error)
               logOwnershipWarning = true;
            
            if (logOwnershipWarning)
               LOG_WARNING_MESSAGE("Failed attempt to update ownership of database configuration file " +
                                   databaseConfigFile.getAbsolutePath() + " to root user: " + error.getMessage() +
                                   " - please ensure that the file owner is root if it contains" +
                                   " sensitive information");
         }
      }
   }

   return Success();
}

void applySqliteOptions(const Settings& settings,
                        const std::string& defaultDatabaseName,
                        SqliteConnectionOptions& options)
{
   // get the database directory - if not specified, we fallback to a hardcoded default path
   options.databaseDirectory = FilePath(settings.get(kSqliteDatabaseDirectory, kDefaultSqliteDatabaseDirectory));
   FilePath databaseFile = options.databaseDirectory.completeChildPath(defaultDatabaseName + ".sqlite");
   options.file = databaseFile.getAbsolutePath();
   options.poolSize = settings.getInt(kConnectionPoolSize, 0);
   options.autoCreate = settings.getBool(kAutoCreateDatabase, false);
}

Error applyPostgresqlOptions(const Settings& settings,
                            const std::string& defaultDatabaseName,
                            PostgresqlConnectionOptions& options)
{
   options.database = settings.get(kDatabaseName, defaultDatabaseName);
   options.host = settings.get(kDatabaseHost, kDefaultDatabaseHost);
   options.username = settings.get(kDatabaseUsername, kDefaultPostgresqlDatabaseUsername);
   options.password = settings.get(kDatabasePassword, std::string());
   options.port = settings.get(kDatabasePort, kDefaultPostgresqlDatabasePort);
   options.connectionTimeoutSeconds = settings.getInt(kPostgresqlDatabaseConnectionTimeoutSeconds,
                                                      kDefaultPostgresqlDatabaseConnectionTimeoutSeconds);
   options.connectionUri = settings.get(kPostgresqlDatabaseConnectionUri, std::string());
   std::string secureKey = core::http::secure_cookie::getKey();
   OBFUSCATE_KEY(secureKey);
   options.secureKey = secureKey;
   options.secureKeyFileUsed = core::http::secure_cookie::getKeyFileUsed();
   options.secureKeyHash = core::http::secure_cookie::getKeyHash();
   options.poolSize = settings.getInt(kConnectionPoolSize, 0);
   options.autoCreate = settings.getBool(kAutoCreateDatabase, false);

   Error error = utils::overlay::readPostgresqlOptions(options);
   if (error)
   {
      return error;
   }

   return Success();
} 

Error applyOptionsFromSettings(const Settings& databaseSettings,
                  ConnectionOptions* pOptions,
                  const std::string& defaultDatabaseName,
                  const std::string& defaultDatabaseProvider)
{
   Error result = Success();
   
   // If the defaultDatabaseProvider is specified, use it. This allows for a "forced" provider
   // to be used, which we use for internal database migration.
   std::string databaseProvider = defaultDatabaseProvider.empty()? databaseSettings.get(kDatabaseProvider, defaultDatabaseProvider) : defaultDatabaseProvider;

   if (boost::iequals(databaseProvider, kDatabaseProviderSqlite))
   {
      SqliteConnectionOptions options;

      applySqliteOptions(databaseSettings, defaultDatabaseName, options);

      LOG_INFO_MESSAGE("Finished reading configuration for sqlite database at " + options.file);
      *pOptions = options;
   }
   else if (boost::iequals(databaseProvider, kDatabaseProviderPostgresql))
   {
      PostgresqlConnectionOptions options;

      result = applyPostgresqlOptions(databaseSettings, defaultDatabaseName, options);
      if (result != Success())
      {
         return result;
      }

      LOG_INFO_MESSAGE("Finished reading configuration for Postgres database " + options.username + "@" + options.host + ":" + options.port + "/" + options.database);

      *pOptions = options;
   }
   else if (databaseProvider.empty())
   {
      ProviderNotSpecifiedConnectionOptions options;
      *pOptions = options;
   }
   else
   {
      return systemError(boost::system::errc::protocol_error,
                         "Invalid database provider specified: " + databaseProvider,
                         ERROR_LOCATION);
   }

   return result;
}

core::database::Driver getConfiguredDriver(const ConnectionOptions& options) 
{
   ConfiguredDriverVisitor visitor;
   return boost::apply_visitor(visitor, options);
}

Error migrationsDir(const std::string& rootDir, FilePath* pMigrationsDir)
{
   return migrationsDir(rootDir, "", pMigrationsDir);
}

Error migrationsDir(const std::string& rootDir, const std::string& migrationEnvVarName, core::FilePath* pMigrationsDir)
{
   FilePath exePath;
   Error error = core::system::executablePath(nullptr, &exePath);
   if (error)
      return error;

   // get the path for the migration files - this may be overridden via env var
   // for supporting development setups
   FilePath migrationsDir;
   if (!migrationEnvVarName.empty())
   {
      std::string migrationsPathEnv = core::system::getenv(migrationEnvVarName);
      if (!migrationsPathEnv.empty())
      {
         *pMigrationsDir = FilePath(migrationsPathEnv);
         return Success();
      }
   }
   
   *pMigrationsDir = exePath.getParent().getParent().completeChildPath(rootDir);

   return Success();
}

} // namespace utils
} // namespace database
} // namespace server_core
} // namespace rstudio
