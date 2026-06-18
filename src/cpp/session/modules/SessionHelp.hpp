/*
 * SessionHelp.hpp
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

#ifndef SESSION_HELP_HPP
#define SESSION_HELP_HPP

#include <vector>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace help {

core::Error initialize();

// Helpers for help page post-processing. Exposed via the header only so
// they can be exercised by SessionHelpTests; production code applies them
// from the help contents filter when serving help pages.
namespace detail {

// Add the "r-arguments-title" class to the "<h3>Arguments</h3>" section
// header of a rendered help page. The h3 may carry attributes (e.g. an id
// when R's dynamic help server emits a table of contents, R >= 4.6.0), so
// any existing attributes are preserved.
void addArgumentsHeaderClass(std::vector<char>* pContents);

} // namespace detail

} // namespace help
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_HELP_HPP
