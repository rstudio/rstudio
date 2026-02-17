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
#include <server_core/ServerDatabaseOverlay.hpp>

#include <server_core/DatabaseConstants.hpp>
#include <server_core/DatabaseUtils.hpp>
#include <server_core/ServerLicense.hpp>
#include <server_core/ServerKeyObfuscation.hpp>
#include <server_core/http/SecureCookie.hpp>

#include <shared_core/SafeConvert.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/thread.hpp>
#include <boost/regex.hpp>

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

using namespace core;
using namespace core::database;

namespace {

// settings constants
constexpr const char* kConfigFile = "database.conf";

// environment variables
constexpr const char* kDatabaseMigrationsPathEnvVar = "RS_DB_MIGRATIONS_PATH";
constexpr const char* kDatabaseMigrationsRootDir = "db";

boost::shared_ptr<ConnectionPool> s_connectionPool;
boost::optional<ConnectionOptions> s_connectionOptions = boost::none;

// Converts the database configuration file path to a FilePath object.
FilePath getDatabaseFilePath(const std::string& databaseConfigFile)
{
   // read the options from the specified configuration file
   // if not specified, fall back to system configuration
   FilePath optionsFile = !databaseConfigFile.empty() ?
            FilePath(databaseConfigFile) :
            core::system::xdg::findSystemConfigFile("database configuration", kConfigFile);
   
   return optionsFile;
}

void setAutoCreateDatabaseOption(Settings& settings, const std::string& databaseProvider, const std::string& optionsFilePath)
{
   if (boost::iequals(databaseProvider, kDatabaseProviderSqlite))
   {
      // We create the internal database by default for SQLite
      if (!settings.contains(kAutoCreateDatabase))
      {
         settings.set(kAutoCreateDatabase, true);
      }
   }
   else if (boost::iequals(databaseProvider, kDatabaseProviderPostgresql))
   {
      // We never create the internal database automatically for PostgreSQL
      if (settings.getBool(kAutoCreateDatabase, false))
      {
         LOG_INFO_MESSAGE("The internal database cannot be created automatically for PostgreSQL. \"" + std::string(kAutoCreateDatabase) + "\" in " + optionsFilePath + " will be ignored. To remove this warning, remove this setting and verify the database already exists in the PostgreSQL server.");
      }
      settings.set(kAutoCreateDatabase, false);
   }
}

} // anonymous namespace

core::database::Driver getConfiguredDriver(const ConnectionOptions& options) {
   return utils::getConfiguredDriver(options);
}

core::database::Driver getConfiguredDriver(const std::string& databaseConfigFile)
{
   FilePath optionsFile = getDatabaseFilePath(databaseConfigFile);
   std::string defaultDatabaseName = overlay::getDefaultDatabaseName();

   ConnectionOptions options;

   // Read options from the config file into a Settings object
   Settings settings;
   Error error = settings.initialize(optionsFile);
   if (error)
   {
      LOG_ERROR(error);
      return core::database::Driver::Unknown;
   }

   // If the database provider is not specified, use the default provider
   std::string databaseProvider = settings.get(kDatabaseProvider, kDatabaseProviderSqlite);

   setAutoCreateDatabaseOption(settings, databaseProvider, optionsFile.getAbsolutePath());

   // Translate the Settings object to a database::ConnectionOptions object
   error = utils::applyOptionsFromSettings(settings, &options, defaultDatabaseName, databaseProvider);
   if (error)
   {
      LOG_ERROR(error);
      return core::database::Driver::Unknown;
   }

   return utils::getConfiguredDriver(options);
}

boost::optional<core::database::ConnectionOptions> getConnectionOptions()
{
   return s_connectionOptions;
}

Error processOptions(const std::string& defaultDatabaseName,
                  const boost::optional<system::User>& databaseFileUser,
                  const FilePath& databaseConfigFile,
                  ConnectionOptions* pOptions)
{
   return boost::apply_visitor(utils::ConnectionOptionsVisitor(databaseFileUser, databaseConfigFile, defaultDatabaseName), *pOptions);
}

Error readOptions(const std::string& databaseConfigFile,
                  const boost::optional<system::User>& databaseFileUser,
                  ConnectionOptions* pOptions,
                  std::string_view forceDatabaseProvider)
{
   FilePath optionsFile = getDatabaseFilePath(databaseConfigFile);
   std::string defaultDatabaseName = overlay::getDefaultDatabaseName();

   // Read options from the config file into a Settings object
   Settings settings;
   Error error = settings.initialize(optionsFile);
   if (error)
   {
      LOG_ERROR_MESSAGE("Failed to read database options from " + optionsFile.getAbsolutePath() + ": " + error.getMessage());
      return error;
   }

   // Internal database defaults to SQLite if no provider is specified
   std::string databaseProvider = settings.get(kDatabaseProvider, kDatabaseProviderSqlite);

   // Database migration uses a forced provider to get settings
   if (!forceDatabaseProvider.empty())
      databaseProvider = forceDatabaseProvider;
   
   setAutoCreateDatabaseOption(settings, databaseProvider, optionsFile.getAbsolutePath());

   // Translate the Settings object to a database::ConnectionOptions object
   error = utils::applyOptionsFromSettings(settings, pOptions, defaultDatabaseName, databaseProvider);
   if (error)
   {
      LOG_ERROR_MESSAGE("Failed to apply database options from " + optionsFile.getAbsolutePath() + ": " + error.getMessage());
      return error;
   }

   LOG_DEBUG_MESSAGE("Applied database configuration from " + optionsFile.getAbsolutePath() +
                     " with provider: " + databaseProvider);

   if (getConfiguredDriver(*pOptions) == Driver::Unknown)
   {
      LOG_ERROR_MESSAGE("No database provider specified in " + optionsFile.getAbsolutePath() +
                        ". Please specify a provider using the 'provider' option.");
      return systemError(boost::system::errc::protocol_error,
                         "No database provider specified in " + optionsFile.getAbsolutePath() +
                         ". Please specify a provider using the 'provider' option.",
                         ERROR_LOCATION);
   }

   // Process and validate the options based on the configured driver
   return processOptions(defaultDatabaseName, databaseFileUser, optionsFile, pOptions);
}

Error initialize(const std::string& databaseConfigFile,
                 bool updateSchema,
                 const boost::optional<system::User>& databaseFileUser)
{
   ConnectionOptions options;
   Error error = database::readOptions(databaseConfigFile, databaseFileUser, &options);
   if (error)
      return error;

   // Named parameters that are treated as secrets with trace-db-enabled
   core::database::addSecretParamNames({"refresh_token_key", "access_token", "refresh_token", "token_key"});

   // Attempt to read pool size from configuration file
   size_t poolSize = 0;
   std::string source = databaseConfigFile.empty() ? kConfigFile : databaseConfigFile;
   utils::determineConnectionPoolSize(options, poolSize, source);

   LOG_INFO_MESSAGE("Creating database connection pool of size " +
         safe_convert::numberToString(poolSize) + " (source: " + source + ")");

   error = createConnectionPool(poolSize, options, &s_connectionPool);
   if (error)
      return error;
   
   if (utils::getConfiguredDriver(options) == Driver::Postgresql)
   {
      utils::validateMinimumPostgreSqlVersion(s_connectionPool->getConnection());
   }

   if (updateSchema)
   {
      boost::shared_ptr<IConnection> connection = s_connectionPool->getConnection();

      FilePath migrationsDirectory;
      error = utils::migrationsDir(kDatabaseMigrationsRootDir, kDatabaseMigrationsPathEnvVar, &migrationsDirectory);
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

   s_connectionOptions = options;

   return Success();
}

// Execute the database command using the underlying configuration
core::Error execute(const std::string& databaseConfigFile,
                    const boost::optional<core::system::User>& databaseFileUser,
                    std::string_view command)
{
   return overlay::execute(databaseConfigFile, databaseFileUser, command);
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
