/*
 * ServerCheckConfig.hpp
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

#ifndef SERVER_CHECK_CONFIG_HPP
#define SERVER_CHECK_CONFIG_HPP

#include <ostream>
#include <string>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace server {

// Check whether a configured file path exists on disk, printing a [PASS] or
// [FAIL] line to `out`.  When the path is empty the check is skipped and true
// is returned.  When `informational` is true a missing path is reported as
// [PASS] with a clarifying note rather than [FAIL] (used for paths that the
// server creates on startup, such as server-data-dir).  Returns true if the
// overall check passed (or was skipped / informational).
bool checkConfigFilePath(const std::string& optionName,
                         const core::FilePath& path,
                         std::ostream& out,
                         bool informational = false);

// Convenience overload that accepts a plain string path.
bool checkConfigFilePath(const std::string& optionName,
                         const std::string& path,
                         std::ostream& out,
                         bool informational = false);

} // namespace server
} // namespace rstudio

#endif // SERVER_CHECK_CONFIG_HPP
