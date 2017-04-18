/*
 * SessionFind.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef SESSION_FIND_HPP
#define SESSION_FIND_HPP

#include <core/Error.hpp>
#include <core/json/Json.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace find {

core::json::Object findInFilesStateAsJson();

core::Error initialize();

} // namespace find
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_FIND_HPP
