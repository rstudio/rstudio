/*
 * ServerErrorCategory.cpp
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

#include <server/ServerErrorCategory.hpp>

namespace rstudio {
namespace server {

class ServerErrorCategory : public boost::system::error_category
{
public:
   virtual const char * name() const BOOST_NOEXCEPT;
   virtual std::string message( int ev ) const;
};

const boost::system::error_category& serverCategory()
{
   static ServerErrorCategory serverErrorCategoryConst;
   return serverErrorCategoryConst;
}

const char * ServerErrorCategory::name() const BOOST_NOEXCEPT
{
   return "server";
}

std::string ServerErrorCategory::message( int ev ) const
{
   std::string message;
   switch (ev)
   {
      case errc::AuthenticationError:
         message = "Authentication error";
         break;

     case errc::SessionUnavailableError:
         message = "Session unavailable error";
         break;

     case errc::InvalidSessionScopeError:
         message = "Invalid session scope error";
         break;
         
      default:
         message = "Unknown error";
         break;
   }

   return message;
}


bool isAuthenticationError(const core::Error& error)
{
   if (error == server::errc::AuthenticationError)
      return true;
   else
      return false;
}

bool isSessionUnavailableError(const core::Error& error)
{
   if (error == server::errc::SessionUnavailableError)
      return true;
   else
      return false;
}

bool isInvalidSessionScopeError(const core::Error& error)
{
   if (error == server::errc::InvalidSessionScopeError)
      return true;
   else
      return false;
}

} // namespace server
} // namespace rstudio
