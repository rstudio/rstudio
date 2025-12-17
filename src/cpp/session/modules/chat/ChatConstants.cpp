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
const char* const kPositAiDirName = "ai";
const char* const kClientDirPath = "dist/client";
const char* const kServerScriptPath = "dist/server/main.js";
const char* const kIndexFileName = "index.html";

// Protocol Version (SUPPORTED_PROTOCOL_VERSION)
const char* const kProtocolVersion = "4.0";

} // namespace constants
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
