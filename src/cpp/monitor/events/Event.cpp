/*
 * Event.cpp
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

#include <monitor/events/Event.hpp>

#include <iostream>

#include <core/Log.hpp>
#include <core/StringUtils.hpp>

#include <core/http/Util.hpp>

namespace rstudio {
namespace monitor {

Event::Event(int scope,
             int id,
             const std::string& data,
             const std::string& username,
             PidType pid,
             boost::posix_time::ptime timestamp)
   : empty_(false),
     scope_(scope),
     id_(id),
     pid_(pid),
     timestamp_(core::date_time::millisecondsSinceEpoch(timestamp))
{
   ::memset(&username_, 0, kMaxEventDataSize+1);
   username.copy(username_, kMaxEventDataSize);
   if (username.length() > kMaxEventDataSize)
   {
      LOG_WARNING_MESSAGE("Event " + eventScopeAndIdAsString(*this) + " username was truncated");
   }
   ::memset(&data_, 0, kMaxEventDataSize+1);
   data.copy(data_, kMaxEventDataSize);
   if (data.length() > kMaxEventDataSize)
   {
      LOG_WARNING_MESSAGE("Event " + eventScopeAndIdAsString(*this) + " data was truncated");
   }
}

std::string eventScopeAndIdAsString(const Event& event)
{
   std::string scope;
   switch(event.scope())
   {
      case kAuthScope:
         scope = "auth";
         break;
      case kSessionScope:
         scope = "session";
         break;
      default:
         scope = "<unknown>";
         break;
   }
   
   std::string id;
   switch(event.id())
   {
      case kAuthLoginEvent:
         id = "login";
         break;
      case kAuthLogoutEvent:
         id = "logout";
         break;
      case kAuthLoginFailedEvent:
         id = "login_failed";
         break;
      case kAuthLoginThrottledEvent:
         id = "throttled";
         break;
      case kAuthLoginUnlicensedEvent:
         id = "unlicensed";
         break;
      case kAuthLicenseFailedEvent:
         id = "license_failed";
         break;
      case kSessionStartEvent:
         id = "start";
         break;
      case kSessionSuicideEvent:
         id = "suicide";
         break;
      case kSessionSuspendEvent:
         id = "suspend";
         break;
      case kSessionQuitEvent:
         id = "quit";
         break;
      case kSessionExitEvent:
         id = "exit";
         break;
      case kSessionAdminSuspend:
         id = "admin_suspend";
         break;
      case kSessionAdminTerminate:
         id = "admin_terminate";
         break;
      default:
         id = "<unknown>";
         break;
   }
   
   return scope + "_" + id;
}

std::ostream& operator<<(std::ostream& ostr, const Event& event)
{
   ostr << eventScopeAndIdAsString(event);
   ostr << " - ";
   ostr << event.username() << " [" << event.pid() << "] - ";
   ostr << core::http::util::httpDate(event.timestamp());
   if (!event.data().empty())
   {
      ostr << " - " << event.data();
   }

   return ostr;
}

} // namespace monitor
} // namespace rstudio

