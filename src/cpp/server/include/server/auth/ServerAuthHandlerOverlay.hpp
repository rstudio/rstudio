/*
 * ServerAuthHandlerOverlay.hpp
 *
 * Copyright (C) 2023 by Posit Software, PBC
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

#ifndef SERVER_AUTH_HANDLER_OVERLAY_HPP
#define SERVER_AUTH_HANDLER_OVERLAY_HPP

#include <string>

#include <boost/function.hpp>

#include <core/Database.hpp>
#include <core/ExponentialBackoff.hpp>
#include <core/http/UriHandler.hpp>
#include <core/http/AsyncUriHandler.hpp>

#include <shared_core/json/Json.hpp>
#include <shared_core/system/User.hpp>

#include <server/auth/ServerSecureUriHandler.hpp>

namespace rstudio {
namespace server {
namespace auth {
namespace handler {

namespace overlay {

// Allows us to convey the result of an operation to the non-overlay code
// i.e. whether the operation was handled by the overlay or not
typedef std::tuple<core::Error, bool> OverlayResult;
typedef std::tuple<size_t, core::Error> ActiveUsersResult;

core::Error initialize();

bool canStaySignedIn();

bool isUserListCookieValid(const std::string& cookieValue);

bool shouldShowUserLicenseWarning();

bool isUserAdmin(const std::string& username);

bool isUserLocked(bool lockedColumn);

std::string getUserListCookieValue();

bool isUserActive(const boost::posix_time::ptime& lastSignin,
                  const std::string& username);

ActiveUsersResult getActiveUserCount(
    boost::shared_ptr<rstudio::core::database::IConnection> connection =
       nullptr);

unsigned int getNamedUserLimit();

core::json::Array getLicensedUsers();

core::Error lockUser(boost::asio::io_context& ioContext,
                     const std::string& username,
                     bool force = false);

core::Error unlockUser(boost::asio::io_context& ioContext,
                       const std::string& username,
                     bool force = false);

core::Error setAdmin(boost::asio::io_context& ioContext,
                     const std::string& username,
                     bool isAdmin);

OverlayResult addUser(boost::asio::io_context& ioContext,
                      const std::string& username,
                      bool isAdmin = false);

core::Error addUserToDatabase(const boost::shared_ptr<core::database::IConnection>& connection,
                              const core::system::User& user,
                              bool isAdmin = false);

core::Error checkForUninitializedUsername(const boost::shared_ptr<core::database::IConnection>& connection,
                                          core::database::Row& row,
                                          const core::system::User& user,
                                          std::string* pUsername);

bool isUserProvisioningEnabled();

std::string getUsernameDbColumnName();

core::database::Query addUsernameCheckToQuery(core::database::DatabaseConnection connection,
                                        const std::string& statement,
                                        const std::string& username);

} // namespace overlay

} // namespace handler
} // namespace auth
} // namespace server
} // namespace rstudio

#endif // SERVER_AUTH_HANDLER_OVERLAY_HPP
