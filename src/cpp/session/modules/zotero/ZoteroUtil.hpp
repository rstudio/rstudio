/*
 * ZoteroUtil.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef RSTUDIO_SESSION_MODULES_ZOTERO_UTIL_HPP
#define RSTUDIO_SESSION_MODULES_ZOTERO_UTIL_HPP

#include <string>

#include <boost/optional.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace zotero {

extern const char * const kMyLibrary;

void TRACE(const std::string& message, boost::optional<std::size_t> items = boost::optional<std::size_t>());

} // end namespace zotero
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_MODULES_ZOTERO_UTIL_HPP */
