/*
 * SessionNodeTools.hpp
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

#ifndef SESSION_MODULES_SESSION_NODE_TOOLS_HPP
#define SESSION_MODULES_SESSION_NODE_TOOLS_HPP

#include <string>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace node_tools {

#ifndef _WIN32
# define kNodeExe "node"
#else
# define kNodeExe "node.exe"
#endif

/**
 * Find the node.js binary on the system.
 *
 * @param pNodePath Output parameter for the path to node.js
 * @param rOptionName Optional R option name to check for user override (e.g., "rstudio.copilot.nodeBinaryPath")
 * @return Error if node.js could not be found, Success() otherwise
 */
core::Error findNode(core::FilePath* pNodePath, const std::string& rOptionName = "");

} // namespace node_tools
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULES_SESSION_NODE_TOOLS_HPP
