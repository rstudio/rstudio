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
// that is how long the backend waits for the response. It counts from when
// RStudio reads the request, which can be after the backend sent it: the
// backend's messages are read only while R polls for events, so not while R
// is blocked in native code. A non-positive timeout sets no limit.
class ExecutionDeadline
{
public:
   // Starts enforcing timeoutMs from queuedTime, forgetting any earlier pass
   void arm(Clock::time_point queuedTime, int timeoutMs);

   // Whether the deadline has passed by now; a pass is remembered
   bool expired(Clock::time_point now);

   // True for every call at or after the deadline, so the caller interrupts
   // R again if the agent's code caught the last interrupt; false before
   // the deadline and when disarmed
   bool takeInterrupt(Clock::time_point now);

   // Whether the deadline was found passed since the last arm
   bool passed() const;

   // Whether takeInterrupt asked for an interrupt since the last arm
   bool interrupted() const;

   // Stops enforcing the deadline; returns passed()
   bool disarm();

private:
   boost::optional<Clock::time_point> deadline_;
   bool passed_ = false;
   bool interrupted_ = false;
};

// Whether a request queued at queuedTime used up its whole timeout before it
// could start, so it should not start at all
bool expiredInQueue(Clock::time_point queuedTime, int timeoutMs, Clock::time_point now);

// Decides when queued requests release the console's busy state. A request
// that runs marks the console busy, and while more requests wait it is
// handed from one to the next rather than released in between. A request
// that expires without running releases it only when such a handoff left it
// busy: the expiry may happen in idle work inside something else that holds
// the console (a user command in file.choose(), or a chat request in
// readline()), which must keep it.
class BusyHandoff
{
public:
   // A queued request starts running, and owns the busy state it sets
   void started();

   // A queued request ran, or expired without running; returns whether to
   // release the console's busy state
   bool finished(bool ran, bool queueEmpty);

private:
   bool held_ = false;
};

// The timeout option's value, read as a JSON number, in whole milliseconds:
// a fraction rounds up rather than down to "no limit", and a value past
// int's range is clamped. Non-positive means no limit (0).
int timeoutFromOption(double timeoutMs);

} // namespace execution
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_EXECUTION_DEADLINE_HPP
