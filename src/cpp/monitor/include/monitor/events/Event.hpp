/*
 * Event.hpp
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

#ifndef MONITOR_EVENTS_EVENT_HPP
#define MONITOR_EVENTS_EVENT_HPP

#include <iosfwd>
#include <gsl/gsl>

#include <boost/date_time/posix_time/posix_time.hpp>
#include <core/DateTime.hpp>
#include <core/system/System.hpp>

namespace rstudio {
namespace monitor {

enum EventScope
{
   kAuthScope = 0,
   kSessionScope = 1
};

#define kAuthLoginEvent          1001
#define kAuthLogoutEvent         1002
#define kAuthLoginFailedEvent    1003
#define kAuthLoginThrottledEvent 1004
#define kAuthLicenseFailedEvent  1005
#define kAuthLoginUnlicensedEvent 1006

#define kSessionStartEvent       2001
#define kSessionSuicideEvent     2002
#define kSessionSuspendEvent     2003
#define kSessionQuitEvent        2004
#define kSessionExitEvent        2005
#define kSessionAdminSuspend     2006
#define kSessionAdminTerminate   2007

// after username max size
#define kMaxEventDataSize     32

struct Event
{
public:
   Event()
      : empty_(true)
   {
   }

   Event(int scope,
         int id,
         const std::string& data = std::string(),
         const std::string& username = core::system::username(),
         PidType pid = core::system::currentProcessId(),
         boost::posix_time::ptime timestamp =
                        boost::posix_time::microsec_clock::universal_time());


public:
   bool empty() const { return empty_; }
   int scope() const { return scope_; }
   int id() const { return id_; }
   std::string username() const { return username_; }
   PidType pid() const { return pid_; }
   boost::posix_time::ptime timestamp() const
   {
      return core::date_time::timeFromMillisecondsSinceEpoch(gsl::narrow_cast<int64_t>(timestamp_));
   }
   std::string data() const { return data_; }


private:
   bool empty_;
   int scope_;
   int id_;
   char username_[kMaxEventDataSize+1];
   PidType pid_;
   double timestamp_;
   char data_[kMaxEventDataSize+1];
};

std::string eventScopeAndIdAsString(const Event& event);

std::ostream& operator<<(std::ostream& ostr, const Event& event);

} // namespace monitor
} // namespace rstudio

#endif // MONITOR_EVENTS_EVENT_HPP

