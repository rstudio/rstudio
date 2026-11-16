/*
 * ChatBackendStdout.hpp
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

#ifndef SESSION_CHAT_BACKEND_STDOUT_HPP
#define SESSION_CHAT_BACKEND_STDOUT_HPP

#include <cstdint>
#include <string>
#include <vector>

#include "../SessionAssistant.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace backend {

// Reads the stdout of one backend process at a time, identified by its
// generation (never 0). A backend that is stopped or replaced can still write
// before it exits; that output is dropped, so its requests aren't handled and
// a partial message can't swallow the next backend's first messages.
class StdoutReader
{
public:
   // Read this backend's output, and only its output, from now on.
   void start(std::uint64_t generation);

   // Read no output until the next start().
   void stop();

   // The bodies of the messages this chunk of the backend's output completes.
   std::vector<std::string> append(std::uint64_t generation,
                                   const std::string& chunk,
                                   std::vector<std::string>* pErrors = nullptr);

private:
   assistant::AgentStdoutReader reader_;
   std::uint64_t generation_ = 0;
};

} // namespace backend
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_BACKEND_STDOUT_HPP
