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

#include <cstddef>
#include <string>

#include <shared_core/FilePath.hpp>

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

// Default bound applied to agent stderr before logging or accumulating it. A
// crashing Node.js agent prints the offending source line before the stack
// trace, and for a bundled agent that "line" is the entire minified bundle --
// tens of kilobytes per chunk with the useful text (error and stack trace) at
// the end.
constexpr std::size_t kAgentStderrMaxBytes = 4096;

// Bounded tail of agent stderr: keeps at most the last maxLength bytes
// appended, plus a cumulative count of the bytes dropped. text() prefixes a
// truncated tail with a marker noting that count (so the result can slightly
// exceed maxLength). The cut never lands mid-way through a UTF-8 character,
// since the text is embedded in JSON. Exposed for testing.
class AgentStderrTail
{
public:
   explicit AgentStderrTail(std::size_t maxLength)
      : maxLength_(maxLength)
   {
   }

   void append(const std::string& text);
   void set(const std::string& text);
   void clear();
   std::string text() const;

private:
   std::size_t maxLength_;
   std::size_t droppedBytes_ = 0;
   std::string tail_;
};

// One-shot convenience over AgentStderrTail, for bounding a single chunk.
std::string agentStderrTail(const std::string& text, std::size_t maxLength);

// Resolve the next-edit-suggestion (NES) language server script within a Posit
// Assistant installation directory. Returns an empty path when installPath is
// empty (no installation was located) or holds no language server script.
// Exposed for testing.
core::FilePath nesLanguageServerPath(const core::FilePath& installPath);

// Synchronously stop the assistant agent, waiting for the process to exit so
// its file handles are released. Returns true if the agent stopped (or was not
// running), false on timeout. Use before an install or update, which overwrite
// the agent's files on disk.
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
