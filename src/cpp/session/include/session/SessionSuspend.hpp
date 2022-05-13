/*
 * SessionSuspend.hpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#ifndef SESSION_SUSPEND_HPP
#define SESSION_SUSPEND_HPP

#include <boost/function.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace suspend {

// Types of operations that will prevent the session from
// suspending due to inactivity
const char * const kChildProcess = "A child process is running";
const char * const kExecuting = "R is executing";
const char * const kConnection = "A connection is active";
const char * const kExternalPointer = "Active external data pointer";
const char * const kActiveJob = "An active job is running";
const char * const kCommandPrompt = "Incomplete command prompt entered";
const char * const kGenericMethod = "Waiting for event: ";

core::Error initialize();

bool disallowSuspend();
void resetSuspendTimeout();
void addBlockingOp(std::string op);
void addBlockingOp(std::string method, const boost::function<bool()>& allowSuspend);
void removeBlockingOp(std::string op);
bool checkBlockingOp(bool blocking, std::string op);
void checkForSuspend(const boost::function<bool()>& allowSuspend);
std::string getResumedMessage();
void initFromResume();

bool suspendSession(bool force, int status = EXIT_SUCCESS);
void handleUSR1(int unused);
void handleUSR2(int unused);
bool suspendedFromTimeout();
void setSuspendedFromTimeout(bool suspended);
bool sessionResumed();
void setSessionResumed(bool resumed);

} // namespace suspend
} // namespace session
} // namespace rstudio

#endif /* SESSION_SUSPEND_HPP */
