/*
 * SessionLists.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#ifndef SESSION_LISTS_HPP
#define SESSION_LISTS_HPP

#include <string>
#include <utility>

#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace lists {

core::json::Object allListsAsJson();

core::Error initialize();

// Helpers for project_name_mru entry manipulation. Exposed via the header
// only so they can be exercised by SessionListsTests; production code in
// SessionLists.cpp uses the file-local wrappers that pull the home path
// from module_context::userHomePath().
namespace detail {

// Split a project_name_mru entry into (path, name). Returns an empty name
// when the entry has no separator.
std::pair<std::string, std::string> splitProjectMruEntry(const std::string& entry);

// Join a (path, name) pair back into the on-disk MRU entry form.
std::string joinProjectMruEntry(const std::string& path, const std::string& name);

// Canonicalize the path portion of a project_name_mru entry against the
// given home path. Aliased forms (~/...) are resolved to absolute form,
// and separators are normalized so aliased and non-aliased representations
// of the same project produce identical strings (see rstudio/rstudio#17225).
std::string canonicalizeProjectMruEntry(const std::string& entry,
                                        const core::FilePath& homePath);

// Alias the path portion of a project_name_mru entry against the given
// home path. Paths within the home directory are rewritten to ~ form;
// paths already aliased or outside the home directory pass through.
std::string aliasProjectMruEntry(const std::string& entry,
                                 const core::FilePath& homePath);

} // namespace detail

} // namespace lists
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_LISTS_HPP
