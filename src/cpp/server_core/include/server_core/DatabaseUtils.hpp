/*
 * DatabaseUtils.hpp
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

#ifndef SERVER_CORE_DATABASE_UTILS_HPP
#define SERVER_CORE_DATABASE_UTILS_HPP

#include <string_view>
#include <boost/optional.hpp>

#include <core/Database.hpp>

#include <server_core/DatabaseConstants.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/system/User.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
   class Settings;
}
namespace server_core {
namespace database {
namespace utils {

/**
 * Step 1 for reading database configuration files.
 * 
 * Applies the Settings object into the ConnectionOptions object with a Postgresql, Sqlite, or ProviderNotSpecified connection options visitor.
 */
core::Error applyOptionsFromSettings(const core::Settings& databaseSettings,
                  core::database::ConnectionOptions* pOptions,
                  const std::string& defaultDatabaseName,
                  const std::string& defaultDatabaseProvider);

/** Step 2 (sqlite option) for reading database configuration files.
 * 
 * Note: called from the ConnectionOptionsVisitor for SqliteConnectionOptions.
 * 
 * Performs any necessary processing on options like ensuring the SQLite database file exists and is writable.
 */
core::Error processSqliteOptions(const core::database::SqliteConnectionOptions& options,
                           const boost::optional<core::system::User>& databaseFileUser);

/** 
 * Step 2 (postgresql option) for reading database configuration files
 * 
 * Note: called from the ConnectionOptionsVisitor for PostgresqlConnectionOptions.
 * 
 * Performs any necessary processing on options like validating connection and authentication options.
 */                           
core::Error processPostgresqlOptions(const core::database::PostgresqlConnectionOptions& options,
                               const core::FilePath& databaseConfigFile,
                               const std::string& defaultDatabaseName);

/**
 * Use the provided ConnectionOptions object to determine the connection pool size. 
 * 
 * @param options The connection options to use.
 * @param rOutPoolSize The output variable to store the determined pool size.
 * @param rOutSource The output variable to store the source used to determine the pool size (for logging).
 */
void determineConnectionPoolSize(const core::database::ConnectionOptions& options, size_t& rOutPoolSize, std::string& rOutSource);
void validateMinimumPostgreSqlVersion(boost::shared_ptr<core::database::IConnection> pConnection);
core::database::Driver getConfiguredDriver(const core::database::ConnectionOptions& options);

struct ConnectionOptionsVisitor : boost::static_visitor<core::Error>
{
   // Constructor for SqliteConnectionOptions
   ConnectionOptionsVisitor(const boost::optional<core::system::User>& databaseFileUser);

   // Constructor for PostgresqlConnectionOptions
   ConnectionOptionsVisitor(const core::FilePath& databaseConfigFile,
                            const std::string& defaultDatabaseName);
      
   // Generic constructor
   ConnectionOptionsVisitor(const boost::optional<core::system::User>& databaseFileUser,
                            const core::FilePath& databaseConfigFile,
                            const std::string& defaultDatabaseName);

   core::Error operator()(const core::database::SqliteConnectionOptions& options) const;
   core::Error operator()(const core::database::PostgresqlConnectionOptions& options) const;
   core::Error operator()(const core::database::ProviderNotSpecifiedConnectionOptions& options) const;

   const boost::optional<core::system::User> databaseFileUser_;
   const core::FilePath& databaseConfigFile_;
   const std::string& defaultDatabaseName_;
};

} // namespace utils
} // namespace database
} // namespace server_core
} // namespace rstudio


#endif // SERVER_CORE_DATABASE_UTILS_HPP
