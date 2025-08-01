/* 
 * DatabaseConstants.hpp
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

#ifndef SERVER_CORE_DATABASE_CONSTANTS_HPP
#define SERVER_CORE_DATABASE_CONSTANTS_HPP

namespace rstudio {
namespace server_core {
namespace database {

constexpr const char* kDatabaseProviderSqlite = "sqlite";
constexpr const char* kDatabaseProviderPostgresql = "postgresql";

constexpr const char* kDefaultWorkbenchDatabaseName = "rstudio";
constexpr const char* kDefaultOpenSourceDatabaseName = "rstudio-os";

// settings constants
constexpr const char* kDatabaseProvider = "provider";
constexpr const char* kSqliteDatabaseDirectory = "directory";
constexpr const char* kDefaultSqliteDatabaseDirectory = "/var/lib/rstudio-server";
constexpr const char* kDatabaseHost = "host";
constexpr const char* kDefaultDatabaseHost = "localhost";
constexpr const char* kDatabaseName = "database";
constexpr const char* kDatabasePort = "port";
constexpr const char* kDefaultPostgresqlDatabasePort = "5432";
constexpr const char* kDatabaseUsername = "username";
constexpr const char* kDefaultPostgresqlDatabaseUsername = "postgres";
constexpr const char* kDatabasePassword = "password";
constexpr const char* kPostgresqlDatabaseConnectionTimeoutSeconds = "connection-timeout-seconds";
constexpr const int   kDefaultPostgresqlDatabaseConnectionTimeoutSeconds = 10;
constexpr const char* kPostgresqlDatabaseConnectionUri = "connection-uri";
constexpr const char* kConnectionPoolSize = "pool-size";
constexpr const char* kAutoCreateDatabase = "auto-create";

// Choosing a modest pool size as the db usage of rserver is not high enough to
// justify anything larger and with 20 a cluster of 5 nodes hits the postgres default limit of 100.
constexpr const size_t kDefaultMinPoolSize = 4;
constexpr const size_t kDefaultMaxPoolSize = 6;
constexpr const int kMinimumSupportedPostgreSqlMajorVersion = 11;

} // namespace database
} // namespace server_core
} // namespace rstudio

#endif // SERVER_CORE_DATABASE_CONSTANTS_HPP