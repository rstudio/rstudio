/*
 * ServerDatabase.cpp
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

#include <server_core/ServerDatabase.hpp>
#include <server_core/ServerLicense.hpp>
#include <server_core/ServerKeyObfuscation.hpp>
#include <server_core/http/SecureCookie.hpp>

#include <shared_core/SafeConvert.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/thread.hpp>
#include <boost/regex.hpp>

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
constexpr const char* kConfigFile = "database.conf";
constexpr const char* kDatabaseProvider = "provider";
constexpr const char* kDatabaseProviderSqlite = "sqlite";
constexpr const char* kDatabaseProviderPostgresql = "postgresql";
constexpr const char* kSqliteDatabaseDirectory = "directory";
constexpr const char* kDefaultSqliteDatabaseDirectory = "/var/lib/rstudio-server";
constexpr const char* kDatabaseHost = "host";
constexpr const char* kDefaultDatabaseHost = "localhost";
constexpr const char* kDatabaseName = "database";
constexpr const char* kDefaultWorkbenchDatabaseName = "rstudio";
constexpr const char* kDefaultOpenSourceDatabaseName = "rstudio-os";
constexpr const char* kDatabasePort = "port";
constexpr const char* kDefaultPostgresqlDatabasePort = "5432";
constexpr const char* kDatabaseUsername = "username";
constexpr const char* kDefaultPostgresqlDatabaseUsername = "postgres";
constexpr const char* kDatabasePassword = "password";
constexpr const char* kPostgresqlDatabaseConnectionTimeoutSeconds = "connnection-timeout-seconds";
constexpr const int   kDefaultPostgresqlDatabaseConnectionTimeoutSeconds = 10;
constexpr const char* kPostgresqlDatabaseConnectionUri = "connection-uri";
constexpr const char* kConnectionPoolSize = "pool-size";

// environment variables
constexpr const char* kDatabaseMigrationsPathEnvVar = "RS_DB_MIGRATIONS_PATH";

// misc constants
constexpr const size_t kDefaultMinPoolSize = 4;
constexpr const size_t kDefaultMaxPoolSize = 20;

boost::shared_ptr<ConnectionPool> s_connectionPool;

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
};

Error readOptions(const std::string& databaseConfigFile,
                  const boost::optional<system::User>& databaseFileUser,
                  ConnectionOptions* pOptions,
                  const std::string forceDatabaseProvider = "")
{
   // read the options from the specified configuration file
   // if not specified, fall back to system configuration
   FilePath optionsFile = !databaseConfigFile.empty() ?
            FilePath(databaseConfigFile) :
            core::system::xdg::findSystemConfigFile("database configuration", kConfigFile);
   
   Settings settings;
   Error error = settings.initialize(optionsFile);
   if (error)
      return error;

   std::string databaseProvider = settings.get(kDatabaseProvider, kDatabaseProviderSqlite);
   if (!forceDatabaseProvider.empty())
      databaseProvider = forceDatabaseProvider;

   bool checkConfFilePermissions = false;

   std::string defaultDatabaseName;
   if (license::isProfessionalEdition()) 
   {
      defaultDatabaseName = kDefaultWorkbenchDatabaseName;
   }
   else 
   {
      defaultDatabaseName = kDefaultOpenSourceDatabaseName;
   }

   if (boost::iequals(databaseProvider, kDatabaseProviderSqlite))
   {
      SqliteConnectionOptions options;

      // get the database directory - if not specified, we fallback to a hardcoded default path
      FilePath databaseDirectory = FilePath(settings.get(kSqliteDatabaseDirectory, kDefaultSqliteDatabaseDirectory));
      FilePath databaseFile = databaseDirectory.completeChildPath(defaultDatabaseName + ".sqlite");
      options.file = databaseFile.getAbsolutePath();
      options.poolSize = settings.getInt(kConnectionPoolSize, 0);

      error = databaseDirectory.ensureDirectory();
      if (error)
         return error;

      error = databaseFile.ensureFile();
      if (error)
         return error;

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
         error = databaseDirectory.changeOwnership(databaseFileUser.get());
         if (error)
         {
            bool writable = false;
            Error writableError = databaseDirectory.isWriteable(writable);
            if (writableError || !writable)
            {
                LOG_ERROR_MESSAGE("Sqllite database directory: " + databaseDirectory.getAbsolutePath() + " must be writable by: " + databaseFileUser.get().getUsername());
            }
         }
#endif
      }

      LOG_INFO_MESSAGE("Connecting to sqlite3 database at " + options.file);
      *pOptions = options;
   }
   else if (boost::iequals(databaseProvider, kDatabaseProviderPostgresql))
   {
      PostgresqlConnectionOptions options;

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
      *pOptions = options;

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

core::database::Driver getConfiguredDriver(const std::string& databaseConfigFile)
{
   ConnectionOptions options;
   Error error = readOptions(databaseConfigFile, boost::optional<system::User>(), &options);
   if (error)
   {
      LOG_ERROR(error);
      return core::database::Driver::Unknown;
   }

   ConfiguredDriverVisitor visitor;
   return boost::apply_visitor(visitor, options);
}

Error initialize(const std::string& databaseConfigFile,
                 bool updateSchema,
                 const boost::optional<system::User>& databaseFileUser)
{
   ConnectionOptions options;
   Error error = readOptions(databaseConfigFile, databaseFileUser, &options);
   if (error)
      return error;

   // Attempt to read pool size from configuration file
   ConnectionPoolSizeVisitor visitor;
   size_t poolSize = boost::apply_visitor(visitor, options);
   std::string source = databaseConfigFile.empty() ? kConfigFile : databaseConfigFile;

   if (poolSize == 0)
   {
      // If no size specified in config file, start with a connection pool with one connection per
      // logical CPU.
      poolSize = boost::thread::hardware_concurrency();
      source = "logical CPU count";

      if (poolSize == 0)
      {
         // Not able to determine number of CPUs; use the default pool minimum size.
         poolSize = kDefaultMinPoolSize;
         source = "default minimum";
      }

      if (poolSize > kDefaultMaxPoolSize)
      {
         // Some machines have a very large number of logical CPUs (128 or more). A pool that large can
         // exhaust the connection limit on the database, so cap the pool size to be gentler on the
         // database.
         poolSize = kDefaultMaxPoolSize;
         source = "default maximum with " + safe_convert::numberToString(poolSize) + " CPUs";
      }
   }

   LOG_INFO_MESSAGE("Creating database connection pool of size " +
         safe_convert::numberToString(poolSize) + " (source: " + source + ")");

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

Error execute(const std::string& databaseConfigFile,
              const boost::optional<system::User>& databaseFileUser,
              std::string command)
{
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
