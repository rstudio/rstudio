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

/**
 * Find the node.js binary on the system.
 *
 * @param pNodePath Output parameter for the path to node.js
 * @param rOptionName Optional R option name to check for user override (e.g., "rstudio.copilot.nodeBinaryPath")
 * @return Error if node.js could not be found, Success() otherwise
 */
core::Error findNode(core::FilePath* pNodePath, const std::string& rOptionName = "");

/**
 * Append a Node.js option to an existing NODE_OPTIONS value.
 *
 * Preserves the caller's value verbatim and avoids adding a duplicate flag; the
 * option is appended after a single space when absent.
 *
 * @param existingOptions The current NODE_OPTIONS value (may be empty).
 * @param option The flag to append, e.g. "--use-system-ca".
 * @return The merged NODE_OPTIONS value.
 */
std::string appendNodeOption(const std::string& existingOptions,
                             const std::string& option);

/**
 * Parse the major and minor version from `node --version` output
 * (e.g. "v22.22.2\n" -> 22, 22).
 *
 * @param versionOutput Raw stdout from `node --version`.
 * @param pMajor Output major version.
 * @param pMinor Output minor version.
 * @return true if a version was parsed, false otherwise.
 */
bool parseNodeVersion(const std::string& versionOutput, int* pMajor, int* pMinor);

/**
 * Whether the Node binary at nodePath supports `--use-system-ca` via
 * NODE_OPTIONS (Node >= 22.17.0). Runs `<nodePath> --version` and parses the
 * result. Returns false (with a logged warning) if the version cannot be
 * determined, so callers fail safe by not injecting the flag.
 */
bool nodeSupportsSystemCa(const core::FilePath& nodePath);

} // namespace node_tools
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_MODULES_SESSION_NODE_TOOLS_HPP
