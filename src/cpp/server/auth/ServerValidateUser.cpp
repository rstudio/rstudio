/*
 * ServerValidateUser.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <server/auth/ServerValidateUser.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>

#include <core/system/PosixSystem.hpp>
#include <core/system/PosixUser.hpp>

#include <server/ServerOptions.hpp>

using namespace core;

namespace server {
namespace auth {

bool validateUser(const std::string& username, const std::string& requiredGroup)
{
   // short circuit if we aren't validating users
   if (!server::options().authValidateUsers())
      return true;
   
   // get the user
   core::system::user::User user;
   Error error = userFromUsername(username, &user);
   if (error)
   {
      // log the error only if it is unexpected
      if (!core::system::isUserNotFoundError(error))
         LOG_ERROR(error);

      // not found either due to non-existence or an unexpected error
      return false;
   }

   // validate user if necessary
   if (!requiredGroup.empty())
   {    
      // see if they are a member of the required group
      bool belongsToGroup ;
      error = core::system::userBelongsToGroup(user,
                                               requiredGroup,
                                               &belongsToGroup);
      if (error)
      {
         // log and return false
         LOG_ERROR(error);
         return false;
      }
      else
      {
         // return belongs status
         return belongsToGroup;
      }
   }
   else
   {
      // not validating (running in some type of dev mode where we
      // don't have a system account for every login)
      return true;
   }
}

} // namespace auth
} // namespace server



