/*
 * SessionSuspend.hpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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
namespace session {
namespace suspend {

// Types of operations that will prevent the session from
// suspending due to inactivity
const char * const kChildProcess = "active-child-process";
const char * const kExecuting = "executing";
const char * const kConnection = "active-connection";
const char * const kOverlay = "overlay";
const char * const kExternalPointer = "active-external-pointer";
const char * const kActiveJob = "active-job";
const char * const kCommandPrompt = "incomplete-command-prompt";

bool disallowSuspend();
void resetSuspendTimeout();
void addBlockingOp(std::string op);
void addBlockingOp(const std::string& method, const boost::function<bool()>& allowSuspend);
void removeBlockingOp(std::string op);
bool checkBlockingOp(bool blocking, std::string op);
void checkForSuspend(const boost::function<bool()>& allowSuspend);

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
