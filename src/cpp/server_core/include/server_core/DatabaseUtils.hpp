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

#include <boost/optional.hpp>

#include <core/Database.hpp>

#include <server_core/DatabaseConstants.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/system/User.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace server_core {
namespace database {
namespace utils {

core::Error readOptions(const core::FilePath& databaseConfigFile,
                        const boost::optional<core::system::User>& databaseFileUser,
                        core::database::ConnectionOptions* pOptions,
                        const std::string forceDatabaseProvider);
                        
void determineConnectionPoolSize(core::database::ConnectionOptions& options, size_t& poolSize, std::string& source);
void validateMinimumPostgreSqlVersion(boost::shared_ptr<core::database::IConnection> pConnection);
core::database::Driver getConfiguredDriver(core::database::ConnectionOptions options);
core::database::Driver getConfiguredDriver(const core::FilePath& databaseConfigFile);

} // namespace utils
} // namespace database
} // namespace server_core
} // namespace rstudio


#endif // SERVER_CORE_DATABASE_UTILS_HPP
