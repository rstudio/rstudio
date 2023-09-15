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
#include <server/auth/ServerAuthHandlerOverlay.hpp>

#include <core/DateTime.hpp>

using namespace rstudio::core;
using namespace rstudio::core::database;

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

OverlayResult addUser(boost::asio::io_service& ioService,
                      const std::string& username,
                      bool isAdmin)
{
   return std::make_tuple(Success(), false);
}

Error addUserToDatabase(const boost::shared_ptr<IConnection>& connection,
                        const system::User& user,
                        bool isAdmin)
{
   std::string currentTime = core::date_time::format(boost::posix_time::microsec_clock::universal_time(),
                             core::date_time::kIso8601Format);
   int locked = 0;
   Query insertQuery = connection->query("INSERT INTO licensed_users (user_name, user_id, locked, last_sign_in, is_admin) VALUES (:un, :ui, :lk, :ls, :ia)")
         .withInput(user.getUsername())
         .withInput(user.getUserId())
         .withInput(locked)
         .withInput(currentTime)
         .withInput(static_cast<int>(isAdmin));

   return connection->execute(insertQuery);
}

bool isUserProvisioningEnabled()
{
   return false;
}

std::string getUsernameDbColumnName()
{
   return "user_name";
} 

}// namespace overlay

} // namespace handler
} // namespace auth
} // namespace server
} // namespace rstudio
