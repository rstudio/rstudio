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

OverlayResult getAllUsersFromDatabase(const boost::shared_ptr<IConnection>& connection,
                                      core::database::Rowset& rows)
{
   Query query = connection->query("SELECT user_name, locked, last_sign_in, is_admin FROM licensed_users");
   Error error = connection->execute(query, rows);
   if (error)
      return std::make_tuple(error, true);
   return std::make_tuple(Success(), true);
}

OverlayResult getUserFromDatabase(const boost::shared_ptr<IConnection>& connection,
                                  const system::User& user,
                                  core::database::Rowset& rows)
{
   Query userQuery = connection->query("SELECT user_name, user_id, last_sign_in, locked FROM licensed_users WHERE user_id = :uid OR user_name = :username")
         .withInput(user.getUserId())
         .withInput(user.getUsername());
   Error error = connection->execute(userQuery, rows);
   if (error)
      return std::make_tuple(error, true);
   return std::make_tuple(Success(), true);
}

OverlayResult addUserToDatabase(const boost::shared_ptr<IConnection>& connection,
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

   Error error = connection->execute(insertQuery);

   if (error)
      return std::make_tuple(error, true);
   return std::make_tuple(Success(), true);
}

} // namespace overlay

} // namespace handler
} // namespace auth
} // namespace server
} // namespace rstudio