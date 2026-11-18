/*
 * ChatExecutionDeadline.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include "ChatExecutionDeadline.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace execution {

void ExecutionDeadline::arm(Clock::time_point queuedTime, int timeoutMs)
{
   passed_ = false;
   interrupted_ = false;
   if (timeoutMs > 0)
      deadline_ = queuedTime + std::chrono::milliseconds(timeoutMs);
   else
      deadline_ = boost::none;
}

bool ExecutionDeadline::expired(Clock::time_point now)
{
   if (deadline_ && now >= *deadline_)
      passed_ = true;

   return passed_;
}

bool ExecutionDeadline::takeInterrupt(Clock::time_point now)
{
   if (!deadline_ || interrupted_ || !expired(now))
      return false;

   interrupted_ = true;
   return true;
}

bool ExecutionDeadline::passed() const
{
   return passed_;
}

bool ExecutionDeadline::interrupted() const
{
   return interrupted_;
}

bool ExecutionDeadline::disarm()
{
   deadline_ = boost::none;
   return passed_;
}

bool expiredInQueue(Clock::time_point queuedTime, int timeoutMs, Clock::time_point now)
{
   return timeoutMs > 0 && now - queuedTime >= std::chrono::milliseconds(timeoutMs);
}

} // namespace execution
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
