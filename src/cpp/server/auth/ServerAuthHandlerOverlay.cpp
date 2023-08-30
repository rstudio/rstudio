/*
 * ServerAuthHandlerOverlay.cpp
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

#include <server/auth/ServerAuthHandler.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace auth {
namespace handler {
namespace overlay {

Error initialize()
{
   return Success();
}

bool canStaySignedIn()
{
   return true;
}
bool isUserListCookieValid(const std::string& cookieValue)
{
   return true;
}

bool shouldShowUserLicenseWarning()
{
   return false;
}

bool isUserAdmin(const std::string& username)
{
   return false;
}

bool isUserLocked(bool lockedColumn)
{
   return false;
}

std::string getUserListCookieValue()
{
   return "9c16856330a7400cbbbba228392a5d83";
}


unsigned int getActiveUserCount()
{
   return 0;
}

unsigned int getNamedUserLimit()
{
   return 0;
}

json::Array getLicensedUsers()
{
   return getAllUsers();
}

Error lockUser(boost::asio::io_service& ioService,
               const std::string& username)
{
   return Success();
}

Error unlockUser(boost::asio::io_service& ioService,
                 const std::string& username)
{
   return Success();
}

Error setAdmin(boost::asio::io_service& ioService,
               const std::string& username,
               bool isAdmin)
{
   return Success();
}

} // namespace overlay

} // namespace handler
} // namespace auth
} // namespace server
} // namespace rstudio