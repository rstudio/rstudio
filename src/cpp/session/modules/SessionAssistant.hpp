/*
 * SessionAssistant.hpp
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

#ifndef RSTUDIO_SESSION_MODULES_ASSISTANT_HPP
#define RSTUDIO_SESSION_MODULES_ASSISTANT_HPP

namespace rstudio {
namespace core {
class Error;
} // namespace core
} // namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace assistant {

int assistantRuntimeStatus();

// Synchronously stop the assistant agent, waiting for the process to exit so
// its file handles are released. Returns true if the agent stopped (or was not
// running), false on timeout. Use before install/uninstall, which overwrite or
// delete the agent's files on disk.
//
// Must NOT be called from within a process-supervisor poll (e.g. a manifest
// fetch completion delivered inline on the main thread): the wait re-enters the
// supervisor poll, whose re-entrancy guard makes the nested poll a no-op, so the
// agent's exit is never observed and the wait runs its full timeout. Use
// requestAgentStop() on those paths instead.
bool stopAgentForUpdate();

// Request that the assistant agent stop, without waiting for it to exit. The
// process is terminated and reaped by normal background polling. Use when the
// running agent has become unsupported and should no longer run; this is safe to
// call from within a supervisor poll, where stopAgentForUpdate() would block for
// its full timeout.
void requestAgentStop();

core::Error initialize();

} // end namespace assistant
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_ASSISTANT_HPP */
