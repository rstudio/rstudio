/*
 * ServerAuthHandler.hpp
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

#include <shared_core/json/Json.hpp>
#include <shared_core/Error.hpp>

namespace rstudio {
namespace server {
namespace auth {
namespace handler {
   
namespace overlay {

core::Error initialize();
bool canStaySignedIn();
bool isUserListCookieValid(const std::string& cookieValue);
bool shouldShowUserLicenseWarning();
bool isUserAdmin(const std::string& username);
bool isUserLocked(bool lockedColumn);
std::string getUserListCookieValue();
unsigned int getNamedUserLimit();
core::json::Array getLicensedUsers();
core::Error lockUser(boost::asio::io_service& ioService, const std::string& username);
core::Error unlockUser(boost::asio::io_service& ioService, const std::string& username);
core::Error setAdmin(boost::asio::io_service& ioService, const std::string& username, bool isAdmin);

} // namespace overlay

} // namespace handler
} // namespace auth
} // namespace server
} // namespace rstudio

#endif // SERVER_AUTH_HANDLER_OVERLAY_HPP