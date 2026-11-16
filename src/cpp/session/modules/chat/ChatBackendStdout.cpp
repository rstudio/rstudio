/*
 * ChatBackendStdout.cpp
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

#include "ChatBackendStdout.hpp"

#include "ChatLogging.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace backend {

void StdoutReader::start(std::uint64_t generation)
{
   reader_.clear();
   generation_ = generation;
}

void StdoutReader::stop()
{
   reader_.clear();
   generation_ = 0;
}

std::vector<std::string> StdoutReader::append(std::uint64_t generation,
                                              const std::string& chunk,
                                              std::vector<std::string>* pErrors)
{
   if (generation_ == 0 || generation != generation_)
   {
      DLOG("Ignoring stdout from stopped or stale chat backend generation");
      return std::vector<std::string>();
   }

   return reader_.append(chunk, pErrors);
}

} // namespace backend
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
