/*
 * ServerDatabase.hpp
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

#ifndef SERVER_CORE_SERVER_DATABASE_HPP
#define SERVER_CORE_SERVER_DATABASE_HPP

#include <boost/optional.hpp>

#include <core/Database.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/system/User.hpp>

namespace rstudio {
namespace server_core {
namespace database {

// initialize server database, optionally performing migration
// to the latest database schema version
core::Error initialize(const std::string& databaseConfigFile = std::string(),
                       bool updateSchema = false,
                       const boost::optional<core::system::User>& databaseFileUser = boost::none);

boost::shared_ptr<core::database::IConnection> getConnection();
bool getConnection(const boost::posix_time::time_duration& waitTime,
                   boost::shared_ptr<core::database::IConnection>* pConnection);

} // namespace database
} // namespace server_core
} // namespace rstudio


#endif // SERVER_CORE_SERVER_DATABASE_HPP
