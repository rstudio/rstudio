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

#include <boost/foreach.hpp>
#include <boost/tokenizer.hpp>
#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>

#include <core/system/PosixSystem.hpp>
#include <core/system/PosixUser.hpp>

#include <server/ServerOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace auth {

bool validateUser(const std::string& username,
                  const std::string& requiredGroup,
                  unsigned int minimumUserId,
                  bool failureWarning)
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

   // validate minimum user id
   if (user.userId < minimumUserId)
   {
      if (failureWarning)
      {
         boost::format fmt(
            "User %1% could not be authenticated because they "
            "did not meet the minimum required user id (%2%). "
            "The minimum user id is controlled by the "
            "auth-minimum-user-id rserver.conf option.");
         std::string msg = boost::str(fmt % username % minimumUserId);
         LOG_WARNING_MESSAGE(msg);
      }

      return false;
   }

   // validate group if necessary
   if (!requiredGroup.empty())
   {    
      // see if they are a member of one of the required groups
      bool belongsToGroup = false;
      using namespace boost ;
      char_separator<char> comma(",");
      tokenizer<char_separator<char> > groups(requiredGroup, comma);
      BOOST_FOREACH(const std::string& group, groups)
      {
         // check group membership
         Error error = core::system::userBelongsToGroup(user,
                                                        group,
                                                        &belongsToGroup);
         if (error)
            LOG_ERROR(error);

         // break if we found a match
         if (belongsToGroup)
            break;
      }

      // log a warning whenever a user doesn't belong to a required group
      if (!belongsToGroup && failureWarning)
      {
         LOG_WARNING_MESSAGE(
          "User " + username + " could not be authenticated because they "
          "do not belong to one of the required groups ("+ requiredGroup +")");
      }

      // return belongs status
      return belongsToGroup;

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
} // namespace rstudio



