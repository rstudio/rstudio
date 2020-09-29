/*
 * ServerDatabase.cpp
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

#include <server_core/ServerDatabase.hpp>
#include <server_core/ServerKeyObfuscation.hpp>
#include <server_core/http/SecureCookie.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/thread.hpp>

#include <core/Log.hpp>
#include <core/Settings.hpp>
#include <core/system/Environment.hpp>
#include <core/system/System.hpp>
#include <core/system/Xdg.hpp>

namespace rstudio {
namespace server_core {
namespace database {

using namespace core;
using namespace core::database;

namespace {

// settings constants
constexpr const char* kDatabaseProvider = "provider";
constexpr const char* kDatabaseProviderSqlite = "sqlite";
constexpr const char* kDatabaseProviderPostgresql = "postgresql";
constexpr const char* kSqliteDatabaseDirectory = "directory";
constexpr const char* kDefaultSqliteDatabaseDirectory = "/var/lib/rstudio-server";
constexpr const char* kDatabaseHost = "host";
constexpr const char* kDefaultDatabaseHost = "localhost";
constexpr const char* kDatabaseName = "database";
constexpr const char* kDefaultDatabaseName = "rstudio";
constexpr const char* kDatabasePort = "port";
constexpr const char* kDefaultPostgresqlDatabasePort = "5432";
constexpr const char* kDatabaseUsername = "username";
constexpr const char* kDefaultPostgresqlDatabaseUsername = "postgres";
constexpr const char* kDatabasePassword = "password";
constexpr const char* kPostgresqlDatabaseConnectionTimeoutSeconds = "connnection-timeout-seconds";
constexpr const int   kDefaultPostgresqlDatabaseConnectionTimeoutSeconds = 10;
constexpr const char* kPostgresqlDatabaseConnectionUri = "connection-uri";

// environment variables
constexpr const char* kDatabaseMigrationsPathEnvVar = "RS_DB_MIGRATIONS_PATH";

//misc constants
constexpr const size_t kDefaultConnectionPoolSize = 4;

boost::shared_ptr<ConnectionPool> s_connectionPool;

Error readOptions(const std::string& databaseConfigFile,
                  const boost::optional<system::User>& databaseFileUser,
                  ConnectionOptions* pOptions)
{
   // read the options from the specified configuration file
   // if not specified, fall back to system configuration
   FilePath optionsFile = !databaseConfigFile.empty() ?
            FilePath(databaseConfigFile) :
            core::system::xdg::systemConfigFile("database.conf");
   
   Settings settings;
   Error error = settings.initialize(optionsFile);
   if (error)
      return error;

   std::string databaseProvider = settings.get(kDatabaseProvider, kDatabaseProviderSqlite);
   bool checkConfFilePermissions = false;

   if (boost::iequals(databaseProvider, kDatabaseProviderSqlite))
   {
      SqliteConnectionOptions options;

      // get the database directory - if not specified, we fallback to a hardcoded default path
      FilePath databaseDirectory = FilePath(settings.get(kSqliteDatabaseDirectory, kDefaultSqliteDatabaseDirectory));
      FilePath databaseFile = databaseDirectory.completeChildPath("rstudio.sqlite");
      options.file = databaseFile.getAbsolutePath();

      error = databaseDirectory.ensureDirectory();
      if (error)
         return error;

      error = databaseFile.ensureFile();
      if (error)
         return error;

      if (databaseFileUser.has_value())
      {
         // always ensure the database file user is correct, if specified
         error = databaseFile.changeOwnership(databaseFileUser.get());
         if (error)
            return error;
      }

      LOG_INFO_MESSAGE("Connecting to sqlite3 database at " + options.file);
      *pOptions = options;
   }
   else if (boost::iequals(databaseProvider, kDatabaseProviderPostgresql))
   {
      PostgresqlConnectionOptions options;
      options.database = settings.get(kDatabaseName, kDefaultDatabaseName);
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
      *pOptions = options;

      if (!options.connectionUri.empty() &&
          (options.database != kDefaultDatabaseName ||
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
         LOG_INFO_MESSAGE("Connecting to Postgres database: " + options.connectionUri);

      checkConfFilePermissions = true;
   }
   else
   {
      return systemError(boost::system::errc::protocol_error,
                         "Invalid database provider specified in " + optionsFile.getAbsolutePath() +
                            ": " + databaseProvider,
                         ERROR_LOCATION);
   }

   if (optionsFile.exists() && checkConfFilePermissions)
   {
      // the database configuration file can potentially contain sensitive information
      // log a warning if permissions are too lax
      FileMode fileMode;
      Error error = optionsFile.getFileMode(fileMode);
      if (error)
      {
         LOG_ERROR_MESSAGE("Could not determine file permissions for database configuration file: " +
                           optionsFile.getAbsolutePath() + " - please ensure that the file has " +
                           "only user read/write permissions (600) if it contains sensitive information");
         LOG_ERROR(error);
      }
      else
      {
         if (fileMode != FileMode::USER_READ_WRITE_EXECUTE &&
             fileMode != FileMode::USER_READ_WRITE)
         {
            LOG_WARNING_MESSAGE("The database configuration file " + optionsFile.getAbsolutePath() +
                                " has unrestrictive permissions. Please ensure that the file has"
                                " only user read/write permissions (600) if it contains sensitive information");
         }
      }
   }

   return Success();
}

Error migrationsDir(FilePath* pMigrationsDir)
{
   FilePath exePath;
   Error error = core::system::executablePath(nullptr, &exePath);
   if (error)
      return error;

   // get the path for the migration files - this may be overridden via env var
   // for supporting development setups
   FilePath migrationsDir;
   std::string migrationsPathEnv = core::system::getenv(kDatabaseMigrationsPathEnvVar);
   if (!migrationsPathEnv.empty())
      *pMigrationsDir = FilePath(migrationsPathEnv);
   else
      *pMigrationsDir = exePath.getParent().getParent().completeChildPath("db");

   return Success();
}

} // anonymous namespace

Error initialize(const std::string& databaseConfigFile,
                 bool updateSchema,
                 const boost::optional<system::User>& databaseFileUser)
{
   ConnectionOptions options;
   Error error = readOptions(databaseConfigFile, databaseFileUser, &options);
   if (error)
      return error;

   size_t poolSize = boost::thread::hardware_concurrency();
   if (poolSize == 0)
      poolSize = kDefaultConnectionPoolSize;

   error = createConnectionPool(poolSize, options, &s_connectionPool);
   if (error)
      return error;

   if (updateSchema)
   {
      boost::shared_ptr<IConnection> connection = s_connectionPool->getConnection();

      FilePath migrationsDirectory;
      error = migrationsDir(&migrationsDirectory);
      if (error)
      {
         LOG_ERROR_MESSAGE("Could not determine path to database migration files");
         return error;
      }

      SchemaUpdater updater(connection, migrationsDirectory);
      error = updater.update();
      if (error)
      {
         LOG_ERROR_MESSAGE("Could not update database to latest schema");
         return error;
      }
   }

   return Success();
}

boost::shared_ptr<IConnection> getConnection()
{
   return s_connectionPool->getConnection();
}

bool getConnection(const boost::posix_time::time_duration& waitTime,
                   boost::shared_ptr<IConnection>* pConnection)
{
   return s_connectionPool->getConnection(waitTime, pConnection);
}

} // namespace database
} // namespace server_core
} // namespace rstudio
