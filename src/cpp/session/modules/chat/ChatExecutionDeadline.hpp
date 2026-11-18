/*
 * ChatExecutionDeadline.hpp
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

#ifndef SESSION_CHAT_EXECUTION_DEADLINE_HPP
#define SESSION_CHAT_EXECUTION_DEADLINE_HPP

#include <chrono>

#include <boost/optional.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace execution {

using Clock = std::chrono::steady_clock;

// The time limit a runtime/executeCode request sets with its timeout option,
// in milliseconds. It covers the whole request, queue wait included, since
// that is how long the backend waits for the response. A non-positive
// timeout sets no limit.
class ExecutionDeadline
{
public:
   // Starts enforcing timeoutMs from queuedTime, forgetting any earlier pass
   void arm(Clock::time_point queuedTime, int timeoutMs);

   // True for the first check at or after the deadline, so the caller
   // interrupts R once; false before it, after it, and when disarmed
   bool check(Clock::time_point now);

   // Whether a check found the deadline passed since the last arm
   bool passed() const;

   // Stops enforcing the deadline; returns passed()
   bool disarm();

private:
   boost::optional<Clock::time_point> deadline_;
   bool passed_ = false;
};

// Whether a request queued at queuedTime used up its whole timeout before it
// could start, so it should not start at all
bool expiredInQueue(Clock::time_point queuedTime, int timeoutMs, Clock::time_point now);

} // namespace execution
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_EXECUTION_DEADLINE_HPP
