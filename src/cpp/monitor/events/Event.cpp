/*
 * Event.cpp
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

#include <monitor/events/Event.hpp>

#include <iostream>

#include <core/StringUtils.hpp>

#include <core/http/Util.hpp>

namespace monitor {

std::ostream& operator<<(std::ostream& ostr, const Event& event)
{
   switch(event.scope())
   {
      case kAuthScope:
         ostr << "auth";
         break;
      case kSessionScope:
         ostr << "session";
         break;
      default:
         ostr << "<unknown>";
         break;
   }
   ostr << " ";

   switch(event.id())
   {
      case kAuthLoginEvent:
         ostr << "login";
         break;
      case kAuthLogoutEvent:
         ostr << "logout";
         break;
      case kSessionStartEvent:
         ostr << "start";
         break;
      case kSessionSuicideEvent:
         ostr << "suicide";
         break;
      case kSessionSuspendEvent:
         ostr << "suspend";
         break;
      case kSessionQuitEvent:
         ostr << "quit";
         break;
      case kSessionExitEvent:
         ostr << "exit";
         break;
      default:
         ostr << "<unknown>";
         break;
   }

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

