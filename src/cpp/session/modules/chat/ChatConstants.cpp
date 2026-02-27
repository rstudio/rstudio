/*
 * ChatConstants.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include "ChatConstants.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace constants {

// Installation paths
const char* const kPositAiDirName = "pai/bin";
const char* const kClientDirPath = "dist/client";
const char* const kServerScriptPath = "dist/server/main.js";
const char* const kIndexFileName = "index.html";
const char* const kCspConfigPath = "dist/csp.json";

// Protocol Version (SUPPORTED_PROTOCOL_VERSION)
const char* const kProtocolVersion = "8.0";

// Capabilities: JSON-RPC methods that RStudio handles
const std::vector<std::string>& rstudioCapabilities()
{
   static const std::vector<std::string> s_capabilities = {
      "runtime/getActiveSession",
      "runtime/getDetailedContext",
      "runtime/executeCode",
      "runtime/getConsoleContent",
      "workspace/readFileContent",
      "workspace/writeFileContent",
      "workspace/editFileContent",
      "workspace/insertIntoNewFile",
      "workspace/insertAtCursor",
      "ui/openDocument",
   };
   return s_capabilities;
}

} // namespace constants
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
