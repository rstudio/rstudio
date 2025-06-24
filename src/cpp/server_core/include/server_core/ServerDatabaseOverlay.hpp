/*
 * ServerDatabaseOverlay.hpp
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

#ifndef SERVER_CORE_SERVER_DATABASE_OVERLAY_HPP
#define SERVER_CORE_SERVER_DATABASE_OVERLAY_HPP

#include <boost/optional.hpp>

#include <core/Database.hpp>

#include <server_core/DatabaseConstants.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/system/User.hpp>

namespace rstudio {
namespace server_core {
namespace database {
namespace overlay {

// Execute the database command using the underlying configuration
core::Error execute(const std::string& databaseConfigFile,
                    const boost::optional<core::system::User>& databaseFileUser,
                    std::string_view command);

} // namespace overlay
} // namespace database
} // namespace server_core
} // namespace rstudio

#endif // SERVER_CORE_SERVER_DATABASE_OVERLAY_HPP
